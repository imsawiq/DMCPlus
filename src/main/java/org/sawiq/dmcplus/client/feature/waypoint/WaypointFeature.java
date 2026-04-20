package org.sawiq.dmcplus.client.feature.waypoint;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.sawiq.dmcplus.client.feature.trade.TradeListing;

public class WaypointFeature {

    private static final double REACHED_DISTANCE = 10.0D;
    private static final float FADE_STEP = 0.12F;

    private final WaypointHudRenderer renderer = new WaypointHudRenderer();
    private WaypointTarget activeTarget;
    private float visibility;
    private boolean hiding;

    public void setTradeTarget(MinecraftClient client, TradeListing listing) {
        boolean sourceNether = client.world != null && client.world.getRegistryKey() == World.NETHER;
        this.setTarget(client, listing.productName(), listing.position(), sourceNether, 0xD9B15F, Text.translatable(
                "message.dmcplus.waypoint_set",
                listing.productName(),
                listing.position().getX(),
                listing.position().getY(),
                listing.position().getZ()
        ));
    }

    public void setGuardTarget(MinecraftClient client, String playerName, BlockPos sourcePosition, boolean sourceNether) {
        boolean currentNether = client.world != null && client.world.getRegistryKey() == World.NETHER;
        BlockPos displayPosition = this.convertPosition(sourcePosition, sourceNether, currentNether);
        this.setTarget(client, "Вызов: " + playerName, sourcePosition, sourceNether, 0x6AA9FF, Text.translatable(
                sourceNether != currentNether ? "message.dmcplus.guard_waypoint_set_converted" : "message.dmcplus.guard_waypoint_set",
                playerName,
                displayPosition.getX(),
                displayPosition.getY(),
                displayPosition.getZ()
        ));
    }

    private void setTarget(MinecraftClient client, String label, BlockPos sourcePosition, boolean sourceNether, int color, Text message) {
        this.activeTarget = new WaypointTarget(label, sourcePosition, sourceNether, color);
        this.hiding = false;
        if (client.player != null) {
            client.player.sendMessage(message.copy().formatted(Formatting.GOLD), true);
        }
    }

    public void clear(MinecraftClient client) {
        if (this.activeTarget == null) {
            return;
        }

        this.hiding = true;
        if (client.player != null) {
            client.player.sendMessage(Text.translatable("message.dmcplus.waypoint_cleared").formatted(Formatting.RED), true);
        }
    }

    public void tick(MinecraftClient client) {
        if (this.activeTarget == null) {
            this.visibility = 0.0F;
            this.hiding = false;
            return;
        }

        if (!this.hiding && client.player != null) {
            boolean currentNether = client.world != null && client.world.getRegistryKey() == World.NETHER;
            double distance = Math.sqrt(this.activeTarget.positionFor(currentNether).getSquaredDistance(client.player.getBlockPos()));
            if (distance <= REACHED_DISTANCE) {
                this.hiding = true;
                client.player.sendMessage(Text.translatable("message.dmcplus.waypoint_reached").formatted(Formatting.GREEN), true);
            }
        }

        if (this.hiding) {
            this.visibility = Math.max(0.0F, this.visibility - FADE_STEP);
            if (this.visibility <= 0.0F) {
                this.activeTarget = null;
                this.hiding = false;
            }
        } else {
            this.visibility = Math.min(1.0F, this.visibility + FADE_STEP);
        }
    }

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || this.activeTarget == null || client.currentScreen != null || this.visibility <= 0.0F) {
            return;
        }

        this.renderer.render(context, client, this.activeTarget, this.visibility);
    }

    public boolean hasTarget() {
        return this.activeTarget != null;
    }

    private BlockPos convertPosition(BlockPos sourcePosition, boolean sourceNether, boolean currentNether) {
        if (sourceNether == currentNether) {
            return sourcePosition;
        }

        if (sourceNether) {
            return new BlockPos(sourcePosition.getX() * 8, sourcePosition.getY(), sourcePosition.getZ() * 8);
        }

        return new BlockPos(Math.floorDiv(sourcePosition.getX(), 8), sourcePosition.getY(), Math.floorDiv(sourcePosition.getZ(), 8));
    }
}
