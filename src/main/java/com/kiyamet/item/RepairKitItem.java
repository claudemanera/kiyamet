package com.kiyamet.item;

import com.kiyamet.defense.BlockDurability;
import com.kiyamet.skill.Skill;
import com.kiyamet.skill.SkillManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Restores 50% of a damaged wall's durability (100% at Engineering 100). */
public class RepairKitItem extends Item {
    public RepairKitItem(Properties p) { super(p); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
        int eng = SkillManager.get(p).level(Skill.ENGINEERING);
        if (!BlockDurability.repair(level, ctx.getClickedPos(), eng >= 100 ? 1f : 0.5f)) {
            p.displayClientMessage(Component.literal("This block is not damaged."), true);
            return InteractionResult.FAIL;
        }
        if (!p.isCreative()) ctx.getItemInHand().shrink(1);
        SkillManager.addXp(p, Skill.ENGINEERING, 20);
        level.sendParticles(ParticleTypes.WAX_ON, ctx.getClickedPos().getX() + 0.5, ctx.getClickedPos().getY() + 0.5, ctx.getClickedPos().getZ() + 0.5, 20, 0.5, 0.5, 0.5, 0.1);
        level.playSound(null, ctx.getClickedPos(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6f, 1.4f);
        return InteractionResult.SUCCESS;
    }
}
