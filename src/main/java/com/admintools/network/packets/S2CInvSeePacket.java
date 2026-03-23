package com.admintools.network.packets;

import com.admintools.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Serveur -> Client : envoie les 41 slots de l'inventaire cible.
 *
 * IMPORTANT : ce fichier NE doit pas contenir de reference directe a
 * Minecraft, Screen, ou toute classe @OnlyIn(CLIENT) dans son bytecode
 * (meme dans des lambdas). Le serveur dedie charge cette classe et
 * crasherait sinon. ClientPacketHandler est l'intermediaire.
 */
public class S2CInvSeePacket {

    private final String          targetName;
    private final List<ItemStack> items;

    public S2CInvSeePacket(String targetName, List<ItemStack> items) {
        this.targetName = targetName;
        this.items      = items;
    }

    public static void encode(S2CInvSeePacket p, FriendlyByteBuf b) {
        b.writeUtf(p.targetName);
        b.writeVarInt(p.items.size());
        for (ItemStack s : p.items) b.writeItem(s);
    }

    public static S2CInvSeePacket decode(FriendlyByteBuf b) {
        String name  = b.readUtf();
        int    count = b.readVarInt();
        List<ItemStack> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) items.add(b.readItem());
        return new S2CInvSeePacket(name, items);
    }

    /**
     * Gestion du packet : N'importe quel acces a Minecraft / Screen
     * passe par ClientPacketHandler pour eviter le chargement cote serveur.
     */
    public static void handle(S2CInvSeePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            // DistExecutor garantit que ClientPacketHandler.openInvSee
            // n'est jamais APPELE sur le serveur.
            // La reference a ClientPacketHandler dans le lambda est safe
            // car ClientPacketHandler n'a pas @OnlyIn et ses methodes sont
            // chargees paresseusement par la JVM.
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.openInvSee(pkt.targetName, pkt.items))
        );
        ctx.get().setPacketHandled(true);
    }
}
