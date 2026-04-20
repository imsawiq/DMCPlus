package org.sawiq.dmcplus.client;

import net.minecraft.client.MinecraftClient;

import java.util.Locale;

public final class DmcplusServerScope {

    private static final String REQUIRED_ADDRESS_PART = "dmc-minecraft";

    private DmcplusServerScope() {
    }

    public static boolean isAllowed(MinecraftClient client) {
        if (client == null) {
            return false;
        }

        var serverEntry = client.getCurrentServerEntry();
        if (serverEntry == null || serverEntry.address == null) {
            return false;
        }

        return serverEntry.address.toLowerCase(Locale.ROOT).contains(REQUIRED_ADDRESS_PART);
    }
}
