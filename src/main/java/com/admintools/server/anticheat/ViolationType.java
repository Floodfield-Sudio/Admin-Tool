package com.admintools.server.anticheat;

/** Types de violations detectes par l'anti-cheat. */
public enum ViolationType {
    FLY    ("§cFLY",     "Vol illegal detecte"),
    NOCLIP ("§6NOCLIP",  "Deplacement dans un bloc solide"),
    XRAY   ("§eX-RAY",   "Taux de minage de minerais suspect");

    public final String coloredName;
    public final String description;

    ViolationType(String coloredName, String description) {
        this.coloredName  = coloredName;
        this.description  = description;
    }
}
