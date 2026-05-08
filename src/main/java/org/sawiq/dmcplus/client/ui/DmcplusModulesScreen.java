package org.sawiq.dmcplus.client.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.sawiq.dmcplus.client.DmcplusClient;

public class DmcplusModulesScreen extends Screen {

    private static final int PANEL_WIDTH = 390;
    private static final int CONTENT_PADDING = 16;
    private static final int BUTTON_HEIGHT = 20;
    private static final int CARD_GAP = 8;
    private static final int SECTION_LABEL_HEIGHT = 11;
    private static final int HEADER_HEIGHT = 32;
    private static final int FOOTER_HEIGHT = 42;

    private final Screen parent;

    public DmcplusModulesScreen(Screen parent) {
        super(Text.translatable("screen.dmcplus.modules.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ScreenSections panel = this.panel();
        ScreenSections content = this.content(panel);
        DmcplusClient client = DmcplusClient.getInstance();

        int columnGap = 10;
        int columnWidth = (content.width() - columnGap) / 2;
        int leftX = content.x();
        int rightX = content.x() + columnWidth + columnGap;
        int startY = content.y() + HEADER_HEIGHT;

        this.addModuleButton(leftX, startY + SECTION_LABEL_HEIGHT, columnWidth, Text.translatable("screen.dmcplus.modules.rules"),
                button -> client.getRulesFeature().open(this.client));
        this.addModuleButton(leftX, startY + SECTION_LABEL_HEIGHT + BUTTON_HEIGHT + CARD_GAP, columnWidth, Text.translatable("screen.dmcplus.modules.scan_qr"),
                button -> client.getQrScannerFeature().scanCurrentFrame(this.client));

        this.addModuleButton(rightX, startY + SECTION_LABEL_HEIGHT, columnWidth, Text.translatable("screen.dmcplus.modules.trade"),
                button -> client.getTradeFederationFeature().open(this.client));
        this.addModuleButton(rightX, startY + SECTION_LABEL_HEIGHT + BUTTON_HEIGHT + CARD_GAP, columnWidth, Text.translatable("screen.dmcplus.modules.clear_waypoint"),
                button -> client.getWaypointFeature().clear(this.client));

        int secondSectionY = startY + SECTION_LABEL_HEIGHT + (BUTTON_HEIGHT + CARD_GAP) * 2 + 15;
        this.addModuleButton(leftX, secondSectionY + SECTION_LABEL_HEIGHT, columnWidth, Text.translatable("screen.dmcplus.modules.toggle_branches"),
                button -> client.getBranchHudFeature().toggle(this.client));

        int staffY = secondSectionY + SECTION_LABEL_HEIGHT;
        if (client.getGuardCallFeature().isGuard(this.client)) {
            this.addModuleButton(rightX, staffY, columnWidth, Text.translatable(
                    client.getGuardCallFeature().isEnabled()
                            ? "screen.dmcplus.modules.guard_calls_on"
                            : "screen.dmcplus.modules.guard_calls_off"
            ), button -> {
                client.getGuardCallFeature().toggle(this.client);
                this.clearAndInit();
            });
            staffY += BUTTON_HEIGHT + CARD_GAP;
        }

        if (client.getAdminPanelFeature().hasAccess(this.client)) {
            this.addModuleButton(rightX, staffY, columnWidth, Text.translatable("screen.dmcplus.modules.admin"),
                    button -> client.getAdminPanelFeature().open(this.client));
        }

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.back"),
                button -> this.close()
        ).dimensions(panel.x() + (panel.width() - 100) / 2, panel.bottom() - 30, 100, BUTTON_HEIGHT).build());
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderInGameBackground(context);
        context.fillGradient(0, 0, this.width, this.height, 0xB0101010, 0xC0141414);

        ScreenSections panel = this.panel();
        ScreenSections content = this.content(panel);
        this.renderPanel(context, panel);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, content.y() + 8, 0xFFE8E8E8);

        int columnGap = 10;
        int columnWidth = (content.width() - columnGap) / 2;

        super.render(context, mouseX, mouseY, delta);
    }

    private void addModuleButton(int x, int y, int width, Text text, ButtonWidget.PressAction action) {
        this.addDrawableChild(ButtonWidget.builder(text, action).dimensions(x, y, width, BUTTON_HEIGHT).build());
    }

    private ScreenSections panel() {
        int panelHeight = CONTENT_PADDING * 2 + HEADER_HEIGHT + SECTION_LABEL_HEIGHT * 2 + (BUTTON_HEIGHT + CARD_GAP) * 3 + 15 + FOOTER_HEIGHT;
        return new ScreenSections((this.width - PANEL_WIDTH) / 2, (this.height - panelHeight) / 2 - 4, PANEL_WIDTH, panelHeight);
    }

    private ScreenSections content(ScreenSections panel) {
        return new ScreenSections(
                panel.x() + CONTENT_PADDING,
                panel.y() + CONTENT_PADDING,
                panel.width() - CONTENT_PADDING * 2,
                panel.height() - CONTENT_PADDING * 2
        );
    }

    private void renderPanel(DrawContext context, ScreenSections panel) {
        context.fill(panel.x() + 2, panel.y() + 2, panel.right() + 2, panel.bottom() + 2, 0x66000000);
        context.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), 0xF01A1A1A);
        context.fill(panel.x() + 2, panel.y() + 2, panel.right() - 2, panel.bottom() - 2, 0xF0262626);
        context.drawBorder(panel.x(), panel.y(), panel.width(), panel.height(), 0xFF5C5C5C);
        context.drawBorder(panel.x() + 1, panel.y() + 1, panel.width() - 2, panel.height() - 2, 0xFF111111);
    }
}
