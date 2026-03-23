package com.admintools.client;

import com.admintools.TopLuckEntry;
import com.admintools.server.TopLuckTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;

/**
 * Ecran Top Luck.
 *
 * Panel gauche : liste des joueurs triee par ratio (ores / roche hote).
 * Panel droit  : detail du joueur selectionne avec 2 onglets :
 *   [Ores]        -- barres par groupe de minerai
 *   [Tous blocs]  -- tous les blocs casses (tri decroissant)
 */
public class TopLuckScreen extends Screen {

    // ── Geometrie ─────────────────────────────────────────────────────────────
    private static final int W = 480, H = 320;
    private static final int LIST_W  = 148;
    private static final int DET_X   = LIST_W + 10;
    private static final int DET_W   = W - DET_X - 4;
    private static final int ROW_H   = 22;
    private static final int LIST_ROWS = 10;

    // ── Couleurs ──────────────────────────────────────────────────────────────
    private static final int C_BG     = 0xDD0A0A14;
    private static final int C_BORDER = 0xFF3A3A5A;
    private static final int C_PANEL  = 0xAA060610;
    private static final int C_DIM    = 0xFF6C7086;
    private static final int C_SEL    = 0x6689B4FA;
    private static final int C_HOV    = 0x33FFFFFF;
    private static final int C_TAB_ON = 0xFF313250;

    // ── Etat ──────────────────────────────────────────────────────────────────
    private final List<TopLuckEntry> entries;
    private int     selectedIndex = -1;
    private int     listScroll    = 0;
    private int     detScroll     = 0;
    private boolean showAllBlocks = false;   // false = onglet Ores, true = Tous blocs

    private static final int DET_ROWS = 10;  // lignes visibles dans le panneau detail

    public TopLuckScreen(List<TopLuckEntry> entries) {
        super(Component.literal("Top Luck"));
        this.entries = entries.stream()
            .sorted((a, b) -> Double.compare(b.ratio(), a.ratio()))
            .toList();
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        int px = (width - W) / 2, py = (height - H) / 2;

        // Onglets panneau detail
        addRenderableWidget(Button.builder(Component.literal("Ores"),
            b -> { showAllBlocks = false; detScroll = 0; })
            .bounds(px + DET_X, py + 24, 60, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Tous les blocs"),
            b -> { showAllBlocks = true; detScroll = 0; })
            .bounds(px + DET_X + 64, py + 24, 100, 16).build());

        // Fermer
        addRenderableWidget(Button.builder(Component.literal("Fermer"),
            b -> onClose())
            .bounds(px + W - 72, py + H - 24, 68, 20).build());
    }

    // ── Rendu ─────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float partial) {
        int px = (width - W) / 2, py = (height - H) / 2;

        renderBackground(gfx);
        gfx.fill(px - 1, py - 1, px + W + 1, py + H + 1, C_BORDER);
        gfx.fill(px, py, px + W, py + H, C_BG);

        // Titre
        gfx.drawCenteredString(font,
            "\u00a7e\u00a7l\u265b Top Luck \u00a7r\u00a77\u2014 Ores / Roche h\u00f4te",
            px + W / 2, py + 6, 0xFFFFDD00);
        gfx.fill(px + 4, py + 20, px + W - 4, py + 21, C_BORDER);

        renderList(gfx, px, py, mx, my);
        renderDetail(gfx, px, py, mx, my);

        // Note ratio
        gfx.drawString(font,
            "\u00a78Ratio = ores / (pierre+netherrack+end stone)  |  Suspect \u2265 "
            + (int)(TopLuckTracker.SUSPECT_RATIO * 100) + "%  |  /at topluck reset <joueur>",
            px + 4, py + H - 34, C_DIM, false);

