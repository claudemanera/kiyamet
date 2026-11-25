package com.kiyamet.stone;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A loose power stone: right click uses its active power; in the offhand its passive works. */
public class StoneItem extends Item {
    public final StoneKind kind;
    public StoneItem(Properties p, StoneKind kind) { super(p); this.kind = kind; }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer p && StonePowers.activate(p, kind))
            p.getCooldowns().addCooldown(stack, StonePowers.cooldown(p, kind));
        return InteractionResult.SUCCESS;
    }
}
