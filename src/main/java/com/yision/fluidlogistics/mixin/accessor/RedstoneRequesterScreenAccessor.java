package com.yision.fluidlogistics.mixin.accessor;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterScreen;

@Mixin(RedstoneRequesterScreen.class)
public interface RedstoneRequesterScreenAccessor {

    @Accessor(value = "amounts", remap = false)
    List<Integer> getAmounts();
}
