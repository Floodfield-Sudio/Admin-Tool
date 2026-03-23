package com.admintools.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static final KeyMapping OPEN_PANEL = new KeyMapping(
        "key.admintools.open_panel",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K,
        "key.categories.admintools"
    );
    // L'enregistrement se fait dans ClientEvents.ModBusEvents via RegisterKeyMappingsEvent
}
