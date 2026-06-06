package gcewing.sgcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import gcewing.sgcraft.block.entity.DHDBlockEntity;
import gcewing.sgcraft.registry.ModBlocks;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3fc;
import java.util.function.Consumer;

public class DHDItemRenderer implements NoDataSpecialModelRenderer {
    private final DHDBlockEntity dummy = new DHDBlockEntity(BlockPos.ZERO, ModBlocks.STARGATE_CONTROLLER.get().defaultBlockState());
    private final DHDBlockEntityRenderer blockEntityRenderer = new DHDBlockEntityRenderer(null);

    public DHDItemRenderer() {
    }

    @Override
    public void submit(ItemDisplayContext context, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean flag, int i) {
        net.minecraft.client.renderer.OrderedSubmitNodeCollector ordered = collector.order(light);
        blockEntityRenderer.renderInternal(dummy, 0f, poseStack, ordered, light, overlay);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
        consumer.accept(new org.joml.Vector3f(0f, 0f, 0f));
        consumer.accept(new org.joml.Vector3f(1f, 1f, 1f));
    }

    public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

    public static class Unbaked implements SpecialModelRenderer.Unbaked {
        @Override
        public SpecialModelRenderer<?> bake(net.minecraft.client.renderer.special.SpecialModelRenderer.BakingContext context) {
            return new DHDItemRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
            return CODEC;
        }
    }
}
