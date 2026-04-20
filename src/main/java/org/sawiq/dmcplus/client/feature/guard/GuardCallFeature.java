package org.sawiq.dmcplus.client.feature.guard;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.sawiq.dmcplus.client.feature.waypoint.WaypointFeature;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuardCallFeature {

    private static final String AXE_BADGE = "\uD83E\uDE93";
    private static final Pattern ACCEPTED_CALL_PATTERN = Pattern.compile(
            "Вы приняли вызов игрока\\s+([^!\\s]+)!.*?\\[(-?\\d+),\\s*(-?\\d+),\\s*(-?\\d+),\\s*([^\\]]+)]",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.UNICODE_CASE
    );

    private final WaypointFeature waypointFeature;
    private boolean enabled = true;

    public GuardCallFeature(WaypointFeature waypointFeature) {
        this.waypointFeature = waypointFeature;
    }

    public void toggle(MinecraftClient client) {
        if (!this.isGuard(client)) {
            if (client.player != null) {
                client.player.sendMessage(Text.translatable("message.dmcplus.guard_not_allowed").formatted(Formatting.RED), true);
            }
            return;
        }

        this.enabled = !this.enabled;
        if (client.player != null) {
            client.player.sendMessage(
                    Text.translatable(this.enabled ? "message.dmcplus.guard_calls_on" : "message.dmcplus.guard_calls_off")
                            .formatted(this.enabled ? Formatting.GREEN : Formatting.RED),
                    true
            );
        }
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isGuard(MinecraftClient client) {
        if (client == null || client.player == null || client.getNetworkHandler() == null) {
            return false;
        }

        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        if (entry == null || entry.getDisplayName() == null) {
            return false;
        }

        return entry.getDisplayName().getString().contains(AXE_BADGE);
    }

    public void onGameMessage(MinecraftClient client, Text message) {
        if (!this.enabled || !this.isGuard(client) || client.player == null || client.world == null) {
            return;
        }

        Matcher matcher = ACCEPTED_CALL_PATTERN.matcher(message.getString());
        if (!matcher.find()) {
            return;
        }

        String playerName = matcher.group(1);
        int x = Integer.parseInt(matcher.group(2));
        int y = Integer.parseInt(matcher.group(3));
        int z = Integer.parseInt(matcher.group(4));
        String sourceWorld = matcher.group(5).trim().toLowerCase();

        boolean sourceNether = sourceWorld.contains("nether");
        this.waypointFeature.setGuardTarget(client, playerName, new net.minecraft.util.math.BlockPos(x, y, z), sourceNether);
    }
}
