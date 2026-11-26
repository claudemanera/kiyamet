package com.kiyamet.vehicle;

import com.kiyamet.Kiyamet;
import com.kiyamet.npc.HumanEntity;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import com.mojang.brigadier.arguments.IntegerArgumentType;

/**
 * 1.6 autopilot: a companion drives your vehicle to a spot. The Autopilot key (Y) sends your newest Xaero's map waypoint;
 * without Xaero's, use /twd drive <x> <z>.
 */
public final class Autopilot {
    public record Drive(int x, int z, String name) implements CustomPacketPayload {
        public static final Type<Drive> TYPE = new Type<>(Kiyamet.id("autopilot"));
        public static final StreamCodec<ByteBuf, Drive> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Drive::x, ByteBufCodecs.VAR_INT, Drive::z, ByteBufCodecs.STRING_UTF8, Drive::name, Drive::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void init() {
        PayloadTypeRegistry.playC2S().register(Drive.TYPE, Drive.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Drive.TYPE, (d, ctx) -> ctx.server().execute(() -> {
            if (d.name().equals("?")) { ctx.player().displayClientMessage(Component.literal("No Xaero's waypoint found. Make one on the map, or use /twd drive <x> <z>.").withStyle(ChatFormatting.YELLOW), false); return; }
            go(ctx.player(), d.x(), d.z(), d.name());
        }));
        CommandRegistrationCallback.EVENT.register((disp, ctx, env) -> disp.register(Commands.literal("twd").then(Commands.literal("drive")
                .then(Commands.argument("x", IntegerArgumentType.integer()).then(Commands.argument("z", IntegerArgumentType.integer()).executes(c -> {
                    go(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "x"), IntegerArgumentType.getInteger(c, "z"), "");
                    return 1;
                }))))));
    }

    static void go(ServerPlayer p, int x, int z, String name) {
        ServerLevel level = p.level();
        VehicleEntity v = p.getVehicle() instanceof VehicleEntity ve ? ve
                : level.getEntitiesOfClass(VehicleEntity.class, p.getBoundingBox().inflate(10), e -> e.getFirstPassenger() == null || e.getFirstPassenger() == p).stream().findFirst().orElse(null);
        if (v == null) { p.displayClientMessage(Component.literal("Get in a vehicle (or stand next to one) first.").withStyle(ChatFormatting.RED), false); return; }
        HumanEntity driver = v.getFirstPassenger() instanceof HumanEntity h && h.isOwnedBy(p) ? h : null;
        if (driver == null) for (var e : v.getPassengers()) if (e instanceof HumanEntity h && h.isOwnedBy(p)) { driver = h; break; }
        if (driver == null) driver = level.getEntitiesOfClass(HumanEntity.class, p.getBoundingBox().inflate(24), h -> h.isOwnedBy(p) && !h.isAway()).stream()
                .min(java.util.Comparator.comparingDouble(h -> h.distanceToSqr(p))).orElse(null);
        if (driver == null) { p.displayClientMessage(Component.literal("You need a companion nearby to drive.").withStyle(ChatFormatting.RED), false); return; }
        if (!name.isEmpty()) p.sendSystemMessage(Component.literal("Waypoint: " + name).withStyle(ChatFormatting.GRAY));
        v.driveTo(p, driver, new Vec3(x + 0.5, v.getY(), z + 0.5));
    }

    private Autopilot() {}
}
