package com.kiyamet.season;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 1.7 daily date line in chat + a weather event for the day, different in every season:
 * winter blizzards and freezing lakes, spring rains and blooming meadows, summer heat waves, droughts and storms,
 * autumn fog, falling leaves, storms and the harvest moon.
 */
public final class SeasonEvents {
    public enum Event {
        NONE("", ""), BLIZZARD("Blizzard", "Heavy snow and wind. Stay inside - the cold bites twice as hard."), DEEP_FREEZE("Deep Freeze", "Lakes and rivers freeze over."),
        SPRING_RAIN("Spring Rain", "Steady rain all day. Crops grow faster."), BLOOM("Bloom", "Flowers everywhere. Bees and peace - for a day."),
        HEAT_WAVE("Heat Wave", "Scorching heat. Drink, find shade, stay out of the noon sun."), DROUGHT("Drought", "Not a cloud in the sky. Fields dry out."),
        SUMMER_STORM("Summer Storm", "Thunder and lightning. Walkers are restless."), FOG("Autumn Fog", "Thick fog. You won't see them coming."),
        LEAF_FALL("Leaf Fall", "Leaves come down in gold and red."), AUTUMN_STORM("Autumn Storm", "Cold rain and wind."), HARVEST("Harvest Moon", "Crops ripen faster today.");
        public final String title, desc;
        Event(String t, String d) { title = t; desc = d; }
    }

    public static Event today = Event.NONE;

    public static void newDay(MinecraftServer server, ServerLevel ow, Seasons.Season s) {
        RandomSource r = ow.random;
        Event[] pool = switch (s) {
            case WINTER -> new Event[]{Event.NONE, Event.NONE, Event.BLIZZARD, Event.DEEP_FREEZE, Event.BLIZZARD};
            case SPRING -> new Event[]{Event.NONE, Event.SPRING_RAIN, Event.SPRING_RAIN, Event.BLOOM};
            case SUMMER -> new Event[]{Event.NONE, Event.NONE, Event.HEAT_WAVE, Event.DROUGHT, Event.SUMMER_STORM};
            case AUTUMN -> new Event[]{Event.NONE, Event.FOG, Event.LEAF_FALL, Event.AUTUMN_STORM, Event.HARVEST};
        };
        today = pool[r.nextInt(pool.length)];
        switch (today) {
            case BLIZZARD, AUTUMN_STORM, SPRING_RAIN -> ow.setWeatherParameters(0, 24000, true, false);
            case SUMMER_STORM -> ow.setWeatherParameters(0, 12000, true, true);
            case DROUGHT, HEAT_WAVE, BLOOM -> ow.setWeatherParameters(24000, 0, false, false);
            default -> {}
        }
        var d = Calendar.date(ow);
        Component line = Component.literal("📅 " + Calendar.format(d)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)
                .append(Component.literal("  ·  " + s.title + ", day " + Seasons.dayOfSeason(ow)).withStyle(s.color))
                .append(Component.literal("  ·  Day " + (Calendar.dayIndex(ow) + 1) + " of the apocalypse").withStyle(ChatFormatting.DARK_GRAY));
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(line);
            if (today != Event.NONE) p.sendSystemMessage(Component.literal("   ⚠ " + today.title + ": " + today.desc).withStyle(s.color));
        }
    }

    /** Extra degrees from today's event (used by the temperature model). */
    public static float tempShift() {
        return switch (today) { case BLIZZARD -> -9; case DEEP_FREEZE -> -5; case HEAT_WAVE -> 9; case DROUGHT -> 4; case FOG -> -2; case AUTUMN_STORM -> -3; default -> 0; };
    }

    public static float cropBoost() { return today == Event.SPRING_RAIN || today == Event.HARVEST ? 0.2f : today == Event.DROUGHT ? -0.15f : 0; }

    public static void tick(MinecraftServer server, ServerLevel ow, Seasons.Season s, int t) {
        if (today == Event.NONE || t % 20 != 0) return;
        RandomSource r = ow.random;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.level() != ow || p.isSpectator()) continue;
            boolean sky = ow.canSeeSky(p.blockPosition().above());
            switch (today) {
                case BLIZZARD -> {
                    ow.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 4, p.getZ(), 200, 10, 3, 10, 0.3);
                    if (sky) { p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, true, false)); if (r.nextInt(6) == 0) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, true, false)); }
                }
                case DEEP_FREEZE -> freeze(ow, p, r, 12);
                case BLOOM -> { if (r.nextInt(3) == 0) bloom(ow, p, r); }
                case HEAT_WAVE -> { if (sky && ow.isBrightOutside()) ow.sendParticles(ParticleTypes.WHITE_ASH, p.getX(), p.getY() + 2, p.getZ(), 10, 6, 2, 6, 0.01); }
                case FOG -> { ow.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 6, 8, 1, 8, 0.001); if (sky && r.nextInt(8) == 0) p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, true, false)); }
                case LEAF_FALL -> leaves(ow, p, r);
                case AUTUMN_STORM, SPRING_RAIN -> { if (today == Event.SPRING_RAIN && r.nextInt(4) == 0) bloom(ow, p, r); }
                default -> {}
            }
        }
    }

    static BlockPos surface(ServerLevel l, ServerPlayer p, RandomSource r, int rad) {
        BlockPos c = p.blockPosition().offset(r.nextInt(rad * 2 + 1) - rad, 0, r.nextInt(rad * 2 + 1) - rad);
        return l.isLoaded(c) ? l.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, c) : null;
    }

    static void freeze(ServerLevel l, ServerPlayer p, RandomSource r, int n) {
        for (int i = 0; i < n; i++) {
            BlockPos top = surface(l, p, r, 32);
            if (top == null) continue;
            BlockState w = l.getBlockState(top.below());
            if (w.is(Blocks.WATER) && w.getFluidState().isSource() && l.canSeeSky(top)) l.setBlockAndUpdate(top.below(), Blocks.ICE.defaultBlockState());
        }
    }

    static void bloom(ServerLevel l, ServerPlayer p, RandomSource r) {
        BlockPos top = surface(l, p, r, 24);
        if (top == null || !l.getBlockState(top).isAir() || !l.getBlockState(top.below()).is(Blocks.GRASS_BLOCK)) return;
        BlockState[] f = {Blocks.POPPY.defaultBlockState(), Blocks.DANDELION.defaultBlockState(), Blocks.CORNFLOWER.defaultBlockState(), Blocks.OXEYE_DAISY.defaultBlockState(), Blocks.ALLIUM.defaultBlockState()};
        l.setBlockAndUpdate(top, f[r.nextInt(f.length)]);
    }

    static void leaves(ServerLevel l, ServerPlayer p, RandomSource r) {
        for (int i = 0; i < 4; i++) {
            BlockPos c = p.blockPosition().offset(r.nextInt(25) - 12, r.nextInt(10), r.nextInt(25) - 12);
            if (l.getBlockState(c).is(net.minecraft.tags.BlockTags.LEAVES) && l.getBlockState(c.below()).isAir())
                l.sendParticles(r.nextBoolean() ? ParticleTypes.CHERRY_LEAVES : ParticleTypes.PALE_OAK_LEAVES, c.getX() + 0.5, c.getY() - 0.2, c.getZ() + 0.5, 3, 0.4, 0.1, 0.4, 0.01);
        }
    }

    private SeasonEvents() {}
}
