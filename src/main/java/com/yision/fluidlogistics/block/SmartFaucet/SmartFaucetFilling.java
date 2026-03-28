package com.yision.fluidlogistics.block.SmartFaucet;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

public final class SmartFaucetFilling {

    private SmartFaucetFilling() {
    }

    public static boolean canItemBeFilled(Level level, ItemStack stack) {
        SingleRecipeInput input = new SingleRecipeInput(stack);
        Optional<RecipeHolder<FillingRecipe>> assemblyRecipe =
            SequencedAssemblyRecipe.getRecipe(level, input, AllRecipeTypes.FILLING.getType(), FillingRecipe.class);
        if (assemblyRecipe.isPresent()) {
            return true;
        }

        if (AllRecipeTypes.FILLING.find(input, level).isPresent()) {
            return true;
        }

        return GenericItemFilling.canItemBeFilled(level, stack);
    }

    public static int getRequiredAmountForItem(Level level, ItemStack stack, FluidStack availableFluid) {
        SingleRecipeInput input = new SingleRecipeInput(stack);
        Optional<RecipeHolder<FillingRecipe>> assemblyRecipe = SequencedAssemblyRecipe.getRecipe(level, input,
            AllRecipeTypes.FILLING.getType(), FillingRecipe.class, matchItemAndFluid(level, availableFluid, input));
        if (assemblyRecipe.isPresent()) {
            SizedFluidIngredient requiredFluid = assemblyRecipe.get().value().getRequiredFluid();
            if (requiredFluid.test(availableFluid)) {
                return requiredFluid.amount();
            }
        }

        for (RecipeHolder<Recipe<SingleRecipeInput>> recipe : level.getRecipeManager()
            .getRecipesFor(AllRecipeTypes.FILLING.getType(), input, level)) {
            FillingRecipe fillingRecipe = (FillingRecipe) recipe.value();
            SizedFluidIngredient requiredFluid = fillingRecipe.getRequiredFluid();
            if (requiredFluid.test(availableFluid)) {
                return requiredFluid.amount();
            }
        }

        return GenericItemFilling.getRequiredAmountForItem(level, stack, availableFluid);
    }

    public static ItemStack fillItem(Level level, int requiredAmount, ItemStack stack, FluidStack availableFluid) {
        FluidStack toFill = availableFluid.copyWithAmount(requiredAmount);
        SingleRecipeInput input = new SingleRecipeInput(stack);

        RecipeHolder<FillingRecipe> fillingRecipe = SequencedAssemblyRecipe.getRecipe(level, input,
            AllRecipeTypes.FILLING.getType(), FillingRecipe.class, matchItemAndFluid(level, availableFluid, input))
            .filter(recipe -> recipe.value().getRequiredFluid().test(toFill))
            .orElseGet(() -> {
                for (RecipeHolder<Recipe<SingleRecipeInput>> recipe : level.getRecipeManager()
                    .getRecipesFor(AllRecipeTypes.FILLING.getType(), input, level)) {
                    FillingRecipe candidate = (FillingRecipe) recipe.value();
                    if (candidate.getRequiredFluid().test(toFill)) {
                        return new RecipeHolder<>(recipe.id(), candidate);
                    }
                }
                return null;
            });

        if (fillingRecipe != null) {
            List<ItemStack> results = fillingRecipe.value().rollResults(level.random);
            availableFluid.shrink(requiredAmount);
            stack.shrink(1);
            return results.isEmpty() ? ItemStack.EMPTY : results.get(0);
        }

        return GenericItemFilling.fillItem(level, requiredAmount, stack, availableFluid);
    }

    private static Predicate<RecipeHolder<FillingRecipe>> matchItemAndFluid(Level level, FluidStack availableFluid,
        SingleRecipeInput input) {
        return recipe -> recipe.value().matches(input, level) && recipe.value().getRequiredFluid().test(availableFluid);
    }
}
