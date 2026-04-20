package org.sawiq.dmcplus.client.feature.trade;

import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

public record TradeListing(
        String productName,
        String displayName,
        Item item,
        String priceText,
        PriceQuote quote,
        BlockPos position
) {
}
