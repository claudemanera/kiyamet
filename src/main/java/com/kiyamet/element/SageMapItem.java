package com.kiyamet.element;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A torn map piece from a city. Unfold it: it shows where one of the sages lives. */
public class SageMapItem extends Item {
    public SageMapItem(Properties p) { super(p); }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
        List<String> got = Temples.mapped(p);
        List<Element> left = new ArrayList<>();
        for (Element e : Element.values()) if (!got.contains(e.id)) left.add(e);
        if (left.isEmpty()) left.addAll(List.of(Element.values()));
        Element e = left.get(p.getRandom().nextInt(left.size()));
        Temples.markMapped(p, e);
        ItemStack map = Temples.sageMap(p.level(), e);
        player.getItemInHand(hand).shrink(1);
        if (!p.getInventory().add(map)) p.drop(map, false);
        level.playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.8f);
        p.sendSystemMessage(Component.literal("The torn map shows the way to the " + e.title + " Sage.").withStyle(e.fmt, ChatFormatting.BOLD));
        return InteractionResult.SUCCESS;
    }
}
