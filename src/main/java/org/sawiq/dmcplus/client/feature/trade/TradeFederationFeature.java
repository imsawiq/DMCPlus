package org.sawiq.dmcplus.client.feature.trade;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import org.sawiq.dmcplus.client.ui.TradeFederationScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class TradeFederationFeature {

    private static final double ATTACHED_FRAME_SCAN_RADIUS = 5.0D;

    private final List<TradeListing> listings = new ArrayList<>();
    private String lastStatus = "Нет сканирования";
    private BlockPos lastUsedBlockPos;
    private BlockPos lastUsedFrameBlockPos;
    private boolean manualSlotInputActive;
    private String manualSlotInput = "";

    public void open(MinecraftClient client) {
        client.setScreen(new TradeFederationScreen(client.currentScreen, this));
    }

    public void scanNearby(MinecraftClient client) {
        this.listings.clear();
        this.listings.addAll(TradeScanner.scan(client));
        this.lastStatus = this.listings.isEmpty()
                ? "Рядом ничего не найдено"
                : "Найдено торговых точек: " + this.listings.size();

        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal(this.lastStatus).formatted(this.listings.isEmpty() ? Formatting.RED : Formatting.GREEN),
                    true
            );
        }
    }

    public List<TradeListing> search(MinecraftClient client, String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<TradeListing> result = new ArrayList<>();

        for (TradeListing listing : this.listings) {
            if (normalizedQuery.isEmpty()
                    || listing.productName().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || listing.displayName().toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                result.add(listing);
            }
        }

        if (client != null && client.player != null) {
            result.sort((left, right) -> Double.compare(
                    left.position().getSquaredDistance(client.player.getBlockPos()),
                    right.position().getSquaredDistance(client.player.getBlockPos())
            ));
        }

        return result;
    }

    public List<TradeListing> getListings() {
        return List.copyOf(this.listings);
    }

    public String getLastStatus() {
        return this.lastStatus;
    }

    public Optional<ContainerSlotCount> countOpenContainerSlots(MinecraftClient client, TradeListing listing) {
        if (client == null || client.player == null || listing == null) {
            return Optional.empty();
        }

        if (!(client.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) {
            return Optional.empty();
        }

        int containerSlots = handler.getRows() * 9;
        int matchingSlots = 0;
        int occupiedSlots = 0;
        for (int index = 0; index < Math.min(containerSlots, handler.slots.size()); index++) {
            Slot slot = handler.slots.get(index);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) {
                continue;
            }

            occupiedSlots++;
            if (stack.isOf(listing.item())) {
                matchingSlots++;
            }
        }

        return Optional.of(new ContainerSlotCount(matchingSlots, occupiedSlots, containerSlots));
    }

    public void rememberUsedBlock(BlockPos pos) {
        this.lastUsedBlockPos = pos;
    }

    public void rememberUsedFrame(ItemFrameEntity frame) {
        this.lastUsedFrameBlockPos = frame.getAttachedBlockPos();
    }

    public void countOpenContainerByNearestFrame(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return;
        }

        ResolvedTrade resolvedTrade = this.resolveTradeForOpenContainer(client);
        if (resolvedTrade.missingTarget()) {
            client.player.sendMessage(Text.translatable("message.dmcplus.trade_count_no_target").formatted(Formatting.RED), true);
            return;
        }
        if (resolvedTrade.listing() == null) {
            client.player.sendMessage(Text.translatable("message.dmcplus.trade_count_no_frame").formatted(Formatting.RED), true);
            return;
        }
        if (resolvedTrade.slotCount().isEmpty()) {
            client.player.sendMessage(Text.translatable("message.dmcplus.trade_count_no_container").formatted(Formatting.RED), true);
            return;
        }

        ContainerSlotCount slotCount = resolvedTrade.slotCount().get();
        String cost = resolvedTrade.listing().quote().formatCost(slotCount.matchingSlots());
        client.player.sendMessage(
                Text.translatable(
                        "message.dmcplus.trade_count_result",
                        resolvedTrade.listing().productName(),
                        slotCount.matchingSlots(),
                        cost,
                        slotCount.occupiedSlots(),
                        slotCount.containerSlots()
                ).formatted(Formatting.GOLD),
                false
        );
    }

    public boolean isTradeContainerOpen(MinecraftClient client) {
        return client != null
                && client.player != null
                && client.currentScreen != null
                && client.player.currentScreenHandler instanceof GenericContainerScreenHandler;
    }

    public void beginManualSlotInput(MinecraftClient client) {
        if (!this.isTradeContainerOpen(client)) {
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.translatable("message.dmcplus.trade_manual_no_container").formatted(Formatting.RED), true);
            }
            return;
        }

        this.manualSlotInputActive = true;
        this.manualSlotInput = "";
    }

    public boolean handleManualSlotInputKey(MinecraftClient client, int keyCode) {
        if (!this.manualSlotInputActive) {
            return false;
        }

        if (!this.isTradeContainerOpen(client)) {
            this.manualSlotInputActive = false;
            this.manualSlotInput = "";
            return false;
        }

        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            this.appendManualDigit((char) ('0' + keyCode - GLFW.GLFW_KEY_0));
            return true;
        }

        if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            this.appendManualDigit((char) ('0' + keyCode - GLFW.GLFW_KEY_KP_0));
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!this.manualSlotInput.isEmpty()) {
                this.manualSlotInput = this.manualSlotInput.substring(0, this.manualSlotInput.length() - 1);
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.manualSlotInputActive = false;
            this.manualSlotInput = "";
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.finishManualSlotInput(client);
            return true;
        }

        return false;
    }

    public void renderManualSlotInputOverlay(Screen screen, DrawContext context) {
        if (!this.manualSlotInputActive) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (!this.isTradeContainerOpen(client)) {
            this.manualSlotInputActive = false;
            this.manualSlotInput = "";
            return;
        }

        String value = this.manualSlotInput.isEmpty() ? "_" : this.manualSlotInput;
        Text title = Text.translatable("overlay.dmcplus.trade_manual.title");
        Text body = Text.translatable("overlay.dmcplus.trade_manual.body", value);
        Text hint = Text.translatable("overlay.dmcplus.trade_manual.hint");
        int width = Math.max(178, Math.max(client.textRenderer.getWidth(title), Math.max(client.textRenderer.getWidth(body), client.textRenderer.getWidth(hint))) + 24);
        int height = 50;
        int x = (screen.width - width) / 2;
        int y = 18;

        context.fill(x + 2, y + 2, x + width + 2, y + height + 2, 0x70000000);
        context.fill(x, y, x + width, y + height, 0xF0202020);
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xF0323232);
        context.drawBorder(x, y, width, height, 0xFF6B6B6B);
        context.drawCenteredTextWithShadow(client.textRenderer, title, x + width / 2, y + 7, 0xFFE8E8E8);
        context.drawCenteredTextWithShadow(client.textRenderer, body, x + width / 2, y + 21, 0xFFF2D085);
        context.drawCenteredTextWithShadow(client.textRenderer, hint, x + width / 2, y + 35, 0xFFBEBEBE);
    }

    private BlockPos resolveLookedContainerPos(MinecraftClient client) {
        if (client.crosshairTarget instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            return blockHit.getBlockPos();
        }

        return null;
    }

    private TradeListing findFrameAttachedTo(MinecraftClient client, BlockPos blockPos) {
        List<ItemFrameEntity> frames = client.world.getEntitiesByClass(
                ItemFrameEntity.class,
                client.player.getBoundingBox().expand(ATTACHED_FRAME_SCAN_RADIUS),
                frame -> blockPos.equals(frame.getAttachedBlockPos()) && !frame.getHeldItemStack().isEmpty()
        );

        for (ItemFrameEntity frame : frames) {
            ItemStack stack = frame.getHeldItemStack();
            String displayName = stack.getName().getString();
            PriceQuote quote = PriceParser.parse(displayName);
            if (quote == null) {
                continue;
            }

            String productName = stack.getItem().getName(stack).getString();
            return new TradeListing(productName, displayName, stack.getItem(), quote.sourceText(), quote, frame.getAttachedBlockPos());
        }

        return null;
    }

    private TradeListing findFrameMatchingOpenContainer(MinecraftClient client) {
        List<TradeListing> nearby = TradeScanner.scan(client);
        TradeListing best = null;
        int bestSlots = 0;

        for (TradeListing listing : nearby) {
            Optional<ContainerSlotCount> count = this.countOpenContainerSlots(client, listing);
            if (count.isEmpty() || count.get().matchingSlots() <= bestSlots) {
                continue;
            }

            best = listing;
            bestSlots = count.get().matchingSlots();
        }

        return best;
    }

    private ResolvedTrade resolveTradeForOpenContainer(MinecraftClient client) {
        BlockPos openedContainerPos = this.lastUsedFrameBlockPos != null
                ? this.lastUsedFrameBlockPos
                : (this.lastUsedBlockPos != null ? this.lastUsedBlockPos : this.resolveLookedContainerPos(client));
        if (openedContainerPos == null) {
            return new ResolvedTrade(null, Optional.empty(), true);
        }

        TradeListing listing = this.findFrameAttachedTo(client, openedContainerPos);
        Optional<ContainerSlotCount> count = Optional.empty();
        if (listing != null) {
            count = this.countOpenContainerSlots(client, listing);
            if (count.isEmpty() || count.get().matchingSlots() <= 0) {
                listing = null;
                count = Optional.empty();
            }
        }

        if (listing == null) {
            listing = this.findFrameMatchingOpenContainer(client);
            if (listing != null) {
                count = this.countOpenContainerSlots(client, listing);
            }
        }

        return new ResolvedTrade(listing, count, false);
    }

    private void appendManualDigit(char digit) {
        if (this.manualSlotInput.length() < 4) {
            this.manualSlotInput += digit;
        }
    }

    private void finishManualSlotInput(MinecraftClient client) {
        if (client == null || client.player == null) {
            return;
        }

        if (this.manualSlotInput.isBlank()) {
            client.player.sendMessage(Text.translatable("message.dmcplus.trade_manual_empty").formatted(Formatting.RED), true);
            return;
        }

        int requestedSlots;
        try {
            requestedSlots = Integer.parseInt(this.manualSlotInput);
        } catch (NumberFormatException exception) {
            client.player.sendMessage(Text.translatable("message.dmcplus.trade_manual_empty").formatted(Formatting.RED), true);
            return;
        }

        if (requestedSlots <= 0) {
            client.player.sendMessage(Text.translatable("message.dmcplus.trade_manual_empty").formatted(Formatting.RED), true);
            return;
        }

        ResolvedTrade resolvedTrade = this.resolveTradeForOpenContainer(client);
        if (resolvedTrade.missingTarget()) {
            client.player.sendMessage(Text.translatable("message.dmcplus.trade_count_no_target").formatted(Formatting.RED), true);
            return;
        }
        if (resolvedTrade.listing() == null) {
            client.player.sendMessage(Text.translatable("message.dmcplus.trade_count_no_frame").formatted(Formatting.RED), true);
            return;
        }

        TradeListing listing = resolvedTrade.listing();
        client.player.sendMessage(
                Text.translatable(
                        "message.dmcplus.trade_manual_result",
                        listing.productName(),
                        requestedSlots,
                        listing.quote().formatCost(requestedSlots)
                ).formatted(Formatting.GOLD),
                false
        );
        this.manualSlotInputActive = false;
        this.manualSlotInput = "";
    }

    private record ResolvedTrade(TradeListing listing, Optional<ContainerSlotCount> slotCount, boolean missingTarget) {
    }
}
