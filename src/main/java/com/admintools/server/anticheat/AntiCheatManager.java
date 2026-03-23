package com.admintools.server.anticheat;

import com.admintools.AdminFeature;
import com.admintools.AdminToolsState;
import com.admintools.network.NetworkHandler;
import com.admintools.network.packets.S2CViolationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Detecte les violations Fly, NoClip et X-Ray.
 * Appele depuis ServerEvents a chaque tick joueur et a chaque bloc casse.
 */
public class AntiCheatManager {

    // -- Seuils de tolerance ---------------------------------------------------
    private static final int FLY_TICKS_THRESHOLD       = 80;   // 4 secondes en l'air
    private static final int FLY_REPORT_COOLDOWN_TICKS = 200;  // delai entre deux rapports
    private static final int NOCLIP_VIOLATIONS_MAX     = 3;    // detections consecutives
    private static final int ORE_SAMPLE_MIN            = 12;   // minerais avant evaluation
    private static final double XRAY_RATIO_THRESHOLD   = 0.35; // 35 % d'ores = suspect

    // -- Donnees par joueur ----------------------------------------------------

    private static class FlyData {
        int airTicks          = 0;
        int reportCooldown    = 0;
        double lastY          = 0;
    }

    private static class NoClipData {
        int consecutiveViolations = 0;
        int reportCooldown        = 0;
    }

    private static class XRayData {
        int oresBroken   = 0;
        int stoneBroken  = 0;
        boolean reported = false;
    }

    private static final Map<UUID, FlyData>    flyMap    = new HashMap<>();
    private static final Map<UUID, NoClipData> noClipMap = new HashMap<>();
    private static final Map<UUID, XRayData>   xRayMap   = new HashMap<>();

    // -- Tick anti-cheat (appele depuis PlayerTickEvent) -----------------------

    public static void tick(ServerPlayer player) {
        // Ne pas surveiller les admins ni les joueurs en creative/spectator
        if (isExempt(player)) return;

        UUID id = player.getUUID();
        tickFly(player, id);
        tickNoClip(player, id);
    }

    private static void tickFly(ServerPlayer player, UUID id) {
        FlyData data = flyMap.computeIfAbsent(id, k -> new FlyData());
        if (data.reportCooldown > 0) { data.reportCooldown--; }

        boolean onGround   = player.onGround();
        boolean inWater    = player.isInWater() || player.isUnderWater();
        boolean onLadder   = player.onClimbable();
        boolean elytra     = player.isFallFlying();
        double  currentY   = player.getY();

        if (!onGround && !inWater && !onLadder && !elytra) {
            data.airTicks++;

            // Detecte la montee anormale sans saut (> 0.42 blocs/tick = vitesse de saut)
            double deltaY = currentY - data.lastY;
            if (deltaY > 0.52 && data.airTicks > 5) {
                data.airTicks += 5; // accelere le compteur
            }

            if (data.airTicks >= FLY_TICKS_THRESHOLD && data.reportCooldown == 0) {
                report(player, ViolationType.FLY,
                    "en l'air depuis " + (data.airTicks / 20) + "s -- deltaY=" + String.format("%.2f", deltaY));
                data.reportCooldown = FLY_REPORT_COOLDOWN_TICKS;
                data.airTicks       = FLY_TICKS_THRESHOLD / 2; // evite le spam
            }
        } else {
            data.airTicks = 0;
        }
        data.lastY = currentY;
    }

    private static void tickNoClip(ServerPlayer player, UUID id) {
        NoClipData data = noClipMap.computeIfAbsent(id, k -> new NoClipData());
        if (data.reportCooldown > 0) { data.reportCooldown--; return; }

        // Verifie si le joueur est a l'interieur d'un bloc solide
        BlockPos pos = player.blockPosition();
        if (isInsideSolidBlock(player, pos)) {
            data.consecutiveViolations++;
            if (data.consecutiveViolations >= NOCLIP_VIOLATIONS_MAX) {
                report(player, ViolationType.NOCLIP,
                    "dans un bloc solide a " + pos.getX() + "," + pos.getY() + "," + pos.getZ());
                data.consecutiveViolations = 0;
                data.reportCooldown        = 100;
            }
        } else {
            data.consecutiveViolations = 0;
        }
    }

