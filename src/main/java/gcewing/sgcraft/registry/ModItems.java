package gcewing.sgcraft.registry;

import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.item.DHDBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SGCraft.MODID);

    // Básicos
    public static final RegistryObject<Item> NAQUADAH = ITEMS.register("naquadah",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> NAQUADAH_INGOT = ITEMS.register("naquadah_ingot",
            () -> new Item(new Item.Properties()));

    // Cristales
    public static final RegistryObject<Item> SG_CORE_CRYSTAL = ITEMS.register("sg_core_crystal",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SG_CONTROLLER_CRYSTAL = ITEMS.register("sg_controller_crystal",
            () -> new Item(new Item.Properties()));

    // Mejoras (Registrados como ítems básicos por ahora)
    public static final RegistryObject<Item> SG_CHEVRON_UPGRADE = ITEMS.register("sg_chevron_upgrade",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SG_IRIS_UPGRADE = ITEMS.register("sg_iris_upgrade",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SG_IRIS_BLADE = ITEMS.register("sg_iris_blade",
            () -> new Item(new Item.Properties()));

    // Block Items
    public static final RegistryObject<Item> NAQUADAH_BLOCK_ITEM = ITEMS.register("naquadah_block",
            () -> new BlockItem(ModBlocks.NAQUADAH_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> NAQUADAH_ORE_ITEM = ITEMS.register("naquadah_ore",
            () -> new BlockItem(ModBlocks.NAQUADAH_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> DEEPSLATE_NAQUADAH_ORE_ITEM = ITEMS.register("deepslate_naquadah_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_NAQUADAH_ORE.get(), new Item.Properties()));

    // Block Items Estructurales
    public static final RegistryObject<Item> STARGATE_RING_ITEM = ITEMS.register("stargate_ring",
            () -> new BlockItem(ModBlocks.STARGATE_RING.get(), new Item.Properties()));

    public static final RegistryObject<Item> STARGATE_CHEVRON_ITEM = ITEMS.register("stargate_chevron",
            () -> new BlockItem(ModBlocks.STARGATE_CHEVRON.get(), new Item.Properties()));

    public static final RegistryObject<Item> STARGATE_BASE_ITEM = ITEMS.register("stargate_base",
            () -> new BlockItem(ModBlocks.STARGATE_BASE.get(), new Item.Properties()));

    public static final RegistryObject<Item> STARGATE_CONTROLLER_ITEM = ITEMS.register("stargate_controller",
            () -> new DHDBlockItem(ModBlocks.STARGATE_CONTROLLER.get(), new Item.Properties()));
}
