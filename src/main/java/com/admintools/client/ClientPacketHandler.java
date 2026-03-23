package com.admintools.client;

import com.admintools.TopLuckEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Proxy CLIENT UNIQUEMENT pour ouvrir les ecrans depuis les packets S2C.
 * Pas @OnlyIn -- corps charges paresseusement, jamais appeles sur le serveur
 * (DistExecutor le garantit dans les packets).
 */
public class ClientPacketHandler {

    public static void openInvSee(String targetName, List<ItemStack> items) {
        Minecraft.getInstance().setScreen(new InvSeeScreen(targetName, items));
    }

    public static void openTopLuck(List<TopLuckEntry> entries) {
        Minecraft.getInstance().setScreen(new TopLuckScreen(entries));
    }
}
