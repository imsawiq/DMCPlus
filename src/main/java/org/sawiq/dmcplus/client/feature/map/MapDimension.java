package org.sawiq.dmcplus.client.feature.map;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public enum MapDimension {
    OVERWORLD("minecraft_overworld"),
    NETHER("minecraft_the_nether"),
    END("minecraft_the_end");

    private final String pathKey;

    MapDimension(String pathKey) {
        this.pathKey = pathKey;
    }

    public String pathKey() {
        return this.pathKey;
    }

    public static MapDimension fromWorld(RegistryKey<World> worldKey) {
        if (worldKey == World.NETHER) {
            return NETHER;
        }
        if (worldKey == World.END) {
            return END;
        }
        return OVERWORLD;
    }
}
