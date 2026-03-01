package com.yision.fluidlogistics.mixin.processing;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.yision.fluidlogistics.item.CompressedTankItem;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

@Mixin(BasinBlock.class)
public abstract class BasinBlockMixin {

    private static final int MAX_FLUID_AMOUNT_MB = 1000;

    @Inject(
        method = "updateEntityAfterFallOn",
        at = @At("HEAD"),
        cancellable = true,
        remap = true
    )
    private void fluidlogistics$onItemFallIntoBasin(BlockGetter worldIn, Entity entityIn, CallbackInfo ci) {
        if (!worldIn.getBlockState(entityIn.blockPosition()).getBlock().equals(this)) {
            return;
        }
        
        if (!(entityIn instanceof ItemEntity itemEntity)) {
            return;
        }
        
        if (!entityIn.isAlive()) {
            return;
        }
        
        ItemStack itemStack = itemEntity.getItem();
        if (!(itemStack.getItem() instanceof CompressedTankItem)) {
            return;
        }
        
        FluidStack fluidStack = CompressedTankItem.getFluid(itemStack);
        if (fluidStack.isEmpty()) {
            return;
        }
        
        if (fluidStack.getAmount() > MAX_FLUID_AMOUNT_MB) {
            return;
        }
        
        if (!(worldIn instanceof Level level)) {
            return;
        }
        
        BlockPos pos = entityIn.blockPosition();
        if (!(level.getBlockEntity(pos) instanceof BasinBlockEntity basinBE)) {
            return;
        }
        
        SmartFluidTankBehaviour inputTank = basinBE.inputTank;
        if (inputTank == null) {
            return;
        }
        
        IFluidHandler fluidHandler = inputTank.getCapability();
        if (fluidHandler == null) {
            return;
        }
        
        int filled = fluidHandler.fill(fluidStack.copy(), IFluidHandler.FluidAction.EXECUTE);
        if (filled <= 0) {
            return;
        }
        
        itemStack.shrink(1);
        if (itemStack.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setItem(itemStack);
        }
        
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
        
        ci.cancel();
    }
}
