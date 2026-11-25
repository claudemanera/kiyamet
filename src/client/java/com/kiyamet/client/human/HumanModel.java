package com.kiyamet.client.human;

import com.kiyamet.Kiyamet;
import com.kiyamet.npc.Faction;
import com.kiyamet.npc.HumanEntity;
import com.kiyamet.npc.Role;
import net.minecraft.resources.Identifier;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

/**
 * Survivors, community people and companions use the shared humanoid model with a painted skin (appearance code).
 * Enemies have their own hand-made designs: Raider, Brute, Raider Woman, Raider Commander and Masked One.
 */
public class HumanModel extends DefaultedEntityGeoModel<HumanEntity> {
    public static final DataTicket<Integer> APPEARANCE = DataTicket.create("kiyamet_appearance", Integer.class);
    public static final DataTicket<Integer> DESIGN = DataTicket.create("kiyamet_design", Integer.class);
    private static final String[] DESIGNS = {"human", "raider", "raider_commander", "masked_one", "raider_brute", "raider_woman"};

    public HumanModel() {
        super(Kiyamet.id("human"));
    }

    static int design(HumanEntity h) {
        Faction f = h.faction();
        if (f == Faction.RAIDER) {
            if (h.role() == Role.COMMANDER) return 2;
            if (h.appearance().female()) return 5;
            return (h.appearanceCode() >> 4 & 3) == 0 ? 4 : 1;      // about a quarter of the men are brutes
        }
        if (f == Faction.WHISPERER) return 3;
        return 0;
    }

    @Override
    public void addAdditionalStateData(HumanEntity animatable, Object relatedObject, GeoRenderState state) {
        super.addAdditionalStateData(animatable, relatedObject, state);
        state.addGeckolibData(APPEARANCE, animatable.appearanceCode());
        state.addGeckolibData(DESIGN, design(animatable));
    }

    @Override
    public Identifier getModelResource(GeoRenderState state) {
        int d = state.getOrDefaultGeckolibData(DESIGN, 0);
        return d == 0 ? super.getModelResource(state) : buildFormattedModelPath(Kiyamet.id(DESIGNS[d]));
    }

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        int d = state.getOrDefaultGeckolibData(DESIGN, 0);
        if (d != 0) return buildFormattedTexturePath(Kiyamet.id(DESIGNS[d]));
        return HumanSkins.texture(state.getOrDefaultGeckolibData(APPEARANCE, 0));
    }

    @Override
    public Identifier getAnimationResource(HumanEntity animatable) {
        int d = design(animatable);
        return d == 0 ? super.getAnimationResource(animatable) : buildFormattedAnimationPath(Kiyamet.id(DESIGNS[d]));
    }
}
