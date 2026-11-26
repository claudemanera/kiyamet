package com.kiyamet.npc;

import com.kiyamet.Kiyamet;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 1.6: every new player starts with one companion at their side (use the Gather button to send them for wood, stone and ores). */
public final class Starter {
    public static final AttachmentType<Boolean> GIVEN = AttachmentRegistry.create(Kiyamet.id("starter_companion"), b -> b.initializer(() -> false).persistent(Codec.BOOL).copyOnDeath());

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer p = handler.getPlayer();
            server.execute(() -> {
                if (p.getAttachedOrElse(GIVEN, false) || !(p.level() instanceof ServerLevel level)) return;
                p.setAttached(GIVEN, true);
                HumanEntity h = ModHumans.spawn(level, p.blockPosition().east(2), Faction.SURVIVOR, Role.MEMBER, 15, "");
                if (h == null) return;
                h.setOwner(p);
                h.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
                p.sendSystemMessage(Component.literal(h.getName().getString() + " has your back. Right-click them for orders - try 'Gather resources' → Develop.").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                Talk.sayLater(p, h, 60, "Hey. Stick together and we might just make it. Want me to gather some wood?");
            });
        });
    }

    private Starter() {}
}
