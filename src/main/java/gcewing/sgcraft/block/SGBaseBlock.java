package gcewing.sgcraft.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import com.mojang.serialization.MapCodec;

public class SGBaseBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<SGBaseBlock> CODEC = simpleCodec(SGBaseBlock::new);
    public static final BooleanProperty MERGED = BooleanProperty.create("merged");
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public SGBaseBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(MERGED, false)
            .setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends SGBaseBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MERGED, LIT);
    }
}
