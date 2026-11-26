package com.kiyamet.twd;

import com.kiyamet.npc.BaseRaids;
import com.kiyamet.npc.HumanEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

/**
 * Shelter Core: the heart of your base. Within 24 blocks walkers don't spawn on their own, you and your
 * companions slowly heal, and it becomes your base for raids. One per player; sneak + right-click to pick it up.
 */
public class ShelterCore extends PathfinderMob implements GeoEntity {
    public static final int RADIUS = 24;
    private static final Map<ResourceKey<Level>, Map<UUID, BlockPos>> CORES = new HashMap<>();
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("misc.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID owner;

    public ShelterCore(EntityType<? extends ShelterCore> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public UUID owner() { return owner; }

    public static boolean protects(ServerLevel level, BlockPos pos) {
        Map<UUID, BlockPos> m = CORES.get(level.dimension());
        if (m == null) return false;
        for (BlockPos c : m.values()) if (c.distSqr(pos) <= RADIUS * RADIUS) return true;
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0, getDeltaMovement().y, 0);
        if (!(level() instanceof ServerLevel sl)) return;
        CORES.computeIfAbsent(sl.dimension(), k -> new HashMap<>()).put(getUUID(), blockPosition());
        if (tickCount % 100 == 0) {
            for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(RADIUS)))
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, true, true));
            for (HumanEntity h : sl.getEntitiesOfClass(HumanEntity.class, getBoundingBox().inflate(RADIUS), h -> owner != null && owner.equals(h.owner())))
                h.heal(2);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.2, getZ(), 6, 0.2, 0.4, 0.2, 0.02);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel sl) { Map<UUID, BlockPos> m = CORES.get(sl.dimension()); if (m != null) m.remove(getUUID()); }
        super.remove(reason);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (sp.isShiftKeyDown() && (owner == null || owner.equals(sp.getUUID()))) {
            ItemStack item = new ItemStack(ModTwd.SHELTER_CORE_ITEM);
            if (!sp.getInventory().add(item)) sp.drop(item, false);
            discard();
            sp.displayClientMessage(Component.literal("Shelter Core packed up.").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.SUCCESS;
        }
        sp.sendSystemMessage(Component.literal("Shelter Core  ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                .append(Component.literal("HP " + (int) getHealth() + "/" + (int) getMaxHealth() + " · walkers don't spawn within " + RADIUS + " blocks · healing aura · raid target. Sneak + right-click to pack up. Iron block: repair.").withStyle(ChatFormatting.GRAY)));
        if (player.getItemInHand(hand).is(net.minecraft.world.item.Items.IRON_BLOCK) && getHealth() < getMaxHealth()) {
            heal(60); if (!sp.isCreative()) player.getItemInHand(hand).shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player p && owner != null && owner.equals(p.getUUID())) return false;
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5, getZ(), 3, 0.3, 0.3, 0.3, 0);
            sl.addFreshEntity(new ItemEntity(sl, getX(), getY() + 0.5, getZ(), new ItemStack(com.kiyamet.registry.ModItems.get("tech_core"))));
            ServerPlayer p = owner == null ? null : sl.getServer().getPlayerList().getPlayer(owner);
            if (p != null) p.sendSystemMessage(Component.literal("Your Shelter Core was destroyed!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        }
        super.die(source);
    }

    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override protected boolean shouldDropLoot(ServerLevel level) { return false; }
    @Override public boolean canBeLeashed() { return false; }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        if (owner != null) out.putString("Owner", owner.toString());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        String o = in.getStringOr("Owner", "");
        if (!o.isEmpty()) try { owner = UUID.fromString(o); } catch (IllegalArgumentException ignored) {}
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<ShelterCore>("main", 0, t -> t.setAndContinue(IDLE)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    public static class CoreItem extends Item {
        public CoreItem(Properties p) { super(p); }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
            for (ShelterCore c : level.getEntitiesOfClass(ShelterCore.class, p.getBoundingBox().inflate(512), c -> p.getUUID().equals(c.owner))) {
                p.displayClientMessage(Component.literal("You already have a Shelter Core at " + c.blockPosition().toShortString() + ".").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
            ShelterCore c = ModTwd.SHELTER_CORE.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (c == null) return InteractionResult.FAIL;
            BlockPos at = ctx.getClickedPos().relative(ctx.getClickedFace());
            c.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            c.owner = p.getUUID();
            level.addFreshEntity(c);
            p.setAttached(BaseRaids.BASE, at);
            level.playSound(null, at, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1f, 1.2f);
            p.sendSystemMessage(Component.literal("Shelter Core online. This is your base now: no walkers spawn within " + RADIUS + " blocks, everyone nearby heals. Raiders will come here.").withStyle(ChatFormatting.AQUA));
            if (!p.isCreative()) ctx.getItemInHand().shrink(1);
            return InteractionResult.SUCCESS;
        }
    }
}
