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
    private static KeyMapping AUTOPILOT;
    /** 2.2: one key per power of the Creation Axe (numpad by default, change them freely) */
    private static final int[] CREATION_KEYS = {GLFW.GLFW_KEY_KP_1, GLFW.GLFW_KEY_KP_2, GLFW.GLFW_KEY_KP_3, GLFW.GLFW_KEY_KP_4, GLFW.GLFW_KEY_KP_5, GLFW.GLFW_KEY_KP_6, GLFW.GLFW_KEY_KP_7,
            GLFW.GLFW_KEY_KP_8, GLFW.GLFW_KEY_KP_9, GLFW.GLFW_KEY_KP_0, GLFW.GLFW_KEY_KP_DIVIDE, GLFW.GLFW_KEY_KP_MULTIPLY, GLFW.GLFW_KEY_KP_SUBTRACT, GLFW.GLFW_KEY_KP_ADD};
    private static final KeyMapping[] CREATION = new KeyMapping[14];

    public static void init() {
        for (int i = 0; i < NAMES.length; i++)
            MAPPINGS[i] = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.kiyamet." + NAMES[i], InputConstants.Type.KEYSYM, KEYS[i], CATEGORY));
        // left-click with the Stone Gauntlet in hand opens its panel
        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.screen != null || !(mc.player.getMainHandItem().getItem() instanceof com.kiyamet.stone.GauntletItem)) return;
            boolean any = false;
            while (mc.options.keyAttack.consumeClick()) any = true;
            if (any) ClientPlayNetworking.send(new KeyAction(KeyAction.GAUNTLET_MENU));
        });
        AUTOPILOT = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.kiyamet.autopilot", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;
            while (AUTOPILOT.consumeClick()) {
                Object[] w = XaeroWaypoints.newest(mc);
                ClientPlayNetworking.send(w == null ? new com.kiyamet.vehicle.Autopilot.Drive(0, 0, "?") : new com.kiyamet.vehicle.Autopilot.Drive((Integer) w[0], (Integer) w[1], (String) w[2]));
            }
        });
        com.kiyamet.element.Element[] els = com.kiyamet.element.Element.values();
        for (int i = 0; i < els.length; i++)
            CREATION[i] = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.kiyamet.creation_" + els[i].id, InputConstants.Type.KEYSYM, CREATION_KEYS[i], CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;
            for (int i = 0; i < CREATION.length; i++)
                while (CREATION[i].consumeClick()) ClientPlayNetworking.send(new KeyAction(KeyAction.CREATION + i));
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;
            for (int i = 0; i < MAPPINGS.length; i++)
                while (MAPPINGS[i].consumeClick()) ClientPlayNetworking.send(new KeyAction(i));
        });
    }

    private ModKeys() {}
}
