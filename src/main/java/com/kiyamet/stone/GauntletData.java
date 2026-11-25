package com.kiyamet.stone;

import com.kiyamet.Kiyamet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Stones socketed into the Stone Gauntlet and which one is selected for the active ability. */
public record GauntletData(List<String> stones, int selected) {
    public static final GauntletData EMPTY = new GauntletData(List.of(), 0);
    public static final Codec<GauntletData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().fieldOf("stones").forGetter(GauntletData::stones),
            Codec.INT.optionalFieldOf("selected", 0).forGetter(GauntletData::selected)).apply(i, GauntletData::new));
    public static final StreamCodec<ByteBuf, GauntletData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), GauntletData::stones,
            ByteBufCodecs.VAR_INT, GauntletData::selected, GauntletData::new);

    public static final DataComponentType<GauntletData> TYPE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Kiyamet.id("gauntlet"),
            DataComponentType.<GauntletData>builder().persistent(CODEC).networkSynchronized(STREAM_CODEC).build());

    public static void init() {}

    public GauntletData add(String id) { List<String> l = new ArrayList<>(stones); l.add(id); return new GauntletData(List.copyOf(l), l.size() - 1); }
    public GauntletData cycle() { return stones.isEmpty() ? this : new GauntletData(stones, (selected + 1) % stones.size()); }
    public StoneKind current() { return stones.isEmpty() ? null : StoneKind.byId(stones.get(Math.min(selected, stones.size() - 1))); }
}
