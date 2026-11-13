package com.sandevistanmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sandevistanmod.robot.RoboDrone;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Modelo do Drone Sucateiro: corpo, olho e duas helices girando. */
public class DroneModel extends EntityModel<RoboDrone> {
    private final ModelPart root;
    private final ModelPart rotorA;
    private final ModelPart rotorB;

    public DroneModel(ModelPart root) {
        this.root = root;
        this.rotorA = root.getChild("rotor_a");
        this.rotorB = root.getChild("rotor_b");
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4f, -3f, -4f, 8f, 5f, 8f), PartPose.offset(0f, 20f, 0f));
        r.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(0, 13).addBox(-1.5f, -1.5f, -5f, 3f, 3f, 1f), PartPose.offset(0f, 19f, 0f));
        r.addOrReplaceChild("rotor_a", CubeListBuilder.create().texOffs(0, 18).addBox(-6f, 0f, -1f, 12f, 1f, 2f), PartPose.offset(0f, 16f, 0f));
        r.addOrReplaceChild("rotor_b", CubeListBuilder.create().texOffs(0, 22).addBox(-1f, 0f, -6f, 2f, 1f, 12f), PartPose.offset(0f, 15f, 0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(RoboDrone entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        rotorA.yRot = ageInTicks * 1.6f;
        rotorB.yRot = ageInTicks * 1.6f;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
