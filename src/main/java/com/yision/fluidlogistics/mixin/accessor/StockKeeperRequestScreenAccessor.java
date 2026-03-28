package com.yision.fluidlogistics.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;

import net.minecraft.client.gui.GuiGraphics;

@Mixin(StockKeeperRequestScreen.class)
public interface StockKeeperRequestScreenAccessor {

    @Invoker(value = "drawItemCount", remap = false)
    void callDrawItemCount(GuiGraphics graphics, int count, int customCount);
}
