package com.kiyamet.element;

import com.kiyamet.Kiyamet;
import com.kiyamet.boss.BossEntity;
import com.kiyamet.boss.BossKind;
import com.kiyamet.boss.ModBosses;
import com.mojang.serialization.Codec;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;

/**
 * One temple per element, somewhere within 50,000 blocks; one sage per element, closer in.
 * Both are built the first time a player comes near. The world remembers what it already built.
 */
public final class Temples {
    public static final AttachmentType<List<String>> BUILT = AttachmentRegistry.create(Kiyamet.id("temples_built"), b -> b.initializer(List::of).persistent(Codec.STRING.listOf()));
    public static final AttachmentType<List<String>> MAPPED = AttachmentRegistry.create(Kiyamet.id("sage_maps"), b -> b.initializer(List::of).persistent(Codec.STRING.listOf()).copyOnDeath());
    static final List<Job> JOBS = new ArrayList<>();

    static long seed(ServerLevel l) { return l.getSeed(); }

    public static BlockPos templePos(ServerLevel l, Element e) {
        Random r = new Random(seed(l) * 31 + e.ordinal() * 7919L);
        double a = Math.toRadians(e.ordinal() * (360.0 / 14) + r.nextDouble() * 18);
        int d = 4000 + ((e.ordinal() * 5 + 3) % 14) * 2900 + r.nextInt(1200);     // 4k .. ~43k
        return new BlockPos((int) (Math.cos(a) * d), 0, (int) (Math.sin(a) * d));
    }

    public static BlockPos sagePos(ServerLevel l, Element e) {
        Random r = new Random(seed(l) * 17 + e.ordinal() * 104729L);
        double a = Math.toRadians(e.ordinal() * (360.0 / 14) + 40 + r.nextDouble() * 30);
        int d = 900 + ((e.ordinal() * 3) % 14) * 450 + r.nextInt(400);           // 0.9k .. 7.5k
        return new BlockPos((int) (Math.cos(a) * d), 0, (int) (Math.sin(a) * d));
    }

    public static List<String> mapped(ServerPlayer p) { return p.getAttachedOrElse(MAPPED, List.of()); }
    public static void markMapped(ServerPlayer p, Element e) {
        List<String> m = new ArrayList<>(mapped(p));
        if (!m.contains(e.id)) m.add(e.id);
        p.setAttached(MAPPED, List.copyOf(m));
    }

