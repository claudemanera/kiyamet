package com.kiyamet.gunsmith;

import com.kiyamet.ammo.Ammo;
import com.kiyamet.item.GunItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 1.9 gun handling: every gun has a clip that you reload with R (or it reloads by itself when you pull the trigger on
 * an empty clip). Guns wear with every shot; a worn gun can jam - press R to clear it. Repair at the Gunsmith Bench.
 * Rename your gun on an anvil or with /twd gunname.
 */
public final class GunHandling {
    static CompoundTag tag(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
    static void put(ItemStack s, CompoundTag t) { s.set(DataComponents.CUSTOM_DATA, CustomData.of(t)); }

    public static int clipSize(ItemStack gun) {
        String id = BuiltInRegistries.ITEM.getKey(gun.getItem()).getPath();
        Ammo.Type t = Ammo.typeFor(id);
        GunItem.Mode m = gun.getItem() instanceof GunItem g ? g.stats().mode() : GunItem.Mode.SINGLE;
        int base = switch (t) {
            case PISTOL -> id.contains("revolver") || id.contains("magnum") || id.equals("hand_cannon") ? 6 : id.equals("machine_pistol") ? 24 : 12;
            case SHELL -> id.equals("auto_shotgun") || id.equals("ak_shotgun") ? 10 : id.equals("sawed_off") ? 2 : 6;
            case SNIPER -> id.equals("anti_materiel_rifle") ? 3 : 5;
            case ROCKET -> 1;
            case PLASMA -> 20;
            default -> id.equals("rpk") ? 60 : m == GunItem.Mode.SINGLE && id.contains("rifle") ? 8 : 30;
        };
        String mag = GunMods.get(gun, GunMods.Slot.MAG);
        return mag.equals("drum") ? base * 2 : mag.equals("extended") ? base * 3 / 2 : base;
    }

    public static int clip(ItemStack gun) { return tag(gun).getIntOr("Clip", 0); }
    public static boolean jammed(ItemStack gun) { return tag(gun).getBooleanOr("Jammed", false); }
    public static float wear(ItemStack gun) { return gun.isDamageableItem() ? gun.getDamageValue() / (float) Math.max(1, gun.getMaxDamage()) : 0; }

    /** called before a shot: false = no shot (empty, jammed or broken) */
    public static boolean beforeShot(ServerPlayer p, ItemStack gun, int rounds) {
        if (jammed(gun)) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 0.7f);
            p.displayClientMessage(Component.literal("JAMMED! Press R to clear it.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
            return false;
        }
        if (gun.isDamageableItem() && gun.getDamageValue() >= gun.getMaxDamage() - 1) {
            p.displayClientMessage(Component.literal("This gun is worn out - repair it at the Gunsmith Bench.").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (clip(gun) <= 0) { reload(p, gun); return false; }
        CompoundTag t = tag(gun);
        t.putInt("Clip", Math.max(0, clip(gun) - rounds));
        float w = wear(gun);
        float eng = Math.min(0.6f, com.kiyamet.skill.SkillManager.get(p).level(com.kiyamet.skill.Skill.ENGINEERING) / 1500f);
        if (p.getRandom().nextFloat() < (0.004f + w * w * 0.12f) * (1f - eng)) {
            t.putBoolean("Jammed", true);
            put(gun, t);
            p.level().playSound(null, p.blockPosition(), SoundEvents.CROSSBOW_LOADING_START.value(), SoundSource.PLAYERS, 1f, 0.5f);
            p.displayClientMessage(Component.literal("Your gun jammed! Press R to clear it.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
            return false;
        }
        put(gun, t);
        if (gun.isDamageableItem() && !p.isCreative() && p.getRandom().nextFloat() >= eng * 0.8f) gun.setDamageValue(Math.min(gun.getMaxDamage() - 1, gun.getDamageValue() + 1));
        return true;
    }

    /** R: clear a jam, or fill the clip from your magazines / loose rounds */
    public static void reload(ServerPlayer p, ItemStack gun) {
        if (p.getCooldowns().isOnCooldown(gun)) return;
        CompoundTag t = tag(gun);
        if (t.getBooleanOr("Jammed", false)) {
            t.putBoolean("Jammed", false); put(gun, t);
            p.getCooldowns().addCooldown(gun, 25);
            p.level().playSound(null, p.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 1f, 1.3f);
            p.displayClientMessage(Component.literal("Jam cleared.").withStyle(ChatFormatting.GREEN), true);
            com.kiyamet.skill.SkillManager.addXp(p, com.kiyamet.skill.Skill.ENGINEERING, 6);
            return;
        }
        int size = clipSize(gun), have = clip(gun);
        if (have >= size) { p.displayClientMessage(Component.literal("Clip full (" + have + "/" + size + ")").withStyle(ChatFormatting.GRAY), true); return; }
        String id = BuiltInRegistries.ITEM.getKey(gun.getItem()).getPath();
        Ammo.Type type = Ammo.typeFor(id);
        int got = 0;
        if (p.isCreative()) got = size - have;
        else while (have + got < size && Ammo.take(p, type, 1)) got++;
        if (got == 0) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.4f);
            p.displayClientMessage(Component.literal("No " + type.label + " ammo to reload!").withStyle(ChatFormatting.RED), true);
            return;
        }
        t.putInt("Clip", have + got); put(gun, t);
        int time = type == Ammo.Type.SHELL ? 12 + got * 6 : type == Ammo.Type.ROCKET ? 50 : type == Ammo.Type.SNIPER ? 45 : 32;
        p.getCooldowns().addCooldown(gun, Math.min(70, time));
        p.level().playSound(null, p.blockPosition(), SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), SoundSource.PLAYERS, 1f, 0.9f);
        p.level().playSound(null, p.blockPosition(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 0.6f, 1.6f);
        p.displayClientMessage(Component.literal("Reloading... " + (have + got) + "/" + size).withStyle(ChatFormatting.YELLOW), true);
    }

    public static Component hud(ItemStack gun) {
        int w = Math.round(wear(gun) * 100);
        return Component.literal("▮ " + clip(gun) + "/" + clipSize(gun) + "  ·  wear " + w + "%" + (jammed(gun) ? "  ·  JAMMED" : ""))
                .withStyle(jammed(gun) ? ChatFormatting.RED : clip(gun) == 0 ? ChatFormatting.GOLD : w > 70 ? ChatFormatting.YELLOW : ChatFormatting.WHITE);
    }

    public static void init() {
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d, ctx, env) -> d.register(net.minecraft.commands.Commands.literal("twd")
                .then(net.minecraft.commands.Commands.literal("gunname").then(net.minecraft.commands.Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ItemStack g = p.getMainHandItem();
                    if (!(g.getItem() instanceof GunItem)) { p.sendSystemMessage(Component.literal("Hold a gun first.").withStyle(ChatFormatting.RED)); return 0; }
                    String n = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "name");
                    if (n.length() > 32) n = n.substring(0, 32);
                    g.set(DataComponents.CUSTOM_NAME, Component.literal(n).withStyle(s -> s.withItalic(false).withColor(ChatFormatting.GOLD)));
                    p.sendSystemMessage(Component.literal("Your gun is now called \"" + n + "\".").withStyle(ChatFormatting.GOLD));
                    return 1;
                })))));
    }

    private GunHandling() {}
}
