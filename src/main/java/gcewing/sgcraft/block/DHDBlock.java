package gcewing.sgcraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import gcewing.sgcraft.block.entity.DHDBlockEntity;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class DHDBlock extends BaseEntityBlock {
    public static final MapCodec<DHDBlock> CODEC = simpleCodec(DHDBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public DHDBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends DHDBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DHDBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return createTickerHelper(type, gcewing.sgcraft.registry.ModBlockEntities.DHD_BLOCK_ENTITY.get(),
                (l, p, s, be) -> be.tick(l, p, s));
    }

    protected static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return false;
    }

    @Override
    public net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        double hitY = hit.getLocation().y - pos.getY();
        if (hitY > 0.5) {
            if (level.isClientSide()) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof DHDBlockEntity dhd) {
                    gcewing.sgcraft.client.ClientScreenHelper.openDHDScreen(dhd);
                }
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        } else {
            if (!level.isClientSide()) {
                player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inv, p) -> new gcewing.sgcraft.world.inventory.DHDFuelMenu(id, inv, level.getBlockEntity(pos), net.minecraft.world.inventory.ContainerLevelAccess.create(level, pos)),
                    net.minecraft.network.chat.Component.literal("DHD Fuel")
                ), pos);
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
