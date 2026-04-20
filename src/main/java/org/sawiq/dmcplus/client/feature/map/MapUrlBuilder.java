package org.sawiq.dmcplus.client.feature.map;

public final class MapUrlBuilder {

    private static final String BASE_URL = "https://map.dmc-minecraft.net/#%s;flat;%d,%d,%d;3";

    private MapUrlBuilder() {
    }

    public static String createUrl(MapDimension dimension, int x, int y, int z) {
        return BASE_URL.formatted(dimension.pathKey(), x, y, z);
    }
}
