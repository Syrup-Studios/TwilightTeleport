package net.ochibo.twilightteleport;

import java.util.Locale;


public final class TeleportCommandMatcher {

    private static final String[] TELEPORT_COMMAND_NAMES = {
            "minecraft:teleport",
            "minecraft:tp",
            "teleport",
            "tp"
    };

    private static final String[] EXECUTE_COMMAND_NAMES = {
            "minecraft:execute",
            "execute"
    };

    private TeleportCommandMatcher() {
    }

    public static boolean isTeleportCommand(String command) {
        return getArgumentString(command) != null;
    }

    
    public static String getArgumentString(String command) {
        if (command == null) {
            return null;
        }

        String normalized = command.stripLeading();

        if (normalized.startsWith("/")) {
            normalized = normalized
                    .substring(1)
                    .stripLeading();
        }

        if (normalized.isEmpty()) {
            return null;
        }

        String lowerCase = normalized.toLowerCase(Locale.ROOT);

        for (String name : TELEPORT_COMMAND_NAMES) {
            if (lowerCase.equals(name)) {
                return "";
            }

            if (lowerCase.startsWith(name + " ")) {
                return normalized
                        .substring(name.length())
                        .stripLeading();
            }
        }

        for (String executeName : EXECUTE_COMMAND_NAMES) {
            if (!lowerCase.startsWith(executeName + " ")) {
                continue;
            }

            int runIndex = lowerCase.indexOf(" run ");

            if (runIndex >= 0) {
                return getArgumentString(
                        normalized
                                .substring(runIndex + 5)
                                .stripLeading()
                );
            }
        }

        return null;
    }
}
