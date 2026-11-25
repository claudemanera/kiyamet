package com.kiyamet.stone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class StoneEvents {
    private static final Map<UUID, List<ItemStack>> SOUL_KEEP = new HashMap<>();
    private static int tick;

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tick % 20 != 0) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) if (p.isAlive()) StonePowers.tickPassives(p);
            for (ServerLevel l : server.getAllLevels()) StonePowers.tickSpirits(l);
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
            if (source.getEntity() instanceof ServerPlayer p && entity != p && taken > 0) StonePowers.onPlayerHit(p, entity, taken);
        });
        // Soul Stone: keep the whole inventory on death.
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer p && StonePowers.active(p).contains(StoneKind.SOUL)) {
                List<ItemStack> saved = new ArrayList<>();
                for (int i = 0; i < p.getInventory().getContainerSize(); i++) saved.add(p.getInventory().getItem(i).copy());
                p.getInventory().clearContent();
                SOUL_KEEP.put(p.getUUID(), saved);
            }
            return true;
        });
        ServerPlayerEvents.COPY_FROM.register((oldP, newP, alive) -> {
            List<ItemStack> saved = SOUL_KEEP.remove(oldP.getUUID());
            if (saved == null || alive) return;
            for (int i = 0; i < saved.size() && i < newP.getInventory().getContainerSize(); i++) newP.getInventory().setItem(i, saved.get(i));
        });
    }

    private StoneEvents() {}
}
