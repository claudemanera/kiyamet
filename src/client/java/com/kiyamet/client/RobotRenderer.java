package com.kiyamet.client;

import com.kiyamet.Kiyamet;
import com.kiyamet.robot.RobotEntity;
import com.kiyamet.robot.RobotKind;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;

/** Enemy robots wear red; your own robot companions are painted blue. */
public class RobotRenderer<R extends LivingEntityRenderState & GeoRenderState> extends GeoEntityRenderer<RobotEntity, R> {
    static final DataTicket<Boolean> ALLY = DataTicket.create("kiyamet_robot_ally", Boolean.class);

    static class Model extends DefaultedEntityGeoModel<RobotEntity> {
        private final Identifier allyTex;

        Model(RobotKind kind) {
            super(Kiyamet.id(kind.id));
            this.allyTex = buildFormattedTexturePath(Kiyamet.id(kind.id + "_ally"));
        }

        @Override
        public void addAdditionalStateData(RobotEntity animatable, Object related, GeoRenderState state) {
            super.addAdditionalStateData(animatable, related, state);
            state.addGeckolibData(ALLY, animatable.isAlly());
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return state.getOrDefaultGeckolibData(ALLY, false) ? allyTex : super.getTextureResource(state);
        }
    }

    public RobotRenderer(EntityRendererProvider.Context ctx, RobotKind kind) {
        super(ctx, new Model(kind));
        withScale(kind.scale);
        this.shadowRadius = 0.5f * kind.scale;
    }
}
