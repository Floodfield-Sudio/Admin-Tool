package com.admintools.server;

import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Suit les statistiques de minage par joueur.
 *
 * RATIO INTELLIGENT par dimension :
 *   - Minerais Overworld  / Roche Overworld  (stone, deepslate, granite, etc.)
 *   - Minerais Nether     / Netherrack       (netherrack, blackstone, basalt)
 *   - Minerais End        / End Stone        (end_stone, end_stone_bricks)
 *   Le ratio global = total_ores / total_roche_hote
 *
 * Tous les blocs casses sont enregistres pour l'affichage detail.
 * Detection automatique des ores de mods.
 */
public class TopLuckTracker {

    public static final int    MIN_SAMPLE    = 20;
    public static final double SUSPECT_RATIO = 0.28;

    // ── Classification des roches hotes ───────────────────────────────────────

    public enum DimCategory { OVERWORLD, NETHER, END, OTHER }

    /** Detecte si un bloc est une roche hote (denominateur du ratio). */
    public static DimCategory getHostRockCategory(ResourceLocation id) {
        String p = id.getPath();
        // Overworld rock
        if (p.equals("stone") || p.equals("deepslate") || p.equals("cobblestone")
         || p.equals("granite") || p.equals("diorite") || p.equals("andesite")
         || p.equals("tuff") || p.equals("calcite") || p.equals("gravel")
         || p.equals("dirt") || p.equals("cobbled_deepslate"))
            return DimCategory.OVERWORLD;
        // Nether rock
        if (p.equals("netherrack") || p.equals("basalt") || p.equals("blackstone")
         || p.equals("soul_sand") || p.equals("soul_soil") || p.equals("nether_bricks"))
            return DimCategory.NETHER;
        // End rock
        if (p.equals("end_stone") || p.equals("end_stone_bricks"))
            return DimCategory.END;
        return DimCategory.OTHER;
    }

    /**
     * Determine la categorie d'un minerai pour lui associer la bonne roche hote.
     * Detection automatique : prefixes et namespaces nether/end.
     */
    public static DimCategory getOreDimCategory(ResourceLocation id) {
        String p  = id.getPath();
        String ns = id.getNamespace();
        // Nether (vanilla + mods avec "nether" dans le nom)
        if (p.startsWith("nether_") || p.equals("ancient_debris")
         || p.contains("nether") || p.contains("quartz"))
            return DimCategory.NETHER;
        // End (mods avec "end" ou "chorus" ou "shulker" dans le nom)
        if (p.startsWith("end_") || p.contains("_end_") || p.contains("chorus"))
            return DimCategory.END;
        return DimCategory.OVERWORLD;
    }

    // ── Normalisation des noms ─────────────────────────────────────────────────

    private static final List<String> STRIP_PREFIXES = List.of(
        "deepslate_", "nether_", "end_", "dense_", "poor_", "rich_",
        "deep_", "raw_", "budding_", "infested_", "gilded_",
        "exposed_", "weathered_", "oxidized_"
    );

    public static boolean isOre(ResourceLocation id) {
        String path = id.getPath();
        return path.contains("_ore") || path.equals("ancient_debris");
    }

