package gcewing.sgcraft.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import gcewing.sgcraft.block.entity.SGBaseBlockEntity;
import gcewing.sgcraft.block.entity.SGRingBlockEntity;
import gcewing.sgcraft.registry.ModBlockEntities;
import gcewing.sgcraft.registry.ModBlocks;
import org.jetbrains.annotations.Nullable;

public class SGBaseBlock extends BaseEntityBlock {
    public static final MapCodec<SGBaseBlock> CODEC = simpleCodec(SGBaseBlock::new);

    static final int[][] PATTERN = {
        { 2, 1, 0, 1, 2 }, // Row 0 (base level)
        { 1, 0, 0, 0, 1 }, // Row 1
        { 2, 0, 0, 0, 2 }, // Row 2
        { 1, 0, 0, 0, 1 }, // Row 3
        { 2, 1, 2, 1, 2 }, // Row 4 (top)
    };

    public SGBaseBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(SGBlockStates.FACING, Direction.NORTH)
            .setValue(SGBlockStates.MERGED, false)
            .setValue(SGBlockStates.LIT, false));
    }

    @Override
    protected MapCodec<? extends SGBaseBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SGBlockStates.FACING, SGBlockStates.MERGED, SGBlockStates.LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(SGBlockStates.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SGBaseBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.SG_BASE_BLOCK_ENTITY.get(), SGBaseBlockEntity::tick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        if (state.getValue(SGBlockStates.MERGED))
            return RenderShape.INVISIBLE;
        return RenderShape.MODEL;
    }

    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            checkForMerge(level, pos, state);
        }
    }




    @Override
    public float getDestroyProgress(BlockState state, net.minecraft.world.entity.player.Player player, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SGBaseBlockEntity te && te.isActive()) {
            return 0.0f; // Bedrock-like while active
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return super.getShape(state, level, pos, context);
    }


    @Override
    public net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (!level.isClientSide()) {
            net.minecraft.world.item.ItemStack stack = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SGBaseBlockEntity baseBE && baseBE.isMerged) {
                // Handle Chevron Upgrade
                if (stack.is(gcewing.sgcraft.registry.ModItems.SG_CHEVRON_UPGRADE.get())) {
                    if (baseBE.inventory.getStackInSlot(SGBaseBlockEntity.SLOT_CHEVRON_UPGRADE).isEmpty()) {
                        baseBE.inventory.setStackInSlot(SGBaseBlockEntity.SLOT_CHEVRON_UPGRADE, stack.split(1));
                        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.NETHERITE_BLOCK_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                        return net.minecraft.world.InteractionResult.SUCCESS;
                    }
                }

                // Handle Iris Upgrade
                if (stack.is(gcewing.sgcraft.registry.ModItems.SG_IRIS_UPGRADE.get())) {
                    if (baseBE.inventory.getStackInSlot(SGBaseBlockEntity.SLOT_IRIS_UPGRADE).isEmpty()) {
                        baseBE.inventory.setStackInSlot(SGBaseBlockEntity.SLOT_IRIS_UPGRADE, stack.split(1));
                        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.NETHERITE_BLOCK_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                        return net.minecraft.world.InteractionResult.SUCCESS;
                    }
                }

                // Toggle Iris (Shift + Right click with empty hand)
                if (player.isShiftKeyDown() && stack.isEmpty() && baseBE.hasIrisUpgrade) {
                    baseBE.toggleIris();
                    return net.minecraft.world.InteractionResult.SUCCESS;
                }

                player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inv, p) -> new gcewing.sgcraft.world.inventory.SGBaseMenu(id, inv, baseBE, net.minecraft.world.inventory.ContainerLevelAccess.create(level, pos)),
                    net.minecraft.network.chat.Component.literal("Stargate Address")
                ), pos);
            }
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    public void checkForMerge(Level level, BlockPos basePos, BlockState state) {
        BlockEntity be = level.getBlockEntity(basePos);
        if (!(be instanceof SGBaseBlockEntity baseBE) || baseBE.isMerged) return;

        Direction facing = state.getValue(SGBlockStates.FACING);

        for (int i = -2; i <= 2; i++) {
            for (int j = 0; j <= 4; j++) {
                if (i == 0 && j == 0) continue;
                int expectedType = PATTERN[j][i + 2];
                if (expectedType == 0) continue;

                BlockPos ringPos = getWorldPos(basePos, facing, i, j);
                int actualType = getRingBlockType(level, ringPos);

                if (actualType != expectedType) return;
            }
        }

        baseBE.setMerged(true);
        level.setBlock(basePos, state.setValue(SGBlockStates.MERGED, true), 3);

        for (int i = -2; i <= 2; i++) {
            for (int j = 0; j <= 4; j++) {
                if (i == 0 && j == 0) continue;
                if (PATTERN[j][i + 2] != 0) {
                    BlockPos ringPos = getWorldPos(basePos, facing, i, j);
                    mergeRingBlock(level, ringPos, basePos);
                }
            }
        }
    }

    public void unmerge(Level level, BlockPos basePos) {
        BlockEntity be = level.getBlockEntity(basePos);
        if (!(be instanceof SGBaseBlockEntity baseBE) || !baseBE.isMerged) return;

        baseBE.setMerged(false);
        
        BlockState state = level.getBlockState(basePos);
        if (state.hasProperty(SGBlockStates.MERGED)) {
            level.setBlock(basePos, state.setValue(SGBlockStates.MERGED, false), 3);
        }

        // Unmerge all surrounding ring blocks (thorough 3D search like 1.20.1)
        for (int i = -2; i <= 2; i++) {
            for (int j = -4; j <= 4; j++) {
                for (int k = -2; k <= 2; k++) {
                    BlockPos rp = basePos.offset(i, j, k);
                    unmergeRingBlock(level, rp, basePos);
                }
            }
        }
    }

    BlockPos getWorldPos(BlockPos basePos, Direction facing, int i, int j) {
        return switch (facing) {
            case NORTH -> basePos.offset(-i, j, 0);
            case SOUTH -> basePos.offset(i, j, 0);
            case WEST -> basePos.offset(0, j, -i);
            case EAST -> basePos.offset(0, j, i);
            default -> basePos.offset(i, j, 0);
        };
    }

    private int getRingBlockType(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block == ModBlocks.STARGATE_RING.get() || block == ModBlocks.STARGATE_CHEVRON.get()) {
            return (block == ModBlocks.STARGATE_RING.get()) ? 1 : 2;
        }
        return 0;
    }

    private void mergeRingBlock(Level level, BlockPos pos, BlockPos basePos) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(SGBlockStates.MERGED)) {
            level.setBlock(pos, state.setValue(SGBlockStates.MERGED, true), 3);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SGRingBlockEntity ringBE) {
                ringBE.setMerged(true, basePos);
            }
        }
    }

    private void unmergeRingBlock(Level level, BlockPos pos, BlockPos basePos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SGRingBlock) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SGRingBlockEntity ringBE) {
                if (ringBE.isMerged && (ringBE.basePos.equals(basePos) || ringBE.basePos.equals(BlockPos.ZERO))) {
                    ringBE.setMerged(false, BlockPos.ZERO);
                    level.setBlock(pos, state.setValue(SGBlockStates.MERGED, false), 3);
                }
            }
        }
    }



    @Override
    public int getLightBlock(BlockState state) {
        return 0;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return false;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return 1.0F;
    }
}
