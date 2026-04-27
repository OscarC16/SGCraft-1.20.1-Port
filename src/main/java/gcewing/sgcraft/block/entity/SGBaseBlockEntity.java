package gcewing.sgcraft.block.entity;

import gcewing.sgcraft.SGAddressing;
import gcewing.sgcraft.SGState;
import gcewing.sgcraft.world.SGNetwork;
import gcewing.sgcraft.registry.ModBlockEntities;
import gcewing.sgcraft.registry.ModSounds;
import gcewing.sgcraft.block.SGBaseBlock;
import gcewing.sgcraft.block.SGIrisBlock;
import gcewing.sgcraft.block.SGRingBlock;
import gcewing.sgcraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.Direction;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;
import java.util.Locale;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;

/**
 * Tile entity for the Stargate Base block — the center-bottom of the multiblock
 * structure.
 * Manages the merge state, Stargate operational state, and chevron data.
 */
public class SGBaseBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Multiblock merge state
    public boolean isMerged = false;

    // Operational state
    public SGState state = SGState.Idle;

    // Chevron/dialling data
    public int numEngagedChevrons = 0;
    public boolean hasChevronUpgrade = false;
    public boolean hasIrisUpgrade = false;
    public boolean isIncoming = false;

    // Iris state
    public enum IrisState {
        OPEN, CLOSED, OPENING, CLOSING
    }

    public IrisState irisState = IrisState.OPEN;
    public float irisPhase = 1.0f; // 1.0 = Fully open, 0.0 = Fully closed
    public static final int IRIS_TIME = 45; // 2 seconds to open/close

    // Ring animation data
    public double ringAngle = 0;
    public double lastRingAngle = 0;
    public double targetRingAngle = 0;
    public double ringRotationSpeed = 0;
    public double startRingAngle = 0;
    public double totalRingRotationDelta = 0;
    public boolean isRingRotating = false;
    public boolean targetResponded = false;

    // Client-side exact animation tracking
    public boolean firstClientSync = true;
    public double previousTargetRingAngle = -9999;
    public int clientAnimationTicks = TICKS_FOR_RING;

    // Energy costs (Forge Energy)
    public static final int ENERGY_OPEN_WORMHOLE = 50000;
    public static final int ENERGY_MAINTAIN_WORMHOLE = 100;
    public static final double INTER_DIMENSION_MULTIPLIER = 10.0;
    public static final double DISTANCE_FACTOR = 0.01;

    // Master dial clock and state
    public int dialTicks = 0; // 0 to 29 for each symbol interval
    public static final int TICKS_PER_SYMBOL = 30;
    public static final int TICKS_FOR_RING = 20; // Ring rotation (1 second)
    public static final int TICKS_FOR_CHEVRON = 10; // Chevron engage (0.5 seconds)

    // Chevron animation data (0.0 to 1.0)
    public float[] chevronEngageAmount = new float[9];
    public boolean isChevronEngaging = false;

    // Chevron mapping
    private static final int[] CHEVRON_ORDER_7 = { 5, 6, 7, 1, 2, 3, 4 };
    private static final int[] CHEVRON_ORDER_9 = { 5, 6, 7, 8, 0, 1, 2, 3, 4 };

    // DHD controller link (for future use)
    public boolean isLinkedToController = false;
    public BlockPos linkedControllerPos = BlockPos.ZERO;

    // Internal Energy Storage (Closed system)
    public int energy = 0;
    public static final int MAX_ENERGY = 1000000;

    // Stargate Address data
    public String homeAddress = "";
    public String addressError = null;

    // Connection data
    public String dialledAddress = "";
    public ResourceKey<Level> targetDimension = null;
    public BlockPos targetPos = null;
    public int connectionTicks = 0;
    public static final int MAX_CONNECTION_TICKS = 2400; // 2 minutes

    // Event Horizon Geometry
    public static final int ehGridRadialSize = 5;
    public static final int ehGridPolarSize = 32;
    private double[][][] ehGrid;
    private static final double openingTransientIntensity = 1.3;
    private static final double openingTransientRandomness = 0.25;
    private static final double closingTransientRandomness = 0.25;
    private SGState lastState = SGState.Idle;

    // Inventory Slot Constants
    public static final int SLOT_CAMO_START = 0;
    public static final int SLOT_CAMO_COUNT = 5;
    public static final int SLOT_CHEVRON_UPGRADE = 5;
    public static final int SLOT_IRIS_UPGRADE = 6;
    public static final int TOTAL_SLOTS = 7;

    // Inventory for camouflage + upgrades
    public final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            updateUpgrades();
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };
    private final LazyOptional<IItemHandler> inventoryHolder = LazyOptional.of(() -> inventory);

    public SGBaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SG_BASE_BLOCK_ENTITY.get(), pos, state);
    }

    private void updateUpgrades() {
        hasChevronUpgrade = !inventory.getStackInSlot(SLOT_CHEVRON_UPGRADE).isEmpty();
        hasIrisUpgrade = !inventory.getStackInSlot(SLOT_IRIS_UPGRADE).isEmpty();
    }

    public int getNumChevrons() {
        if (!inventory.getStackInSlot(SLOT_CHEVRON_UPGRADE).isEmpty()) {
            return 9;
        }
        return hasChevronUpgrade ? 9 : 7;
    }

    public boolean isActive() {
        return state != SGState.Idle;
    }

    public void setMerged(boolean merged) {
        this.isMerged = merged;
        if (level != null && !level.isClientSide) {
            updateForcedChunk(merged);
            SGNetwork network = SGNetwork.get(level);
            if (merged) {
                updateHomeAddress();
                if (homeAddress != null && !homeAddress.isEmpty()) {
                    network.register(homeAddress, level.dimension(), worldPosition);
                }
                updateLight(); // Ensure light state is correct on merge
                // Re-place iris blocks if the structure is completed and iris was closed/closing
                if (hasIrisUpgrade && irisPhase < 1.0f) {
                    placeIrisBlocks();
                }
            } else {
                if (homeAddress != null && !homeAddress.isEmpty()) {
                    network.unregister(homeAddress);
                }
                this.homeAddress = "";
            }
            sync();
        }
    }


    public void updateForcedChunk(boolean force) {
        if (level == null || level.isClientSide)
            return;
        ServerLevel serverLevel = (ServerLevel) level;
        ChunkPos cp = new ChunkPos(worldPosition);
        serverLevel.setChunkForced(cp.x, cp.z, force);
        if (force) {
            LOGGER.info("Stargate at {} is now forcing chunk {} to stay loaded.", worldPosition, cp);
        } else {
            LOGGER.info("Stargate at {} released forced chunk {}.", worldPosition, cp);
        }
    }

    public void updateHomeAddress() {
        if (level != null && !level.isClientSide) {
            int dimIndex = getDimensionIndex(level);
            this.homeAddress = SGAddressing.addressForLocation(worldPosition, dimIndex);
            setChanged();
        }
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private int getDimensionIndex(Level level) {
        ResourceKey<Level> dim = level.dimension();
        if (dim == Level.OVERWORLD)
            return 0;
        if (dim == Level.NETHER)
            return 1;
        if (dim == Level.END)
            return 2;
        return (dim.location().hashCode() & Integer.MAX_VALUE) % SGAddressing.DIMENSION_RANGE;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SGBaseBlockEntity te) {
        if (level.isClientSide) {
            te.updateAnimation();
            return;
        }

        te.updateIris();
        if (te.state == SGState.Connected || te.state == SGState.Transient) {
            te.checkEntityInteractions();
        }

        if (te.state == SGState.Dialling) {
            int requiredChevrons = te.dialledAddress.length();

            if (te.numEngagedChevrons < requiredChevrons) {
                // Determine symbol and target angle
                char symbol = te.dialledAddress.charAt(te.numEngagedChevrons);
                int symbolIndex = SGAddressing.charToSymbol(symbol);
                int[] order = (requiredChevrons == 7) ? CHEVRON_ORDER_7 : CHEVRON_ORDER_9;
                int engagingChevronIndex = order[te.numEngagedChevrons];

                // Chevron 4 is top (0 deg visual offset). Each chevron is spaced by 40 degrees.
                // To rotate the symbol to the physical chevron, we add the relative physical
                // angle.
                double chevronAngleOffset = (4 - engagingChevronIndex) * 40.0;
                // The renderer draws segment i from angle i*theta to (i+1)*theta.
                // To center the segment exactly under the chevron, we must shift the ring
                // backwards by half a segment.
                // With 36 symbols, theta is 10 degrees, so half is 5 degrees.
                double textureAngleOffset = 95.0;
                double targetAngle = ((symbolIndex * 360.0) / SGAddressing.NUM_SYMBOLS) + chevronAngleOffset
                        + textureAngleOffset;

                // Sync start of symbol dialing
                if (te.dialTicks == 0) {
                    te.isRingRotating = true;
                    te.isChevronEngaging = false;

                    // Normalize current angle once at the start of dialing to prevent jumps
                    double oldAngle = te.ringAngle;
                    te.ringAngle %= 360;
                    if (te.ringAngle < 0)
                        te.ringAngle += 360;
                    double delta = te.ringAngle - oldAngle;
                    te.lastRingAngle += delta; // Compensate lastRingAngle to keep interpolation stable

                    // Stabilize rotation: calculate constant speed to avoid the "snap" at the end
                    double diff = targetAngle - te.ringAngle;
                    while (diff < -180)
                        diff += 360;
                    while (diff > 180)
                        diff -= 360;

                    te.startRingAngle = te.ringAngle;
                    te.totalRingRotationDelta = diff;
                    te.targetRingAngle = te.startRingAngle + te.totalRingRotationDelta; // Absolute target for the
                                                                                        // duration of this spin
                    te.ringRotationSpeed = diff / TICKS_FOR_RING;

                    level.playSound(null, pos, ModSounds.STARGATE_RING_START.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
                    te.sync();
                }

                // Persistent Handshake for incoming dialling (caller side)
                if (!te.isIncoming && !te.targetResponded && te.targetPos != null && te.targetDimension != null) {
                    SGNetwork.StargateLocation loc = new SGNetwork.StargateLocation(te.targetDimension, te.targetPos);
                    SGBaseBlockEntity target = te.findRemoteStargate(loc);
                    if (target != null) {
                        target.onIncomingDialling(te.homeAddress, level.dimension(), te.worldPosition,
                                te.dialledAddress.length());
                        te.targetResponded = true;
                        te.sync();
                    }
                }

                te.dialTicks++;

                // Phase 0: Regular Chunk Loading Refresh (Every 20 ticks)
                if (te.dialTicks % 20 == 0) {
                    te.refreshTickets();
                }

                // Phase 1: Ring Rotation
                if (te.dialTicks == TICKS_FOR_RING) {
                    te.isRingRotating = false;
                    te.isChevronEngaging = true;
                    te.ringAngle = te.targetRingAngle; // Server stores final position for chunk saving
                    te.sync();
                }

                // Phase 2: Chevron Engagement (28-39)
                if (te.dialTicks >= TICKS_PER_SYMBOL) {
                    te.isChevronEngaging = false;
                    te.numEngagedChevrons++;
                    te.dialTicks = 0;
                    // Chevron engage sound is part of the next symbol's start or final open

                    if (te.numEngagedChevrons >= requiredChevrons) {
                        te.state = SGState.Transient;
                        te.updateLight();
                        te.connectionTicks = 0;
                        te.spawnWormhole();
                        if (te.level instanceof ServerLevel sl) {
                            sl.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(te.worldPosition), 3,
                                    te.worldPosition);
                        }
                    }
                    te.sync();
                }
            }
        } else if (te.state == SGState.Transient) {
            te.connectionTicks++;
            // Apply Kawoosh physical effects during the expansion phase (ticks 2 to 30)
            if (te.connectionTicks > 2 && te.connectionTicks < 30) {
                te.performKawooshPhysicalEffects();
            }
            if (te.connectionTicks >= 40) {
                te.state = SGState.Connected;
                te.updateLight();
                te.connectionTicks = 0;
                te.sync();
            }
        } else if (te.state == SGState.Connected) {
            // Energy consumption (Every second) - Only for initiator
            if (!te.isIncoming && level.getGameTime() % 20 == 0) {
                double distance = 0;
                if (te.targetPos != null && te.targetDimension != null) {
                    if (level.dimension().equals(te.targetDimension)) {
                        distance = Math.sqrt(te.worldPosition.distSqr(te.targetPos));
                    } else {
                        distance = 1000; // Fixed penalty for dimensions
                    }
                }
                
                double costPerSecond = ENERGY_MAINTAIN_WORMHOLE * 20;
                if (te.targetDimension != null && !level.dimension().equals(te.targetDimension)) {
                    costPerSecond *= INTER_DIMENSION_MULTIPLIER;
                }
                costPerSecond += (distance * DISTANCE_FACTOR * 20);
                
                int totalCost = (int)costPerSecond;
                if (te.energy < totalCost) {
                    LOGGER.info("Stargate at {} shutting down due to power failure", te.worldPosition);
                    te.disconnect();
                } else {
                    te.energy -= totalCost;
                    te.sync();
                }
            }

            // Auto-close logic: 5 minutes max
            te.connectionTicks++;
            if (te.connectionTicks >= MAX_CONNECTION_TICKS) {
                LOGGER.info("Stargate at {} reached max connection time (5m), auto-disconnecting.", pos);
                te.disconnect();
            }
        } else if (te.state == SGState.Disconnecting) {
            te.connectionTicks++;
            if (te.connectionTicks >= 20) {
                te.state = SGState.Idle;
                te.updateLight();
                te.removeWormhole();
                te.numEngagedChevrons = 0;
                te.dialledAddress = "";
                te.targetDimension = null;
                te.targetPos = null;
                te.isRingRotating = false;
                te.isChevronEngaging = false;
                te.dialTicks = 0;
                te.connectionTicks = 0;
                te.targetResponded = false;
                te.sync();
            }
        }
    }

    private void updateAnimation() {
        lastRingAngle = ringAngle;
        updateIrisAnimation();

        // Detect state changes for transient animations
        // Detect state changes for transient animations
        if (state != lastState) {
            if (state == SGState.Transient) {
                initiateOpeningTransient();
            } else if (state == SGState.Disconnecting) {
                initiateClosingTransient();
            }
            lastState = state;
        }

        if (state == SGState.Dialling) {
            if (targetRingAngle != previousTargetRingAngle || state != lastState) {
                previousTargetRingAngle = targetRingAngle;
                clientAnimationTicks = 0;

                // Recalculate parameters natively on the client to guarantee zero jumps
                startRingAngle = ringAngle;
                double diff = targetRingAngle - ringAngle;
                while (diff < -180)
                    diff += 360;
                while (diff > 180)
                    diff -= 360;
                totalRingRotationDelta = diff;
            }

            if (clientAnimationTicks <= TICKS_FOR_RING) {
                // Sinusoidal easing (Ease-In/Out) completely predicted by client
                double t = (double) clientAnimationTicks / TICKS_FOR_RING;
                double smoothT = (1.0 - Math.cos(t * Math.PI)) / 2.0;
                ringAngle = startRingAngle + smoothT * totalRingRotationDelta;

                if (clientAnimationTicks < TICKS_FOR_RING) {
                    clientAnimationTicks++;
                }
            } else {
                ringAngle = targetRingAngle;
            }
        }

        // Update chevron movement based on mapped indices
        int requiredChevrons = dialledAddress.length();
        int[] order = (requiredChevrons == 7) ? CHEVRON_ORDER_7 : CHEVRON_ORDER_9;

        for (int i = 0; i < 9; i++) {
            boolean active = false;
            boolean engaging = false;

            // Find if this physical chevron index 'i' corresponds to an engaged or engaging
            // step
            for (int step = 0; step < order.length; step++) {
                if (order[step] == i) {
                    if (step < numEngagedChevrons)
                        active = true;
                    if (step == numEngagedChevrons && isChevronEngaging)
                        engaging = true;
                    break;
                }
            }

            float target = (active || engaging) ? 1.0f : 0.0f;
            float step = 0.5f; // Very fast movement for snappier feel
            if (chevronEngageAmount[i] < target)
                chevronEngageAmount[i] = Math.min(target, chevronEngageAmount[i] + step);
            else if (chevronEngageAmount[i] > target)
                chevronEngageAmount[i] = Math.max(target, chevronEngageAmount[i] - step);
        }

        // Event Horizon Animation
        if (state == SGState.Transient || state == SGState.Connected || state == SGState.Disconnecting) {
            applyRandomImpulse();
            updateEventHorizon();
        }
    }

    public DHDBlockEntity getLinkedDHD() {
        if (isLinkedToController && level != null) {
            BlockEntity be = level.getBlockEntity(linkedControllerPos);
            if (be instanceof DHDBlockEntity dhd) {
                return dhd;
            }
        }
        return null;
    }

    public void spawnWormhole() {
        if (level == null || level.isClientSide)
            return;
            
        // Consume opening energy
        int openingCost = ENERGY_OPEN_WORMHOLE;
        if (targetDimension != null && !level.dimension().equals(targetDimension)) {
            openingCost *= INTER_DIMENSION_MULTIPLIER;
        }
        this.energy = Math.max(0, this.energy - openingCost);
        sync();

        level.playSound(null, worldPosition, ModSounds.STARGATE_WORMHOLE_OPEN.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
    }

    public void removeWormhole() {
        // Sound is now played at the START of disconnection in disconnect().
        // This method handles any final cleanup after the animation completes.
    }

    public double[][][] getEventHorizonGrid() {
        if (ehGrid == null) {
            ehGrid = new double[2][ehGridPolarSize + 2][ehGridRadialSize + 1];
            for (int i = 0; i < 2; i++) {
                ehGrid[i][0] = ehGrid[i][ehGridPolarSize];
                ehGrid[i][ehGridPolarSize + 1] = ehGrid[i][1];
            }
        }
        return ehGrid;
    }

    public void initiateOpeningTransient() {
        double[][] v = getEventHorizonGrid()[1];
        for (int j = 0; j <= ehGridPolarSize + 1; j++) {
            v[j][0] = openingTransientIntensity;
            v[j][1] = v[j][0] + openingTransientRandomness * level.random.nextGaussian();
        }
    }

    public void initiateClosingTransient() {
        double[][] v = getEventHorizonGrid()[1];
        for (int i = 1; i < ehGridRadialSize; i++) {
            for (int j = 1; j <= ehGridPolarSize; j++) {
                v[j][i] += closingTransientRandomness * level.random.nextGaussian();
            }
        }
    }

    public void applyRandomImpulse() {
        double[][] v = getEventHorizonGrid()[1];
        int i = level.random.nextInt(ehGridRadialSize - 1) + 1;
        int j = level.random.nextInt(ehGridPolarSize) + 1;
        v[j][i] += 0.05 * level.random.nextGaussian();
    }

    public void updateEventHorizon() {
        double[][][] grid = getEventHorizonGrid();
        double[][] u = grid[0];
        double[][] v = grid[1];
        double dt = 1.0;
        double asq = 0.03;
        double d = 0.95;

        for (int i = 1; i < ehGridRadialSize; i++) {
            for (int j = 1; j <= ehGridPolarSize; j++) {
                double du_dr = 0.5 * (u[j][i + 1] - u[j][i - 1]);
                double d2u_drsq = u[j][i + 1] - 2 * u[j][i] + u[j][i - 1];
                double d2u_dthsq = u[j + 1][i] - 2 * u[j][i] + u[j - 1][i];
                v[j][i] = d * v[j][i] + (asq * dt) * (d2u_drsq + du_dr / i + d2u_dthsq / (i * i));
            }
        }

        for (int i = 1; i < ehGridRadialSize; i++) {
            for (int j = 1; j <= ehGridPolarSize; j++) {
                u[j][i] += v[j][i] * dt;
            }
        }

        double u0 = 0, v0 = 0;
        for (int j = 1; j <= ehGridPolarSize; j++) {
            u0 += u[j][1];
            v0 += v[j][1];
        }
        u0 /= ehGridPolarSize;
        v0 /= ehGridPolarSize;

        for (int j = 1; j <= ehGridPolarSize; j++) {
            u[j][0] = u0;
            v[j][0] = v0;
        }

        // Wrap-around for polar coordinates
        for (int i = 0; i < 2; i++) {
            grid[i][0] = grid[i][ehGridPolarSize];
            grid[i][ehGridPolarSize + 1] = grid[i][1];
        }
    }

    public void refreshTickets() {
        if (level == null || level.isClientSide || state == SGState.Idle)
            return;
        ServerLevel serverLevel = (ServerLevel) level;

        // Refresh local chunk ticket
        serverLevel.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(worldPosition), 2, worldPosition);

        // Refresh remote chunk ticket if dialing or connected
        if (targetPos != null && targetDimension != null) {
            ServerLevel targetLevel = serverLevel.getServer().getLevel(targetDimension);
            if (targetLevel != null) {
                targetLevel.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(targetPos), 2, targetPos);
            }
        }
    }

    public void connectOrDisconnect(String address, Player player) {
        if (level == null || level.isClientSide)
            return;

        if (state == SGState.Idle) {
            if (!address.isEmpty()) {
                startDialing(address, player);
            }
        } else {
            disconnect();
        }
    }

    private void startDialing(String address, Player player) {
        address = address.toUpperCase();
        int addressLength = address.length();

        // 1. Check Chevron Upgrade for dimension travel
        if (addressLength > 7 && !hasChevronUpgrade && inventory.getStackInSlot(SLOT_CHEVRON_UPGRADE).isEmpty()) {
            this.addressError = "Needs 9-Chevron Upgrade";
            level.playSound(null, worldPosition, ModSounds.STARGATE_ABORT.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
            sync();
            return;
        }

        // 2. Find target in network
        SGNetwork.StargateLocation loc = SGNetwork.get(level).findStargate(address, level.dimension());
        if (loc == null) {
            this.addressError = "Invalid Address";
            level.playSound(null, worldPosition, ModSounds.STARGATE_ABORT.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
            sync();
            return;
        }

        // 3. Check for Energy
        int openingCost = ENERGY_OPEN_WORMHOLE;
        if (loc.dimension != null && !level.dimension().equals(loc.dimension)) {
            openingCost *= INTER_DIMENSION_MULTIPLIER;
        }
        
        if (this.energy < openingCost) {
            this.addressError = "Insufficient Power (FE)";
            level.playSound(null, worldPosition, ModSounds.STARGATE_ABORT.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
            sync();
            return;
        }

        // 4. Initiate Dialing
        SGBaseBlockEntity target = findRemoteStargate(loc);
        if (target == null) {
            this.addressError = "No response from terminal";
            level.playSound(null, worldPosition, ModSounds.STARGATE_ABORT.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
            sync();
            return;
        }
        if (target.state != SGState.Idle) {
            this.addressError = "Stargate Busy";
            level.playSound(null, worldPosition, ModSounds.STARGATE_ABORT.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
            sync();
            return;
        }

        this.addressError = null;
        this.dialledAddress = address;
        this.targetDimension = loc.dimension;
        this.targetPos = loc.pos;
        this.state = SGState.Dialling;
        this.updateLight();
        this.numEngagedChevrons = 0;
        this.dialTicks = 0;
        this.isIncoming = false;
        this.isChevronEngaging = false;
        this.isRingRotating = false;
        this.targetResponded = false;

        // Notify target Stargate for bidirectional sequence
        if (target != null) {
            target.onIncomingDialling(this.homeAddress, level.dimension(), worldPosition, address.length());
        }

        sync();
    }

    private SGBaseBlockEntity findRemoteStargate(SGNetwork.StargateLocation loc) {
        if (level == null || level.getServer() == null)
            return null;
        ServerLevel targetLevel = level.getServer().getLevel(loc.dimension);
        if (targetLevel == null)
            return null;

        // Force chunk loading for at least 60 ticks (3 seconds) to handle dialing
        // handshake
        ChunkPos cp = new ChunkPos(loc.pos);
        targetLevel.getChunkSource().addRegionTicket(TicketType.PORTAL, cp, 3, loc.pos);

        if (targetLevel.getBlockEntity(loc.pos) instanceof SGBaseBlockEntity te) {
            return te;
        }
        return null;
    }

    public void onIncomingDialling(String address, ResourceKey<Level> originDim, BlockPos originPos,
            int dialledLength) {
        if (this.state != SGState.Idle)
            return; // Busy

        // Prune address to match caller's dialed length for visual sync (7 or 9)
        if (address != null && address.length() > dialledLength) {
            address = address.substring(0, dialledLength);
        }

        this.dialledAddress = address;
        this.targetDimension = originDim;
        this.targetPos = originPos;
        this.state = SGState.Dialling;
        this.updateLight();
        this.numEngagedChevrons = 0;
        this.dialTicks = 0;
        this.isIncoming = true;
        this.isChevronEngaging = false;
        this.isRingRotating = false;
        sync();
    }

    public void disconnect() {
        if (this.state == SGState.Idle || this.state == SGState.Disconnecting)
            return;

        SGState oldState = this.state;
        this.state = SGState.Disconnecting;
        this.updateLight();
        this.connectionTicks = 0;
        this.clientAnimationTicks = 0;
        LOGGER.info("Stargate at {} disconnecting from state {}", worldPosition, oldState);

        if (oldState == SGState.Dialling) {
            // Dialling abort sound
            level.playSound(null, worldPosition, ModSounds.STARGATE_ABORT.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        } else if (oldState == SGState.Connected || oldState == SGState.Transient) {
            // Play close sound immediately at the START of the closing animation
            level.playSound(null, worldPosition, ModSounds.STARGATE_WORMHOLE_CLOSE.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        }

        // Notify target Stargate for bidirectional disconnect
        if (targetPos != null && targetDimension != null) {
            SGBaseBlockEntity target = findRemoteStargate(new SGNetwork.StargateLocation(targetDimension, targetPos));
            if (target != null && target.state != SGState.Idle && target.state != SGState.Disconnecting) {
                target.disconnect();
            }
        }

        sync();
    }


    // --- NBT Serialization ---

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("isMerged", isMerged);
        tag.putString("state", state.name());
        tag.putInt("numEngagedChevrons", numEngagedChevrons);
        tag.putBoolean("hasChevronUpgrade", hasChevronUpgrade);
        tag.putBoolean("hasIrisUpgrade", hasIrisUpgrade);
        tag.putDouble("ringAngle", ringAngle);
        tag.putDouble("targetRingAngle", targetRingAngle);
        tag.putDouble("startRingAngle", startRingAngle);
        tag.putDouble("totalRingRotationDelta", totalRingRotationDelta);
        tag.putDouble("ringRotationSpeed", ringRotationSpeed);
        tag.putBoolean("isRingRotating", isRingRotating);
        tag.putBoolean("isChevronEngaging", isChevronEngaging);
        tag.putBoolean("isIncoming", isIncoming);
        tag.putBoolean("isLinkedToController", isLinkedToController);
        tag.putInt("linkedX", linkedControllerPos.getX());
        tag.putInt("linkedY", linkedControllerPos.getY());
        tag.putInt("linkedZ", linkedControllerPos.getZ());
        tag.putInt("energy", energy);
        tag.putString("homeAddress", homeAddress != null ? homeAddress : "");
        if (addressError != null)
            tag.putString("addressError", addressError);
        tag.put("inventory", inventory.serializeNBT());
        tag.putString("irisState", irisState.name());
        tag.putFloat("irisPhase", irisPhase);

        // Connection persistence
        tag.putString("dialledAddress", dialledAddress);
        tag.putInt("dialTicks", dialTicks);
        tag.putBoolean("targetResponded", targetResponded);
        if (targetDimension != null)
            tag.putString("targetDim", targetDimension.location().toString());
        if (targetPos != null)
            tag.putLong("targetPos", targetPos.asLong());
        tag.putInt("connectionTicks", connectionTicks);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        isMerged = tag.getBoolean("isMerged");
        if (tag.contains("state")) {
            state = SGState.valueOf(tag.getString("state"));
        }
        numEngagedChevrons = tag.getInt("numEngagedChevrons");
        hasChevronUpgrade = tag.getBoolean("hasChevronUpgrade");
        hasIrisUpgrade = tag.getBoolean("hasIrisUpgrade");
        double newRingAngle = tag.getDouble("ringAngle");
        if (level != null && level.isClientSide) {
            // Keep the smooth client-predicted ringAngle, do not overwrite with delayed
            // network packets
            // ONLY sync from the server when the chunk is first loaded
            if (firstClientSync) {
                ringAngle = newRingAngle;
                lastRingAngle = newRingAngle;
                firstClientSync = false;
            }
        } else {
            ringAngle = newRingAngle;
            lastRingAngle = ringAngle;
        }

        targetRingAngle = tag.getDouble("targetRingAngle");

        if (level != null && level.isClientSide && state == SGState.Dialling) {
            // totalRingRotationDelta and startRingAngle are calculated natively by
            // updateAnimation
        } else {
            startRingAngle = tag.getDouble("startRingAngle");
            totalRingRotationDelta = tag.getDouble("totalRingRotationDelta");
        }

        ringRotationSpeed = tag.getDouble("ringRotationSpeed");
        isRingRotating = tag.getBoolean("isRingRotating");
        isChevronEngaging = tag.getBoolean("isChevronEngaging");
        isIncoming = tag.getBoolean("isIncoming");
        isLinkedToController = tag.getBoolean("isLinkedToController");
        linkedControllerPos = new BlockPos(tag.getInt("linkedX"), tag.getInt("linkedY"), tag.getInt("linkedZ"));
        energy = tag.getInt("energy");
        homeAddress = tag.getString("homeAddress");
        addressError = tag.contains("addressError") ? tag.getString("addressError") : null;
        inventory.deserializeNBT(tag.getCompound("inventory"));
        if (tag.contains("irisState")) {
            irisState = IrisState.valueOf(tag.getString("irisState"));
        }
        irisPhase = tag.getFloat("irisPhase");

        // Connection data
        dialledAddress = tag.getString("dialledAddress");
        dialTicks = tag.getInt("dialTicks");
        targetResponded = tag.getBoolean("targetResponded");
        if (tag.contains("targetDim")) {
            targetDimension = ResourceKey.create(Registries.DIMENSION,
                    new ResourceLocation(tag.getString("targetDim")));
        }
        if (tag.contains("targetPos")) {
            targetPos = BlockPos.of(tag.getLong("targetPos"));
        }
        connectionTicks = tag.getInt("connectionTicks");
    }

    // --- Client Sync ---

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // --- Render Bounding Box ---

    @Override
    public AABB getRenderBoundingBox() {
        // The Stargate ring extends ~2.5 blocks in each direction from the base and 5
        // blocks up
        BlockPos pos = getBlockPos();
        return new AABB(
                pos.getX() - 2, pos.getY(), pos.getZ() - 2,
                pos.getX() + 3, pos.getY() + 5, pos.getZ() + 3);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return inventoryHolder.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inventoryHolder.invalidate();
    }

    public void onIrisUpgradeChanged() {
        if (!hasIrisUpgrade) {
            irisState = IrisState.OPEN;
            irisPhase = 1.0f;
            removeIrisBlocks();
        }
        setChanged();
    }

    public void toggleIris() {
        if (!hasIrisUpgrade)
            return;
        if (irisState == IrisState.OPEN || irisState == IrisState.OPENING) {
            irisState = IrisState.CLOSING;
            placeIrisBlocks(); // Start placing immediately
            if (level != null) {
                level.playSound(null, worldPosition, ModSounds.STARGATE_IRIS_CLOSE.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
            }
        } else {
            irisState = IrisState.OPENING;
            if (level != null) {
                level.playSound(null, worldPosition, ModSounds.STARGATE_IRIS_OPEN.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
            }
        }
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void updateIris() {
        if (level == null || level.isClientSide)
            return;

        if (irisState == IrisState.CLOSING) {
            irisPhase -= 1.0f / IRIS_TIME;
            if (irisPhase <= 0.0f) {
                irisPhase = 0.0f;
                irisState = IrisState.CLOSED;
            }
            setChanged();
            if (level.getGameTime() % 5 == 0) { // Periodic sync during transition
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        } else if (irisState == IrisState.OPENING) {
            irisPhase += 1.0f / IRIS_TIME;
            if (irisPhase >= 1.0f) {
                irisPhase = 1.0f;
                irisState = IrisState.OPEN;
                removeIrisBlocks(); // Remove only when fully open
            }
            setChanged();
            if (level.getGameTime() % 5 == 0) { // Periodic sync during transition
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }




    private void placeIrisBlocks() {
        if (level == null || level.isClientSide)
            return;
        BlockPos center = worldPosition.above(2);
        Direction facing = getBlockState().getValue(SGBaseBlock.FACING);
        Direction.Axis axis = facing.getAxis();

        for (int r = -1; r <= 1; r++) {
            for (int u = 0; u <= 2; u++) {
                BlockPos p = center.above(u - 1);
                if (facing.getAxis() == Direction.Axis.X) {
                    p = p.relative(Direction.SOUTH, r);
                } else {
                    p = p.relative(Direction.EAST, r);
                }
                if (level.isEmptyBlock(p)) {
                    level.setBlock(p, ModBlocks.STARGATE_IRIS.get().defaultBlockState()
                            .setValue(SGIrisBlock.AXIS, axis), 3);
                }
            }
        }
    }

    public void removeIrisBlocks() {
        if (level == null || level.isClientSide)
            return;
        BlockPos center = worldPosition.above(2);
        Direction facing = getBlockState().getValue(SGBaseBlock.FACING);

        for (int r = -2; r <= 2; r++) {
            for (int u = -1; u <= 3; u++) {
                BlockPos p = center.above(u - 1);
                if (facing.getAxis() == Direction.Axis.X) {
                    p = p.relative(Direction.SOUTH, r);
                } else {
                    p = p.relative(Direction.EAST, r);
                }
                if (level.getBlockState(p).is(ModBlocks.STARGATE_IRIS.get())) {
                    level.removeBlock(p, false);
                }
            }
        }
    }

    private void checkEntityInteractions() {
        if (level == null || level.isClientSide || state != SGState.Connected) return;

        Direction facing = getBlockState().getValue(SGBaseBlock.FACING);
        BlockPos center = worldPosition.above(2);
        
        // Define a 4x4 area centered on the event horizon
        AABB searchBox;
        if (facing.getAxis() == Direction.Axis.Z) {
            searchBox = new AABB(center.getX() - 2, center.getY() - 1, center.getZ() - 0.5,
                               center.getX() + 2, center.getY() + 3, center.getZ() + 0.5);
        } else {
            searchBox = new AABB(center.getX() - 0.5, center.getY() - 1, center.getZ() - 2,
                               center.getX() + 0.5, center.getY() + 3, center.getZ() + 2);
        }

        List<Entity> entities = level.getEntities(null, searchBox);
        for (Entity entity : entities) {
            if (!entity.isAlive() || entity.isRemoved()) continue;
            
            if (entity.isPassenger() || entity.isVehicle() || !entity.canChangeDimensions()) continue;
            if (entity.tickCount < 10) continue; // Prevent instant re-entry

            // Check if entity is actually touching the "plane" of the gate
            double distToPlane = 0;
            if (facing.getAxis() == Direction.Axis.Z) {
                distToPlane = Math.abs(entity.getZ() - (center.getZ() + 0.5));
            } else {
                distToPlane = Math.abs(entity.getX() - (center.getX() + 0.5));
            }

            if (distToPlane < 0.3) {
                if (irisPhase >= 0.9f) { // Only TP if Iris is fully open
                    teleportEntity(entity);
                }
                // If Iris is closed, we do nothing. The physical blocks will stop the entity.
            }
        }
    }

    private void interceptEntity(Entity entity) {
        if (!entity.isAlive() || entity.isRemoved()) return;

        LOGGER.info("Entity {} intercepted by Iris at {}", entity.getName().getString(), worldPosition);
        level.playSound(null, worldPosition, ModSounds.STARGATE_IRIS_HIT.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
        
        if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
            living.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
            living.kill(); 
        } else {
            entity.discard();
        }
        
        // Force sync to prevent "frozen" state
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void teleportEntity(Entity entity) {
        if (targetPos == null || targetDimension == null || level.isClientSide) return;

        MinecraftServer server = level.getServer();
        if (server == null) return;
        ServerLevel destLevel = server.getLevel(targetDimension);
        if (destLevel == null) return;

        // Check target Iris state before TP
        if (destLevel.getBlockEntity(targetPos) instanceof SGBaseBlockEntity targetBE) {
            if (targetBE.irisPhase < 0.9f) {
                interceptEntity(entity); // Desintegrate incoming entity
                // Also play sound at destination
                destLevel.playSound(null, targetPos, ModSounds.STARGATE_IRIS_HIT.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
                return;
            }
        }

        destLevel.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(targetPos), 3, targetPos);

        Direction destFacing = Direction.NORTH;
        BlockState destState = destLevel.getBlockState(targetPos);
        if (destState.getBlock() instanceof SGBaseBlock) {
            destFacing = destState.getValue(SGBaseBlock.FACING);
        }

        final double destX = targetPos.getX() + 0.5 + destFacing.getStepX() * 1.5;
        final double destY = targetPos.getY() + 1.15;
        final double destZ = targetPos.getZ() + 0.5 + destFacing.getStepZ() * 1.5;

        Direction enterFacing = getBlockState().getValue(SGBaseBlock.FACING);
        float deltaYaw = destFacing.toYRot() - enterFacing.toYRot() + 180.0f;

        float targetYaw = entity.getYRot() + deltaYaw;
        float targetPitch = entity.getXRot();

        final String targetDim = targetDimension.location().toString();
        final String uuid = entity.getStringUUID();
        final String cmd = String.format(Locale.ROOT, "execute in %s run teleport %s %.3f %.3f %.3f %.3f %.3f",
                targetDim, uuid, destX, destY, destZ, targetYaw, targetPitch);

        server.execute(() -> {
            CommandSourceStack source = server.createCommandSourceStack().withPermission(4).withSuppressedOutput();
            server.getCommands().performPrefixedCommand(source, cmd);
        });

        entity.setPortalCooldown();
        entity.setDeltaMovement(Vec3.ZERO);
        entity.resetFallDistance();
    }

    private void updateIrisAnimation() {
        if (irisState == IrisState.CLOSING) {
            irisPhase -= 1.0f / IRIS_TIME;
            if (irisPhase <= 0.0f) {
                irisPhase = 0.0f;
                irisState = IrisState.CLOSED;
            }
        } else if (irisState == IrisState.OPENING) {
            irisPhase += 1.0f / IRIS_TIME;
            if (irisPhase >= 1.0f) {
                irisPhase = 1.0f;
                irisState = IrisState.OPEN;
                removeIrisBlocks(); // Remove only when fully open
            }
        }
    }

    private void performKawooshPhysicalEffects() {
        if (level == null || level.isClientSide || irisPhase < 1.0f)
            return;

        BlockState baseState = level.getBlockState(worldPosition);
        if (!(baseState.getBlock() instanceof SGBaseBlock))
            return;

        Direction facing = baseState.getValue(SGBaseBlock.FACING);
        Direction right = facing.getClockWise();
        BlockPos centerPos = worldPosition.above(2);

        // 1. Vaporize Blocks in front
        int reach = 4;
        for (int d = 1; d <= reach; d++) {
            int range = (d <= 2) ? 1 : 0; // 3x3 for d=1,2; 1x1 for d=3,4
            for (int h = -range; h <= range; h++) {
                for (int w = -range; w <= range; w++) {
                    BlockPos target = centerPos.relative(facing, d).relative(right, w).above(h);
                    BlockState targetState = level.getBlockState(target);

                    // Skip air, bedrock and stargate blocks
                    if (!targetState.isAir() && targetState.getDestroySpeed(level, target) >= 0) {
                        if (!(targetState.getBlock() instanceof SGBaseBlock)
                                && !(targetState.getBlock() instanceof SGRingBlock)) {
                            level.setBlockAndUpdate(target, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                        }
                    }
                }
            }
        }

        // 2. Kill Entities in front
        // Create an AABB that covers the 3x3 area and extends 4 blocks forward
        AABB kawooshBox = new AABB(centerPos).inflate(1.5);
        // Project it forward
        double offX = facing.getStepX() * 2.0;
        double offY = facing.getStepY() * 2.0;
        double offZ = facing.getStepZ() * 2.0;
        kawooshBox = kawooshBox.move(offX, offY, offZ).expandTowards(offX, offY, offZ);

        List<Entity> entities = level.getEntitiesOfClass(Entity.class, kawooshBox);
        for (Entity entity : entities) {
            entity.hurt(level.damageSources().genericKill(), 10000.0f);
        }
    }

    private void updateLight() {
        if (level != null && !level.isClientSide && isMerged) {
            boolean lit = (state == SGState.Transient || state == SGState.Connected || state == SGState.Disconnecting);
            SGBaseBlock.updateStructureLight(level, worldPosition, lit);
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
