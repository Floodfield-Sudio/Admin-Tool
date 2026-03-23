package com.admintools.network;

import com.admintools.AdminToolsMod;
import com.admintools.network.packets.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {

    private static final String PROTOCOL = "3";

    /**
     * Mod OPTIONNEL des deux cotes.
     *
     * Un joueur SANS le mod peut se connecter a un serveur QUI l'a.
     * Un admin AVEC le mod peut utiliser panel/ESP/InvSee normalement.
     *
     * En Forge 1.20.1 :
     *   - NetworkRegistry.ABSENT   est une instance avec .version() -> String
     *   - NetworkRegistry.ACCEPTVANILLA est directement une String (pas d'objet)
     *
     * Les deux predicats (client ET serveur) doivent accepter ABSENT,
     * sinon Forge refuse quand meme la connexion.
     */
    private static boolean acceptVersion(String version) {
        return PROTOCOL.equals(version)
            || NetworkRegistry.ABSENT.version().equals(version);
    }

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(AdminToolsMod.MOD_ID, "main"),
        () -> PROTOCOL,
        NetworkHandler::acceptVersion,  // client : avec ou sans le mod
        NetworkHandler::acceptVersion   // serveur : avec ou sans le mod
    );

    public static void init() {
        int id = 0;

        // Client -> Serveur
        CHANNEL.registerMessage(id++,
            C2STogglePacket.class,
            C2STogglePacket::encode,
            C2STogglePacket::decode,
            C2STogglePacket::handle);

        CHANNEL.registerMessage(id++,
            C2SInvSeePacket.class,
            C2SInvSeePacket::encode,
            C2SInvSeePacket::decode,
            C2SInvSeePacket::handle);

        // Serveur -> Client
        CHANNEL.registerMessage(id++,
            S2CStatePacket.class,
            S2CStatePacket::encode,
            S2CStatePacket::decode,
            S2CStatePacket::handle);

        CHANNEL.registerMessage(id++,
            S2CViolationPacket.class,
            S2CViolationPacket::encode,
            S2CViolationPacket::decode,
            S2CViolationPacket::handle);

        CHANNEL.registerMessage(id++,
            S2CInvSeePacket.class,
            S2CInvSeePacket::encode,
            S2CInvSeePacket::decode,
            S2CInvSeePacket::handle);

        CHANNEL.registerMessage(id++,
            S2CTopLuckPacket.class,
            S2CTopLuckPacket::encode,
            S2CTopLuckPacket::decode,
            S2CTopLuckPacket::handle);
    }
}
