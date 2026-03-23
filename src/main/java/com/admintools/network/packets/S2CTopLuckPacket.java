package com.admintools.network.packets;

import com.admintools.TopLuckEntry;
import com.admintools.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class S2CTopLuckPacket {

    private final List<TopLuckEntry> entries;

    public S2CTopLuckPacket(List<TopLuckEntry> entries) {
        this.entries = entries;
    }

    public static void encode(S2CTopLuckPacket p, FriendlyByteBuf b) {
        b.writeVarInt(p.entries.size());
        for (TopLuckEntry e : p.entries) {
            b.writeUtf(e.name);
            b.writeVarInt(e.overworldRock);
            b.writeVarInt(e.netherRock);
            b.writeVarInt(e.endRock);
            // Ore groups
            b.writeVarInt(e.oreGroups.size());
            for (Map.Entry<String, Integer> g : e.oreGroups.entrySet()) {
                b.writeUtf(g.getKey());
                b.writeVarInt(g.getValue());
            }
            // All blocks (limites a MAX_DETAIL)
            List<Map.Entry<String, Integer>> topBlocks = e.sortedAllBlocks()
                .stream().limit(TopLuckEntry.MAX_DETAIL).toList();
            b.writeVarInt(topBlocks.size());
            for (Map.Entry<String, Integer> bl : topBlocks) {
                b.writeUtf(bl.getKey());
                b.writeVarInt(bl.getValue());
            }
        }
    }

    public static S2CTopLuckPacket decode(FriendlyByteBuf b) {
        int count = b.readVarInt();
        List<TopLuckEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String name  = b.readUtf();
            int owRock   = b.readVarInt();
            int neRock   = b.readVarInt();
            int enRock   = b.readVarInt();
            int oreCount = b.readVarInt();
            Map<String, Integer> oreMap = new LinkedHashMap<>(oreCount);
            for (int g = 0; g < oreCount; g++) oreMap.put(b.readUtf(), b.readVarInt());
            int blCount = b.readVarInt();
            Map<String, Integer> blMap = new LinkedHashMap<>(blCount);
            for (int g = 0; g < blCount; g++) blMap.put(b.readUtf(), b.readVarInt());
            entries.add(new TopLuckEntry(name, owRock, neRock, enRock, oreMap, blMap));
        }
        return new S2CTopLuckPacket(entries);
    }

    public static void handle(S2CTopLuckPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.openTopLuck(pkt.entries))
        );
        ctx.get().setPacketHandled(true);
    }
}
