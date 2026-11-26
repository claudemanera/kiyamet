package com.kiyamet.egg;

import com.kiyamet.registry.ModItems;
import java.util.ArrayList;
import java.util.List;

/** Spawn eggs for every walker, boss, robot and kind of person, plus car kits for every vehicle. */
public final class ModEggs {
    public static final List<String> EGGS = new ArrayList<>(), KITS = new ArrayList<>();
    public static final String[][] HUMANS = {{"survivor", "SURVIVOR", "MEMBER"}, {"companion", "COMPANION", "MEMBER"}, {"community_member", "COMMUNITY", "MEMBER"},
            {"community_guard", "COMMUNITY", "GUARD"}, {"trader", "COMMUNITY", "TRADER"}, {"raider", "RAIDER", "MEMBER"}, {"raider_guard", "RAIDER", "GUARD"}, {"masked_one", "WHISPERER", "MEMBER"}};

    public static void init() {
        for (com.kiyamet.entity.ZombieKind k : com.kiyamet.entity.ZombieKind.values()) egg(k.id, "entity:" + k.id);
        for (com.kiyamet.boss.BossKind k : com.kiyamet.boss.BossKind.values()) egg(k.id, "entity:" + k.id);
        for (com.kiyamet.robot.RobotKind k : com.kiyamet.robot.RobotKind.values()) egg(k.id, "entity:" + k.id);
        for (com.kiyamet.element.Element e : com.kiyamet.element.Element.values()) { egg(e.guardian(), "entity:" + e.guardian()); egg(e.sage(), "entity:" + e.sage()); }
        for (com.kiyamet.wild.WildKind k : com.kiyamet.wild.WildKind.values()) egg(k.id, "entity:" + k.id);
        for (String[] h : HUMANS) egg(h[0], "human:" + h[1] + ":" + h[2]);
        for (com.kiyamet.vehicle.VehicleKind k : com.kiyamet.vehicle.VehicleKind.values()) {
            String id = k.id + "_kit";
            ModItems.custom(id, p -> new TwdEggItem(p.stacksTo(1), "vehicle:" + k.name()));
            KITS.add(id);
        }
    }

    static void egg(String id, String what) {
        ModItems.custom(id + "_spawn_egg", p -> new TwdEggItem(p, what));
        EGGS.add(id);
    }

    private ModEggs() {}
}
