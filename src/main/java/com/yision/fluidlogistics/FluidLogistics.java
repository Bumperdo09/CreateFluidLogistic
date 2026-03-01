package com.yision.fluidlogistics;

import com.mojang.logging.LogUtils;
import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.yision.fluidlogistics.config.Config;
import com.yision.fluidlogistics.block.FluidPackagerBlockEntity;
import com.yision.fluidlogistics.handler.FluidPackageUnpackingHandler;
import com.yision.fluidlogistics.network.FluidLogisticsPackets;
import com.yision.fluidlogistics.registry.AllBlockEntities;
import com.yision.fluidlogistics.registry.AllBlocks;
import com.yision.fluidlogistics.registry.AllDataComponents;
import com.yision.fluidlogistics.registry.AllItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(FluidLogistics.MODID)
public class FluidLogistics {
    public static final String MODID = "fluidlogistics";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID)
            .defaultCreativeTab("fluidlogistics_tab", b -> b.icon(() -> AllItems.RARE_FLUID_PACKAGE.asStack()))
            .build();

    public FluidLogistics(IEventBus modEventBus, ModContainer modContainer) {
        REGISTRATE.registerEventListeners(modEventBus);

        AllDataComponents.register(modEventBus);
        AllBlocks.register();
        AllBlockEntities.register();
        AllItems.register();

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);

        NeoForge.EVENT_BUS.register(this);

        LOGGER.info("FluidLogistics initialized!");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FluidLogisticsPackets.register();
            registerUnpackingHandlers();
            LOGGER.info("FluidLogistics unpacking handlers registered!");
        });
    }

    private void registerCapabilities(final RegisterCapabilitiesEvent event) {
        FluidPackagerBlockEntity.registerCapabilities(event);
    }

    private void registerUnpackingHandlers() {
        Block fluidTank = com.simibubi.create.AllBlocks.FLUID_TANK.get();
        Block creativeFluidTank = com.simibubi.create.AllBlocks.CREATIVE_FLUID_TANK.get();
        
        if (fluidTank != null) {
            UnpackingHandler.REGISTRY.register(fluidTank, FluidPackageUnpackingHandler.INSTANCE);
        }
        if (creativeFluidTank != null) {
            UnpackingHandler.REGISTRY.register(creativeFluidTank, FluidPackageUnpackingHandler.INSTANCE);
        }
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    @net.neoforged.bus.api.SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("FluidLogistics server starting!");
    }
}
