package gcewing.sgcraft.registry;

import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.block.entity.SGBaseBlockEntity;
import gcewing.sgcraft.block.entity.SGRingBlockEntity;
import gcewing.sgcraft.block.entity.DHDBlockEntity;
import gcewing.sgcraft.block.entity.RFPowerBlockEntity;
import gcewing.sgcraft.block.entity.NaquadahGeneratorBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, SGCraft.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SGBaseBlockEntity>> SG_BASE_BLOCK_ENTITY = 
        BLOCK_ENTITIES.register("sg_base_block_entity", () -> new BlockEntityType<>(SGBaseBlockEntity::new, ModBlocks.STARGATE_BASE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SGRingBlockEntity>> SG_RING_BLOCK_ENTITY = 
        BLOCK_ENTITIES.register("sg_ring_block_entity", () -> new BlockEntityType<>(SGRingBlockEntity::new, ModBlocks.STARGATE_RING.get(), ModBlocks.STARGATE_CHEVRON.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DHDBlockEntity>> DHD_BLOCK_ENTITY = 
        BLOCK_ENTITIES.register("dhd_block_entity", () -> new BlockEntityType<>(DHDBlockEntity::new, ModBlocks.STARGATE_CONTROLLER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RFPowerBlockEntity>> RF_POWER_UNIT_BLOCK_ENTITY = 
        BLOCK_ENTITIES.register("rf_power_unit_block_entity", () -> new BlockEntityType<>(RFPowerBlockEntity::new, ModBlocks.RF_POWER_UNIT.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NaquadahGeneratorBlockEntity>> NAQUADAH_GENERATOR_BLOCK_ENTITY = 
        BLOCK_ENTITIES.register("naquadah_generator_block_entity", () -> new BlockEntityType<>(NaquadahGeneratorBlockEntity::new, ModBlocks.NAQUADAH_GENERATOR.get()));
}
