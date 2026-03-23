package com.admintools.client;

import com.admintools.AdminFeature;
import com.admintools.client.render.ESPRenderer;
import com.admintools.network.NetworkHandler;
import com.admintools.network.packets.C2SInvSeePacket;
import com.admintools.network.packets.C2STogglePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Panel admin (touche K) -- 3 onglets.
 *
 * [Features]  -- toggles ESP / Admin / Anti-Cheat
 * [Joueurs]   -- liste des joueurs en ligne, TP / InvSee / Check
 * [Violations]-- log des alertes anti-cheat
 */
public class AdminPanel extends Screen {

    // ── Couleurs ──────────────────────────────────────────────────────────────
    private static final int BG       = 0xDD0A0A14;
    private static final int BORDER   = 0xFF3A3A5A;
    private static final int TITLE    = 0xFF89B4FA;
    private static final int SEC      = 0xFF6C7086;
    private static final int LOG_BG   = 0xAA060610;
    private static final int LOG_TEXT = 0xFFCDD6F4;
    private static final int TAB_ON   = 0xFF313250;
    private static final int TAB_OFF  = 0xFF1A1A2A;

    private static final int W = 450, H = 350;
    private static final int BTN_W = 134, BTN_H = 20;

    // ── Etat ──────────────────────────────────────────────────────────────────
    private enum Tab { FEATURES, PLAYERS, VIOLATIONS }
    private Tab currentTab = Tab.FEATURES;

    private int     logScroll       = 0;
    private String  selectedPlayer  = null;
    private int     playerScroll    = 0;

    private static final int LOG_LINES = 7;
    private static final int PLAYER_LINES = 8;

    private final List<ToggleButton> toggleButtons = new ArrayList<>();

    public AdminPanel() {
        super(Component.literal("AdminTools"));
    }

    // ── Init ------------------------------------------------------------------

    @Override
    protected void init() {
        clearWidgets();
        toggleButtons.clear();

        int px = (width - W) / 2;
        int py = (height - H) / 2;

        // ── Onglets ───────────────────────────────────────────────────────────
        addRenderableWidget(Button.builder(Component.literal("Features"),
            b -> setTab(Tab.FEATURES)).bounds(px + 2, py + 2, 96, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Joueurs"),
            b -> setTab(Tab.PLAYERS)).bounds(px + 100, py + 2, 96, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Violations"),
            b -> setTab(Tab.VIOLATIONS)).bounds(px + 198, py + 2, 96, 18).build());

        // Bouton Fermer (toujours visible)
        addRenderableWidget(Button.builder(Component.literal("Fermer"),
            b -> onClose()).bounds(px + W - 72, py + 2, 68, 18).build());

        switch (currentTab) {
            case FEATURES   -> initFeatures(px, py);
            case PLAYERS    -> initPlayers(px, py);
            case VIOLATIONS -> initViolations(px, py);
        }
    }

    private void setTab(Tab tab) {
        currentTab = tab;
        logScroll = 0;
        init();
    }

    // ── Onglet Features ───────────────────────────────────────────────────────

    private void initFeatures(int px, int py) {
        int y1 = py + 46;
        // Ligne 1 : ESP
        addToggle(AdminFeature.ORE_ESP,     px + 6, y1);
        addToggle(AdminFeature.ENTITY_ESP,  px + 6 + BTN_W + 4, y1);

        // Ligne 2 : Admin tools
        int y2 = y1 + BTN_H + 4;
        addToggle(AdminFeature.VANISH,  px + 6, y2);
        addToggle(AdminFeature.NOCLIP,  px + 6 + BTN_W + 4, y2);
        addToggle(AdminFeature.ALERTS,  px + 6 + (BTN_W + 4) * 2, y2);

        // Ligne 3 : Anti-cheat
        int y3 = y2 + BTN_H + 4;
        addToggle(AdminFeature.FLY_DETECT,    px + 6, y3);
        addToggle(AdminFeature.NOCLIP_DETECT, px + 6 + BTN_W + 4, y3);
        addToggle(AdminFeature.XRAY_DETECT,   px + 6 + (BTN_W + 4) * 2, y3);
    }

    private void addToggle(AdminFeature feature, int x, int y) {
        ToggleButton btn = new ToggleButton(feature, x, y, BTN_W, BTN_H);
        toggleButtons.add(btn);
        addRenderableWidget(btn);
    }

    // ── Onglet Joueurs ────────────────────────────────────────────────────────

