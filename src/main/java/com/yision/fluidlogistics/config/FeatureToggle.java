package com.yision.fluidlogistics.config;

import com.yision.fluidlogistics.FluidLogistics;
import net.minecraft.resources.ResourceLocation;

public final class FeatureToggle {
    public static final ResourceLocation FLUID_TRANSPORTER = FluidLogistics.asResource("fluid_transporter");

    private FeatureToggle() {
    }

    public static boolean isEnabled(ResourceLocation feature) {
        if (FLUID_TRANSPORTER.equals(feature)) {
            return Config.isFluidTransporterEnabled();
        }
        return true;
    }
}
