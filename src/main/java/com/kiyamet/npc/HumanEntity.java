package com.kiyamet.npc;

import com.kiyamet.boss.BossEntity;
import com.kiyamet.entity.KZombie;
import com.kiyamet.robot.RobotEntity;
import com.kiyamet.ui.Ui;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Every living person in the apocalypse: survivors, community members, raiders, masked ones and companions.
 * One level (1-300) drives health, armour and weapon damage. Companions level up from their kills.
 */
public class HumanEntity extends PathfinderMob implements GeoEntity {
    public static final int MAX_LEVEL = 300;
    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("misc.idle");
    public static final RawAnimation WALK = RawAnimation.begin().thenLoop("move.walk");
    public static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack.swing");

    private static final EntityDataAccessor<Integer> APPEARANCE = SynchedEntityData.defineId(HumanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(HumanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FACTION = SynchedEntityData.defineId(HumanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(HumanEntity.class, EntityDataSerializers.INT);

    public enum Order { FOLLOW, STAY, GUARD }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean initialized;
    private int xp;
    @Nullable private UUID owner;
    private Order order = Order.FOLLOW;
    @Nullable private BlockPos home;
    private String community = "", communityType = "";
    private int shotCooldown;
    @Nullable private ServerBossEvent bar;

    public HumanEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCanPickUpLoot(false);
    }

    // ---------------------------------------------------------------- data

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(APPEARANCE, 0);
        b.define(LEVEL, 1);
        b.define(FACTION, Faction.SURVIVOR.ordinal());
        b.define(ROLE, Role.MEMBER.ordinal());
    }

    public Appearance appearance() { return Appearance.unpack(entityData.get(APPEARANCE)); }
    public int appearanceCode() { return entityData.get(APPEARANCE); }
    public void setAppearance(Appearance a) { entityData.set(APPEARANCE, a.pack()); }
    public int lvl() { return entityData.get(LEVEL); }
    public Faction faction() { return Faction.byId(entityData.get(FACTION)); }
    public Role role() { return Role.byId(entityData.get(ROLE)); }
    public boolean isWhisperer() { return faction() == Faction.WHISPERER; }
    public String community() { return community; }
    public String communityType() { return communityType; }
    @Nullable public UUID owner() { return owner; }
    public Order order() { return order; }
    public int xp() { return xp; }
    public static int xpToNext(int level) { return 40 + level * 10; }

    public boolean isOwnedBy(Player p) { return owner != null && owner.equals(p.getUUID()); }

    @Nullable public ServerPlayer ownerPlayer() {
        return owner != null && level() instanceof ServerLevel sl ? sl.getServer().getPlayerList().getPlayer(owner) : null;
    }

    /** Sets everything at once (used by spawners and the structure loader). */
    public void setup(Faction f, Role r, int level, @Nullable String communityType) {
        entityData.set(FACTION, f.ordinal());
        entityData.set(ROLE, r.ordinal());
        entityData.set(LEVEL, Math.max(1, Math.min(MAX_LEVEL, level)));
        if (communityType != null) this.communityType = communityType;
        if (entityData.get(APPEARANCE) == 0) setAppearance(Appearance.random(random, f, r));
        if (getMainHandItem().isEmpty()) setItemSlot(EquipmentSlot.MAINHAND, Weapon.random(random, f, r, level));
        setDropChance(EquipmentSlot.MAINHAND, f == Faction.COMPANION ? 1f : 0.15f);
        String name = Names.random(random, f, r);
        if (name != null && !hasCustomName()) setCustomName(Component.literal(name));
        initialized = true;
        applyStats(true);
    }

    public void applyStats(boolean heal) {
        int lv = lvl();
        Role r = role();
        set(Attributes.MAX_HEALTH, (20 + lv * 1.1) * r.hpMult);
        set(Attributes.ATTACK_DAMAGE, (2 + lv * 0.07) * r.dmgMult);
        set(Attributes.ARMOR, Math.min(24, lv * 0.075 * r.hpMult));
        set(Attributes.ARMOR_TOUGHNESS, Math.min(12, lv * 0.03));
        set(Attributes.MOVEMENT_SPEED, faction() == Faction.WHISPERER ? 0.2 : 0.3 + Math.min(0.06, lv * 0.0002));
        set(Attributes.KNOCKBACK_RESISTANCE, Math.min(0.8, lv / 375.0));
        if (heal) setHealth(getMaxHealth());
        refreshBar();
    }

    private void set(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> a, double v) {
        AttributeInstance inst = getAttribute(a);
        if (inst != null) inst.setBaseValue(v);
    }

    public void setOwner(@Nullable ServerPlayer p) {
        owner = p == null ? null : p.getUUID();
        if (p != null) {
            entityData.set(FACTION, Faction.COMPANION.ordinal());
            community = "";
            home = null;
            order = Order.FOLLOW;
            setDropChance(EquipmentSlot.MAINHAND, 1f);
        } else entityData.set(FACTION, Faction.SURVIVOR.ordinal());
        setTarget(null);
    }

    public void setOrder(Order o) {
        order = o;
        if (o != Order.FOLLOW) home = blockPosition();
        getNavigation().stop();
    }

    public void setAppearanceCode(int code) { entityData.set(APPEARANCE, code); }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("Appearance", entityData.get(APPEARANCE));
        out.putInt("Level", lvl());
        out.putString("Faction", faction().name());
        out.putString("Role", role().name());
        out.putInt("Xp", xp);
        out.putString("Order", order.name());
        out.putString("Community", community);
        out.putString("CommunityType", communityType);
        if (owner != null) out.putString("Owner", owner.toString());
        if (home != null) out.putLong("Home", home.asLong());
        out.putBoolean("Ready", initialized);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        entityData.set(APPEARANCE, in.getIntOr("Appearance", 0));
        entityData.set(LEVEL, Math.max(1, in.getIntOr("Level", 1)));
        try { entityData.set(FACTION, Faction.valueOf(in.getStringOr("Faction", "SURVIVOR")).ordinal()); } catch (IllegalArgumentException ignored) {}
        try { entityData.set(ROLE, Role.valueOf(in.getStringOr("Role", "MEMBER")).ordinal()); } catch (IllegalArgumentException ignored) {}
        xp = in.getIntOr("Xp", 0);
        try { order = Order.valueOf(in.getStringOr("Order", "FOLLOW")); } catch (IllegalArgumentException ignored) {}
        community = in.getStringOr("Community", "");
        communityType = in.getStringOr("CommunityType", "");
        String o = in.getStringOr("Owner", "");
        owner = o.isEmpty() ? null : UUID.fromString(o);
        long h = in.getLongOr("Home", Long.MIN_VALUE);
        home = h == Long.MIN_VALUE ? null : BlockPos.of(h);
        initialized = in.getBooleanOr("Ready", false);
    }

