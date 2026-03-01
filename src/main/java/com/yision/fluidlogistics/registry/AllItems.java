package com.yision.fluidlogistics.registry;

import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.foundation.data.AssetLookup;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.yision.fluidlogistics.item.CompressedTankItem;
import com.yision.fluidlogistics.item.FluidPackageItem;

import static com.yision.fluidlogistics.FluidLogistics.REGISTRATE;

public class AllItems {

    public static final ItemEntry<CompressedTankItem> COMPRESSED_STORAGE_TANK = REGISTRATE
            .item("compressed_storage_tank", CompressedTankItem::new)
            .properties(p -> p.stacksTo(1))
            .model(AssetLookup.existingItemModel())
            .register();

    public static final ItemEntry<FluidPackageItem> RARE_FLUID_PACKAGE = REGISTRATE
            .item("rare_fluid_package", FluidPackageItem::new)
            .properties(p -> p.stacksTo(1))
            .tag(AllItemTags.PACKAGES.tag)
            .model(AssetLookup.existingItemModel())
            .lang("Rare Fluid Package")
            .register();

    public static void register() {
    }
}
