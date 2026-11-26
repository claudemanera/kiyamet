package com.kiyamet.season;

import com.kiyamet.Kiyamet;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * TWD 1.5 seasons and body temperature.
 * A year is 4 seasons of 7 days. Winter: snow piles up to 1.5 blocks, crops barely grow and rot in the open,
 * walkers slow down in the cold. Summer: heat waves, dry fields wither. Spring melts the snow, autumn ripens the harvest.
 * Your body temperature follows the weather, the biome, day and night, shade, water, fire and your clothes.
 * Too cold: you freeze (frost vignette, slowness, damage). Too hot: hunger, blurred vision, poison, even burning.
 * The temperature is written in chat whenever it changes, coloured from navy (freezing) to red (burning).
 */
public final class Seasons {
    public enum Season {
        SPRING("Spring", ChatFormatting.GREEN, 13), SUMMER("Summer", ChatFormatting.GOLD, 29), AUTUMN("Autumn", ChatFormatting.RED, 11), WINTER("Winter", ChatFormatting.AQUA, -7);
        public final String title; public final ChatFormatting color; public final float base;
        Season(String t, ChatFormatting c, float b) { title = t; color = c; base = b; }
    }

    public static final int DAYS = 7;
    public static final AttachmentType<Float> BODY = AttachmentRegistry.create(Kiyamet.id("body_temp"), b -> b.initializer(() -> 20f).persistent(Codec.FLOAT));
    private static final java.util.Map<java.util.UUID, int[]> SHOWN = new java.util.HashMap<>();   // last shown temp, last state, tick
    private static int lastSeason = -1;
    private static int forced = -1;

    public static Season season(Level level) {
        if (forced >= 0) return Season.values()[forced];
        if (level.getServer() == null) return Season.SPRING;
        return Calendar.seasonOf(Calendar.date(level));
    }

