package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.CampfireRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CampfireRenderer implements BlockEntityRenderer<CampfireBlockEntity, CampfireRenderState> {
    private static final float SIZE = 0.375F;
    private final ItemModelResolver itemModelResolver;

    public CampfireRenderer(BlockEntityRendererProvider.Context p_173602_) {
        this.itemModelResolver = p_173602_.itemModelResolver();
    }

    public CampfireRenderState createRenderState() {
        return new CampfireRenderState();
    }

    public void extractRenderState(
        CampfireBlockEntity p_445642_,
        CampfireRenderState p_446494_,
        float p_446709_,
        Vec3 p_445382_,
        ModelFeatureRenderer.@Nullable CrumblingOverlay p_446369_
    ) {
        BlockEntityRenderer.super.extractRenderState(p_445642_, p_446494_, p_446709_, p_445382_, p_446369_);
        p_446494_.facing = p_445642_.getBlockState().getValue(CampfireBlock.FACING);
        int i = (int)p_445642_.getBlockPos().asLong();
        p_446494_.items = new ArrayList<>();

        for (int j = 0; j < p_445642_.getItems().size(); j++) {
            ItemStackRenderState itemstackrenderstate = new ItemStackRenderState();
            this.itemModelResolver
                .updateForTopItem(itemstackrenderstate, p_445642_.getItems().get(j), ItemDisplayContext.FIXED, p_445642_.getLevel(), null, i + j);
            p_446494_.items.add(itemstackrenderstate);
        }
    }

    public void submit(CampfireRenderState p_447184_, PoseStack p_439883_, SubmitNodeCollector p_439365_, CameraRenderState p_451159_) {
        Direction direction = p_447184_.facing;
        List<ItemStackRenderState> list = p_447184_.items;

        for (int i = 0; i < list.size(); i++) {
            ItemStackRenderState itemstackrenderstate = list.get(i);
            if (!itemstackrenderstate.isEmpty()) {
                p_439883_.pushPose();
                p_439883_.translate(0.5F, 0.44921875F, 0.5F);
                Direction direction1 = Direction.from2DDataValue((i + direction.get2DDataValue()) % 4);
                float f = -direction1.toYRot();
                p_439883_.mulPose(Axis.YP.rotationDegrees(f));
                p_439883_.mulPose(Axis.XP.rotationDegrees(90.0F));
                p_439883_.translate(-0.3125F, -0.3125F, 0.0F);
                p_439883_.scale(0.375F, 0.375F, 0.375F);
                itemstackrenderstate.submit(p_439883_, p_439365_, p_447184_.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                p_439883_.popPose();
            }
        }
    }
}
