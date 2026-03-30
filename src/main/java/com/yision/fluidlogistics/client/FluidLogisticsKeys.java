package com.yision.fluidlogistics.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.yision.fluidlogistics.FluidLogistics;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = FluidLogistics.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class FluidLogisticsKeys {

    public static final KeyMapping OPEN_PORTABLE_STOCK_TICKER = new KeyMapping(
            "key.fluidlogistics.open_portable_stock_ticker",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.fluidlogistics"
    );

    private FluidLogisticsKeys() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_PORTABLE_STOCK_TICKER);
    }
}
