package dev.halo.render;

import dev.halo.module.ModuleManager;
import dev.halo.module.Modules;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;

/** Client-side 3D hats drawn on top of your player's head. */
public class HatFeature extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    private static final Identifier TEX = Identifier.of("halo", "textures/entity/hat.png");
    private static final ModelPart[] PARTS = new ModelPart[5];

    public HatFeature(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) { super(context); }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void register() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (type == EntityType.PLAYER) helper.register(new HatFeature((FeatureRendererContext) renderer));
        });
    }

    // y is negative = up. The top of the head is at y = -8; hats start a hair above it to avoid z-fighting.
    private static ModelPart build(int type) {
        ModelPartBuilder b = ModelPartBuilder.create().uv(0, 0);
        switch (type) {
            case 0 -> { // top hat
                b.cuboid(-6f, -9.1f, -6f, 12, 1, 12);
                b.cuboid(-4f, -16.1f, -4f, 8, 7, 8);
            }
            case 1 -> { // cap
                b.cuboid(-4.5f, -12.1f, -4.5f, 9, 4, 9);
                b.cuboid(-4.5f, -9.1f, -8.5f, 9, 1, 4);
            }
            case 2 -> { // wizard
                b.cuboid(-6.5f, -9.1f, -6.5f, 13, 1, 13);
                b.cuboid(-4.5f, -13.1f, -4.5f, 9, 4, 9);
                b.cuboid(-3.5f, -16.1f, -3.5f, 7, 3, 7);
                b.cuboid(-2.5f, -19.1f, -2.5f, 5, 3, 5);
                b.cuboid(-1.5f, -22.1f, -1.5f, 3, 3, 3);
                b.cuboid(-0.5f, -25.1f, -0.5f, 1, 3, 1);
            }
            case 3 -> { // crown
                b.cuboid(-4.5f, -11.1f, -4.5f, 9, 3, 9);
                for (float sx : new float[] { -4.5f, 2.5f }) for (float sz : new float[] { -4.5f, 2.5f }) b.cuboid(sx, -14.1f, sz, 2, 3, 2);
                b.cuboid(-1f, -14.1f, -4.5f, 2, 3, 2);
                b.cuboid(-1f, -14.1f, 2.5f, 2, 3, 2);
                b.cuboid(-4.5f, -14.1f, -1f, 2, 3, 2);
                b.cuboid(2.5f, -14.1f, -1f, 2, 3, 2);
            }
            default -> { // halo
                b.cuboid(-4f, -14f, -5f, 8, 1, 1);
                b.cuboid(-4f, -14f, 4f, 8, 1, 1);
                b.cuboid(-5f, -14f, -4f, 1, 1, 8);
                b.cuboid(4f, -14f, -4f, 1, 1, 8);
            }
        }
        ModelData md = new ModelData();
        md.getRoot().addChild("hat", b, ModelTransform.NONE);
        return TexturedModelData.of(md, 64, 64).createModel();
    }

    @Override
    public void render(MatrixStack m, OrderedRenderCommandQueue q, int light, PlayerEntityRenderState s, float yaw, float pitch) {
        Modules.Hats hats = ModuleManager.HATS;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (hats == null || !hats.enabled || mc.player == null || s.id != mc.player.getId()) return;

        int t = hats.type.index;
        if (PARTS[t] == null) PARTS[t] = build(t);

        m.push();
        m.multiply(new Quaternionf().rotationZYX(0f, (float) Math.toRadians(yaw), (float) Math.toRadians(pitch)));
        float sz = hats.size.f();
        m.translate(0f, -0.5f, 0f);
        m.scale(sz, sz, sz);
        m.translate(0f, 0.5f, 0f);
        q.submitModelPart(PARTS[t], m, RenderLayer.getEntityCutoutNoCull(TEX), light, OverlayTexture.DEFAULT_UV,
                null, 0xFF000000 | hats.color.rgb, null);
        m.pop();
    }
}
