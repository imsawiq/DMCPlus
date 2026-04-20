package org.sawiq.dmcplus.client.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.sawiq.dmcplus.client.DmcplusClient;
import org.sawiq.dmcplus.client.feature.trade.ContainerSlotCount;
import org.sawiq.dmcplus.client.feature.trade.PriceQuote;
import org.sawiq.dmcplus.client.feature.trade.TradeFederationFeature;
import org.sawiq.dmcplus.client.feature.trade.TradeListing;

import java.util.List;
import java.util.Optional;

public class TradeFederationScreen extends Screen {

    private static final int PANEL_MAX_WIDTH = 430;
    private static final int PANEL_HEIGHT = 280;
    private static final int CONTENT_PADDING = 14;
    private static final int RESULT_ROW_HEIGHT = 24;
    private static final int MARKER_BUTTON_WIDTH = 52;

    private final Screen parent;
    private final TradeFederationFeature feature;

    private TextFieldWidget searchField;
    private TextFieldWidget slotsField;
    private List<TradeListing> currentResults = List.of();
    private TradeListing countedListing;
    private ContainerSlotCount countedSlots;
    private String countStatus = "";
    private int scrollOffset;

    public TradeFederationScreen(Screen parent, TradeFederationFeature feature) {
        super(Text.translatable("screen.dmcplus.trade.title"));
        this.parent = parent;
        this.feature = feature;
    }

