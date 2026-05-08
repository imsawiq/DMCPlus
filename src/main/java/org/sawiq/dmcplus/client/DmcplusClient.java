package org.sawiq.dmcplus.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import org.sawiq.dmcplus.client.feature.QrScannerFeature;
import org.sawiq.dmcplus.client.feature.admin.AdminPanelFeature;
import org.sawiq.dmcplus.client.feature.branches.BranchHudFeature;
import org.sawiq.dmcplus.client.feature.guard.GuardCallFeature;
import org.sawiq.dmcplus.client.feature.rules.RulesFeature;
import org.sawiq.dmcplus.client.feature.trade.TradeFederationFeature;
import org.sawiq.dmcplus.client.feature.waypoint.WaypointFeature;
import org.sawiq.dmcplus.client.update.ModrinthVersionChecker;
import org.sawiq.dmcplus.client.ui.DmcplusModulesScreen;

import java.net.URI;

public class DmcplusClient implements ClientModInitializer {

    private static DmcplusClient instance;

    private final BranchHudFeature branchHudFeature = new BranchHudFeature();
    private final QrScannerFeature qrScannerFeature = new QrScannerFeature();
    private final RulesFeature rulesFeature = new RulesFeature();
    private final TradeFederationFeature tradeFederationFeature = new TradeFederationFeature();
    private final WaypointFeature waypointFeature = new WaypointFeature();
    private final GuardCallFeature guardCallFeature = new GuardCallFeature(this.waypointFeature);
    private final AdminPanelFeature adminPanelFeature = new AdminPanelFeature();
    private final ModrinthVersionChecker versionChecker = new ModrinthVersionChecker();
    private boolean countTradeSlotsKeyDown;
    private boolean manualTradeSlotsKeyDown;
    private boolean updateCheckStarted;

