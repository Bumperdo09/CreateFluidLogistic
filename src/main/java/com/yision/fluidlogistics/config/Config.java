package com.yision.fluidlogistics.config;

import com.simibubi.create.content.logistics.box.PackageItem;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = "fluidlogistics", bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final boolean FLUID_TRANSPORTER_ENABLED_DEFAULT = true;
    private static final int COMPRESSED_TANK_CAPACITY_DEFAULT = 10000;
    private static final int COMPRESSED_TANK_CAPACITY_MIN = 1000;
    private static final int COMPRESSED_TANK_CAPACITY_MAX = 10000;
    private static final int FLUID_PACKAGE_CAPACITY_DEFAULT = 10000;
    private static final int FLUID_PACKAGE_CAPACITY_MIN = 1;
    private static final int FLUID_PACKAGE_CAPACITY_MAX = COMPRESSED_TANK_CAPACITY_MAX * PackageItem.SLOTS;

    public static final ModConfigSpec.BooleanValue FLUID_TRANSPORTER_ENABLED = BUILDER
            .comment("Whether the Fluid Transporter is enabled")
            .translation("fluidlogistics.configuration.fluidTransporterEnabled")
            .define("fluidTransporterEnabled", FLUID_TRANSPORTER_ENABLED_DEFAULT);

    public static final ModConfigSpec.IntValue COMPRESSED_TANK_CAPACITY = BUILDER
            .comment("The maximum amount of fluid (in mB) a compressed tank can hold")
            .translation("fluidlogistics.configuration.compressedTankCapacity")
            .defineInRange("compressedTankCapacity",
                    COMPRESSED_TANK_CAPACITY_DEFAULT,
                    COMPRESSED_TANK_CAPACITY_MIN,
                    COMPRESSED_TANK_CAPACITY_MAX);

    public static final ModConfigSpec.IntValue FLUID_PACKAGE_CAPACITY = BUILDER
            .comment("The maximum amount of fluid (in mB) a fluid package can hold",
                    "Effective maximum is clamped to 9x the configured compressed tank capacity")
            .translation("fluidlogistics.configuration.fluidPackageCapacity")
            .defineInRange("fluidPackageCapacity",
                    FLUID_PACKAGE_CAPACITY_DEFAULT,
                    FLUID_PACKAGE_CAPACITY_MIN,
                    FLUID_PACKAGE_CAPACITY_MAX);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean fluidTransporterEnabled = FLUID_TRANSPORTER_ENABLED_DEFAULT;
    private static int compressedTankCapacity = COMPRESSED_TANK_CAPACITY_DEFAULT;
    private static int fluidPackageCapacity = FLUID_PACKAGE_CAPACITY_DEFAULT;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        fluidTransporterEnabled = FLUID_TRANSPORTER_ENABLED.get();
        compressedTankCapacity = COMPRESSED_TANK_CAPACITY.get();
        fluidPackageCapacity = FLUID_PACKAGE_CAPACITY.get();
    }

    public static boolean isFluidTransporterEnabled() {
        return fluidTransporterEnabled;
    }

    public static int getCompressedTankCapacity() {
        return compressedTankCapacity;
    }

    public static int getFluidPerPackage() {
        int maxFluidPerPackage = compressedTankCapacity * PackageItem.SLOTS;
        return Math.max(FLUID_PACKAGE_CAPACITY_MIN, Math.min(fluidPackageCapacity, maxFluidPerPackage));
    }
}
