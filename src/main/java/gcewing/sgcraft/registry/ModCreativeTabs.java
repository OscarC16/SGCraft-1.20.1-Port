package gcewing.sgcraft.registry;

import gcewing.sgcraft.SGCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SGCraft.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SGCRAFT_TAB = CREATIVE_MODE_TABS.register("sgcraft_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.sgcraft"))
            .icon(() -> new ItemStack(ModBlocks.STARGATE_BASE.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModBlocks.NAQUADAH_ORE.get());
                output.accept(ModBlocks.DEEPSLATE_NAQUADAH_ORE.get());
                output.accept(ModBlocks.NAQUADAH_BLOCK.get());
                output.accept(ModItems.NAQUADAH.get());
                output.accept(ModItems.NAQUADAH_INGOT.get());
                output.accept(ModBlocks.STARGATE_RING.get());
                output.accept(ModBlocks.STARGATE_CHEVRON.get());
                output.accept(ModBlocks.STARGATE_BASE.get());
                output.accept(ModItems.STARGATE_CONTROLLER_ITEM.get());
                output.accept(ModItems.SG_CORE_CRYSTAL.get());
                output.accept(ModItems.SG_CONTROLLER_CRYSTAL.get());
                output.accept(ModItems.SG_CHEVRON_UPGRADE.get());
                output.accept(ModItems.SG_IRIS_UPGRADE.get());
                output.accept(ModItems.SG_IRIS_BLADE.get());
                output.accept(ModItems.NAQUADAH_REACTOR_CORE.get());
                output.accept(ModItems.CHEVRON_ASSEMBLY.get());
                output.accept(ModItems.STARGATE_PAD.get());
                output.accept(ModItems.RF_POWER_UNIT_ITEM.get());
                output.accept(ModItems.NAQUADAH_GENERATOR_ITEM.get());
            }).build());
}
