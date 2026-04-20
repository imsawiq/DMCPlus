package org.sawiq.dmcplus.client.feature.branches;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.math.MathHelper;

public class BranchHudRenderer {

    private static final int PANEL_OUTER = 0xD0323232;
    private static final int PANEL_INNER = 0xE03B3B3B;
    private static final int PANEL_BORDER = 0x90787878;
    private static final int SHADOW_COLOR = 0x50000000;
    private static final int SUBTITLE_COLOR = 0xFFD8D8D8;

    public void render(DrawContext context, MinecraftClient client, BranchContext branchContext, BranchHudState state) {
        float visible = state.visibility();
        if (visible < 0.03F || client.player == null) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        Branch branch = branchContext.branch();

        String title = branch.displayName();
        String subtitle = branch.direction().axisName() + ": " + branchContext.axisDistance();
        int width = Math.max(88, Math.max(textRenderer.getWidth(title), textRenderer.getWidth(subtitle)) + 16);
        int height = 22;
        int x = (context.getScaledWindowWidth() - width) / 2;
        int y = 8 - Math.round((1.0F - visible) * 8.0F);

        int outerColor = scaleAlpha(PANEL_OUTER, visible);
        int innerColor = scaleAlpha(PANEL_INNER, visible);
        int borderColor = scaleAlpha(PANEL_BORDER, visible);
        int titleColor = scaleAlpha(branch.color(), 0.8F + visible * 0.2F);
        int subtitleColor = scaleAlpha(SUBTITLE_COLOR, visible);
        int shadowColor = scaleAlpha(SHADOW_COLOR, visible);

        context.fill(x + 1, y + 1, x + width + 1, y + height + 1, shadowColor);
        context.fill(x, y, x + width, y + height, outerColor);
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, innerColor);
        context.drawBorder(x, y, width, height, borderColor);
        int progressWidth = Math.max(6, Math.round((width - 6) * MathHelper.clamp(state.displayedProgress(), 0.0F, 1.0F)));
        context.fill(x + 3, y + height - 3, x + 3 + progressWidth, y + height - 2, scaleAlpha(branch.color(), visible));

        int titleX = x + (width - textRenderer.getWidth(title)) / 2;
        int subtitleX = x + (width - textRenderer.getWidth(subtitle)) / 2;
        context.drawTextWithShadow(textRenderer, title, titleX, y + 4, titleColor);
        context.drawTextWithShadow(textRenderer, subtitle, subtitleX, y + 13, subtitleColor);
    }

    private static int scaleAlpha(int color, float alphaMultiplier) {
        int alpha = color >>> 24;
        int scaledAlpha = MathHelper.clamp(Math.round(alpha * alphaMultiplier), 0, 255);
        return (scaledAlpha << 24) | (color & 0x00FFFFFF);
    }
}
