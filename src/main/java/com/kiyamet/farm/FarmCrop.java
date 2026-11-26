package com.kiyamet.farm;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

/** A 1.6 crop: 8 growth stages, drops its produce (and seeds) via its loot table. */
public class FarmCrop extends CropBlock {
    final String seeds;
    public FarmCrop(Properties p, String seeds) { super(p); this.seeds = seeds; }
    @Override protected ItemLike getBaseSeedId() { return com.kiyamet.registry.ModItems.get(seeds); }
}
