package com.kiyamet.client;

import com.kiyamet.Kiyamet;
import com.kiyamet.net.KeyAction;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Key bindings, changeable in Options > Controls > Key Binds > The Walking Dead. */
public final class ModKeys {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Kiyamet.id("twd"));
    private static final String[] NAMES = {"skills", "powers", "companions", "gauntlet_use", "gauntlet_cycle", "camouflage", "night_vision", "shadow_step", "backpack", "squad"};
    private static final int[] KEYS = {GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_P, GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_G};
    private static final KeyMapping[] MAPPINGS = new KeyMapping[NAMES.length];

    public static void init() {
        for (int i = 0; i < NAMES.length; i++)
            MAPPINGS[i] = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.kiyamet." + NAMES[i], InputConstants.Type.KEYSYM, KEYS[i], CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;
            for (int i = 0; i < MAPPINGS.length; i++)
                while (MAPPINGS[i].consumeClick()) ClientPlayNetworking.send(new KeyAction(i));
        });
    }

    private ModKeys() {}
}
