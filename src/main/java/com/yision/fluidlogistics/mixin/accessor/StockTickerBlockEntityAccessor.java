package com.yision.fluidlogistics.mixin.accessor;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;

import net.minecraft.world.item.ItemStack;

@Mixin(StockTickerBlockEntity.class)
public interface StockTickerBlockEntityAccessor {

    @Accessor("categories")
    List<ItemStack> fluidlogistics$getCategories();
}