    public static int dayOfSeason(Level level) {
        java.time.LocalDate d = Calendar.date(level);
        java.time.LocalDate first = java.time.LocalDate.of(d.getMonthValue() == 12 ? d.getYear() : d.getMonthValue() <= 2 ? d.getYear() - 1 : d.getYear(), switch (Calendar.seasonOf(d)) { case SPRING -> 3; case SUMMER -> 6; case AUTUMN -> 9; case WINTER -> 12; }, 1);
        return (int) java.time.temporal.ChronoUnit.DAYS.between(first, d) + 1;
    }
    private static long lastDay = Long.MIN_VALUE;

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int t = server.getTickCount();
            ServerLevel ow = server.overworld();
            Season s = season(ow);
            if (t % 100 == 0 && s.ordinal() != lastSeason) { if (lastSeason >= 0) announce(server, s); lastSeason = s.ordinal(); weather(ow, s); }
            if (t % 20 == 0) {
                long day = Calendar.dayIndex(ow);
                if (day != lastDay) { boolean first = lastDay == Long.MIN_VALUE; lastDay = day; if (!first || ow.getDayTime() % 24000 < 200) SeasonEvents.newDay(server, ow, s); }
            }
            SeasonEvents.tick(server, ow, s, t);
            if (t % 20 != 0) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.isSpectator()) continue;
                ServerLevel level = p.level();
                if (level == ow) { snow(level, p, s); if (t % 40 == 0) crops(level, p, s); }
                temperature(level, p, s);
            }
        });
        CommandRegistrationCallback.EVENT.register((d, ctx, env) -> d.register(Commands.literal("twd")
                .then(Commands.literal("season").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Season s = season(p.level());
                    p.sendSystemMessage(Component.literal(Calendar.format(Calendar.date(p.level())) + " · " + s.title + ", day " + dayOfSeason(p.level()) + ". ").withStyle(s.color, ChatFormatting.BOLD)
                            .append(tempText(p.getAttachedOrElse(BODY, 20f))));
                    return 1;
                }).then(Commands.argument("set", com.mojang.brigadier.arguments.IntegerArgumentType.integer(-1, 3)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (!p.isCreative()) return 0;
                    forced = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "set");
                    p.sendSystemMessage(Component.literal(forced < 0 ? "Seasons follow the calendar again." : "Season forced: " + Season.values()[forced].title));
                    return 1;
                })))));
    }

    private static void announce(MinecraftServer server, Season s) {
        String sub = switch (s) {
            case SPRING -> "The snow melts. Plant your fields.";
            case SUMMER -> "Heat waves. Stay in the shade, keep water close.";
            case AUTUMN -> "Harvest time. Prepare for the cold.";
            case WINTER -> "Snow, cold, and empty bellies. Stay warm.";
        };
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(20, 80, 30));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(sub).withStyle(ChatFormatting.GRAY)));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(s.title.toUpperCase()).withStyle(s.color, ChatFormatting.BOLD)));
            p.sendSystemMessage(Component.literal("❖ " + s.title + " has come. " + sub).withStyle(s.color));
        }
    }

    private static void weather(ServerLevel ow, Season s) {
        if (s == Season.WINTER) ow.setWeatherParameters(0, 12000 + ow.random.nextInt(12000), true, false);
        if (s == Season.SUMMER) ow.setWeatherParameters(24000 * 3, 0, false, false);
    }

    // ------------------------------------------------------------------ snow

    static boolean snowing(ServerLevel l, Season s) { return s == Season.WINTER && l.isRaining(); }

    private static void snow(ServerLevel level, ServerPlayer p, Season s) {
        RandomSource r = level.random;
        if (snowing(level, s)) level.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 10, p.getZ(), 80, 14, 5, 14, 0.01);
        int tries = s == Season.WINTER ? (level.isRaining() ? 40 : 3) : s == Season.SPRING ? 30 : s == Season.SUMMER ? 50 : 0;
        for (int i = 0; i < tries; i++) {
            BlockPos col = p.blockPosition().offset(r.nextInt(81) - 40, 0, r.nextInt(81) - 40);
            if (!level.isLoaded(col)) continue;
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, col);   // first free block above the surface
            BlockState below = level.getBlockState(top.below());
            if (s == Season.WINTER) addSnow(level, top, below);
            else meltSnow(level, top, below);
        }
    }

    /** Snow layers pile up to 12 (one full snow block + 4 layers = 1.5 blocks). */
    private static void addSnow(ServerLevel level, BlockPos top, BlockState below) {
        if (!level.canSeeSky(top) || !level.getBlockState(top).isAir()) return;
        if (below.is(Blocks.SNOW)) {
            int layers = below.getValue(SnowLayerBlock.LAYERS);
            BlockPos at = top.below();
            boolean onSnowBlock = level.getBlockState(at.below()).is(Blocks.SNOW_BLOCK);
            if (layers < 8) level.setBlock(at, below.setValue(SnowLayerBlock.LAYERS, layers + 1), 3);
            else if (!onSnowBlock) level.setBlock(at, Blocks.SNOW_BLOCK.defaultBlockState(), 3);
            return;
        }
        if (below.is(Blocks.SNOW_BLOCK)) {
            if (!level.getBlockState(top.below(2)).is(Blocks.SNOW_BLOCK)) level.setBlock(top, Blocks.SNOW.defaultBlockState(), 3);   // max 1 block + layers
            return;
        }
        if (below.getBlock() instanceof CropBlock || below.getBlock() instanceof FarmBlock || below.getBlock() instanceof LeavesBlock || below.getFluidState().isSource()) return;
        if (Blocks.SNOW.defaultBlockState().canSurvive(level, top)) level.setBlock(top, Blocks.SNOW.defaultBlockState(), 3);
    }

    private static void meltSnow(ServerLevel level, BlockPos top, BlockState below) {
        BlockPos at = top.below();
        if (below.is(Blocks.SNOW)) {
            int layers = below.getValue(SnowLayerBlock.LAYERS);
            level.setBlock(at, layers > 1 ? below.setValue(SnowLayerBlock.LAYERS, layers - 1) : Blocks.AIR.defaultBlockState(), 3);
        } else if (below.is(Blocks.SNOW_BLOCK) && level.getBlockState(at.below()).isSolid()) {
            level.setBlock(at, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 7), 3);
        }
    }

    // ------------------------------------------------------------------ crops

    private static void crops(ServerLevel level, ServerPlayer p, Season s) {
        RandomSource r = level.random;
        for (int i = 0; i < 40; i++) {
            BlockPos pos = p.blockPosition().offset(r.nextInt(49) - 24, r.nextInt(9) - 4, r.nextInt(49) - 24);
            BlockState st = level.getBlockState(pos);
            if (!(st.getBlock() instanceof CropBlock crop)) continue;
            boolean sheltered = !level.canSeeSky(pos) || warmNear(level, pos, 4);   // greenhouses and heated fields are safe
            int age = crop.getAge(st);
            switch (s) {
                case WINTER -> {
                    if (sheltered) break;
                    if (r.nextFloat() < 0.07f) {           // rots in the frozen field
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.01);
                    } else if (age > 0 && r.nextFloat() < 0.6f) level.setBlock(pos, crop.getStateForAge(age - 1), 2);   // almost no growth
                }
                case SUMMER -> {
                    BlockState soil = level.getBlockState(pos.below());
                    boolean dry = soil.hasProperty(FarmBlock.MOISTURE) && soil.getValue(FarmBlock.MOISTURE) == 0;
                    float heat = level.getBiome(pos).value().getBaseTemperature() > 1.5f ? 2f : 1f;
                    if (dry && !sheltered && r.nextFloat() < 0.08f * heat) {   // dries out
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        level.sendParticles(ParticleTypes.WHITE_ASH, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.01);
                    }
                }
                case AUTUMN -> { if (!crop.isMaxAge(st) && r.nextFloat() < 0.3f) level.setBlock(pos, crop.getStateForAge(age + 1), 2); }
                case SPRING -> { if (!crop.isMaxAge(st) && r.nextFloat() < 0.15f) level.setBlock(pos, crop.getStateForAge(age + 1), 2); }
            }
            float boost = SeasonEvents.cropBoost();
            st = level.getBlockState(pos);
            if (boost > 0 && st.getBlock() instanceof CropBlock c2 && !c2.isMaxAge(st) && r.nextFloat() < boost) level.setBlock(pos, c2.getStateForAge(c2.getAge(st) + 1), 2);
        }
    }

    // ------------------------------------------------------------------ temperature

    public static boolean heatBlock(BlockState b) {
        if (b.is(Blocks.FIRE) || b.is(Blocks.SOUL_FIRE) || b.is(Blocks.LAVA) || b.is(Blocks.MAGMA_BLOCK)) return true;
        if ((b.getBlock() instanceof CampfireBlock || b.getBlock() instanceof AbstractFurnaceBlock) && b.hasProperty(BlockStateProperties.LIT)) return b.getValue(BlockStateProperties.LIT);
        return b.getBlock() instanceof com.kiyamet.defense.GeneratorBlock && b.getValue(BlockStateProperties.LIT);
    }

    public static boolean warmNear(ServerLevel level, BlockPos c, int r) {
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-r, -2, -r), c.offset(r, 2, r))) if (heatBlock(level.getBlockState(q))) return true;
        return false;
    }

    static boolean coldNear(ServerLevel level, BlockPos c) {
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-2, -1, -2), c.offset(2, 1, 2))) {
            BlockState b = level.getBlockState(q);
            if (b.is(Blocks.ICE) || b.is(Blocks.PACKED_ICE) || b.is(Blocks.BLUE_ICE)) return true;
        }
        return false;
    }

    /** The air temperature where the player stands, in °C. */
    public static float ambient(ServerLevel level, ServerPlayer p, Season s) {
        BlockPos pos = p.blockPosition();
        float t = level.dimension() == Level.NETHER ? 45 : level.dimension() == Level.END ? 5 : s.base + SeasonEvents.tempShift();
        if (level.dimension() == Level.OVERWORLD) {
            float biome = level.getBiome(pos).value().getBaseTemperature();
            t += (biome - 0.8f) * 18;
            long tod = level.getDayTime() % 24000;
            boolean day = tod < 12500 || tod > 23500;
            t += day ? (tod > 4000 && tod < 9000 ? 6 : 3) : -7;
            if (pos.getY() > 95) t -= (pos.getY() - 95) / 8f;
            if (level.isRaining()) t -= s == Season.WINTER ? 5 : 3;
            boolean sky = level.canSeeSky(pos.above());
            if (!sky) t = t + (17 - t) * 0.5f;                 // indoors / underground evens things out
            else if (day && s == Season.SUMMER) t += 4;        // direct summer sun
        }
        if (p.isInWater()) t -= 9;
        if (warmNear(level, pos, 4)) t += s == Season.SUMMER ? 6 : 16;
        if (coldNear(level, pos)) t -= 10;
        // clothes: leather keeps you warm, metal cooks you in summer
        int leather = 0, metal = 0;
        for (EquipmentSlot es : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack a = p.getItemBySlot(es);
            if (a.is(Items.LEATHER_HELMET) || a.is(Items.LEATHER_CHESTPLATE) || a.is(Items.LEATHER_LEGGINGS) || a.is(Items.LEATHER_BOOTS)) leather++;
            else if (!a.isEmpty()) metal++;
        }
        float fur = com.kiyamet.farm.Fur.warmth(p) + com.kiyamet.wild.WildAnimal.petWarmth(p);
        if (t < 15) t += leather * 3.5f + metal * 1f + fur;
        else if (t > 24) t += leather * 1.5f + metal * 1.2f + com.kiyamet.farm.Fur.pieces(p) * 2.5f;   // fur in summer = sweating
        else t += fur * 0.3f;
        if (p.getVehicle() instanceof com.kiyamet.vehicle.VehicleEntity v && v.kind != com.kiyamet.vehicle.VehicleKind.ATV && v.kind != com.kiyamet.vehicle.VehicleKind.MOTORCYCLE)
            t = t + (20 - t) * 0.4f;                           // cars have heaters and shade
        return t;
    }

    private static void temperature(ServerLevel level, ServerPlayer p, Season s) {
        float body = p.getAttachedOrElse(BODY, 20f);
        float amb = ambient(level, p, s);
        if (p.isCreative()) amb = 20;
        body += (amb - body) * 0.08f;
        p.setAttached(BODY, body);
        int state = state(body);
        RandomSource r = level.random;
        switch (state) {
            case -3 -> {   // freezing
                p.setTicksFrozen(Math.min(p.getTicksRequiredToFreeze() + 20, p.getTicksFrozen() + 60));
                p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, true, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 40, 0, true, false, true));
                p.causeFoodExhaustion(0.6f);
            }
            case -2 -> {   // cold
                p.setTicksFrozen(Math.min(p.getTicksRequiredToFreeze() - 10, p.getTicksFrozen() + 30));
                p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, true, false, true));
                p.causeFoodExhaustion(0.3f);
            }
            case 2 -> p.causeFoodExhaustion(0.25f);   // warm: thirsty work
            case 3 -> {    // hot
                p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 0, true, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false, true));
            }
            case 4 -> {    // heatstroke
                p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 1, true, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 160, 0, true, false, true));
                if (r.nextInt(5) == 0) p.addEffect(new MobEffectInstance(MobEffects.POISON, 50, 0, true, false, true));
                if (body >= 48 && level.canSeeSky(p.blockPosition().above()) && level.isBrightOutside() && r.nextInt(4) == 0) p.igniteForSeconds(2);
            }
            default -> {}
        }
        // chat report: when the temperature moves 3 °C or the state changes (at most every 15 s)
        int[] last = SHOWN.computeIfAbsent(p.getUUID(), k -> new int[]{Integer.MIN_VALUE, 99, 0});
        int shown = Math.round(body);
        int now = p.level().getServer().getTickCount();
        if ((state != last[1] || Math.abs(shown - last[0]) >= 3) && now - last[2] >= 300) {
            last[0] = shown; last[1] = state; last[2] = now;
            p.sendSystemMessage(tempText(body));
        }
    }

    public static int state(float t) {
        if (t < -2) return -3;
        if (t < 5) return -2;
        if (t < 30) return t < 10 ? -1 : 0;
        if (t < 38) return 2;
        if (t < 44) return 3;
        return 4;
    }

    public static ChatFormatting color(float t) {
        if (t < -2) return ChatFormatting.DARK_BLUE;     // navy
        if (t < 3) return ChatFormatting.BLUE;
        if (t < 8) return ChatFormatting.WHITE;
        if (t < 14) return ChatFormatting.AQUA;          // light blue
        if (t < 26) return ChatFormatting.GREEN;
        if (t < 32) return ChatFormatting.YELLOW;
        if (t < 40) return ChatFormatting.GOLD;          // orange
        if (t < 46) return ChatFormatting.RED;
        return ChatFormatting.DARK_RED;
    }

    public static Component tempText(float t) {
        String label = switch (state(t)) {
            case -3 -> "Freezing! Find fire or shelter"; case -2 -> "Cold"; case -1 -> "Chilly"; case 2 -> "Warm"; case 3 -> "Hot - get into the shade";
            case 4 -> "Heatstroke! Water, shade, now"; default -> "Comfortable";
        };
        return Component.literal("🌡 " + Math.round(t) + "°C  ").withStyle(color(t), ChatFormatting.BOLD)
                .append(Component.literal(label).withStyle(color(t)));
    }

    private Seasons() {}
}
