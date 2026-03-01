package com.yision.fluidlogistics.render;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

@OnlyIn(Dist.CLIENT)
public class FluidRenderEntry {
    public int x;
    public int y;
    public FluidStack fluid;
    
    public FluidRenderEntry(int x, int y, FluidStack fluid) {
        this.x = x;
        this.y = y;
        this.fluid = fluid;
    }
}
