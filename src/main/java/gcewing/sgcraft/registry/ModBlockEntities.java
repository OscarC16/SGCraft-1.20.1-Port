package gcewing.sgcraft.registry;

import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.block.entity.DHDBlockEntity;
import gcewing.sgcraft.block.entity.SGBaseBlockEntity;
import gcewing.sgcraft.block.entity.SGRingBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SGCraft.MODID);

    public static final RegistryObject<BlockEntityType<DHDBlockEntity>> DHD_BLOCK_ENTITY = BLOCK_ENTITIES.register("dhd_block_entity",
            () -> BlockEntityType.Builder.of(DHDBlockEntity::new, ModBlocks.STARGATE_CONTROLLER.get()).build(null));

    public static final RegistryObject<BlockEntityType<SGBaseBlockEntity>> SG_BASE_BLOCK_ENTITY = BLOCK_ENTITIES.register("sg_base_block_entity",
            () -> BlockEntityType.Builder.of(SGBaseBlockEntity::new, ModBlocks.STARGATE_BASE.get()).build(null));

    public static final RegistryObject<BlockEntityType<SGRingBlockEntity>> SG_RING_BLOCK_ENTITY = BLOCK_ENTITIES.register("sg_ring_block_entity",
            () -> BlockEntityType.Builder.of(SGRingBlockEntity::new,
                    ModBlocks.STARGATE_RING.get(),
                    ModBlocks.STARGATE_CHEVRON.get()
            ).build(null));
}
