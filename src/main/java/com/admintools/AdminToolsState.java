package com.admintools;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Etat AdminTools cote SERVEUR uniquement.
 * Aucune reference a du code client (@OnlyIn) dans cette classe --
 * elle est chargee sur le serveur dedie et doit compiler sans les classes client.
 *
 * L'etat cote client est dans ClientAdminState (charge uniquement sur le client).
 */
public class AdminToolsState {

    // ── Features actives par admin (UUID -> EnumSet) ──────────────────────────
    private static final Map<UUID, EnumSet<AdminFeature>> adminFeatures = new ConcurrentHashMap<>();

    // ── GameMode sauvegarde avant passage en spec (NoClip admin) ─────────────
    private static final Map<UUID, GameType> savedGameModes = new ConcurrentHashMap<>();

    // ── API features ──────────────────────────────────────────────────────────

    public static boolean isEnabled(UUID uuid, AdminFeature feature) {
        return adminFeatures.getOrDefault(uuid, EnumSet.noneOf(AdminFeature.class))
                            .contains(feature);
    }

    /** Bascule la feature, retourne le nouvel etat. */
    public static boolean toggle(UUID uuid, AdminFeature feature) {
        EnumSet<AdminFeature> set =
            adminFeatures.computeIfAbsent(uuid, k -> EnumSet.noneOf(AdminFeature.class));
        if (set.contains(feature)) { set.remove(feature); return false; }
        else                       { set.add(feature);    return true;  }
    }

    public static EnumSet<AdminFeature> getFeatures(UUID uuid) {
        EnumSet<AdminFeature> src =
            adminFeatures.getOrDefault(uuid, EnumSet.noneOf(AdminFeature.class));
        return src.isEmpty() ? EnumSet.noneOf(AdminFeature.class) : EnumSet.copyOf(src);
    }

    /** Initialise les features par defaut pour un admin qui se connecte. */
    public static void initAdmin(UUID uuid) {
        adminFeatures.computeIfAbsent(uuid, k -> EnumSet.of(
            AdminFeature.ALERTS,
            AdminFeature.FLY_DETECT,
            AdminFeature.NOCLIP_DETECT,
            AdminFeature.XRAY_DETECT
        ));
    }

    public static void removeAdmin(UUID uuid) {
        adminFeatures.remove(uuid);
        savedGameModes.remove(uuid);
    }

    // ── Vanish ────────────────────────────────────────────────────────────────

    public static void applyVanish(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(
            MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "\u00a78[AdminTools] \u00a77Vous etes maintenant \u00a7bvanish\u00a77."));
    }

    public static void removeVanish(ServerPlayer player) {
        player.removeEffect(MobEffects.INVISIBILITY);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "\u00a78[AdminTools] \u00a77Vous etes de nouveau \u00a7cvisible\u00a77."));
    }

    // ── NoClip admin (spectateur) ─────────────────────────────────────────────

    /**
     * Active le NoClip pour un admin :
     *   - sauvegarde son gamemode actuel
     *   - le passe en spectateur (seul mode permettant de traverser les blocs)
     */
    public static void applyNoclip(ServerPlayer player) {
        GameType current = player.gameMode.getGameModeForPlayer();
        savedGameModes.put(player.getUUID(), current);
        player.setGameMode(GameType.SPECTATOR);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "\u00a78[AdminTools] \u00a77NoClip \u00a7aactive\u00a77"
            + " \u00a78(\u00a77mode precedent : \u00a7f" + current.getName() + "\u00a78)"));
    }

    /**
     * Desactive le NoClip : restaure le gamemode sauvegarde
     * (ou SURVIVAL si aucun n'est sauvegarde).
     */
    public static void removeNoclip(ServerPlayer player) {
        GameType previous = savedGameModes.getOrDefault(player.getUUID(), GameType.SURVIVAL);
        savedGameModes.remove(player.getUUID());
        player.setGameMode(previous);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "\u00a78[AdminTools] \u00a77NoClip \u00a7cdesactive\u00a77"
            + " \u00a78(\u00a77retour en : \u00a7f" + previous.getName() + "\u00a78)"));
    }

    // ── Alertes : liste des admins recepteurs ─────────────────────────────────

    public static Set<UUID> getAlertRecipients() {
        Set<UUID> result = new HashSet<>();
        for (Map.Entry<UUID, EnumSet<AdminFeature>> e : adminFeatures.entrySet()) {
            if (e.getValue().contains(AdminFeature.ALERTS)) result.add(e.getKey());
        }
        return result;
    }
}
