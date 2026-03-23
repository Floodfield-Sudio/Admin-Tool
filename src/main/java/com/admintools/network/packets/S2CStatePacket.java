package com.admintools.network.packets;

import com.admintools.AdminFeature;
import com.admintools.client.ClientAdminState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Serveur -> Client : synchronise l'etat complet des features de l'admin.
 * Envoye apres chaque toggle et a la connexion.
 */
public class S2CStatePacket {

    private final Set<AdminFeature> features;

    public S2CStatePacket(Set<AdminFeature> features) {
        this.features = features;
    }

    public static void encode(S2CStatePacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.features.size());
        for (AdminFeature f : pkt.features) buf.writeEnum(f);
    }

    public static S2CStatePacket decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        EnumSet<AdminFeature> set = EnumSet.noneOf(AdminFeature.class);
        for (int i = 0; i < count; i++) set.add(buf.readEnum(AdminFeature.class));
        return new S2CStatePacket(set);
    }

    public static void handle(S2CStatePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Mise a jour de l'etat client uniquement
            ClientAdminState.setFeatures(pkt.features);
        });
        ctx.get().setPacketHandled(true);
    }
}
