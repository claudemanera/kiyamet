package com.kiyamet.furniture;

import com.kiyamet.food.Spoilage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Storage for furniture (wardrobe, cabinets, fridge...) and the placed bags. Fridge: top 2 rows freezer, the rest cold. */
public class StorageBE extends BaseContainerBlockEntity {
    NonNullList<ItemStack> items;

    public StorageBE(BlockPos pos, BlockState state) {
        super(Furniture.STORAGE_BE, pos, state);
        items = NonNullList.withSize(size(state), ItemStack.EMPTY);
    }

    static int size(BlockState s) {
        if (s.getBlock() instanceof FurnitureBlock f) return f.def.slots();
        return 2;
    }

    public boolean fridge() { return getBlockState().getBlock() instanceof FurnitureBlock f && f.def.kind() == Furniture.Kind.FRIDGE; }

    void fridgeTick(Level level) {
        if (level.getGameTime() % 20 != 0) return;
        boolean changed = false;
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            if (s.isEmpty() || !Spoilage.spoils(s)) continue;
            Spoilage.stamp(level, s);
            Spoilage.delay(s, i < 18 ? 20 : 15);     // freezer: stopped; fridge: 4x slower
            changed = true;
        }
        if (changed) setChanged();
    }

    @Override protected Component getDefaultName() { return getBlockState().getBlock().getName(); }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> list) { items = list; }
    @Override public int getContainerSize() { return items.size(); }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        int rows = Math.max(1, items.size() / 9);
        MenuType<ChestMenu> t = switch (rows) { case 1 -> MenuType.GENERIC_9x1; case 2 -> MenuType.GENERIC_9x2; case 3 -> MenuType.GENERIC_9x3; case 4 -> MenuType.GENERIC_9x4; case 5 -> MenuType.GENERIC_9x5; default -> MenuType.GENERIC_9x6; };
        return new ChestMenu(t, id, inv, this, rows);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items = NonNullList.withSize(size(getBlockState()), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in, items);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, items);
    }
}
