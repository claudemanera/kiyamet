package com.kiyamet.robot;

import com.kiyamet.skill.Skill;
import com.kiyamet.skill.SkillManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Robot core: use on the ground to build a robot companion that fights for you (max 3). */
public class RobotCoreItem extends Item {
    public static final int MAX = 3;
    private final RobotKind kind;

    public RobotCoreItem(Properties p, RobotKind kind) { super(p); this.kind = kind; }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
        long mine = level.getEntitiesOfClass(RobotEntity.class, p.getBoundingBox().inflate(256), r -> r.isAlly() && p.getUUID().equals(r.owner())).size();
        if (mine >= MAX) { p.displayClientMessage(Component.literal("You already command " + MAX + " robots.").withStyle(ChatFormatting.RED), true); return InteractionResult.FAIL; }
        RobotEntity r = ModRobots.TYPES.get(kind).create(level, EntitySpawnReason.MOB_SUMMONED);
        if (r == null) return InteractionResult.FAIL;
        BlockPos at = ctx.getClickedPos().relative(ctx.getClickedFace());
        r.setPos(at.getX() + 0.5, at.getY() + (kind.flying ? 2 : 0), at.getZ() + 0.5);
        r.makeAlly(p, SkillManager.get(p).level(Skill.ENGINEERING));
        level.addFreshEntity(r);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, r.getX(), r.getY() + 1, r.getZ(), 60, 0.5, 0.8, 0.5, 0.3);
        level.playSound(null, at, SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 1f, 0.7f);
        p.sendSystemMessage(Component.literal("Robot online. Right-click: follow/stay, iron ingot: repair.").withStyle(ChatFormatting.AQUA));
        if (!p.isCreative()) ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
