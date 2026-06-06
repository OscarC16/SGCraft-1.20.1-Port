package gcewing.sgcraft.registry;

import gcewing.sgcraft.SGCraft;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import gcewing.sgcraft.block.*;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SGCraft.MODID);

    public static final net.neoforged.neoforge.registries.DeferredBlock<Block> NAQUADAH_BLOCK = BLOCKS.registerBlock("naquadah_block", 
        Block::new, props -> props.mapColor(MapColor.COLOR_LIGHT_GREEN).strength(8.0F, 12.0F).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops());

    public static final net.neoforged.neoforge.registries.DeferredBlock<Block> NAQUADAH_ORE = BLOCKS.registerBlock("naquadah_ore", 
        Block::new, props -> props.mapColor(MapColor.STONE).strength(3.0F, 3.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());

    public static final net.neoforged.neoforge.registries.DeferredBlock<Block> DEEPSLATE_NAQUADAH_ORE = BLOCKS.registerBlock("deepslate_naquadah_ore", 
        Block::new, props -> props.mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops());

    public static final net.neoforged.neoforge.registries.DeferredBlock<SGRingBlock> STARGATE_RING = BLOCKS.registerBlock("stargate_ring", 
        SGRingBlock::new, props -> props.mapColor(MapColor.STONE).strength(15.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK).noOcclusion().requiresCorrectToolForDrops()
            .lightLevel(state -> state.getValue(gcewing.sgcraft.block.SGBlockStates.LIT) ? 15 : 0));

    public static final net.neoforged.neoforge.registries.DeferredBlock<SGRingBlock> STARGATE_CHEVRON = BLOCKS.registerBlock("stargate_chevron", 
        SGRingBlock::new, props -> props.mapColor(MapColor.STONE).strength(15.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK).noOcclusion().requiresCorrectToolForDrops()
            .lightLevel(state -> state.getValue(gcewing.sgcraft.block.SGBlockStates.LIT) ? 15 : 0));

    public static final net.neoforged.neoforge.registries.DeferredBlock<SGBaseBlock> STARGATE_BASE = BLOCKS.registerBlock("stargate_base", 
        SGBaseBlock::new, props -> props.mapColor(MapColor.STONE).strength(15.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK).noOcclusion().requiresCorrectToolForDrops()
            .lightLevel(state -> state.getValue(gcewing.sgcraft.block.SGBlockStates.LIT) ? 15 : 0));

    public static final net.neoforged.neoforge.registries.DeferredBlock<DHDBlock> STARGATE_CONTROLLER = BLOCKS.registerBlock("stargate_controller", 
        DHDBlock::new, props -> props.mapColor(MapColor.STONE).strength(5.0F, 6.0F).sound(SoundType.POLISHED_DEEPSLATE).noOcclusion().requiresCorrectToolForDrops());

    public static final net.neoforged.neoforge.registries.DeferredBlock<SGIrisBlock> STARGATE_IRIS = BLOCKS.registerBlock("stargate_iris", 
        SGIrisBlock::new, props -> props.mapColor(MapColor.COLOR_GRAY).strength(-1.0F, 3600000.0F).noOcclusion().pushReaction(PushReaction.BLOCK));

    public static final net.neoforged.neoforge.registries.DeferredBlock<RFPowerBlock> RF_POWER_UNIT = BLOCKS.registerBlock("rf_power_unit", 
        RFPowerBlock::new, props -> props.mapColor(MapColor.STONE).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final net.neoforged.neoforge.registries.DeferredBlock<NaquadahGeneratorBlock> NAQUADAH_GENERATOR = BLOCKS.registerBlock("naquadah_generator", 
        NaquadahGeneratorBlock::new, props -> props.mapColor(MapColor.STONE).strength(5.0F, 6.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
}