    // ---------------------------------------------------------------- AI

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        goalSelector.addGoal(2, new ShootGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true) {
            @Override public boolean canUse() { return !Weapon.ranged(getMainHandItem()) || closeToTarget() ? super.canUse() : false; }
            @Override public boolean canContinueToUse() { return (!Weapon.ranged(getMainHandItem()) || closeToTarget()) && super.canContinueToUse(); }
        });
        goalSelector.addGoal(4, new FollowOwnerGoal());
        goalSelector.addGoal(5, new StayHomeGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7) {
            @Override public boolean canUse() { return order == Order.FOLLOW && owner == null && super.canUse(); }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(HumanEntity.class));
        targetSelector.addGoal(2, new OwnerDefenseGoal());
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (t, l) -> isHostileTo(t)));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, HumanEntity.class, 10, true, false, (t, l) -> isHostileTo(t)));
        targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                (t, l) -> !isWhisperer() && (t instanceof KZombie || t instanceof RobotEntity || t instanceof BossEntity && owner != null)));
    }

    private boolean closeToTarget() {
        LivingEntity t = getTarget();
        return t != null && distanceTo(t) < 3.5;
    }

    /** Who this person wants to fight. */
    public boolean isHostileTo(LivingEntity e) {
        if (e == this || !e.isAlive()) return false;
        Faction f = faction();
        if (e instanceof Player p) {
            if (p.isCreative() || p.isSpectator()) return false;
            return switch (f) {
                case RAIDER, WHISPERER -> true;
                case COMMUNITY -> Communities.reputation(p, community) < 0;
                default -> false;
            };
        }
        if (e instanceof HumanEntity h) {
            Faction o = h.faction();
            boolean bad = o == Faction.RAIDER || o == Faction.WHISPERER;
            boolean me = f == Faction.RAIDER || f == Faction.WHISPERER;
            if (f == Faction.COMPANION) return bad || (o == Faction.COMMUNITY && ownerPlayer() != null && Communities.reputation(ownerPlayer(), h.community) < 0 && h.getTarget() != null);
            if (me) return !bad;                                         // raiders and masked ones prey on everyone else
            return bad;                                                  // decent folk fight raiders
        }
        return false;
    }

    class ShootGoal extends Goal {
        ShootGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive() && Weapon.ranged(getMainHandItem()) && !closeToTarget();
        }

        @Override public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            getLookControl().setLookAt(t, 30f, 30f);
            double d = distanceTo(t);
            boolean see = hasLineOfSight(t);
            if (d > 14 || !see) getNavigation().moveTo(t, 1.05);
            else getNavigation().stop();
            if (shotCooldown > 0 || !see || d > 24 || !(level() instanceof ServerLevel sl)) return;
            ItemStack w = getMainHandItem();
            shotCooldown = w.getItem() instanceof com.kiyamet.item.GunItem ? 22 : w.is(Items.CROSSBOW) ? 30 : 26;
            if (role() == Role.COMMANDER) shotCooldown = 12;
            Vec3 from = getEyePosition(), to = t.getEyePosition();
            float spread = (float) Math.max(0.05, 0.5 - lvl() / 600.0);
            boolean hit = random.nextFloat() > spread * 0.6;
            int n = (int) (from.distanceTo(to) * 2);
            for (int i = 0; i < n; i++) { Vec3 at = from.add(to.subtract(from).scale(i / (double) n)); sl.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 1, 0, 0, 0, 0); }
            sl.playSound(null, blockPosition(), w.getItem() instanceof com.kiyamet.item.GunItem ? SoundEvents.CROSSBOW_SHOOT : SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 1f, 0.9f + random.nextFloat() * 0.2f);
            triggerAnim("attack", "attack");
            if (hit) {
                float dmg = (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.85 * Weapon.tier(w));
                t.hurtServer(sl, sl.damageSources().arrow(null, HumanEntity.this), dmg);
            }
            if (w.getItem() instanceof com.kiyamet.item.GunItem) com.kiyamet.world.NoiseSystem.make(sl, blockPosition(), 20, 2);
        }
    }

    class FollowOwnerGoal extends Goal {
        FollowOwnerGoal() { setFlags(EnumSet.of(Flag.MOVE)); }

        @Override public boolean canUse() {
            ServerPlayer p = ownerPlayer();
            return order == Order.FOLLOW && p != null && p.level() == level() && distanceTo(p) > 5 && getTarget() == null;
        }

        @Override public boolean canContinueToUse() { return canUse() && ownerPlayer() != null && distanceTo(ownerPlayer()) > 3; }

        @Override public void tick() {
            ServerPlayer p = ownerPlayer();
            if (p == null) return;
            getLookControl().setLookAt(p, 10f, getMaxHeadXRot());
            if (distanceTo(p) > 28) { teleportTo(p.getX() + random.nextInt(3) - 1, p.getY(), p.getZ() + random.nextInt(3) - 1); getNavigation().stop(); }
            else getNavigation().moveTo(p, distanceTo(p) > 10 ? 1.3 : 1.0);
        }
    }

    class StayHomeGoal extends Goal {
        StayHomeGoal() { setFlags(EnumSet.of(Flag.MOVE)); }

        private int radius() { return order == Order.STAY ? 1 : order == Order.GUARD ? 10 : 18; }

        @Override public boolean canUse() {
            if (home == null || order == Order.FOLLOW && owner != null) return false;
            return home.distSqr(blockPosition()) > radius() * radius() && (getTarget() == null || order == Order.STAY);
        }

        @Override public void start() { getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0); }

        @Override public boolean canContinueToUse() { return home != null && !getNavigation().isDone() && home.distSqr(blockPosition()) > 4; }
    }

    class OwnerDefenseGoal extends Goal {
        @Override public boolean canUse() {
            ServerPlayer p = ownerPlayer();
            if (p == null) return false;
            LivingEntity a = p.getLastHurtByMob();
            LivingEntity b = p.getLastHurtMob();
            LivingEntity pick = a != null && a.isAlive() && a != HumanEntity.this ? a : b != null && b.isAlive() && !(b instanceof HumanEntity h && h.isOwnedBy(p)) ? b : null;
            if (pick == null || pick == getTarget() || pick.distanceTo(HumanEntity.this) > 32) return false;
            setTarget(pick);
            return false;
        }
    }

    // ---------------------------------------------------------------- ticking

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (!initialized) setup(faction(), role(), lvl(), null);
        if (shotCooldown > 0) shotCooldown--;
        if (home == null && owner == null && faction() == Faction.COMMUNITY) home = blockPosition();
        if (faction() == Faction.COMMUNITY && community.isEmpty() && tickCount > 5) community = Communities.assign(sl, this);
        if (bar != null) bar.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 100 == 0 && getHealth() < getMaxHealth() && getTarget() == null) heal(1 + lvl() / 60f);
        if (faction() == Faction.COMPANION && owner != null && tickCount % 40 == 0 && ownerPlayer() == null && order == Order.FOLLOW) setOrder(Order.STAY);
    }

    public void setCommunity(String key) { community = key; }

    private void refreshBar() {
        if (role() == Role.COMMANDER && bar == null) {
            bar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_6);
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (bar != null) bar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (bar != null) bar.removePlayer(player);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        float base = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * Weapon.tier(getMainHandItem());
        boolean hit = target.hurtServer(level, level.damageSources().mobAttack(this), base);
        if (hit) { triggerAnim("attack", "attack"); swing(InteractionHand.MAIN_HAND); }
        return hit;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker instanceof Player p && isOwnedBy(p)) return false;                               // no friendly fire
        if (attacker instanceof HumanEntity h && h.owner != null && owner != null && h.owner.equals(owner)) return false;
        if (attacker instanceof ServerPlayer p && faction() == Faction.COMMUNITY) Communities.onMemberHurt(level, this, p);
        return super.hurtServer(level, source, amount * com.kiyamet.twd.Headshot.multiplier(level, this, source));
    }

    /** Companions earn XP from kills and level up to 300. */
    public void onKill(ServerLevel level, LivingEntity victim) {
        if (faction() != Faction.COMPANION) return;
        int gain = victim instanceof BossEntity ? 400 : victim instanceof HumanEntity h ? 20 + h.lvl() / 3 : victim instanceof RobotEntity ? 25 : 10;
        xp += gain;
        while (lvl() < MAX_LEVEL && xp >= xpToNext(lvl())) {
            xp -= xpToNext(lvl());
            entityData.set(LEVEL, lvl() + 1);
            applyStats(false);
            heal(getMaxHealth() * 0.25f);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 1, getZ(), 30, 0.4, 0.6, 0.4, 0.3);
            level.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.8f, 1.2f);
            ServerPlayer p = ownerPlayer();
            if (p != null && lvl() % 5 == 0)
                p.sendSystemMessage(Component.literal(getName().getString() + " reached level " + lvl() + "!").withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel sl)) return;
        if (faction() != Faction.COMPANION) {
            int rubies = lvl() / 40 + (role() == Role.COMMANDER ? 12 : role() == Role.LEADER ? 4 : 0);
            if (rubies > 0) sl.addFreshEntity(new ItemEntity(sl, getX(), getY() + 0.5, getZ(), new ItemStack(Ui.item("kiyamet:ruby"), rubies)));
            if (random.nextFloat() < 0.4f) sl.addFreshEntity(new ItemEntity(sl, getX(), getY() + 0.5, getZ(), new ItemStack(Items.ARROW, 4 + random.nextInt(12))));
            if (random.nextFloat() < 0.2f) sl.addFreshEntity(new ItemEntity(sl, getX(), getY() + 0.5, getZ(), new ItemStack(Ui.item("kiyamet:bandage"))));
        } else {
            ServerPlayer p = ownerPlayer();
            if (p != null) p.sendSystemMessage(Component.literal(getName().getString() + " has died.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        }
        if (bar != null) bar.removeAllPlayers();
        // Everyone is infected: the dead come back.
        if (faction() != Faction.WHISPERER && random.nextFloat() < 0.6f) {
            KZombie z = com.kiyamet.entity.ModEntities.ZOMBIES.get(com.kiyamet.entity.ZombieKind.RUNNER).create(sl, net.minecraft.world.entity.EntitySpawnReason.CONVERSION);
            if (z != null) {
                z.setPos(getX(), getY(), getZ());
                sl.getServer().execute(() -> { if (sl.getEntity(z.getUUID()) == null) sl.addFreshEntity(z); });
            }
        }
    }

    // ---------------------------------------------------------------- interaction

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.NAME_TAG)) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp) || !(level() instanceof ServerLevel)) return InteractionResult.SUCCESS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (getTarget() == player) return InteractionResult.FAIL;
        switch (faction()) {
            case COMPANION -> {
                if (isOwnedBy(sp)) CompanionMenu.open(sp, this);
                else sp.displayClientMessage(Component.literal("This companion follows someone else."), true);
            }
            case SURVIVOR -> RecruitMenu.open(sp, this);
            case COMMUNITY -> {
                if (Communities.reputation(sp, community) < 0) { sp.displayClientMessage(Component.literal("They don't trust you.").withStyle(ChatFormatting.RED), true); break; }
                switch (role()) {
                    case TRADER -> TradeMenu.open(sp, this);
                    case LEADER -> LeaderMenu.open(sp, this);
                    default -> sp.displayClientMessage(Component.literal(getName().getString() + ": " + Communities.greeting(random, communityType)), false);
                }
            }
            default -> {}
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public Component getTypeName() {
        return Component.literal(faction() == Faction.COMMUNITY && role() == Role.MEMBER ? Communities.title(communityType) : role() == Role.MEMBER ? faction().title : faction().title + " " + role().title);
    }

    @Override
    public Component getDisplayName() {
        Component base = hasCustomName() ? getCustomName() : getTypeName();
        ChatFormatting c = switch (faction()) {
            case RAIDER, WHISPERER -> ChatFormatting.RED;
            case COMPANION -> ChatFormatting.AQUA;
            case COMMUNITY -> ChatFormatting.GREEN;
            default -> ChatFormatting.WHITE;
        };
        return Component.empty().append(base).append(Component.literal(" [Lv " + lvl() + "]").withStyle(ChatFormatting.GRAY)).withStyle(c);
    }

    @Override
    public boolean shouldShowName() { return faction() == Faction.COMPANION || role() == Role.LEADER || role() == Role.COMMANDER || role() == Role.TRADER || super.shouldShowName(); }

    // ---------------------------------------------------------------- GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<HumanEntity>("main", 4, t -> t.setAndContinue(t.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<HumanEntity>("attack", 0, t -> PlayState.STOP).triggerableAnim("attack", ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    public List<HumanEntity> allies(ServerLevel sl, double r) {
        return sl.getEntitiesOfClass(HumanEntity.class, getBoundingBox().inflate(r), h -> h != this && !community.isEmpty() && community.equals(h.community));
    }
}
