package com.yision.fluidlogistics.mixin.kinetics;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.yision.fluidlogistics.block.SmartFaucet.SmartFaucetBlock;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.simibubi.create.content.kinetics.belt.transport.BeltInventory")
public class BeltInventoryMixin {

    @Shadow
    @Final
    private BeltBlockEntity belt;

    @Inject(method = "getBeltProcessingAtSegment", at = @At("RETURN"), cancellable = true)
    private void fluidlogistics$findSmartFaucetOneBlockAbove(int segment,
        CallbackInfoReturnable<BeltProcessingBehaviour> cir) {
        if (cir.getReturnValue() != null) {
            return;
        }

        BlockPos beltPos = BeltHelper.getPositionForOffset(belt, segment);
        BlockPos processorPos = beltPos.above();
        if (!(belt.getLevel().getBlockState(processorPos).getBlock() instanceof SmartFaucetBlock)) {
            return;
        }

        BeltProcessingBehaviour behaviour =
            BlockEntityBehaviour.get(belt.getLevel(), processorPos, BeltProcessingBehaviour.TYPE);
        if (behaviour != null) {
            cir.setReturnValue(behaviour);
        }
    }
}
