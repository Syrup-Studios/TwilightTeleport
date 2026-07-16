package net.ochibo.twilightteleport;

public final class TeleportTimings {

    
    public static final int MOVE_TO_SIDE_TICKS = 20;

    
    public static final int DISSOLVE_TICKS = 60;

    
    public static final int DESTINATION_HOLD_TICKS = 5;

    
    public static final int REBUILD_MESH_DELAY_TICKS = 6;

    
    public static final int REBUILD_TICKS = 60;

    
    public static final int RETURN_TO_FIRST_PERSON_TICKS = 10;

    
    public static final int MILLISECONDS_PER_TICK = 50;

    private TeleportTimings() {
    }

    public static int ticksToMilliseconds(int ticks) {
        return ticks * MILLISECONDS_PER_TICK;
    }

    
    public static int getTotalRebuildTicks() {
        return REBUILD_MESH_DELAY_TICKS
                + REBUILD_TICKS;
    }
}