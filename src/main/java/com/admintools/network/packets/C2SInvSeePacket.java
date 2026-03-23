package com.admintools.network.packets;

import com.admintools.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Client -> Serveur : demande l'inventaire d'un joueur.
 * Le serveur repond avec S2CInvSeePacket.
 */
public class C2SInvSeePacket {

    private final String targetName;

    public C2SInvSeePacket(String targetName) { this.targetName = targetName; }

    public static void encode(C2SInvSeePacket p, FriendlyByteBuf b) { b.writeUtf(p.targetName); }
    public static C2SInvSeePacket decode(FriendlyByteBuf b) {
        return new C2SInvSeePacket(b.readUtf());
    }

    public static void handle(C2SInvSeePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer requester = ctx.get().getSender();
            if (requester == null) return;
            if (!requester.getServer().getPlayerList().isOp(requester.getGameProfile())) return;

            ServerPlayer target = requester.getServer()
                .getPlayerList().getPlayerByName(pkt.targetName);
            if (target == null) {
                requester.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a7c[AdminTools] Joueur introuvable : " + pkt.targetName));
                return;
            }

            // Collecte les 41 slots : [0-35]=main+hotbar, [36-39]=armure, [40]=offhand
            List<ItemStack> items = new ArrayList<>(41);
            for (int i = 0; i < 36; i++) items.add(target.getInventory().getItem(i).copy());
            for (int i = 0; i < 4;  i++) items.add(target.getInventory().armor.get(i).copy());
            items.add(target.getInventory().offhand.get(0).copy());

            NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> requester),
                new S2CInvSeePacket(pkt.targetName, items)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
