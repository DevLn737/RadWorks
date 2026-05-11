package dev.radworks.radiation;

import dev.radworks.config.RadWorksConfig;

public final class DynamicRadiusModel {
    public static final String FORMULA_LOG2_SCALED = "base + scale*log2(units), capped";

    private DynamicRadiusModel() {
    }

    public static double effectiveRadius(double baseRadius, double aggregateUnits) {
        if (!RadWorksConfig.dynamicRadiusEnabled()) {
            return clampRadius(baseRadius);
        }

        double units = Math.max(1.0D, aggregateUnits);
        double bonus = RadWorksConfig.dynamicRadiusScale() * log2(units);
        double uncapped = baseRadius + Math.max(0.0D, bonus);
        return clampRadius(uncapped);
    }

    public static double dynamicRadiusBonus(double baseRadius, double effectiveRadius) {
        return Math.max(0.0D, effectiveRadius - baseRadius);
    }

    public static double aggregateUnitsForItems(int aggregateCount) {
        return Math.max(1.0D, aggregateCount);
    }

    public static double aggregateUnitsForFluids(int aggregateAmountMb) {
        return Math.max(1.0D, aggregateAmountMb / 1000.0D);
    }

    public static String radiusFormulaLabel() {
        return RadWorksConfig.dynamicRadiusFormulaLabel();
    }

    public static boolean isActive(double distance, double effectiveRadius) {
        return distance <= effectiveRadius;
    }

    public static String outsideDynamicRadiusReason() {
        return "outside_dynamic_radius";
    }

    private static double clampRadius(double radius) {
        double maxCap = Math.max(0.0D, RadWorksConfig.dynamicRadiusMaxCap());
        return Math.max(0.0D, Math.min(maxCap, radius));
    }

    private static double log2(double value) {
        return Math.log(value) / Math.log(2.0D);
    }
}
