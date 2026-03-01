package com.yision.fluidlogistics.mixin.item;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.yision.fluidlogistics.item.CompressedTankItem;
import com.yision.fluidlogistics.util.FluidAmountHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemStackHandler;

@Mixin(PackageItem.class)
public abstract class PackageItemMixin {

    @Shadow
    public static ItemStackHandler getContents(ItemStack stack) {
        throw new AssertionError();
    }

    @Overwrite(remap = false)
    public void appendHoverText(ItemStack stack, Item.TooltipContext tooltipContext, 
            List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        
        if (stack.has(AllDataComponents.PACKAGE_ADDRESS))
            tooltipComponents.add(Component.literal("\u2192 " + stack.get(AllDataComponents.PACKAGE_ADDRESS))
                .withStyle(ChatFormatting.GOLD));

        if (!stack.has(AllDataComponents.PACKAGE_CONTENTS))
            return;

        int visibleNames = 0;
        int skippedNames = 0;
        ItemStackHandler contents = getContents(stack);
        
        List<FluidStack> mergedFluids = new ArrayList<>();
        List<Integer> mergedAmounts = new ArrayList<>();
        
        for (int i = 0; i < contents.getSlots(); i++) {
            ItemStack itemstack = contents.getStackInSlot(i);
            if (itemstack.isEmpty())
                continue;
            if (itemstack.getItem() instanceof SpawnEggItem)
                continue;
            
            if (itemstack.getItem() instanceof CompressedTankItem) {
                FluidStack fluid = CompressedTankItem.getFluid(itemstack);
                if (!fluid.isEmpty()) {
                    int amount = fluid.getAmount() * itemstack.getCount();
                    boolean merged = false;
                    for (int j = 0; j < mergedFluids.size(); j++) {
                        if (FluidStack.isSameFluidSameComponents(mergedFluids.get(j), fluid)) {
                            mergedAmounts.set(j, mergedAmounts.get(j) + amount);
                            merged = true;
                            break;
                        }
                    }
                    if (!merged) {
                        mergedFluids.add(fluid.copy());
                        mergedAmounts.add(amount);
                    }
                    continue;
                }
            }
            
            if (visibleNames > 2) {
                skippedNames++;
                continue;
            }
            visibleNames++;
            tooltipComponents.add(itemstack.getHoverName()
                .copy()
                .append(" x")
                .append(String.valueOf(itemstack.getCount()))
                .withStyle(ChatFormatting.GRAY));
        }
        
        int addedFluids = 0;
        for (int i = 0; i < mergedFluids.size(); i++) {
            if (addedFluids >= 3 - visibleNames) {
                skippedNames += mergedFluids.size() - i;
                break;
            }
            FluidStack fluid = mergedFluids.get(i);
            int totalAmount = mergedAmounts.get(i);
            String amountStr = FluidAmountHelper.format(totalAmount);
            tooltipComponents.add(Component.literal("")
                    .append(fluid.getHoverName())
                    .append(" " + amountStr)
                    .withStyle(ChatFormatting.GRAY));
            addedFluids++;
        }

        if (skippedNames > 0)
            tooltipComponents.add(Component.translatable("container.shulkerBox.more", skippedNames)
                .withStyle(ChatFormatting.ITALIC));
    }
}
