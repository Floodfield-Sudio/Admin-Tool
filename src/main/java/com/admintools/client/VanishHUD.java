package com.admintools.client;

import com.admintools.AdminFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

/**
 * Affiche un indicateur "[VANISH]" clignotant en haut a droite de l'ecran
 * quand le vanish est actif -- visible meme hors du panel admin.
 */
public class VanishHUD {

    private static int tick = 0;

    /**
     * Appele depuis ClientEvents lors du rendu du HUD (apres la hotbar).
     */
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (!ClientAdminState.isEnabled(AdminFeature.VANISH)) return;

        tick++;

        // Clignotement : visible 25 ticks, invisible 15 ticks (cycle 40)
        boolean visible = (tick % 40) < 25;
        if (!visible) return;

        GuiGraphics gfx = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();

        String text = "\u00a78[\u00a7b\u00a7lVANISH\u00a7r\u00a78]";
        // width() ne prend pas les codes couleur en compte correctement -- on fixe la largeur
        int textW = mc.font.width("[ VANISH ]");
        int x = screenW - textW - 6;
        int y = 6;

        // Fond semi-transparent
        gfx.fill(x - 3, y - 2, x + textW + 3, y + 10, 0xAA000000);
        gfx.drawString(mc.font, text, x, y, 0xFFFFFFFF, false);
    }
}
