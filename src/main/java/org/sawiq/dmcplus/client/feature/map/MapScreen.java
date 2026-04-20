package org.sawiq.dmcplus.client.feature.map;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;

public class MapScreen extends Screen {

    private static final int FRAME_MARGIN = 14;
    private static final int HEADER_HEIGHT = 28;

    private final Screen parent;
    private final String mapUrl;
    private final Text dimensionText;
    private final Text coordinatesText;
    private final Text markerText;

    private MCEFBrowser browser;
    private boolean browserFocused;
    private boolean draggingBrowser;

    public MapScreen(Screen parent, String mapUrl, Text dimensionText, Text coordinatesText) {
        this(parent, mapUrl, dimensionText, coordinatesText, null);
    }

    public MapScreen(Screen parent, String mapUrl, Text dimensionText, Text coordinatesText, Text markerText) {
        super(Text.translatable("screen.dmcplus.map.title"));
        this.parent = parent;
        this.mapUrl = mapUrl;
        this.dimensionText = dimensionText;
        this.coordinatesText = coordinatesText;
        this.markerText = markerText;
    }

    @Override
    protected void init() {
        this.initializeBrowser();
    }

    private void initializeBrowser() {
        if (this.browser != null || !MCEF.isInitialized()) {
            return;
        }

        this.browser = MCEF.createBrowser(this.mapUrl, false);
        this.browser.useBrowserControls(false);
        this.browser.setCursorChangeListener(cursorType -> {
        });
        this.resizeBrowser();
    }

    private void resizeBrowser() {
        if (this.browser == null) {
            return;
        }

        int browserWidth = Math.max(1, this.browserViewportWidth() * this.scaleFactor());
        int browserHeight = Math.max(1, this.browserViewportHeight() * this.scaleFactor());
        this.browser.resize(browserWidth, browserHeight);
    }

    private int browserViewportX() {
        return FRAME_MARGIN;
    }

    private int browserViewportY() {
        return FRAME_MARGIN + HEADER_HEIGHT;
    }

    private int browserViewportWidth() {
        return this.width - FRAME_MARGIN * 2;
    }

    private int browserViewportHeight() {
        return this.height - FRAME_MARGIN * 2 - HEADER_HEIGHT;
    }

    private int scaleFactor() {
        MinecraftClient client = this.client != null ? this.client : MinecraftClient.getInstance();
        return Math.max(1, (int) client.getWindow().getScaleFactor());
    }

    private int browserMouseX(double mouseX) {
        return (int) Math.round((mouseX - this.browserViewportX()) * this.scaleFactor());
    }

