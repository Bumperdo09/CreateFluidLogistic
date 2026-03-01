package com.yision.fluidlogistics.registry;

import java.util.function.UnaryOperator;

import org.jetbrains.annotations.ApiStatus.Internal;

import com.yision.fluidlogistics.datacomponent.FluidTankContent;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AllDataComponents {

    private static final DeferredRegister.DataComponents DATA_COMPONENTS = 
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, "fluidlogistics");

    public static final DataComponentType<FluidTankContent> FLUID_TANK_CONTENT = register(
            "fluid_tank_content",
            builder -> builder.persistent(FluidTankContent.CODEC)
                    .networkSynchronized(FluidTankContent.STREAM_CODEC)
    );

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        DataComponentType<T> type = builder.apply(DataComponentType.builder()).build();
        DATA_COMPONENTS.register(name, () -> type);
        return type;
    }

    @Internal
    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}
