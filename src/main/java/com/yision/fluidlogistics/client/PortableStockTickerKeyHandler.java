package com.yision.fluidlogistics.client;

import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.network.PortableStockTickerOpenPacket;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = FluidLogistics.MODID, value = Dist.CLIENT)
public final class PortableStockTickerKeyHandler {

    private PortableStockTickerKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        while (FluidLogisticsKeys.OPEN_PORTABLE_STOCK_TICKER.consumeClick()) {
            PortableStockTickerOpenPacket.send();
        }
    }
}
