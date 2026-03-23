package com.admintools.events;

import com.admintools.AdminToolsMod;
import com.admintools.client.AdminPanel;
import com.admintools.client.KeyBindings;
import com.admintools.client.VanishHUD;
import com.admintools.client.render.ESPRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AdminToolsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

    // ── Enregistrement de la touche (bus MOD) ─────────────────────────────────

    @Mod.EventBusSubscriber(modid = AdminToolsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(KeyBindings.OPEN_PANEL);
        }
    }

    // ── Touche K ──────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        if (KeyBindings.OPEN_PANEL.consumeClick()) {
            mc.setScreen(new AdminPanel());
        }
    }

    // ── Indicateur VANISH (HUD) ───────────────────────────────────────────────

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        VanishHUD.render(event);
    }

    // ── ESP dans le monde ─────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.gameRenderer == null) return;

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        ESPRenderer.render(
            event.getPoseStack(),
            buffers,
            mc.gameRenderer.getMainCamera().getPosition()
        );
    }
}
