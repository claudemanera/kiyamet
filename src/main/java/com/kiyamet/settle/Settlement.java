package com.kiyamet.settle;

import com.kiyamet.Kiyamet;
import com.kiyamet.npc.HumanEntity;
import com.kiyamet.registry.ModItems;
import com.kiyamet.ui.PanelMenu;
import com.kiyamet.ui.Ui;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1.9 your own settlement. Place a Settlement Bell, then open a companion's panel and choose "Settle here".
 * Settlers live around the bell and work every minute: farmers harvest and replant, smiths turn iron into plates and
 * ammo, doctors heal, guards watch for walkers, cooks cook raw food. Everything goes into (and comes out of) the chests
 * within 12 blocks of the bell. Every morning each settler eats one food from those chests and needs a bed nearby -
 * fed and rested settlers stay happy, hungry or bedless ones lose morale (and a broken settler may leave... or worse).
 */
public final class Settlement {
    public static final String[] JOBS = {"FARMER", "SMITH", "DOCTOR", "GUARD", "COOK"};
    public static final Item[] JOB_ICONS = {Items.IRON_HOE, Items.ANVIL, Items.GOLDEN_APPLE, Items.SHIELD, Items.SMOKER};
    public static final String[] JOB_DESC = {"Harvests ripe crops near the bell and replants them", "Turns iron into metal plates and makes ammo",
            "Heals you and the settlers, makes bandages", "Watches the walls, marks walkers that come close", "Cooks raw meat and fish from the chests"};
    public static final int RADIUS = 24, CHEST_RANGE = 12;
    public static Block BELL;
    static final Map<UUID, Map<String, Integer>> MADE = new HashMap<>();

