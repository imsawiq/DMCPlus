package org.sawiq.dmcplus.client.feature.waypoint;

import net.minecraft.util.math.BlockPos;

public record WaypointTarget(
        String label,
        BlockPos sourcePosition,
        boolean sourceNether,
        int color
) {

    public BlockPos positionFor(boolean currentNether) {
        if (this.sourceNether == currentNether) {
            return this.sourcePosition;
        }

        if (this.sourceNether) {
            return new BlockPos(this.sourcePosition.getX() * 8, this.sourcePosition.getY(), this.sourcePosition.getZ() * 8);
        }

        return new BlockPos(Math.floorDiv(this.sourcePosition.getX(), 8), this.sourcePosition.getY(), Math.floorDiv(this.sourcePosition.getZ(), 8));
    }

    public boolean convertedFor(boolean currentNether) {
        return this.sourceNether != currentNether;
    }
}
