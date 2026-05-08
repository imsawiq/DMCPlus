package org.sawiq.dmcplus.client.feature.admin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.sawiq.dmcplus.client.ui.AdminPanelScreen;

public class AdminPanelFeature {

    private static final String STAFF_BADGE = "⭐";
    private static final String TESTER_NAME = "sawiq_";
    private static final double TARGET_RANGE = 128.0D;
    private static final double TARGET_BOX_PADDING = 0.45D;

    private AdminAction quickAction = AdminAction.TEMP_BAN;
    private String targetName = "";
    private String duration = "1d";
    private String reason = "Нарушение правил";
    private String banTemplate = "tempban {player} {duration} {reason}";
    private String textMuteTemplate = "tempmute {player} {duration} {reason}";
    private String voiceMuteTemplate = "vmute {player} {duration} {reason}";

    public void open(MinecraftClient client) {
        if (!this.hasAccess(client)) {
            this.sendNoAccess(client);
            return;
        }

        client.setScreen(new AdminPanelScreen(client.currentScreen, this));
    }

    public void executeQuickAction(MinecraftClient client) {
        this.executeAction(client, this.quickAction);
    }

    public void executeAction(MinecraftClient client, AdminAction action) {
        if (!this.hasAccess(client)) {
            this.sendNoAccess(client);
            return;
        }

        String target = this.resolveTargetName(client);
        if (target.isBlank()) {
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.translatable("message.dmcplus.admin_no_target").formatted(Formatting.RED), true);
            }
            return;
        }

        String command = this.buildCommand(action, target);
        if (command.isBlank() || client.player == null || client.player.networkHandler == null) {
            return;
        }

        client.player.networkHandler.sendChatCommand(stripSlash(command));
        client.player.sendMessage(
                Text.translatable("message.dmcplus.admin_command_sent", action.displayName(), target)
                        .formatted(Formatting.GREEN),
                true
        );
    }

    public PlayerEntity getLookedPlayer(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return null;
        }

        if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) client.crosshairTarget).getEntity();
            if (entity instanceof PlayerEntity player && player != client.player) {
                this.targetName = player.getGameProfile().getName();
                return player;
            }
        }

        Vec3d start = client.player.getCameraPosVec(1.0F);
        Vec3d end = start.add(client.player.getRotationVec(1.0F).multiply(TARGET_RANGE));
        PlayerEntity bestPlayer = null;
        double bestDistance = TARGET_RANGE * TARGET_RANGE;

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || player.isSpectator()) {
                continue;
            }

            Box box = player.getBoundingBox().expand(TARGET_BOX_PADDING);
            if (box.raycast(start, end).isEmpty()) {
                continue;
            }

            double distance = player.squaredDistanceTo(client.player);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestPlayer = player;
            }
        }

        if (bestPlayer != null) {
            this.targetName = bestPlayer.getGameProfile().getName();
        }
        return bestPlayer;
    }

    public boolean hasAccess(MinecraftClient client) {
        if (client == null || client.player == null) {
            return false;
        }

        if (TESTER_NAME.equalsIgnoreCase(client.player.getGameProfile().getName())) {
            return true;
        }

        if (client.getNetworkHandler() == null) {
            return false;
        }

        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry != null
                && entry.getDisplayName() != null
                && entry.getDisplayName().getString().contains(STAFF_BADGE);
    }

    public AdminAction quickAction() {
        return this.quickAction;
    }

    public void setQuickAction(AdminAction quickAction) {
        this.quickAction = quickAction;
    }

    public String duration() {
        return this.duration;
    }

    public String targetName() {
        return this.targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName == null ? "" : targetName.trim();
    }

    public void setDuration(String duration) {
        this.duration = clean(duration, "1d");
    }

    public String reason() {
        return this.reason;
    }

    public void setReason(String reason) {
        this.reason = clean(reason, "Нарушение правил");
    }

    public String template(AdminAction action) {
        return switch (action) {
            case TEMP_BAN -> this.banTemplate;
            case TEXT_MUTE -> this.textMuteTemplate;
            case VOICE_MUTE -> this.voiceMuteTemplate;
        };
    }

    public void setTemplate(AdminAction action, String template) {
        String cleaned = clean(template, this.defaultTemplate(action));
        switch (action) {
            case TEMP_BAN -> this.banTemplate = cleaned;
            case TEXT_MUTE -> this.textMuteTemplate = cleaned;
            case VOICE_MUTE -> this.voiceMuteTemplate = cleaned;
        }
    }

    private String buildCommand(AdminAction action, String playerName) {
        return this.template(action)
                .replace("{player}", playerName)
                .replace("{duration}", this.duration)
                .replace("{reason}", this.reason)
                .trim();
    }

    private String defaultTemplate(AdminAction action) {
        return switch (action) {
            case TEMP_BAN -> "tempban {player} {duration} {reason}";
            case TEXT_MUTE -> "tempmute {player} {duration} {reason}";
            case VOICE_MUTE -> "vmute {player} {duration} {reason}";
        };
    }

    private String resolveTargetName(MinecraftClient client) {
        PlayerEntity lookedPlayer = this.getLookedPlayer(client);
        if (lookedPlayer != null) {
            return lookedPlayer.getGameProfile().getName();
        }
        return this.targetName;
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? "" : value.trim();
        return cleaned.isEmpty() ? fallback : cleaned;
    }

    private static String stripSlash(String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }

    private void sendNoAccess(MinecraftClient client) {
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.translatable("message.dmcplus.admin_not_allowed").formatted(Formatting.RED), true);
        }
    }
}
