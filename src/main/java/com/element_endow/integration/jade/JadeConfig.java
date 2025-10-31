package com.element_endow.integration.jade;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class JadeConfig {
    public static final ForgeConfigSpec SPEC;
    public static final Config CONFIG;

    static {
        final Pair<Config, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Config::new);
        SPEC = specPair.getRight();
        CONFIG = specPair.getLeft();
    }

    public static class Config {
        public final ForgeConfigSpec.BooleanValue showElementValues;
        public final ForgeConfigSpec.BooleanValue showCombinations;
        public final ForgeConfigSpec.BooleanValue showMounts;

        public Config(ForgeConfigSpec.Builder builder) {
            builder.comment("Element Endow Jade Integration Settings")
                    .push("jade_integration");

            showElementValues = builder
                    .comment("Show element values in tooltip")
                    .define("showElementValues", true);

            showCombinations = builder
                    .comment("Show active combinations in tooltip")
                    .define("showCombinations", true);

            showMounts = builder
                    .comment("Show mount effects in tooltip")
                    .define("showMounts", true);

            builder.pop();
        }
    }
}