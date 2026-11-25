package com.kiyamet.npc;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** "Rename" button: the next chat message you type becomes the companion's name (and is not sent to chat). */
public final class Rename {
    private static final Map<UUID, UUID> PENDING = new HashMap<>();

    public static void start(ServerPlayer p, HumanEntity h) {
        PENDING.put(p.getUUID(), h.getUUID());
        p.closeContainer();
        p.sendSystemMessage(Component.literal("Type the new name for " + h.getName().getString() + " in chat (or type 'cancel').").withStyle(ChatFormatting.AQUA));
    }

    public static void init() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            UUID target = PENDING.remove(sender.getUUID());
            if (target == null) return true;
            String name = message.signedContent().trim();
            if (name.equalsIgnoreCase("cancel") || name.isEmpty()) { sender.sendSystemMessage(Component.literal("Renaming cancelled.")); return false; }
            if (name.length() > 24) name = name.substring(0, 24);
            Entity e = ((ServerLevel) sender.level()).getEntity(target);
            if (e instanceof HumanEntity h && h.isOwnedBy(sender)) {
                h.setCustomName(Component.literal(name));
                sender.sendSystemMessage(Component.literal("Your companion is now called " + name + ".").withStyle(ChatFormatting.GREEN));
            }
            return false;
        });
    }

    private Rename() {}
}
