package com.yision.fluidlogistics.handler;

import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.yision.fluidlogistics.item.CompressedTankItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;

public class FluidPackageUnpackingHandler implements UnpackingHandler {

    public static final FluidPackageUnpackingHandler INSTANCE = new FluidPackageUnpackingHandler();

    @Override
    public boolean unpack(Level level, BlockPos pos, BlockState state, Direction side, List<ItemStack> items,
                          @Nullable PackageOrderWithCrafts orderContext, boolean simulate) {
        IFluidHandler fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, state, null, side);
        if (fluidHandler == null) {
            return false;
        }

        int totalFluidAmount = 0;
        for (ItemStack item : items) {
            if (item.getItem() instanceof CompressedTankItem) {
                FluidStack fluid = CompressedTankItem.getFluid(item);
                if (!fluid.isEmpty()) {
                    totalFluidAmount += fluid.getAmount();
                }
            }
        }

        if (totalFluidAmount > 0) {
            int totalCapacity = 0;
            for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
                FluidStack tankFluid = fluidHandler.getFluidInTank(tank);
                int tankCapacity = fluidHandler.getTankCapacity(tank);
                totalCapacity += (tankCapacity - tankFluid.getAmount());
            }

            if (totalCapacity < totalFluidAmount) {
                return false;
            }
        }

        if (simulate) {
            return true;
        }

        Iterator<ItemStack> iterator = items.iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (item.getItem() instanceof CompressedTankItem) {
                FluidStack fluid = CompressedTankItem.getFluid(item);
                if (!fluid.isEmpty()) {
                    fluidHandler.fill(fluid, FluidAction.EXECUTE);
                    iterator.remove();
                } else {
                    iterator.remove();
                }
            }
        }

        return true;
    }

    public boolean unpackIntoItemHandler(IItemHandler itemHandler, List<ItemStack> items, boolean simulate) {
        boolean anyProcessed = false;

        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            if (item.getItem() instanceof CompressedTankItem) {
                FluidStack fluid = CompressedTankItem.getFluid(item);
                if (!fluid.isEmpty()) {
                    ItemStack tankCopy = item.copy();
                    ItemStack remainder = ItemHandlerHelper.insertItemStacked(itemHandler, tankCopy, simulate);
                    if (remainder.isEmpty()) {
                        if (!simulate) {
                            items.set(i, ItemStack.EMPTY);
                        }
                        anyProcessed = true;
                    }
                } else {
                    ItemStack remainder = ItemHandlerHelper.insertItemStacked(itemHandler, item.copy(), simulate);
                    if (remainder.isEmpty()) {
                        if (!simulate) {
                            items.set(i, ItemStack.EMPTY);
                        }
                        anyProcessed = true;
                    }
                }
            }
        }

        items.removeIf(ItemStack::isEmpty);
        return anyProcessed;
    }
}
