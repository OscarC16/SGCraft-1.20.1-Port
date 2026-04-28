package gcewing.sgcraft.registry;

import gcewing.sgcraft.SGCraft;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SGCraft.MODID);

    public static final net.neoforged.neoforge.registries.DeferredBlock<Block> NAQUADAH_BLOCK = BLOCKS.registerBlock("naquadah_block", 
        Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final net.neoforged.neoforge.registries.DeferredBlock<Block> NAQUADAH_ORE = BLOCKS.registerBlock("naquadah_ore", 
        Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 3.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());

    public static final net.neoforged.neoforge.registries.DeferredBlock<Block> DEEPSLATE_NAQUADAH_ORE = BLOCKS.registerBlock("deepslate_naquadah_ore", 
        Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops());

    public static final net.neoforged.neoforge.registries.DeferredBlock<Block> STARGATE_RING = BLOCKS.registerBlock("stargate_ring", 
        Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 3.0F).sound(SoundType.STONE).noOcclusion().requiresCorrectToolForDrops());

    public static final net.neoforged.neoforge.registries.DeferredBlock<Block> STARGATE_CHEVRON = BLOCKS.registerBlock("stargate_chevron", 
        Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 3.0F).sound(SoundType.STONE).noOcclusion().requiresCorrectToolForDrops());

    public static final net.neoforged.neoforge.registries.DeferredBlock<Block> STARGATE_BASE = BLOCKS.registerBlock("stargate_base", 
        Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 3.0F).sound(SoundType.STONE).noOcclusion().requiresCorrectToolForDrops());

    public static final net.neoforged.neoforge.registries.DeferredBlock<Block> STARGATE_CONTROLLER = BLOCKS.registerBlock("stargate_controller", 
        Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 3.0F).sound(SoundType.STONE).noOcclusion().requiresCorrectToolForDrops());

    public static final net.neoforged.neoforge.registries.DeferredBlock<Block> STARGATE_IRIS = BLOCKS.registerBlock("stargate_iris", 
        Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(-1.0F, 3600000.0F).noOcclusion().pushReaction(PushReaction.BLOCK));
}