    public static String getOreGroup(ResourceLocation id) {
        String path = id.getPath();
        if (path.equals("ancient_debris"))   return "Ancient Debris";
        if (path.equals("nether_gold_ore"))  return "Gold";
        if (path.equals("gilded_blackstone"))return "Gold";

        String stripped = path;
        for (String prefix : STRIP_PREFIXES) {
            if (stripped.startsWith(prefix)) {
                stripped = stripped.substring(prefix.length()); break;
            }
        }
        if (stripped.endsWith("_ore"))
            stripped = stripped.substring(0, stripped.length() - 4);

        String[] words = stripped.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) sb.append(w.substring(1));
            }
        }
        String ns = id.getNamespace();
        String result = sb.toString();
        if (!ns.equals("minecraft") && !result.isEmpty())
            result = result + " \u00a78(" + ns + ")";
        return result.isEmpty() ? path : result;
    }

    // ── Donnees par joueur ────────────────────────────────────────────────────

    public static class Entry {
        public final String name;

        /** Groupe d'ore -> nombre de blocs casses */
        public final Map<String, Integer> oreGroups = new LinkedHashMap<>();

        /**
         * Tous les blocs casses (ore ET non-ore), chemin du bloc -> count.
         * Utilise pour l'affichage detail "tous les blocs".
         * Limite a MAX_BLOCK_TYPES entrees pour eviter la memoire infinie.
         */
        public final Map<String, Integer> allBlocks = new LinkedHashMap<>();
        public static final int MAX_BLOCK_TYPES = 80;

        /** Roche hote cassee par dimension. */
        public int overworldRock = 0;
        public int netherRock    = 0;
        public int endRock       = 0;

        Entry(String name) { this.name = name; }

        public int totalOres() {
            return oreGroups.values().stream().mapToInt(i -> i).sum();
        }

        /** Roche hote totale = denominateur du ratio. */
        public int totalHostRock() {
            return overworldRock + netherRock + endRock;
        }

        /** Nombre total de blocs casses (ores + tout le reste). */
        public int totalBroken() {
            return allBlocks.values().stream().mapToInt(i -> i).sum();
        }

        /**
         * Ratio principal : ores / roche_hote.
         * Beaucoup plus precis que ores / tous_les_blocs car
         * ca ne penalise pas les joueurs qui cassent du bois, de la terre, etc.
         */
        public double ratio() {
            int rock = totalHostRock();
            return rock == 0 ? 0.0 : (double) totalOres() / rock;
        }

        public boolean isSuspect() {
            return totalHostRock() >= MIN_SAMPLE && ratio() >= SUSPECT_RATIO;
        }

        public List<Map.Entry<String, Integer>> sortedGroups() {
            List<Map.Entry<String, Integer>> list = new ArrayList<>(oreGroups.entrySet());
            list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            return list;
        }

        /** Blocs non-ore tries par count decroissant (pour l'affichage detail). */
        public List<Map.Entry<String, Integer>> sortedAllBlocks() {
            List<Map.Entry<String, Integer>> list = new ArrayList<>(allBlocks.entrySet());
            list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            return list;
        }
    }

    private static final Map<UUID, Entry> stats = new ConcurrentHashMap<>();

    // ── API publique ──────────────────────────────────────────────────────────

    public static void recordOre(UUID id, String playerName, ResourceLocation blockId) {
        Entry e = stats.computeIfAbsent(id, k -> new Entry(playerName));
        // Groupe ore
        String group = getOreGroup(blockId);
        e.oreGroups.merge(group, 1, Integer::sum);
        // Tous les blocs
        recordAllBlocks(e, blockId.getPath());
    }

    public static void recordBlock(UUID id, String playerName, ResourceLocation blockId) {
        Entry e = stats.computeIfAbsent(id, k -> new Entry(playerName));
        // Roche hote
        DimCategory cat = getHostRockCategory(blockId);
        if      (cat == DimCategory.OVERWORLD) e.overworldRock++;
        else if (cat == DimCategory.NETHER)    e.netherRock++;
        else if (cat == DimCategory.END)       e.endRock++;
        // Tous les blocs
        recordAllBlocks(e, blockId.getPath());
    }

    private static void recordAllBlocks(Entry e, String path) {
        if (e.allBlocks.size() < Entry.MAX_BLOCK_TYPES || e.allBlocks.containsKey(path)) {
            e.allBlocks.merge(path, 1, Integer::sum);
        }
    }

    public static List<Entry> getLeaderboard() {
        List<Entry> list = new ArrayList<>(stats.values());
        list.removeIf(e -> e.totalHostRock() < MIN_SAMPLE);
        list.sort((a, b) -> Double.compare(b.ratio(), a.ratio()));
        return list;
    }

    public static void cleanup(UUID id) { stats.remove(id); }

    public static void reset(UUID id) {
        Entry e = stats.get(id);
        if (e != null) {
            e.oreGroups.clear(); e.allBlocks.clear();
            e.overworldRock = 0; e.netherRock = 0; e.endRock = 0;
        }
    }
}
