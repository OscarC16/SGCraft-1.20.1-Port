package gcewing.sgcraft.registry;

import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.world.inventory.DHDFuelMenu;
import gcewing.sgcraft.world.inventory.SGBaseMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, SGCraft.MODID);

    public static final RegistryObject<MenuType<SGBaseMenu>> SG_BASE_MENU = registerMenuType("sg_base", SGBaseMenu::new);
    public static final RegistryObject<MenuType<DHDFuelMenu>> DHD_FUEL_MENU = registerMenuType("dhd_fuel", DHDFuelMenu::new);

    private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IForgeMenuType.create(factory));
    }
}
