package org.sawiq.dmcplus.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.sawiq.dmcplus.client.feature.rules.RulesCategory;
import org.sawiq.dmcplus.client.feature.rules.RulesFeature;
import org.sawiq.dmcplus.client.feature.rules.RulesPage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RulesScreen extends Screen {

    private static final int PANEL_MAX_WIDTH = 720;
    private static final int PANEL_MAX_HEIGHT = 420;
    private static final int CONTENT_PADDING = 14;
    private static final int TAB_HEIGHT = 20;
    private static final int PAGE_ROW_HEIGHT = 28;

    private final Screen parent;
    private final RulesFeature feature;
    private List<RulesCategory> categories = List.of();
    private List<MarkdownLine> wrappedLines = List.of();
    private TextFieldWidget searchField;
    private String searchQuery = "";
    private int selectedCategory;
    private int selectedPage;
    private int categoryScroll;
    private int pageScroll;
    private int contentScroll;
    private String status = "Загружаю законодательную базу...";
    private RulesPage loadedPage;

    public RulesScreen(Screen parent, RulesFeature feature) {
        super(Text.translatable("screen.dmcplus.rules.title"));
        this.parent = parent;
        this.feature = feature;
    }

    @Override
    protected void init() {
        ScreenSections panel = this.panel();
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.back"),
                button -> this.close()
        ).dimensions(panel.right() - 88, panel.bottom() - 28, 74, 20).build());
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.dmcplus.rules.reload"),
                button -> this.loadSelectedPage(true)
        ).dimensions(panel.right() - 170, panel.bottom() - 28, 76, 20).build());

        int searchWidth = Math.min(190, panel.width() / 3);
        this.searchField = this.addDrawableChild(new TextFieldWidget(
                this.textRenderer,
                panel.right() - CONTENT_PADDING - searchWidth,
                panel.y() + 8,
                searchWidth,
                18,
                Text.translatable("screen.dmcplus.rules.search")
        ));
        this.searchField.setPlaceholder(Text.translatable("screen.dmcplus.rules.search_placeholder"));
        this.searchField.setMaxLength(60);
        this.searchField.setText(this.searchQuery);
        this.searchField.setChangedListener(text -> {
            this.searchQuery = text.trim();
            this.jumpToSearchResult();
        });

        this.feature.loadCategories().thenAccept(loaded -> MinecraftClient.getInstance().execute(() -> {
            this.categories = loaded;
            if (this.categories.isEmpty()) {
                this.status = "Не удалось загрузить список документов.";
                return;
            }
            this.selectedCategory = MathHelper.clamp(this.selectedCategory, 0, this.categories.size() - 1);
            this.selectedPage = MathHelper.clamp(this.selectedPage, 0, this.currentPages().size() - 1);
            this.loadSelectedPage(false);
        }));
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
        context.drawTextWithShadow(this.textRenderer, this.title, content.x(), panel.y() + 10, 0xFFE8E8E8);

        int tabsY = panel.y() + 32;
        int bodyY = tabsY + TAB_HEIGHT + 8;
        int pagesWidth = MathHelper.clamp(content.width() / 3, 142, 190);
        int pageListBottom = panel.bottom() - 52;
        int articleX = content.x() + pagesWidth + 10;
        int articleWidth = content.right() - articleX;

        this.renderTabs(context, content.x(), tabsY, content.width(), mouseX, mouseY);
        context.fill(content.x(), bodyY - 4, content.right(), bodyY - 3, 0xFF4B4B4B);
        this.renderPageList(context, content.x(), bodyY, pagesWidth, pageListBottom, mouseX, mouseY);
        context.fill(articleX - 6, bodyY, articleX - 5, pageListBottom, 0xFF3A3A3A);
        this.renderArticle(context, articleX, bodyY, articleWidth, pageListBottom);
        context.drawTextWithShadow(
                this.textRenderer,
                Text.translatable("screen.dmcplus.rules.gitbook_notice"),
                content.x(),
                panel.bottom() - 25,
                0xFF9A9A9A
        );

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0 || this.categories.isEmpty()) {
            return false;
        }

        ScreenSections panel = this.panel();
        ScreenSections content = this.content(panel);
        int tabsY = panel.y() + 32;
        int bodyY = tabsY + TAB_HEIGHT + 8;
        int pagesWidth = MathHelper.clamp(content.width() / 3, 142, 190);
        int pageListBottom = panel.bottom() - 52;

        int tabIndex = this.hitTab((int) mouseX, (int) mouseY, content.x(), tabsY, content.width());
        if (tabIndex >= 0) {
            this.selectedCategory = tabIndex + this.categoryScroll;
            this.selectedPage = 0;
            this.pageScroll = 0;
            this.contentScroll = 0;
            this.loadSelectedPage(false);
            return true;
        }

        int pageIndex = this.hitPage((int) mouseX, (int) mouseY, content.x(), bodyY, pagesWidth, pageListBottom);
        if (pageIndex >= 0) {
            this.selectedPage = pageIndex + this.pageScroll;
            this.contentScroll = 0;
            this.loadSelectedPage(false);
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        ScreenSections panel = this.panel();
        ScreenSections content = this.content(panel);
        int tabsY = panel.y() + 32;
        int bodyY = tabsY + TAB_HEIGHT + 8;
        int pagesWidth = MathHelper.clamp(content.width() / 3, 142, 190);
        int pageListBottom = panel.bottom() - 52;

        if (mouseY >= tabsY && mouseY <= tabsY + TAB_HEIGHT) {
            this.categoryScroll += verticalAmount < 0.0D ? 1 : -1;
            this.clampScrolls();
            return true;
        }

        if (mouseX < content.x() + pagesWidth && mouseY >= bodyY && mouseY <= pageListBottom) {
            this.pageScroll += verticalAmount < 0.0D ? 1 : -1;
            this.clampScrolls();
            return true;
        }

        this.contentScroll += verticalAmount < 0.0D ? 3 : -3;
        this.clampScrolls();
        return true;
    }

    private void loadSelectedPage(boolean forceReload) {
        List<RulesPage> pages = this.currentPages();
        if (pages.isEmpty()) {
            this.status = "В разделе нет страниц.";
            this.wrappedLines = List.of();
            return;
        }

        RulesPage page = pages.get(this.selectedPage);
        this.loadedPage = page;
        this.status = "Загружаю: " + page.title();
        this.wrappedLines = List.of();
        (forceReload ? this.feature.reloadPage(page) : this.feature.loadPage(page)).thenAccept(text -> MinecraftClient.getInstance().execute(() -> {
            if (this.loadedPage != page) {
                return;
            }
            this.status = "";
            this.wrapContent(text);
        })).exceptionally(throwable -> {
            MinecraftClient.getInstance().execute(() -> {
                if (this.loadedPage == page) {
                    this.status = "Не удалось загрузить страницу. Нажми \"Обновить\", чтобы попробовать снова.";
                    this.wrappedLines = List.of();
                }
            });
            return null;
        });
    }

    private void wrapContent(String text) {
        ScreenSections panel = this.panel();
        ScreenSections content = this.content(panel);
        int pagesWidth = MathHelper.clamp(content.width() / 3, 142, 190);
        int articleWidth = content.right() - (content.x() + pagesWidth + 10);
        this.wrappedLines = this.wrapMarkdown(text, articleWidth);
        this.jumpToSearchResult();
        this.clampScrolls();
    }

    private List<MarkdownLine> wrapMarkdown(String text, int width) {
        ArrayList<MarkdownLine> lines = new ArrayList<>();
        for (String rawLine : text.split("\\n", -1)) {
            MarkdownStyle style = this.styleFor(rawLine);
            String line = style.text();
            if (line.isBlank()) {
                lines.add(new MarkdownLine("", 0xFFE2E2E2, 0, false));
                continue;
            }

            int wrapWidth = Math.max(40, width - style.indent());
            for (String wrapped : this.wrapPlainLine(line, wrapWidth)) {
                lines.add(new MarkdownLine(wrapped, style.color(), style.indent(), style.block()));
            }
        }
        return List.copyOf(lines);
    }

    private MarkdownStyle styleFor(String rawLine) {
        String line = rawLine.stripLeading();
        if (line.startsWith("#### ")) {
            return new MarkdownStyle(line.substring(5), 0xFFE9CB82, 0, false);
        }
        if (line.startsWith("### ")) {
            return new MarkdownStyle(line.substring(4), 0xFFF2D085, 0, false);
        }
        if (line.startsWith("## ")) {
            return new MarkdownStyle(line.substring(3), 0xFFFFD98A, 0, false);
        }
        if (line.startsWith("# ")) {
            return new MarkdownStyle(line.substring(2), 0xFFFFE3A6, 0, false);
        }
        if (line.startsWith(">")) {
            return new MarkdownStyle(line.replaceFirst("^>\\s?", ""), 0xFFB9C7E6, 14, true);
        }
        if (line.startsWith("- ")) {
            return new MarkdownStyle("• " + line.substring(2), 0xFFE2E2E2, 12, false);
        }
        return new MarkdownStyle(rawLine, 0xFFE2E2E2, 0, false);
    }

    private List<String> wrapPlainLine(String line, int width) {
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : line.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (this.textRenderer.getWidth(candidate) <= width || current.isEmpty()) {
                current.setLength(0);
                current.append(candidate);
            } else {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines.isEmpty() ? List.of("") : lines;
    }

    private void jumpToSearchResult() {
        if (this.searchQuery.isBlank()) {
            return;
        }

        String needle = this.searchQuery.toLowerCase(Locale.ROOT);
        for (int index = 0; index < this.wrappedLines.size(); index++) {
            if (this.wrappedLines.get(index).text().toLowerCase(Locale.ROOT).contains(needle)) {
                this.contentScroll = Math.max(0, index - 2);
                this.clampScrolls();
                return;
            }
        }
    }

    private void renderTabs(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
        int visible = this.visibleTabs(width);
        for (int index = 0; index < visible; index++) {
            int categoryIndex = index + this.categoryScroll;
            if (categoryIndex >= this.categories.size()) {
                break;
            }
            int tabX = x + index * this.tabWidth(width);
            int tabWidth = this.tabWidth(width) - 4;
            boolean selected = categoryIndex == this.selectedCategory;
            boolean hovered = mouseX >= tabX && mouseX <= tabX + tabWidth && mouseY >= y && mouseY <= y + TAB_HEIGHT;
            context.fill(tabX, y, tabX + tabWidth, y + TAB_HEIGHT, selected ? 0xFF4A412F : (hovered ? 0xFF3B3B3B : 0xFF2B2B2B));
            context.drawBorder(tabX, y, tabWidth, TAB_HEIGHT, selected ? 0xFFD5B85C : 0xFF171717);
            String title = this.textRenderer.trimToWidth(this.categories.get(categoryIndex).title(), tabWidth - 8);
            context.drawCenteredTextWithShadow(this.textRenderer, title, tabX + tabWidth / 2, y + 6, selected ? 0xFFF2D085 : 0xFFE0E0E0);
        }
    }

    private void renderPageList(DrawContext context, int x, int y, int width, int bottom, int mouseX, int mouseY) {
        List<RulesPage> pages = this.currentPages();
        int visible = Math.min(this.visiblePages(y, bottom), Math.max(0, pages.size() - this.pageScroll));
        for (int index = 0; index < visible; index++) {
            int pageIndex = index + this.pageScroll;
            RulesPage page = pages.get(pageIndex);
            int rowY = y + index * PAGE_ROW_HEIGHT;
            boolean selected = pageIndex == this.selectedPage;
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + PAGE_ROW_HEIGHT - 4;
            context.fill(x, rowY, x + width, rowY + PAGE_ROW_HEIGHT - 4, selected ? 0xAA4A412F : (hovered ? 0x70404040 : 0x70303030));
            context.drawBorder(x, rowY, width, PAGE_ROW_HEIGHT - 4, selected ? 0xFFD5B85C : 0xFF171717);
            context.drawText(this.textRenderer, this.textRenderer.trimToWidth(page.title(), width - 10), x + 5, rowY + 4, 0xFFE8E8E8, false);
            if (!page.description().isBlank()) {
                context.drawText(this.textRenderer, this.textRenderer.trimToWidth(page.description(), width - 10), x + 5, rowY + 14, 0xFFAAAAAA, false);
            }
        }
    }

    private void renderArticle(DrawContext context, int x, int y, int width, int bottom) {
        RulesPage page = this.selectedPageObject();
        if (page != null) {
            context.drawTextWithShadow(this.textRenderer, page.title(), x, y, 0xFFF2D085);
        }
        if (!this.status.isBlank()) {
            context.drawTextWithShadow(this.textRenderer, this.status, x, y + 18, 0xFFBEBEBE);
            return;
        }

        int lineY = y + 18;
        int maxLines = Math.max(1, (bottom - lineY) / 10);
        for (int index = 0; index < maxLines; index++) {
            int lineIndex = index + this.contentScroll;
            if (lineIndex >= this.wrappedLines.size()) {
                break;
            }
            MarkdownLine line = this.wrappedLines.get(lineIndex);
            int textX = x + line.indent();
            int textY = lineY + index * 10;
            if (line.block()) {
                context.fill(x + 3, textY - 1, x + 6, textY + 9, 0xFF5A6F9E);
                context.fill(x + 8, textY - 1, x + width, textY + 9, 0x332F4778);
            }
            this.renderSearchHighlights(context, line.text(), textX, textY);
            context.drawText(this.textRenderer, line.text(), textX, textY, line.color(), false);
        }

        if (this.wrappedLines.size() > maxLines) {
            String scrollText = (this.contentScroll + 1) + "-" + Math.min(this.contentScroll + maxLines, this.wrappedLines.size()) + " / " + this.wrappedLines.size();
            context.drawTextWithShadow(this.textRenderer, scrollText, x + width - this.textRenderer.getWidth(scrollText), bottom + 4, 0xFF9A9A9A);
        }
    }

    private int hitTab(int mouseX, int mouseY, int x, int y, int width) {
        if (mouseY < y || mouseY > y + TAB_HEIGHT) {
            return -1;
        }
        int tabWidth = this.tabWidth(width);
        int index = (mouseX - x) / tabWidth;
        return index >= 0 && index < this.visibleTabs(width) && index + this.categoryScroll < this.categories.size() ? index : -1;
    }

    private int hitPage(int mouseX, int mouseY, int x, int y, int width, int bottom) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > bottom) {
            return -1;
        }
        int index = (mouseY - y) / PAGE_ROW_HEIGHT;
        return index >= 0 && index < this.visiblePages(y, bottom) && index + this.pageScroll < this.currentPages().size() ? index : -1;
    }

    private List<RulesPage> currentPages() {
        if (this.categories.isEmpty() || this.selectedCategory >= this.categories.size()) {
            return List.of();
        }
        return this.categories.get(this.selectedCategory).pages();
    }

    private RulesPage selectedPageObject() {
        List<RulesPage> pages = this.currentPages();
        return pages.isEmpty() || this.selectedPage >= pages.size() ? null : pages.get(this.selectedPage);
    }

    private int visibleTabs(int width) {
        return Math.max(1, width / this.tabWidth(width));
    }

    private int tabWidth(int width) {
        return MathHelper.clamp(width / 4, 118, 168);
    }

    private int visiblePages(int y, int bottom) {
        return Math.max(1, (bottom - y) / PAGE_ROW_HEIGHT);
    }

    private void clampScrolls() {
        ScreenSections panel = this.panel();
        ScreenSections content = this.content(panel);
        int tabsY = panel.y() + 32;
        int bodyY = tabsY + TAB_HEIGHT + 8;
        int pageListBottom = panel.bottom() - 52;
        int articleStart = bodyY + 18;
        int articleLines = Math.max(1, (pageListBottom - articleStart) / 10);

        this.categoryScroll = MathHelper.clamp(this.categoryScroll, 0, Math.max(0, this.categories.size() - this.visibleTabs(content.width())));
        this.selectedCategory = MathHelper.clamp(this.selectedCategory, 0, Math.max(0, this.categories.size() - 1));
        this.pageScroll = MathHelper.clamp(this.pageScroll, 0, Math.max(0, this.currentPages().size() - this.visiblePages(bodyY, pageListBottom)));
        this.selectedPage = MathHelper.clamp(this.selectedPage, 0, Math.max(0, this.currentPages().size() - 1));
        this.contentScroll = MathHelper.clamp(this.contentScroll, 0, Math.max(0, this.wrappedLines.size() - articleLines));
    }

    private ScreenSections panel() {
        int panelWidth = MathHelper.clamp(this.width - 24, 420, PANEL_MAX_WIDTH);
        int panelHeight = MathHelper.clamp(this.height - 28, 260, PANEL_MAX_HEIGHT);
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

    private void renderSearchHighlights(DrawContext context, String line, int x, int y) {
        if (this.searchQuery.isBlank()) {
            return;
        }

        String lowerLine = line.toLowerCase(Locale.ROOT);
        String lowerQuery = this.searchQuery.toLowerCase(Locale.ROOT);
        int from = 0;
        while (from < lowerLine.length()) {
            int index = lowerLine.indexOf(lowerQuery, from);
            if (index < 0) {
                return;
            }

            int startX = x + this.textRenderer.getWidth(line.substring(0, index));
            int endX = startX + this.textRenderer.getWidth(line.substring(index, Math.min(line.length(), index + this.searchQuery.length())));
            context.fill(startX - 1, y - 1, endX + 1, y + 9, 0xAA8F6A16);
            from = index + Math.max(1, lowerQuery.length());
        }
    }

    private record MarkdownLine(String text, int color, int indent, boolean block) {
    }

    private record MarkdownStyle(String text, int color, int indent, boolean block) {
    }
}
