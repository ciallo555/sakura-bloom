package dev.sakurabloom.adventure.pouch;

import dev.sakurabloom.SakuraBloom;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PouchContent {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SakuraBloom.ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, SakuraBloom.ID);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, SakuraBloom.ID);
    public static final DeferredItem<PouchItem> KNOTWORK = ITEMS.register("knotwork_pouch", () -> new PouchItem(4, 3));
    public static final DeferredItem<PouchItem> CLOUD = ITEMS.register("cloud_brocade_pouch", () -> new PouchItem(5, 4));
    static final DeferredHolder<DataComponentType<?>, DataComponentType<PouchLayout>> LAYOUT = COMPONENTS.register(
        "pouch_layout", () -> DataComponentType.<PouchLayout>builder().persistent(PouchLayout.CODEC).cacheEncoding().build());
    static final DeferredHolder<MenuType<?>, MenuType<PouchMenu>> MENU = MENUS.register(
        "charm_pouch", () -> IMenuTypeExtension.create((id, inv, buf) -> new PouchMenu(id, inv, buf.readVarInt())));

    private PouchContent() {}

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
        ITEMS.register(bus);
        MENUS.register(bus);
    }

    public static void display(CreativeModeTab.Output output) {
        output.accept(KNOTWORK.get());
        output.accept(CLOUD.get());
    }
}
