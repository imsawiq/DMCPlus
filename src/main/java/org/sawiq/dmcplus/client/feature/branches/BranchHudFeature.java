package org.sawiq.dmcplus.client.feature.branches;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.Locale;

public class BranchHudFeature {

    private final BranchHudState state = new BranchHudState();
    private final BranchHudRenderer renderer = new BranchHudRenderer();
    private boolean enabled = true;

    public void tick(MinecraftClient client) {
        BranchContext context = this.resolveContext(client);
        this.state.tick(context != null, context != null ? context.progress() : 0.0F);
    }

    public void toggle(MinecraftClient client) {
        this.enabled = !this.enabled;
        if (client.player != null) {
            client.player.sendMessage(
                    Text.translatable(
                            this.enabled ? "message.dmcplus.branches_hud_on" : "message.dmcplus.branches_hud_off"
                    ).formatted(this.enabled ? Formatting.GREEN : Formatting.RED),
                    true
            );
        }
    }

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        BranchContext branchContext = this.resolveContext(client);
        if (branchContext == null) {
            return;
        }

        this.renderer.render(context, client, branchContext, this.state);
    }

    private BranchContext resolveContext(MinecraftClient client) {
        if (!this.enabled || client == null || client.player == null) {
            return null;
        }

        ClientWorld world = client.player.clientWorld;
        if (world == null || world.getRegistryKey() != World.NETHER) {
            return null;
        }

        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo == null) {
            return null;
        }

        String address = serverInfo.address == null ? "" : serverInfo.address.toLowerCase(Locale.ROOT);
        if (!address.contains("dmc-minecraft.net")) {
            return null;
        }

        return BranchResolver.resolve(client.player.getX(), client.player.getZ());
    }
}
