package com.kiyamet.client;

import com.kiyamet.Kiyamet;
import com.kiyamet.backpack.BackpackTier;
import com.kiyamet.backpack.Backpacks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/** Draws the worn backpack on the player's back (each of the 10 backpacks has its own colours). */
public class BackpackLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final ModelPart PACK;
    private static final Identifier[] TEX = new Identifier[BackpackTier.values().length];

    static {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("pack", CubeListBuilder.create().texOffs(0, 0).addBox(-4f, 1f, 2f, 8, 10, 4), PartPose.ZERO);
        PACK = LayerDefinition.create(mesh, 32, 32).bakeRoot().getChild("pack");
        for (BackpackTier t : BackpackTier.values()) TEX[t.ordinal()] = Kiyamet.id("textures/entity/backpack/" + t.id + ".png");
    }

    public BackpackLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(state.id);
        if (e == null || e.isInvisible()) return;
        int tier = e.getAttachedOrElse(Backpacks.WORN_TIER, -1);
        if (tier < 0 || tier >= TEX.length) return;
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        collector.submitModelPart(PACK, pose, RenderTypes.entityCutout(TEX[tier]), light, OverlayTexture.NO_OVERLAY, null);
        pose.popPose();
    }
}
