package org.sawiq.dmcplus.client.feature.trade;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TradeScanner {

    private static final double SCAN_RADIUS = 48.0D;

    private TradeScanner() {
    }

    public static List<TradeListing> scan(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return List.of();
        }

        Box scanBox = client.player.getBoundingBox().expand(SCAN_RADIUS);
        List<ItemFrameEntity> frames = client.world.getEntitiesByClass(
                ItemFrameEntity.class,
                scanBox,
                frame -> !frame.getHeldItemStack().isEmpty()
        );

        List<TradeListing> listings = new ArrayList<>();
        for (ItemFrameEntity frame : frames) {
            ItemStack stack = frame.getHeldItemStack();
            Text itemText = stack.getItem().getName(stack);
            String productName = itemText.getString();
            String displayName = stack.getName().getString();
            PriceQuote quote = PriceParser.parse(displayName);
            if (quote == null) {
                continue;
            }

            listings.add(new TradeListing(
                    productName,
                    displayName,
                    stack.getItem(),
                    quote != null ? quote.sourceText() : displayName,
                    quote,
                    frame.getAttachedBlockPos()
            ));
        }

        listings.sort(Comparator
                .comparing(TradeListing::productName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(listing -> listing.position().getX())
                .thenComparing(listing -> listing.position().getY())
                .thenComparing(listing -> listing.position().getZ()));
        return listings;
    }
}
