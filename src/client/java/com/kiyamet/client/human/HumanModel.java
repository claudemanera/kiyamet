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
    public static final DataTicket<Integer> GEAR = DataTicket.create("kiyamet_gear", Integer.class);
    public static final DataTicket<Integer> DESIGN = DataTicket.create("kiyamet_design", Integer.class);
    private static final String[] DESIGNS = {"human", "raider", "raider_commander", "masked_one", "raider_brute", "raider_woman"};

    public HumanModel() {
        super(Kiyamet.id("human"));
    }

    static int design(HumanEntity h) {
        return switch (h.profession()) {
            case RAIDER -> 1; case COMMANDER -> 2; case MASKED_ONE -> 3; case BRUTE -> 4; case RAIDER_WOMAN -> 5;
            default -> h.faction() == Faction.RAIDER ? (h.role() == Role.COMMANDER ? 2 : 1) : h.faction() == Faction.WHISPERER ? 3 : 0;
        };
    }

    @Override
    public void addAdditionalStateData(HumanEntity animatable, Object relatedObject, GeoRenderState state) {
        super.addAdditionalStateData(animatable, relatedObject, state);
        state.addGeckolibData(APPEARANCE, animatable.appearanceCode());
        state.addGeckolibData(DESIGN, design(animatable));
        state.addGeckolibData(GEAR, animatable.gearCode());
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
        return HumanSkins.texture(state.getOrDefaultGeckolibData(APPEARANCE, 0), state.getOrDefaultGeckolibData(GEAR, 0));
    }

    @Override
    public Identifier getAnimationResource(HumanEntity animatable) {
        int d = design(animatable);
        return d == 0 ? super.getAnimationResource(animatable) : buildFormattedAnimationPath(Kiyamet.id(DESIGNS[d]));
    }
}
