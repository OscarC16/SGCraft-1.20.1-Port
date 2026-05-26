package gcewing.sgcraft.block.entity;

import gcewing.sgcraft.block.SGBaseBlock;
import gcewing.sgcraft.block.SGBlockStates;
import gcewing.sgcraft.block.SGIrisBlock;
import gcewing.sgcraft.registry.ModBlockEntities;
import gcewing.sgcraft.registry.ModBlocks;
import gcewing.sgcraft.registry.ModSounds;
import gcewing.sgcraft.world.SGNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

@SuppressWarnings({"deprecation", "removal"})
public class SGBaseBlockEntity extends BlockEntity {

    public enum State {
        Idle, Dialing, Transient, Connected, Disconnecting, InterDialling
    }

    public enum IrisState {
        OPEN, CLOSED, OPENING, CLOSING
    }

    // Configuration & Upgrades
    public boolean isMerged = false;
    public boolean hasChevronUpgrade = false;
    public boolean hasIrisUpgrade = false;
    public static final int TOTAL_SLOTS = 7;
    public static final int SLOT_CAMO_START = 0;
    public static final int SLOT_CAMO_COUNT = 5;
    public static final int SLOT_CHEVRON_UPGRADE = 5;
    public static final int SLOT_IRIS_UPGRADE = 6;

    // Energy
    public static final int MAX_ENERGY = 2000000;
    public int energy = 0;
    private static final int ENERGY_OPEN_WORMHOLE = 50000;
    private static final int ENERGY_KEEP_WORMHOLE = 100;
    private static final int INTER_DIMENSION_MULTIPLIER = 10;

    // Network & Connection State
    public State state = State.Idle;
    public String homeAddress = "";
    public String dialledAddress = "";
    public BlockPos targetPos = null;
    public ResourceKey<Level> targetDimension = null;
    public boolean isIncoming = false;
    public int numEngagedChevrons = 0;
    public int dialTicks = 0;
    public int connectionTicks = 0;
    public String addressError = "";

    // Dialing Sequence
    public int dialingStep = -1;
    public int stepTicks = 0;
    private static final int[] CHEVRON_ORDER_7 = { 1, 2, 3, 6, 7, 8, 0 };
    private static final int[] CHEVRON_ORDER_9 = { 1, 2, 3, 4, 5, 6, 7, 8, 0 };

    // Dialing Timing
    public static final int TICKS_PER_SYMBOL = 30;
    public static final int TICKS_FOR_RING = 20;
    public static final int TICKS_FOR_CHEVRON = 4;

    // Iris State
    public IrisState irisState = IrisState.OPEN;
    public float irisPhase = 1.0f; // 1.0 = Open, 0.0 = Closed
    public float prevIrisPhase = 1.0f;
    public static final int IRIS_TIME = 40;

    // Animation State
    public double ringAngle = 0;
    public double prevRingAngle = 0;
    public double targetRingAngle = 0;
    public double startRingAngle = 0;
    public double totalRingRotationDelta = 0;
    public boolean isRingRotating = false;
    public boolean isChevronEngaging = false;
    public float[] chevronEngageAmount = new float[9];
    public float[] prevChevronEngageAmount = new float[9];

    // Event Horizon Grid
    public static final int ehGridRadialSize = 5;
    public static final int ehGridPolarSize = 32;
    private double[][][] ehGrid;
    private static final double openingTransientIntensity = 1.3;
    private static final double openingTransientRandomness = 0.25;
    private static final double closingTransientRandomness = 0.25;

    // Linked DHD
    public boolean isLinkedToController = false;
    public BlockPos linkedControllerPos = BlockPos.ZERO;

