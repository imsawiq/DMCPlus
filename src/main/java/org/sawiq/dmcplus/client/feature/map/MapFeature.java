package org.sawiq.dmcplus.client.feature.map;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class MapFeature {

    public void open(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        int x = client.player.getBlockX();
        int y = client.player.getBlockY();
        int z = client.player.getBlockZ();
        MapDimension dimension = MapDimension.fromWorld(client.world.getRegistryKey());
        String url = MapUrlBuilder.createUrl(dimension, x, y, z);

        Text dimensionText = Text.translatable(
                "screen.dmcplus.map.dimension",
                switch (dimension) {
                    case OVERWORLD -> "Overworld";
                    case NETHER -> "Nether";
                    case END -> "End";
                }
        );
        Text coordinatesText = Text.translatable("screen.dmcplus.map.coordinates", x, y, z);

        client.setScreen(new MapScreen(client.currentScreen, url, dimensionText, coordinatesText));
    }

    public void openAt(MinecraftClient client, BlockPos pos) {
        if (client.player == null || client.world == null) {
            return;
        }

        MapDimension dimension = MapDimension.fromWorld(client.world.getRegistryKey());
        String url = MapUrlBuilder.createUrl(dimension, pos.getX(), pos.getY(), pos.getZ());

        Text dimensionText = Text.translatable(
                "screen.dmcplus.map.dimension",
                switch (dimension) {
                    case OVERWORLD -> "Overworld";
                    case NETHER -> "Nether";
                    case END -> "End";
                }
        );
        Text coordinatesText = Text.translatable("screen.dmcplus.map.coordinates", pos.getX(), pos.getY(), pos.getZ());
        Text markerText = Text.translatable("screen.dmcplus.map.target_marker", pos.getX(), pos.getY(), pos.getZ());

        client.setScreen(new MapScreen(client.currentScreen, url, dimensionText, coordinatesText, markerText));
    }
}
