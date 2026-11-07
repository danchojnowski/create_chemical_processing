package com.createchemicalprocessing.centrifuge;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CentrifugeConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MINIMUM_RPM;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("centrifuge");
        MINIMUM_RPM = builder.comment("Minimum absolute shaft speed in RPM required for the centrifuge to run.")
                .defineInRange("minimumRpm", 128, 1, 256);
        builder.pop();
        SPEC = builder.build();
    }

    private CentrifugeConfig() {}
}
