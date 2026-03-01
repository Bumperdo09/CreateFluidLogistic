package com.yision.fluidlogistics.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer;
import com.yision.fluidlogistics.item.CompressedTankItem;
import com.yision.fluidlogistics.render.FluidSlotRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

@OnlyIn(Dist.CLIENT)
@Mixin(ValueBoxRenderer.class)
public class ValueBoxRendererMixin {

    @Inject(
        method = "renderItemIntoValueBox",
        at = @At("HEAD"),
        cancellable = true,
        remap = true
    )
    private static void fluidlogistics$renderFluidIntoValueBox(ItemStack filter, PoseStack ms, 
            MultiBufferSource buffer, int light, int overlay, CallbackInfo ci) {
        if (filter.getItem() instanceof CompressedTankItem && CompressedTankItem.isVirtual(filter)) {
            FluidStack fluid = CompressedTankItem.getFluid(filter);
            if (!fluid.isEmpty()) {
                FluidSlotRenderer.renderFluidInWorld(fluid, ms, buffer, light);
                ci.cancel();
            }
        }
    }
}