    private static boolean isInsideSolidBlock(ServerPlayer player, BlockPos pos) {
        AABB box   = player.getBoundingBox().deflate(0.05);
        var level  = player.level();
        // Verifie tous les blocs autour du joueur
        for (BlockPos bp : BlockPos.betweenClosed(
                (int) box.minX, (int) box.minY, (int) box.minZ,
                (int) Math.ceil(box.maxX), (int) Math.ceil(box.maxY), (int) Math.ceil(box.maxZ))) {
            BlockState state = level.getBlockState(bp);
            if (state.isSolidRender(level, bp) && !state.isAir()) {
                return true;
            }
        }
        return false;
    }

    // -- X-Ray (appele depuis BlockBreakEvent) --------------------------------

    public static void onBlockBroken(ServerPlayer player, BlockState state) {
        if (isExempt(player)) return;
        UUID     id   = player.getUUID();
        XRayData data = xRayMap.computeIfAbsent(id, k -> new XRayData());

        if (isOre(state)) {
            data.oresBroken++;
        } else if (isStoneType(state)) {
            data.stoneBroken++;
        }

        int total = data.oresBroken + data.stoneBroken;
        if (!data.reported && total >= ORE_SAMPLE_MIN) {
            double ratio = (double) data.oresBroken / total;
            if (ratio >= XRAY_RATIO_THRESHOLD) {
                report(player, ViolationType.XRAY,
                    data.oresBroken + " ores / " + total + " blocs (" + String.format("%.0f", ratio * 100) + "%)");
                data.reported = true;
                // Reinitialise partiellement pour pouvoir re-detecter
                data.oresBroken  = 0;
                data.stoneBroken = 0;
                data.reported    = false;
            }
            // Fenetre glissante : on efface quand on a assez de donnees
            if (total > 100) {
                data.oresBroken  = data.oresBroken  / 2;
                data.stoneBroken = data.stoneBroken / 2;
            }
        }
    }

    // -- Helpers ---------------------------------------------------------------

    private static boolean isExempt(ServerPlayer player) {
        var gm = player.gameMode.getGameModeForPlayer();
        return gm == GameType.CREATIVE || gm == GameType.SPECTATOR
            || player.isCreative()
            || AdminToolsState.isEnabled(player.getUUID(), AdminFeature.ORE_ESP); // les admins sont exemptes
    }

    private static boolean isOre(BlockState state) {
        var block = state.getBlock();
        var reg   = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
        var id    = reg.getKey(block).getPath();
        return id.contains("_ore") || id.equals("ancient_debris");
    }

    private static boolean isStoneType(BlockState state) {
        var block = state.getBlock();
        var reg   = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
        var id    = reg.getKey(block).getPath();
        return id.equals("stone") || id.equals("deepslate") || id.equals("netherrack")
            || id.equals("cobblestone") || id.equals("dirt") || id.equals("gravel");
    }

    // -- Envoi de l'alerte aux admins -----------------------------------------

    private static void report(ServerPlayer player, ViolationType type, String details) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        String msg = "§8[§cAnti-Cheat§8] §f" + player.getName().getString()
            + " §8-- " + type.coloredName + " §8-- §7" + details;

        for (UUID adminId : AdminToolsState.getAlertRecipients()) {
            ServerPlayer admin = server.getPlayerList().getPlayer(adminId);
            if (admin != null && admin != player) {
                // Envoie le paquet de violation au client admin
                NetworkHandler.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> admin),
                    new S2CViolationPacket(type, player.getName().getString(), details)
                );
                // Aussi dans le chat serveur
                admin.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg));
            }
        }
        // Log console serveur
        server.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg));
    }

    /** Nettoie les donnees d'un joueur deconnecte. */
    public static void cleanup(UUID id) {
        flyMap.remove(id);
        noClipMap.remove(id);
        xRayMap.remove(id);
    }
}
