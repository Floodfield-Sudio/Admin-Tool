package com.admintools;

import com.admintools.server.TopLuckTracker;

import java.util.*;

/**
 * DTO neutre (pas @OnlyIn, pas de superclasse client) pour le classement Top Luck.
 * Serialise via S2CTopLuckPacket, affiche via TopLuckScreen.
 */
public class TopLuckEntry {

    public final String  name;
    public final int     overworldRock;
    public final int     netherRock;
    public final int     endRock;
    /** Groupe ore -> count, trie par count decroissant. */
    public final Map<String, Integer> oreGroups;
    /**
     * Tous les blocs casses (chemin du bloc -> count), tries par count decroissant.
     * Limite a MAX_DETAIL entrees dans le packet.
     */
    public final Map<String, Integer> allBlocks;
    public static final int MAX_DETAIL = 50;

    public TopLuckEntry(String name, int overworldRock, int netherRock, int endRock,
                        Map<String, Integer> oreGroups, Map<String, Integer> allBlocks) {
        this.name          = name;
        this.overworldRock = overworldRock;
        this.netherRock    = netherRock;
        this.endRock       = endRock;
        this.oreGroups     = Collections.unmodifiableMap(oreGroups);
        this.allBlocks     = Collections.unmodifiableMap(allBlocks);
    }

    public int totalOres() {
        return oreGroups.values().stream().mapToInt(i -> i).sum();
    }

    public int totalHostRock() {
        return overworldRock + netherRock + endRock;
    }

    public int totalBroken() {
        return allBlocks.values().stream().mapToInt(i -> i).sum();
    }

    /** Ratio principal : ores / roche_hote. */
    public double ratio() {
        int rock = totalHostRock();
        return rock == 0 ? 0.0 : (double) totalOres() / rock;
    }

    public boolean isSuspect() {
        return totalHostRock() >= TopLuckTracker.MIN_SAMPLE
            && ratio() >= TopLuckTracker.SUSPECT_RATIO;
    }

    public List<Map.Entry<String, Integer>> sortedGroups() {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(oreGroups.entrySet());
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        return list;
    }

    public List<Map.Entry<String, Integer>> sortedAllBlocks() {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(allBlocks.entrySet());
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        return list;
    }
}