    @Override
    protected void init() {
        ScreenSections panel = this.panel();
        ScreenSections content = this.content(panel);
        ScreenSections.Row form = this.formRow(content);

        int scanWidth = 72;
        int slotsWidth = 62;
        int gap = 6;
        int searchWidth = form.width() - scanWidth - slotsWidth - gap * 2;

        this.searchField = this.addDrawableChild(new TextFieldWidget(
                this.textRenderer,
                form.x(),
                form.y() + 12,
                searchWidth,
                18,
                Text.translatable("screen.dmcplus.trade.search")
        ));
        this.searchField.setMaxLength(60);
        this.searchField.setPlaceholder(Text.translatable("screen.dmcplus.trade.search_placeholder"));

        this.slotsField = this.addDrawableChild(new TextFieldWidget(
                this.textRenderer,
                form.x() + searchWidth + gap,
                form.y() + 12,
                slotsWidth,
                18,
                Text.translatable("screen.dmcplus.trade.slots")
        ));
        this.slotsField.setMaxLength(10);
        this.slotsField.setPlaceholder(Text.translatable("screen.dmcplus.trade.slots_placeholder"));

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.dmcplus.trade.scan"),
                button -> this.feature.scanNearby(this.client)
        ).dimensions(form.right() - scanWidth, form.y() + 11, scanWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.back"),
                button -> this.close()
        ).dimensions(panel.right() - 88, panel.bottom() - 28, 74, 20).build());
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
        int calcY = this.calculatorY(panel);

        this.renderPanel(context, panel);
        context.fill(content.x(), calcY - 10, content.right(), calcY - 9, 0xFF4B4B4B);

        context.drawTextWithShadow(this.textRenderer, this.title, content.x(), panel.y() + 12, 0xFFE8E8E8);
        context.drawTextWithShadow(
                this.textRenderer,
                Text.literal(this.feature.getLastStatus()),
                content.x(),
                panel.y() + 64,
                0xFFB9B9B9
        );

        ScreenSections.Row form = this.formRow(content);
        int scanWidth = 72;
        int slotsWidth = 62;
        int gap = 6;
        int searchWidth = form.width() - scanWidth - slotsWidth - gap * 2;
        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.dmcplus.trade.search"), form.x(), form.y(), 0xFFCFCFCF);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.dmcplus.trade.slots"), form.x() + searchWidth + gap, form.y(), 0xFFCFCFCF);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.dmcplus.trade.results"), content.x(), panel.y() + 82, 0xFFE8E8E8);
        context.drawTextWithShadow(
                this.textRenderer,
                Text.translatable("screen.dmcplus.trade.open_hint"),
                content.x(),
                panel.y() + 94,
                0xFF9A9A9A
        );

        this.currentResults = this.feature.search(this.client, this.searchField != null ? this.searchField.getText() : "");
        this.clampScrollOffset();
        double requestedSlots = this.parseSlots();

        int startY = this.resultsStartY(panel);
        int visibleCount = Math.min(this.currentResults.size(), this.visibleResultCount(panel));
        int hoveredRow = this.getHoveredResultIndex(mouseX, mouseY, content, startY, visibleCount);
        for (int index = 0; index < visibleCount; index++) {
            this.renderResultRow(context, content, startY, index + this.scrollOffset, hoveredRow, requestedSlots);
        }

        if (this.currentResults.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.translatable("screen.dmcplus.trade.no_results"),
                    this.width / 2,
                    panel.y() + 148,
                    0xFFBEBEBE
            );
        } else if (this.currentResults.size() > visibleCount) {
            context.drawTextWithShadow(
                    this.textRenderer,
                    Text.translatable("screen.dmcplus.trade.more_results", this.scrollOffset + 1, Math.min(this.scrollOffset + visibleCount, this.currentResults.size()), this.currentResults.size()),
                    content.x(),
                    calcY - 22,
                    0xFF9A9A9A
            );
        }

        this.renderCalculator(context, content, calcY, requestedSlots);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button != 0 || this.client == null || this.currentResults.isEmpty()) {
            return false;
        }

        ScreenSections panel = this.panel();
        ScreenSections content = this.content(panel);
        int startY = this.resultsStartY(panel);
        int visibleCount = Math.min(this.currentResults.size(), this.visibleResultCount(panel));
        int hoveredRow = this.getActionButtonIndex((int) mouseX, (int) mouseY, content, startY, visibleCount, true);
        if (hoveredRow < 0) {
            return false;
        }

        TradeListing listing = this.currentResults.get(hoveredRow + this.scrollOffset);
        DmcplusClient.getInstance().getWaypointFeature().setTradeTarget(this.client, listing);
        this.client.setScreen(null);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.currentResults.size() <= this.visibleResultCount(this.panel())) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        this.scrollOffset += verticalAmount < 0.0D ? 1 : -1;
        this.clampScrollOffset();
        return true;
    }

    private double parseSlots() {
        if (this.slotsField == null) {
            return 0.0D;
        }

        String text = this.slotsField.getText().trim().replace(',', '.');
        if (text.isEmpty()) {
            return 0.0D;
        }

        try {
            return MathHelper.clamp(Double.parseDouble(text), 0.0D, 9999.0D);
        } catch (NumberFormatException exception) {
            return 0.0D;
        }
    }

    private String buildPriceText(TradeListing listing, double requestedSlots) {
        PriceQuote quote = listing.quote();
        if (quote == null) {
            return Text.translatable("screen.dmcplus.trade.no_price_short").getString();
        }

        if (requestedSlots <= 0.0D) {
            return quote.sourceText();
        }

        return quote.sourceText() + "  ->  " + quote.formatCost(requestedSlots);
    }

    private void renderCalculator(DrawContext context, ScreenSections content, int y, double requestedSlots) {
        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.dmcplus.trade.calculator"), content.x(), y, 0xFFE8E8E8);

        TradeListing listing = this.countedListing != null ? this.countedListing : (this.currentResults.isEmpty() ? null : this.currentResults.getFirst());
        String text;
        int color;
        if (listing == null) {
            text = Text.translatable("screen.dmcplus.trade.calc_no_item").getString();
            color = 0xFF9A9A9A;
        } else if (listing.quote() == null) {
            text = Text.translatable("screen.dmcplus.trade.calc_no_price").getString();
            color = 0xFFD08A72;
        } else if (this.countedSlots != null && this.countedListing == listing) {
            text = Text.translatable(
                    "screen.dmcplus.trade.calc_barrel_result",
                    listing.productName(),
                    this.countedSlots.matchingSlots(),
                    listing.quote().formatCost(this.countedSlots.matchingSlots()),
                    this.countedSlots.occupiedSlots(),
                    this.countedSlots.containerSlots()
            ).getString();
            color = 0xFFF2D085;
        } else if (!this.countStatus.isEmpty()) {
            text = this.countStatus;
            color = 0xFFD08A72;
        } else if (requestedSlots <= 0.0D) {
            text = Text.translatable("screen.dmcplus.trade.calc_enter_slots", listing.quote().sourceText()).getString();
            color = 0xFFBEBEBE;
        } else {
            text = Text.translatable(
                    "screen.dmcplus.trade.calc_result",
                    requestedSlots,
                    listing.quote().formatCost(requestedSlots),
                    listing.quote().sourceText()
            ).getString();
            color = 0xFFF2D085;
        }

        text = this.textRenderer.trimToWidth(text, content.width());
        context.drawTextWithShadow(this.textRenderer, text, content.x(), y + 14, color);
    }

    private void renderResultRow(DrawContext context, ScreenSections content, int startY, int index, int hoveredRow, double requestedSlots) {
        TradeListing listing = this.currentResults.get(index);
        int visibleIndex = index - this.scrollOffset;
        int rowY = startY + visibleIndex * RESULT_ROW_HEIGHT;
        boolean hovered = visibleIndex == hoveredRow;
        int rowColor = hovered ? 0xAA3A3327 : ((index % 2 == 0) ? 0x70313131 : 0x70373737);
        context.fill(content.x(), rowY - 2, content.right(), rowY + 20, rowColor);
        context.drawBorder(content.x(), rowY - 2, content.width(), 22, hovered ? 0xFF8D7A58 : 0xFF1A1A1A);

        int markerX = content.right() - MARKER_BUTTON_WIDTH - 8;
        int priceRight = markerX - 8;
        int productWidth = Math.max(80, priceRight - content.x() - 12);

        String coords = listing.position().toShortString();
        String product = this.textRenderer.trimToWidth(listing.productName(), productWidth);
        context.drawText(this.textRenderer, product, content.x() + 6, rowY, hovered ? 0xFFF2EEE4 : 0xFFE6E6E6, false);
        context.drawText(this.textRenderer, coords, content.x() + 6, rowY + 10, 0xFFAAAAAA, false);

        String right = this.textRenderer.trimToWidth(this.buildPriceText(listing, requestedSlots), Math.max(60, priceRight - content.x() - productWidth - 16));
        context.drawText(this.textRenderer, right, priceRight - this.textRenderer.getWidth(right), rowY, hovered ? 0xFFE5D28A : 0xFFD5B85C, false);

        this.renderSmallButton(context, markerX, rowY + 1, MARKER_BUTTON_WIDTH, Text.translatable("screen.dmcplus.trade.marker_button"), hovered);
    }

    private void renderSmallButton(DrawContext context, int x, int y, int width, Text text, boolean hovered) {
        int color = hovered ? 0xFF605742 : 0xFF4B4435;
        context.fill(x, y, x + width, y + 16, color);
        context.drawBorder(x, y, width, 16, 0xFF1A1A1A);
        context.drawCenteredTextWithShadow(this.textRenderer, text, x + width / 2, y + 4, 0xFFF1E7C9);
    }

    private int getHoveredResultIndex(int mouseX, int mouseY, ScreenSections content, int startY, int visibleCount) {
        if (mouseX < content.x() || mouseX > content.right()) {
            return -1;
        }

        for (int index = 0; index < visibleCount; index++) {
            int rowY = startY + index * RESULT_ROW_HEIGHT;
            if (mouseY >= rowY - 2 && mouseY <= rowY + 18) {
                return index;
            }
        }

        return -1;
    }

    private int getActionButtonIndex(int mouseX, int mouseY, ScreenSections content, int startY, int visibleCount, boolean marker) {
        int left = content.right() - MARKER_BUTTON_WIDTH - 8;
        int right = left + MARKER_BUTTON_WIDTH;
        if (mouseX < left || mouseX > right) {
            return -1;
        }

        for (int index = 0; index < visibleCount; index++) {
            int rowY = startY + index * RESULT_ROW_HEIGHT;
            if (mouseY >= rowY && mouseY <= rowY + 16) {
                return index;
            }
        }

        return -1;
    }

    private void countSlotsFor(TradeListing listing) {
        this.countedListing = listing;
        Optional<ContainerSlotCount> count = this.feature.countOpenContainerSlots(this.client, listing);
        if (count.isEmpty()) {
            this.countedSlots = null;
            this.countStatus = Text.translatable("screen.dmcplus.trade.calc_open_barrel").getString();
            return;
        }

        this.countedSlots = count.get();
        this.countStatus = "";
    }

    private ScreenSections panel() {
        int panelWidth = MathHelper.clamp(this.width - 24, 320, PANEL_MAX_WIDTH);
        int panelHeight = MathHelper.clamp(this.height - 28, 230, PANEL_HEIGHT);
        return new ScreenSections((this.width - panelWidth) / 2, (this.height - panelHeight) / 2 - 4, panelWidth, panelHeight);
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
        context.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), 0xF0191919);
        context.fill(panel.x() + 2, panel.y() + 2, panel.right() - 2, panel.bottom() - 2, 0xF0262626);
        context.drawBorder(panel.x(), panel.y(), panel.width(), panel.height(), 0xFF5C5C5C);
        context.drawBorder(panel.x() + 1, panel.y() + 1, panel.width() - 2, panel.height() - 2, 0xFF111111);
    }

    private ScreenSections.Row formRow(ScreenSections content) {
        return content.row(32, 32);
    }

    private int resultsStartY(ScreenSections panel) {
        return panel.y() + 110;
    }

    private int calculatorY(ScreenSections panel) {
        return panel.bottom() - 48;
    }

    private int visibleResultCount(ScreenSections panel) {
        int availableHeight = this.calculatorY(panel) - 12 - this.resultsStartY(panel);
        return Math.max(1, availableHeight / RESULT_ROW_HEIGHT);
    }

    private void clampScrollOffset() {
        int maxOffset = Math.max(0, this.currentResults.size() - this.visibleResultCount(this.panel()));
        this.scrollOffset = MathHelper.clamp(this.scrollOffset, 0, maxOffset);
    }
}