    @Override
    public void onInitializeClient() {
        instance = this;

        KeyBinding scanQrKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dmcplus.scan_qr",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "category.dmcplus.main"
        ));

        KeyBinding toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dmcplus.toggle_branches_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.dmcplus.main"
        ));

        KeyBinding openModulesKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dmcplus.open_modules",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.dmcplus.main"
        ));

        KeyBinding clearWaypointKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dmcplus.clear_waypoint",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_DELETE,
                "category.dmcplus.main"
        ));

        KeyBinding countTradeSlotsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dmcplus.count_trade_slots",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.dmcplus.main"
        ));

        KeyBinding manualTradeSlotsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dmcplus.manual_trade_slots",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_L,
                "category.dmcplus.main"
        ));

        KeyBinding openRulesKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dmcplus.open_rules",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.dmcplus.main"
        ));

        KeyBinding adminQuickActionKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dmcplus.admin_quick_action",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.dmcplus.main"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean inScreen = client.currentScreen != null;
            boolean allowedServer = DmcplusServerScope.isAllowed(client);
            if (allowedServer) {
                this.checkForUpdates(client);
            }

            while (scanQrKey.wasPressed()) {
                if (allowedServer && !inScreen) {
                    this.qrScannerFeature.scanCurrentFrame(client);
                }
            }

            while (toggleHudKey.wasPressed()) {
                if (allowedServer && !inScreen) {
                    this.branchHudFeature.toggle(client);
                }
            }

            while (openModulesKey.wasPressed()) {
                if (allowedServer && !inScreen) {
                    this.openModules(client);
                }
            }

            while (openRulesKey.wasPressed()) {
                if (allowedServer && !inScreen) {
                    this.rulesFeature.open(client);
                }
            }

            while (clearWaypointKey.wasPressed()) {
                if (allowedServer && !inScreen) {
                    this.waypointFeature.clear(client);
                }
            }

            while (adminQuickActionKey.wasPressed()) {
                if (allowedServer && !inScreen) {
                    this.adminPanelFeature.executeQuickAction(client);
                }
            }

            boolean countKeyDown = client.getWindow() != null
                    && GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_K) == GLFW.GLFW_PRESS;
            if (allowedServer && countKeyDown && !this.countTradeSlotsKeyDown && this.tradeFederationFeature.isTradeContainerOpen(client)) {
                this.tradeFederationFeature.countOpenContainerByNearestFrame(client);
            }
            this.countTradeSlotsKeyDown = countKeyDown;

            boolean manualKeyDown = client.getWindow() != null
                    && GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_L) == GLFW.GLFW_PRESS;
            if (allowedServer && manualKeyDown && !this.manualTradeSlotsKeyDown && this.tradeFederationFeature.isTradeContainerOpen(client)) {
                while (manualTradeSlotsKey.wasPressed()) {
                    // Consume Fabric's key state so L does not fire later after closing the container.
                }
                this.tradeFederationFeature.beginManualSlotInput(client);
            }
            this.manualTradeSlotsKeyDown = manualKeyDown;

            if (allowedServer) {
                this.branchHudFeature.tick(client);
                this.tradeFederationFeature.tick(client);
                this.waypointFeature.tick(client);
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) ->
                {
                    if (DmcplusServerScope.isAllowed(MinecraftClient.getInstance())) {
                        this.branchHudFeature.render(drawContext, tickCounter);
                        this.waypointFeature.render(drawContext, tickCounter);
                    }
                }
        );

        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
                {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (DmcplusServerScope.isAllowed(client)) {
                        this.guardCallFeature.onGameMessage(client, message);
                    }
                }
        );

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenEvents.afterRender(screen).register((screenInstance, drawContext, mouseX, mouseY, tickDelta) ->
                    {
                        if (DmcplusServerScope.isAllowed(client)) {
                            this.tradeFederationFeature.renderManualSlotInputOverlay(screenInstance, drawContext);
                        }
                    }
            );
            ScreenKeyboardEvents.allowKeyPress(screen).register((screenInstance, key, scancode, modifiers) ->
                    !DmcplusServerScope.isAllowed(client) || !this.tradeFederationFeature.handleManualSlotInputKey(client, key)
            );
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient && DmcplusServerScope.isAllowed(MinecraftClient.getInstance())) {
                this.tradeFederationFeature.rememberUsedBlock(hitResult.getBlockPos());
            }
            return ActionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient && DmcplusServerScope.isAllowed(MinecraftClient.getInstance()) && entity instanceof ItemFrameEntity frame) {
                this.tradeFederationFeature.rememberUsedFrame(frame);
            }
            return ActionResult.PASS;
        });
    }

    public static DmcplusClient getInstance() {
        return instance;
    }

    public BranchHudFeature getBranchHudFeature() {
        return this.branchHudFeature;
    }

    public QrScannerFeature getQrScannerFeature() {
        return this.qrScannerFeature;
    }

    public RulesFeature getRulesFeature() {
        return this.rulesFeature;
    }

    public TradeFederationFeature getTradeFederationFeature() {
        return this.tradeFederationFeature;
    }

    public WaypointFeature getWaypointFeature() {
        return this.waypointFeature;
    }

    public GuardCallFeature getGuardCallFeature() {
        return this.guardCallFeature;
    }

    public AdminPanelFeature getAdminPanelFeature() {
        return this.adminPanelFeature;
    }

    public void openModules(MinecraftClient client) {
        if (!DmcplusServerScope.isAllowed(client)) {
            return;
        }

        client.setScreen(new DmcplusModulesScreen(client.currentScreen));
    }

    private void checkForUpdates(MinecraftClient client) {
        if (this.updateCheckStarted || client.player == null) {
            return;
        }
        this.updateCheckStarted = true;

        this.versionChecker.checkAsync().thenAccept(result -> {
            if (result == null) {
                return;
            }

            client.execute(() -> {
                if (client.player == null) {
                    return;
                }

                MutableText link = Text.literal(result.version()).formatted(Formatting.AQUA, Formatting.UNDERLINE)
                        .styled(style -> style
                                .withClickEvent(new ClickEvent.OpenUrl(URI.create(result.url())))
                                .withHoverEvent(new HoverEvent.ShowText(Text.translatable("message.dmcplus.update_open")))
                        );
                client.player.sendMessage(Text.translatable("message.dmcplus.update_available", link).formatted(Formatting.GOLD), false);
            });
        });
    }
}
