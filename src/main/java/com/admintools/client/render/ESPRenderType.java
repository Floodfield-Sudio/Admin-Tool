package com.admintools.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;

/**
 * RenderType sans depth test pour dessiner les contours ESP
 * a travers les blocs (X-Ray ESP, Entity ESP).
 *
 * Extends RenderType pour acceder aux champs proteges (NO_DEPTH_TEST, etc.)
 */
public class ESPRenderType extends RenderType {

    // Constructeur requis par l'heritage -- jamais utilise directement
    public ESPRenderType(String name, VertexFormat fmt, VertexFormat.Mode mode,
                         int bufSize, boolean affectsCrumbling, boolean sortOnUpload,
                         Runnable setup, Runnable clear) {
        super(name, fmt, mode, bufSize, affectsCrumbling, sortOnUpload, setup, clear);
    }

    /** Lignes de 2 px, sans depth test pour ESP ore et entity. */
    public static final RenderType ESP_LINES = create(
        "admintools_esp_lines",
        DefaultVertexFormat.POSITION_COLOR_NORMAL,
        VertexFormat.Mode.LINES,
        256,
        false,
        false,
        CompositeState.builder()
            .setShaderState(RENDERTYPE_LINES_SHADER)
            .setLineState(new LineStateShard(OptionalDouble.of(2.0)))
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setOutputState(ITEM_ENTITY_TARGET)
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .setDepthTestState(NO_DEPTH_TEST)          // traverse les murs
            .createCompositeState(false)
    );

    /** Lignes fines (1.5 px) pour les petits minerais. */
    public static final RenderType ESP_LINES_THIN = create(
        "admintools_esp_lines_thin",
        DefaultVertexFormat.POSITION_COLOR_NORMAL,
        VertexFormat.Mode.LINES,
        256,
        false,
        false,
        CompositeState.builder()
            .setShaderState(RENDERTYPE_LINES_SHADER)
            .setLineState(new LineStateShard(OptionalDouble.of(1.5)))
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setOutputState(ITEM_ENTITY_TARGET)
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .setDepthTestState(NO_DEPTH_TEST)
            .createCompositeState(false)
    );
}
