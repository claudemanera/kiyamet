package com.kiyamet.stone;

import com.kiyamet.skill.SkillManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Stone Gauntlet. Slots open at Total Power 200 / 1000 / 3000 / 6000 / 10000 (tier I–V).
 * Stone in offhand + right click: socket it. Sneak + right click: select next stone. Right click: use selected stone. Left click: gauntlet panel.
 */
public class GauntletItem extends Item {
    public static final int[] SLOT_POWER = {200, 1000, 3000, 6000, 10000};

    public GauntletItem(Properties p) { super(p); }

    public static int slots(int power) {
        int n = 0;
        for (int t : SLOT_POWER) if (power >= t) n++;
        return n;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer p) || hand != InteractionHand.MAIN_HAND) return InteractionResult.SUCCESS;
        GauntletData d = stack.getOrDefault(GauntletData.TYPE, GauntletData.EMPTY);
        ItemStack off = p.getOffhandItem();
        if (off.getItem() instanceof StoneItem si) {
            int slots = slots(SkillManager.get(p).totalPower());
            if (d.stones().contains(si.kind.id)) { p.sendSystemMessage(Component.translatable("kiyamet.msg.gauntlet_has").withStyle(ChatFormatting.GRAY)); return InteractionResult.FAIL; }
            if (d.stones().size() >= slots) {
                int next = d.stones().size() < SLOT_POWER.length ? SLOT_POWER[d.stones().size()] : -1;
                p.sendSystemMessage(Component.translatable("kiyamet.msg.gauntlet_full", next).withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            stack.set(GauntletData.TYPE, d.add(si.kind.id));
            if (!p.isCreative()) off.shrink(1);
            p.sendSystemMessage(Component.translatable("kiyamet.msg.gauntlet_socket", Component.translatable("item.kiyamet." + si.kind.id)).withStyle(ChatFormatting.LIGHT_PURPLE));
            return InteractionResult.CONSUME;
        }
        if (p.isShiftKeyDown()) {
            GauntletData c = d.cycle();
            stack.set(GauntletData.TYPE, c);
            if (c.current() != null) p.sendSystemMessage(Component.translatable("kiyamet.msg.gauntlet_select", Component.translatable("item.kiyamet." + c.current().id)).withStyle(ChatFormatting.AQUA));
            return InteractionResult.CONSUME;
        }
        StoneKind k = d.current();
        if (k == null) { p.sendSystemMessage(Component.translatable("kiyamet.msg.gauntlet_empty").withStyle(ChatFormatting.GRAY)); return InteractionResult.FAIL; }
        if (StonePowers.activate(p, k)) p.getCooldowns().addCooldown(stack, StonePowers.cooldown(p, k));
        return InteractionResult.CONSUME;
    }
}
