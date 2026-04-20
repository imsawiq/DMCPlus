package org.sawiq.dmcplus.client.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.sawiq.dmcplus.client.DmcplusClient;

public class DmcplusModulesScreen extends Screen {

    private static final int PANEL_WIDTH = 260;
    private static final int CONTENT_PADDING = 16;
    private static final int BUTTON_WIDTH = 190;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_STEP = 24;
    private static final int HEADER_HEIGHT = 28;
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
        int buttonX = panel.x() + (panel.width() - BUTTON_WIDTH) / 2;
        int firstRowY = content.y() + HEADER_HEIGHT;
        int row = 0;
        DmcplusClient client = DmcplusClient.getInstance();

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.dmcplus.modules.trade"),
                button -> client.getTradeFederationFeature().open(this.client)
        ).dimensions(buttonX, firstRowY + ROW_STEP * row++, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.dmcplus.modules.scan_qr"),
                button -> client.getQrScannerFeature().scanCurrentFrame(this.client)
        ).dimensions(buttonX, firstRowY + ROW_STEP * row++, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.dmcplus.modules.toggle_branches"),
                button -> client.getBranchHudFeature().toggle(this.client)
        ).dimensions(buttonX, firstRowY + ROW_STEP * row++, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        if (client.getGuardCallFeature().isGuard(this.client)) {
            this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable(
                            client.getGuardCallFeature().isEnabled()
                                    ? "screen.dmcplus.modules.guard_calls_on"
                                    : "screen.dmcplus.modules.guard_calls_off"
                    ),
                    button -> {
                        client.getGuardCallFeature().toggle(this.client);
                        this.clearAndInit();
                    }
            ).dimensions(buttonX, firstRowY + ROW_STEP * row++, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.dmcplus.modules.clear_waypoint"),
                button -> client.getWaypointFeature().clear(this.client)
        ).dimensions(buttonX, firstRowY + ROW_STEP * row, BUTTON_WIDTH, BUTTON_HEIGHT).build());

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

        context.fill(panel.x() + 2, panel.y() + 2, panel.right() + 2, panel.bottom() + 2, 0x66000000);
        context.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), 0xF01A1A1A);
        context.fill(panel.x() + 2, panel.y() + 2, panel.right() - 2, panel.bottom() - 2, 0xF0262626);
        context.drawBorder(panel.x(), panel.y(), panel.width(), panel.height(), 0xFF5C5C5C);
        context.drawBorder(panel.x() + 1, panel.y() + 1, panel.width() - 2, panel.height() - 2, 0xFF111111);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, content.y() + 8, 0xFFE8E8E8);

        super.render(context, mouseX, mouseY, delta);
    }

    private ScreenSections panel() {
        int rows = this.moduleRows();
        int panelHeight = CONTENT_PADDING * 2 + HEADER_HEIGHT + rows * ROW_STEP + FOOTER_HEIGHT;
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

    private int moduleRows() {
        int rows = 4;
        DmcplusClient client = DmcplusClient.getInstance();
        if (client != null && client.getGuardCallFeature().isGuard(this.client)) {
            rows++;
        }
        return rows;
    }
}
