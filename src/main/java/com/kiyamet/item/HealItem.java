package com.kiyamet.item;

import com.kiyamet.skill.Skill;
import com.kiyamet.skill.SkillManager;
import com.kiyamet.world.Infection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Bandage, medkit, healing syringe and vaccine: right click to use. Medicine skill makes healing stronger. */
public class HealItem extends Item {
    public enum Kind { BANDAGE, MEDKIT, SYRINGE, VACCINE }
    private final Kind kind;

    public HealItem(Properties props, Kind kind) { super(props); this.kind = kind; }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
        int medicine = SkillManager.get(p).level(Skill.MEDICINE);
        float boost = 1f + medicine / 999f;
        switch (kind) {
            case BANDAGE -> { if (p.getHealth() >= p.getMaxHealth()) return InteractionResult.PASS; p.heal(4f * boost); SkillManager.addXp(p, Skill.MEDICINE, 12); }
            case MEDKIT -> { p.heal(12f * boost); p.removeEffect(MobEffects.POISON); SkillManager.addXp(p, Skill.MEDICINE, 30); }
            case SYRINGE -> { p.heal(8f * boost); p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1)); SkillManager.addXp(p, Skill.MEDICINE, 25); }
            case VACCINE -> { if (Infection.get(p) <= 0) return InteractionResult.PASS; Infection.cure(p); SkillManager.addXp(p, Skill.MEDICINE, 60); }
        }
        level.playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1.2f);
        if (!p.isCreative()) stack.shrink(1);
        p.getCooldowns().addCooldown(stack, kind == Kind.BANDAGE ? 20 : 60);
        return InteractionResult.CONSUME;
    }
}
