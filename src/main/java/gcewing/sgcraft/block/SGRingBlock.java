package gcewing.sgcraft.block;

import gcewing.sgcraft.block.entity.SGRingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stargate Ring Block — used for both ring and chevron positions in the Stargate structure.
 * When merged into a Stargate, this block becomes invisible via the MERGED blockstate property
 * (the base block renderer draws the 3D ring instead).
 * When standalone, it renders as a normal block using its block model.
 */
public class SGRingBlock extends BaseEntityBlock {

    /** Blockstate property: when true, the block model is invisible (merged into stargate) */
    public static final BooleanProperty MERGED = BooleanProperty.create("merged");
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    /** Whether this is a chevron variant (true) or a plain ring (false) */
    private final boolean isChevron;

    public SGRingBlock(BlockBehaviour.Properties properties, boolean isChevron) {
        super(properties);
        this.isChevron = isChevron;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(MERGED, false)
                .setValue(LIT, false));
    }

    public boolean isChevron() {
        return isChevron;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MERGED, LIT);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SGRingBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // When merged, the model is empty (invisible) — defined in blockstate JSON
        // When not merged, the model is the normal block cube
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
    public boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return true; // Dejar pasar la luz del sol
    }

    @Override
    @SuppressWarnings("deprecation")
    public float getDestroyProgress(BlockState state, Player player, net.minecraft.world.level.BlockGetter world, BlockPos pos) {
        if (state.getValue(MERGED)) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof SGRingBlockEntity rte && rte.isMerged) {
                BlockEntity baseBE = world.getBlockEntity(rte.basePos);
                if (baseBE instanceof gcewing.sgcraft.block.entity.SGBaseBlockEntity sbe && sbe.isActive()) {
                    return 0.0F; // Unbreakable while active
                }
            }
        }
        return super.getDestroyProgress(state, player, world, pos);
    }

    // --- Multiblock triggers ---

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            // When a ring block is placed, check nearby base blocks for potential merge
            searchForBaseBlocks(level, pos);
        }
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (state.getValue(MERGED)) {
            if (level.getBlockEntity(pos) instanceof SGRingBlockEntity rte && rte.isMerged) {
                BlockState baseState = level.getBlockState(rte.basePos);
                if (baseState.getBlock() instanceof SGBaseBlock baseBlock) {
                    return baseBlock.use(baseState, level, rte.basePos, player, hand, hit);
                }
            }
        }
        return InteractionResult.PASS; // Cambio clave: permitir que la interacción pase si no estamos fusionados
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            // Block is being destroyed (not just state change) — trigger unmerge
            if (!level.isClientSide) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof SGRingBlockEntity ringBE && ringBE.isMerged) {
                    BlockEntity bbe = level.getBlockEntity(ringBE.basePos);
                    if (bbe instanceof gcewing.sgcraft.block.entity.SGBaseBlockEntity sbe) {
                        sbe.removeIrisBlocks();
                    }
                    Block baseBlock = level.getBlockState(ringBE.basePos).getBlock();
                    if (baseBlock instanceof SGBaseBlock sgBase) {
                        sgBase.unmerge(level, ringBE.basePos);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * Search nearby positions for SGBaseBlocks and trigger their merge check.
     */
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

    // --- Called by SGBaseBlock during merge/unmerge ---

    /**
     * Sets the MERGED blockstate property, which switches the block model
     * between normal (visible cube) and empty (invisible).
     */
    public void setMergedState(Level level, BlockPos pos, boolean merged) {
        BlockState current = level.getBlockState(pos);
        if (current.getBlock() == this && current.getValue(MERGED) != merged) {
            level.setBlock(pos, current.setValue(MERGED, merged), 3);
        }
    }
}
