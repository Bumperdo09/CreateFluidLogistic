package com.yision.fluidlogistics.registry;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.yision.fluidlogistics.block.FluidPackagerBlockEntity;
import com.yision.fluidlogistics.block.FluidPackagerRenderer;
import com.yision.fluidlogistics.block.FluidPackagerVisual;

import static com.yision.fluidlogistics.FluidLogistics.REGISTRATE;

public class AllBlockEntities {

    public static final BlockEntityEntry<FluidPackagerBlockEntity> FLUID_PACKAGER = REGISTRATE
            .blockEntity("fluid_packager", FluidPackagerBlockEntity::new)
            .visual(() -> FluidPackagerVisual::new, true)
            .validBlocks(AllBlocks.FLUID_PACKAGER)
            .renderer(() -> FluidPackagerRenderer::new)
            .register();

    public static void register() {
    }
}
