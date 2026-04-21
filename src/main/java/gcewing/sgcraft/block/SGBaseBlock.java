package gcewing.sgcraft.block;

import gcewing.sgcraft.block.entity.SGBaseBlockEntity;
import gcewing.sgcraft.block.entity.SGRingBlockEntity;
import gcewing.sgcraft.block.entity.DHDBlockEntity;
import gcewing.sgcraft.registry.ModBlockEntities;
import gcewing.sgcraft.registry.ModBlocks;
import gcewing.sgcraft.world.inventory.SGBaseMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/**
 * Stargate Base Block — the center-bottom block of the Stargate multiblock
 * structure.
 * When placed, it checks surrounding blocks for the correct ring/chevron
 * pattern.
 * If the pattern matches, it "merges" the structure and the 3D Stargate ring is
 * rendered.
 *
 * Pattern (viewed from front, base at position [B]):
 * Col: -2 -1 0 +1 +2
 * Row4: 2 1 2 1 2 (top)
 * Row3: 1 0 0 0 1
 * Row2: 2 0 0 0 2
 * Row1: 1 0 0 0 1
 * Row0: 2 1 [B] 1 2 (bottom/base)
 *
 * 0 = Air, 1 = Ring block, 2 = Chevron block, [B] = this block
 */
public class SGBaseBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty MERGED = BooleanProperty.create("merged");
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    // Multiblock pattern: pattern[row][col] where row 0 is the base row
    // Indexed as pattern[4-j][2+i] in the original, here stored directly as
    // [row][col]
    static final int[][] PATTERN = {
            { 2, 1, 0, 1, 2 }, // Row 0 (base level) — center is the base block itself
            { 1, 0, 0, 0, 1 }, // Row 1
            { 2, 0, 0, 0, 2 }, // Row 2
            { 1, 0, 0, 0, 1 }, // Row 3
            { 2, 1, 2, 1, 2 }, // Row 4 (top)
    };

    public SGBaseBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(MERGED, false)
                .setValue(LIT, false));
    }

    private static final VoxelShape BASE_SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BASE_SHAPE;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BASE_SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MERGED, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SGBaseBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.SG_BASE_BLOCK_ENTITY.get(), SGBaseBlockEntity::tick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public int getLightBlock(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return 0; // Transparente a la luz
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return false;
    }

    @Override
    public float getShadeBrightness(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return 1.0F; // No proyectar sombras negras
    }

    @Override
    @SuppressWarnings("deprecation")
    public float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof SGBaseBlockEntity baseBE && baseBE.isActive()) {
            return 0.0F; // Unbreakable while active
        }
        return super.getDestroyProgress(state, player, world, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SGBaseBlockEntity baseBE) {
                // 1. Unlink DHD
                if (baseBE.isLinkedToController) {
                    BlockEntity controllerBE = level.getBlockEntity(baseBE.linkedControllerPos);
                    if (controllerBE instanceof DHDBlockEntity dhd) {
                        dhd.clearLinkToStargate();
                    }
                }
                // 1.5 Cleanup Iris blocks
                baseBE.removeIrisBlocks();
                // 2. Unmerge structure
                unmerge(level, pos);
                // 3. Drop inventory
                for (int i = 0; i < baseBE.inventory.getSlots(); i++) {
                    net.minecraft.world.item.ItemStack stack = baseBE.inventory.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter level,
            net.minecraft.core.BlockPos pos) {
        return true; // Dejar pasar la luz del sol
    }

    // --- Multiblock Structure Logic ---

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            checkForMerge(level, pos, state);
        }
    }



    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos,
            @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (!level.isClientSide) {
            net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
            BlockEntity be = level.getBlockEntity(pos);

            if (be instanceof SGBaseBlockEntity baseBE && baseBE.isMerged) {
                // Handle Chevron Upgrade
                if (stack.is(gcewing.sgcraft.registry.ModItems.SG_CHEVRON_UPGRADE.get())) {
                    if (baseBE.inventory.getStackInSlot(SGBaseBlockEntity.SLOT_CHEVRON_UPGRADE).isEmpty()) {
                        baseBE.inventory.setStackInSlot(SGBaseBlockEntity.SLOT_CHEVRON_UPGRADE, stack.split(1));
                        baseBE.hasChevronUpgrade = true;
                        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.NETHERITE_BLOCK_PLACE,
                                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                        level.sendBlockUpdated(pos, state, state, 3);
                        return InteractionResult.SUCCESS;
                    }
                }

                // Handle Iris Upgrade
                if (stack.is(gcewing.sgcraft.registry.ModItems.SG_IRIS_UPGRADE.get())) {
                    if (baseBE.inventory.getStackInSlot(SGBaseBlockEntity.SLOT_IRIS_UPGRADE).isEmpty()) {
                        baseBE.inventory.setStackInSlot(SGBaseBlockEntity.SLOT_IRIS_UPGRADE, stack.split(1));
                        baseBE.hasIrisUpgrade = true;
                        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.NETHERITE_BLOCK_PLACE,
                                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                        level.sendBlockUpdated(pos, state, state, 3);
                        return InteractionResult.SUCCESS;
                    }
                }

                // Toggle Iris (Sneak + Right click with empty hand)
                if (player.isShiftKeyDown() && stack.isEmpty() && baseBE.hasIrisUpgrade) {
                    baseBE.toggleIris();
                    return InteractionResult.SUCCESS;
                }

                // Open GUI if not holding an upgrade
                NetworkHooks.openScreen((ServerPlayer) player, new SimpleMenuProvider(
                        (id, inv, p) -> new SGBaseMenu(id, inv, baseBE, ContainerLevelAccess.create(level, pos)),
                        Component.literal("Stargate Address")), pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Check if all ring/chevron blocks are in the correct positions to form a
     * Stargate.
     * If so, merge them all into the structure.
     */
    public void checkForMerge(Level level, BlockPos basePos, BlockState state) {
        SGBaseBlockEntity baseBE = getBlockEntity(level, basePos);
        if (baseBE == null || baseBE.isMerged)
            return;

        Direction facing = state.getValue(FACING);

        // Verify all pattern positions
        for (int i = -2; i <= 2; i++) { // lateral (left-right)
            for (int j = 0; j <= 4; j++) { // vertical (up)
                if (i == 0 && j == 0)
                    continue; // Skip base block itself

                int expectedType = PATTERN[j][i + 2];
                if (expectedType == 0)
                    continue; // Air position, skip

                BlockPos ringPos = getWorldPos(basePos, facing, i, j);
                int actualType = getRingBlockType(level, ringPos);

                if (actualType != expectedType) {
                    return; // Pattern doesn't match, abort
                }
            }
        }

        // Pattern matches! Merge all blocks
        baseBE.setMerged(true);
        // Make the base block invisible via blockstate
        level.setBlock(basePos, state.setValue(MERGED, true), 3);

        for (int i = -2; i <= 2; i++) {
            for (int j = 0; j <= 4; j++) {
                if (i == 0 && j == 0)
                    continue;

                int expectedType = PATTERN[j][i + 2];
                if (expectedType == 0)
                    continue;

                BlockPos ringPos = getWorldPos(basePos, facing, i, j);
                mergeRingBlock(level, ringPos, basePos);
            }
        }
    }

    /**
     * Dissolve the merged structure — restore all ring blocks to individual state.
     */
    public void unmerge(Level level, BlockPos basePos) {
        SGBaseBlockEntity baseBE = getBlockEntity(level, basePos);
        if (baseBE == null)
            return;

        if (baseBE.isMerged) {
            baseBE.setMerged(false);
            // Restore the base block visibility
            BlockState currentState = level.getBlockState(basePos);
            if (currentState.getBlock() instanceof SGBaseBlock && currentState.getValue(MERGED)) {
                level.setBlock(basePos, currentState.setValue(MERGED, false), 3);
            }

            // Unmerge all surrounding ring blocks
            for (int i = -2; i <= 2; i++) {
                for (int j = -4; j <= 4; j++) {
                    for (int k = -2; k <= 2; k++) {
                        BlockPos rp = basePos.offset(i, j, k);
                        unmergeRingBlock(level, rp, basePos);
                    }
                }
            }
        }
    }

    /**
     * Convert local coordinates (i=lateral, j=vertical) to world position
     * based on the facing direction of the base block.
     */
    BlockPos getWorldPos(BlockPos basePos, Direction facing, int i, int j) {
        return switch (facing) {
            case NORTH -> basePos.offset(-i, j, 0);
            case SOUTH -> basePos.offset(i, j, 0);
            case WEST -> basePos.offset(0, j, -i);
            case EAST -> basePos.offset(0, j, i);
            default -> basePos.offset(i, j, 0);
        };
    }

    /**
     * Determine what type of ring block is at the given position.
     * 
     * @return 1 = ring, 2 = chevron, 0 = air, -1 = incompatible block
     */
    private int getRingBlockType(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (block == ModBlocks.STARGATE_RING.get()) {
            // Verify it's not already merged
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SGRingBlockEntity ringBE && ringBE.isMerged) {
                return -1; // Already part of another structure
            }
            return 1;
        }
        if (block == ModBlocks.STARGATE_CHEVRON.get()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SGRingBlockEntity ringBE && ringBE.isMerged) {
                return -1;
            }
            return 2;
        }
        return -1; // Not a valid ring block
    }

    private void mergeRingBlock(Level level, BlockPos pos, BlockPos basePos) {
        BlockState ringState = level.getBlockState(pos);
        Block block = ringState.getBlock();
        if (block instanceof SGRingBlock ringBlock) {
            // Update the blockstate to make the block invisible
            ringBlock.setMergedState(level, pos, true);
            // Update the block entity data
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SGRingBlockEntity ringBE) {
                ringBE.setMerged(true, basePos);
            }
        }
    }

    private void unmergeRingBlock(Level level, BlockPos ringPos, BlockPos basePos) {
        BlockState ringState = level.getBlockState(ringPos);
        Block block = ringState.getBlock();
        if (block instanceof SGRingBlock ringBlock) {
            BlockEntity be = level.getBlockEntity(ringPos);
            if (be instanceof SGRingBlockEntity ringBE) {
                if (ringBE.isMerged && ringBE.basePos.equals(basePos)) {
                    ringBE.setMerged(false, BlockPos.ZERO);
                    // Restore block visibility
                    ringBlock.setMergedState(level, ringPos, false);
                }
            }
        }
    }

    private SGBaseBlockEntity getBlockEntity(BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SGBaseBlockEntity)
            return (SGBaseBlockEntity) be;
        return null;
    }


    /**
     * Update the LIT property of all blocks in the Stargate structure.
     */
    public static void updateStructureLight(Level level, BlockPos basePos, boolean lit) {
        BlockState state = level.getBlockState(basePos);
        if (!(state.getBlock() instanceof SGBaseBlock baseBlock))
            return;

        // 1. Update Base Block
        if (state.getValue(LIT) != lit) {
            level.setBlock(basePos, state.setValue(LIT, lit), 3);
        }

        // 2. Update Ring/Chevron Blocks
        Direction facing = state.getValue(FACING);
        for (int i = -2; i <= 2; i++) {
            for (int j = 0; j <= 4; j++) {
                if (i == 0 && j == 0)
                    continue;

                if (PATTERN[j][i + 2] != 0) {
                    BlockPos ringPos = baseBlock.getWorldPos(basePos, facing, i, j);
                    BlockState rs = level.getBlockState(ringPos);
                    if (rs.hasProperty(SGRingBlock.LIT) && rs.getValue(SGRingBlock.LIT) != lit) {
                        level.setBlock(ringPos, rs.setValue(SGRingBlock.LIT, lit), 3);
                    }
                }
            }
        }
    }
}
