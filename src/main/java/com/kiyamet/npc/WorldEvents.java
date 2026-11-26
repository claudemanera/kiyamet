package com.kiyamet.npc;

import com.kiyamet.entity.KZombie;
import com.kiyamet.entity.ZombieKind;
import com.kiyamet.registry.ModItems;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * World events: walker herds crossing the land, acid rain, and raider envoys demanding tribute
 * (pay and the raid clock resets, refuse and they come tonight).
 */
public final class WorldEvents {
    static boolean acidRain;
    private static boolean wasRaining;

    private record Herd(List<UUID> walkers, BlockPos dest, long until) {}
    private static final List<Herd> HERDS = new ArrayList<>();

    private record Envoy(UUID human, UUID player, int demand, long until) {}
    private static final Map<UUID, Envoy> ENVOYS = new HashMap<>();   // envoy entity -> deal
    private static final Map<UUID, Long> LEAVING = new HashMap<>();    // envoy entity -> despawn tick

    public static boolean acidRain() { return acidRain; }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int t = server.getTickCount();
            ServerLevel ow = server.overworld();
            if (t % 20 == 0) weather(server, ow);
            if (t % 40 == 0 && acidRain) acid(ow);
            if (t % 20 == 5) herds(ow, t);
            if (t % 20 == 10) envoys(server, ow, t);
            if (t % 2400 == 600) for (ServerPlayer p : server.getPlayerList().getPlayers()) roll(p, ow);
        });

        // hitting the envoy = refusing
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((e, src, amount) -> {
            if (e instanceof HumanEntity h && h.envoy && src.getEntity() instanceof ServerPlayer p) { refuse(p, h); }
            return true;
        });

        CommandRegistrationCallback.EVENT.register((d, ctx, env) -> d.register(Commands.literal("twd")
                .then(Commands.literal("event").then(Commands.argument("what", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (!p.isCreative()) { p.sendSystemMessage(Component.literal("Creative mode only.")); return 0; }
                    switch (com.mojang.brigadier.arguments.StringArgumentType.getString(c, "what")) {
                        case "herd" -> herd(p, p.level());
                        case "acid" -> { acidRain = !acidRain; p.sendSystemMessage(Component.literal("Acid rain: " + acidRain)); }
                        case "tribute" -> envoy(p, p.level());
                        default -> p.sendSystemMessage(Component.literal("herd | acid | tribute"));
                    }
                    return 1;
                })))));
    }

    private static void roll(ServerPlayer p, ServerLevel ow) {
        if (p.level() != ow || p.isSpectator()) return;
        int power = ModHumans.power(p);
        float r = ow.random.nextFloat();
        if (power >= 150 && r < 0.07f) herd(p, ow);
        else if (power >= 500 && r < 0.11f && !BaseRaids.busy(p) && ENVOYS.values().stream().noneMatch(e -> e.player.equals(p.getUUID())) && ow.isBrightOutside()) envoy(p, ow);
    }

    // ---------------------------------------------------------------- acid rain

    private static void weather(MinecraftServer server, ServerLevel ow) {
        boolean raining = ow.isRaining();
        if (raining && !wasRaining && ow.getDayTime() / 24000 >= 5 && ow.random.nextFloat() < 0.3f) {
            acidRain = true;
            for (ServerPlayer p : server.getPlayerList().getPlayers())
                p.sendSystemMessage(Component.literal("The rain burns. ACID RAIN - get under a roof or wear a helmet!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        }
        if (!raining && acidRain) {
            acidRain = false;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) p.sendSystemMessage(Component.literal("The acid rain has stopped.").withStyle(ChatFormatting.GRAY));
        }
        wasRaining = raining;
    }

    private static void acid(ServerLevel ow) {
        for (ServerPlayer p : ow.players()) {
            if (p.isCreative() || p.isSpectator()) continue;
            if (p.getVehicle() instanceof com.kiyamet.vehicle.VehicleEntity v && v.kind != com.kiyamet.vehicle.VehicleKind.ATV && v.kind != com.kiyamet.vehicle.VehicleKind.MOTORCYCLE) continue;
            if (!ow.isRainingAt(BlockPos.containing(p.getEyePosition()).above())) continue;
            ItemStack helmet = p.getItemBySlot(EquipmentSlot.HEAD);
            if (!helmet.isEmpty() && helmet.isDamageableItem()) helmet.hurtAndBreak(1, p, EquipmentSlot.HEAD);
            else p.hurtServer(ow, ow.damageSources().magic(), 1f);
            ow.sendParticles(ParticleTypes.SNEEZE, p.getX(), p.getY() + 1.8, p.getZ(), 4, 0.3, 0.2, 0.3, 0);
        }
        // the acid also hurts walkers and raiders out in the open, a little
        for (ServerPlayer p : ow.players())
            for (Mob m : ow.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32), m -> (m instanceof KZombie || m instanceof HumanEntity h && h.faction() == Faction.RAIDER) && ow.isRainingAt(m.blockPosition().above(2))))
                if (ow.random.nextFloat() < 0.3f) m.hurtServer(ow, ow.damageSources().magic(), 1f);
    }

    // ---------------------------------------------------------------- herds

    private static final ZombieKind[] HERD = {ZombieKind.FARMER, ZombieKind.FARMER, ZombieKind.WOMAN, ZombieKind.NURSE, ZombieKind.DOCTOR, ZombieKind.POLICE, ZombieKind.CRAWLER, ZombieKind.TOXIC, ZombieKind.ARMORED, ZombieKind.SCREAMER};

    static void herd(ServerPlayer p, ServerLevel level) {
        BlockPos start = ModHumans.surface(level, p.blockPosition(), 56, 72);
        if (start == null) return;
        BlockPos dest = new BlockPos(2 * p.getBlockX() - start.getX() + level.random.nextInt(31) - 15, p.getBlockY(), 2 * p.getBlockZ() - start.getZ() + level.random.nextInt(31) - 15);
        int n = 18 + level.random.nextInt(17) + Math.min(16, ModHumans.power(p) / 400);
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            BlockPos at = start.offset(level.random.nextInt(13) - 6, 0, level.random.nextInt(13) - 6);
            at = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);
            KZombie z = BaseRaids.walker(level, at, HERD[level.random.nextInt(HERD.length)]);
            if (z != null) ids.add(z.getUUID());
        }
        HERDS.add(new Herd(ids, dest, level.getGameTime() + 20 * 60 * 4));
        String dir = dir(start, p.blockPosition());
        p.sendSystemMessage(Component.literal("The ground trembles... a HERD of " + n + " walkers is moving through, coming from the " + dir + ". Stay quiet or get out of the way.").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
        level.playSound(null, p.blockPosition(), SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 2f, 0.5f);
        for (HumanEntity h : level.getEntitiesOfClass(HumanEntity.class, p.getBoundingBox().inflate(48), h -> h.isOwnedBy(p) && h.job() == Job.SCOUT)) {
            p.sendSystemMessage(Component.literal("[" + h.getName().getString() + "] Herd from the " + dir + "! Dozens of them. Let them pass.").withStyle(ChatFormatting.AQUA));
            break;
        }
    }

    static String dir(BlockPos from, BlockPos to) {
        double a = Math.atan2(from.getZ() - to.getZ(), from.getX() - to.getX());
        String[] d = {"east", "south-east", "south", "south-west", "west", "north-west", "north", "north-east"};
        return d[Math.floorMod((int) Math.round(a / (Math.PI / 4)), 8)];
    }

    private static void herds(ServerLevel level, int t) {
        for (Iterator<Herd> it = HERDS.iterator(); it.hasNext(); ) {
            Herd h = it.next();
            boolean over = level.getGameTime() > h.until;
            int alive = 0;
            for (UUID u : h.walkers) {
                Entity e = level.getEntity(u);
                if (!(e instanceof KZombie z) || !z.isAlive()) continue;
                alive++;
                if (over) { if (level.getNearestPlayer(z, 40) == null) z.discard(); continue; }
                if (z.getTarget() == null && t % 60 == 5) z.getNavigation().moveTo(h.dest.getX() + 0.5, h.dest.getY(), h.dest.getZ() + 0.5, 0.9);
            }
            if (over || alive == 0) it.remove();
        }
    }

    // ---------------------------------------------------------------- tribute

    static void envoy(ServerPlayer p, ServerLevel level) {
        BlockPos at = ModHumans.surface(level, p.blockPosition(), 18, 26);
        if (at == null) return;
        int power = ModHumans.power(p);
        HumanEntity h = ModHumans.spawn(level, at, Faction.RAIDER, Role.LEADER, Mth.clamp(20 + power / 25, 20, 240), null);
        if (h == null) return;
        h.envoy = true;
        h.setTarget(null);
        h.setCustomName(Component.literal("Raider Envoy").withStyle(ChatFormatting.GOLD));
        int demand = Mth.clamp(8 + power / 150, 8, 40);
        ENVOYS.put(h.getUUID(), new Envoy(h.getUUID(), p.getUUID(), demand, level.getGameTime() + 20 * 150));
        p.sendSystemMessage(Component.literal("A raider walks up with his hands raised. He wants to talk - right-click him.").withStyle(ChatFormatting.GOLD));
    }

    static void envoyTalk(ServerPlayer p, HumanEntity h) {
        Envoy e = ENVOYS.get(h.getUUID());
        if (e == null || !e.player.equals(p.getUUID())) { Talk.say(p, h, "Not you. Where's the one in charge?"); return; }
        Dialogues.envoy(p, h, e.demand,
                d -> { Envoy cur = ENVOYS.get(h.getUUID()); if (cur != null) ENVOYS.put(h.getUUID(), new Envoy(cur.human, cur.player, d, cur.until)); },
                () -> pay(p, h),
                () -> refuse(p, h));
    }

    private static void pay(ServerPlayer p, HumanEntity h) {
        Envoy e = ENVOYS.get(h.getUUID());
        if (e == null) return;
        if (!Shop.take(p, Shop.RUBY, e.demand)) return;
        BaseRaids.paid(p);
        Talk.say(p, h, "Smart. Nobody touches your place for a while.");
        p.sendSystemMessage(Component.literal("(" + e.demand + " rubies paid - no raids for at least a week)").withStyle(ChatFormatting.GOLD));
        leave(h);
    }

    private static void refuse(ServerPlayer p, HumanEntity h) {
        Envoy e = ENVOYS.remove(h.getUUID());
        h.envoy = false;
        h.setCustomName(null);
        if (e == null) return;
        ServerPlayer owner = p.level().getServer().getPlayerList().getPlayer(e.player);
        if (owner == null) return;
        BaseRaids.scheduleTonight(owner);
        owner.sendSystemMessage(Component.literal("Raiders will hit your base after dark!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
    }

    private static void leave(HumanEntity h) {
        ENVOYS.remove(h.getUUID());
        LEAVING.put(h.getUUID(), h.level().getGameTime() + 400);
        BlockPos away = h.blockPosition().offset(h.getRandom().nextInt(80) - 40, 0, h.getRandom().nextInt(80) - 40);
        h.getNavigation().moveTo(away.getX(), away.getY(), away.getZ(), 1.0);
    }

    private static void envoys(MinecraftServer server, ServerLevel level, int t) {
        for (Iterator<Map.Entry<UUID, Envoy>> it = ENVOYS.entrySet().iterator(); it.hasNext(); ) {
            Envoy e = it.next().getValue();
            Entity ent = level.getEntity(e.human);
            if (!(ent instanceof HumanEntity h) || !h.isAlive()) { it.remove(); continue; }
            ServerPlayer p = server.getPlayerList().getPlayer(e.player);
            h.setTarget(null);
            if (p != null && p.level() == level && h.distanceTo(p) > 4) h.getNavigation().moveTo(p, 0.9);
            else h.getNavigation().stop();
            if (p != null && p.level() == level) h.getLookControl().setLookAt(p);
            if (level.getGameTime() > e.until) {
                it.remove();
                h.envoy = false;
                h.setCustomName(null);
                if (p != null) {
                    BaseRaids.scheduleTonight(p);
                    p.sendSystemMessage(Component.literal("[Envoy] \"No answer is an answer.\" - Raiders will hit your base tonight!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                }
                leave(h);
            }
        }
        for (Iterator<Map.Entry<UUID, Long>> it = LEAVING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> en = it.next();
            if (level.getGameTime() < en.getValue()) continue;
            Entity ent = level.getEntity(en.getKey());
            if (ent instanceof HumanEntity h && h.faction() == Faction.RAIDER && level.getNearestPlayer(h, 24) == null) h.discard();
            it.remove();
        }
    }

    private WorldEvents() {}
}
