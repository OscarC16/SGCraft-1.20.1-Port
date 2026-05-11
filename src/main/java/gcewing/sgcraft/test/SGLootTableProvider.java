package gcewing.sgcraft.test;

import gcewing.sgcraft.registry.ModBlocks;
import gcewing.sgcraft.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class SGLootTableProvider {

    public static LootTableProvider create(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        return new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(SGBlockLoot::new, LootContextParamSets.BLOCK)
        ), registries);
    }

    public static class SGBlockLoot extends BlockLootSubProvider {
        protected SGBlockLoot(HolderLookup.Provider pRegistries) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), pRegistries);
        }

        @Override
        protected void generate() {
            // Drop base blocks
            this.dropSelf(ModBlocks.NAQUADAH_BLOCK.get());
            this.dropSelf(ModBlocks.STARGATE_RING.get());
            this.dropSelf(ModBlocks.STARGATE_CHEVRON.get());
            this.dropSelf(ModBlocks.STARGATE_BASE.get());
            this.dropSelf(ModBlocks.STARGATE_CONTROLLER.get());

            // Drop 2-4 naquadah items from ores, with fortune support
            this.add(ModBlocks.NAQUADAH_ORE.get(), block ->
                    this.createOreDrop(block, ModItems.NAQUADAH.get())
                            .apply(net.minecraft.world.level.storage.loot.functions.SetItemCountFunction.setCount(UniformGenerator.between(2.0f, 4.0f)))
            );

            this.add(ModBlocks.DEEPSLATE_NAQUADAH_ORE.get(), block ->
                    this.createOreDrop(block, ModItems.NAQUADAH.get())
                            .apply(net.minecraft.world.level.storage.loot.functions.SetItemCountFunction.setCount(UniformGenerator.between(2.0f, 4.0f)))
            );
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(
                ModBlocks.NAQUADAH_BLOCK.get(),
                ModBlocks.NAQUADAH_ORE.get(),
                ModBlocks.DEEPSLATE_NAQUADAH_ORE.get(),
                ModBlocks.STARGATE_RING.get(),
                ModBlocks.STARGATE_CHEVRON.get(),
                ModBlocks.STARGATE_BASE.get(),
                ModBlocks.STARGATE_CONTROLLER.get(),
                ModBlocks.STARGATE_IRIS.get()
            );
        }
    }
}
