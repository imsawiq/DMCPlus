package org.sawiq.dmcplus.client.feature.waypoint;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class WaypointHudRenderer {

    private static final int PANEL_OUTER = 0xD0282828;
    private static final int PANEL_INNER = 0xF0333333;
    private static final int PANEL_BORDER = 0xA0626262;
    private static final int PANEL_SHADOW = 0x50000000;
    private static final int TEXT_PRIMARY = 0xFFF1F1F1;
    private static final int TEXT_SECONDARY = 0xFFBEBEBE;
    private static final int TEXT_HIGHLIGHT = 0xFFF2D085;
    private static final int BAR_BACKGROUND = 0xFF1B1B1B;
    private static final int BAR_CENTER = 0xFF797979;
    private static final int MIN_PANEL_WIDTH = 188;
    private static final int MAX_PANEL_WIDTH = 260;

    public void render(DrawContext context, MinecraftClient client, WaypointTarget target, float visibility) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        boolean currentNether = client.world != null && client.world.getRegistryKey() == World.NETHER;
        BlockPos displayPosition = target.positionFor(currentNether);
        double dx = displayPosition.getX() + 0.5D - player.getX();
        double dz = displayPosition.getZ() + 0.5D - player.getZ();
        int distance = Math.max(0, MathHelper.floor(Math.sqrt(dx * dx + dz * dz)));

        String title = target.label();
        String coordinates = displayPosition.getX() + " " + displayPosition.getY() + " " + displayPosition.getZ() + "  |  " + distance + "м";
        String conversion = target.convertedFor(currentNether) ? "другой мир, координаты пересчитаны" : null;
        int contentWidth = Math.max(textRenderer.getWidth(title), textRenderer.getWidth(coordinates));
        if (conversion != null) {
            contentWidth = Math.max(contentWidth, textRenderer.getWidth(conversion));
        }

        int width = MathHelper.clamp(contentWidth + 24, MIN_PANEL_WIDTH, MAX_PANEL_WIDTH);
        int height = conversion == null ? 52 : 64;
        int x = (context.getScaledWindowWidth() - width) / 2;
        int y = 38 - Math.round((1.0F - visibility) * 8.0F);
        int outerColor = scaleAlpha(PANEL_OUTER, visibility);
        int innerColor = scaleAlpha(PANEL_INNER, visibility);
        int borderColor = scaleAlpha(PANEL_BORDER, visibility);
        int shadowColor = scaleAlpha(PANEL_SHADOW, visibility);
        int primaryColor = scaleAlpha(TEXT_PRIMARY, visibility);
        int secondaryColor = scaleAlpha(TEXT_SECONDARY, visibility);

        context.fill(x + 1, y + 1, x + width + 1, y + height + 1, shadowColor);
        context.fill(x, y, x + width, y + height, outerColor);
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, innerColor);
        context.drawBorder(x, y, width, height, borderColor);

        int accentColor = scaleAlpha(0xFF000000 | (target.color() & 0x00FFFFFF), visibility);
        context.fill(x + 3, y + 3, x + 5, y + height - 3, accentColor);

        context.drawCenteredTextWithShadow(textRenderer, trimToWidth(textRenderer, title, width - 18), x + width / 2, y + 7, primaryColor);
        context.drawCenteredTextWithShadow(textRenderer, coordinates, x + width / 2, y + 19, secondaryColor);

        int barY = y + 33;
        if (conversion != null) {
            context.drawCenteredTextWithShadow(textRenderer, conversion, x + width / 2, y + 31, scaleAlpha(TEXT_HIGHLIGHT, visibility));
            barY = y + 45;
        }

        this.renderDirectionBar(context, textRenderer, player, target, x + 12, barY, width - 24, accentColor, visibility);
    }

    private void renderDirectionBar(
            DrawContext context,
            TextRenderer textRenderer,
            ClientPlayerEntity player,
            WaypointTarget target,
            int x,
            int y,
            int width,
            int accentColor,
            float visibility
    ) {
        float targetYaw = (float) (Math.toDegrees(Math.atan2(
                target.positionFor(player.clientWorld.getRegistryKey() == World.NETHER).getZ() + 0.5D - player.getZ(),
                target.positionFor(player.clientWorld.getRegistryKey() == World.NETHER).getX() + 0.5D - player.getX()
        )) - 90.0D);
        float relativeYaw = MathHelper.wrapDegrees(targetYaw - player.getYaw());
        float normalized = MathHelper.clamp(relativeYaw / 90.0F, -1.0F, 1.0F);

        context.fill(x, y, x + width, y + 9, scaleAlpha(BAR_BACKGROUND, visibility));
        context.drawBorder(x, y, width, 9, scaleAlpha(PANEL_BORDER, visibility));

        int centerX = x + width / 2;
        context.fill(centerX, y + 1, centerX + 1, y + 8, scaleAlpha(BAR_CENTER, visibility));

        int markerX = centerX + Math.round(normalized * ((width / 2) - 6));
        context.fill(markerX - 2, y + 2, markerX + 3, y + 7, accentColor);

        String hint = Math.abs(relativeYaw) < 12.0F ? "прямо" : (relativeYaw < 0.0F ? "левее" : "правее");
        context.drawCenteredTextWithShadow(textRenderer, hint, centerX, y + 10, scaleAlpha(TEXT_HIGHLIGHT, visibility));
    }

    private static String trimToWidth(TextRenderer textRenderer, String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }

        return textRenderer.trimToWidth(text, Math.max(0, maxWidth - textRenderer.getWidth("..."))) + "...";
    }

    private static int scaleAlpha(int color, float alphaMultiplier) {
        int alpha = color >>> 24;
        int scaledAlpha = MathHelper.clamp(Math.round(alpha * alphaMultiplier), 0, 255);
        return (scaledAlpha << 24) | (color & 0x00FFFFFF);
    }
}
