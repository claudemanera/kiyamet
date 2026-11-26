package com.kiyamet.backpack;

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

/** Right-click: open it. Sneak + right-click: wear it on your back (its own slot, shown on your back). */
public class BackpackItem extends Item {
    public final BackpackTier tier;

    public BackpackItem(Properties p, BackpackTier tier) {
        super(p);
        this.tier = tier;
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext ctx) {
        InteractionResult r = com.kiyamet.furniture.BagBlock.place(ctx);
        return r != InteractionResult.PASS ? r : super.useOn(ctx);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        ItemStack stack = sp.getItemInHand(hand);
        if (sp.isShiftKeyDown()) {
            ItemStack worn = Backpacks.worn(sp);
            Backpacks.setWorn(sp, stack.copy());
            sp.setItemInHand(hand, worn);
            sp.level().playSound(null, sp.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1f, 1f);
            sp.displayClientMessage(Component.literal("You put on the " + tier.title + ". Press U to open it.").withStyle(ChatFormatting.GOLD), true);
            return InteractionResult.CONSUME;
        }
        BackpackMenu.openFromHand(sp, hand);
        return InteractionResult.CONSUME;
    }
}
