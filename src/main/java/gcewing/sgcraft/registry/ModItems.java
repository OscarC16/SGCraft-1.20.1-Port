package gcewing.sgcraft.registry;

import gcewing.sgcraft.SGCraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SGCraft.MODID);

    public static final DeferredItem<Item> NAQUADAH = ITEMS.registerSimpleItem("naquadah", new Item.Properties());
    public static final DeferredItem<Item> NAQUADAH_INGOT = ITEMS.registerSimpleItem("naquadah_ingot", new Item.Properties());
    public static final DeferredItem<Item> SG_CORE_CRYSTAL = ITEMS.registerSimpleItem("sg_core_crystal", new Item.Properties());
    public static final DeferredItem<Item> SG_CONTROLLER_CRYSTAL = ITEMS.registerSimpleItem("sg_controller_crystal", new Item.Properties());
    public static final DeferredItem<Item> SG_CHEVRON_UPGRADE = ITEMS.registerSimpleItem("sg_chevron_upgrade", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SG_IRIS_UPGRADE = ITEMS.registerSimpleItem("sg_iris_upgrade", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SG_IRIS_BLADE = ITEMS.registerSimpleItem("sg_iris_blade", new Item.Properties());

    public static final DeferredItem<BlockItem> NAQUADAH_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.NAQUADAH_BLOCK);
    public static final DeferredItem<BlockItem> NAQUADAH_ORE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.NAQUADAH_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_NAQUADAH_ORE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_NAQUADAH_ORE);
    public static final DeferredItem<BlockItem> STARGATE_RING_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.STARGATE_RING);
    public static final DeferredItem<BlockItem> STARGATE_CHEVRON_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.STARGATE_CHEVRON);
    public static final DeferredItem<BlockItem> STARGATE_BASE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.STARGATE_BASE);
    public static final DeferredItem<BlockItem> STARGATE_CONTROLLER_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.STARGATE_CONTROLLER);
}
