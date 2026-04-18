package com.yision.fluidlogistics.handler.fluidpackage;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class BasinFluidPackageTargetAdapter implements FluidPackageTargetAdapter {

    @Override
    public boolean matches(BlockEntity target, IFluidHandler fluidHandler) {
        return target instanceof BasinBlockEntity;
    }

    @Override
    public boolean canAcceptAll(BlockEntity target, IFluidHandler fluidHandler, List<FluidStack> packageFluids) {
        if (!(target instanceof BasinBlockEntity basin)) {
            return false;
        }

        SmartFluidTankBehaviour inputTank = basin.getBehaviour(SmartFluidTankBehaviour.INPUT);
        if (inputTank == null) {
            return false;
        }

        IFluidHandler inputHandler = inputTank.getCapability();
        if (inputHandler == null) {
            return false;
        }

        List<TankSnapshot> tankSnapshots = new ArrayList<>();
        for (int tank = 0; tank < inputHandler.getTanks(); tank++) {
            tankSnapshots.add(new TankSnapshot(inputHandler.getFluidInTank(tank).copy(), inputHandler.getTankCapacity(tank)));
        }

        for (FluidStack fluid : packageFluids) {
            if (fluid.isEmpty()) {
                continue;
            }
            if (simulateVarietyFill(inputHandler, tankSnapshots, fluid.copy()) != fluid.getAmount()) {
                return false;
            }
        }

        return true;
    }

    private static int simulateVarietyFill(IFluidHandler fluidHandler, List<TankSnapshot> tankSnapshots, FluidStack fluid) {
        int filled = 0;
        boolean fittingTankFound = false;

        for (boolean searchPass : List.of(true, false)) {
            for (int tank = 0; tank < tankSnapshots.size(); tank++) {
                TankSnapshot snapshot = tankSnapshots.get(tank);

                if (searchPass && FluidStack.isSameFluidSameComponents(snapshot.fluid(), fluid)) {
                    fittingTankFound = true;
                }
                if (searchPass && !fittingTankFound) {
                    continue;
                }

                int filledIntoTank = fillTank(fluidHandler, tankSnapshots, tank, fluid);
                fluid.shrink(filledIntoTank);
                filled += filledIntoTank;

                if (fluid.isEmpty()) {
                    return filled;
                }
                if (fittingTankFound) {
                    return filled;
                }
            }
        }

        return filled;
    }

    private static int fillTank(IFluidHandler fluidHandler, List<TankSnapshot> tankSnapshots, int tank, FluidStack fluid) {
        TankSnapshot snapshot = tankSnapshots.get(tank);
        if (!snapshot.fluid().isEmpty() && !FluidStack.isSameFluidSameComponents(snapshot.fluid(), fluid)) {
            return 0;
        }
        if (snapshot.fluid().isEmpty() && !fluidHandler.isFluidValid(tank, fluid)) {
            return 0;
        }

        int filled = Math.min(snapshot.capacity() - snapshot.fluid().getAmount(), fluid.getAmount());
        if (filled <= 0) {
            return 0;
        }

        FluidStack updated = snapshot.fluid().isEmpty() ? fluid.copyWithAmount(filled) : snapshot.fluid().copy();
        if (!snapshot.fluid().isEmpty()) {
            updated.grow(filled);
        }
        tankSnapshots.set(tank, new TankSnapshot(updated, snapshot.capacity()));
        return filled;
    }

    private record TankSnapshot(FluidStack fluid, int capacity) {
    }
}
