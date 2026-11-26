package com.kiyamet.vehicle;

import com.kiyamet.npc.Shop;
import com.kiyamet.skill.Skill;
import com.kiyamet.skill.SkillManager;
import com.kiyamet.ui.PanelMenu;
import com.kiyamet.ui.Ui;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Vehicle Workshop: build a car kit from parts. Place the kit on the ground to get the vehicle. */
public class WorkshopMenu extends PanelMenu {
    record Build(VehicleKind kind, int eng, Object[] parts) {}
    static final List<Build> BUILDS = List.of(
            new Build(VehicleKind.MOTORCYCLE, 60, new Object[]{"minecraft:iron_ingot", 8, "kiyamet:fuel_canister", 2, "kiyamet:circuit", 1, "minecraft:leather", 2}),
            new Build(VehicleKind.ATV, 100, new Object[]{"minecraft:iron_ingot", 12, "kiyamet:fuel_canister", 3, "kiyamet:circuit", 2, "minecraft:black_wool", 4}),
            new Build(VehicleKind.SEDAN, 160, new Object[]{"minecraft:iron_ingot", 20, "kiyamet:metal_plate", 4, "kiyamet:fuel_canister", 4, "kiyamet:circuit", 3, "minecraft:glass", 6}),
            new Build(VehicleKind.PICKUP, 220, new Object[]{"minecraft:iron_ingot", 24, "kiyamet:metal_plate", 6, "kiyamet:fuel_canister", 5, "kiyamet:circuit", 3, "minecraft:glass", 6}),
            new Build(VehicleKind.POLICE, 300, new Object[]{"minecraft:iron_ingot", 20, "kiyamet:metal_plate", 6, "kiyamet:fuel_canister", 5, "kiyamet:circuit", 4, "minecraft:redstone", 8}),
            new Build(VehicleKind.AMBULANCE, 340, new Object[]{"minecraft:iron_ingot", 20, "kiyamet:metal_plate", 6, "kiyamet:fuel_canister", 5, "kiyamet:circuit", 4, "kiyamet:medkit", 4}),
            new Build(VehicleKind.ARMORED_SUV, 500, new Object[]{"kiyamet:metal_plate", 16, "kiyamet:steel_block", 6, "kiyamet:fuel_canister", 6, "kiyamet:circuit", 5, "kiyamet:tech_core", 2}),
            new Build(VehicleKind.TAXI, 180, new Object[]{"minecraft:iron_ingot", 20, "kiyamet:metal_plate", 4, "kiyamet:fuel_canister", 4, "kiyamet:circuit", 3, "minecraft:yellow_dye", 4}),
            new Build(VehicleKind.JEEP, 240, new Object[]{"minecraft:iron_ingot", 22, "kiyamet:metal_plate", 6, "kiyamet:fuel_canister", 5, "kiyamet:circuit", 3, "minecraft:black_wool", 6}),
            new Build(VehicleKind.VAN, 260, new Object[]{"minecraft:iron_ingot", 26, "kiyamet:metal_plate", 6, "kiyamet:fuel_canister", 5, "kiyamet:circuit", 3, "minecraft:chest", 2}),
            new Build(VehicleKind.MUSCLE_CAR, 320, new Object[]{"minecraft:iron_ingot", 24, "kiyamet:metal_plate", 8, "kiyamet:fuel_canister", 6, "kiyamet:circuit", 4, "minecraft:black_dye", 4}),
            new Build(VehicleKind.SCHOOL_BUS, 360, new Object[]{"minecraft:iron_ingot", 40, "kiyamet:metal_plate", 10, "kiyamet:fuel_canister", 8, "kiyamet:circuit", 4, "minecraft:glass", 12}),
            new Build(VehicleKind.SPORTS_TURBO, 400, new Object[]{"minecraft:iron_ingot", 24, "kiyamet:metal_plate", 8, "kiyamet:fuel_canister", 6, "kiyamet:circuit", 6, "kiyamet:tech_core", 1}),
            new Build(VehicleKind.SUPERBIKE_VORTEX, 420, new Object[]{"minecraft:iron_ingot", 14, "kiyamet:metal_plate", 4, "kiyamet:fuel_canister", 4, "kiyamet:circuit", 4, "kiyamet:tech_core", 1}),
            new Build(VehicleKind.SPORTS_GT, 440, new Object[]{"minecraft:iron_ingot", 24, "kiyamet:metal_plate", 8, "kiyamet:fuel_canister", 6, "kiyamet:circuit", 6, "kiyamet:tech_core", 2}),
            new Build(VehicleKind.FIRE_TRUCK, 460, new Object[]{"minecraft:iron_ingot", 40, "kiyamet:metal_plate", 14, "kiyamet:fuel_canister", 8, "kiyamet:circuit", 5, "minecraft:water_bucket", 2}),
            new Build(VehicleKind.SPORTS_RS, 520, new Object[]{"minecraft:iron_ingot", 26, "kiyamet:metal_plate", 10, "kiyamet:fuel_canister", 6, "kiyamet:circuit", 8, "kiyamet:tech_core", 2}),
            new Build(VehicleKind.SUPERBIKE_BLAZE, 560, new Object[]{"minecraft:iron_ingot", 16, "kiyamet:metal_plate", 6, "kiyamet:fuel_canister", 4, "kiyamet:circuit", 6, "kiyamet:tech_core", 2}),
            new Build(VehicleKind.ARMORED_TRUCK, 750, new Object[]{"kiyamet:metal_plate", 24, "kiyamet:steel_block", 8, "kiyamet:fuel_canister", 8, "kiyamet:circuit", 6, "kiyamet:tech_core", 4, "kiyamet:energy_core", 2}));
    static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

