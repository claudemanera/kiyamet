package com.kiyamet.enchant;

import com.kiyamet.npc.Shop;
import com.kiyamet.skill.Skill;
import com.kiyamet.skill.SkillData;
import com.kiyamet.skill.SkillManager;
import com.kiyamet.ui.PanelMenu;
import com.kiyamet.ui.Ui;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Enchantment Forge: hold an item, sneak + right-click an enchanting table. Add or raise any enchantment the item can take,
 * up to the level your Enchanting skill has unlocked (gun enchantments: Firearms, and a blue tome to learn each one).
 * Costs XP levels and lapis.
 */
public class ForgeMenu extends PanelMenu {
    private final List<Holder<Enchantment>> shown = new ArrayList<>();

    public static void open(ServerPlayer p) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ForgeMenu(id, inv, p), Component.literal("Enchantment Forge")));
    }

    private ForgeMenu(int id, Inventory inv, ServerPlayer p) { super(id, inv, p); redraw(); }

    record Offer(Holder<Enchantment> h, String id, int cur, int next, int cap, int max, int xp, int lapis, String block) {}

    Offer offer(Holder<Enchantment> h, ItemStack stack, SkillData d) {
        String id = Enchants.id(h);
        ItemEnchantments en = stack.getEnchantments();
        int cur = en.getLevel(h), max = Enchants.maxCap(id), next = cur + 1;
        int cap = Enchants.cap(id, d.level(Skill.ENCHANTING), d.level(Skill.ARCHERY));
        int vmax = h.value().getMaxLevel();
        int xp = 3 + 2 * next + (next > vmax ? 4 * (next - vmax) : 0);
        int lapis = Math.min(64, next * 2);
        String block = null;
        if (cur >= max) block = "MAX";
        else if (next > cap) {
            int at = Enchants.nextUnlock(id, cur);
            block = Enchants.isGun(id) ? (at > 0 ? "Unlocks at Firearms " + at : "Locked") : (at > 0 ? "Unlocks at Enchanting " + at : "Locked");
        } else if (cur == 0 && !EnchantmentHelper.isEnchantmentCompatible(en.keySet(), h)) block = "Conflicts with an enchantment on this item";
        else if (cur == 0 && Enchants.isGun(id) && !hasTome(id)) block = "Needs a blue Gunsmith Tome: " + Enchants.gunTitle(id);
        return new Offer(h, id, cur, next, cap, max, xp, lapis, block);
    }

    boolean hasTome(String id) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(ModEnchant.GUN_TOME) && GunTomeItem.ench(s).equals(id)) return true;
        }
        return false;
    }

    boolean takeTome(String id) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(ModEnchant.GUN_TOME) && GunTomeItem.ench(s).equals(id)) { s.shrink(1); return true; }
        }
        return false;
    }

    @Override
    protected void draw() {
        ItemStack stack = player.getMainHandItem();
        SkillData d = SkillManager.get(player);
        shown.clear();
        c.setItem(4, stack.isEmpty() ? Ui.icon(Items.BARRIER, Ui.text("Hold the item you want to enchant", ChatFormatting.RED)) : stack.copy());
        c.setItem(0, Ui.icon(Items.EXPERIENCE_BOTTLE, Ui.text("Your XP levels: " + player.experienceLevel, ChatFormatting.GREEN),
                Ui.text("Lapis: " + Shop.count(player, "minecraft:lapis_lazuli"), ChatFormatting.BLUE),
                Ui.text("Enchanting " + d.level(Skill.ENCHANTING) + " · Archery " + d.level(Skill.ARCHERY), ChatFormatting.GRAY)));
        c.setItem(8, Ui.icon(Items.BOOK, Ui.text("How it works", ChatFormatting.GOLD),
                Ui.text("Each click raises one enchantment by one level.", ChatFormatting.GRAY),
                Ui.text("Higher levels unlock along the Enchanting road.", ChatFormatting.GRAY),
                Ui.text("Gun enchantments: Firearms road + blue tomes.", ChatFormatting.AQUA)));
        if (stack.isEmpty()) return;
        List<Holder<Enchantment>> all = new ArrayList<>();
        player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().forEach(h -> { if (h.value().canEnchant(stack) || stack.getEnchantments().getLevel(h) > 0) all.add(h); });
        all.sort(Comparator.comparing((Holder<Enchantment> h) -> stack.getEnchantments().getLevel(h) > 0 ? 0 : 1).thenComparing(Enchants::id));
        int slot = 9;
        for (Holder<Enchantment> h : all) {
            if (slot >= 45) break;
            Offer o = offer(h, stack, d);
            boolean gun = Enchants.isGun(o.id);
            boolean ok = o.block == null && player.experienceLevel >= o.xp && Shop.count(player, "minecraft:lapis_lazuli") >= o.lapis;
            String name = Enchants.title(o.id);
            ItemStack icon = gun ? GunTomeItem.of(o.id) : new ItemStack(o.cur > 0 ? Items.ENCHANTED_BOOK : Items.BOOK);
            List<Component> lore = new ArrayList<>();
            lore.add(Ui.text(o.cur > 0 ? "Now: " + name + " " + Enchants.roman(o.cur) : "Not on this item", ChatFormatting.GRAY));
            if (o.block == null) {
                lore.add(Ui.text("Cost: " + o.xp + " XP levels + " + o.lapis + " lapis" + (o.cur == 0 && gun ? " + the tome" : ""), ok ? ChatFormatting.GREEN : ChatFormatting.RED));
                lore.add(Ui.text(ok ? "Click to enchant!" : "Not enough XP or lapis", ok ? ChatFormatting.YELLOW : ChatFormatting.RED));
            } else lore.add(Ui.text(o.block, o.block.equals("MAX") ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.RED));
            lore.add(Ui.text("Unlocked up to " + Enchants.roman(Math.max(1, o.cap)) + " · max " + Enchants.roman(o.max), ChatFormatting.DARK_AQUA));
            String title = o.cur >= o.max ? name + " " + Enchants.roman(o.cur) + " (MAX)" : name + " " + (o.cur > 0 ? Enchants.roman(o.cur) + " → " : "") + Enchants.roman(o.next);
            icon.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Ui.text(title, gun ? ChatFormatting.AQUA : ok ? ChatFormatting.GREEN : ChatFormatting.WHITE, ChatFormatting.BOLD));
            Ui.lore(icon, lore);
            c.setItem(slot, Ui.glint(icon, ok));
            shown.add(h);
            slot++;
        }
        c.setItem(49, Ui.icon(Items.BARRIER, Ui.text("Close", ChatFormatting.RED)));
    }

    @Override
    protected boolean click(int slot, ClickType type) {
        if (slot == 49) { player.closeContainer(); return false; }
        int i = slot - 9;
        if (i < 0 || i >= shown.size()) return false;
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return false;
        Offer o = offer(shown.get(i), stack, SkillManager.get(player));
        if (o.block != null) { player.displayClientMessage(Component.literal(o.block).withStyle(ChatFormatting.RED), true); return false; }
        if (player.experienceLevel < o.xp && !player.isCreative()) { player.displayClientMessage(Component.literal("You need " + o.xp + " XP levels.").withStyle(ChatFormatting.RED), true); return false; }
        if (!player.isCreative() && !Shop.take(player, "minecraft:lapis_lazuli", o.lapis)) { player.displayClientMessage(Component.literal("You need " + o.lapis + " lapis.").withStyle(ChatFormatting.RED), true); return false; }
        if (o.cur == 0 && Enchants.isGun(o.id) && !player.isCreative() && !takeTome(o.id)) return false;
        if (!player.isCreative()) player.giveExperienceLevels(-o.xp);
        stack.enchant(o.h, o.next);
        SkillManager.addXp(player, Skill.ENCHANTING, 25 * o.next);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1f);
        player.displayClientMessage(Component.literal(Enchants.title(o.id) + " " + Enchants.roman(o.next) + "!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), true);
        return true;
    }
}
