package gcewing.sgcraft.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import gcewing.sgcraft.block.entity.SGRingBlockEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class SGRingBlock extends BaseEntityBlock {
    public static final MapCodec<SGRingBlock> CODEC = simpleCodec(SGRingBlock::new);

    public SGRingBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(SGBlockStates.MERGED, false)
            .setValue(SGBlockStates.LIT, false));
    }

    @Override
    protected MapCodec<? extends SGRingBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SGRingBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        if (state.getValue(SGBlockStates.MERGED))
            return RenderShape.INVISIBLE;
        return RenderShape.MODEL;
    }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return super.getShape(state, level, pos, context);
    }

    @Override
    public float getDestroyProgress(BlockState state, net.minecraft.world.entity.player.Player player, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SGRingBlockEntity ringBE && ringBE.isMerged) {
            net.minecraft.world.level.block.entity.BlockEntity baseBE = level.getBlockEntity(ringBE.basePos);
            if (baseBE instanceof gcewing.sgcraft.block.entity.SGBaseBlockEntity base && base.isActive()) {
                return 0.0f;
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            searchForBaseBlocks(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof SGRingBlockEntity ringBE && ringBE.isMerged) {
                    BlockState baseState = level.getBlockState(ringBE.basePos);
                    if (baseState.getBlock() instanceof SGBaseBlock baseBlock) {
                        baseBlock.unmerge(level, ringBE.basePos);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SGRingBlockEntity ringBE && ringBE.isMerged) {
            BlockPos basePos = ringBE.basePos;
            BlockState baseState = level.getBlockState(basePos);
            if (baseState.getBlock() instanceof SGBaseBlock baseBlock) {
                return baseBlock.useWithoutItem(baseState, level, basePos, player, hit);
            }
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    private void searchForBaseBlocks(Level level, BlockPos ringPos) {
        for (int i = -2; i <= 2; i++) {
            for (int j = -4; j <= 0; j++) {
                for (int k = -2; k <= 2; k++) {
                    BlockPos bp = ringPos.offset(i, j, k);
                    BlockState bpState = level.getBlockState(bp);
                    if (bpState.getBlock() instanceof SGBaseBlock sgBase) {
                        sgBase.checkForMerge(level, bp, bpState);
                    }
                }
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SGBlockStates.MERGED, SGBlockStates.LIT);
    }
}
