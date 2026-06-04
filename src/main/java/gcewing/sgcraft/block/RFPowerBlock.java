package gcewing.sgcraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import gcewing.sgcraft.block.entity.RFPowerBlockEntity;
import gcewing.sgcraft.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

public class RFPowerBlock extends BaseEntityBlock {
    public static final MapCodec<RFPowerBlock> CODEC = simpleCodec(RFPowerBlock::new);

    public RFPowerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends RFPowerBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RFPowerBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RF_POWER_UNIT_BLOCK_ENTITY.get(),
                (l, p, s, be) -> be.tick(l, p, s));
    }
}
