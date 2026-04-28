package gcewing.sgcraft.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import com.mojang.serialization.MapCodec;

public class SGRingBlock extends Block {
    public static final MapCodec<SGRingBlock> CODEC = simpleCodec(SGRingBlock::new);
    public static final BooleanProperty MERGED = BooleanProperty.create("merged");

    public SGRingBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(MERGED, false));
    }

    @Override
    protected MapCodec<? extends SGRingBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MERGED);
    }
}
