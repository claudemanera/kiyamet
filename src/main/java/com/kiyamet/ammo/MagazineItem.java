package com.kiyamet.ammo;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

/** A magazine: right-click to load rounds from the inventory, sneak + right-click to empty it back. Guns draw from it first. */
public class MagazineItem extends Item {
    public final int capacity;

    public MagazineItem(Properties p, int capacity) { super(p); this.capacity = capacity; }

    static CompoundTag tag(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
    public static int rounds(ItemStack s) { return tag(s).getIntOr("Rounds", 0); }
    public static Ammo.Type type(ItemStack s) { return Ammo.Type.byName(tag(s).getStringOr("Ammo", "")); }

    static void set(ItemStack s, Ammo.Type t, int n) {
        CompoundTag c = tag(s);
        if (n <= 0 || t == null) { c.remove("Rounds"); c.remove("Ammo"); } else { c.putInt("Rounds", n); c.putString("Ammo", t.name()); }
        if (c.isEmpty()) s.remove(DataComponents.CUSTOM_DATA); else s.set(DataComponents.CUSTOM_DATA, CustomData.of(c));
        int cap = s.getItem() instanceof MagazineItem m ? m.capacity : 0;
        List<Component> lore = new ArrayList<>();
        if (n > 0 && t != null) lore.add(Component.literal(t.label + ": " + n + " / " + cap).withStyle(t.color));
        else lore.add(Component.literal("Empty - holds " + cap + " rounds").withStyle(ChatFormatting.GRAY));
        lore.add(Component.literal("Right-click: load · Sneak: empty").withStyle(ChatFormatting.DARK_GRAY));
        s.set(DataComponents.LORE, new ItemLore(lore));
    }

    /** Removes up to n rounds, returns how many were taken. */
    static int drain(ItemStack s, int n) {
        int have = rounds(s), k = Math.min(have, n);
        set(s, type(s), have - k);
        return k;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack mag = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
        Inventory inv = p.getInventory();
        Ammo.Type t = type(mag);
        int have = rounds(mag);
        if (p.isShiftKeyDown()) {   // empty it
            if (have <= 0 || t == null) return InteractionResult.FAIL;
            int left = have;
            while (left > 0) { int k = Math.min(left, t.item().getDefaultMaxStackSize()); ItemStack out = new ItemStack(t.item(), k); if (!inv.add(out)) p.drop(out, false); left -= k; }
            set(mag, null, 0);
            level.playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_CHAIN.value(), SoundSource.PLAYERS, 0.7f, 0.8f);
            p.displayClientMessage(Component.literal("Magazine emptied: " + have + " " + t.label).withStyle(t.color), true);
            return InteractionResult.CONSUME;
        }
        if (t == null) {   // pick the type: the gun in the other hand, else the first ammo found
            ItemStack other = p.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (other.getItem() instanceof com.kiyamet.item.GunItem) t = Ammo.typeFor(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(other.getItem()).getPath());
            if (t == null || Ammo.loose(inv, t) == 0)
                for (int i = 0; i < inv.getContainerSize() && (t == null || Ammo.loose(inv, t) == 0); i++) { Ammo.Type f = Ammo.Type.ofItem(inv.getItem(i)); if (f != null) t = f; }
        }
        if (t == null || Ammo.loose(inv, t) == 0) { p.displayClientMessage(Component.literal("No rounds to load.").withStyle(ChatFormatting.RED), true); return InteractionResult.FAIL; }
        int room = capacity - have;
        if (room <= 0) { p.displayClientMessage(Component.literal("Magazine is full.").withStyle(ChatFormatting.YELLOW), true); return InteractionResult.FAIL; }
        int added = 0;
        for (int i = 0; i < inv.getContainerSize() && added < room; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(t.item())) { int k = Math.min(room - added, s.getCount()); s.shrink(k); added += k; }
        }
        set(mag, t, have + added);
        level.playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 0.8f, 1.3f);
        p.displayClientMessage(Component.literal("Loaded " + added + " " + t.label + "  (" + (have + added) + "/" + capacity + ")").withStyle(t.color), true);
        p.getCooldowns().addCooldown(mag, 10);
        return InteractionResult.CONSUME;
    }

    @Override public boolean isBarVisible(ItemStack s) { return rounds(s) > 0; }
    @Override public int getBarWidth(ItemStack s) { return Math.round(13f * rounds(s) / capacity); }
    @Override public int getBarColor(ItemStack s) { Ammo.Type t = type(s); Integer c = t == null ? null : t.color.getColor(); return c == null ? 0xFFFFFF : c; }
}
