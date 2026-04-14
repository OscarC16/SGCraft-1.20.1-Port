package gcewing.sgcraft.registry;

import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.block.DHDBlock;
import gcewing.sgcraft.block.SGBaseBlock;
import gcewing.sgcraft.block.SGRingBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SGCraft.MODID);

    public static final RegistryObject<Block> NAQUADAH_BLOCK = BLOCKS.register("naquadah_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
            ));

    public static final RegistryObject<Block> NAQUADAH_ORE = BLOCKS.register("naquadah_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
            ));

    public static final RegistryObject<Block> DEEPSLATE_NAQUADAH_ORE = BLOCKS.register("deepslate_naquadah_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()
            ));

    // Bloques Estructurales del Stargate
    public static final RegistryObject<Block> STARGATE_RING = BLOCKS.register("stargate_ring",
            () -> new SGRingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .requiresCorrectToolForDrops(),
                    false // not chevron
            ));

    public static final RegistryObject<Block> STARGATE_CHEVRON = BLOCKS.register("stargate_chevron",
            () -> new SGRingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .requiresCorrectToolForDrops(),
                    true // is chevron
            ));

    public static final RegistryObject<Block> STARGATE_BASE = BLOCKS.register("stargate_base",
            () -> new SGBaseBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()
            ));

    public static final RegistryObject<Block> STARGATE_CONTROLLER = BLOCKS.register("stargate_controller",
            () -> new DHDBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()
            ));
}
