package com.admintools.client.render;

import com.admintools.AdminFeature;
import com.admintools.client.ClientAdminState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Rendu ESP :
 *   - Ore X-Ray : boites colorees autour des minerais (through walls)
 *   - Entity ESP : boites autour des entites vivantes (through walls)
 *
 * Cache les minerais toutes les CACHE_INTERVAL ticks pour les perfs.
 */
public class ESPRenderer {

    private static final List<BlockPos> cachedOres  = new ArrayList<>();
    private static int  cacheTimer   = 0;
    private static final int CACHE_INTERVAL = 40;  // ~2 secondes
    private static final int SCAN_RADIUS    = 64;  // 64 blocs dans toutes les directions

    // ── Point d'entree ────────────────────────────────────────────────────────

    public static void render(PoseStack pose, MultiBufferSource.BufferSource buffers, Vec3 camPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        if (ClientAdminState.isEnabled(AdminFeature.ORE_ESP)) {
            renderOreESP(pose, buffers, camPos, mc);
        }
        if (ClientAdminState.isEnabled(AdminFeature.ENTITY_ESP)) {
            renderEntityESP(pose, buffers, camPos, mc);
        }
    }

    // ── Ore X-Ray ESP ─────────────────────────────────────────────────────────

    private static void renderOreESP(PoseStack pose, MultiBufferSource.BufferSource buffers,
                                     Vec3 camPos, Minecraft mc) {
        cacheTimer++;
        if (cacheTimer >= CACHE_INTERVAL) {
            rebuildOreCache(mc);
            cacheTimer = 0;
        }

        VertexConsumer vc = buffers.getBuffer(ESPRenderType.ESP_LINES_THIN);
        pose.pushPose();
        pose.translate(-camPos.x, -camPos.y, -camPos.z);

        for (BlockPos bp : cachedOres) {
            BlockState state = mc.level.getBlockState(bp);
            if (state.isAir()) continue;
            float[] col = oreColor(state);
            AABB box = new AABB(bp).inflate(0.004);
            LevelRenderer.renderLineBox(pose, vc,
                box.minX, box.minY, box.minZ,
                box.maxX, box.maxY, box.maxZ,
                col[0], col[1], col[2], 0.85f);
        }

        pose.popPose();
        buffers.endBatch(ESPRenderType.ESP_LINES_THIN);
    }

    private static void rebuildOreCache(Minecraft mc) {
        cachedOres.clear();
        if (mc.player == null || mc.level == null) return;
        BlockPos center = mc.player.blockPosition();
        int r = SCAN_RADIUS;
        for (BlockPos bp : BlockPos.betweenClosed(
                center.offset(-r, -r, -r), center.offset(r, r, r))) {
            BlockState state = mc.level.getBlockState(bp);
            if (!state.isAir() && isOre(state)) cachedOres.add(bp.immutable());
        }
    }

    private static boolean isOre(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return id.contains("_ore") || id.equals("ancient_debris");
    }

    private static float[] oreColor(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (id.contains("diamond"))        return new float[]{0.00f, 0.90f, 1.00f};
        if (id.contains("emerald"))        return new float[]{0.00f, 1.00f, 0.25f};
        if (id.contains("gold"))           return new float[]{1.00f, 0.85f, 0.00f};
        if (id.contains("iron"))           return new float[]{0.80f, 0.60f, 0.40f};
        if (id.contains("ancient_debris")) return new float[]{0.80f, 0.30f, 0.10f};
        if (id.contains("redstone"))       return new float[]{1.00f, 0.10f, 0.10f};
        if (id.contains("lapis"))          return new float[]{0.10f, 0.20f, 0.90f};
        if (id.contains("coal"))           return new float[]{0.30f, 0.30f, 0.30f};
        if (id.contains("copper"))         return new float[]{0.80f, 0.50f, 0.20f};
        if (id.contains("nether_quartz"))  return new float[]{0.95f, 0.95f, 0.95f};
        return new float[]{1f, 0f, 1f};
    }

    // ── Entity ESP ────────────────────────────────────────────────────────────

    private static void renderEntityESP(PoseStack pose, MultiBufferSource.BufferSource buffers,
                                        Vec3 camPos, Minecraft mc) {
        VertexConsumer vc = buffers.getBuffer(ESPRenderType.ESP_LINES);
        pose.pushPose();
        pose.translate(-camPos.x, -camPos.y, -camPos.z);

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player) continue;
            if (!(entity instanceof LivingEntity)) continue;
            if (entity.isInvisibleTo(mc.player)) continue;

            float[] col = entityColor(entity);
            float ft = mc.getFrameTime();
            double x = entity.xo + (entity.getX() - entity.xo) * ft;
            double y = entity.yo + (entity.getY() - entity.yo) * ft;
            double z = entity.zo + (entity.getZ() - entity.zo) * ft;
            double w = entity.getBbWidth()  / 2.0 + 0.1;
            double h = entity.getBbHeight() + 0.1;

            LevelRenderer.renderLineBox(pose, vc,
                x - w, y - 0.05, z - w,
                x + w, y + h,    z + w,
                col[0], col[1], col[2], 0.9f);
        }

        pose.popPose();
        buffers.endBatch(ESPRenderType.ESP_LINES);
    }

    private static float[] entityColor(Entity entity) {
        if (entity instanceof Player)  return new float[]{0.20f, 0.60f, 1.00f}; // bleu
        if (entity instanceof Monster) return new float[]{1.00f, 0.15f, 0.15f}; // rouge
        return                                new float[]{0.20f, 1.00f, 0.30f}; // vert
    }

    /** Force un rebuild du cache au prochain tick. */
    public static void invalidateCache() {
        cacheTimer = CACHE_INTERVAL;
    }
}