    private int browserMouseY(double mouseY) {
        return (int) Math.round((mouseY - this.browserViewportY()) * this.scaleFactor());
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        this.resizeBrowser();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void removed() {
        if (this.browser != null) {
            this.browser.close();
            this.browser = null;
        }
        this.browserFocused = false;
        this.draggingBrowser = false;
        super.removed();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderInGameBackground(context);
        context.fill(0, 0, this.width, this.height, 0x7810161E);

        int frameX = FRAME_MARGIN;
        int frameY = FRAME_MARGIN;
        int frameWidth = this.width - FRAME_MARGIN * 2;
        int frameHeight = this.height - FRAME_MARGIN * 2;
        int browserX = this.browserViewportX();
        int browserY = this.browserViewportY();
        int browserWidth = this.browserViewportWidth();
        int browserHeight = this.browserViewportHeight();

        context.fill(frameX, frameY, frameX + frameWidth, frameY + frameHeight, 0xE0343A46);
        context.fill(frameX + 1, frameY + 1, frameX + frameWidth - 1, frameY + frameHeight - 1, 0xF0181D24);
        context.drawBorder(frameX, frameY, frameWidth, frameHeight, 0x808F9FB2);

        context.fill(frameX + 1, frameY + 1, frameX + frameWidth - 1, frameY + HEADER_HEIGHT, 0xFF2A313B);
        context.fill(browserX, browserY, browserX + browserWidth, browserY + browserHeight, 0xFF0A0C10);
        context.drawBorder(browserX - 1, browserY - 1, browserWidth + 2, browserHeight + 2, 0x66485262);

        context.drawTextWithShadow(this.textRenderer, this.title, frameX + 10, frameY + 10, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, this.dimensionText, frameX + 96, frameY + 10, 0xFFF2C94C);
        context.drawTextWithShadow(this.textRenderer, this.coordinatesText, frameX + frameWidth - 10 - this.textRenderer.getWidth(this.coordinatesText), frameY + 10, 0xFFD7E0EA);

        if (this.browser != null && this.browser.isTextureReady()) {
            Identifier texture = this.resolveBrowserTexture();
            if (texture != null) {
                context.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        texture,
                        browserX,
                        browserY,
                        0.0F,
                        0.0F,
                        browserWidth,
                        browserHeight,
                        browserWidth,
                        browserHeight
                );
            }
        } else {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.translatable("screen.dmcplus.map.loading"),
                    this.width / 2,
                    this.height / 2 - 8,
                    0xFFE5E7EB
            );
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.translatable("screen.dmcplus.map.loading_hint"),
                    this.width / 2,
                    this.height / 2 + 6,
                    0xFF9CA3AF
            );
        }

        if (this.markerText != null) {
            this.renderTargetMarker(context, browserX, browserY, browserWidth, browserHeight);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderTargetMarker(DrawContext context, int browserX, int browserY, int browserWidth, int browserHeight) {
        int centerX = browserX + browserWidth / 2;
        int centerY = browserY + browserHeight / 2;

        context.fill(centerX - 1, centerY - 9, centerX + 1, centerY + 10, 0xFFD64A3A);
        context.fill(centerX - 9, centerY - 1, centerX + 10, centerY + 1, 0xFFD64A3A);
        context.fill(centerX - 3, centerY - 3, centerX + 4, centerY + 4, 0xFFF3E6C8);

        int badgeWidth = this.textRenderer.getWidth(this.markerText) + 10;
        int badgeX = centerX - badgeWidth / 2;
        int badgeY = browserY + 10;
        context.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + 14, 0xE01E1E1E);
        context.drawBorder(badgeX, badgeY, badgeWidth, 14, 0xFF5C5C5C);
        context.drawCenteredTextWithShadow(this.textRenderer, this.markerText, centerX, badgeY + 3, 0xFFF2E6C7);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.browser != null && this.isInsideBrowser(mouseX, mouseY)) {
            this.browser.sendMousePress(this.browserMouseX(mouseX), this.browserMouseY(mouseY), button);
            this.browser.setFocus(true);
            this.browserFocused = true;
            this.draggingBrowser = true;
        } else {
            this.browserFocused = false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.browser != null && this.isInsideBrowser(mouseX, mouseY)) {
            this.browser.sendMouseRelease(this.browserMouseX(mouseX), this.browserMouseY(mouseY), button);
            this.browser.setFocus(true);
        }
        this.draggingBrowser = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (this.browser != null && (this.draggingBrowser || this.isInsideBrowser(mouseX, mouseY))) {
            this.browser.sendMouseMove(this.browserMouseX(mouseX), this.browserMouseY(mouseY));
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.browser != null && this.draggingBrowser) {
            this.browser.sendMouseMove(this.browserMouseX(mouseX), this.browserMouseY(mouseY));
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.browser != null && this.isInsideBrowser(mouseX, mouseY)) {
            this.browser.sendMouseWheel(this.browserMouseX(mouseX), this.browserMouseY(mouseY), verticalAmount, 0);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.browser != null && this.browserFocused && !this.isCloseKey(keyCode)) {
            this.browser.sendKeyPress(keyCode, scanCode, modifiers);
            this.browser.setFocus(true);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (this.browser != null && this.browserFocused && !this.isCloseKey(keyCode)) {
            this.browser.sendKeyRelease(keyCode, scanCode, modifiers);
            this.browser.setFocus(true);
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.browser != null && this.browserFocused && chr != 0) {
            this.browser.sendKeyTyped(chr, modifiers);
            this.browser.setFocus(true);
        }
        return super.charTyped(chr, modifiers);
    }

    private boolean isInsideBrowser(double mouseX, double mouseY) {
        return mouseX >= this.browserViewportX()
                && mouseX <= this.browserViewportX() + this.browserViewportWidth()
                && mouseY >= this.browserViewportY()
                && mouseY <= this.browserViewportY() + this.browserViewportHeight();
    }

    private boolean isCloseKey(int keyCode) {
        return this.client != null && this.client.options.inventoryKey.matchesKey(keyCode, 0);
    }

    private Identifier resolveBrowserTexture() {
        if (this.browser == null) {
            return null;
        }

        try {
            Method method = this.browser.getClass().getMethod("getTextureIdentifier");
            Object value = method.invoke(this.browser);
            if (value instanceof Identifier identifier) {
                return identifier;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Method method = this.browser.getClass().getMethod("getTextureLocation");
            Object value = method.invoke(this.browser);
            if (value instanceof Identifier identifier) {
                return identifier;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return null;
    }
}
