package com.yision.fluidlogistics.mixin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class FluidLogisticsMixinPlugin implements IMixinConfigPlugin {

    private static final String BASIN_CAPACITY_EXPANSION_KEY = "basinCapacityExpansionEnabled";
    private static final String JEI_RUNTIME_CLASS = "mezz.jei.api.runtime.IJeiRuntime";
    private static final Pattern BOOLEAN_CONFIG_PATTERN = Pattern.compile("^\\s*([A-Za-z0-9_]+)\\s*=\\s*(true|false)\\s*(?:#.*)?$");
    private static final Set<String> JEI_ONLY_MIXINS = Set.of(
        "com.yision.fluidlogistics.mixin.client.StockKeeperTransferHandlerMixin"
    );
    private static final Set<String> BASIN_CAPACITY_MIXINS = Set.of(
        "com.yision.fluidlogistics.mixin.basin.BasinBlockEntityMixin",
        "com.yision.fluidlogistics.mixin.client.BasinRendererMixin"
    );

    private boolean jeiLoaded;
    private boolean basinCapacityExpansionEnabled;

    @Override
    public void onLoad(String mixinPackage) {
        jeiLoaded = isClassPresent(JEI_RUNTIME_CLASS);
        basinCapacityExpansionEnabled = readBooleanConfig("fluidlogistics-common.toml",
            BASIN_CAPACITY_EXPANSION_KEY, true);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (JEI_ONLY_MIXINS.contains(mixinClassName)) {
            return jeiLoaded;
        }
        if (BASIN_CAPACITY_MIXINS.contains(mixinClassName)) {
            return basinCapacityExpansionEnabled;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, FluidLogisticsMixinPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean readBooleanConfig(String fileName, String key, boolean defaultValue) {
        Path configPath = Path.of("config", fileName);
        if (!Files.isRegularFile(configPath)) {
            return defaultValue;
        }

        try {
            for (String line : Files.readAllLines(configPath)) {
                Matcher matcher = BOOLEAN_CONFIG_PATTERN.matcher(line);
                if (!matcher.matches()) {
                    continue;
                }
                if (key.equals(matcher.group(1))) {
                    return Boolean.parseBoolean(matcher.group(2));
                }
            }
        } catch (IOException ignored) {
            return defaultValue;
        }

        return defaultValue;
    }
}
