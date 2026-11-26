package com.kiyamet.npc;

import com.kiyamet.Kiyamet;
import com.kiyamet.entity.KZombie;
import com.kiyamet.entity.ModEntities;
import com.kiyamet.entity.ZombieKind;
import com.kiyamet.registry.ModItems;
import com.kiyamet.skill.Skill;
import com.kiyamet.skill.SkillManager;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

/**
 * Blood Moons and raids on your base (the last bed you slept in).
 * Raids come every 1-2 game weeks: the risk grows with every day since the last raid,
 * jumps on a Blood Moon night and again 2-3 days after one. Raiders bring walkers with them
 * (and the Masked at high power, the Commander at very high power), attack in waves, smash walls
 * and steal from your chests if they are still there at dawn.
 */
public final class BaseRaids {
    public static final AttachmentType<BlockPos> BASE = AttachmentRegistry.create(Kiyamet.id("raid_base"),
            b -> b.persistent(BlockPos.CODEC).copyOnDeath());
    public static final AttachmentType<Integer> LAST_RAID = AttachmentRegistry.create(Kiyamet.id("raid_last"),
            b -> b.initializer(() -> -1).persistent(Codec.INT).copyOnDeath());
    public static final AttachmentType<Integer> LAST_BLOOD = AttachmentRegistry.create(Kiyamet.id("raid_last_blood_moon"),
            b -> b.initializer(() -> -100).persistent(Codec.INT).copyOnDeath());

    static boolean bloodMoon;
    private static long lastDuskDay = -1;
    /** Player -> server tick when tonight's raid starts. */
    private static final Map<UUID, Long> PENDING = new HashMap<>();
    private static final Map<UUID, Raid> ACTIVE = new HashMap<>();

    public static boolean bloodMoon() { return bloodMoon; }

    /** Refused tribute: they come tonight. */
    public static void scheduleTonight(ServerPlayer p) {
        if (ACTIVE.containsKey(p.getUUID())) return;
        PENDING.put(p.getUUID(), (long) p.level().getServer().getTickCount() + 600);
    }

    /** Paid tribute: the raid clock starts over with a few days of grace. */
    public static void paid(ServerPlayer p) {
        PENDING.remove(p.getUUID());
        p.setAttached(LAST_RAID, day(p) + 3);
    }

    public static boolean busy(ServerPlayer p) { return PENDING.containsKey(p.getUUID()) || ACTIVE.containsKey(p.getUUID()); }

