package com.yision.fluidlogistics.handler.fluidpackage;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class FluidPackageTargetAdapters {

    private static final List<FluidPackageTargetAdapter> ADAPTERS = List.of(
        new BasinFluidPackageTargetAdapter(),
        new SharedCapacityFluidPackageTargetAdapter()
    );

    private FluidPackageTargetAdapters() {
    }

    @Nullable
    public static FluidPackageTargetAdapter find(BlockEntity target, IFluidHandler fluidHandler) {
        for (FluidPackageTargetAdapter adapter : ADAPTERS) {
            if (adapter.matches(target, fluidHandler)) {
                return adapter;
            }
        }
        return null;
    }
}
