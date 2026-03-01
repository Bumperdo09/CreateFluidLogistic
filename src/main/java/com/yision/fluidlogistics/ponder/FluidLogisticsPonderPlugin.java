package com.yision.fluidlogistics.ponder;

import com.simibubi.create.Create;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.registry.AllBlocks;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class FluidLogisticsPonderPlugin implements PonderPlugin {

    public static final ResourceLocation HIGH_LOGISTICS = Create.asResource("high_logistics");

    @Override
    public String getModId() {
        return FluidLogistics.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> registration = helper.withKeyFunction(RegistryEntry::getId);

        registration.forComponents(AllBlocks.FLUID_PACKAGER)
                .addStoryBoard(FluidPackagerScenes.FLUID_PACKAGER, FluidPackagerScenes::fluidPackager)
                .addStoryBoard(FluidPackagerScenes.FLUID_PACKAGER_ADDRESS, FluidPackagerScenes::fluidPackagerAddress);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        PonderTagRegistrationHelper<ItemProviderEntry<?, ?>> registration = helper.withKeyFunction(RegistryEntry::getId);

        registration.addToTag(HIGH_LOGISTICS)
                .add(AllBlocks.FLUID_PACKAGER);
    }
}
