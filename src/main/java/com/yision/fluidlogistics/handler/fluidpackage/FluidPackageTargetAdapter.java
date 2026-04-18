package com.yision.fluidlogistics.handler.fluidpackage;

import java.util.List;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public interface FluidPackageTargetAdapter {

    boolean matches(BlockEntity target, IFluidHandler fluidHandler);

    boolean canAcceptAll(BlockEntity target, IFluidHandler fluidHandler, List<FluidStack> packageFluids);
}
