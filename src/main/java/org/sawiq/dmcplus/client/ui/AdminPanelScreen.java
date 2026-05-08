package org.sawiq.dmcplus.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.sawiq.dmcplus.client.feature.admin.AdminAction;
import org.sawiq.dmcplus.client.feature.admin.AdminPanelFeature;

import java.util.EnumMap;
import java.util.Map;

public class AdminPanelScreen extends Screen {

    private static final int PANEL_MAX_WIDTH = 560;
    private static final int PANEL_HEIGHT = 318;
    private static final int CONTENT_PADDING = 14;
    private static final int FIELD_HEIGHT = 18;
    private static final int ACTION_ROW_HEIGHT = 28;

    private final Screen parent;
    private final AdminPanelFeature feature;
    private final Map<AdminAction, TextFieldWidget> templateFields = new EnumMap<>(AdminAction.class);
    private TextFieldWidget targetField;
    private TextFieldWidget durationField;
    private TextFieldWidget reasonField;

    public AdminPanelScreen(Screen parent, AdminPanelFeature feature) {
        super(Text.translatable("screen.dmcplus.admin.title"));
        this.parent = parent;
        this.feature = feature;
    }

    @Override
    protected void init() {
        ScreenSections panel = this.panel();
        ScreenSections content = this.content(panel);
        int y = panel.y() + 42;
        int gap = 6;
        int targetWidth = 126;
        int durationWidth = 84;
        int buttonWidth = 92;

        this.targetField = this.addDrawableChild(new TextFieldWidget(
                this.textRenderer,
                content.x(),
                y + 12,
                targetWidth,
                FIELD_HEIGHT,
                Text.translatable("screen.dmcplus.admin.target_field")
        ));
        this.targetField.setMaxLength(32);
        this.targetField.setText(this.feature.targetName());

        this.durationField = this.addDrawableChild(new TextFieldWidget(
                this.textRenderer,
                content.x() + targetWidth + gap,
                y + 12,
                durationWidth,
                FIELD_HEIGHT,
                Text.translatable("screen.dmcplus.admin.duration")
        ));
        this.durationField.setMaxLength(20);
        this.durationField.setText(this.feature.duration());

        this.reasonField = this.addDrawableChild(new TextFieldWidget(
                this.textRenderer,
                content.x() + targetWidth + durationWidth + gap * 2,
                y + 12,
                content.width() - targetWidth - durationWidth - gap * 2,
                FIELD_HEIGHT,
                Text.translatable("screen.dmcplus.admin.reason")
        ));
        this.reasonField.setMaxLength(120);
        this.reasonField.setText(this.feature.reason());

        y += 39;
        for (AdminAction action : AdminAction.values()) {
            int rowY = y;
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(action == this.feature.quickAction() ? "★ " + action.displayName() : action.displayName()),
                    button -> {
                        this.saveSettings();
                        this.feature.setQuickAction(action);
                        this.clearAndInit();
                    }
            ).dimensions(content.x(), rowY, buttonWidth, 20).build());

            TextFieldWidget templateField = this.addDrawableChild(new TextFieldWidget(
                    this.textRenderer,
                    content.x() + buttonWidth + gap,
                    rowY + 1,
                    content.width() - buttonWidth - gap - 70,
                    FIELD_HEIGHT,
                    Text.literal(action.displayName())
            ));
            templateField.setMaxLength(160);
            templateField.setText(this.feature.template(action));
            this.templateFields.put(action, templateField);

            this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("screen.dmcplus.admin.execute"),
                    button -> {
                        this.saveSettings();
                        this.feature.executeAction(this.client, action);
                    }
            ).dimensions(content.right() - 64, rowY, 64, 20).build());
            y += ACTION_ROW_HEIGHT;
        }

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.back"),
                button -> this.close()
        ).dimensions(panel.right() - 88, panel.bottom() - 30, 74, 20).build());
    }

    @Override
    public void close() {
        this.saveSettings();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void removed() {
        this.saveSettings();
        super.removed();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderInGameBackground(context);
        context.fillGradient(0, 0, this.width, this.height, 0xB0101010, 0xC0141414);

        ScreenSections panel = this.panel();
        ScreenSections content = this.content(panel);
        this.renderPanel(context, panel);

        context.drawTextWithShadow(this.textRenderer, this.title, content.x(), panel.y() + 12, 0xFFE8E8E8);
        context.drawTextWithShadow(this.textRenderer, this.targetText(), content.x(), panel.y() + 24, 0xFFBEBEBE);

        int y = panel.y() + 42;
        int targetWidth = 126;
        int durationWidth = 84;
        int gap = 6;
        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.dmcplus.admin.target_field"), content.x(), y, 0xFFCFCFCF);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.dmcplus.admin.duration"), content.x() + targetWidth + gap, y, 0xFFCFCFCF);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.dmcplus.admin.reason"), content.x() + targetWidth + durationWidth + gap * 2, y, 0xFFCFCFCF);

        context.drawTextWithShadow(
                this.textRenderer,
                Text.translatable("screen.dmcplus.admin.template_hint"),
                content.x(),
                panel.bottom() - 25,
                0xFF9A9A9A
        );

        super.render(context, mouseX, mouseY, delta);
    }

    private Text targetText() {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity target = this.feature.getLookedPlayer(client);
        if (target == null) {
            String savedTarget = this.targetField != null && !this.targetField.getText().isBlank()
                    ? this.targetField.getText().trim()
                    : this.feature.targetName();
            return savedTarget.isBlank()
                    ? Text.translatable("screen.dmcplus.admin.target_none")
                    : Text.translatable("screen.dmcplus.admin.target_manual", savedTarget);
        }

        return Text.translatable("screen.dmcplus.admin.target", target.getGameProfile().getName());
    }

    private void saveSettings() {
        if (this.targetField != null) {
            this.feature.setTargetName(this.targetField.getText());
        }
        if (this.durationField != null) {
            this.feature.setDuration(this.durationField.getText());
        }
        if (this.reasonField != null) {
            this.feature.setReason(this.reasonField.getText());
        }
        for (Map.Entry<AdminAction, TextFieldWidget> entry : this.templateFields.entrySet()) {
            this.feature.setTemplate(entry.getKey(), entry.getValue().getText());
        }
    }

    private ScreenSections panel() {
        int panelWidth = MathHelper.clamp(this.width - 24, 380, PANEL_MAX_WIDTH);
        int panelHeight = MathHelper.clamp(this.height - 28, 250, PANEL_HEIGHT);
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
}
