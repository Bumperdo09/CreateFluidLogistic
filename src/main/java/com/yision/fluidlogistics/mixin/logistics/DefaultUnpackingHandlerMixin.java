package com.yision.fluidlogistics.mixin.logistics;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.impl.unpacking.DefaultUnpackingHandler;
import com.yision.fluidlogistics.item.CompressedTankItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandler;

@Mixin(DefaultUnpackingHandler.class)
public abstract class DefaultUnpackingHandlerMixin implements UnpackingHandler {

    private record TankSnapshot(FluidStack fluid, int capacity) {
    }

    @Inject(
        method = "unpack",
        at = @At("HEAD"),
        cancellable = true
    )
    private void fluidlogistics$onUnpack(Level level, BlockPos pos, BlockState state, Direction side,
            List<ItemStack> items, @Nullable PackageOrderWithCrafts orderContext, boolean simulate,
            CallbackInfoReturnable<Boolean> cir) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return;
        }

        IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, state, blockEntity, side);
        if (itemHandler == null) {
            return;
        }

        IFluidHandler fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, state, blockEntity, side);
        if (fluidHandler == null) {
            return;
        }

        boolean hasCompressedTank = false;
        boolean processedAny = false;
        List<FluidStack> packageFluids = new ArrayList<>();
        for (ItemStack item : items) {
            if (!(item.getItem() instanceof CompressedTankItem)) {
                continue;
            }

            FluidStack fluid = CompressedTankItem.getFluid(item);
            if (fluid.isEmpty()) {
                continue;
            }

            hasCompressedTank = true;
            for (int count = 0; count < item.getCount(); count++) {
                packageFluids.add(fluid.copy());
            }
        }

        if (!hasCompressedTank) {
            return;
        }

        if (!fluidlogistics$canAcceptAllFluids(fluidHandler, packageFluids)) {
            cir.setReturnValue(false);
            return;
        }

        for (FluidStack fluid : packageFluids) {
            int accepted = fluidHandler.fill(fluid.copy(), FluidAction.SIMULATE);
            if (accepted != fluid.getAmount()) {
                cir.setReturnValue(false);
                return;
            }
        }

        Iterator<ItemStack> iterator = items.iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (!(item.getItem() instanceof CompressedTankItem)) {
                continue;
            }

            FluidStack fluid = CompressedTankItem.getFluid(item);
            if (fluid.isEmpty()) {
                continue;
            }

            int stackCount = item.getCount();
            int insertedCount = 0;
            for (int count = 0; count < stackCount; count++) {
                int filled = fluidHandler.fill(fluid.copy(), simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
                if (filled != fluid.getAmount()) {
                    break;
                }

                insertedCount++;
            }

            if (insertedCount == 0) {
                continue;
            }

            processedAny = true;

            if (insertedCount >= stackCount) {
                iterator.remove();
            } else {
                item.shrink(insertedCount);
            }

            if (!simulate) {
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }

        if (!processedAny) {
            return;
        }
    }

    private static boolean fluidlogistics$canAcceptAllFluids(IFluidHandler fluidHandler, List<FluidStack> packageFluids) {
        int totalCapacity = 0;
        int totalFreeSpace = 0;
        List<TankSnapshot> tankSnapshots = new ArrayList<>();

        for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
            FluidStack tankFluid = fluidHandler.getFluidInTank(tank).copy();
            int tankCapacity = fluidHandler.getTankCapacity(tank);
            totalCapacity += tankCapacity;
            totalFreeSpace += Math.max(0, tankCapacity - tankFluid.getAmount());
            tankSnapshots.add(new TankSnapshot(tankFluid, tankCapacity));
        }

        int totalPackageFluidAmount = 0;
        for (FluidStack fluid : packageFluids) {
            totalPackageFluidAmount += fluid.getAmount();
        }

        if (totalPackageFluidAmount > totalCapacity || totalPackageFluidAmount > totalFreeSpace) {
            return false;
        }

        for (FluidStack fluid : packageFluids) {
            if (!fluidlogistics$reserveFluid(tankSnapshots, fluid)) {
                return false;
            }
        }

        return true;
    }

    private static boolean fluidlogistics$reserveFluid(List<TankSnapshot> tankSnapshots, FluidStack fluid) {
        int remaining = fluid.getAmount();

        for (int tank = 0; tank < tankSnapshots.size() && remaining > 0; tank++) {
            TankSnapshot snapshot = tankSnapshots.get(tank);
            if (snapshot.fluid().isEmpty()) {
                continue;
            }
            if (!FluidStack.isSameFluidSameComponents(snapshot.fluid(), fluid)) {
                continue;
            }

            int space = snapshot.capacity() - snapshot.fluid().getAmount();
            if (space <= 0) {
                continue;
            }

            int inserted = Math.min(space, remaining);
            FluidStack updated = snapshot.fluid().copy();
            updated.grow(inserted);
            tankSnapshots.set(tank, new TankSnapshot(updated, snapshot.capacity()));
            remaining -= inserted;
        }

        for (int tank = 0; tank < tankSnapshots.size() && remaining > 0; tank++) {
            TankSnapshot snapshot = tankSnapshots.get(tank);
            if (!snapshot.fluid().isEmpty()) {
                continue;
            }

            int inserted = Math.min(snapshot.capacity(), remaining);
            FluidStack updated = fluid.copyWithAmount(inserted);
            tankSnapshots.set(tank, new TankSnapshot(updated, snapshot.capacity()));
            remaining -= inserted;
        }

        return remaining == 0;
    }
}
