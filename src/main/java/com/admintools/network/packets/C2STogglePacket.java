package com.admintools.network.packets;

import com.admintools.AdminFeature;
import com.admintools.AdminToolsState;
import com.admintools.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * Client -> Serveur : bascule une feature admin.
 */
public class C2STogglePacket {

    private final AdminFeature feature;

    public C2STogglePacket(AdminFeature feature) { this.feature = feature; }

    public static void encode(C2STogglePacket p, FriendlyByteBuf b) { b.writeEnum(p.feature); }
    public static C2STogglePacket decode(FriendlyByteBuf b) {
        return new C2STogglePacket(b.readEnum(AdminFeature.class));
    }

    public static void handle(C2STogglePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!player.getServer().getPlayerList().isOp(player.getGameProfile())) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a7c[AdminTools] Droits operateur requis."));
                return;
            }
            boolean now = AdminToolsState.toggle(player.getUUID(), pkt.feature);
            applyEffect(player, pkt.feature, now);
            NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new S2CStatePacket(AdminToolsState.getFeatures(player.getUUID()))
            );
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * Effets de bord serveur lors du toggle.
     * Methode public static pour etre appelee depuis AdminCommand.
     */
    public static void applyEffect(ServerPlayer player, AdminFeature feature, boolean enabled) {
        switch (feature) {
            case VANISH -> {
                if (enabled) AdminToolsState.applyVanish(player);
                else         AdminToolsState.removeVanish(player);
            }
            case NOCLIP -> {
                // NOCLIP = usage personnel de l'admin (spectateur)
                // NOCLIP_DETECT = detection des autres joueurs uniquement
                if (enabled) AdminToolsState.applyNoclip(player);
                else         AdminToolsState.removeNoclip(player);
            }
            default -> { /* pas d'effet de bord */ }
        }
    }
}
