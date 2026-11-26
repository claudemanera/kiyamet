package com.kiyamet.twd;

import com.kiyamet.Kiyamet;
import com.kiyamet.entity.KZombie;
import com.kiyamet.entity.ZombieKind;
import com.kiyamet.npc.*;
import com.kiyamet.registry.ModItems;
import com.kiyamet.skill.Skill;
import com.kiyamet.skill.SkillManager;
import com.kiyamet.vehicle.ModVehicles;
import com.kiyamet.vehicle.VehicleEntity;
import com.kiyamet.vehicle.VehicleKind;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.*;

/**
 * "The Long Road": an 8-chapter story told at the mission boards, GTA-style. Marcus, who runs the settlement,
 * sends you out: medicine runs, a missing scout, a raider camp, a truck, a siege, a herd, a heist, and the
 * Commander himself. Objectives show on the action bar with an arrow and distance; dying fails the mission
 * (just start it again from any board).
 */
public final class Story {
    /** "chapter;stage;x;y;z;bx;by;bz;targetUuid;count;deadline" */
    public static final AttachmentType<String> STATE = AttachmentRegistry.create(Kiyamet.id("story"),
            b -> b.initializer(() -> "1;0").persistent(Codec.STRING).copyOnDeath());

    static final String MARCUS = "Marcus", ELENA = "Elena", DANI = "Dani", RADIO = "Radio";
    static final ChatFormatting MC = ChatFormatting.GREEN, EC = ChatFormatting.LIGHT_PURPLE, DC = ChatFormatting.AQUA, RC = ChatFormatting.RED;

    public static final StoryChapters.Chapter[] CHAPTERS = StoryChapters.ALL;
    public static int total() { return CHAPTERS.length; }
    public static int reward(int ch) { return 15 + ch * 4 + (ch % 10 == 0 ? 40 : 0); }