    static ItemStack map(ServerLevel l, BlockPos at, String title, ChatFormatting color, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> mark, byte scale) {
        ItemStack s = MapItem.create(l, at.getX(), at.getZ(), scale, true, true);
        MapItem.renderBiomePreviewMap(l, s);
        net.minecraft.world.level.saveddata.maps.MapItemSavedData.addTargetDecoration(s, at, "+", mark);
        s.set(DataComponents.ITEM_NAME, Component.literal(title).withStyle(color, ChatFormatting.BOLD));
        s.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("X: " + at.getX() + "   Z: " + at.getZ()).withStyle(ChatFormatting.GRAY))));
        return s;
    }

    public static ItemStack sageMap(ServerLevel l, Element e) { return map(l, sagePos(l, e), "Map to the " + e.title + " Sage", e.fmt, MapDecorationTypes.TARGET_X, (byte) 1); }
    public static ItemStack templeMap(ServerLevel l, Element e) { return map(l, templePos(l, e), "Map of the " + e.title + " Temple", e.fmt, MapDecorationTypes.RED_X, (byte) 2); }

    static boolean built(ServerLevel l, String key) { return l.getAttachedOrElse(BUILT, List.of()).contains(key); }
    static void markBuilt(ServerLevel l, String key) { List<String> b = new ArrayList<>(l.getAttachedOrElse(BUILT, List.of())); b.add(key); l.setAttached(BUILT, List.copyOf(b)); }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerLevel ow = server.overworld();
            // run queued building work (spread over ticks)
            if (!JOBS.isEmpty()) {
                Job j = JOBS.get(0);
                if (j.step(ow, 30000)) JOBS.remove(0);
            }
            if (server.getTickCount() % 40 != 0) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.level() != ow) continue;
                for (Element e : Element.values()) {
                    BlockPos sp = sagePos(ow, e), tp = templePos(ow, e);
                    if (!built(ow, "sage:" + e.id) && horiz(p.blockPosition(), sp) < 72 * 72) { markBuilt(ow, "sage:" + e.id); buildCamp(ow, e, sp); }
                    if (!built(ow, "temple:" + e.id) && horiz(p.blockPosition(), tp) < 140 * 140) { markBuilt(ow, "temple:" + e.id); JOBS.add(new TempleBuilder(ow, e, tp).job()); }
                    if (built(ow, "temple:" + e.id) && horiz(p.blockPosition(), tp) < 30 * 30 && !p.getTags().contains("temple_" + e.id)) {
                        p.addTag("temple_" + e.id);
                        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("TEMPLE OF " + e.title.toUpperCase()).withStyle(e.fmt, ChatFormatting.BOLD)));
                        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Its lord waits at the very bottom").withStyle(ChatFormatting.GRAY)));
                        ow.playSound(null, p.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 0.6f, 0.7f);
                    }
                }
            }
        });
        // creative helpers: /twd temple <element> (teleport), /twd sage <element>
        CommandRegistrationCallback.EVENT.register((d, ctx, env) -> {
            var temple = Commands.literal("temple");
            var sage = Commands.literal("sage");
            for (Element e : Element.values()) {
                temple.then(Commands.literal(e.id).executes(c -> tp(c.getSource().getPlayerOrException(), templePos(c.getSource().getLevel(), e), 30)));
                sage.then(Commands.literal(e.id).executes(c -> tp(c.getSource().getPlayerOrException(), sagePos(c.getSource().getLevel(), e), 6)));
            }
            d.register(Commands.literal("twd").then(temple).then(sage));
        });
    }

    static int tp(ServerPlayer p, BlockPos at, int back) {
        if (!p.isCreative()) { p.sendSystemMessage(Component.literal("Creative mode only - follow the maps!").withStyle(ChatFormatting.RED)); return 0; }
        ServerLevel l = p.level();
        l.getChunk(at.getX() >> 4, (at.getZ() + back) >> 4);
        int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ() + back);
        p.teleportTo(l, at.getX() + 0.5, y + 1, at.getZ() + back + 0.5, java.util.Set.of(), 180, 0, true);
        return 1;
    }

    static long horiz(BlockPos a, BlockPos b) { long dx = a.getX() - b.getX(), dz = a.getZ() - b.getZ(); return dx * dx + dz * dz; }

    /** the sage's little camp: a tent in the element's colour, a campfire, a lantern */
    static void buildCamp(ServerLevel l, Element e, BlockPos at) {
        l.getChunk(at.getX() >> 4, at.getZ() >> 4);
        int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
        BlockPos o = new BlockPos(at.getX(), y, at.getZ());
        BlockState wool = TempleBuilder.wool(e);
        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
            for (int h = 0; h < 6; h++) l.setBlock(o.offset(x, h, z), Blocks.AIR.defaultBlockState(), 2);
            l.setBlock(o.offset(x, -1, z), (Math.abs(x) + Math.abs(z)) % 5 == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.PODZOL.defaultBlockState(), 2);
        }
        for (int z = -3; z <= 0; z++) for (int x = -3; x <= 3; x++) {
            int h = 3 - Math.abs(x);
            if (h >= 0) l.setBlock(o.offset(x, h, z), wool, 2);
            if (z == -3 && h >= 0) for (int k = 0; k < h; k++) l.setBlock(o.offset(x, k, z), wool, 2);
        }
        for (int k = 0; k < 4; k++) { l.setBlock(o.offset(-4, k, 1), Blocks.OAK_FENCE.defaultBlockState(), 2); l.setBlock(o.offset(4, k, 1), Blocks.OAK_FENCE.defaultBlockState(), 2); }
        l.setBlock(o.offset(-4, 4, 1), Blocks.LANTERN.defaultBlockState(), 3);
        l.setBlock(o.offset(4, 4, 1), Blocks.LANTERN.defaultBlockState(), 3);
        l.setBlock(o.offset(0, 0, 3), Blocks.CAMPFIRE.defaultBlockState(), 3);
        l.setBlock(o.offset(2, 0, -2), Blocks.BARREL.defaultBlockState(), 3);
        l.setBlock(o.offset(-2, 0, -2), Blocks.LECTERN.defaultBlockState(), 3);
        for (int x : new int[]{-2, 2}) l.setBlock(o.offset(x, 0, 4), Blocks.OAK_LOG.defaultBlockState(), 3);
        Sage s = ElementMobs.SAGES.get(e).create(l, EntitySpawnReason.STRUCTURE);
        if (s != null) {
            s.snapTo(o.getX() + 0.5, o.getY(), o.getZ() + 1.5, 180, 0);
            s.setCustomName(Component.literal(e.title + " Sage").withStyle(e.fmt));
            s.setCustomNameVisible(true);
            l.addFreshEntity(s);
        }
    }

    /** a long job done a slice per tick */
    public interface Job { boolean step(ServerLevel l, int budget); }

    private Temples() {}
}