    private void initPlayers(int px, int py) {
        int listY  = py + 26;
        int listH  = PLAYER_LINES * 12 + 4;
        int actionY = listY + listH + 6;

        // Boutons scroll liste
        addRenderableWidget(Button.builder(Component.literal("\u25b2"),
            b -> { if (playerScroll > 0) playerScroll--; })
            .bounds(px + W - 30, listY, 22, 12).build());
        addRenderableWidget(Button.builder(Component.literal("\u25bc"),
            b -> playerScroll++)
            .bounds(px + W - 30, listY + listH - 12, 22, 12).build());

        // Boutons actions (actifs si joueur selectionne)
        addRenderableWidget(Button.builder(Component.literal("\u2708 TP"),
            b -> doTp()).bounds(px + 6, actionY, 68, 20).build());

        addRenderableWidget(Button.builder(Component.literal("\uD83C\uDF92 InvSee"),
            b -> doInvSee()).bounds(px + 78, actionY, 80, 20).build());

        addRenderableWidget(Button.builder(Component.literal("\uD83D\uDD0E Check"),
            b -> doCheck()).bounds(px + 162, actionY, 80, 20).build());
    }

    private List<String> getOnlinePlayers() {
        Minecraft mc = Minecraft.getInstance();
        List<String> names = new ArrayList<>();
        if (mc.getConnection() != null) {
            for (PlayerInfo pi : mc.getConnection().getOnlinePlayers())
                names.add(pi.getProfile().getName());
            names.sort(String.CASE_INSENSITIVE_ORDER);
        }
        return names;
    }

    private void doTp() {
        if (selectedPlayer == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.connection.sendCommand("at tp " + selectedPlayer);
            onClose();
        }
    }

    private void doInvSee() {
        if (selectedPlayer == null) return;
        NetworkHandler.CHANNEL.sendToServer(new C2SInvSeePacket(selectedPlayer));
        onClose();
    }

    private void doCheck() {
        if (selectedPlayer == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null)
            mc.player.connection.sendCommand("at check " + selectedPlayer);
    }

    // ── Onglet Violations ─────────────────────────────────────────────────────

    private void initViolations(int px, int py) {
        int bottomY = py + H - 26;
        addRenderableWidget(Button.builder(Component.literal("Effacer log"),
            b -> { ClientAdminState.violationLog.clear(); logScroll = 0; })
            .bounds(px + 6, bottomY, 100, 20).build());
    }

    // ── Rendu -----------------------------------------------------------------

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        int px = (width - W) / 2;
        int py = (height - H) / 2;

        renderBackground(gfx);
        gfx.fill(px - 1, py - 1, px + W + 1, py + H + 1, BORDER);
        gfx.fill(px,     py,     px + W,     py + H,     BG);

        // Titre
        gfx.drawCenteredString(font,
            "\u00a7b\u00a7lAdminTools \u00a7r\u00a77\u2014 " + tabLabel(),
            px + W / 2, py + 8, TITLE);

        // Separateur sous les onglets
        gfx.fill(px + 2, py + 22, px + W - 2, py + 23, BORDER);

        switch (currentTab) {
            case FEATURES   -> renderFeatures(gfx, px, py, mouseX, mouseY);
            case PLAYERS    -> renderPlayers(gfx, px, py, mouseX, mouseY);
            case VIOLATIONS -> renderViolations(gfx, px, py);
        }

        super.render(gfx, mouseX, mouseY, partial);