    // stages: 0 = not started, 1 = go to target, 2 = do the thing there, 3 = return to the board, 4+ = special
    private static final Map<UUID, Set<UUID>> TRACKED = new HashMap<>();
    private static final Map<UUID, Integer> KILLS = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 10 != 0) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) tick(p);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((e, src) -> {
            if (e instanceof ServerPlayer p && stage(p) > 0) fail(p, "You died");
            for (Map.Entry<UUID, Set<UUID>> en : TRACKED.entrySet()) if (en.getValue().contains(e.getUUID())) {
                KILLS.merge(en.getKey(), 1, Integer::sum);
                ServerPlayer p = ((ServerLevel) e.level()).getServer().getPlayerList().getPlayer(en.getKey());
                if (p == null) continue;
                String[] st = state(p);
                if (e.getUUID().toString().equals(st[8])) onTargetDeath(p, (LivingEntity) e);
            }
        });
    }

    // ------------------------------------------------------------------ state

    static String[] state(ServerPlayer p) {
        String[] s = p.getAttachedOrElse(STATE, "1;0").split(";");
        String[] out = new String[11];
        Arrays.fill(out, "0");
        out[8] = "";
        System.arraycopy(s, 0, out, 0, Math.min(s.length, 11));
        return out;
    }
    static void save(ServerPlayer p, String[] s) { p.setAttached(STATE, String.join(";", s)); }
    public static int chapter(ServerPlayer p) { return Integer.parseInt(state(p)[0]); }
    public static int stage(ServerPlayer p) { return Integer.parseInt(state(p)[1]); }
    static BlockPos target(String[] s) { return new BlockPos(Integer.parseInt(s[2]), Integer.parseInt(s[3]), Integer.parseInt(s[4])); }
    static BlockPos board(String[] s) { return new BlockPos(Integer.parseInt(s[5]), Integer.parseInt(s[6]), Integer.parseInt(s[7])); }
    static void setStage(ServerPlayer p, int stage) { String[] s = state(p); s[1] = String.valueOf(stage); save(p, s); }

    // ------------------------------------------------------------------ starting

    /** Called from a mission board. */
    public static void offer(ServerPlayer p, MissionBoard board) {
        int ch = chapter(p);
        if (ch > CHAPTERS.length) { Talk.sayAs(p, MARCUS, MC, "You've done more than anyone could ask. The road is yours now."); return; }
        if (stage(p) > 0) { Talk.sayAs(p, MARCUS, MC, "You're already on a job. Finish it first."); return; }
        StoryChapters.Chapter c = CHAPTERS[ch - 1];
        int d = 0;
        for (String line : c.intro()) { speak(p, d, line); d += Math.min(60, 20 + line.length() / 2); }
        Talk.askAs(p, board, MARCUS, MC, d, "", null,
                new Talk.Option(c.accept(), pl -> start(pl, board, ch)),
                new Talk.Option("Not now.", pl -> Talk.sayAs(pl, MARCUS, MC, "Don't take too long.")));
    }

    static void speak(ServerPlayer p, int delay, String line) {
        String[] kv = line.contains("|") ? line.split("\\|", 2) : new String[]{MARCUS, line};
        ChatFormatting col = kv[0].equals(ELENA) ? EC : kv[0].equals(DANI) ? DC : kv[0].equals(MARCUS) ? MC : ChatFormatting.YELLOW;
        Talk.sayAsLater(p, delay, kv[0], col, kv[1]);
    }

    static StoryChapters.Chapter ch(ServerPlayer p) { return CHAPTERS[Math.min(CHAPTERS.length, chapter(p)) - 1]; }

    static void start(ServerPlayer p, MissionBoard board, int ch) {
        ServerLevel level = (ServerLevel) p.level();
        StoryChapters.Chapter c = CHAPTERS[ch - 1];
        BlockPos b = board.blockPosition();
        boolean defend = c.type() == StoryChapters.Type.DEFEND;
        int dist = defend ? 0 : Math.min(700, 110 + ch * 11);
        BlockPos t = b;
        if (!defend) {
            double a = level.random.nextDouble() * Math.PI * 2;
            BlockPos raw = b.offset((int) (Math.cos(a) * dist), 0, (int) (Math.sin(a) * dist));
            t = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, raw);
        }
        String[] s = {String.valueOf(ch), defend ? "4" : "1", "" + t.getX(), "" + t.getY(), "" + t.getZ(), "" + b.getX(), "" + b.getY(), "" + b.getZ(), "", "0", "0"};
        save(p, s);
        TRACKED.put(p.getUUID(), new HashSet<>());
        KILLS.put(p.getUUID(), 0);
        title(p, Component.literal("CHAPTER " + ch + ": " + c.title().toUpperCase()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal(c.tagline()).withStyle(ChatFormatting.GRAY));
        level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.5f, 0.6f);
        if (defend) siegeWave(p, level, b, 0);
    }

    // ------------------------------------------------------------------ ticking

    private static void tick(ServerPlayer p) {
        String[] s = state(p);
        int chn = Integer.parseInt(s[0]), stage = Integer.parseInt(s[1]);
        if (stage == 0 || chn > CHAPTERS.length) return;
        StoryChapters.Chapter c = CHAPTERS[chn - 1];
        ServerLevel level = (ServerLevel) p.level();
        BlockPos t = target(s), b = board(s);
        Set<UUID> tracked = TRACKED.computeIfAbsent(p.getUUID(), k -> new HashSet<>());
        Entity tgt = s[8].isEmpty() ? null : level.getEntity(UUID.fromString(s[8]));
        switch (stage) {
            case 1 -> {
                hud(p, goal(c), t);
                if (p.blockPosition().distSqr(t) < 28 * 28) arrive(p, level, chn, t);
            }
            case 2 -> {
                switch (c.type()) {
                    case FETCH -> {
                        hud(p, "Take the " + c.item() + " from the crate", t);
                        if (hasQuestItem(p, c.item())) { setStage(p, 3); Talk.sayAs(p, RADIO, RC, pick(level, "Got it? Get back here, fast.", "Good. Now get out of there!", "That's it. Come home.")); }
                    }
                    case RESCUE -> {
                        if (tgt instanceof HumanEntity d && d.isAlive()) {
                            if (d.isOwnedBy(p)) { setStage(p, 3); Talk.say(p, d, "Let's go. I'll stay close."); }
                            else hud(p, "Reach " + name(c) + " and talk to them", d.blockPosition());
                        } else if (tgt == null && level.isLoaded(t) && p.blockPosition().distSqr(t) < 48 * 48 && !s[8].isEmpty()) fail(p, name(c) + " didn't make it");
                    }
                    case KILL -> hud(p, "Kill " + name(c), tgt != null ? tgt.blockPosition() : t);
                    case DRIVE -> {
                        if (tgt instanceof VehicleEntity v && v.isAlive()) {
                            hud(p, v.hasPassenger(p) ? "Drive it back to the settlement" : "Get in the vehicle", v.hasPassenger(p) ? b : v.blockPosition());
                            if (v.hasPassenger(p) && v.blockPosition().distSqr(b) < 18 * 18) complete(p, level);
                        } else if (tgt == null && level.isLoaded(t) && p.blockPosition().distSqr(t) < 64 * 64 && !s[8].isEmpty()) fail(p, "The vehicle was destroyed");
                    }
                    case HERD -> {
                        int k = KILLS.getOrDefault(p.getUUID(), 0);
                        long left = Long.parseLong(s[10]) - level.getGameTime();
                        p.displayClientMessage(Component.literal("▶ Thin the herd: " + k + " / " + c.count() + "   ⏱ " + Math.max(0, left / 20) + "s").withStyle(ChatFormatting.GOLD), true);
                        if (k >= c.count()) complete(p, level);
                        else if (left <= 0) fail(p, "The herd got through");
                    }
                    default -> {}
                }
            }
            case 3 -> {
                if (c.type() == StoryChapters.Type.RESCUE && tgt != null && !tgt.isAlive()) { fail(p, name(c) + " didn't make it"); return; }
                hud(p, c.type() == StoryChapters.Type.RESCUE ? "Bring " + name(c) + " home" : !c.item().isEmpty() ? "Bring the " + c.item() + " to Marcus" : "Return to Marcus", b);
                if (p.blockPosition().distSqr(b) < 10 * 10) {
                    if (c.type() == StoryChapters.Type.RESCUE && tgt instanceof HumanEntity d && d.distanceTo(p) > 16) {
                        p.displayClientMessage(Component.literal("Wait for " + name(c) + "!").withStyle(ChatFormatting.YELLOW), true);
                    } else if (c.item().isEmpty() || takeQuestItem(p, c.item())) complete(p, level);
                    else p.displayClientMessage(Component.literal("You lost the " + c.item() + "! Go back for it.").withStyle(ChatFormatting.RED), true);
                }
            }
            default -> { // 4.. : defend waves
                int wave = stage - 4;
                tracked.removeIf(u -> !(level.getEntity(u) instanceof LivingEntity l) || !l.isAlive());
                p.displayClientMessage(Component.literal("▶ Defend the settlement - wave " + (wave + 1) + "/" + c.count() + " · " + tracked.size() + " left").withStyle(ChatFormatting.GOLD), true);
                if (tracked.isEmpty() && level.getGameTime() > Long.parseLong(s[10])) {
                    if (wave + 1 >= c.count()) complete(p, level);
                    else { setStage(p, stage + 1); siegeWave(p, level, b, wave + 1); }
                }
            }
        }
    }

    static String pick(ServerLevel l, String... o) { return o[l.random.nextInt(o.length)]; }

    static String name(StoryChapters.Chapter c) {
        String n = c.target().replace("+", "").replace("!", "").replace("&truck", "");
        if (n.startsWith("boss:")) { try { return com.kiyamet.boss.BossKind.valueOf(n.substring(5).toUpperCase()).title; } catch (IllegalArgumentException e) { return "the beast"; } }
        return n;
    }

    static String goal(StoryChapters.Chapter c) {
        return switch (c.type()) {
            case FETCH -> "Find the " + c.item();
            case RESCUE -> "Find " + name(c);
            case KILL -> "Hunt down " + name(c);
            case DRIVE -> "Find the vehicle";
            case HERD -> "Meet the herd";
            default -> "Go";
        };
    }

    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    static void hud(ServerPlayer p, String text, BlockPos to) {
        double dx = to.getX() + 0.5 - p.getX(), dz = to.getZ() + 0.5 - p.getZ();
        int dist = (int) Math.sqrt(dx * dx + dz * dz);
        float yawTo = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
        float rel = Mth.wrapDegrees(yawTo - p.getYRot());
        String arrow = ARROWS[Math.floorMod(Math.round(rel / 45f), 8)];
        p.displayClientMessage(Component.literal("▶ " + text + "   ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(arrow + " " + dist + " m").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)), true);
    }

    // ------------------------------------------------------------------ encounters

    private static final ZombieKind[] WALKERS = {ZombieKind.FARMER, ZombieKind.WOMAN, ZombieKind.NURSE, ZombieKind.DOCTOR, ZombieKind.POLICE, ZombieKind.CRAWLER, ZombieKind.TOXIC};

    private static void walkers(ServerPlayer p, ServerLevel level, BlockPos at, int n) {
        for (int i = 0; i < n; i++) {
            BlockPos q = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.offset(level.random.nextInt(17) - 8, 0, level.random.nextInt(17) - 8));
            KZombie z = ModEntities_walker(level, q, WALKERS[level.random.nextInt(WALKERS.length)]);
            if (z != null) track(p, z);
        }
    }

    private static KZombie ModEntities_walker(ServerLevel level, BlockPos at, ZombieKind k) {
        KZombie z = com.kiyamet.entity.ModEntities.ZOMBIES.get(k).create(level, EntitySpawnReason.EVENT);
        if (z == null) return null;
        z.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        z.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.EVENT, null);
        z.setPersistenceRequired();
        level.addFreshEntity(z);
        return z;
    }

    private static HumanEntity human(ServerPlayer p, ServerLevel level, BlockPos at, Faction f, Role r, int lv, String name) {
        BlockPos q = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.offset(level.random.nextInt(9) - 4, 0, level.random.nextInt(9) - 4));
        HumanEntity h = ModHumans.spawn(level, q, f, r, lv, null);
        if (h == null) return null;
        if (name != null) h.setCustomName(Component.literal(name));
        h.setPersistenceRequired();
        track(p, h);
        return h;
    }

    private static void track(ServerPlayer p, Entity e) { TRACKED.computeIfAbsent(p.getUUID(), k -> new HashSet<>()).add(e.getUUID()); }

    /** Enemy level: grows with the chapter and with your power. */
    private static int lv(ServerPlayer p, int ch) { return Mth.clamp(8 + ch * 4 + SkillManager.get(p).totalPower() / 40, 5, 290); }

    /** How many guards: the chapter's base count, growing with each chapter. */
    private static int scale(int base, int ch) { return Math.round(base * (1 + ch / 25f)); }

    private static void guards(ServerPlayer p, ServerLevel level, BlockPos at, char g, int n, int ch) {
        for (int i = 0; i < n; i++) {
            char k = g == 'X' ? "WWRMB".charAt(level.random.nextInt(ch >= 29 ? 5 : 4)) : g;
            switch (k) {
                case 'R' -> human(p, level, at, Faction.RAIDER, i == 0 ? Role.LEADER : Role.MEMBER, lv(p, ch), null);
                case 'M' -> human(p, level, at, Faction.WHISPERER, Role.MEMBER, lv(p, ch), null);
                case 'B' -> robot(p, level, at, ch);
                default -> walkers(p, level, at, 1);
            }
        }
        if (g == 'M') walkers(p, level, at, n / 2);   // the masked always bring walkers
    }

    private static void robot(ServerPlayer p, ServerLevel level, BlockPos at, int ch) {
        com.kiyamet.robot.RobotKind k = ch >= 36 && level.random.nextFloat() < 0.3f ? com.kiyamet.robot.RobotKind.ROBOT_HEAVY
                : level.random.nextFloat() < 0.25f ? com.kiyamet.robot.RobotKind.SCOUT_DRONE : com.kiyamet.robot.RobotKind.ROBOT_TROOPER;
        var r = com.kiyamet.robot.ModRobots.TYPES.get(k).create(level, EntitySpawnReason.EVENT);
        if (r == null) return;
        BlockPos q = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.offset(level.random.nextInt(11) - 5, 0, level.random.nextInt(11) - 5));
        r.setPos(q.getX() + 0.5, q.getY(), q.getZ() + 0.5);
        r.setPersistenceRequired();
        level.addFreshEntity(r);
        track(p, r);
    }

    private static void arrive(ServerPlayer p, ServerLevel level, int chn, BlockPos t) {
        StoryChapters.Chapter c = CHAPTERS[chn - 1];
        String[] s = state(p);
        s[1] = "2";
        int n = scale(c.count(), chn);
        switch (c.type()) {
            case FETCH -> {
                BlockPos at = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, t);
                level.setBlock(at, Blocks.CHEST.defaultBlockState(), 3);
                if (level.getBlockEntity(at) instanceof ChestBlockEntity chest) {
                    chest.setItem(13, questItem(c.item()));
                    chest.setItem(11, new ItemStack(ModItems.get("medkit"), 1 + level.random.nextInt(2)));
                    chest.setItem(15, new ItemStack(ModItems.get("ruby"), 3 + chn / 4));
                }
                s[2] = "" + at.getX(); s[3] = "" + at.getY(); s[4] = "" + at.getZ();
                guards(p, level, at, c.guards(), n, chn);
                Talk.sayAs(p, RADIO, RC, pick(level, "Careful. You won't be alone out there.", "Eyes open. Something's guarding it.", "Stay low. Grab it and go."));
            }
            case RESCUE -> {
                HumanEntity d = human(p, level, t, Faction.SURVIVOR, Role.MEMBER, lv(p, chn), name(c));
                if (d != null) { d.setHealth(d.getMaxHealth() * 0.35f); s[8] = d.getUUID().toString(); TRACKED.get(p.getUUID()).remove(d.getUUID()); }
                guards(p, level, t, c.guards(), n, chn);
                if (d != null) Talk.say(p, d, pick(level, "Over here! Help me!", "Please - they've got me pinned!", "Is someone there?! I'm hurt!"));
            }
            case KILL -> {
                LivingEntity target;
                String tn = c.target();
                if (tn.startsWith("boss:")) {
                    target = null;
                    try {
                        var kind = com.kiyamet.boss.BossKind.valueOf(tn.substring(5).toUpperCase());
                        var boss = com.kiyamet.boss.ModBosses.TYPES.get(kind).create(level, EntitySpawnReason.EVENT);
                        if (boss != null) { boss.setPos(t.getX() + 0.5, t.getY(), t.getZ() + 0.5); boss.setPersistenceRequired(); level.addFreshEntity(boss); target = boss; track(p, boss); }
                    } catch (IllegalArgumentException ignored) {}
                } else {
                    boolean cmd = tn.startsWith("!");
                    target = human(p, level, t, c.guards() == 'M' ? Faction.WHISPERER : Faction.RAIDER, cmd ? Role.COMMANDER : Role.LEADER, cmd ? 300 : Math.min(290, lv(p, chn) + 20), name(c));
                }
                if (target != null) s[8] = target.getUUID().toString();
                if (c.target().contains("&truck")) {
                    List<HumanEntity> crew = new ArrayList<>();
                    for (int i = 0; i < 5; i++) { HumanEntity h = human(p, level, t, Faction.RAIDER, Role.MEMBER, lv(p, chn), null); if (h != null) crew.add(h); }
                    ModVehicles.raiderRide(level, t, crew, true, 9999);
                }
                guards(p, level, t, c.guards(), n, chn);
                Talk.sayAs(p, name(c), RC, pick(level, "You shouldn't have come here.", "So you're the one everyone talks about. Let's see.", "Nobody leaves this place alive.", "..."));
            }
            case DRIVE -> {
                VehicleKind k;
                try { k = VehicleKind.valueOf(c.target()); } catch (IllegalArgumentException e) { k = VehicleKind.PICKUP; }
                VehicleEntity v = ModVehicles.spawn(level, k, t.getX() + 0.5, t.getY(), t.getZ() + 0.5, level.random.nextFloat() * 360, 0.5f, 0.7f);
                if (v != null) s[8] = v.getUUID().toString();
                guards(p, level, t, c.guards(), n, chn);
                Talk.sayAs(p, RADIO, RC, "There it is. Keys should still be in it.");
            }
            case HERD -> {
                walkers(p, level, t, n + 8);
                s[10] = String.valueOf(level.getGameTime() + 20L * Math.max(150, 420 - chn * 4));
                KILLS.put(p.getUUID(), 0);
                Talk.sayAs(p, RADIO, RC, "There they are. Thin them out before they reach us!");
            }
            default -> {}
        }
        save(p, s);
    }

    private static void onTargetDeath(ServerPlayer p, LivingEntity e) {
        StoryChapters.Chapter c = ch(p);
        if (c.type() != StoryChapters.Type.KILL || stage(p) != 2) return;
        ServerLevel level = (ServerLevel) e.level();
        if (!c.item().isEmpty()) level.addFreshEntity(new ItemEntity(level, e.getX(), e.getY() + 0.5, e.getZ(), questItem(c.item())));
        Talk.sayAs(p, RADIO, RC, c.item().isEmpty() ? "Is it done? ...Come home." : "Grab the " + c.item() + " and come home.");
        setStage(p, 3);
    }

    private static void siegeWave(ServerPlayer p, ServerLevel level, BlockPos b, int wave) {
        StoryChapters.Chapter c = ch(p);
        int chn = chapter(p);
        String[] s = state(p);
        s[10] = String.valueOf(level.getGameTime() + 100);
        save(p, s);
        double a = level.random.nextDouble() * Math.PI * 2;
        BlockPos at = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, b.offset((int) (Math.cos(a) * 32), 0, (int) (Math.sin(a) * 32)));
        int n = scale(4 + wave * 3, chn);
        guards(p, level, at, c.guards(), n, chn);
        if (c.guards() != 'W') walkers(p, level, at, 2 + wave * 2);
        for (UUID u : TRACKED.getOrDefault(p.getUUID(), Set.of())) if (level.getEntity(u) instanceof net.minecraft.world.entity.Mob m && m.getTarget() == null) m.setTarget(p);
        level.playSound(null, b, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 3f, 1f);
        Talk.sayAs(p, MARCUS, MC, wave == 0 ? "Here they come! Everyone to the walls!" : wave + 1 == c.count() ? "Last push - they're throwing everything at us!" : "More of them! Hold!");
    }

    // ------------------------------------------------------------------ end

    static void complete(ServerPlayer p, ServerLevel level) {
        int chn = chapter(p);
        StoryChapters.Chapter c = CHAPTERS[chn - 1];
        String[] st = state(p);
        Entity tgt = st[8].isEmpty() ? null : level.getEntity(UUID.fromString(st[8]));
        if (c.type() == StoryChapters.Type.RESCUE && tgt instanceof HumanEntity h && !c.target().startsWith("+")) {
            Talk.say(p, h, "Thank you. I'll be safe here now.");
            h.discard();
        }
        save(p, new String[]{String.valueOf(chn + 1), "0"});
        TRACKED.remove(p.getUUID());
        int reward = reward(chn);
        title(p, Component.literal("MISSION PASSED").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), Component.literal("+" + reward + " rubies").withStyle(ChatFormatting.GREEN));
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 0.8f);
        Shop.give(p, new ItemStack(ModItems.get("ruby"), reward));
        SkillManager.addXp(p, Skill.SURVIVAL, 150 + chn * 40);
        SkillManager.addXp(p, Skill.SWORDS, 100 + chn * 30);
        if (chn % 10 == 0) Shop.give(p, new ItemStack(ModItems.get("tech_core"), 1 + chn / 20));
        if (chn == 8) Shop.give(p, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Kiyamet.id("armored_truck_keys"))));
        int d = 20;
        for (String o : c.outro()) { speak(p, d, o); d += 50; }
        if (chn == CHAPTERS.length)
            Talk.later(p, d + 20, () -> p.sendSystemMessage(Component.literal("★ THE LONG ROAD - COMPLETE. Thank you for playing the story.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
        else Talk.later(p, d + 20, () -> p.sendSystemMessage(Component.literal("Chapter " + (chn + 1) + " is waiting at any mission board.").withStyle(ChatFormatting.GRAY)));
    }

    static void fail(ServerPlayer p, String why) {
        String[] s = state(p);
        s[1] = "0";
        save(p, new String[]{s[0], "0"});
        Set<UUID> t = TRACKED.remove(p.getUUID());
        if (t != null) for (UUID u : t) {
            Entity e = ((ServerLevel) p.level()).getEntity(u);
            if (e instanceof HumanEntity h && h.faction() == Faction.RAIDER && !h.hasCustomName()) h.discard();
        }
        title(p, Component.literal("MISSION FAILED").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), Component.literal(why + " - try again from a mission board").withStyle(ChatFormatting.GRAY));
    }

    // ------------------------------------------------------------------ quest items

    static ItemStack questItem(String name) {
        ItemStack s = new ItemStack(ModTwd.QUEST_ITEM);
        CompoundTag tag = new CompoundTag();
        tag.putString("Quest", name);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        s.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(st -> st.withItalic(false).withColor(ChatFormatting.GOLD)));
        return s;
    }

    static boolean isQuest(ItemStack s, String name) {
        return s.is(ModTwd.QUEST_ITEM) && s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr("Quest", "").equals(name);
    }

    static boolean hasQuestItem(ServerPlayer p, String name) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (isQuest(p.getInventory().getItem(i), name)) return true;
        return false;
    }

    static boolean takeQuestItem(ServerPlayer p, String name) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (isQuest(p.getInventory().getItem(i), name)) { p.getInventory().getItem(i).shrink(1); return true; }
        return false;
    }

    /** The person you came to rescue: talk to them (a medkit or bandage in hand heals them) and they follow you. */
    public static boolean interact(ServerPlayer p, HumanEntity h) {
        String[] s = state(p);
        if (!"2".equals(s[1]) || !h.getUUID().toString().equals(s[8])) return false;
        ItemStack held = p.getMainHandItem();
        if (held.is(ModItems.get("medkit")) || held.is(ModItems.get("bandage"))) { held.shrink(1); h.heal(12); Talk.say(p, h, "Oh god, that's better. Thank you."); }
        else Talk.say(p, h, "Marcus sent you? ...Okay. Get me out of here.");
        h.setOwner(p);
        return true;
    }

    static void title(ServerPlayer p, Component t, Component sub) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
        p.connection.send(new ClientboundSetTitleTextPacket(t));
    }

    private Story() {}
}
