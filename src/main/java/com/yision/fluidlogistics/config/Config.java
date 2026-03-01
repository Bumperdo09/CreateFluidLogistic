package com.yision.fluidlogistics.config;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = "fluidlogistics", bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue COMPRESSED_TANK_CAPACITY = BUILDER
            .comment("The maximum amount of fluid (in mB) a compressed tank can hold")
            .defineInRange("compressedTankCapacity", 10000, 1000, 10000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static int compressedTankCapacity;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        compressedTankCapacity = COMPRESSED_TANK_CAPACITY.get();
    }

    public static int getCompressedTankCapacity() {
        return compressedTankCapacity;
    }

    public static int getFluidPerPackage() {
        return compressedTankCapacity * 9;
    }
}
