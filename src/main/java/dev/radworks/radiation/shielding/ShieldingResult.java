package dev.radworks.radiation.shielding;

public record ShieldingResult(
        String shielding,
        int shieldingBlocksHit,
        double shieldingMultiplier,
        double shieldingReduction,
        double finalContribution) {
    public static ShieldingResult notApplicable(double rawContribution) {
        return new ShieldingResult("not_applicable", 0, 1.0D, 0.0D, rawContribution);
    }

    public static ShieldingResult clear(double rawContribution) {
        return new ShieldingResult("clear", 0, 1.0D, 0.0D, rawContribution);
    }

    public static ShieldingResult reduced(double rawContribution, int shieldingBlocksHit) {
        double multiplier = Math.max(0.1D, Math.pow(0.5D, shieldingBlocksHit));
        double finalContribution = rawContribution * multiplier;
        return new ShieldingResult(
                "reduced",
                shieldingBlocksHit,
                multiplier,
                rawContribution - finalContribution,
                finalContribution);
    }
}
