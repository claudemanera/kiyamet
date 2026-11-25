package com.kiyamet.defense;

import com.kiyamet.Kiyamet;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

/** Remembers which powered devices were switched off in a generator's panel (stored on the chunk). */
public final class DeviceSwitch {
    public static final AttachmentType<List<Long>> OFF = AttachmentRegistry.create(Kiyamet.id("devices_off"),
            b -> b.initializer(List::of).persistent(Codec.LONG.listOf()));

    public static boolean isOff(ServerLevel level, BlockPos pos) {
        LevelChunk c = level.getChunkAt(pos);
        List<Long> l = c.getAttachedOrElse(OFF, List.of());
        return !l.isEmpty() && l.contains(pos.asLong());
    }

    public static void set(ServerLevel level, BlockPos pos, boolean on) {
        LevelChunk c = level.getChunkAt(pos);
        List<Long> l = new ArrayList<>(c.getAttachedOrElse(OFF, List.of()));
        boolean changed = on ? l.remove((Long) pos.asLong()) : !l.contains(pos.asLong()) && l.add(pos.asLong());
        if (changed) c.setAttached(OFF, List.copyOf(l));
    }

    public static void init() {}

    private DeviceSwitch() {}
}
