package com.yision.fluidlogistics.registry;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.yision.fluidlogistics.block.FluidPackager.FluidPackagerBlockEntity;
import com.yision.fluidlogistics.block.FluidPackager.FluidPackagerRenderer;
import com.yision.fluidlogistics.block.FluidPackager.FluidPackagerVisual;
import com.yision.fluidlogistics.block.HorizontalMultiFluidTank.HorizontalMultiFluidTankBlockEntity;
import com.yision.fluidlogistics.block.HorizontalMultiFluidTank.HorizontalMultiFluidTankRenderer;
import com.yision.fluidlogistics.block.MultiFluidTank.MultiFluidTankBlockEntity;
import com.yision.fluidlogistics.block.MultiFluidTank.MultiFluidTankRenderer;

import static com.yision.fluidlogistics.FluidLogistics.REGISTRATE;

public class AllBlockEntities {

    public static final BlockEntityEntry<FluidPackagerBlockEntity> FLUID_PACKAGER = REGISTRATE
            .blockEntity("fluid_packager", FluidPackagerBlockEntity::new)
            .visual(() -> FluidPackagerVisual::new, true)
            .validBlocks(AllBlocks.FLUID_PACKAGER)
            .renderer(() -> FluidPackagerRenderer::new)
            .register();

    public static final BlockEntityEntry<MultiFluidTankBlockEntity> MULTI_FLUID_TANK = REGISTRATE
            .blockEntity("multi_fluid_tank", MultiFluidTankBlockEntity::new)
            .validBlocks(AllBlocks.MULTI_FLUID_TANK)
            .renderer(() -> MultiFluidTankRenderer::new)
            .register();

    public static final BlockEntityEntry<HorizontalMultiFluidTankBlockEntity> HORIZONTAL_MULTI_FLUID_TANK = REGISTRATE
            .blockEntity("horizontal_multi_fluid_tank", HorizontalMultiFluidTankBlockEntity::new)
            .validBlocks(AllBlocks.HORIZONTAL_MULTI_FLUID_TANK)
            .renderer(() -> HorizontalMultiFluidTankRenderer::new)
            .register();

    public static void register() {
    }
}
