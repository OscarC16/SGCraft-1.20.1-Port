package gcewing.sgcraft.registry;

import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.world.inventory.DHDFuelMenu;
import gcewing.sgcraft.world.inventory.SGBaseMenu;
import gcewing.sgcraft.world.inventory.NaquadahGeneratorMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, SGCraft.MODID);

    public static final Supplier<MenuType<SGBaseMenu>> SG_BASE_MENU = MENUS.register("sg_base", 
        () -> IMenuTypeExtension.create((windowId, inv, data) -> new SGBaseMenu(windowId, inv, data)));

    public static final Supplier<MenuType<DHDFuelMenu>> DHD_FUEL_MENU = MENUS.register("dhd_fuel", 
        () -> IMenuTypeExtension.create((windowId, inv, data) -> new DHDFuelMenu(windowId, inv, data)));

    public static final Supplier<MenuType<NaquadahGeneratorMenu>> NAQUADAH_GENERATOR_MENU = MENUS.register("naquadah_generator", 
        () -> IMenuTypeExtension.create((windowId, inv, data) -> new NaquadahGeneratorMenu(windowId, inv, data)));
}
