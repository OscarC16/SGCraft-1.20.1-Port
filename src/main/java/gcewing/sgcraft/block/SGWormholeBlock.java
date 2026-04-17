package gcewing.sgcraft.block;

import gcewing.sgcraft.block.entity.SGBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.Vec3;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import net.minecraft.server.level.TicketType;

/**
 * The event horizon block of the Stargate.
 * Handles entity collision and teleportation to the linked destination.
 */
public class SGWormholeBlock extends Block {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    // Very thin shapes (1 pixel thick)
    protected static final VoxelShape X_AXIS_SHAPE = Block.box(7.5D, 0.0D, 0.0D, 8.5D, 16.0D, 16.0D);
    protected static final VoxelShape Z_AXIS_SHAPE = Block.box(0.0D, 0.0D, 7.5D, 16.0D, 16.0D, 8.5D);

    public SGWormholeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.INVISIBLE; // Rendering is handled by the Stargate Base Renderer
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
            @NotNull CollisionContext context) {
        // Use thin shape instead of empty to ensure entityInside triggers reliably in
        // 1.20
        return state.getValue(AXIS) == Direction.Axis.Z ? Z_AXIS_SHAPE : X_AXIS_SHAPE;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level,
            @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return Shapes.empty(); // No physical collision, allow walking through
    }

    @Override
    public @NotNull VoxelShape getOcclusionShape(@NotNull BlockState state, @NotNull BlockGetter level,
            @NotNull BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public void entityInside(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
            @NotNull Entity entity) {
        if (!level.isClientSide && !entity.isPassenger() && !entity.isVehicle() && entity.canChangeDimensions()) {
            LOGGER.debug("[Wormhole] Entity {} inside at {}", entity.getName().getString(), pos);
            SGBaseBlockEntity base = findBase(level, pos);
            if (base != null) {
                if (base.state == gcewing.sgcraft.SGState.Connected) {
                    teleportEntity(entity, base);
                } else {
                    // Log state mismatches only occasionally to avoid spam
                    if (level.getGameTime() % 20 == 0)
                        LOGGER.debug("[Wormhole] Base found at {} but state is {}", base.getBlockPos(), base.state);
                }
            } else {
                if (level.getGameTime() % 20 == 0)
                    LOGGER.warn("[Wormhole] No base found for wormhole at {}", pos);
            }
        }
    }

    private SGBaseBlockEntity findBase(Level level, BlockPos pos) {
        // Search downwards for the SGBaseBlock - wormholes are always 2-4 blocks above
        // base
        for (int i = 1; i <= 4; i++) {
            BlockPos p = pos.below(i);
            if (level.getBlockEntity(p) instanceof SGBaseBlockEntity base) {
                return base;
            }
        }
        return null;
    }

    private void teleportEntity(Entity entity, SGBaseBlockEntity base) {
        if (base.targetPos == null || base.targetDimension == null || entity.level().isClientSide)
            return;

        ServerLevel destLevel = entity.getServer().getLevel(base.targetDimension);
        if (destLevel == null)
            return;

        // Ensure target area is loaded
        destLevel.getChunkSource().addRegionTicket(TicketType.PORTAL,
                new net.minecraft.world.level.ChunkPos(base.targetPos), 3, base.targetPos);

        // Calculate destination data
        Direction destFacing = Direction.NORTH;
        BlockState destState = destLevel.getBlockState(base.targetPos);
        if (destState.getBlock() instanceof SGBaseBlock) {
            destFacing = destState.getValue(SGBaseBlock.FACING);
        } else if (destLevel.getBlockEntity(base.targetPos) instanceof SGBaseBlockEntity targetBE) {
            destFacing = targetBE.getBlockState().getValue(SGBaseBlock.FACING);
        }

        final double destX = base.targetPos.getX() + 0.5 + destFacing.getStepX() * 1.0;
        final double destY = base.targetPos.getY() + 1.15;
        final double destZ = base.targetPos.getZ() + 0.5 + destFacing.getStepZ() * 1.0;

        Direction enterFacing = base.getBlockState().getValue(SGBaseBlock.FACING);
        float deltaYaw = destFacing.toYRot() - enterFacing.toYRot() + 180.0f;

        // Safe Deferred Execution: Move the command out of the collision/physics tick
        MinecraftServer server = entity.getServer();
        if (server != null) {
            float targetYaw = entity.getYRot() + deltaYaw;
            float targetPitch = entity.getXRot();

            final String targetDim = base.targetDimension.location().toString();
            final String uuid = entity.getStringUUID();
            final String cmd = String.format(Locale.ROOT, "execute in %s run teleport %s %.3f %.3f %.3f %.3f %.3f",
                    targetDim, uuid, destX, destY, destZ, targetYaw, targetPitch);

            LOGGER.info("Stargate [Command TP] executing: {}", cmd);

            server.execute(() -> {
                CommandSourceStack source = server.createCommandSourceStack()
                        .withPermission(4)
                        .withSuppressedOutput();

                server.getCommands().performPrefixedCommand(source, cmd);
            });

            entity.setPortalCooldown();
        }

        entity.setDeltaMovement(Vec3.ZERO);
        entity.resetFallDistance();
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
