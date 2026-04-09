package com.yision.fluidlogistics.client;

import java.lang.reflect.Method;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;

public final class JeiClientBridge {

    private static final String TOOLTIP_PROVIDER_CLASS = "com.yision.fluidlogistics.compat.jei.JeiFluidTooltipProvider";

    private static boolean jeiAvailable;

    @Nullable
    private static Class<?> tooltipProviderClass;

    private JeiClientBridge() {
    }

    public static List<Component> getFluidTooltipLines(FluidStack fluid) {
        List<Component> lines = invokeListMethod("getFluidTooltipLines", fluid);
        if (lines != null && !lines.isEmpty()) {
            return lines;
        }
        return List.of(fluid.getHoverName());
    }

    public static void renderFluidTooltip(GuiGraphics graphics, Font fallbackFont, FluidStack fluid, int x, int y) {
        List<Component> lines = invokeListMethod("getFluidTooltipLines", fluid);
        if (lines != null && !lines.isEmpty()) {
            Font font = invokeFontMethod("getFluidTooltipFont", fluid);
            graphics.renderComponentTooltip(font != null ? font : fallbackFont, lines, x, y);
            return;
        }

        graphics.renderComponentTooltip(fallbackFont, List.of(fluid.getHoverName()), x, y);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static List<Component> invokeListMethod(String methodName, FluidStack fluid) {
        ensureInitialized();
        if (!jeiAvailable || tooltipProviderClass == null) {
            return null;
        }

        try {
            Method method = tooltipProviderClass.getMethod(methodName, FluidStack.class);
            return (List<Component>) method.invoke(null, fluid);
        } catch (ReflectiveOperationException | ClassCastException | LinkageError e) {
            return null;
        }
    }

    @Nullable
    private static Font invokeFontMethod(String methodName, FluidStack fluid) {
        ensureInitialized();
        if (!jeiAvailable || tooltipProviderClass == null) {
            return null;
        }

        try {
            Method method = tooltipProviderClass.getMethod(methodName, FluidStack.class);
            return (Font) method.invoke(null, fluid);
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    private static void ensureInitialized() {
        if (jeiAvailable && tooltipProviderClass != null) {
            return;
        }

        if (!ModList.get().isLoaded("jei")) {
            reset();
            return;
        }

        try {
            ClassLoader classLoader = JeiClientBridge.class.getClassLoader();
            tooltipProviderClass = Class.forName(TOOLTIP_PROVIDER_CLASS, false, classLoader);
            jeiAvailable = true;
        } catch (ReflectiveOperationException | LinkageError e) {
            reset();
        }
    }

    private static void reset() {
        jeiAvailable = false;
        tooltipProviderClass = null;
    }
}
