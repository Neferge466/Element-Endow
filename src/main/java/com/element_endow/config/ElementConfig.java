package com.element_endow.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.Arrays;
import java.util.List;

public class ElementConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ELEMENTS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("elements");

        ELEMENTS = builder
                .comment("Registered element attributes, format: modid:element_name")
                .defineList("element_list",
                        Arrays.asList(

                        ),
                        obj -> obj instanceof String
                );

        builder.pop();
        SPEC = builder.build();
    }
}