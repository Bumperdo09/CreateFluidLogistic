package com.yision.fluidlogistics.compat.jei;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.compat.jei.CreateJEI;
import com.yision.fluidlogistics.portableticker.PortableStockTickerScreen;

import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.renderer.Rect2i;

public class PortableStockTickerGuiHandler implements IGuiContainerHandler<PortableStockTickerScreen> {

    @Override
    public @NotNull List<Rect2i> getGuiExtraAreas(PortableStockTickerScreen containerScreen) {
        return containerScreen.getExtraAreas();
    }

    @Override
    public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(PortableStockTickerScreen containerScreen,
            double mouseX, double mouseY) {
        if (CreateJEI.runtime == null) {
            return Optional.empty();
        }
        return containerScreen.getHoveredIngredient((int) mouseX, (int) mouseY)
                .flatMap(pair -> CreateJEI.runtime.getIngredientManager()
                        .createClickableIngredient(pair.getFirst(), pair.getSecond(), true));
    }
}
