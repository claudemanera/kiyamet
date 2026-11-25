package com.kiyamet.net;

import com.kiyamet.Kiyamet;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: a mod key was pressed. */
public record KeyAction(int action) implements CustomPacketPayload {
    public static final int SKILLS = 0, POWERS = 1, COMPANIONS = 2, GAUNTLET_USE = 3, GAUNTLET_CYCLE = 4, CAMOUFLAGE = 5, NIGHT_VISION = 6, SHADOW_STEP = 7;
    public static final Type<KeyAction> TYPE = new Type<>(Kiyamet.id("key_action"));
    public static final StreamCodec<ByteBuf, KeyAction> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, KeyAction::action, KeyAction::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
