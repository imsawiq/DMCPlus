package org.sawiq.dmcplus.client.feature.trade;

public record ContainerSlotCount(
        int matchingSlots,
        int occupiedSlots,
        int containerSlots
) {
}
