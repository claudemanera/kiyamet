package com.kiyamet.end;

import com.kiyamet.Kiyamet;
import com.kiyamet.element.Temples;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * TWD 2.2: five great structures float in the outer End, over and over: the Void Citadel, the Sky Spire, the Fallen Sky
 * Ship, the Ender Hive and the Starfall Observatory. Each is built the first time a player comes near. The Temple of
 * Wisdom stands alone, just south of the dragon's island.
 */
public final class EndStructures {
    public enum Type {
        VOID_CITADEL("void_citadel", "The Void Citadel", "A fortress the plague took. Its tyrant waits in the keep.", 60),
        SKY_SPIRE("sky_spire", "The Sky Spire", "Climb. The herald waits at the crown.", 40),
        FALLEN_SKYSHIP("fallen_skyship", "The Fallen Sky Ship", "Its dead captain never left the cabin.", 48),
        ENDER_HIVE("ender_hive", "The Ender Hive", "Something breeds inside. Mind the eggs.", 32),
        STARFALL_OBSERVATORY("starfall_observatory", "The Starfall Observatory", "Where the stars fell, a warden still keeps watch.", 40);

        public final String id, title, hint;
        public final int radius;
        Type(String id, String title, String hint, int radius) { this.id = id; this.title = title; this.hint = hint; this.radius = radius; }
        public static Type of(String id) { for (Type t : values()) if (t.id.equals(id)) return t; return null; }
    }

    public static final AttachmentType<List<String>> BUILT = AttachmentRegistry.create(Kiyamet.id("end_built"), b -> b.initializer(List::of).persistent(Codec.STRING.listOf()));
    static final List<Temples.Job> JOBS = new ArrayList<>();
    static final int CELL = 576;
    public static final BlockPos WISDOM = new BlockPos(0, 72, 150);

    public record Site(int cx, int cz, BlockPos pos, Type type) { String key() { return cx + "," + cz; } }

    /** the structure of one region of the outer End, or null */
    public static Site site(long seed, int cx, int cz) {
        Random r = new Random(seed ^ (cx * 341873128712L) ^ (cz * 132897987541L));
        if (r.nextFloat() < 0.12f) return null;
        int x = cx * CELL + 100 + r.nextInt(CELL - 200), z = cz * CELL + 100 + r.nextInt(CELL - 200);
        if ((long) x * x + (long) z * z < 1150L * 1150L) return null;                       // the outer islands only
        int y = 140 + r.nextInt(30);                                                       // high above the End islands
        Type t = Type.values()[Math.floorMod(cx + 2 * cz + (int) (seed & 0xff), 5)];
        return new Site(cx, cz, new BlockPos(x, y, z), t);
    }

    static boolean built(ServerLevel l, String key) { return l.getAttachedOrElse(BUILT, List.of()).contains(key); }
    static void markBuilt(ServerLevel l, String key) { List<String> b = new ArrayList<>(l.getAttachedOrElse(BUILT, List.of())); b.add(key); l.setAttached(BUILT, List.copyOf(b)); }

    public static Builder plan(ServerLevel l, Type t, BlockPos at) {
        Builder b = new Builder(l, at, at.asLong() * 31 + t.ordinal());
        b.loadChunks(t.radius + 20);
        EndDesigns.plan(b, t);
        return b;
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerLevel end = server.getLevel(Level.END);
            if (end == null) return;
            if (!JOBS.isEmpty() && JOBS.get(0).step(end, 20000)) JOBS.remove(0);
            if (server.getTickCount() % 40 != 0) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.level() != end) continue;
                if (!built(end, "wisdom")) { markBuilt(end, "wisdom"); JOBS.add(0, WisdomTemple.plan(end, WISDOM).job()); }
                if (!p.getTags().contains("twd_wisdom_told")) {
                    p.addTag("twd_wisdom_told");
                    p.sendSystemMessage(Component.literal("✦ Somewhere close, something white is glowing. The Temple of Wisdom stands at X " + WISDOM.getX() + "  Z " + WISDOM.getZ()
                            + " - follow the white bridge south of this island.").withStyle(ChatFormatting.WHITE));
                }
                int pcx = Math.floorDiv(p.getBlockX(), CELL), pcz = Math.floorDiv(p.getBlockZ(), CELL);
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                    Site s = site(end.getSeed(), pcx + dx, pcz + dz);
                    if (s == null) continue;
                    long ddx = p.getBlockX() - s.pos().getX(), ddz = p.getBlockZ() - s.pos().getZ(), d2 = ddx * ddx + ddz * ddz;
                    if (!built(end, s.key()) && d2 < 230L * 230L) { markBuilt(end, s.key()); JOBS.add(plan(end, s.type(), s.pos()).job()); }
                    String tag = "end_" + s.key();
                    if (built(end, s.key()) && d2 < 60L * 60L && !p.getTags().contains(tag)) {
                        p.addTag(tag);
                        com.kiyamet.skill.SkillManager.addXp(p, com.kiyamet.skill.Skill.EXPLORATION, 1500);
                        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(s.type().title.toUpperCase()).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)));
                        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(s.type().hint).withStyle(ChatFormatting.GRAY)));
                        end.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 0.8f, 0.6f);
                    }
                }
            }
        });
        CommandRegistrationCallback.EVENT.register((d, ctx, env) -> {
            var build = Commands.literal("endbuild");
            var find = Commands.literal("endfind");
            for (Type t : Type.values()) {
                // testing: build this structure right here (console or creative)
                build.then(Commands.literal(t.id).executes(c -> {
                    if (c.getSource().getPlayer() != null && !c.getSource().getPlayer().isCreative()) return 0;
                    BlockPos at = BlockPos.containing(c.getSource().getPosition());
                    JOBS.add(plan(c.getSource().getLevel(), t, at).job());
                    c.getSource().sendSuccess(() -> Component.literal("Building " + t.title + " at " + at.toShortString()), true);
                    return 1;
                }));
                // creative: teleport to the nearest one of this kind in the End
                find.then(Commands.literal(t.id).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (!p.isCreative()) return 0;
                    ServerLevel end = c.getSource().getServer().getLevel(Level.END);
                    if (end == null) return 0;
                    Site best = null; long bd = Long.MAX_VALUE;
                    int pcx = Math.floorDiv(p.getBlockX(), CELL), pcz = Math.floorDiv(p.getBlockZ(), CELL);
                    for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) {
                        Site s = site(end.getSeed(), pcx + dx, pcz + dz);
                        if (s == null || s.type() != t) continue;
                        long ex = s.pos().getX() - p.getBlockX(), ez = s.pos().getZ() - p.getBlockZ();
                        if (ex * ex + ez * ez < bd) { bd = ex * ex + ez * ez; best = s; }
                    }
                    if (best == null) return 0;
                    p.teleportTo(end, best.pos().getX() + 0.5, best.pos().getY() + 30, best.pos().getZ() + 0.5, java.util.Set.of(), 0, 0, true);
                    return 1;
                }));
            }
            d.register(Commands.literal("twd").then(build).then(find)
                    .then(Commands.literal("buildwisdom").executes(c -> {
                        if (c.getSource().getPlayer() != null && !c.getSource().getPlayer().isCreative()) return 0;
                        BlockPos at = BlockPos.containing(c.getSource().getPosition());
                        JOBS.add(0, WisdomTemple.plan(c.getSource().getLevel(), at).job());
                        c.getSource().sendSuccess(() -> Component.literal("Building the Temple of Wisdom at " + at.toShortString()), true);
                        return 1;
                    })));
        });
    }

    private EndStructures() {}
}
