package net.ochibo.twilightteleport.client.render;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;


public final class TeleportShaderPackCompat {

    private static final String[] IRIS_API_CLASS_NAMES = {
            "net.irisshaders.iris.api.v0.IrisApi",
            "net.coderbot.iris.api.v0.IrisApi"
    };

    private static final String[] IRIS_API_IMPL_CLASS_NAMES = {
            "net.irisshaders.iris.apiimpl.IrisApiV0Impl",
            "net.coderbot.iris.apiimpl.IrisApiV0Impl"
    };

    private static final String[] SHADOW_STATE_CLASS_NAMES = {
            "net.irisshaders.iris.shadows.ShadowRenderingState",
            "net.coderbot.iris.shadows.ShadowRenderingState"
    };

    private static boolean apiInitialized;
    private static Method getInstanceMethod;
    private static Method isShaderPackInUseMethod;
    private static Method apiIsRenderingShadowPassMethod;

    private static boolean internalShadowStateInitialized;
    private static Object internalApiInstance;
    private static Method internalIsRenderingShadowPassMethod;
    private static Method areShadowsCurrentlyBeingRenderedMethod;

    private TeleportShaderPackCompat() {
    }

    public static boolean isShaderPackInUse() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) {
            return false;
        }

        initializeApi();

        if (getInstanceMethod == null
                || isShaderPackInUseMethod == null) {
            return false;
        }

        try {
            Object irisApi = getInstanceMethod.invoke(null);

            return Boolean.TRUE.equals(
                    isShaderPackInUseMethod.invoke(irisApi)
            );
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    
    public static boolean isShadowPass() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) {
            return false;
        }

        initializeApi();

        
        if (getInstanceMethod != null
                && apiIsRenderingShadowPassMethod != null) {
            try {
                Object irisApi = getInstanceMethod.invoke(null);

                return Boolean.TRUE.equals(
                        apiIsRenderingShadowPassMethod.invoke(irisApi)
                );
            } catch (ReflectiveOperationException ignored) {
                
            }
        }

        initializeInternalShadowState();

        if (internalApiInstance != null
                && internalIsRenderingShadowPassMethod != null) {
            try {
                return Boolean.TRUE.equals(
                        internalIsRenderingShadowPassMethod.invoke(
                                internalApiInstance
                        )
                );
            } catch (ReflectiveOperationException ignored) {
                
            }
        }

        if (areShadowsCurrentlyBeingRenderedMethod != null) {
            try {
                return Boolean.TRUE.equals(
                        areShadowsCurrentlyBeingRenderedMethod.invoke(null)
                );
            } catch (ReflectiveOperationException ignored) {
                return false;
            }
        }

        return false;
    }

    private static synchronized void initializeApi() {
        if (apiInitialized) {
            return;
        }

        apiInitialized = true;

        for (String className : IRIS_API_CLASS_NAMES) {
            try {
                Class<?> irisApiClass = Class.forName(
                        className,
                        false,
                        TeleportShaderPackCompat.class.getClassLoader()
                );

                getInstanceMethod = irisApiClass.getMethod("getInstance");
                isShaderPackInUseMethod = irisApiClass.getMethod(
                        "isShaderPackInUse"
                );

                try {
                    apiIsRenderingShadowPassMethod =
                            irisApiClass.getMethod(
                                    "isRenderingShadowPass"
                            );
                } catch (NoSuchMethodException ignored) {
                    apiIsRenderingShadowPassMethod = null;
                }

                return;
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                
            }
        }
    }

    private static synchronized void initializeInternalShadowState() {
        if (internalShadowStateInitialized) {
            return;
        }

        internalShadowStateInitialized = true;
        ClassLoader loader = TeleportShaderPackCompat.class.getClassLoader();

        
        for (String className : IRIS_API_IMPL_CLASS_NAMES) {
            try {
                Class<?> implementationClass = Class.forName(
                        className,
                        false,
                        loader
                );

                Field instanceField =
                        implementationClass.getField("INSTANCE");

                Method shadowPassMethod =
                        implementationClass.getMethod(
                                "isRenderingShadowPass"
                        );

                internalApiInstance = instanceField.get(null);
                internalIsRenderingShadowPassMethod = shadowPassMethod;
                break;
            } catch (
                    ClassNotFoundException
                            | NoSuchFieldException
                            | NoSuchMethodException
                            | IllegalAccessException ignored
            ) {
                
            }
        }

        
        for (String className : SHADOW_STATE_CLASS_NAMES) {
            try {
                Class<?> shadowStateClass = Class.forName(
                        className,
                        false,
                        loader
                );

                areShadowsCurrentlyBeingRenderedMethod =
                        shadowStateClass.getMethod(
                                "areShadowsCurrentlyBeingRendered"
                        );
                return;
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                
            }
        }
    }
}
