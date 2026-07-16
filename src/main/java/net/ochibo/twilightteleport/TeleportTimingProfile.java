package net.ochibo.twilightteleport;

import net.minecraft.util.math.MathHelper;


public record TeleportTimingProfile(
        int moveToSideTicks,
        int dissolveTicks,
        int destinationHoldTicks,
        int rebuildMeshDelayTicks,
        int rebuildTicks,
        int returnToFirstPersonTicks
) {

    public static final int MIN_MOVE_TO_SIDE_TICKS = 2;
    public static final int MAX_MOVE_TO_SIDE_TICKS = 200;

    public static final int MIN_DISSOLVE_TICKS = 5;
    public static final int MAX_DISSOLVE_TICKS = 400;

    public static final int MIN_DESTINATION_HOLD_TICKS = 0;
    public static final int MAX_DESTINATION_HOLD_TICKS = 100;

    public static final int MIN_REBUILD_MESH_DELAY_TICKS = 0;
    public static final int MAX_REBUILD_MESH_DELAY_TICKS = 100;

    public static final int MIN_REBUILD_TICKS = 5;
    public static final int MAX_REBUILD_TICKS = 400;

    public static final int MIN_RETURN_TO_FIRST_PERSON_TICKS = 2;
    public static final int MAX_RETURN_TO_FIRST_PERSON_TICKS = 200;

    public TeleportTimingProfile {
        moveToSideTicks = MathHelper.clamp(
                moveToSideTicks,
                MIN_MOVE_TO_SIDE_TICKS,
                MAX_MOVE_TO_SIDE_TICKS
        );
        dissolveTicks = MathHelper.clamp(
                dissolveTicks,
                MIN_DISSOLVE_TICKS,
                MAX_DISSOLVE_TICKS
        );
        destinationHoldTicks = MathHelper.clamp(
                destinationHoldTicks,
                MIN_DESTINATION_HOLD_TICKS,
                MAX_DESTINATION_HOLD_TICKS
        );
        rebuildMeshDelayTicks = MathHelper.clamp(
                rebuildMeshDelayTicks,
                MIN_REBUILD_MESH_DELAY_TICKS,
                MAX_REBUILD_MESH_DELAY_TICKS
        );
        rebuildTicks = MathHelper.clamp(
                rebuildTicks,
                MIN_REBUILD_TICKS,
                MAX_REBUILD_TICKS
        );
        returnToFirstPersonTicks = MathHelper.clamp(
                returnToFirstPersonTicks,
                MIN_RETURN_TO_FIRST_PERSON_TICKS,
                MAX_RETURN_TO_FIRST_PERSON_TICKS
        );
    }

    public static TeleportTimingProfile defaults() {
        return new TeleportTimingProfile(
                TeleportTimings.MOVE_TO_SIDE_TICKS,
                TeleportTimings.DISSOLVE_TICKS,
                TeleportTimings.DESTINATION_HOLD_TICKS,
                TeleportTimings.REBUILD_MESH_DELAY_TICKS,
                TeleportTimings.REBUILD_TICKS,
                TeleportTimings.RETURN_TO_FIRST_PERSON_TICKS
        );
    }

    public int totalRebuildTicks() {
        return rebuildMeshDelayTicks + rebuildTicks;
    }

    public int dissolveTimeoutTicks() {
        return moveToSideTicks + dissolveTicks + 40;
    }

    public TeleportTimingProfile withDeparture(
            int moveToSideTicks,
            int dissolveTicks
    ) {
        return new TeleportTimingProfile(
                moveToSideTicks,
                dissolveTicks,
                destinationHoldTicks,
                rebuildMeshDelayTicks,
                rebuildTicks,
                returnToFirstPersonTicks
        );
    }

    public TeleportTimingProfile withRebuild(
            int rebuildMeshDelayTicks,
            int rebuildTicks
    ) {
        return new TeleportTimingProfile(
                moveToSideTicks,
                dissolveTicks,
                destinationHoldTicks,
                rebuildMeshDelayTicks,
                rebuildTicks,
                returnToFirstPersonTicks
        );
    }
}
