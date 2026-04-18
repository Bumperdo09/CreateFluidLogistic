package com.yision.fluidlogistics.handler.fluidpackage;

import java.util.List;

import com.yision.fluidlogistics.util.SharedCapacityFluidHandler;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class SharedCapacityFluidPackageTargetAdapter implements FluidPackageTargetAdapter {

    @Override
    public boolean matches(BlockEntity target, IFluidHandler fluidHandler) {
        return fluidHandler instanceof SharedCapacityFluidHandler;
    }

    @Override
    public boolean canAcceptAll(BlockEntity target, IFluidHandler fluidHandler, List<FluidStack> packageFluids) {
        if (!(fluidHandler instanceof SharedCapacityFluidHandler sharedCapacityFluidHandler)) {
            return false;
        }
        return sharedCapacityFluidHandler.canFillAll(packageFluids);
    }
}