    public static void open(ServerPlayer p) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new WorkshopMenu(id, inv, p), Component.literal("Vehicle Workshop")));
    }

    private WorkshopMenu(int id, Inventory inv, ServerPlayer p) { super(id, inv, p); redraw(); }

    boolean has(Build b) {
        for (int i = 0; i < b.parts.length; i += 2) if (Shop.count(player, (String) b.parts[i]) < (Integer) b.parts[i + 1]) return false;
        return true;
    }

    @Override
    protected void draw() {
        int eng = SkillManager.get(player).level(Skill.ENGINEERING);
        c.setItem(4, Ui.icon(Items.ANVIL, Ui.text("Vehicle Workshop", ChatFormatting.GOLD, ChatFormatting.BOLD), Ui.text("Your Engineering: " + eng, ChatFormatting.GRAY),
                Ui.text("Build a kit, then place it on the ground", ChatFormatting.DARK_GRAY)));
        for (int i = 0; i < BUILDS.size(); i++) {
            Build b = BUILDS.get(i);
            List<Component> lore = new ArrayList<>();
            boolean lvOk = eng >= b.eng, parts = has(b);
            lore.add(Ui.text("Needs Engineering " + b.eng, lvOk ? ChatFormatting.GREEN : ChatFormatting.RED));
            for (int j = 0; j < b.parts.length; j += 2) {
                String id = (String) b.parts[j]; int n = (Integer) b.parts[j + 1], have = Shop.count(player, id);
                lore.add(Component.literal((have >= n ? "✔ " : "✘ ") + n + "× ").append(new ItemStack(Ui.item(id)).getHoverName()).append(" (" + have + ")")
                        .withStyle(have >= n ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            }
            lore.add(Ui.text("Speed " + Math.round(b.kind.speed * 100) + " · Seats " + b.kind.seats.length + " · Armor " + b.kind.armor + " · Trunk " + b.kind.trunk, ChatFormatting.AQUA));
            lore.add(Ui.text(lvOk && parts ? "Click to build!" : "Not ready yet", lvOk && parts ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY));
            ItemStack icon = new ItemStack(Ui.item("kiyamet:" + b.kind.id + "_kit"));
            icon.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Ui.text(b.kind.title, ChatFormatting.GOLD, ChatFormatting.BOLD));
            c.setItem(SLOTS[i], Ui.glint(Ui.lore(icon, lore), lvOk && parts));
        }
        c.setItem(49, Ui.icon(Items.BARRIER, Ui.text("Close", ChatFormatting.RED)));
    }

    @Override
    protected boolean click(int slot, ClickType type) {
        if (slot == 49) { player.closeContainer(); return false; }
        for (int i = 0; i < SLOTS.length; i++) {
            if (slot != SLOTS[i]) continue;
            Build b = BUILDS.get(i);
            if (SkillManager.get(player).level(Skill.ENGINEERING) < b.eng || !has(b)) return false;
            for (int j = 0; j < b.parts.length; j += 2) Shop.take(player, (String) b.parts[j], (Integer) b.parts[j + 1]);
            Shop.give(player, new ItemStack(Ui.item("kiyamet:" + b.kind.id + "_kit")));
            SkillManager.addXp(player, Skill.ENGINEERING, 40 + b.eng / 4);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 1f);
            player.sendSystemMessage(Ui.text("Built: " + b.kind.title + " kit. Place it on the ground.", ChatFormatting.GOLD));
            return true;
        }
        return false;
    }
}
