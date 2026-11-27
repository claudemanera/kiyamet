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

    public enum Order { FOLLOW, STAY, GUARD, GATHER }
    /** what a GATHER order collects (see Gatherer) */
    public String gather = "";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean initialized;
    private int xp;
    @Nullable private UUID owner;
    private Order order = Order.FOLLOW;
    @Nullable private BlockPos home;
    private String community = "", communityType = "";
    private int shotCooldown;
    private Job job = Job.NONE;
    private int team;

    public int team() { return team; }
    public void setTeam(int t) { team = Math.floorMod(t, 3); }
    /** Game time when the current supply run ends (0 = not away). */
    private long awayUntil;
    private int awayMinutes, awayGroup;

    public Job job() { return job; }
    public void setJob(Job j) { job = j; applyStats(false); }
    public boolean isAway() { return awayUntil > 0; }
    public long awayUntil() { return awayUntil; }
    private static final EntityDataAccessor<Integer> GEAR = SynchedEntityData.defineId(HumanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PROFESSION = SynchedEntityData.defineId(HumanEntity.class, EntityDataSerializers.INT);

    public Specialty specialty() { return profession().specialty(); }
    public Profession profession() { int p = entityData.get(PROFESSION); return p < 0 ? Profession.SURVIVOR : Profession.values()[Math.min(p, Profession.values().length - 1)]; }
    public boolean hasProfession() { return entityData.get(PROFESSION) >= 0; }
    public Gear gear() { return Gear.unpack(entityData.get(GEAR)); }
    public int gearCode() { return entityData.get(GEAR); }
    public void setGear(Gear g) { entityData.set(GEAR, g.pack()); }
    public void setProfession(Profession p) { entityData.set(PROFESSION, p.ordinal()); }
    @Nullable private ServerBossEvent bar;
    /** Companion backpack: 27 slots, opened from the companion panel. Medkits and bandages inside are used automatically. */
    private final net.minecraft.world.SimpleContainer inventory = new net.minecraft.world.SimpleContainer(27);

    public net.minecraft.world.SimpleContainer inventory() { return inventory; }

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
        b.define(GEAR, 0);
        b.define(PROFESSION, -1);
    }

    public Appearance appearance() { return Appearance.unpack(entityData.get(APPEARANCE)); }
    public int appearanceCode() { return entityData.get(APPEARANCE); }
    public void setAppearance(Appearance a) { entityData.set(APPEARANCE, a.pack()); }
    public int lvl() { return entityData.get(LEVEL); }
    public Faction faction() { return Faction.byId(entityData.get(FACTION)); }
    public Role role() { return Role.byId(entityData.get(ROLE)); }
    public boolean isWhisperer() { return faction() == Faction.WHISPERER; }
    /** Part of a base raid: allied with the walkers it came with. */
    public boolean raiding;
    /** A raider envoy who came to demand tribute: peaceful until refused. */
    public boolean envoy;
    public final Captives.Data captives = new Captives.Data();
    /** Companion morale 0-100 (see Mind). */
    public float morale = 70;
    /** 1.9 settlement + family */
    public String work = "";                       // settlement job: FARMER, SMITH, DOCTOR, GUARD, COOK ("" = not settled)
    @Nullable public BlockPos settleAt;            // the settlement bell they live by
    public boolean partner;                        // your partner
    public long pregnantDay = -1, bornDay = -1;    // calendar day index
    public @Nullable BlockPos home() { return home; }
    public void setHome(@Nullable BlockPos h) { home = h; }
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
        boolean fresh = entityData.get(APPEARANCE) == 0;
        boolean female = fresh ? random.nextFloat() < (f == Faction.RAIDER ? 0.3f : 0.42f) : appearance().female();
        if (!hasProfession()) setProfession(Profession.choose(random, f, r, this.communityType, female));
        Profession prof = profession();
        if (prof.gender == 2) female = true;
        if (prof.gender == 1) female = false;
        if (fresh) {
            setAppearance(prof.dress(random, Appearance.random(random, f, r, female)));
            setGear(prof.gear(random));
        }
        if (getMainHandItem().isEmpty()) {
            String w = prof.randomWeapon(random);
            setItemSlot(EquipmentSlot.MAINHAND, w.isEmpty() ? Weapon.random(random, f, r, level) : new ItemStack(Ui.item(w)));
        }
        setDropChance(EquipmentSlot.MAINHAND, f == Faction.COMPANION ? 1f : 0.15f);
        String name = Names.random(random, f, r, appearance().female());
        if (name != null && !hasCustomName()) setCustomName(Component.literal(name));
        initialized = true;
        applyStats(true);
    }

    public void applyStats(boolean heal) {
        int lv = lvl();
        Role r = role();
        set(Attributes.MAX_HEALTH, (20 + lv * 1.1) * r.hpMult * (job == Job.GUARDIAN ? 1.4 : 1.0));
        set(Attributes.ATTACK_DAMAGE, (2 + lv * 0.07) * r.dmgMult);
        set(Attributes.ARMOR, Math.min(30, lv * 0.075 * r.hpMult + (job == Job.GUARDIAN ? 6 : 0)));
        set(Attributes.ARMOR_TOUGHNESS, Math.min(12, lv * 0.03));
        set(Attributes.MOVEMENT_SPEED, (faction() == Faction.WHISPERER ? 0.2 : 0.3 + Math.min(0.06, lv * 0.0002)) * (job == Job.SCOUT ? 1.15 : 1.0));
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

    /** 1.9: a companion whose mind broke turns on you - steals something and joins the raiders. */
    public void betray(ServerPlayer from) {
        owner = null; community = ""; home = null;
        entityData.set(FACTION, Faction.RAIDER.ordinal());
        var inv = from.getInventory();
        java.util.List<Integer> slots = new java.util.ArrayList<>();
        for (int i = 9; i < 36; i++) if (!inv.getItem(i).isEmpty()) slots.add(i);
        if (!slots.isEmpty()) {
            int i = slots.get(random.nextInt(slots.size()));
            ItemStack stolen = inv.removeItemNoUpdate(i);
            from.sendSystemMessage(Component.literal(getName().getString() + " stole your " + stolen.getHoverName().getString() + " (x" + stolen.getCount() + ")! Kill them to get it back.").withStyle(ChatFormatting.RED));
            setItemSlot(EquipmentSlot.OFFHAND, stolen); setDropChance(EquipmentSlot.OFFHAND, 1f);
        }
        setTarget(from);
        refreshBar();
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
        out.putBoolean("Raiding", raiding);
        out.putBoolean("Envoy", envoy);
        Captives.save(out, captives);
        out.putFloat("Morale", morale);
        out.putString("Faction", faction().name());
        out.putString("Role", role().name());
        out.putInt("Xp", xp);
        out.putString("Profession", profession().name());
        out.putString("Job", job.name());
        out.putInt("KTeam", team);
        out.putLong("AwayUntil", awayUntil);
        out.putInt("AwayMinutes", awayMinutes);
        out.putInt("AwayGroup", awayGroup);
        out.putInt("Gear", entityData.get(GEAR));
        out.putString("Order", order.name());
        out.putString("Gather", gather);
        out.putString("Community", community);
        out.putString("Work", work);
        if (settleAt != null) out.putLong("SettleAt", settleAt.asLong());
        out.putBoolean("Partner", partner);
        out.putLong("PregnantDay", pregnantDay);
        out.putLong("BornDay", bornDay);
        out.putString("CommunityType", communityType);
        if (owner != null) out.putString("Owner", owner.toString());
        if (home != null) out.putLong("Home", home.asLong());
        out.putBoolean("Ready", initialized);
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) items.add(inventory.getItem(i));
        out.store("Inventory", ItemStack.OPTIONAL_CODEC.listOf(), items);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        entityData.set(APPEARANCE, in.getIntOr("Appearance", 0));
        entityData.set(LEVEL, Math.max(1, in.getIntOr("Level", 1)));
        raiding = in.getBooleanOr("Raiding", false);
        envoy = in.getBooleanOr("Envoy", false);
        Captives.load(in, captives);
        morale = in.getFloatOr("Morale", 70f);
        work = in.getStringOr("Work", "");
        long sa = in.getLongOr("SettleAt", Long.MIN_VALUE); settleAt = sa == Long.MIN_VALUE ? null : BlockPos.of(sa);
        partner = in.getBooleanOr("Partner", false);
        pregnantDay = in.getLongOr("PregnantDay", -1L);
        bornDay = in.getLongOr("BornDay", -1L);
        try { entityData.set(FACTION, Faction.valueOf(in.getStringOr("Faction", "SURVIVOR")).ordinal()); } catch (IllegalArgumentException ignored) {}
        try { entityData.set(ROLE, Role.valueOf(in.getStringOr("Role", "MEMBER")).ordinal()); } catch (IllegalArgumentException ignored) {}
        xp = in.getIntOr("Xp", 0);
        job = Job.byName(in.getStringOr("Job", "NONE"));
        team = in.getIntOr("KTeam", 0);
        awayUntil = in.getLongOr("AwayUntil", 0L);
        awayMinutes = in.getIntOr("AwayMinutes", 0);
        awayGroup = in.getIntOr("AwayGroup", 1);
        String prof = in.getStringOr("Profession", "");
        if (!prof.isEmpty()) setProfession(Profession.byName(prof));
        else switch (Specialty.byName(in.getStringOr("Specialty", "NONE"))) {
            case NURSE -> setProfession(Profession.NURSE);
            case HUNTRESS -> setProfession(Profession.HUNTRESS);
            case SCAVENGER -> setProfession(Profession.SCAVENGER);
            case ENGINEER -> setProfession(Profession.ENGINEER);
            default -> {}
        }
        entityData.set(GEAR, in.getIntOr("Gear", 0));
        try { order = Order.valueOf(in.getStringOr("Order", "FOLLOW")); } catch (IllegalArgumentException ignored) {}
        gather = in.getStringOr("Gather", "");
        community = in.getStringOr("Community", "");
        communityType = in.getStringOr("CommunityType", "");
        String o = in.getStringOr("Owner", "");
        owner = o.isEmpty() ? null : UUID.fromString(o);
        long h = in.getLongOr("Home", Long.MIN_VALUE);
        home = h == Long.MIN_VALUE ? null : BlockPos.of(h);
        initialized = in.getBooleanOr("Ready", false);
        in.read("Inventory", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(l -> { for (int i = 0; i < l.size() && i < inventory.getContainerSize(); i++) inventory.setItem(i, l.get(i)); });
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
        goalSelector.addGoal(4, new Gatherer(this));
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
                (t, l) -> !isWhisperer() && !(raiding && t instanceof KZombie) && (t instanceof KZombie || t instanceof RobotEntity || t instanceof BossEntity && owner != null)));
    }

    private boolean closeToTarget() {
        LivingEntity t = getTarget();
        return t != null && distanceTo(t) < 3.5;
    }

    /** Who this person wants to fight. */
    public boolean isHostileTo(LivingEntity e) {
        if (e == this || !e.isAlive()) return false;
        if (envoy || e instanceof HumanEntity he && (he.envoy || he.captives.captive) || captives.captive) return false;
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
            if (specialty() == Specialty.HUNTRESS) shotCooldown = (int) (shotCooldown * 0.7);
            if (job == Job.GUNNER) shotCooldown = (int) (shotCooldown * 0.8);
            Vec3 from = getEyePosition(), to = t.getEyePosition();
            float spread = (float) Math.max(0.05, 0.5 - lvl() / 600.0);
            boolean hit = random.nextFloat() > spread * 0.6 || specialty() == Specialty.HUNTRESS && d < 12;
            int n = (int) (from.distanceTo(to) * 2);
            for (int i = 0; i < n; i++) { Vec3 at = from.add(to.subtract(from).scale(i / (double) n)); sl.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 1, 0, 0, 0, 0); }
            sl.playSound(null, blockPosition(), w.getItem() instanceof com.kiyamet.item.GunItem ? SoundEvents.CROSSBOW_SHOOT : SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 1f, 0.9f + random.nextFloat() * 0.2f);
            triggerAnim("attack", "attack");
            if (hit) {
                float dmg = (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.85 * Weapon.tier(w) * (specialty() == Specialty.HUNTRESS ? 1.3 : 1.0) * (job == Job.GUNNER ? 1.25 : 1.0));
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
            if (home == null || order == Order.GATHER || order == Order.FOLLOW && owner != null) return false;
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
        if (awayUntil > 0) { tickAway(sl); return; }
        if (owner != null && (tickCount + getId()) % 1200 == 0) com.kiyamet.settle.Settlement.work(sl, this);
        if (bornDay >= 0 && tickCount % 40 == 0) com.kiyamet.settle.Family.grow(sl, this);
        if (bornDay >= 0 && getTags().contains(com.kiyamet.settle.Family.CARRY_TAG)) com.kiyamet.settle.Family.follow(sl, this);
        if (owner != null && tickCount % 60 == 0) Radio.report(this);
        if (owner != null && job == Job.SCOUT && tickCount % 100 == 0) scout(sl);
        if (owner != null && job == Job.GUARDIAN && tickCount % 40 == 0) guard(sl);
        if (owner != null && job == Job.LOOTER && tickCount % 6000 == 3000) scavenge();
        Captives.tick(sl, this, captives);
        if (faction() == Faction.COMPANION && tickCount % 20 == 3 && ownerPlayer() instanceof ServerPlayer mo && mo.level() == sl && !isAway()) Mind.companion(sl, this, mo);
        // 1.6: ride along in the owner's vehicle, get out when they do
        if (faction() == Faction.COMPANION && tickCount % 10 == 7 && ownerPlayer() instanceof ServerPlayer op && op.level() == sl) {
            if (op.getVehicle() instanceof com.kiyamet.vehicle.VehicleEntity v && order == Order.FOLLOW && getVehicle() != v && distanceTo(v) < 16
                    && v.getPassengers().size() < v.kind.seats.length) { stopRiding(); startRiding(v, true, true); }
            else if (getVehicle() instanceof com.kiyamet.vehicle.VehicleEntity v && op.getVehicle() != v && v.autoDest == null) stopRiding();
        }
        if (raiding && !captives.captive) com.kiyamet.defense.DefenseRules.tickRaider(sl, this);
        if (faction() == Faction.COMPANION && tickCount % 20 == 7 && random.nextInt(450) == 0 && ownerPlayer() instanceof ServerPlayer op
                && op.level() == sl && distanceTo(op) < 24 && !Talk.busy(op) && !isAway()) Dialogues.chatter(op, this);
        if (role() == Role.LEADER && faction() == Faction.COMMUNITY && tickCount % 400 == 100
                && sl.getEntitiesOfClass(com.kiyamet.twd.MissionBoard.class, getBoundingBox().inflate(40)).isEmpty()) {
            com.kiyamet.twd.MissionBoard b = com.kiyamet.twd.ModTwd.MISSION_BOARD.create(sl, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if (b != null) {
                BlockPos at = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockPosition().offset(3, 0, 2));
                b.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
                b.community = community;
                sl.addFreshEntity(b);
            }
        }
        if (shotCooldown > 0) shotCooldown--;
        if (home == null && owner == null && faction() == Faction.COMMUNITY) home = blockPosition();
        if (faction() == Faction.COMMUNITY && community.isEmpty() && tickCount > 5) community = Communities.assign(sl, this);
        if (bar != null) bar.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 100 == 0 && getHealth() < getMaxHealth() && getTarget() == null) heal(1 + lvl() / 60f);
        if (faction() == Faction.COMPANION && owner != null && tickCount % 40 == 0 && ownerPlayer() == null && order == Order.FOLLOW) setOrder(Order.STAY);
        if (tickCount % 40 == 0 && getHealth() < getMaxHealth() * 0.4f) selfHeal();
        if (specialty() == Specialty.NURSE && tickCount % 200 == 0) nurse(sl);
        if (specialty() == Specialty.ENGINEER && tickCount % 300 == 150) engineer(sl);
        if (specialty() == Specialty.SCAVENGER && owner != null && tickCount % 3600 == 1800) scavenge();
    }

    public void setCommunity(String key) { community = key; }

    // ---------------------------------------------------------------- jobs & supply runs

    private void scout(ServerLevel sl) {
        List<net.minecraft.world.entity.Mob> foes = sl.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, getBoundingBox().inflate(40), m -> m instanceof net.minecraft.world.entity.monster.Enemy && m.isAlive());
        for (net.minecraft.world.entity.Mob m : foes) m.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 120, 0, false, false));
        ServerPlayer p = ownerPlayer();
        if (p != null && foes.size() >= 6 && tickCount % 600 == 0)
            p.sendSystemMessage(Component.literal(getName().getString() + " (Scout): " + foes.size() + " hostiles nearby!").withStyle(ChatFormatting.GOLD));
    }

    private void guard(ServerLevel sl) {
        ServerPlayer p = ownerPlayer();
        if (p == null || distanceTo(p) > 16) return;
        for (net.minecraft.world.entity.Mob m : sl.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, getBoundingBox().inflate(10), m -> m.getTarget() == p && !(m instanceof HumanEntity h && h.isOwnedBy(p))))
            m.setTarget(this);
    }

    /** Sends this companion on a supply run with {@code group} people for {@code minutes}. */
    public void sendAway(int minutes, int group) {
        if (!(level() instanceof ServerLevel sl)) return;
        awayMinutes = minutes;
        awayGroup = Math.max(1, group);
        awayUntil = sl.getGameTime() + minutes * 1200L;
        setInvisible(true);
        setInvulnerable(true);
        setSilent(true);
        setNoAi(true);
        noPhysics = true;
        setTarget(null);
        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 20, 0.3, 0.6, 0.3, 0.02);
    }

    private void tickAway(ServerLevel sl) {
        ServerPlayer p = ownerPlayer();
        if (p != null && p.level() == sl) setPos(p.getX(), p.getY() + 0.1, p.getZ());   // stays with you, unseen
        if (sl.getGameTime() < awayUntil) return;
        awayUntil = 0;
        setInvisible(false);
        setInvulnerable(false);
        setSilent(false);
        setNoAi(false);
        noPhysics = false;
        setOrder(Order.FOLLOW);
        // danger: the longer the run and the lower the level, the riskier
        float risk = Math.max(0.01f, awayMinutes * 0.02f - lvl() / 400f - (awayGroup - 1) * 0.02f);
        if (random.nextFloat() < risk * 0.25f) {
            if (p != null) p.sendSystemMessage(Component.literal(getName().getString() + " never came back from the supply run...").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            discard();
            return;
        }
        if (random.nextFloat() < risk) setHealth(Math.max(1, getHealth() * 0.4f));
        int rolls = (int) Math.ceil(awayMinutes * (1 + lvl() / 150.0) * (job == Job.LOOTER ? 2 : 1) * (1 + (awayGroup - 1) * 0.15));
        String[][] loot = {{"minecraft:bread", "4"}, {"minecraft:cooked_beef", "3"}, {"minecraft:arrow", "16"}, {"kiyamet:bandage", "2"}, {"minecraft:iron_ingot", "3"},
                {"minecraft:coal", "6"}, {"kiyamet:metal_plate", "2"}, {"kiyamet:circuit", "1"}, {"kiyamet:fuel_canister", "1"}, {"minecraft:leather", "3"},
                {"kiyamet:medkit", "1"}, {"kiyamet:dollar_10", "2"}, {"minecraft:gold_ingot", "2"}, {"kiyamet:energy_cell", "1"}, {"minecraft:diamond", "1"}, {"kiyamet:vaccine", "1"}};
        int found = 0;
        for (int i = 0; i < rolls; i++) {
            int max = lvl() > 150 ? loot.length : lvl() > 60 ? loot.length - 2 : loot.length - 5;
            String[] e = loot[random.nextInt(max)];
            ItemStack rest = inventory.addItem(new ItemStack(Ui.item(e[0]), Integer.parseInt(e[1])));
            if (rest.isEmpty()) found++;
        }
        xp += awayMinutes * 15;
        onKill(sl, null);
        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1, getZ(), 15, 0.4, 0.6, 0.4, 0);
        if (p != null) p.sendSystemMessage(Component.literal(getName().getString() + " is back from the supply run with " + found + " finds" +
                (getHealth() < getMaxHealth() * 0.5f ? " (badly hurt)" : "") + ". Check their inventory!").withStyle(ChatFormatting.GREEN));
    }

    private void nurse(ServerLevel sl) {
        float amount = 2 + lvl() / 40f;
        boolean healed = false;
        ServerPlayer p = ownerPlayer();
        if (p != null && p.distanceTo(this) < 10 && p.getHealth() < p.getMaxHealth()) {
            p.heal(amount); p.removeEffect(net.minecraft.world.effect.MobEffects.POISON); healed = true;
            if (lvl() >= 120 && tickCount % 6000 == 0 && com.kiyamet.world.Infection.get(p) > 0) {
                com.kiyamet.world.Infection.cure(p);
                p.sendSystemMessage(Component.literal(getName().getString() + " treated your infection.").withStyle(ChatFormatting.GREEN));
            }
        }
        for (HumanEntity h : sl.getEntitiesOfClass(HumanEntity.class, getBoundingBox().inflate(8), h -> h.getHealth() < h.getMaxHealth()
                && (owner != null ? owner.equals(h.owner) : !community.isEmpty() && community.equals(h.community)))) { h.heal(amount); healed = true; }
        if (healed) sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 2.1, getZ(), 3, 0.4, 0.2, 0.4, 0);
    }

    private void engineer(ServerLevel sl) {
        float f = lvl() >= 150 ? 0.4f : 0.2f;
        int fixed = 0;
        for (BlockPos p : com.kiyamet.defense.BlockDurability.damagedNear(sl, blockPosition(), 10)) {
            if (fixed >= 6) break;
            if (com.kiyamet.defense.BlockDurability.repair(sl, p, f)) {
                fixed++;
                sl.sendParticles(ParticleTypes.WAX_OFF, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 5, 0.3, 0.3, 0.3, 0);
            }
        }
        for (net.minecraft.world.entity.LivingEntity e : sl.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, getBoundingBox().inflate(10),
                e -> (e instanceof com.kiyamet.robot.RobotEntity r && r.isAlly() || e instanceof com.kiyamet.ally.TurretDrone) && e.getHealth() < e.getMaxHealth())) {
            e.heal(e.getMaxHealth() * f * 0.5f);
            sl.sendParticles(ParticleTypes.WAX_OFF, e.getX(), e.getY() + 1, e.getZ(), 8, 0.4, 0.4, 0.4, 0);
            fixed++;
        }
        if (fixed > 0) { swing(net.minecraft.world.InteractionHand.MAIN_HAND); sl.playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.ANVIL_USE, net.minecraft.sounds.SoundSource.NEUTRAL, 0.3f, 1.4f); }
    }

    private void scavenge() {
        String[][] finds = {{"minecraft:bread", "3"}, {"minecraft:arrow", "12"}, {"kiyamet:bandage", "2"}, {"minecraft:iron_ingot", "2"}, {"minecraft:cooked_chicken", "3"},
                {"kiyamet:metal_plate", "1"}, {"minecraft:string", "4"}, {"kiyamet:fuel_canister", "1"}, {"kiyamet:circuit", "1"}, {"kiyamet:walker_guts", "2"},
                {"kiyamet:medkit", "1"}, {"kiyamet:dollar_10", "1"}, {"minecraft:leather", "3"}, {"kiyamet:energy_cell", "1"}};
        int pick = random.nextInt(lvl() >= 100 ? finds.length : finds.length - 3);
        ItemStack found = new ItemStack(Ui.item(finds[pick][0]), Integer.parseInt(finds[pick][1]) + lvl() / 100);
        ItemStack rest = inventory.addItem(found);
        ServerPlayer p = ownerPlayer();
        if (p != null) p.sendSystemMessage(Component.literal(getName().getString() + " found " + found.getCount() + "× " + found.getHoverName().getString()
                + (rest.isEmpty() ? "" : " (her bag is full!)")).withStyle(ChatFormatting.GOLD));
    }

    private void selfHeal() {
        for (String id : new String[]{"kiyamet:medkit", "kiyamet:bandage", "kiyamet:healing_syringe"}) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack s = inventory.getItem(i);
                if (s.is(Ui.item(id))) {
                    s.shrink(1);
                    heal(id.endsWith("medkit") ? getMaxHealth() * 0.6f : id.endsWith("bandage") ? getMaxHealth() * 0.2f : getMaxHealth() * 0.4f);
                    playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1f, 1.3f);
                    return;
                }
            }
        }
    }

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
        if (awayUntil > 0) return false;
        Entity attacker = source.getEntity();
        if (attacker instanceof Player p && isOwnedBy(p)) return false;                               // no friendly fire
        if (attacker instanceof HumanEntity h && h.owner != null && owner != null && h.owner.equals(owner)) return false;
        if (attacker instanceof ServerPlayer p && faction() == Faction.COMMUNITY) Communities.onMemberHurt(level, this, p);
        return super.hurtServer(level, source, amount * com.kiyamet.twd.Headshot.multiplier(level, this, source));
    }

    /** Companions earn XP from kills and level up to 300. */
    public void onKill(ServerLevel level, @Nullable LivingEntity victim) {
        if (faction() != Faction.COMPANION) return;
        int gain = victim == null ? 0 : victim instanceof BossEntity ? 400 : victim instanceof HumanEntity h ? 20 + h.lvl() / 3 : victim instanceof RobotEntity ? 25 : 10;
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
            if (rubies > 0) sl.addFreshEntity(new ItemEntity(sl, getX(), getY() + 0.5, getZ(), com.kiyamet.money.Money.stack(rubies * com.kiyamet.money.Money.RATE)));
            if (random.nextFloat() < 0.4f) sl.addFreshEntity(new ItemEntity(sl, getX(), getY() + 0.5, getZ(), new ItemStack(Items.ARROW, 4 + random.nextInt(12))));
            if (random.nextFloat() < 0.2f) sl.addFreshEntity(new ItemEntity(sl, getX(), getY() + 0.5, getZ(), new ItemStack(Ui.item("kiyamet:bandage"))));
        } else {
            ServerPlayer p = ownerPlayer();
            if (p != null) p.sendSystemMessage(Component.literal(getName().getString() + " has died.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        }
        if (bar != null) bar.removeAllPlayers();
        Radio.forget(this);
        net.minecraft.world.Containers.dropContents(sl, this, inventory);
        // Everyone is infected: the dead come back.
        if (faction() != Faction.WHISPERER && random.nextFloat() < 0.8f) {
            com.kiyamet.entity.ZombieKind zk = appearance().female() ? com.kiyamet.entity.ZombieKind.WOMAN : profession() == Profession.DOCTOR ? com.kiyamet.entity.ZombieKind.DOCTOR
                    : profession() == Profession.POLICE ? com.kiyamet.entity.ZombieKind.POLICE : profession() == Profession.FARMER ? com.kiyamet.entity.ZombieKind.FARMER : com.kiyamet.entity.ZombieKind.RUNNER;
            KZombie z = com.kiyamet.entity.ModEntities.ZOMBIES.get(zk).create(sl, net.minecraft.world.entity.EntitySpawnReason.CONVERSION);
            if (z != null) {
                z.setPos(getX(), getY(), getZ());
                // the dead keep their name: you can see who they were
                z.setCustomName(Component.literal(getName().getString()).withStyle(ChatFormatting.DARK_GREEN));
                z.setCustomNameVisible(true);
                z.setPersistenceRequired();
                z.formerName = getName().getString();
                if (faction() == Faction.COMPANION && ownerPlayer() != null) z.formerOwner = ownerPlayer().getUUID();
                sl.getServer().execute(() -> { if (sl.getEntity(z.getUUID()) == null) sl.addFreshEntity(z); });
            }
        }
    }

    // ---------------------------------------------------------------- interaction

    @Override
    public boolean canBeLeashed() { return Captives.canCapture(this); }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.NAME_TAG)) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp) || !(level() instanceof ServerLevel)) return InteractionResult.SUCCESS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (captives.captive && !held.is(Items.LEAD)) { Captives.talk(sp, this); return InteractionResult.SUCCESS; }
        if (faction() == Faction.COMPANION && isOwnedBy(sp) && held.has(net.minecraft.core.component.DataComponents.FOOD) && !sp.isShiftKeyDown()) {
            held.shrink(1);
            heal(4);
            morale = Math.min(100, morale + 15);
            Talk.say(sp, this, Talk.pick(random, "Thanks. I needed that.", "Real food... you're the best.", "*eats* Okay. I can keep going.", "You didn't have to share. Thank you."));
            return InteractionResult.SUCCESS;
        }
        if (getTarget() == player) return InteractionResult.FAIL;
        if (envoy) { WorldEvents.envoyTalk(sp, this); return InteractionResult.SUCCESS; }
        if (com.kiyamet.twd.Story.interact(sp, this)) return InteractionResult.SUCCESS;
        switch (faction()) {
            case COMPANION -> {
                if (isOwnedBy(sp)) CompanionMenu.open(sp, this);
                else sp.displayClientMessage(Component.literal("This companion follows someone else."), true);
            }
            case SURVIVOR -> Dialogues.survivor(sp, this);
            case COMMUNITY -> {
                if (Communities.reputation(sp, community) < 0) { sp.displayClientMessage(Component.literal("They don't trust you.").withStyle(ChatFormatting.RED), true); break; }
                switch (role()) {
                    case TRADER -> Dialogues.trader(sp, this);
                    case LEADER -> Dialogues.leader(sp, this);
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
        if (hasProfession() && faction() != Faction.RAIDER && faction() != Faction.WHISPERER && role() == Role.MEMBER) return Component.literal(profession().title);
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
    public boolean shouldShowName() { return faction() == Faction.COMPANION || specialty() != Specialty.NONE || role() == Role.LEADER || role() == Role.COMMANDER || role() == Role.TRADER || super.shouldShowName(); }

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