    public static class BellBlock extends Block {
        static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);
        public BellBlock(Properties p) { super(p); }
        @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer sp) {
                level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1f, 1f);
                Panel.open(sp, pos.immutable());
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static void init() {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Kiyamet.id("settlement_bell"));
        BELL = Registry.register(BuiltInRegistries.BLOCK, key, new BellBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BELL).noOcclusion().setId(key)));
        ModItems.custom("settlement_bell", ip -> new BlockItem(BELL, ip.useBlockDescriptionPrefix()));
        // the morning: food and beds
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerLevel ow = server.overworld();
            if (ow.getDayTime() % 24000 != 1000) return;
            Map<UUID, List<HumanEntity>> byOwner = new HashMap<>();
            for (ServerLevel l : server.getAllLevels())
                for (HumanEntity h : l.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(HumanEntity.class), h -> !h.work.isEmpty() && h.settleAt != null && h.ownerPlayer() != null))
                    byOwner.computeIfAbsent(h.ownerPlayer().getUUID(), k -> new ArrayList<>()).add(h);
            byOwner.forEach((id, list) -> morning(server.getPlayerList().getPlayer(id), list));
        });
    }

    public static List<Container> chests(ServerLevel l, BlockPos bell) {
        List<Container> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(bell.offset(-CHEST_RANGE, -4, -CHEST_RANGE), bell.offset(CHEST_RANGE, 4, CHEST_RANGE))) {
            BlockEntity be = l.getBlockEntity(p);
            if (be instanceof Container c && !(be instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity) && c.getContainerSize() >= 9) out.add(c);
        }
        return out;
    }

    static boolean store(List<Container> cs, ItemStack s) {
        for (Container c : cs) {
            for (int i = 0; i < c.getContainerSize() && !s.isEmpty(); i++) {
                ItemStack in = c.getItem(i);
                if (in.isEmpty()) { c.setItem(i, s.copy()); s.setCount(0); }
                else if (ItemStack.isSameItemSameComponents(in, s) && in.getCount() < in.getMaxStackSize()) { int m = Math.min(s.getCount(), in.getMaxStackSize() - in.getCount()); in.grow(m); s.shrink(m); }
            }
            c.setChanged();
            if (s.isEmpty()) return true;
        }
        return s.isEmpty();
    }

    static ItemStack takeOne(List<Container> cs, java.util.function.Predicate<ItemStack> what) {
        for (Container c : cs) for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack in = c.getItem(i);
            if (!in.isEmpty() && what.test(in)) { ItemStack one = in.split(1); c.setChanged(); return one; }
        }
        return ItemStack.EMPTY;
    }

    static int count(List<Container> cs, java.util.function.Predicate<ItemStack> what) {
        int n = 0;
        for (Container c : cs) for (int i = 0; i < c.getContainerSize(); i++) { ItemStack in = c.getItem(i); if (!in.isEmpty() && what.test(in)) n += in.getCount(); }
        return n;
    }

    static boolean isFood(ItemStack s) { return s.has(DataComponents.FOOD) && !s.is(Items.ROTTEN_FLESH) && !s.is(Items.SPIDER_EYE) && !s.is(Items.POISONOUS_POTATO); }

    public static int beds(ServerLevel l, BlockPos bell) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(bell.offset(-RADIUS, -6, -RADIUS), bell.offset(RADIUS, 6, RADIUS))) {
            BlockState s = l.getBlockState(p);
            if (s.getBlock() instanceof com.kiyamet.furniture.FurnitureBed) n++;
            else if (s.getBlock() instanceof BedBlock && s.hasProperty(BedBlock.PART) && s.getValue(BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD) n++;
        }
        return n;
    }

    static void made(HumanEntity h, String what, int n) {
        if (h.ownerPlayer() == null || n <= 0) return;
        MADE.computeIfAbsent(h.ownerPlayer().getUUID(), k -> new LinkedHashMap<>()).merge(what, n, Integer::sum);
    }

    static Item cooked(Item raw) {
        String id = BuiltInRegistries.ITEM.getKey(raw).toString();
        String c = switch (id) {
            case "minecraft:beef" -> "minecraft:cooked_beef"; case "minecraft:porkchop" -> "minecraft:cooked_porkchop"; case "minecraft:chicken" -> "minecraft:cooked_chicken";
            case "minecraft:mutton" -> "minecraft:cooked_mutton"; case "minecraft:rabbit" -> "minecraft:cooked_rabbit"; case "minecraft:cod" -> "minecraft:cooked_cod";
            case "minecraft:salmon" -> "minecraft:cooked_salmon"; case "minecraft:potato" -> "minecraft:baked_potato";
            default -> id.startsWith("kiyamet:") && !id.contains("cooked") && BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.Identifier.parse("kiyamet:cooked_" + id.substring(8))) ? "kiyamet:cooked_" + id.substring(8) : null;
        };
        return c == null ? null : Ui.item(c);
    }

    /** one minute of work */
    public static void work(ServerLevel l, HumanEntity h) {
        if (h.work.isEmpty() || h.settleAt == null || !l.isLoaded(h.settleAt) || !(l.getBlockState(h.settleAt).getBlock() instanceof BellBlock)) return;
        if (h.morale < 25 && h.getRandom().nextBoolean()) return;           // unhappy settlers slack off
        List<Container> cs = chests(l, h.settleAt);
        switch (h.work) {
            case "FARMER" -> {
                int n = 0;
                for (BlockPos p : BlockPos.betweenClosed(h.settleAt.offset(-RADIUS, -4, -RADIUS), h.settleAt.offset(RADIUS, 4, RADIUS))) {
                    if (n >= 8) break;
                    BlockState s = l.getBlockState(p);
                    if (s.getBlock() instanceof CropBlock crop && crop.isMaxAge(s)) {
                        for (ItemStack d : Block.getDrops(s, l, p, null)) if (!store(cs, d)) Block.popResource(l, h.settleAt.above(), d);
                        l.setBlock(p, crop.getStateForAge(0), 2);
                        l.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0);
                        n++;
                    }
                }
                made(h, "crops harvested", n);
            }
            case "SMITH" -> {
                ItemStack iron = takeOne(cs, s -> s.is(Items.IRON_INGOT));
                if (!iron.isEmpty()) { store(cs, h.getRandom().nextBoolean() ? new ItemStack(Ui.item("kiyamet:metal_plate")) : new ItemStack(Ui.item("kiyamet:pistol_ammo"), 12)); made(h, "smithing jobs", 1); }
                else if (h.getRandom().nextInt(3) == 0) { store(cs, new ItemStack(Ui.item("kiyamet:pistol_ammo"), 3)); made(h, "rounds of scrap ammo", 3); }
                l.playSound(null, h.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 0.4f, 1.2f);
            }
            case "DOCTOR" -> {
                int healed = 0;
                for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, h.getBoundingBox().inflate(RADIUS), e -> (e instanceof Player || e instanceof HumanEntity o && o.ownerPlayer() == h.ownerPlayer()) && e.getHealth() < e.getMaxHealth())) {
                    e.heal(6); e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0)); healed++;
                }
                made(h, "patients treated", healed);
                if (h.getRandom().nextInt(4) == 0) { store(cs, new ItemStack(Ui.item("kiyamet:bandage"))); made(h, "bandages", 1); }
            }
            case "GUARD" -> {
                int seen = 0;
                for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, h.getBoundingBox().inflate(RADIUS + 16), e -> e instanceof Enemy && e.isAlive())) { e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1200, 0, false, false)); seen++; }
                if (seen > 0 && h.ownerPlayer() instanceof ServerPlayer o && o.blockPosition().closerThan(h.settleAt, 64))
                    o.displayClientMessage(Component.literal("Guard " + h.getName().getString() + ": " + seen + " walker" + (seen > 1 ? "s" : "") + " near the settlement!").withStyle(ChatFormatting.GOLD), true);
            }
            case "COOK" -> {
                int n = 0;
                for (int i = 0; i < 6; i++) {
                    ItemStack raw = takeOne(cs, s -> cooked(s.getItem()) != null);
                    if (raw.isEmpty()) break;
                    store(cs, new ItemStack(cooked(raw.getItem()))); n++;
                }
                if (n > 0) l.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, h.getX(), h.getY() + 2, h.getZ(), 2, 0.1, 0.1, 0.1, 0.01);
                made(h, "meals cooked", n);
            }
            default -> {}
        }
    }

    static void morning(ServerPlayer owner, List<HumanEntity> settlers) {
        if (settlers.isEmpty()) return;
        ServerLevel l = (ServerLevel) settlers.get(0).level();
        BlockPos bell = settlers.get(0).settleAt;
        List<Container> cs = chests(l, bell);
        int beds = beds(l, bell), fed = 0;
        for (HumanEntity h : settlers) {
            ItemStack f = takeOne(cs, Settlement::isFood);
            if (!f.isEmpty()) { fed++; h.morale = Math.min(100, h.morale + 6); h.heal(4); }
            else h.morale = Math.max(0, h.morale - 15);
        }
        if (beds < settlers.size()) for (HumanEntity h : settlers) h.morale = Math.max(0, h.morale - 8);
        else for (HumanEntity h : settlers) h.morale = Math.min(100, h.morale + 3);
        if (owner == null) return;
        float avg = 0; for (HumanEntity h : settlers) avg += h.morale; avg /= settlers.size();
        owner.sendSystemMessage(Component.literal("🏘 Settlement report: " + settlers.size() + " settlers · fed " + fed + "/" + settlers.size() + " · beds " + beds + "/" + settlers.size()
                + " · morale " + Math.round(avg) + " · food left " + count(cs, Settlement::isFood)).withStyle(ChatFormatting.GREEN));
        Map<String, Integer> m = MADE.remove(owner.getUUID());
        if (m != null && !m.isEmpty()) {
            StringBuilder b = new StringBuilder("   Yesterday: ");
            m.forEach((k, v) -> b.append(v).append(' ').append(k).append(", "));
            owner.sendSystemMessage(Component.literal(b.substring(0, b.length() - 2)).withStyle(ChatFormatting.GRAY));
        }
        if (fed < settlers.size()) owner.sendSystemMessage(Component.literal("   Not enough food in the settlement chests!").withStyle(ChatFormatting.RED));
        if (beds < settlers.size()) owner.sendSystemMessage(Component.literal("   Your people need more beds.").withStyle(ChatFormatting.RED));
    }

    /** Companion panel button: settle at the nearest bell, or leave the settlement */
    public static void toggle(ServerPlayer p, HumanEntity h) {
        if (!h.work.isEmpty()) {
            h.work = ""; h.settleAt = null; h.setOrder(HumanEntity.Order.FOLLOW);
            p.displayClientMessage(Component.literal(h.getName().getString() + " left the settlement and follows you again.").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        BlockPos bell = null; double best = 1e9;
        for (BlockPos q : BlockPos.betweenClosed(h.blockPosition().offset(-48, -12, -48), h.blockPosition().offset(48, 12, 48)))
            if (h.level().getBlockState(q).getBlock() instanceof BellBlock) { double d = q.distSqr(h.blockPosition()); if (d < best) { best = d; bell = q.immutable(); } }
        if (bell == null) { p.displayClientMessage(Component.literal("No Settlement Bell within 48 blocks. Build one at the Main Table.").withStyle(ChatFormatting.RED), true); return; }
        h.settleAt = bell; h.work = "FARMER";
        h.setHome(bell); h.setOrder(HumanEntity.Order.GUARD); h.setHome(bell);
        p.sendSystemMessage(Component.literal(h.getName().getString() + " settled at your settlement as a Farmer. Change their job at the bell.").withStyle(ChatFormatting.GREEN));
    }

    /** the bell panel: overview + every settler; click a settler to change their job */
    public static class Panel extends PanelMenu {
        final BlockPos bell;
        List<HumanEntity> people = List.of();
        static void open(ServerPlayer p, BlockPos bell) { p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new Panel(id, inv, p, bell), Component.literal("Settlement"))); }
        Panel(int id, Inventory inv, ServerPlayer p, BlockPos bell) { super(id, inv, p); this.bell = bell; redraw(); }

        @Override
        protected void draw() {
            ServerLevel l = (ServerLevel) player.level();
            people = l.getEntitiesOfClass(HumanEntity.class, new net.minecraft.world.phys.AABB(bell).inflate(96), h -> bell.equals(h.settleAt) && h.isOwnedBy(player));
            List<Container> cs = chests(l, bell);
            int beds = beds(l, bell), food = count(cs, Settlement::isFood);
            float avg = 0; for (HumanEntity h : people) avg += h.morale; if (!people.isEmpty()) avg /= people.size();
            c.setItem(4, Ui.glint(Ui.icon(BELL.asItem(), Ui.text("Your Settlement", ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Ui.text("Settlers: " + people.size(), ChatFormatting.WHITE),
                    Ui.text("Beds: " + beds + (beds < people.size() ? "  (need " + (people.size() - beds) + " more!)" : ""), beds < people.size() ? ChatFormatting.RED : ChatFormatting.GREEN),
                    Ui.text("Food in chests: " + food + (food < people.size() * 3 ? "  (running low)" : ""), food < people.size() * 3 ? ChatFormatting.RED : ChatFormatting.GREEN),
                    Ui.text("Chests linked: " + cs.size() + " (within " + CHEST_RANGE + " blocks)", ChatFormatting.GRAY),
                    Ui.text("Morale: " + Math.round(avg) + "/100", avg >= 60 ? ChatFormatting.GREEN : avg >= 35 ? ChatFormatting.YELLOW : ChatFormatting.RED),
                    Ui.text("Add settlers: companion panel → Settle here", ChatFormatting.DARK_GRAY)), true));
            for (int i = 0; i < people.size() && i < 36; i++) {
                HumanEntity h = people.get(i);
                int j = Math.max(0, Arrays.asList(JOBS).indexOf(h.work));
                c.setItem(9 + i, Ui.icon(JOB_ICONS[j], Ui.text(h.getName().getString() + " - " + title(JOBS[j]), ChatFormatting.AQUA, ChatFormatting.BOLD),
                        Ui.text(JOB_DESC[j], ChatFormatting.GRAY), Ui.text("Morale " + Math.round(h.morale) + " · Health " + (int) h.getHealth(), ChatFormatting.WHITE),
                        Ui.text("Click: next job", ChatFormatting.YELLOW)));
            }
            c.setItem(49, Ui.icon(Items.BARRIER, Ui.text("Close", ChatFormatting.RED)));
        }

        @Override
        protected boolean click(int slot, ClickType type) {
            if (slot == 49) { player.closeContainer(); return false; }
            int i = slot - 9;
            if (i < 0 || i >= people.size()) return false;
            HumanEntity h = people.get(i);
            int j = Math.max(0, Arrays.asList(JOBS).indexOf(h.work));
            h.work = JOBS[(j + 1) % JOBS.length];
            player.level().playSound(null, bell, SoundEvents.VILLAGER_WORK_TOOLSMITH, SoundSource.BLOCKS, 0.7f, 1f);
            return true;
        }
    }

    public static String title(String job) { return job.charAt(0) + job.substring(1).toLowerCase(); }

    private Settlement() {}
}
