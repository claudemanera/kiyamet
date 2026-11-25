package com.kiyamet.item;

import com.kiyamet.skill.Skill;
import com.kiyamet.skill.SkillManager;
import com.kiyamet.twd.Camouflage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Smear walker guts on yourself: zombies ignore you for 2 minutes (+1 s per 4 Stealth levels). Water washes it off. */
public class WalkerGutsItem extends Item {
    public WalkerGutsItem(Properties p) { super(p); }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (sp.isInWaterOrRain()) { sp.displayClientMessage(Component.literal("It would wash right off in the rain.").withStyle(ChatFormatting.GRAY), true); return InteractionResult.FAIL; }
        int stealth = SkillManager.get(sp).level(Skill.STEALTH);
        int ticks = (120 + stealth / 4) * 20;
        sp.addEffect(new MobEffectInstance(Camouflage.EFFECT, ticks, 0));
        sp.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 60, 0));
        for (Mob m : sl.getEntitiesOfClass(Mob.class, sp.getBoundingBox().inflate(32), m -> m.getTarget() == sp && m instanceof com.kiyamet.entity.KZombie))
            if (m.getLastHurtByMob() != sp) m.setTarget(null);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()), sp.getX(), sp.getY() + 1, sp.getZ(), 40, 0.3, 0.6, 0.3, 0.1);
        sl.playSound(null, sp.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1f, 0.6f);
        sp.sendSystemMessage(Component.literal("You smear walker guts on yourself. The dead ignore you for " + ticks / 20 + " s.").withStyle(ChatFormatting.DARK_GREEN));
        SkillManager.addXp(sp, Skill.STEALTH, 25);
        if (!sp.isCreative()) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