    // Inventory
    public final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            updateUpgrades();
            sync();
        }
    };

    public SGBaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SG_BASE_BLOCK_ENTITY.get(), pos, state);
    }

    public void setMerged(boolean merged) {
        this.isMerged = merged;
        sync();
    }

    private void updateUpgrades() {
        hasChevronUpgrade = !inventory.getStackInSlot(SLOT_CHEVRON_UPGRADE).isEmpty();
        hasIrisUpgrade = !inventory.getStackInSlot(SLOT_IRIS_UPGRADE).isEmpty();
        if (!hasIrisUpgrade && irisState != IrisState.OPEN) {
            irisState = IrisState.OPEN;
            irisPhase = 1.0f;
            removeIrisBlocks();
        }
    }

    public boolean isActive() {
        return state != State.Idle;
    }

    public int getNumChevrons() {
        return hasChevronUpgrade ? 9 : 7;
    }

    public void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide() && level instanceof ServerLevel serverLevel) {
            serverLevel.setChunkForced(worldPosition.getX() >> 4, worldPosition.getZ() >> 4, true);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide() && level instanceof ServerLevel serverLevel) {
            serverLevel.setChunkForced(worldPosition.getX() >> 4, worldPosition.getZ() >> 4, false);
        }
        super.setRemoved();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SGBaseBlockEntity te) {
        if (level.isClientSide()) {
            te.updateAnimation();
            return;
        }

        te.updateIris();
        if (te.state == State.Connected || te.state == State.Transient) {
            te.checkEntityInteractions();
        }

        if (te.state == State.Transient || te.state == State.Connected || te.state == State.Disconnecting) {
            te.applyRandomImpulse();
            te.updateEventHorizon();
        }

        te.updateServerLogic();
    }

    private State lastState = State.Idle;
    public int clientAnimationTicks = 0;
    private double lastTargetRingAngle = -1e9;
    private double clientStartRingAngle = 0;
    private double clientTotalRingDelta = 0;
    private int clientDialTicks = 0;

    private void updateRingLighting(boolean lit) {
        if (level == null || level.isClientSide())
            return;
        Direction facing = getBlockState().getValue(gcewing.sgcraft.block.SGBlockStates.FACING);
        Direction.Axis axis = facing.getAxis();

        for (int i = -2; i <= 2; i++) {
            for (int j = 0; j <= 4; j++) {
                if (i == 0 && j == 0)
                    continue;
                BlockPos p = worldPosition.above(j);
                if (axis == Direction.Axis.X) {
                    p = p.relative(Direction.SOUTH, i);
                } else {
                    p = p.relative(Direction.EAST, i);
                }
                BlockState state = level.getBlockState(p);
                if (state.hasProperty(gcewing.sgcraft.block.SGBlockStates.LIT)
                        && state.getValue(gcewing.sgcraft.block.SGBlockStates.LIT) != lit) {
                    level.setBlock(p, state.setValue(gcewing.sgcraft.block.SGBlockStates.LIT, lit), 3);
                }
            }
        }
    }

    private void updateAnimation() {
        prevRingAngle = ringAngle;
        prevIrisPhase = irisPhase;
        for (int i = 0; i < 9; i++)
            prevChevronEngageAmount[i] = chevronEngageAmount[i];

        if (state != lastState) {
            if (state == State.Transient)
                initiateOpeningTransient();
            else if (state == State.Disconnecting)
                initiateClosingTransient();
            lastState = state;
        }

        if (state == State.Dialing) {
            // Client-side local rotation - triggered ONLY when server sends a new target
            // angle
            if (targetRingAngle != lastTargetRingAngle) {
                clientStartRingAngle = ringAngle;
                clientDialTicks = 0;
                lastTargetRingAngle = targetRingAngle;

                double diff = targetRingAngle - clientStartRingAngle;
                while (diff < -180)
                    diff += 360;
                while (diff > 180)
                    diff -= 360;
                clientTotalRingDelta = diff;
            }

            if (clientDialTicks < TICKS_FOR_RING) {
                clientDialTicks++;
                double tRing = (double) clientDialTicks / TICKS_FOR_RING;
                double smoothT = (1.0 - Math.cos(tRing * Math.PI)) / 2.0;
                ringAngle = clientStartRingAngle + smoothT * clientTotalRingDelta;
            } else {
                ringAngle = targetRingAngle;
                clientDialTicks++;
            }

            // Update chevrons during dialing
            int requiredChevrons = (dialledAddress != null && !dialledAddress.isEmpty()) ? dialledAddress.length() : 0;
            int[] order = (requiredChevrons == 9) ? CHEVRON_ORDER_9 : CHEVRON_ORDER_7;
            for (int i = 0; i < 9; i++) {
                float currentEngage = 0.0f;
                for (int step = 0; step < requiredChevrons; step++) {
                    if (order[step] == i) {
                        if (step < numEngagedChevrons) {
                            currentEngage = 1.0f;
                        } else if (step == numEngagedChevrons) {
                            // Local chevron engagement timing
                            if (clientDialTicks >= TICKS_FOR_RING) {
                                double tChev = Math.min(1.0,
                                        (double) (clientDialTicks - TICKS_FOR_RING) / TICKS_FOR_CHEVRON);
                                currentEngage = (float) Math.max(0, tChev);
                            }
                        }
                        break;
                    }
                }
                chevronEngageAmount[i] = currentEngage;
            }

        } else if (state == State.Connected || state == State.Transient) {
            lastTargetRingAngle = -1e9; // reset for next dial session

            // Activate only chevrons that were actually dialed
            int requiredChevrons = (dialledAddress != null && !dialledAddress.isEmpty()) ? dialledAddress.length()
                    : (hasChevronUpgrade ? 9 : 7);
            int[] order = (requiredChevrons == 9) ? CHEVRON_ORDER_9 : CHEVRON_ORDER_7;
            for (int i = 0; i < 9; i++) {
                float currentEngage = 0.0f;
                for (int step = 0; step < requiredChevrons; step++) {
                    if (order[step] == i) {
                        currentEngage = 1.0f;
                        break;
                    }
                }
                chevronEngageAmount[i] = currentEngage;
            }

        } else if (state == State.Disconnecting || state == State.Idle) {
            lastTargetRingAngle = -1e9;
            for (int i = 0; i < 9; i++)
                chevronEngageAmount[i] = 0;
        }

        if (state == State.Transient || state == State.Connected || state == State.Disconnecting) {
            applyRandomImpulse();
            updateEventHorizon();
        }

        if (hasIrisUpgrade)
            updateIris();
    }

    private void updateServerLogic() {
        if (homeAddress.isEmpty())
            updateHomeAddress();

        // Update lighting state based on active call
        if (level.getGameTime() % 5 == 0) {
            boolean active = (state == State.Connected || state == State.Transient);
            if (state == State.Disconnecting && connectionTicks < 15) {
                active = true;
            }

            if (getBlockState().getValue(gcewing.sgcraft.block.SGBlockStates.LIT) != active) {
                level.setBlock(worldPosition, getBlockState().setValue(gcewing.sgcraft.block.SGBlockStates.LIT, active),
                        3);
                updateRingLighting(active);
            }
        }

        switch (state) {
            case Dialing:
                updateDialingSequence();
                break;
            case Transient:
                connectionTicks++;
                if (connectionTicks >= 40) { // Longer Kawoosh animation
                    state = State.Connected;
                    connectionTicks = 0;
                    sync();
                }
                break;
            case Connected:
                connectionTicks++;
                // Energy consumption logic
                int costPerTick = ENERGY_KEEP_WORMHOLE;
                if (targetDimension != null && !level.dimension().equals(targetDimension)) {
                    costPerTick *= INTER_DIMENSION_MULTIPLIER;
                }

                if (!isIncoming) {
                    // Only the calling gate consumes maintenance energy
                    if (energy < costPerTick || connectionTicks > 2400) {
                        disconnect();
                    } else {
                        energy -= costPerTick;
                        if (connectionTicks % 20 == 0)
                            sync();
                        checkEntityInteractions();
                    }
                } else {
                    // Incoming gate only checks timeout and origin presence
                    if (connectionTicks > 2400 || !isOriginStillConnected()) {
                        disconnect();
                    } else if (connectionTicks % 20 == 0) {
                        sync();
                    }
                    checkEntityInteractions();
                }
                break;
            case Disconnecting:
                connectionTicks++;
                if (connectionTicks >= 20) { // Duration for closing animation
                    state = State.Idle;
                    numEngagedChevrons = 0;
                    connectionTicks = 0;
                    sync();
                }
                break;
            default:
                break;
        }
    }

    private void updateDialingSequence() {
        int requiredChevrons = dialledAddress.length();
        if (numEngagedChevrons >= requiredChevrons) {
            completeDialing();
            return;
        }

        char symbol = dialledAddress.charAt(numEngagedChevrons);
        int symbolIndex = gcewing.sgcraft.SGAddressing.charToSymbol(symbol);
        int[] order = (requiredChevrons == 7) ? CHEVRON_ORDER_7 : CHEVRON_ORDER_9;
        int engagingChevronIndex = order[numEngagedChevrons];
        if (dialTicks == 0) {
            isRingRotating = true;
            isChevronEngaging = false;

            double chevronAngleOffset = (4 - engagingChevronIndex) * 40.0;
            double textureAngleOffset = -65.0; // Alignment offset from 1.20.1
            double targetAngle = ((symbolIndex * 360.0) / gcewing.sgcraft.SGAddressing.NUM_SYMBOLS) + chevronAngleOffset
                    + textureAngleOffset;

            startRingAngle = ringAngle;
            double diff = targetAngle - ringAngle;
            while (diff < -180)
                diff += 360;
            while (diff > 180)
                diff -= 360;

            totalRingRotationDelta = diff;
            targetRingAngle = startRingAngle + totalRingRotationDelta;

            // Play ring start for every symbol movement
            level.playSound(null, worldPosition, ModSounds.STARGATE_RING_START.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
            sync(); // Primary sync for new symbol
        }

        dialTicks++;

        // Phase 1: Ring Rotation ends
        if (dialTicks == TICKS_FOR_RING) {
            isRingRotating = false;
            isChevronEngaging = true;
            ringAngle = targetRingAngle;
            sync(); // Sync to trigger chevron engagement animation on client
        }

        // Phase 2: Chevron Engagement ends
        if (dialTicks == TICKS_FOR_RING + TICKS_FOR_CHEVRON) {
            isChevronEngaging = false;
        }

        if (dialTicks >= TICKS_PER_SYMBOL) {
            numEngagedChevrons++;
            dialTicks = 0;
        }
    }

    public int getEngagedChevronIndex(int step) {
        int[] order = hasChevronUpgrade ? CHEVRON_ORDER_9 : CHEVRON_ORDER_7;
        if (step >= 0 && step < order.length)
            return order[step];
        return -1;
    }

    public void updateHomeAddress() {
        if (level != null && !level.isClientSide()) {
            int dimIndex = getDimensionIndex(level);
            this.homeAddress = gcewing.sgcraft.SGAddressing.addressForLocation(worldPosition, dimIndex);
            SGNetwork.get(level).register(homeAddress, level.dimension(), worldPosition);
            sync();
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
        return Math.abs(dim.identifier().toString().hashCode() % gcewing.sgcraft.SGAddressing.DIMENSION_RANGE);
    }

    public void connectOrDisconnect(String address, ServerPlayer player) {
        if (state == State.Idle) {
            // 1. Validate 9-symbol upgrade requirement
            if (address.length() > 7 && !hasChevronUpgrade) {
                player.sendSystemMessage(
                        Component.literal("Error: Se requiere mejora de Chevron para direcciones de 9 símbolos."));
                return;
            }

            // 2. Find destination location
            SGNetwork.StargateLocation loc = SGNetwork.get(level).findStargate(address, level.dimension());
            if (loc == null) {
                player.sendSystemMessage(Component.literal("Error: Dirección no válida."));
                return;
            }

            // 3. Load destination level and check for Stargate
            ServerLevel destLevel = level.getServer().getLevel(loc.dimension);
            if (destLevel == null) {
                player.sendSystemMessage(Component.literal("Error: Dimensión de destino no cargada."));
                return;
            }

            if (!(destLevel.getBlockEntity(loc.pos) instanceof SGBaseBlockEntity targetBE)) {
                player.sendSystemMessage(Component.literal("Error: No se encontró un Stargate en el destino."));
                return;
            }

            // 4. Case 2: Check if destination is busy
            if (targetBE.state != State.Idle) {
                player.sendSystemMessage(Component.literal("Error: El portal de destino está ocupado."));
                level.playSound(null, worldPosition, ModSounds.STARGATE_ABORT.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
                return;
            }

            // 5. Dimension/Symbol restrictions
            boolean isInterDimensional = !level.dimension().equals(loc.dimension);
            if (address.length() == 7 && isInterDimensional) {
                player.sendSystemMessage(
                        Component.literal("Error: Solo se permiten llamadas intradimensionales con 7 símbolos."));
                return;
            }

            // 5.5. Energy Check
            int openingCost = ENERGY_OPEN_WORMHOLE;
            if (isInterDimensional) {
                openingCost *= INTER_DIMENSION_MULTIPLIER;
            }
            if (this.energy < openingCost) {
                player.sendSystemMessage(Component.literal("Error: Energía insuficiente para iniciar la conexión."));
                level.playSound(null, worldPosition, ModSounds.STARGATE_ABORT.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
                return;
            }

            // 6. Start mutual dialing sequence
            String returnAddress = this.homeAddress;
            if (address.length() == 7) {
                returnAddress = returnAddress.substring(0, 7);
            }
            this.startDialing(address, loc.pos, loc.dimension, false);
            targetBE.startDialing(returnAddress, worldPosition, level.dimension(), true);
        } else {
            disconnect();
        }
    }

    private void completeDialing() {
        if (level.isClientSide())
            return;

        this.state = State.Transient;
        this.connectionTicks = 0;

        // Subtract opening cost at the moment the horizon opens
        if (!isIncoming) {
            int openingCost = ENERGY_OPEN_WORMHOLE;
            if (targetDimension != null && !level.dimension().equals(targetDimension)) {
                openingCost *= INTER_DIMENSION_MULTIPLIER;
            }
            energy -= openingCost;
        }

        level.playSound(null, worldPosition, ModSounds.STARGATE_WORMHOLE_OPEN.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
        initiateOpeningTransient();
        onKawoosh();

        this.dialledAddress = "";
        this.numEngagedChevrons = 0;
        sync();
    }

    public void startDialing(String address, BlockPos targetPos, ResourceKey<Level> targetDim, boolean isIncoming) {
        if (!level.isClientSide()) {
            // Check opening cost for origin gate
            if (!isIncoming) {
                int openingCost = ENERGY_OPEN_WORMHOLE;
                if (!level.dimension().equals(targetDim)) {
                    openingCost *= INTER_DIMENSION_MULTIPLIER;
                }
                if (energy < openingCost)
                    return; // Check only, don't subtract yet
            }

            this.dialledAddress = address;
            this.targetPos = targetPos;
            this.targetDimension = targetDim;
            this.isIncoming = isIncoming;
            this.state = State.Dialing;
            this.dialingStep = 0;
            this.dialTicks = 0;
            this.numEngagedChevrons = 0;
            this.targetRingAngle = this.ringAngle;
            sync();
        }
    }

    private boolean isOriginStillConnected() {
        if (targetDimension == null || targetPos == null)
            return false;
        ServerLevel destLevel = level.getServer().getLevel(targetDimension);
        if (destLevel == null)
            return false;
        if (destLevel.getBlockEntity(targetPos) instanceof SGBaseBlockEntity targetBE) {
            return targetBE.state == State.Connected || targetBE.state == State.Transient
                    || targetBE.state == State.Dialing;
        }
        return false;
    }

    public void onIncomingConnection(String address, ResourceKey<Level> originDim, BlockPos originPos) {
        this.state = State.Connected;
        this.dialledAddress = address;
        this.targetDimension = originDim;
        this.targetPos = originPos;
        this.isIncoming = true;
        this.connectionTicks = 0;
        sync();
    }

    public void onKawoosh() {
        if (level == null || level.isClientSide())
            return;

        Direction facing = getBlockState().getValue(SGBlockStates.FACING);
        BlockPos center = worldPosition.above(2);

        // Phase 1: 3x3x2 area
        for (int d = 1; d <= 2; d++) {
            BlockPos layerCenter = center.relative(facing, d);
            destroyArea(layerCenter, facing.getAxis(), 1); // 3x3
        }

        // Phase 2: 1x1x2 area
        for (int d = 3; d <= 4; d++) {
            BlockPos layerCenter = center.relative(facing, d);
            destroyArea(layerCenter, facing.getAxis(), 0); // 1x1
        }
    }

    private void destroyArea(BlockPos center, Direction.Axis axis, int radius) {
        for (int u = -radius; u <= radius; u++) {
            for (int v = -radius; v <= radius; v++) {
                BlockPos target = (axis == Direction.Axis.X)
                        ? center.offset(0, u, v)
                        : center.offset(u, v, 0);

                if (level.getBlockState(target).getDestroySpeed(level, target) >= 0) {
                    level.destroyBlock(target, false);
                }

                // Damage entities
                AABB damageBox = new AABB(target);
                List<Entity> entities = level.getEntitiesOfClass(Entity.class, damageBox);
                for (Entity entity : entities) {
                    entity.hurt(level.damageSources().generic(), 20.0f);
                }
            }
        }
    }

    public void disconnect() {
        if (state == State.Idle)
            return;

        if (targetDimension != null && targetPos != null) {
            ServerLevel destLevel = level.getServer().getLevel(targetDimension);
            if (destLevel != null && destLevel.getBlockEntity(targetPos) instanceof SGBaseBlockEntity targetBE) {
                if (targetBE.state != State.Idle && targetBE.state != State.Disconnecting) {
                    if (targetBE.state == State.Connected || targetBE.state == State.Transient) {
                        targetBE.state = State.Disconnecting;
                        targetBE.connectionTicks = 0;
                    } else {
                        targetBE.state = State.Idle;
                    }
                    targetBE.targetPos = null;
                    targetBE.targetDimension = null;
                    targetBE.dialledAddress = "";
                    targetBE.numEngagedChevrons = 0;
                    targetBE.dialingStep = -1;
                    targetBE.isIncoming = false;
                    targetBE.sync();
                }
            }
        }

        if (this.state == State.Connected || this.state == State.Transient) {
            level.playSound(null, worldPosition, ModSounds.STARGATE_WORMHOLE_CLOSE.get(), SoundSource.BLOCKS, 0.5f,
                    1.0f);
            initiateClosingTransient();
            this.state = State.Disconnecting;
            this.connectionTicks = 0;
        } else {
            level.playSound(null, worldPosition, ModSounds.STARGATE_ABORT.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
            this.state = State.Idle;
        }
        this.connectionTicks = 0;
        this.targetPos = null;
        this.targetDimension = null;
        this.dialledAddress = "";
        this.numEngagedChevrons = 0;
        this.dialingStep = -1;
        this.isIncoming = false;
        sync();
    }

    private void checkEntityInteractions() {
        if (level == null || level.isClientSide() || state != State.Connected)
            return;

        BlockPos center = worldPosition.above(2);
        Direction facing = getBlockState().getValue(SGBlockStates.FACING);
        double cx = center.getX() + 0.5;
        double cy = center.getY() + 0.5;
        double cz = center.getZ() + 0.5;
        AABB ehBox;

        if (facing.getAxis() == Direction.Axis.X) {
            ehBox = new AABB(cx - 0.05, cy - 2.0, cz - 2.0, cx + 0.05, cy + 2.0, cz + 2.0);
        } else {
            ehBox = new AABB(cx - 2.0, cy - 2.0, cz - 0.05, cx + 2.0, cy + 2.0, cz + 0.05);
        }

        List<Entity> entities = level.getEntitiesOfClass(Entity.class, ehBox);
        for (Entity entity : entities) {
            if (!entity.isAlive() || entity.isRemoved())
                continue;
            if (entity.isOnPortalCooldown())
                continue;
            if (entity.isPassenger() || entity.isVehicle() || !entity.canUsePortal(false))
                continue;

            if (irisPhase >= 0.9f) {
                teleportEntity(entity);
            }
        }
    }

    private void teleportEntity(Entity entity) {
        if (targetPos == null || targetDimension == null)
            return;

        ServerLevel destLevel = level.getServer().getLevel(targetDimension);
        if (destLevel == null)
            return;

        BlockEntity targetBE = destLevel.getBlockEntity(targetPos);
        if (targetBE instanceof SGBaseBlockEntity destSG) {
            if (destSG.irisPhase < 0.9f) {
                interceptEntity(entity);
                destLevel.playSound(null, targetPos, ModSounds.STARGATE_IRIS_HIT.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
                return;
            }
        }

        Direction destFacing = Direction.NORTH;
        BlockState destState = destLevel.getBlockState(targetPos);
        if (destState.getBlock() instanceof SGBaseBlock) {
            destFacing = destState.getValue(SGBlockStates.FACING);
        }

        double destX = targetPos.getX() + 0.5 + destFacing.getStepX() * 1.5;
        double destY = targetPos.getY() + 1.15;
        double destZ = targetPos.getZ() + 0.5 + destFacing.getStepZ() * 1.5;

        Direction enterFacing = getBlockState().getValue(SGBlockStates.FACING);
        float deltaYaw = destFacing.toYRot() - enterFacing.toYRot() + 180.0f;
        float targetYaw = entity.getYRot() + deltaYaw;
        float targetPitch = entity.getXRot();

        if (entity instanceof ServerPlayer player) {
            player.teleportTo(destLevel, destX, destY, destZ, java.util.Set.of(), targetYaw, targetPitch, false);
        } else {
            entity.teleportTo(destLevel, destX, destY, destZ, java.util.Set.of(), targetYaw, targetPitch, false);
        }
        entity.setPortalCooldown();
    }

    private void interceptEntity(Entity entity) {
        level.playSound(null, entity.blockPosition(), ModSounds.STARGATE_IRIS_HIT.get(), SoundSource.BLOCKS, 0.5F,
                1.0F);
        if (entity instanceof LivingEntity living) {
            living.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
            living.discard();
        } else {
            entity.discard();
        }
    }

    public void toggleIris() {
        if (!hasIrisUpgrade)
            return;
        if (irisState == IrisState.OPEN || irisState == IrisState.OPENING) {
            irisState = IrisState.CLOSING;
            placeIrisBlocks();
            level.playSound(null, worldPosition, ModSounds.STARGATE_IRIS_CLOSE.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        } else {
            irisState = IrisState.OPENING;
            level.playSound(null, worldPosition, ModSounds.STARGATE_IRIS_OPEN.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        sync();
    }

    private void updateIris() {
        if (irisState == IrisState.CLOSING) {
            irisPhase -= 1.0f / IRIS_TIME;
            if (irisPhase <= 0.0f) {
                irisPhase = 0.0f;
                irisState = IrisState.CLOSED;
            }
            if (!level.isClientSide() && level.getGameTime() % 5 == 0)
                sync();
        } else if (irisState == IrisState.OPENING) {
            irisPhase += 1.0f / IRIS_TIME;
            if (irisPhase >= 1.0f) {
                irisPhase = 1.0f;
                irisState = IrisState.OPEN;
                if (!level.isClientSide())
                    removeIrisBlocks();
            }
            if (!level.isClientSide() && level.getGameTime() % 5 == 0)
                sync();
        }
    }

    private void placeIrisBlocks() {
        if (level == null || level.isClientSide())
            return;
        BlockPos center = worldPosition.above(2);
        Direction facing = getBlockState().getValue(SGBlockStates.FACING);
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
                    level.setBlock(p,
                            ModBlocks.STARGATE_IRIS.get().defaultBlockState().setValue(SGIrisBlock.AXIS, axis), 3);
                }
            }
        }
    }

    public void removeIrisBlocks() {
        if (level == null || level.isClientSide())
            return;
        BlockPos center = worldPosition.above(2);
        Direction facing = getBlockState().getValue(SGBlockStates.FACING);

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

    @Override
    protected void saveAdditional(@NotNull ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("isMerged", isMerged);
        output.putBoolean("isLinkedToController", isLinkedToController);
        output.putLong("linkedControllerPos", linkedControllerPos.asLong());
        output.putInt("energy", energy);
        output.putBoolean("hasChevronUpgrade", hasChevronUpgrade);
        output.putBoolean("hasIrisUpgrade", hasIrisUpgrade);
        output.putString("irisState", irisState.name());
        output.putFloat("irisPhase", irisPhase);
        output.putString("state", state.name());
        output.putDouble("ringAngle", ringAngle);
        
        inventory.serialize(output.child("inventory"));
        
        output.putInt("numEngagedChevrons", numEngagedChevrons);
        output.putString("dialledAddress", dialledAddress);
        output.putInt("dialingStep", dialingStep);
        output.putInt("dialTicks", dialTicks);
        output.putBoolean("isRingRotating", isRingRotating);
        output.putBoolean("isChevronEngaging", isChevronEngaging);
        output.putDouble("targetRingAngle", targetRingAngle);
        
        if (targetPos != null)
            output.putLong("targetPos", targetPos.asLong());
        if (targetDimension != null)
            output.putString("targetDim", targetDimension.identifier().toString());
            
        output.putBoolean("isIncoming", isIncoming);
        output.putString("homeAddress", homeAddress);
    }

    @Override
    public void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        isMerged = input.getBooleanOr("isMerged", false);
        isLinkedToController = input.getBooleanOr("isLinkedToController", false);
        linkedControllerPos = net.minecraft.core.BlockPos.of(input.getLongOr("linkedControllerPos", 0L));
        energy = input.getIntOr("energy", 0);
        hasChevronUpgrade = input.getBooleanOr("hasChevronUpgrade", false);
        hasIrisUpgrade = input.getBooleanOr("hasIrisUpgrade", false);
        
        String isStr = input.getStringOr("irisState", "");
        if (!isStr.isEmpty()) irisState = IrisState.valueOf(isStr);
        
        irisPhase = input.getFloatOr("irisPhase", 1.0f);
        
        String stStr = input.getStringOr("state", "");
        if (!stStr.isEmpty()) state = State.valueOf(stStr);
        
        double newRingAngle = input.getDoubleOr("ringAngle", 0.0);
        if (level == null || !level.isClientSide() || state != State.Dialing) {
            ringAngle = newRingAngle;
        }
        
        inventory.deserialize(input.childOrEmpty("inventory"));
        
        numEngagedChevrons = input.getIntOr("numEngagedChevrons", 0);
        dialledAddress = input.getStringOr("dialledAddress", "");
        dialingStep = input.getIntOr("dialingStep", -1);
        
        if (level == null || !level.isClientSide() || state != State.Dialing) {
            dialTicks = input.getIntOr("dialTicks", 0);
        }
        
        isRingRotating = input.getBooleanOr("isRingRotating", false);
        isChevronEngaging = input.getBooleanOr("isChevronEngaging", false);
        targetRingAngle = input.getDoubleOr("targetRingAngle", 0.0);
        
        long tPos = input.getLongOr("targetPos", -1L);
        if (tPos != -1L) targetPos = net.minecraft.core.BlockPos.of(tPos);
        
        String tDim = input.getStringOr("targetDim", "");
        if (!tDim.isEmpty()) {
            targetDimension = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.Identifier.parse(tDim));
        }
        
        isIncoming = input.getBooleanOr("isIncoming", false);
        homeAddress = input.getStringOr("homeAddress", "");
        updateUpgrades();
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

        for (int i = 0; i < 2; i++) {
            grid[i][0] = grid[i][ehGridPolarSize];
            grid[i][ehGridPolarSize + 1] = grid[i][1];
        }
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        net.minecraft.nbt.CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("isMerged", isMerged);
        tag.putBoolean("isLinkedToController", isLinkedToController);
        tag.putLong("linkedControllerPos", linkedControllerPos.asLong());
        tag.putInt("energy", energy);
        tag.putBoolean("hasChevronUpgrade", hasChevronUpgrade);
        tag.putBoolean("hasIrisUpgrade", hasIrisUpgrade);
        tag.putString("irisState", irisState.name());
        tag.putFloat("irisPhase", irisPhase);
        tag.putString("state", state.name());
        tag.putDouble("ringAngle", ringAngle);
        tag.putInt("numEngagedChevrons", numEngagedChevrons);
        tag.putString("dialledAddress", dialledAddress);
        tag.putInt("dialingStep", dialingStep);
        tag.putInt("dialTicks", dialTicks);
        tag.putBoolean("isRingRotating", isRingRotating);
        tag.putBoolean("isChevronEngaging", isChevronEngaging);
        tag.putDouble("targetRingAngle", targetRingAngle);
        
        if (targetPos != null)
            tag.putLong("targetPos", targetPos.asLong());
        if (targetDimension != null)
            tag.putString("targetDim", targetDimension.identifier().toString());
            
        tag.putBoolean("isIncoming", isIncoming);
        tag.putString("homeAddress", homeAddress);
        net.minecraft.world.level.storage.TagValueOutput output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, registries);
        inventory.serialize(output);
        tag.put("inventory", output.buildResult());
        return tag;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null && !this.level.isClientSide()) {
            for (int i = 0; i < this.inventory.getSlots(); i++) {
                net.minecraft.world.item.ItemStack stack = this.inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
            }
            if (state.getBlock() instanceof gcewing.sgcraft.block.SGBaseBlock baseBlock) {
                baseBlock.unmerge(this.level, pos);
            }
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
