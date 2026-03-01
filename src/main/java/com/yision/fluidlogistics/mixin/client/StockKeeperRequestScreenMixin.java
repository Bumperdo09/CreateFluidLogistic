package com.yision.fluidlogistics.mixin.client;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.yision.fluidlogistics.item.CompressedTankItem;
import com.yision.fluidlogistics.render.FluidSlotAmountRenderer;
import com.yision.fluidlogistics.render.FluidSlotRenderer;
import com.yision.fluidlogistics.util.FluidAmountHelper;

import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;

@Mixin(StockKeeperRequestScreen.class)
public abstract class StockKeeperRequestScreenMixin {

    @Unique
    private boolean fluidlogistics$isCompressedTank = false;

    @Unique
    private int fluidlogistics$fluidAmount = 0;

    @Unique
    private FluidStack fluidlogistics$cachedFluid = null;

    @Inject(
        method = "renderItemEntry",
        at = @At("HEAD"),
        remap = false,
        cancellable = true
    )
    private void fluidlogistics$onRenderItemEntryHead(GuiGraphics graphics, float scale, BigItemStack entry, 
            boolean isStackHovered, boolean isRenderingOrders, CallbackInfo ci) {
        fluidlogistics$isCompressedTank = false;
        fluidlogistics$fluidAmount = 0;
        fluidlogistics$cachedFluid = null;

        ItemStack stack = entry.stack;
        if (stack.getItem() instanceof CompressedTankItem && CompressedTankItem.isVirtual(stack)) {
            FluidStack fluid = CompressedTankItem.getFluid(stack);
            if (!fluid.isEmpty()) {
                fluidlogistics$isCompressedTank = true;
                fluidlogistics$fluidAmount = entry.count;
                fluidlogistics$cachedFluid = fluid;
            }
        }
    }



    @Redirect(
        method = "renderItemEntry",
        at = @At(
            value = "INVOKE",
            target = "Lnet/createmod/catnip/gui/element/GuiGameElement;of(Lnet/minecraft/world/item/ItemStack;)Lnet/createmod/catnip/gui/element/GuiGameElement$GuiRenderBuilder;",
            remap = false
        ),
        remap = false
    )
    private GuiGameElement.GuiRenderBuilder fluidlogistics$redirectGuiGameElementOf(
            ItemStack itemStack,
            @Local(argsOnly = true) GuiGraphics graphics) {
        if (fluidlogistics$isCompressedTank && fluidlogistics$cachedFluid != null) {
            FluidSlotRenderer.renderFluidSlot(graphics, 0, 0, fluidlogistics$cachedFluid);
            return GuiGameElement.of(Blocks.AIR.asItem().getDefaultInstance());
        }
        return GuiGameElement.of(itemStack);
    }

    @Redirect(
        method = "renderItemEntry",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/logistics/stockTicker/StockKeeperRequestScreen;" +
                     "drawItemCount(Lnet/minecraft/client/gui/GuiGraphics;II)V",
            remap = false
        ),
        remap = false
    )
    private void fluidlogistics$redirectDrawItemCount(StockKeeperRequestScreen instance, 
            GuiGraphics graphics, int count, int customCount) {
        if (fluidlogistics$isCompressedTank) {
            if (fluidlogistics$fluidAmount > 1) {
                FluidSlotAmountRenderer.render(graphics, fluidlogistics$fluidAmount);
            }
            return;
        }
        ((StockKeeperRequestScreenAccessor) instance).callDrawItemCount(graphics, count, customCount);
    }

    @Redirect(
        method = "renderForeground",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;renderTooltip(" +
                     "Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V",
            remap = true
        )
    )
    private void fluidlogistics$redirectTooltip(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        if (stack.getItem() instanceof CompressedTankItem && CompressedTankItem.isVirtual(stack)) {
            FluidStack fluid = CompressedTankItem.getFluid(stack);
            if (!fluid.isEmpty()) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(fluid.getHoverName());
                tooltip.add(Component.literal(FluidAmountHelper.formatDetailed(fluid.getAmount()))
                    .withStyle(ChatFormatting.GRAY));
                graphics.renderComponentTooltip(font, tooltip, x, y);
                return;
            }
        }
        graphics.renderTooltip(font, stack, x, y);
    }

    @ModifyExpressionValue(
        method = "mouseClicked",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I",
            remap = true
        ),
        remap = false
    )
    private int fluidlogistics$modifyTransferShiftForFluid(int original, @Local BigItemStack entry) {
        if (entry.stack.getItem() instanceof CompressedTankItem) {
            return 50000;
        }
        return original;
    }

    @ModifyExpressionValue(
        method = "mouseClicked",
        at = @At(
            value = "CONSTANT",
            args = "intValue=10"
        ),
        remap = false
    )
    private int fluidlogistics$modifyTransferCtrlForFluid(int original, @Local BigItemStack entry) {
        if (entry.stack.getItem() instanceof CompressedTankItem) {
            return 10000;
        }
        return original;
    }

    @ModifyExpressionValue(
        method = "mouseClicked",
        at = @At(
            value = "CONSTANT",
            args = "intValue=1"
        ),
        remap = false
    )
    private int fluidlogistics$modifyTransferNormalForFluid(int original, @Local BigItemStack entry) {
        if (entry.stack.getItem() instanceof CompressedTankItem) {
            return 1000;
        }
        return original;
    }

    @ModifyExpressionValue(
        method = "mouseScrolled",
        at = @At(
            value = "CONSTANT",
            args = "intValue=10"
        ),
        remap = false
    )
    private int fluidlogistics$modifyScrollTransferCtrlForFluid(int original, @Local BigItemStack entry) {
        if (entry.stack.getItem() instanceof CompressedTankItem) {
            return 10000;
        }
        return original;
    }

    @ModifyExpressionValue(
        method = "mouseScrolled",
        at = @At(
            value = "CONSTANT",
            args = "intValue=1"
        ),
        remap = false
    )
    private int fluidlogistics$modifyScrollTransferNormalForFluid(int original, @Local BigItemStack entry) {
        if (entry.stack.getItem() instanceof CompressedTankItem) {
            return 1000;
        }
        return original;
    }
}
