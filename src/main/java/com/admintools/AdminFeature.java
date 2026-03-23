package com.admintools;

/**
 * Toutes les features toggleables via le panel (touche K) ou /at.
 */
public enum AdminFeature {

    // ── Outils visuels (client) ───────────────────────────────────────────────
    ORE_ESP       ("Ore X-Ray ESP",     "Affiche les minerais a travers les blocs"),
    ENTITY_ESP    ("Entity ESP",        "Affiche joueurs / mobs a travers les murs"),

    // ── Outils admin (serveur) ────────────────────────────────────────────────
    VANISH        ("Vanish",            "Rend l'admin invisible pour les joueurs normaux"),
    NOCLIP        ("NoClip Admin",      "Passe en mode spectateur (traverse les blocs). Restaure le mode precedent a la desactivation."),
    ALERTS        ("Alertes Anti-Cheat","Recoit les alertes de violation en temps reel"),

    // ── Detections anti-cheat (serveur) ───────────────────────────────────────
    FLY_DETECT    ("Detection Fly",     "Signale les joueurs qui volent illegalement"),
    NOCLIP_DETECT ("Detection NoClip",  "Signale les joueurs qui se deplacent dans des blocs solides"),
    XRAY_DETECT   ("Detection X-Ray",  "Signale les taux de minage de minerais suspects");

    public final String displayName;
    public final String tooltip;

    AdminFeature(String displayName, String tooltip) {
        this.displayName = displayName;
        this.tooltip     = tooltip;
    }
}