        // Tooltips features
        if (currentTab == Tab.FEATURES) {
            for (ToggleButton tb : toggleButtons) {
                if (tb.isHoveredOrFocused()) {
                    List<FormattedCharSequence> lines = font.split(
                        Component.literal("\u00a77" + tb.feature.tooltip), W - 20);
                    gfx.renderTooltip(font, lines, mouseX, mouseY);
                    break;
                }
            }
        }
    }

    private String tabLabel() {
        return switch (currentTab) {
            case FEATURES   -> "Features";
            case PLAYERS    -> "Joueurs en ligne";
            case VIOLATIONS -> "Violations (" + ClientAdminState.violationLog.size() + ")";
        };
    }

    // ── Rendu par onglet ──────────────────────────────────────────────────────

    private void renderFeatures(GuiGraphics gfx, int px, int py, int mx, int my) {
        int y1 = py + 38;
        gfx.drawString(font, "\u00a77ESP & Visibilite",      px + 8, y1 - 10, SEC, false);
        gfx.drawString(font, "\u00a77Outils Admin",          px + 8, y1 + BTN_H + 4 - 10, SEC, false);
        gfx.drawString(font, "\u00a77Detections Anti-Cheat", px + 8, y1 + (BTN_H + 4) * 2 - 10, SEC, false);

        // Info noclip
        gfx.drawString(font,
            "\u00a78* NOCLIP Admin = spectateur (restaure le mode precedent a la desactivation)",
            px + 8, py + H - 14, 0xFF444466, false);
    }

    private void renderPlayers(GuiGraphics gfx, int px, int py, int mx, int my) {
        int listX  = px + 6;
        int listY  = py + 26;
        int listW  = W - 40;

        List<String> players = getOnlinePlayers();
        int maxScroll = Math.max(0, players.size() - PLAYER_LINES);
        playerScroll = Math.min(playerScroll, maxScroll);

        gfx.fill(listX, listY, listX + listW, listY + PLAYER_LINES * 12 + 4, 0xAA060610);

        for (int i = playerScroll; i < Math.min(players.size(), playerScroll + PLAYER_LINES); i++) {
            String name = players.get(i);
            int y   = listY + 2 + (i - playerScroll) * 12;
            boolean sel = name.equals(selectedPlayer);
            boolean hov = mx >= listX && mx < listX + listW && my >= y && my < y + 12;
            if (sel) gfx.fill(listX, y, listX + listW, y + 12, 0x66A0C0FF);
            else if (hov) gfx.fill(listX, y, listX + listW, y + 12, 0x33FFFFFF);
            gfx.drawString(font, (sel ? "\u00a7b\u00a7l> " : "\u00a77") + name, listX + 4, y + 2, 0xFFFFFF, false);
        }

        // Joueur selectionne
        if (selectedPlayer != null) {
            gfx.drawString(font, "\u00a77Selectionne : \u00a7f" + selectedPlayer,
                listX, listY + PLAYER_LINES * 12 + 8, SEC, false);
        } else {
            gfx.drawString(font, "\u00a77Cliquez sur un joueur pour le selectionner",
                listX, listY + PLAYER_LINES * 12 + 8, SEC, false);
        }
    }

    private void renderViolations(GuiGraphics gfx, int px, int py) {
        int logY = py + 26;
        List<String> log = ClientAdminState.violationLog;

        gfx.fill(px + 4, logY, px + W - 4, logY + LOG_LINES * 12 + 4, LOG_BG);

        for (int i = logScroll; i < Math.min(log.size(), logScroll + LOG_LINES); i++) {
            String line = log.get(i);
            String colored;
            if      (line.contains("FLY"))    colored = "\u00a7c" + line;
            else if (line.contains("NOCLIP")) colored = "\u00a76" + line;
            else if (line.contains("XRAY"))   colored = "\u00a7e" + line;
            else                              colored = "\u00a7f" + line;
            gfx.drawString(font, colored, px + 6, logY + 2 + (i - logScroll) * 12, LOG_TEXT, false);
        }

        if (log.isEmpty()) {
            gfx.drawCenteredString(font, "\u00a77Aucune violation enregistree",
                px + W / 2, logY + LOG_LINES * 6, 0x888888);
        }
    }

    // ── Interactions ----------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (currentTab == Tab.PLAYERS) {
            int px = (width - W) / 2;
            int py = (height - H) / 2;
            int listX = px + 6;
            int listY = py + 26;
            int listW = W - 40;
            List<String> players = getOnlinePlayers();

            if (mx >= listX && mx < listX + listW) {
                for (int i = playerScroll; i < Math.min(players.size(), playerScroll + PLAYER_LINES); i++) {
                    int y = listY + 2 + (i - playerScroll) * 12;
                    if (my >= y && my < y + 12) {
                        String name = players.get(i);
                        selectedPlayer = name.equals(selectedPlayer) ? null : name;
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        switch (currentTab) {
            case VIOLATIONS -> {
                int max = Math.max(0, ClientAdminState.violationLog.size() - LOG_LINES);
                logScroll = (int) Math.max(0, Math.min(logScroll - delta, max));
            }
            case PLAYERS -> {
                int max = Math.max(0, getOnlinePlayers().size() - PLAYER_LINES);
                playerScroll = (int) Math.max(0, Math.min(playerScroll - delta, max));
            }
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ── Bouton toggle interne ─────────────────────────────────────────────────

    private static class ToggleButton extends Button {
        final AdminFeature feature;

        ToggleButton(AdminFeature feature, int x, int y, int w, int h) {
            super(x, y, w, h, buildLabel(feature), btn -> {
                NetworkHandler.CHANNEL.sendToServer(new C2STogglePacket(feature));
                if (feature == AdminFeature.ORE_ESP || feature == AdminFeature.ENTITY_ESP)
                    ESPRenderer.invalidateCache();
            }, Button.DEFAULT_NARRATION);
            this.feature = feature;
        }

        @Override
        public void render(GuiGraphics gfx, int mx, int my, float partial) {
            setMessage(buildLabel(feature));
            super.render(gfx, mx, my, partial);
        }

        private static Component buildLabel(AdminFeature f) {
            boolean on = ClientAdminState.isEnabled(f);
            return Component.literal((on ? "\u00a7a[ON]  " : "\u00a7c[OFF] ") + f.displayName);
        }
    }
}
