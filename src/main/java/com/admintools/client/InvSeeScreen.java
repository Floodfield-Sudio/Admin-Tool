package com.admintools.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Affiche en lecture seule l'inventaire d'un joueur cible.
 *
 * Layout (41 slots) :
 *   Armure (4 cases verticales) | Inventaire principal 9x3 | Hors-ligne 1 case
 *                               | Hotbar 9x1               |
 *
 * Ouvert automatiquement quand S2CInvSeePacket est recu.
 */
public class InvSeeScreen extends Screen {

    private static final int SLOT  = 18;   // taille d'une case
    private static final int PAD   = 4;

    private final String         targetName;
    private final List<ItemStack> allItems;  // 41 slots : [0-35]=main+hotbar, [36-39]=armure, [40]=offhand

    // Slot survole pour le tooltip
    private int hoveredIndex = -1;

    public InvSeeScreen(String targetName, List<ItemStack> allItems) {
        super(Component.literal("Inventaire \u2014 " + targetName));
        this.targetName = targetName;
        this.allItems   = allItems;
    }

    // ── Position de l'ecran ───────────────────────────────────────────────────

    private int originX() { return (width  - (SLOT * 9 + SLOT + PAD * 3)) / 2; }
    private int originY() { return (height - (SLOT * 4 + 30))              / 2; }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        hoveredIndex = -1;  // reset a chaque frame avant de tester les slots
        renderBackground(gfx);

        int ox = originX();
        int oy = originY();

        // ── Titre ─────────────────────────────────────────────────────────────
        gfx.drawCenteredString(font,
            "\u00a7b\u00a7l" + targetName + " \u00a7r\u00a77\u2014 Inventaire (lecture seule)",
            width / 2, oy - 16, 0xFFFFFFFF);

        // ── Fond du panneau ───────────────────────────────────────────────────
        int panelW = SLOT * 9 + SLOT + PAD * 3;
        int panelH = SLOT * 4 + PAD;
        gfx.fill(ox - 4, oy - 4, ox + panelW + 4, oy + panelH + 4, 0xFF222233);
        gfx.fill(ox - 3, oy - 3, ox + panelW + 3, oy + panelH + 3, 0xFF33334A);

        // ── Armure : 4 cases verticales a gauche (tete=haut, pieds=bas) ───────
        for (int i = 0; i < 4; i++) {
            int armorSlot = 36 + (3 - i); // index 39=tete, 38=torse, 37=jambiere, 36=bottes
            int x = ox;
            int y = oy + i * SLOT;
            drawSlot(gfx, x, y, getItem(armorSlot), mouseX, mouseY, armorSlot);
        }

        // ── Offhand : sous l'armure ───────────────────────────────────────────
        drawSlot(gfx, ox, oy + 4 * SLOT - SLOT, getItem(40), mouseX, mouseY, 40);

        // ── Inventaire principal (9 x 3) ──────────────────────────────────────
        int invX = ox + SLOT + PAD;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = 9 + row * 9 + col; // slots 9-35
                drawSlot(gfx, invX + col * SLOT, oy + row * SLOT, getItem(slot), mouseX, mouseY, slot);
            }
        }

        // ── Hotbar (9 x 1) sous le main inventory ────────────────────────────
        gfx.fill(invX - 1, oy + 3 * SLOT + PAD - 1,
                 invX + 9 * SLOT + 1, oy + 3 * SLOT + PAD + SLOT + 1, 0xFF555570);
        for (int col = 0; col < 9; col++) {
            drawSlot(gfx, invX + col * SLOT, oy + 3 * SLOT + PAD, getItem(col), mouseX, mouseY, col);
        }

        super.render(gfx, mouseX, mouseY, partial);

        // ── Tooltip ───────────────────────────────────────────────────────────
        if (hoveredIndex >= 0 && hoveredIndex < allItems.size()) {
            ItemStack stack = allItems.get(hoveredIndex);
            if (!stack.isEmpty()) {
                gfx.renderTooltip(font, stack, mouseX, mouseY);
            }
        }

        // ── Hint fermeture ────────────────────────────────────────────────────
        gfx.drawCenteredString(font, "\u00a77[Echap] Fermer",
            width / 2, oy + panelH + 8, 0x888888);
    }

    private void drawSlot(GuiGraphics gfx, int x, int y, ItemStack stack,
                          int mouseX, int mouseY, int index) {
        boolean hov = mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
        if (hov) hoveredIndex = index;

        // Fond case
        gfx.fill(x,     y,     x + 17, y + 17, 0xFF444455);
        gfx.fill(x + 1, y + 1, x + 17, y + 17, 0xFF2A2A3A);
        if (hov) gfx.fill(x + 1, y + 1, x + 16, y + 16, 0x44FFFFFF);

        if (!stack.isEmpty()) {
            gfx.renderItem(stack, x + 1, y + 1);
            gfx.renderItemDecorations(font, stack, x + 1, y + 1);
        }
    }

    @Override
    public void renderBackground(GuiGraphics gfx) {
        // Fond sombre semi-transparent
        gfx.fill(0, 0, width, height, 0xAA000000);
    }

    private ItemStack getItem(int index) {
        return (index >= 0 && index < allItems.size()) ? allItems.get(index) : ItemStack.EMPTY;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // Reset du slot survole a chaque frame (avant le rendu des slots)
    // On ne peut pas overrider mouseMoved (void en 1.20.1), on remet a zero
    // en debut de render() a la place.
}