        super.render(gfx, mx, my, partial);
    }

    // ── Liste joueurs ─────────────────────────────────────────────────────────

    private void renderList(GuiGraphics gfx, int px, int py, int mx, int my) {
        int listY = py + 24;
        gfx.fill(px + 2, listY, px + LIST_W, listY + LIST_ROWS * ROW_H + 2, C_PANEL);

        int maxScroll = Math.max(0, entries.size() - LIST_ROWS);
        listScroll = Math.min(listScroll, maxScroll);

        for (int i = listScroll; i < Math.min(entries.size(), listScroll + LIST_ROWS); i++) {
            TopLuckEntry e = entries.get(i);
            int ry  = listY + (i - listScroll) * ROW_H;
            boolean sel = (i == selectedIndex);
            boolean hov = mx >= px + 2 && mx < px + LIST_W && my >= ry && my < ry + ROW_H;

            if (sel) gfx.fill(px + 2, ry, px + LIST_W, ry + ROW_H, C_SEL);
            else if (hov) gfx.fill(px + 2, ry, px + LIST_W, ry + ROW_H, C_HOV);

            gfx.drawString(font,
                "\u00a78#" + (i + 1) + " \u00a7f" + trunc(e.name, 10),
                px + 4, ry + 3, 0xFFFFFF, false);
            gfx.drawString(font,
                ratioColor(e.ratio()) + String.format("%.1f%%", e.ratio() * 100)
                + (e.isSuspect() ? " \u00a7c!" : ""),
                px + 4, ry + 12, 0xFFFFFF, false);
        }

        // Scrollbar
        if (entries.size() > LIST_ROWS) {
            int sbH = LIST_ROWS * ROW_H;
            int tH  = Math.max(14, sbH * LIST_ROWS / entries.size());
            float pct = maxScroll == 0 ? 0 : (float) listScroll / maxScroll;
            int tY  = (int)(pct * (sbH - tH));
            gfx.fill(px + LIST_W, listY, px + LIST_W + 3, listY + sbH, 0xFF222233);
            gfx.fill(px + LIST_W, listY + tY, px + LIST_W + 3, listY + tY + tH, 0xFF6666AA);
        }
    }

    // ── Panneau detail ────────────────────────────────────────────────────────

    private void renderDetail(GuiGraphics gfx, int px, int py, int mx, int my) {
        int detX = px + DET_X;
        int detY = py + 43;   // sous les onglets
        int detH = DET_ROWS * 12 + 4;

        // Surbrillance onglet actif
        if (!showAllBlocks)
            gfx.fill(px + DET_X, py + 24, px + DET_X + 60, py + 40, C_TAB_ON);
        else
            gfx.fill(px + DET_X + 64, py + 24, px + DET_X + 164, py + 40, C_TAB_ON);

        gfx.fill(detX, detY, detX + DET_W, detY + detH, C_PANEL);

        if (selectedIndex < 0 || selectedIndex >= entries.size()) {
            if (entries.isEmpty()) {
                gfx.drawCenteredString(font,
                    "\u00a77Aucune donnee (min " + TopLuckTracker.MIN_SAMPLE + " blocs de roche)",
                    detX + DET_W / 2, detY + detH / 2 - 4, C_DIM);
            } else {
                gfx.drawCenteredString(font,
                    "\u00a77Cliquez sur un joueur",
                    detX + DET_W / 2, detY + detH / 2 - 4, C_DIM);
            }
            return;
        }

        TopLuckEntry e = entries.get(selectedIndex);

        if (!showAllBlocks) {
            renderOresTab(gfx, e, detX + 2, detY + 2, DET_W - 4);
        } else {
            renderAllBlocksTab(gfx, e, detX + 2, detY + 2, DET_W - 4);
        }
    }

    // ── Onglet Ores ───────────────────────────────────────────────────────────

    private void renderOresTab(GuiGraphics gfx, TopLuckEntry e, int x, int y, int w) {
        // Header stats
        gfx.drawString(font, "\u00a7f\u00a7l" + e.name, x, y, 0xFFFFFF, false);

        String ratioStr = ratioColor(e.ratio()) + String.format("%.1f%%", e.ratio() * 100);
        gfx.drawString(font,
            "\u00a77Ores : " + ratioStr + " \u00a78| \u00a77Roche : \u00a7f" + e.totalHostRock()
            + (e.isSuspect() ? "  \u00a7c\u26a0 SUSPECT" : ""),
            x, y + 10, 0xFFFFFF, false);

        // Roche hote par dimension
        gfx.drawString(font,
            "\u00a78Pierre: \u00a7f" + e.overworldRock
            + "  \u00a78Netherrack: \u00a7f" + e.netherRock
            + "  \u00a78End Stone: \u00a7f" + e.endRock,
            x, y + 20, 0xFFFFFF, false);

        gfx.fill(x, y + 31, x + w, y + 32, C_BORDER);

        // Barres par groupe d'ore
        List<Map.Entry<String, Integer>> groups = e.sortedGroups();
        int bY = y + 36;
        int maxCount = groups.isEmpty() ? 1 : groups.get(0).getValue();
        int maxBar   = w - 72;

        int maxRows = (DET_ROWS * 12 - 40) / 12;
        for (int i = detScroll; i < Math.min(groups.size(), detScroll + maxRows); i++) {
            Map.Entry<String, Integer> g = groups.get(i);
            int cnt    = g.getValue();
            int barLen = Math.max(2, cnt * maxBar / maxCount);
            double pct = e.totalHostRock() == 0 ? 0 : (double) cnt / e.totalHostRock();
            int color  = oreColor(g.getKey());

            gfx.drawString(font, "\u00a77" + trunc(stripCodes(g.getKey()), 9),
                x, bY + 1, 0xFFFFFF, false);
            int bx = x + 62;
            gfx.fill(bx, bY + 1, bx + barLen, bY + 9, color);
            gfx.fill(bx + barLen, bY + 1, bx + maxBar, bY + 9, 0xFF1A1A2A);
            gfx.drawString(font,
                "\u00a7f" + cnt + " \u00a78(" + String.format("%.1f%%", pct * 100) + ")",
                bx + maxBar + 2, bY + 1, 0xFFFFFF, false);
            bY += 12;
        }

        renderScrollbar(gfx, x + w - 3, y + 36, (DET_ROWS * 12 - 40), groups.size(), maxRows);
    }

    // ── Onglet Tous les blocs ─────────────────────────────────────────────────

    private void renderAllBlocksTab(GuiGraphics gfx, TopLuckEntry e, int x, int y, int w) {
        gfx.drawString(font,
            "\u00a7f\u00a7l" + e.name + " \u00a7r\u00a77\u2014 "
            + e.totalBroken() + " blocs au total (" + e.allBlocks.size() + " types)",
            x, y, 0xFFFFFF, false);
        gfx.fill(x, y + 11, x + w, y + 12, C_BORDER);

        List<Map.Entry<String, Integer>> allSorted = e.sortedAllBlocks();
        int total = e.totalBroken();
        int maxCount = allSorted.isEmpty() ? 1 : allSorted.get(0).getValue();
        int maxBar   = w - 100;
        int bY  = y + 16;
        int maxRows = (DET_ROWS * 12 - 20) / 12;

        for (int i = detScroll; i < Math.min(allSorted.size(), detScroll + maxRows); i++) {
            Map.Entry<String, Integer> bl = allSorted.get(i);
            String path  = bl.getKey();
            int    cnt   = bl.getValue();
            int    barLen = Math.max(1, cnt * maxBar / maxCount);
            double pct   = total == 0 ? 0 : (double) cnt / total * 100;

            // Couleur : vert pour ores, gris pour roche hote, blanc pour reste
            int color;
            if (path.contains("_ore") || path.equals("ancient_debris")) color = 0xFF00FFAA;
            else if (path.equals("stone") || path.equals("deepslate")
                  || path.equals("netherrack") || path.equals("end_stone")) color = 0xFF778899;
            else color = 0xFF556677;

            gfx.drawString(font, "\u00a77" + trunc(path, 12), x, bY + 1, 0xFFFFFF, false);
            int bx = x + 84;
            gfx.fill(bx, bY + 1, bx + barLen, bY + 9, color);
            gfx.fill(bx + barLen, bY + 1, bx + maxBar, bY + 9, 0xFF1A1A2A);
            gfx.drawString(font,
                "\u00a7f" + cnt + " \u00a78(" + String.format("%.1f%%", pct) + ")",
                bx + maxBar + 2, bY + 1, 0xFFFFFF, false);
            bY += 12;
        }

        renderScrollbar(gfx, x + w - 3, y + 16, (DET_ROWS * 12 - 20),
            allSorted.size(), maxRows);
    }

    private void renderScrollbar(GuiGraphics gfx, int x, int y, int h, int total, int visible) {
        if (total <= visible) return;
        int tH  = Math.max(12, h * visible / total);
        int max = total - visible;
        float pct = max == 0 ? 0 : (float) detScroll / max;
        int tY = (int)(pct * (h - tH));
        gfx.fill(x, y, x + 3, y + h, 0xFF222233);
        gfx.fill(x, y + tY, x + 3, y + tY + tH, 0xFF6666AA);
    }

    // ── Helpers couleurs ──────────────────────────────────────────────────────

    private static String ratioColor(double r) {
        if (r >= TopLuckTracker.SUSPECT_RATIO) return "\u00a7c";
        if (r >= 0.18) return "\u00a76";
        return "\u00a7a";
    }

    private static int oreColor(String g) {
        String l = g.toLowerCase();
        if (l.contains("diamond"))  return 0xFF00E5FF;
        if (l.contains("emerald"))  return 0xFF00FF40;
        if (l.contains("gold"))     return 0xFFFFD700;
        if (l.contains("iron"))     return 0xFFCC9966;
        if (l.contains("coal"))     return 0xFF555555;
        if (l.contains("copper"))   return 0xFFCC7733;
        if (l.contains("redstone")) return 0xFFFF2222;
        if (l.contains("lapis"))    return 0xFF2244EE;
        if (l.contains("ancient") || l.contains("debris")) return 0xFFCC4411;
        if (l.contains("quartz"))   return 0xFFEEEECC;
        int h = Math.abs(g.hashCode());
        return 0xFF000000 | ((80+h%176)<<16) | ((80+(h>>8)%176)<<8) | (80+(h>>16)%176);
    }

    private static String trunc(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n-1) + ".";
    }

    private static String stripCodes(String s) {
        return s.replaceAll("\u00a7[0-9a-fk-or]", "");
    }

    // ── Interactions ──────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int px = (width - W) / 2, py = (height - H) / 2;
        int listY = py + 24;

        if (mx >= px + 2 && mx < px + LIST_W) {
            for (int i = listScroll; i < Math.min(entries.size(), listScroll + LIST_ROWS); i++) {
                int ry = listY + (i - listScroll) * ROW_H;
                if (my >= ry && my < ry + ROW_H) {
                    selectedIndex = (selectedIndex == i) ? -1 : i;
                    detScroll = 0;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int px = (width - W) / 2;
        if (mx < px + LIST_W) {
            // Scroll liste joueurs
            listScroll = (int) Math.max(0,
                Math.min(listScroll - delta, Math.max(0, entries.size() - LIST_ROWS)));
        } else {
            // Scroll panneau detail
            int total = selectedIndex >= 0 && selectedIndex < entries.size()
                ? (showAllBlocks ? entries.get(selectedIndex).allBlocks.size()
                                 : entries.get(selectedIndex).oreGroups.size())
                : 0;
            int maxRows = 6;
            detScroll = (int) Math.max(0, Math.min(detScroll - delta, Math.max(0, total - maxRows)));
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
