package com.yision.fluidlogistics.mixin.processing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.impl.unpacking.BasinUnpackingHandler;
import com.yision.fluidlogistics.item.CompressedTankItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

@Mixin(BasinUnpackingHandler.class)
public abstract class BasinUnpackingHandlerMixin {

    private static final int MAX_FLUID_AMOUNT_MB = 1000;

    @Inject(
        method = "unpack",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void fluidlogistics$onUnpack(Level level, BlockPos pos, BlockState state, Direction side,
            List<ItemStack> items, @Nullable PackageOrderWithCrafts orderContext, boolean simulate,
            CallbackInfoReturnable<Boolean> cir) {
        
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BasinBlockEntity basinBE)) {
            return;
        }
        
        boolean hasSmallTank = false;
        for (ItemStack item : items) {
            if (item.getItem() instanceof CompressedTankItem) {
                FluidStack fluid = CompressedTankItem.getFluid(item);
                if (!fluid.isEmpty() && fluid.getAmount() <= MAX_FLUID_AMOUNT_MB) {
                    hasSmallTank = true;
                    break;
                }
            }
        }
        
        if (!hasSmallTank) {
            return;
        }
        
        if (simulate) {
            IFluidHandler fluidHandler = basinBE.inputTank.getCapability();
            if (fluidHandler == null) {
                return;
            }
            
            Map<net.minecraft.resources.ResourceLocation, Integer> fluidsToFill = new HashMap<>();
            for (ItemStack item : items) {
                if (item.getItem() instanceof CompressedTankItem) {
                    FluidStack fluid = CompressedTankItem.getFluid(item);
                    if (!fluid.isEmpty() && fluid.getAmount() <= MAX_FLUID_AMOUNT_MB) {
                        net.minecraft.resources.ResourceLocation fluidId = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid.getFluid());
                        fluidsToFill.merge(fluidId, fluid.getAmount(), Integer::sum);
                    }
                }
            }
            
            if (!fluidsToFill.isEmpty()) {
                Map<net.minecraft.resources.ResourceLocation, Integer> existingFluids = new HashMap<>();
                for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
                    FluidStack tankFluid = fluidHandler.getFluidInTank(tank);
                    if (!tankFluid.isEmpty()) {
                        net.minecraft.resources.ResourceLocation fluidId = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(tankFluid.getFluid());
                        existingFluids.merge(fluidId, tankFluid.getAmount(), Integer::sum);
                    }
                }
                
                for (Map.Entry<net.minecraft.resources.ResourceLocation, Integer> entry : fluidsToFill.entrySet()) {
                    net.minecraft.resources.ResourceLocation fluidId = entry.getKey();
                    int toFill = entry.getValue();
                    int existing = existingFluids.getOrDefault(fluidId, 0);
                    int availableForFluid = MAX_FLUID_AMOUNT_MB - existing;
                    
                    if (availableForFluid < toFill) {
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
        } else {
            IFluidHandler fluidHandler = basinBE.inputTank.getCapability();
            if (fluidHandler == null) {
                return;
            }
            
            boolean anyFluidConverted = false;
            Iterator<ItemStack> iterator = items.iterator();
            while (iterator.hasNext()) {
                ItemStack item = iterator.next();
                if (item.getItem() instanceof CompressedTankItem) {
                    FluidStack fluid = CompressedTankItem.getFluid(item);
                    if (!fluid.isEmpty() && fluid.getAmount() <= MAX_FLUID_AMOUNT_MB) {
                        int filled = fluidHandler.fill(fluid.copy(), IFluidHandler.FluidAction.EXECUTE);
                        if (filled > 0) {
                            iterator.remove();
                            anyFluidConverted = true;
                        }
                    }
                }
            }
            
            if (anyFluidConverted) {
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }
        
        if (items.isEmpty()) {
            cir.setReturnValue(true);
            return;
        }
        
        basinBE.inputInventory.packagerMode = true;
        try {
            boolean result = UnpackingHandler.DEFAULT.unpack(level, pos, state, side, items, orderContext, simulate);
            cir.setReturnValue(result);
        } finally {
            basinBE.inputInventory.packagerMode = false;
        }
    }
}
