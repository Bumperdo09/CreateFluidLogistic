package com.yision.fluidlogistics.registry;

import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.BuilderTransformers;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import com.yision.fluidlogistics.block.FluidPackagerBlock;
import com.yision.fluidlogistics.block.FluidPackagerGenerator;
import com.yision.fluidlogistics.block.WaterproofCardboardBlock;

import static com.simibubi.create.foundation.data.TagGen.axeOnly;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;
import static com.yision.fluidlogistics.FluidLogistics.REGISTRATE;

public class AllBlocks {

    public static final BlockEntry<FluidPackagerBlock> FLUID_PACKAGER = REGISTRATE.block("fluid_packager", FluidPackagerBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.noOcclusion())
            .properties(p -> p.isRedstoneConductor(($1, $2, $3) -> false))
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_BLUE)
                    .sound(SoundType.NETHERITE_BLOCK))
            .transform(pickaxeOnly())
            .addLayer(() -> RenderType::cutoutMipped)
            .blockstate(new FluidPackagerGenerator()::generate)
            .item()
            .model(AssetLookup::customItemModel)
            .build()
            .register();

    public static final BlockEntry<WaterproofCardboardBlock> WATERPROOF_CARDBOARD_BLOCK = REGISTRATE.block("waterproof_cardboard_block", WaterproofCardboardBlock::new)
            .initialProperties(() -> Blocks.MUSHROOM_STEM)
            .properties(p -> p.mapColor(MapColor.COLOR_BROWN)
                    .sound(SoundType.CHISELED_BOOKSHELF))
            .transform(axeOnly())
            .blockstate(BlockStateGen.horizontalAxisBlockProvider(false))
            .item()
            .build()
            .lang("Waterproof Block of Cardboard")
            .register();

    public static void register() {
    }
}
