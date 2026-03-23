package com.admintools.events;

import com.admintools.AdminFeature;
import com.admintools.AdminToolsMod;
import com.admintools.AdminToolsState;
import com.admintools.network.NetworkHandler;
import com.admintools.network.packets.S2CStatePacket;
import com.admintools.server.TopLuckTracker;
import com.admintools.server.anticheat.AntiCheatManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = AdminToolsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEvents {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.getServer().getPlayerList().isOp(player.getGameProfile())) return;

        AdminToolsState.initAdmin(player.getUUID());
        NetworkHandler.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new S2CStatePacket(AdminToolsState.getFeatures(player.getUUID()))
        );
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "\u00a78[\u00a7bAdminTools\u00a78] \u00a77Panel : \u00a7b[K] \u00a77| Commande : \u00a7b/at"));
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        AntiCheatManager.cleanup(player.getUUID());
        TopLuckTracker.cleanup(player.getUUID());
        AdminToolsState.removeAdmin(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        var recipients = AdminToolsState.getAlertRecipients();
        if (recipients.isEmpty()) return;

        boolean anyFly    = recipients.stream().anyMatch(id -> AdminToolsState.isEnabled(id, AdminFeature.FLY_DETECT));
        boolean anyNoclip = recipients.stream().anyMatch(id -> AdminToolsState.isEnabled(id, AdminFeature.NOCLIP_DETECT));
        if (anyFly || anyNoclip) AntiCheatManager.tick(player);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        BlockState state  = event.getState();
        ResourceLocation  blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String playerName = player.getName().getString();

        // ── TopLuck ───────────────────────────────────────────────────────────
        if (TopLuckTracker.isOre(blockId)) {
            // Ore : enregistre le groupe + incremente la roche hote correspondante
            // Note : on n'incremente PAS la roche hote ici, on le fait seulement
            // pour les blocs non-ore. Le ratio = ores / roche_hote reste propre.
            TopLuckTracker.recordOre(player.getUUID(), playerName, blockId);
        } else {
            // Non-ore : enregistre la roche hote si applicable + tous les blocs
            TopLuckTracker.recordBlock(player.getUUID(), playerName, blockId);
        }

        // ── AntiCheat XRay ────────────────────────────────────────────────────
        var recipients = AdminToolsState.getAlertRecipients();
        boolean anyXray = !recipients.isEmpty()
            && recipients.stream().anyMatch(id -> AdminToolsState.isEnabled(id, AdminFeature.XRAY_DETECT));
        if (anyXray) AntiCheatManager.onBlockBroken(player, state);
    }
}
