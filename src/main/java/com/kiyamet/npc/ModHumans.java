package com.kiyamet.npc;

import com.kiyamet.Kiyamet;
import com.kiyamet.entity.KZombie;
import com.kiyamet.entity.ModEntities;
import com.kiyamet.entity.ZombieKind;
import com.kiyamet.skill.SkillManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModHumans {
    private static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, Kiyamet.id("human"));
    public static final EntityType<HumanEntity> HUMAN = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
            EntityType.Builder.<HumanEntity>of(HumanEntity::new, MobCategory.CREATURE).sized(0.6f, 1.95f).eyeHeight(1.62f).clientTrackingRange(10).build(KEY));

    public static void init() {
        FabricDefaultAttributeRegistry.register(HUMAN, PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20).add(Attributes.ATTACK_DAMAGE, 3).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32).add(Attributes.ARMOR, 0).add(Attributes.ARMOR_TOUGHNESS, 0).add(Attributes.KNOCKBACK_RESISTANCE, 0));

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (source.getEntity() instanceof HumanEntity killer && entity.level() instanceof ServerLevel sl) killer.onKill(sl, entity);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int t = server.getTickCount();
            if (t % 40 == 0) for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.isSpectator()) continue;
                ServerLevel sl = p.level();
                for (HumanEntity h : sl.getEntitiesOfClass(HumanEntity.class, p.getBoundingBox().inflate(22), h -> h.faction() == Faction.COMMUNITY && !h.community().isEmpty()))
                    Communities.firstContact(sl, p, h);
            }
            if (t % 2400 == 1200) for (ServerPlayer p : server.getPlayerList().getPlayers()) events(p);
        });

        CommandRegistrationCallback.EVENT.register((d, ctx, env) -> d.register(Commands.literal("twd")
                .then(Commands.literal("spawn").then(Commands.argument("what", StringArgumentType.word()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (!p.isCreative()) { p.sendSystemMessage(Component.literal("Creative mode only.")); return 0; }
                    String w = StringArgumentType.getString(c, "what");
                    BlockPos at = p.blockPosition().offset(6, 0, 0);
                    switch (w) {
                        case "raiders" -> raiders(p.level(), at, power(p), false);
                        case "commander" -> raiders(p.level(), at, power(p), true);
                        case "masked" -> whisperers(p.level(), at, power(p));
                        case "survivor" -> spawn(p.level(), at, Faction.SURVIVOR, Role.MEMBER, 5 + p.getRandom().nextInt(40), null);
                        default -> { p.sendSystemMessage(Component.literal("raiders | commander | masked | survivor")); return 0; }
                    }
                    return 1;
                })))));
    }

    static int power(ServerPlayer p) { return SkillManager.get(p).totalPower(); }

    public static HumanEntity spawn(ServerLevel level, BlockPos pos, Faction f, Role r, int lv, String community) {
        HumanEntity h = HUMAN.create(level, EntitySpawnReason.EVENT);
        if (h == null) return null;
        h.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        h.setup(f, r, lv, community);
        level.addFreshEntity(h);
        return h;
    }

    private static BlockPos surface(ServerLevel level, BlockPos around, int min, int max) {
        for (int tries = 0; tries < 12; tries++) {
            double a = level.random.nextDouble() * Math.PI * 2, d = min + level.random.nextInt(max - min);
            int x = around.getX() + (int) (Math.cos(a) * d), z = around.getZ() + (int) (Math.sin(a) * d);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos p = new BlockPos(x, y, z);
            if (!level.getFluidState(p.below()).isEmpty() || !level.getFluidState(p).isEmpty()) continue;
            if (com.kiyamet.world.ModStructures.inSanctuary(level, p)) continue;
            return p;
        }
        return null;
    }

    /** Roaming events around each player: lone survivors, raider gangs, masked herds and the raider commander. */
    private static void events(ServerPlayer p) {
        ServerLevel level = p.level();
        if (level.dimension() != Level.OVERWORLD || p.isSpectator()) return;
        if (level.getEntitiesOfClass(HumanEntity.class, p.getBoundingBox().inflate(128), h -> h.faction() == Faction.RAIDER || h.faction() == Faction.WHISPERER).size() > 10) return;
        int power = power(p);
        float roll = level.random.nextFloat();
        BlockPos at = surface(level, p.blockPosition(), 40, 64);
        if (at == null) return;
        if (power >= 4000 && roll < 0.02f) {
            raiders(level, at, power, true);
            p.sendSystemMessage(Component.literal("Engines roar in the distance... the Raider Commander is coming for you.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            level.playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 2f, 0.6f);
        } else if (power >= 1500 && roll < 0.08f) {
            whisperers(level, at, power);
            p.sendSystemMessage(Component.literal("You hear whispers among the groans of the dead...").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else if (power >= 300 && roll < 0.2f) {
            raiders(level, at, power, false);
            p.sendSystemMessage(Component.literal("Voices and laughter nearby. Raiders.").withStyle(ChatFormatting.RED));
        } else if (roll < 0.42f) {
            spawn(level, at, Faction.SURVIVOR, Role.MEMBER, 1 + level.random.nextInt(10 + power / 40), null);
        }
    }

    public static void raiders(ServerLevel level, BlockPos at, int power, boolean commander) {
        int base = Mth.clamp(5 + power / 25, 5, 240);
        int n = 3 + level.random.nextInt(3) + (commander ? 3 : 0);
        for (int i = 0; i < n; i++) {
            Role r = i == 0 ? (commander ? Role.COMMANDER : Role.LEADER) : Role.MEMBER;
            int lv = r == Role.COMMANDER ? 260 + level.random.nextInt(41) : Mth.clamp(base + level.random.nextInt(21) - 10 + (r == Role.LEADER ? 15 : 0), 1, 250);
            spawn(level, at.offset(level.random.nextInt(5) - 2, 0, level.random.nextInt(5) - 2), Faction.RAIDER, r, lv, null);
        }
    }

    public static void whisperers(ServerLevel level, BlockPos at, int power) {
        int base = Mth.clamp(20 + power / 30, 20, 220);
        int n = 4 + level.random.nextInt(3);
        for (int i = 0; i < n; i++)
            spawn(level, at.offset(level.random.nextInt(7) - 3, 0, level.random.nextInt(7) - 3), Faction.WHISPERER,
                    i == 0 && power > 5000 ? Role.COMMANDER : i == 0 ? Role.LEADER : Role.MEMBER, i == 0 && power > 5000 ? 270 + level.random.nextInt(31) : base + level.random.nextInt(20));
        for (int i = 0; i < 8; i++) {
            KZombie z = ModEntities.ZOMBIES.get(level.random.nextBoolean() ? ZombieKind.FARMER : ZombieKind.TOXIC).create(level, EntitySpawnReason.EVENT);
            if (z == null) continue;
            z.setPos(at.getX() + level.random.nextInt(11) - 5, at.getY(), at.getZ() + level.random.nextInt(11) - 5);
            z.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.EVENT, null);
            level.addFreshEntity(z);
        }
    }

    private ModHumans() {}
}
