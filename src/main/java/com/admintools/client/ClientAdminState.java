package com.admintools.client;

import com.admintools.AdminFeature;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Etat AdminTools cote CLIENT uniquement.
 *
 * Cette classe n'est JAMAIS referencee depuis du code commun (ServerEvents,
 * AdminToolsState, etc.) -- uniquement depuis AdminPanel, ESPRenderer,
 * ClientEvents et les packets S2C.
 *
 * Cela evite le NoSuchFieldError sur serveur dedie qui survenait quand
 * AdminToolsState contenait des champs @OnlyIn(Dist.CLIENT) : le
 * chargeur de classes du serveur initialisait quand meme AdminToolsState
 * et plantait sur les champs absents.
 */
public final class ClientAdminState {

    private ClientAdminState() {}

    /** Features actives pour le joueur admin local. */
    public static EnumSet<AdminFeature> features = EnumSet.noneOf(AdminFeature.class);

    /** Log des violations recues via S2CViolationPacket. */
    public static final List<String> violationLog = new ArrayList<>();

    // ── Accesseurs ────────────────────────────────────────────────────────────

    public static boolean isEnabled(AdminFeature feature) {
        return features.contains(feature);
    }

    public static void setFeatures(Set<AdminFeature> incoming) {
        features = incoming.isEmpty()
            ? EnumSet.noneOf(AdminFeature.class)
            : EnumSet.copyOf(incoming);
    }

    public static void addViolation(String msg) {
        violationLog.add(0, msg);
        if (violationLog.size() > 50) violationLog.remove(violationLog.size() - 1);
    }

    public static void clear() {
        features = EnumSet.noneOf(AdminFeature.class);
        violationLog.clear();
    }
}