    public static void init() {
        EntitySleepEvents.START_SLEEPING.register((e, pos) -> {
            if (!(e instanceof ServerPlayer p) || p.level().dimension() != Level.OVERWORLD) return;
            if (!p.level().getEntitiesOfClass(com.kiyamet.twd.ShelterCore.class, p.getBoundingBox().inflate(512), c -> p.getUUID().equals(c.owner())).isEmpty()) return;
            BlockPos old = p.getAttached(BASE);
            p.setAttached(BASE, pos.immutable());
            if (old == null || old.distSqr(pos) > 48 * 48)
                p.sendSystemMessage(Component.literal("This is your base now. Defend it - raiders will come for it.").withStyle(ChatFormatting.GOLD));
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            ServerLevel ow = server.overworld();
            long time = ow.getDayTime(), day = time / 24000, tod = time % 24000;
            if (tod >= 12500 && tod < 13000 && lastDuskDay != day) { lastDuskDay = day; dusk(server, ow, (int) day); }
            if (bloodMoon && (tod < 12000 || tod >= 23200)) endBloodMoon(server);
            if (bloodMoon && server.getTickCount() % 600 == 0) bloodMoonTick(ow);

            long now = server.getTickCount();
            for (Iterator<Map.Entry<UUID, Long>> it = PENDING.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<UUID, Long> en = it.next();
                if (now < en.getValue()) continue;
                ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
                if (p == null) continue;                             // waits until they log back in
                if (tod < 12500 || tod > 22000) continue;            // raids only start at night
                BlockPos base = p.getAttached(BASE);
                if (p.level() != ow) continue;
                if (base != null && p.blockPosition().distSqr(base) > 120 * 120) {
                    if (now - en.getValue() < 40) p.sendSystemMessage(Component.literal("Your radio crackles: raiders are circling your base, waiting for you to come home...").withStyle(ChatFormatting.RED));
                    continue;                                        // they wait for you to come back
                }
                it.remove();
                start(p, ow, base != null ? base : p.blockPosition(), false);
            }
            ACTIVE.values().removeIf(r -> r.tick(server, ow, tod));
        });

        CommandRegistrationCallback.EVENT.register((d, ctx, env) -> d.register(Commands.literal("twd")
                .then(Commands.literal("raid").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (!p.isCreative()) return info(p);
                    BlockPos base = p.getAttached(BASE);
                    start(p, p.level(), base != null ? base : p.blockPosition(), true);
                    return 1;
                }))
                .then(Commands.literal("raidinfo").executes(c -> info(c.getSource().getPlayerOrException())))
                .then(Commands.literal("bloodmoon").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (!p.isCreative()) { p.sendSystemMessage(Component.literal("Creative mode only.")); return 0; }
                    if (bloodMoon) endBloodMoon(c.getSource().getServer()); else startBloodMoon(c.getSource().getServer(), (int) (p.level().getDayTime() / 24000));
                    return 1;
                }))));
    }

    // ---------------------------------------------------------------- schedule

    private static int day(ServerPlayer p) { return (int) (p.level().getServer().overworld().getDayTime() / 24000); }

    /** Tonight's raid chance for this player (0..1). */
    public static float risk(ServerPlayer p, int day, boolean blood) {
        int last = p.getAttachedOrElse(LAST_RAID, -1);
        if (last < 0) { last = Math.max(0, day - 2); p.setAttached(LAST_RAID, last); }  // first week is quiet
        int since = day - last;
        if (day < 6 || since < 4) return 0;
        float c = (since - 4) * 0.08f;
        if (blood) c += 0.25f;
        int afterBlood = day - p.getAttachedOrElse(LAST_BLOOD, -100);
        if (afterBlood >= 2 && afterBlood <= 3) c += 0.3f;
        if (since >= 14) c = 1;
        return Mth.clamp(c, 0, 1);
    }

    public static String riskLabel(ServerPlayer p) {
        if (ACTIVE.containsKey(p.getUUID())) return "UNDER ATTACK";
        if (PENDING.containsKey(p.getUUID())) return "Tonight!";
        float r = risk(p, day(p), false);
        return r <= 0 ? "Low" : r < 0.25f ? "Medium" : r < 0.5f ? "High" : "Very high";
    }

    private static int info(ServerPlayer p) {
        int d = day(p), since = d - p.getAttachedOrElse(LAST_RAID, d);
        BlockPos base = p.getAttached(BASE);
        p.sendSystemMessage(Component.literal("Base: " + (base == null ? "none (sleep in a bed to set it)" : base.toShortString())
                + "  |  Days since last raid: " + since + "  |  Raid risk: " + riskLabel(p)
                + (bloodMoon ? "  |  BLOOD MOON" : "")).withStyle(ChatFormatting.GOLD));
        return 1;
    }

    private static void dusk(MinecraftServer server, ServerLevel ow, int day) {
        boolean full = ow.getMoonBrightness(BlockPos.ZERO) >= 0.99f;
        if (day >= 4 && ow.random.nextFloat() < (full ? 0.4f : 0.06f)) startBloodMoon(server, day);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator() || PENDING.containsKey(p.getUUID()) || ACTIVE.containsKey(p.getUUID())) continue;
            if (ow.random.nextFloat() >= risk(p, day, bloodMoon)) continue;
            PENDING.put(p.getUUID(), (long) server.getTickCount() + 1200 + ow.random.nextInt(4800));
            p.sendSystemMessage(Component.literal("Smoke on the horizon. Engines in the distance... Raiders are coming for your base tonight.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            ow.playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.6f, 0.7f);
            for (HumanEntity h : ow.getEntitiesOfClass(HumanEntity.class, p.getBoundingBox().inflate(48), h -> h.isOwnedBy(p) && h.job() == Job.SCOUT)) {
                p.sendSystemMessage(Component.literal("[" + h.getName().getString() + "] I saw them on the road. A big group, with walkers following. Get ready.").withStyle(ChatFormatting.AQUA));
                break;
            }
        }
    }

    private static void startBloodMoon(MinecraftServer server, int day) {
        bloodMoon = true;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.setAttached(LAST_BLOOD, day);
            title(p, Component.literal("BLOOD MOON").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    Component.literal("The dead are restless tonight").withStyle(ChatFormatting.RED));
            p.level().playSound(null, p.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.5f, 0.5f);
            p.sendSystemMessage(Component.literal("A Blood Moon rises. Walkers are faster, stronger and there are many more of them. Raiders like nights like this.").withStyle(ChatFormatting.DARK_RED));
        }
    }

    private static void endBloodMoon(MinecraftServer server) {
        bloodMoon = false;
        for (ServerPlayer p : server.getPlayerList().getPlayers())
            p.sendSystemMessage(Component.literal("The Blood Moon sets. In 2-3 days raiders may use the chaos to strike.").withStyle(ChatFormatting.GRAY));
    }

    private static final ZombieKind[] WALKERS = {ZombieKind.FARMER, ZombieKind.FARMER, ZombieKind.WOMAN, ZombieKind.NURSE, ZombieKind.DOCTOR,
            ZombieKind.POLICE, ZombieKind.RUNNER, ZombieKind.CRAWLER, ZombieKind.TOXIC, ZombieKind.ARMORED};

    /** Every 30 s of a Blood Moon: more walkers near every player, and all of them get stronger. */
    private static void bloodMoonTick(ServerLevel ow) {
        for (ServerPlayer p : ow.players()) {
            if (p.isSpectator() || p.isCreative()) continue;
            List<KZombie> near = ow.getEntitiesOfClass(KZombie.class, p.getBoundingBox().inflate(48));
            for (KZombie z : near) buff(z);
            if (near.size() > 30) continue;
            BlockPos at = ModHumans.surface(ow, p.blockPosition(), 24, 40);
            if (at == null) continue;
            int n = 3 + ow.random.nextInt(3);
            for (int i = 0; i < n; i++) {
                KZombie z = walker(ow, at, WALKERS[ow.random.nextInt(WALKERS.length)]);
                if (z != null) { buff(z); z.setTarget(p); }
            }
        }
    }

    private static void buff(Mob z) {
        z.addEffect(new MobEffectInstance(MobEffects.SPEED, 700, 0, true, false));
        z.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 700, 0, true, false));
    }

    static KZombie walker(ServerLevel level, BlockPos at, ZombieKind kind) {
        KZombie z = ModEntities.ZOMBIES.get(kind).create(level, EntitySpawnReason.EVENT);
        if (z == null) return null;
        z.setPos(at.getX() + level.random.nextInt(7) - 3 + 0.5, at.getY(), at.getZ() + level.random.nextInt(7) - 3 + 0.5);
        z.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.EVENT, null);
        z.setPersistenceRequired();
        level.addFreshEntity(z);
        return z;
    }

    private static void title(ServerPlayer p, Component t, Component sub) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
        p.connection.send(new ClientboundSetTitleTextPacket(t));
    }

    // ---------------------------------------------------------------- the raid

    private static void start(ServerPlayer p, ServerLevel level, BlockPos base, boolean test) {
        if (ACTIVE.containsKey(p.getUUID())) { p.sendSystemMessage(Component.literal("A raid is already on.")); return; }
        PENDING.remove(p.getUUID());
        Raid r = new Raid(p, level, base, ModHumans.power(p));
        ACTIVE.put(p.getUUID(), r);
        r.spawnWave(level);
    }

    private static final String[] DIRS = {"east", "south-east", "south", "south-west", "west", "north-west", "north", "north-east"};

    private static final class Raid {
        final UUID player;
        final BlockPos base;
        final int power, waves;
        final boolean blood;
        final ServerBossEvent bar;
        final Set<UUID> mobs = new HashSet<>();
        int wave, waveSize, pause, lifetime;

        Raid(ServerPlayer p, ServerLevel level, BlockPos base, int power) {
            this.player = p.getUUID();
            this.base = base;
            this.power = power;
            this.blood = bloodMoon;
            this.waves = (power < 800 ? 2 : power < 2500 ? 3 : 4) + (bloodMoon ? 1 : 0);
            this.bar = new ServerBossEvent(Component.literal("Base Raid"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
            title(p, Component.literal("RAID!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    Component.literal(waves + " waves of raiders and walkers").withStyle(ChatFormatting.GOLD));
        }

        void spawnWave(ServerLevel level) {
            BlockPos at = null;
            double ang = 0;
            for (int t = 0; t < 10 && at == null; t++) {
                BlockPos c = ModHumans.surface(level, base, 38, 52);
                if (c != null) { at = c; ang = Math.atan2(c.getZ() - base.getZ(), c.getX() - base.getX()); }
            }
            if (at == null) at = base.offset(40, 0, 0);
            String dir = DIRS[Math.floorMod((int) Math.round(ang / (Math.PI / 4)), 8)];
            boolean last = wave == waves - 1;
            int lvBase = Mth.clamp(5 + power / 25 + wave * 6, 5, 240);

            int raiders = Mth.clamp(3 + wave * 2 + power / 600, 3, 12);
            List<HumanEntity> crew = new ArrayList<>();
            boolean cmd = last && power >= 4000;
            for (int i = 0; i < raiders; i++) {
                Role role = i == 0 ? (cmd ? Role.COMMANDER : Role.LEADER) : Role.MEMBER;
                int lv = role == Role.COMMANDER ? 260 + level.random.nextInt(41) : Mth.clamp(lvBase + level.random.nextInt(21) - 10 + (role == Role.LEADER ? 15 : 0), 1, 250);
                HumanEntity h = ModHumans.spawn(level, at.offset(level.random.nextInt(7) - 3, 0, level.random.nextInt(7) - 3), Faction.RAIDER, role, lv, null);
                if (h != null) { crew.add(h); add(h); }
            }
            if (cmd || power >= 800 && level.random.nextFloat() < 0.6f) com.kiyamet.vehicle.ModVehicles.raiderRide(level, at, crew, cmd, power);
            if (power >= 1500 && wave >= 1) {
                int masked = 2 + level.random.nextInt(2);
                for (int i = 0; i < masked; i++)
                    add(ModHumans.spawn(level, at.offset(level.random.nextInt(9) - 4, 0, level.random.nextInt(9) - 4), Faction.WHISPERER, Role.MEMBER, Mth.clamp(lvBase + 10, 20, 240), null));
            }
            int walkers = Mth.clamp(4 + wave * 3 + power / 500, 4, 18);
            if (blood) walkers = walkers * 3 / 2;
            for (int i = 0; i < walkers; i++) {
                ZombieKind k = power >= 2000 && level.random.nextFloat() < 0.12f ? (level.random.nextBoolean() ? ZombieKind.TANK : ZombieKind.EXPLODER)
                        : WALKERS[level.random.nextInt(WALKERS.length)];
                KZombie z = walker(level, at, k);
                if (z != null) { if (blood) buff(z); add(z); }
            }
            waveSize = mobs.size();
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(player);
            if (p != null) {
                p.sendSystemMessage(Component.literal("Wave " + (wave + 1) + "/" + waves + ": " + raiders + " raiders and " + walkers + " walkers from the " + dir + "!")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                level.playSound(null, base, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 4f, 1f);
            }
        }

        void add(Entity e) {
            if (e == null) return;
            if (e instanceof HumanEntity h) h.raiding = true;
            mobs.add(e.getUUID());
        }

        /** @return true when the raid is over. */
        boolean tick(MinecraftServer server, ServerLevel level, long tod) {
            lifetime++;
            ServerPlayer p = server.getPlayerList().getPlayer(player);
            List<Mob> alive = new ArrayList<>();
            for (Iterator<UUID> it = mobs.iterator(); it.hasNext(); ) {
                Entity e = level.getEntity(it.next());
                if (e instanceof Mob m && m.isAlive()) alive.add(m); else it.remove();
            }
            // show the bar to everyone around the base
            for (ServerPlayer sp : level.players()) {
                boolean near = sp.blockPosition().distSqr(base) < 160 * 160;
                if (near && !bar.getPlayers().contains(sp)) bar.addPlayer(sp);
                else if (!near && bar.getPlayers().contains(sp)) bar.removePlayer(sp);
            }
            bar.setName(Component.literal("Base Raid - Wave " + (wave + 1) + "/" + waves + "  (" + alive.size() + " left)"));
            bar.setProgress(waveSize == 0 ? 0 : Mth.clamp(alive.size() / (float) waveSize, 0, 1));

            // march on the base
            for (Mob m : alive) {
                if (m.getTarget() == null || !m.getTarget().isAlive()) {
                    if (m.blockPosition().distSqr(base) > 6 * 6) m.getNavigation().moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 1.0);
                    else if (p != null && !p.isCreative() && p.blockPosition().distSqr(base) < 40 * 40) m.setTarget(p);
                }
                if (m instanceof HumanEntity h && h.raiding) m.setPersistenceRequired();
            }

            if (alive.isEmpty()) {
                if (wave + 1 >= waves) { win(level, p); return true; }
                if (++pause >= 10) {
                    pause = 0;
                    wave++;
                    spawnWave(level);
                } else if (pause == 1 && p != null) p.sendSystemMessage(Component.literal("Wave cleared. More are coming...").withStyle(ChatFormatting.GOLD));
                return false;
            }
            // daylight (or 15 minutes): the raiders grab what they can and run
            if ((tod > 23500 || tod < 11000) && lifetime > 300 || lifetime > 900) { lose(level, p, alive); return true; }
            return false;
        }

        void finish(ServerLevel level, ServerPlayer p) {
            bar.removeAllPlayers();
            if (p != null) p.setAttached(LAST_RAID, day(p));
        }

        void win(ServerLevel level, ServerPlayer p) {
            finish(level, p);
            if (p == null) return;
            title(p, Component.literal("RAID DEFEATED").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                    Component.literal("Your base still stands").withStyle(ChatFormatting.GOLD));
            level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
            List<ItemStack> loot = new ArrayList<>();
            loot.addAll(com.kiyamet.money.Money.bills((5 + waves * 3 + power / 300) * com.kiyamet.money.Money.RATE));
            loot.add(new ItemStack(ModItems.get("medkit"), 1 + waves / 2));
            loot.add(new ItemStack(Items.ARROW, 32 + waves * 8));
            loot.add(new ItemStack(Items.COOKED_BEEF, 8));
            if (power >= 800) loot.add(new ItemStack(ModItems.get("circuit"), 2 + waves));
            if (power >= 2500) loot.add(new ItemStack(ModItems.get("tech_core"), 1));
            if (blood) loot.add(new ItemStack(ModItems.get("vaccine"), 1));
            for (ItemStack s : loot) level.addFreshEntity(new ItemEntity(level, base.getX() + 0.5, base.getY() + 1.2, base.getZ() + 0.5, s));
            for (Skill s : new Skill[]{Skill.SWORDS, Skill.ARCHERY, Skill.DEFENSE}) SkillManager.addXp(p, s, 150 + waves * 60);
            p.sendSystemMessage(Component.literal("The raiders are dead. Their supplies are at your base. (+XP Swords, Firearms, Defense)").withStyle(ChatFormatting.GREEN));
        }

        void lose(ServerLevel level, ServerPlayer p, List<Mob> alive) {
            finish(level, p);
            int stolen = 0;
            BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
            outer:
            for (int dx = -10; dx <= 10; dx++) for (int dy = -4; dy <= 4; dy++) for (int dz = -10; dz <= 10; dz++) {
                BlockEntity be = level.getBlockEntity(mp.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz));
                if (!(be instanceof Container c)) continue;
                for (int i = 0; i < c.getContainerSize() && stolen < 4; i++) {
                    if (!c.getItem(i).isEmpty() && level.random.nextFloat() < 0.25f) { c.setItem(i, ItemStack.EMPTY); stolen++; }
                }
                if (stolen >= 4) break outer;
            }
            for (Mob m : alive) if (m instanceof HumanEntity) m.discard(); else m.setPersistenceRequired();
            if (p != null) p.sendSystemMessage(Component.literal("The raiders fled with your supplies" + (stolen > 0 ? " (" + stolen + " stacks stolen from your chests)" : "") + ". The walkers they brought are still out there.")
                    .withStyle(ChatFormatting.DARK_RED));
        }
    }

    private BaseRaids() {}
}
