package com.kiyamet.money;

import com.kiyamet.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

/**
 * TWD 1.6 money: paper dollars ($1, $5, $10, $20, $50, $100) and a Money Bag. Every trade in the game is paid in dollars now,
 * villagers included (their emeralds become $10 bills). Old prices in "rubies" are $10 each.
 */
public final class Money {
    public static final int[] BILLS = {100, 50, 20, 10, 5, 1};
    public static final int RATE = 10;    // one old ruby price = $10

    public static Item bill(int v) { return ModItems.get("dollar_" + v); }
    static int value(ItemStack s) { for (int v : BILLS) if (!s.isEmpty() && s.is(bill(v))) return v; return 0; }

    public static int total(Player p) {
        int n = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            n += value(s) * s.getCount();
            if (s.getItem() instanceof BagItem) n += BagItem.balance(s);
        }
        return n;
    }

    /** Pays: takes bills (and change comes back), then money from bags. */
    public static boolean take(ServerPlayer p, int dollars) {
        if (p.isCreative() || dollars <= 0) return true;
        if (total(p) < dollars) return false;
        Inventory inv = p.getInventory();
        int loose = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) { ItemStack s = inv.getItem(i); int v = value(s); if (v > 0) { loose += v * s.getCount(); inv.setItem(i, ItemStack.EMPTY); } }
        int left = dollars - loose;
        if (left <= 0) { give(p, -left); return true; }
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof BagItem) { int b = BagItem.balance(s), k = Math.min(b, left); BagItem.set(s, b - k); left -= k; }
        }
        return true;
    }

    public static List<ItemStack> bills(int dollars) {
        List<ItemStack> out = new ArrayList<>();
        for (int v : BILLS) { int n = dollars / v; dollars %= v; while (n > 0) { int k = Math.min(n, 64); out.add(new ItemStack(bill(v), k)); n -= k; } }
        return out;
    }

    public static void give(ServerPlayer p, int dollars) {
        for (ItemStack s : bills(dollars)) if (!p.getInventory().add(s)) p.drop(s, false);
    }

    /** A single stack worth about that much (for chests and drops). */
    public static ItemStack stack(int dollars) {
        for (int v : BILLS) if (dollars >= v && dollars / v <= 64 && dollars % v == 0) return new ItemStack(bill(v), dollars / v);
        return new ItemStack(bill(10), Math.max(1, Math.min(64, dollars / 10)));
    }

    public static String fmt(int d) { return "$" + String.format("%,d", d); }

    // ------------------------------------------------------------------ items
    public static class BillItem extends Item {
        public BillItem(Properties p) { super(p); }
    }

    /** Money Bag ($20,000) / Gold Money Bag ($100,000): all your cash lives here. Bills you pick up go straight in.
     *  Right-click: put loose bills in · sneak + right-click: take $100 out · sneak + right-click a block: put it down. */
    public static class BagItem extends Item {
        public final int cap;
        public BagItem(Properties p, int cap) { super(p); this.cap = cap; }
        @Override
        public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext ctx) {
            InteractionResult r = com.kiyamet.furniture.BagBlock.place(ctx);
            return r != InteractionResult.PASS ? r : super.useOn(ctx);
        }
        static int balance(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr("Dollars", 0); }
        static void set(ItemStack s, int d) {
            CompoundTag t = s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            t.putInt("Dollars", Math.max(0, d));
            s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
            int cap = s.getItem() instanceof BagItem b ? b.cap : 0;
            s.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Holds " + fmt(Math.max(0, d)) + " / " + fmt(cap)).withStyle(st -> st.withItalic(false).withColor(ChatFormatting.GREEN)),
                    Component.literal("Right-click: put your bills in · Sneak: take $100 out").withStyle(st -> st.withItalic(false).withColor(ChatFormatting.DARK_GRAY)))));
        }
        /** Moves loose bills into the bag up to its limit (gives change back as smaller bills if needed). */
        static int deposit(ServerPlayer p, ItemStack bag) {
            int room = ((BagItem) bag.getItem()).cap - balance(bag);
            if (room <= 0) return 0;
            Inventory inv = p.getInventory();
            int add = 0;
            for (int v : BILLS) for (int i = 0; i < inv.getContainerSize() && room - add >= v; i++) {
                ItemStack s = inv.getItem(i);
                if (value(s) != v) continue;
                int k = Math.min(s.getCount(), (room - add) / v);
                s.shrink(k); add += k * v;
            }
            if (add > 0) set(bag, balance(bag) + add);
            return add;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!(player instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
            ItemStack bag = player.getItemInHand(hand);
            int b = balance(bag);
            if (p.isShiftKeyDown()) {
                int k = Math.min(100, b);
                if (k <= 0) return InteractionResult.FAIL;
                set(bag, b - k); give(p, k);
            } else {
                int add = deposit(p, bag);
                if (add == 0) { p.displayClientMessage(Component.literal("Bag: " + fmt(b) + " / " + fmt(cap)).withStyle(ChatFormatting.GREEN), true); return InteractionResult.FAIL; }
            }
            level.playSound(null, p.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8f, 1.1f);
            p.displayClientMessage(Component.literal("Money Bag: " + fmt(balance(bag)) + "   ·   You carry " + fmt(total(p))).withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.CONSUME;
        }
    }

    // ------------------------------------------------------------------ villagers trade in dollars
    static ItemCost cost(ItemCost c) {
        return c.item().value() == Items.EMERALD ? new ItemCost(bill(10), c.count()) : c;
    }

    static void convert(MerchantOffers offers) {
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer o = offers.get(i);
            boolean em = o.getItemCostA().item().value() == Items.EMERALD || o.getItemCostB().map(c -> c.item().value() == Items.EMERALD).orElse(false) || o.getResult().is(Items.EMERALD);
            if (!em) continue;
            ItemStack res = o.getResult().is(Items.EMERALD) ? new ItemStack(bill(10), o.getResult().getCount()) : o.getResult();
            Optional<ItemCost> b = o.getItemCostB().map(Money::cost);
            offers.set(i, new MerchantOffer(cost(o.getItemCostA()), b, res, o.getUses(), o.getMaxUses(), o.getXp(), o.getPriceMultiplier(), o.getDemand()));
        }
    }

    public static void init() {
        for (int v : BILLS) ModItems.custom("dollar_" + v, BillItem::new);
        ModItems.custom("money_bag", p -> new BagItem(p.stacksTo(1), 20_000));
        ModItems.custom("gold_money_bag", p -> new BagItem(p.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE), 100_000));
        // cash you pick up goes straight into your bag
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 7) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                Inventory inv = p.getInventory();
                boolean bills = false;
                for (int i = 0; i < inv.getContainerSize() && !bills; i++) bills = value(inv.getItem(i)) > 0;
                if (!bills) continue;
                for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).getItem() instanceof BagItem) BagItem.deposit(p, inv.getItem(i));
            }
        });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!level.isClientSide() && entity instanceof net.minecraft.world.entity.npc.villager.AbstractVillager v) convert(v.getOffers());
            return InteractionResult.PASS;
        });
        // walkers drop a few dollars and a little something
        ServerLivingEntityEvents.AFTER_DEATH.register((e, src) -> {
            if (!(e instanceof com.kiyamet.entity.KZombie z) || !(src.getEntity() instanceof Player) || !(e.level() instanceof ServerLevel level)) return;
            var r = level.random;
            if (r.nextFloat() < 0.6f) {
                int[] pick = {1, 1, 1, 5, 5, 10, 20};
                e.spawnAtLocation(level, new ItemStack(bill(pick[r.nextInt(pick.length)]), 1 + r.nextInt(3)));
            }
            if (r.nextFloat() < 0.35f) {
                String[] small = switch (z.kind.id) {
                    case "doctor_zombie", "nurse_zombie" -> new String[]{"kiyamet:bandage", "kiyamet:bandage", "kiyamet:medkit"};
                    case "police_zombie" -> new String[]{"kiyamet:pistol_ammo", "kiyamet:pistol_ammo", "kiyamet:magazine_small"};
                    case "farmer_zombie" -> new String[]{"kiyamet:tomato_seeds", "kiyamet:corn_seeds", "kiyamet:onion", "minecraft:bread"};
                    case "armored_zombie", "tank_zombie" -> new String[]{"minecraft:iron_nugget", "kiyamet:metal_plate", "kiyamet:rifle_ammo"};
                    case "toxic_zombie" -> new String[]{"minecraft:spider_eye", "kiyamet:vaccine"};
                    case "hunter_zombie", "runner_zombie" -> new String[]{"kiyamet:rifle_ammo", "minecraft:leather", "kiyamet:shotgun_shell"};
                    case "exploder_zombie" -> new String[]{"minecraft:gunpowder", "kiyamet:frag_grenade"};
                    case "alpha_zombie", "mutant_zombie" -> new String[]{"kiyamet:ruby", "kiyamet:circuit", "kiyamet:sniper_ammo"};
                    default -> new String[]{"minecraft:bread", "kiyamet:pistol_ammo", "minecraft:string", "minecraft:paper", "kiyamet:bandage", "minecraft:apple"};
                };
                e.spawnAtLocation(level, new ItemStack(com.kiyamet.ui.Ui.item(small[r.nextInt(small.length)]), 1 + (r.nextInt(3) == 0 ? 1 : 0)));
            }
        });
    }

    private Money() {}
}
