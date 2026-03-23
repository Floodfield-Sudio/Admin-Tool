package com.admintools.network.packets;

import com.admintools.client.ClientAdminState;
import com.admintools.server.anticheat.ViolationType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * Serveur -> Client admin : notifie une violation detectee.
 */
public class S2CViolationPacket {

    private final ViolationType type;
    private final String        playerName;
    private final String        details;

    public S2CViolationPacket(ViolationType type, String playerName, String details) {
        this.type       = type;
        this.playerName = playerName;
        this.details    = details;
    }

    public static void encode(S2CViolationPacket pkt, FriendlyByteBuf buf) {
        buf.writeEnum(pkt.type);
        buf.writeUtf(pkt.playerName);
        buf.writeUtf(pkt.details);
    }

    public static S2CViolationPacket decode(FriendlyByteBuf buf) {
        return new S2CViolationPacket(
            buf.readEnum(ViolationType.class),
            buf.readUtf(),
            buf.readUtf()
        );
    }

    public static void handle(S2CViolationPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            String time  = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String entry = "[" + time + "] " + pkt.playerName
                         + " \u2014 " + pkt.type.name()
                         + " \u2014 " + pkt.details;
            ClientAdminState.addViolation(entry);
        });
        ctx.get().setPacketHandled(true);
    }
}
