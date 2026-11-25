package com.kiyamet.client.human;

import com.kiyamet.Kiyamet;
import com.kiyamet.npc.HumanEntity;
import net.minecraft.resources.Identifier;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

/** Shared humanoid model; the texture comes from the entity's appearance code. */
public class HumanModel extends DefaultedEntityGeoModel<HumanEntity> {
    public static final DataTicket<Integer> APPEARANCE = DataTicket.create("kiyamet_appearance", Integer.class);

    public HumanModel() {
        super(Kiyamet.id("human"));
    }

    @Override
    public void addAdditionalStateData(HumanEntity animatable, Object relatedObject, GeoRenderState state) {
        super.addAdditionalStateData(animatable, relatedObject, state);
        state.addGeckolibData(APPEARANCE, animatable.appearanceCode());
    }

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        Integer code = state.getOrDefaultGeckolibData(APPEARANCE, 0);
        return HumanSkins.texture(code);
    }
}
