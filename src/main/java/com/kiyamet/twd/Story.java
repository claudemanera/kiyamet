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

    public record Chapter(String title, String tagline, int reward, String[] intro, String accept) {}
    public static final Chapter[] CHAPTERS = {
            new Chapter("Medicine Run", "Elena's daughter is burning up", 20, new String[]{
                    "You. You're the one who answered the three questions right. I'm Marcus. I keep this place running.",
                    "Elena's girl has a fever. Bad one. We're out of antibiotics.",
                    "There's a pharmacy crate out past the old road. Walkers got there first. Bring the medicine back."}, "I'll get the medicine."),
            new Chapter("The Missing Scout", "Dani went out and didn't come back", 25, new String[]{
                    "Lily's fever broke. Elena hasn't stopped crying. Thank you.",
                    "Now I need you again. Dani - our best scout - went out three days ago. Her radio's gone quiet.",
                    "Last signal came from out there. Find her. If she's alive, bring her home."}, "I'll find her."),
            new Chapter("Stolen Fuel", "Raiders took the generator fuel", 30, new String[]{
                    "Dani's tough. She says she'd follow you anywhere now. Can't blame her.",
                    "Bad news: raiders hit our fuel depot last night. A man called Spike leads that crew.",
                    "No fuel, no fence. No fence... you know. Get it back. Spike has it."}, "Spike won't keep it."),
            new Chapter("Wheels", "The settlement needs a truck", 35, new String[]{
                    "With that fuel we can finally run a truck. Problem is, we don't have one.",
                    "Dani spotted an old pickup on the highway. Half a tank, maybe.",
                    "Drive it back here. Try not to hit too many walkers... actually, hit as many as you want."}, "I'll bring the truck."),
            new Chapter("The Siege", "The Commander wants an answer", 45, new String[]{
                    "We've got a problem. A man came to the gate. Says he speaks for 'the Commander'.",
                    "Half of everything we have, every month. Or they burn us.",
                    "I told him no. They're coming tonight - maybe sooner. Stand with us at the board."}, "We hold the line."),
            new Chapter("The Herd", "A herd is heading for the walls", 45, new String[]{
                    "We held. Barely. But the noise drew something worse.",
                    "Dani counted over thirty walkers moving this way. A whole herd.",
                    "Meet them out there before they reach the walls. Thin them out - twenty-five at least."}, "I'll thin the herd."),
            new Chapter("Inside Man", "The Commander's map", 55, new String[]{
                    "Elena overheard one of the raiders we captured. The Commander keeps a map of every settlement he taxes.",
                    "If we had it, we could warn them all. Maybe even find where he sleeps.",
                    "It's in a crate at their outpost. Guarded. Some of those masked freaks are with them now. Get it."}, "I'll get the map."),
            new Chapter("The Commander", "End this", 100, new String[]{
                    "The map shows everything. His convoy, his route... and where he'll be tonight.",
                    "This is it. Kill the Commander and this whole region breathes again.",
                    "Take whatever you need. And come back alive. That's an order."}, "Let's end this.")};

    static final String[] QUEST_ITEM = {"Antibiotics", "", "Stolen Fuel", "", "", "", "Commander's Map", ""};

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
        Chapter c = CHAPTERS[ch - 1];
        int d = 0;
        for (String line : c.intro) { Talk.sayAsLater(p, d, MARCUS, MC, line); d += Math.min(60, 20 + line.length() / 2); }
        Talk.askAs(p, board, MARCUS, MC, d, "", null,
                new Talk.Option(c.accept, pl -> start(pl, board, ch)),
                new Talk.Option("Not now.", pl -> Talk.sayAs(pl, MARCUS, MC, "Don't take too long.")));
    }

    static void start(ServerPlayer p, MissionBoard board, int ch) {
        ServerLevel level = (ServerLevel) p.level();
        BlockPos b = board.blockPosition();
        int dist = ch == 5 ? 0 : 110 + ch * 20;
        BlockPos t = b;
        if (dist > 0) {
            double a = level.random.nextDouble() * Math.PI * 2;
            BlockPos raw = b.offset((int) (Math.cos(a) * dist), 0, (int) (Math.sin(a) * dist));
            t = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, raw);
        }
        String[] s = {String.valueOf(ch), ch == 5 ? "4" : "1", "" + t.getX(), "" + t.getY(), "" + t.getZ(), "" + b.getX(), "" + b.getY(), "" + b.getZ(), "", "0", "0"};
        save(p, s);
        TRACKED.put(p.getUUID(), new HashSet<>());
        KILLS.put(p.getUUID(), 0);
        title(p, Component.literal("CHAPTER " + ch + ": " + CHAPTERS[ch - 1].title.toUpperCase()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal(CHAPTERS[ch - 1].tagline).withStyle(ChatFormatting.GRAY));
        level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.5f, 0.6f);
        if (ch == 5) siegeWave(p, level, b, 0);
    }

    // ------------------------------------------------------------------ ticking

    private static void tick(ServerPlayer p) {
        String[] s = state(p);
        int ch = Integer.parseInt(s[0]), stage = Integer.parseInt(s[1]);
        if (stage == 0 || ch > CHAPTERS.length) return;
        ServerLevel level = (ServerLevel) p.level();
        BlockPos t = target(s), b = board(s);
        Set<UUID> tracked = TRACKED.computeIfAbsent(p.getUUID(), k -> new HashSet<>());
        switch (stage) {
            case 1 -> { // go there
                hud(p, goal(ch), t);
                if (p.blockPosition().distSqr(t) < 28 * 28) { arrive(p, level, ch, t); }
            }
            case 2 -> { // do the thing
                switch (ch) {
                    case 1, 7 -> { hud(p, ch == 1 ? "Grab the Antibiotics from the crate" : "Steal the map from the crate", t); if (hasQuestItem(p, QUEST_ITEM[ch - 1])) { setStage(p, 3); Talk.sayAs(p, RADIO, RC, ch == 1 ? "Marcus: Got it? Good. Get back here, fast." : "Marcus: You have it?! Get out of there!"); } }
                    case 2 -> {
                        Entity e = s[8].isEmpty() ? null : level.getEntity(UUID.fromString(s[8]));
                        if (e instanceof HumanEntity d && d.isAlive()) {
                            if (d.isOwnedBy(p)) { setStage(p, 3); Talk.sayAs(p, DANI, DC, "Let's go home. I'll watch your back."); }
                            else hud(p, "Find Dani and talk to her", d.blockPosition());
                        } else if (e == null && level.isLoaded(t)) fail(p, "Dani didn't make it");
                    }
                    case 3, 8 -> hud(p, ch == 3 ? "Kill Spike and take the fuel" : "Kill the Commander", t);
                    case 4 -> {
                        Entity e = s[8].isEmpty() ? null : level.getEntity(UUID.fromString(s[8]));
                        if (e instanceof VehicleEntity v && v.isAlive()) {
                            hud(p, v.hasPassenger(p) ? "Drive the pickup back to the settlement" : "Get in the pickup", v.hasPassenger(p) ? b : v.blockPosition());
                            if (v.hasPassenger(p) && v.blockPosition().distSqr(b) < 18 * 18) complete(p, level);
                        } else if (e == null && level.isLoaded(t) && p.blockPosition().distSqr(t) < 64 * 64) fail(p, "The pickup was destroyed");
                    }
                    case 6 -> {
                        int k = KILLS.getOrDefault(p.getUUID(), 0);
                        long left = Long.parseLong(s[10]) - level.getGameTime();
                        p.displayClientMessage(Component.literal("▶ Thin the herd: " + k + " / 25   ⏱ " + Math.max(0, left / 20) + "s").withStyle(ChatFormatting.GOLD), true);
                        if (k >= 25) complete(p, level);
                        else if (left <= 0) fail(p, "The herd reached the walls");
                    }
                }
            }
            case 3 -> { // back to the board
                if (ch == 2 && !s[8].isEmpty()) {
                    Entity dani = level.getEntity(UUID.fromString(s[8]));
                    if (dani == null && level.isLoaded(p.blockPosition()) && p.blockPosition().distSqr(b) < 32 * 32) { /* she may lag behind */ }
                    else if (dani != null && !dani.isAlive()) { fail(p, "Dani didn't make it"); return; }
                }
                hud(p, ch == 2 ? "Bring Dani home" : ch == 3 || ch == 1 || ch == 7 ? "Bring it back to Marcus" : "Return to Marcus", b);
                if (p.blockPosition().distSqr(b) < 10 * 10) {
                    if (QUEST_ITEM[ch - 1].isEmpty() || takeQuestItem(p, QUEST_ITEM[ch - 1])) complete(p, level);
                    else p.displayClientMessage(Component.literal("You lost the " + QUEST_ITEM[ch - 1] + "! Go back for it.").withStyle(ChatFormatting.RED), true);
                }
            }
            case 4, 5, 6 -> { // siege waves at the board
                int wave = stage - 4;
                tracked.removeIf(u -> !(level.getEntity(u) instanceof LivingEntity l) || !l.isAlive());
                p.displayClientMessage(Component.literal("▶ Defend the settlement - wave " + (wave + 1) + "/3 · " + tracked.size() + " left").withStyle(ChatFormatting.GOLD), true);
                if (tracked.isEmpty() && level.getGameTime() > Long.parseLong(s[10])) {
                    if (wave == 2) complete(p, level);
                    else { setStage(p, stage + 1); siegeWave(p, level, b, wave + 1); }
                }
            }
            default -> {}
        }
    }

    static String goal(int ch) {
        return switch (ch) {
            case 1 -> "Go to the pharmacy crate"; case 2 -> "Follow Dani's last signal"; case 3 -> "Find Spike's camp"; case 4 -> "Find the pickup on the highway";
            case 6 -> "Meet the herd"; case 7 -> "Reach the raider outpost"; case 8 -> "Find the Commander's convoy"; default -> "Go";
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

    private static int lv(ServerPlayer p, int min) { return Mth.clamp(min + SkillManager.get(p).totalPower() / 30, min, 250); }

    private static void arrive(ServerPlayer p, ServerLevel level, int ch, BlockPos t) {
        String[] s = state(p);
        s[1] = "2";
        switch (ch) {
            case 1, 7 -> {
                BlockPos c = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, t);
                level.setBlock(c, Blocks.CHEST.defaultBlockState(), 3);
                if (level.getBlockEntity(c) instanceof ChestBlockEntity chest) {
                    chest.setItem(13, questItem(QUEST_ITEM[ch - 1]));
                    chest.setItem(11, new ItemStack(ModItems.get("medkit"), 1 + level.random.nextInt(2)));
                    chest.setItem(15, new ItemStack(ModItems.get("ruby"), 3 + level.random.nextInt(5)));
                }
                s[2] = "" + c.getX(); s[3] = "" + c.getY(); s[4] = "" + c.getZ();
                if (ch == 1) { walkers(p, level, c, 8); Talk.sayAs(p, RADIO, RC, "Marcus: Careful. There'll be walkers around the crate."); }
                else {
                    for (int i = 0; i < 4; i++) human(p, level, c, Faction.RAIDER, i == 0 ? Role.LEADER : Role.MEMBER, lv(p, 30), null);
                    for (int i = 0; i < 3; i++) human(p, level, c, Faction.WHISPERER, Role.MEMBER, lv(p, 30), null);
                    Talk.sayAs(p, RADIO, RC, "Dani: I see raiders... and the masked ones. Stay low.");
                }
            }
            case 2 -> {
                HumanEntity d = human(p, level, t, Faction.SURVIVOR, Role.MEMBER, lv(p, 20), DANI);
                if (d != null) { d.setHealth(d.getMaxHealth() * 0.35f); s[8] = d.getUUID().toString(); }
                walkers(p, level, t, 7);
                Talk.sayAs(p, DANI, DC, "Over here! I'm hurt - they've got me pinned!");
            }
            case 3 -> {
                HumanEntity spike = human(p, level, t, Faction.RAIDER, Role.LEADER, lv(p, 40) + 15, "Spike");
                for (int i = 0; i < 5; i++) human(p, level, t, Faction.RAIDER, Role.MEMBER, lv(p, 25), null);
                if (spike != null) s[8] = spike.getUUID().toString();
                Talk.sayAs(p, "Spike", RC, "You lost, friend? This fuel's ours now. Walk away... or don't.");
            }
            case 4 -> {
                VehicleEntity v = ModVehicles.spawn(level, VehicleKind.PICKUP, t.getX() + 0.5, t.getY(), t.getZ() + 0.5, level.random.nextFloat() * 360, 0.5f, 0.7f);
                if (v != null) s[8] = v.getUUID().toString();
                walkers(p, level, t, 6);
                Talk.sayAs(p, RADIO, RC, "Dani: That's it, the red pickup. Keys should still be in it.");
            }
            case 6 -> {
                walkers(p, level, t, 32);
                s[10] = String.valueOf(level.getGameTime() + 20 * 60 * 6);
                KILLS.put(p.getUUID(), 0);
                Talk.sayAs(p, RADIO, RC, "Dani: There they are. God, there's so many. You've got six minutes before they reach us.");
            }
            case 8 -> {
                List<HumanEntity> crew = new ArrayList<>();
                HumanEntity cmd = human(p, level, t, Faction.RAIDER, Role.COMMANDER, 300, "The Commander");
                for (int i = 0; i < 6; i++) { HumanEntity h = human(p, level, t, Faction.RAIDER, Role.MEMBER, lv(p, 60), null); if (h != null) crew.add(h); }
                ModVehicles.raiderRide(level, t, crew, true, 9999);
                if (cmd != null) s[8] = cmd.getUUID().toString();
                Talk.sayAs(p, "The Commander", RC, "So you're the one who's been killing my men. Let's see what you're made of.");
            }
            default -> {}
        }
        save(p, s);
    }

    private static void onTargetDeath(ServerPlayer p, LivingEntity e) {
        int ch = chapter(p);
        ServerLevel level = (ServerLevel) e.level();
        if (ch == 3) {
            level.addFreshEntity(new ItemEntity(level, e.getX(), e.getY() + 0.5, e.getZ(), questItem(QUEST_ITEM[2])));
            Talk.sayAs(p, RADIO, RC, "Marcus: Spike's down? Grab the fuel and come home.");
            setStage(p, 3);
        } else if (ch == 8) {
            Talk.sayAs(p, RADIO, RC, "Marcus: ...Is it done? Is he really dead? Come home. Come home now.");
            setStage(p, 3);
        }
    }

    private static void siegeWave(ServerPlayer p, ServerLevel level, BlockPos b, int wave) {
        String[] s = state(p);
        s[10] = String.valueOf(level.getGameTime() + 100);
        save(p, s);
        double a = level.random.nextDouble() * Math.PI * 2;
        BlockPos at = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, b.offset((int) (Math.cos(a) * 32), 0, (int) (Math.sin(a) * 32)));
        int raiders = 3 + wave * 2;
        for (int i = 0; i < raiders; i++) { HumanEntity h = human(p, level, at, Faction.RAIDER, i == 0 ? Role.LEADER : Role.MEMBER, lv(p, 30), null); if (h != null) h.setTarget(p); }
        walkers(p, level, at, 4 + wave * 3);
        level.playSound(null, b, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 3f, 1f);
        Talk.sayAs(p, MARCUS, MC, wave == 0 ? "Here they come! Everyone to the walls!" : wave == 1 ? "More of them! Hold!" : "Last push - they're throwing everything at us!");
    }

    // ------------------------------------------------------------------ end

    static void complete(ServerPlayer p, ServerLevel level) {
        int ch = chapter(p);
        Chapter c = CHAPTERS[ch - 1];
        String[] s = {String.valueOf(ch + 1), "0"};
        save(p, s);
        TRACKED.remove(p.getUUID());
        title(p, Component.literal("MISSION PASSED").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), Component.literal("+" + c.reward + " rubies").withStyle(ChatFormatting.GREEN));
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 0.8f);
        Shop.give(p, new ItemStack(ModItems.get("ruby"), c.reward));
        SkillManager.addXp(p, Skill.SURVIVAL, 150 + ch * 60);
        SkillManager.addXp(p, Skill.SWORDS, 100 + ch * 40);
        String[] outro = switch (ch) {
            case 1 -> new String[]{ELENA + "|You... you brought it. Thank you. I won't forget this."};
            case 2 -> new String[]{DANI + "|Marcus, I'm fine. Thanks to this one. I'm sticking with them from now on."};
            case 3 -> new String[]{MARCUS + "|The fence is humming again. Spike won't be bothering anyone."};
            case 4 -> new String[]{MARCUS + "|She runs! Keep her - you earned it. Just bring her back for supply runs."};
            case 5 -> new String[]{MARCUS + "|We're still standing. Because of you. The Commander won't like this."};
            case 6 -> new String[]{DANI + "|The herd's scattered. The walls held. You're insane, you know that?"};
            case 7 -> new String[]{MARCUS + "|This map... he's got eleven settlements under his boot. Not for long."};
            case 8 -> new String[]{MARCUS + "|It's over. The Commander's dead. Every settlement on that map is free.", ELENA + "|Lily drew you a picture. She says you're a hero.",
                    MARCUS + "|Whatever comes next - you've got a home here. Always."};
            default -> new String[0];
        };
        int d = 20;
        for (String o : outro) {
            String[] kv = o.split("\\|", 2);
            ChatFormatting col = kv[0].equals(ELENA) ? EC : kv[0].equals(DANI) ? DC : MC;
            Talk.sayAsLater(p, d, kv[0], col, kv[1]);
            d += 50;
        }
        if (ch == 8) {
            Shop.give(p, new ItemStack(ModItems.get("tech_core"), 2));
            Shop.give(p, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Kiyamet.id("armored_truck_keys"))));
            Talk.later(p, d + 20, () -> p.sendSystemMessage(Component.literal("★ THE LONG ROAD - COMPLETE. You received the Commander's armored truck.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
        }
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

    /** Dani needs help: right-click her with a medkit or bandage (or just talk) and she follows you. */
    public static boolean interact(ServerPlayer p, HumanEntity h) {
        String[] s = state(p);
        if (!"2".equals(s[0]) || !"2".equals(s[1]) || !h.getUUID().toString().equals(s[8])) return false;
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
