package com.kiyamet.wild;

import com.kiyamet.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Wild animals (1.9). Predators don't attack on sight - they watch you, and the longer you stay near them the more likely
 * they are to charge (as fast as you sprint, and they hit hard). Lions and tigers can be tamed with about 20 pieces of venison
 * - or they lose patience and attack. Walker forms attack at once.
 */
public class WildAnimal extends TamableAnimal implements GeoEntity {
    static final RawAnimation IDLE = RawAnimation.begin().thenLoop("misc.idle"), WALK = RawAnimation.begin().thenLoop("move.walk"),
            RUN = RawAnimation.begin().thenLoop("move.run"), ATTACK = RawAnimation.begin().thenPlay("attack.swing");
    public final WildKind kind;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int stalk, trust;
    private ServerPlayer watched;

    public WildAnimal(EntityType<? extends TamableAnimal> type, Level level, WildKind kind) {
        super(type, level);
        this.kind = kind;
        xpReward = kind.predator ? 10 : 3;
    }

    @Override
    protected void registerGoals() {
        WildKind k = kindSafe();
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        if (k == null) return;
        if (k.predator || k == WildKind.WILD_BOAR || k == WildKind.MOOSE || k.dog()) goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        if (k == WildKind.DEER) { goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 14f, 1.3, 1.6)); goalSelector.addGoal(3, new PanicGoal(this, 1.7)); }
        goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.2, 10f, 3f));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
        if (k.dog()) targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.Mob.class, 5, true, false,
                (e, l) -> isTame() && e instanceof net.minecraft.world.entity.monster.Enemy && !(e instanceof net.minecraft.world.entity.monster.Creeper)));
        if (k == WildKind.CAT) goalSelector.addGoal(3, new AvoidEntityGoal<>(this, net.minecraft.world.entity.Mob.class, 8f, 1.2, 1.5, e -> e instanceof net.minecraft.world.entity.monster.Enemy));
        if (k.zombie) { targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, false));
            targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, com.kiyamet.npc.HumanEntity.class, false)); }
        if (k.predator && !k.zombie) targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, WildAnimal.class, 10, true, false,
                (e, l) -> e instanceof WildAnimal w && (w.kind == WildKind.DEER || w.kind == WildKind.WILD_BOAR) && !isTame()));
    }

    /** registerGoals runs inside the super constructor, before kind is set */
    WildKind kindSafe() {
        if (kind != null) return kind;
        for (WildKind k : WildKind.values()) if (ModWild.TYPES.get(k) == getType()) return k;
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl) || kind == null) return;
        if (kind == WildKind.ICE_BEAR && tickCount % 20 == 0) sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 1, getZ(), 2, 0.4, 0.4, 0.4, 0.01);
        if (kind.pet() && isTame()) petTick(sl);
        if (!kind.predator || kind.zombie || isTame() || getTarget() != null || tickCount % 10 != 0) return;
        // stalking: the longer you hang around, the closer it gets to charging
        Player p = sl.getNearestPlayer(this, 18);
        if (p == null || p.isCreative() || p.isSpectator() || !hasLineOfSight(p)) { stalk = Math.max(0, stalk - 2); return; }
        getLookControl().setLookAt(p, 30f, 30f);
        if (distanceTo(p) > 9) getNavigation().moveTo(p, 0.6);
        else getNavigation().stop();
        stalk += sl.isDarkOutside() ? 2 : 1;
        if (p.isCrouching()) stalk--;
        if (stalk == 6 && p instanceof ServerPlayer sp) { playSound(SoundEvents.POLAR_BEAR_WARNING, 1f, kind == WildKind.LION || kind == WildKind.TIGER ? 0.6f : 0.9f);
            sp.displayClientMessage(Component.literal("The " + kind.title.toLowerCase() + " is watching you...").withStyle(ChatFormatting.GOLD), true); }
        if (stalk > 14 + random.nextInt(8)) { setTarget(p); stalk = 0; playSound(SoundEvents.RAVAGER_ROAR, 1.2f, 1.4f); }
    }

    /** dogs smell walkers and bark (marks them), huskies keep you warm, cats bring you little gifts at dawn */
    void petTick(ServerLevel sl) {
        if (!(getOwner() instanceof ServerPlayer owner) || owner.level() != sl) return;
        if (kind.dog() && tickCount % 100 == 0) {
            var foes = sl.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, getBoundingBox().inflate(kind == WildKind.SHEPHERD ? 28 : 20), e -> e instanceof net.minecraft.world.entity.monster.Enemy && e.isAlive());
            if (!foes.isEmpty()) {
                playSound(SoundEvents.WOLF_SOUNDS.get(net.minecraft.world.entity.animal.wolf.WolfSoundVariants.SoundSet.CLASSIC).growlSound().value(), 1.4f, 1f);
                for (var f : foes) f.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 110, 0, false, false));
                if (tickCount % 600 == 0) owner.displayClientMessage(Component.literal((hasCustomName() ? getCustomName().getString() : kind.title) + " smells " + foes.size() + " walker" + (foes.size() > 1 ? "s" : "") + " nearby!").withStyle(ChatFormatting.GOLD), true);
            }
        }
        if (kind == WildKind.CAT && sl.getDayTime() % 24000 == 0 && distanceTo(owner) < 20) {
            net.minecraft.world.item.Item[] gifts = {net.minecraft.world.item.Items.STRING, net.minecraft.world.item.Items.FEATHER, net.minecraft.world.item.Items.RABBIT_HIDE, net.minecraft.world.item.Items.IRON_NUGGET, net.minecraft.world.item.Items.COD, net.minecraft.world.item.Items.PHANTOM_MEMBRANE};
            spawnAtLocation(sl, new ItemStack(gifts[random.nextInt(gifts.length)]));
            owner.displayClientMessage(Component.literal("Your cat brought you a gift.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    /** a tamed husky lying next to you is like a small campfire */
    public static float petWarmth(Player p) {
        return p.level().getEntitiesOfClass(WildAnimal.class, p.getBoundingBox().inflate(4), w -> w.kind == WildKind.HUSKY && w.isTame() && w.isOwnedBy(p)).isEmpty() ? 0 : 4f;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity le) {
            le.knockback(0.8, getX() - le.getX(), getZ() - le.getZ());
            triggerAnim("main", "attack");
            if (kind != null && kind.zombie) le.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 80, 0));
        }
        return hit;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        if (kind == null) return super.mobInteract(player, hand);
        if (isTame() && isOwnedBy(player)) {
            if (s.is(ModItems.get("venison")) || s.is(ModItems.get("cooked_venison")) || (kind.dog() && isMeat(s)) || (kind == WildKind.CAT && s.is(net.minecraft.world.item.Items.COD))) { if (!level().isClientSide()) { heal(10); s.consume(1, player); } return InteractionResult.SUCCESS; }
            if (!level().isClientSide()) { setOrderedToSit(!isOrderedToSit()); setInSittingPose(isOrderedToSit()); getNavigation().stop(); setTarget(null); }
            return InteractionResult.SUCCESS;
        }
        if (kind.pet() && !isTame() && (kind.dog() ? (s.is(net.minecraft.world.item.Items.BONE) || s.has(net.minecraft.core.component.DataComponents.FOOD) && s.getItem() != net.minecraft.world.item.Items.ROTTEN_FLESH && isMeat(s))
                : (s.is(net.minecraft.world.item.Items.COD) || s.is(net.minecraft.world.item.Items.SALMON) || s.is(net.minecraft.world.item.Items.COOKED_COD)))) {
            if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
            s.consume(1, player);
            if (random.nextInt(3) == 0) {
                tame(sp); setOrderedToSit(false); level().broadcastEntityEvent(this, (byte) 7);
                sp.sendSystemMessage(Component.literal("The " + kind.title.toLowerCase() + " is your pet now! Right-click to make it sit or follow. Use a name tag to name it.").withStyle(ChatFormatting.GREEN));
            } else level().broadcastEntityEvent(this, (byte) 6);
            return InteractionResult.CONSUME;
        }
        if (kind.tameable && !kind.pet() && !isTame() && s.is(ModItems.get("venison"))) {
            if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
            s.consume(1, player);
            trust++; stalk = 0;
            ((ServerLevel) level()).sendParticles(ParticleTypes.HEART, getX(), getY() + 1.2, getZ(), 1, 0.3, 0.3, 0.3, 0);
            if (trust >= 20) {
                if (random.nextFloat() < 0.65f) {
                    tame(sp); setOrderedToSit(false);
                    level().broadcastEntityEvent(this, (byte) 7);
                    sp.sendSystemMessage(Component.literal("The " + kind.title.toLowerCase() + " trusts you now. It's yours.").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                } else {
                    trust = 0; setTarget(sp);
                    playSound(SoundEvents.RAVAGER_ROAR, 1.2f, 1.2f);
                    sp.sendSystemMessage(Component.literal("It lost its patience!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                }
            } else sp.displayClientMessage(Component.literal("Trust: " + trust + " / 20").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource src, boolean byPlayer) {
        super.dropCustomDeathLoot(level, src, byPlayer);
        if (kind == null) return;
        if (!kind.meat.isEmpty()) spawnAtLocation(level, new ItemStack(ModItems.get(isOnFire() ? "cooked_" + kind.meat : kind.meat), 1 + random.nextInt(kind.predator ? 3 : 2)));
        if (kind.zombie) spawnAtLocation(level, new ItemStack(net.minecraft.world.item.Items.ROTTEN_FLESH, 1 + random.nextInt(3)));
        if (!kind.pelt.isEmpty() && random.nextFloat() < (kind.zombie ? 0.25f : 0.8f)) spawnAtLocation(level, new ItemStack(ModItems.get(kind.pelt)));
        if (kind == WildKind.DEER || kind == WildKind.MOOSE || kind == WildKind.WILD_BOAR) spawnAtLocation(level, new ItemStack(net.minecraft.world.item.Items.LEATHER, 1 + random.nextInt(2)));
    }

    static boolean isMeat(ItemStack s) {
        String n = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
        return n.contains("beef") || n.contains("pork") || n.contains("chicken") || n.contains("mutton") || n.contains("rabbit") || n.contains("meat") || n.contains("venison");
    }

    @Override public boolean isFood(ItemStack s) { return false; }
    @Override public AgeableMob getBreedOffspring(ServerLevel l, AgeableMob m) { return null; }
    @Override public boolean removeWhenFarAway(double d) { return kind != null && kind.zombie && !isTame() && !hasCustomName(); }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); out.putInt("Trust", trust); }
    @Override
    protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); trust = in.getIntOr("Trust", 0); }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<WildAnimal>("main", 4, t -> t.setAndContinue(isInSittingPose() ? IDLE : t.isMoving() ? (getTarget() != null || getDeltaMovement().horizontalDistanceSqr() > 0.02 ? RUN : WALK) : IDLE))
                .triggerableAnim("attack", ATTACK));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
