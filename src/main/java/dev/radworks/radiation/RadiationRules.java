package dev.radworks.radiation;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class RadiationRules {
    public static final String VALIDATION_MODE = "lenient/dev";

    private static final RadiationRules NOT_LOADED = new RadiationRules(
            false,
            "not_loaded",
            List.of(),
            0,
            new RadiationRuleValidationResult());

    private final boolean loaded;
    private final String checksum;
    private final List<RadiationRule> activeRules;
    private final int disabledRules;
    private final RadiationRuleValidationResult validationResult;

    public RadiationRules(
            boolean loaded,
            String checksum,
            List<RadiationRule> activeRules,
            int disabledRules,
            RadiationRuleValidationResult validationResult) {
        this.loaded = loaded;
        this.checksum = checksum;
        this.activeRules = List.copyOf(activeRules);
        this.disabledRules = disabledRules;
        this.validationResult = validationResult;
    }

    public static RadiationRules notLoaded() {
        return NOT_LOADED;
    }

    public boolean loaded() {
        return loaded;
    }

    public String checksum() {
        return checksum;
    }

    public String shortChecksum() {
        if (!loaded || checksum.length() <= 12) {
            return checksum;
        }
        return checksum.substring(0, 12);
    }

    public List<RadiationRule> activeRules() {
        return activeRules;
    }

    public int disabledRules() {
        return disabledRules;
    }

    public RadiationRuleValidationResult validationResult() {
        return validationResult;
    }

    public int itemRules() {
        return count(RadiationRuleType.ITEM);
    }

    public int blockRules() {
        return count(RadiationRuleType.BLOCK);
    }

    public int fluidRules() {
        return count(RadiationRuleType.FLUID);
    }

    public Optional<RadiationRule> itemRule(ResourceLocation id) {
        for (RadiationRule rule : activeRules) {
            if (rule.type() == RadiationRuleType.ITEM && rule.id().equals(id)) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    public Optional<RadiationRule> blockRule(ResourceLocation id) {
        for (RadiationRule rule : activeRules) {
            if (rule.type() == RadiationRuleType.BLOCK && rule.id().equals(id)) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    public List<RadiationRule> activeBlockRules() {
        return activeRules.stream()
                .filter(rule -> rule.type() == RadiationRuleType.BLOCK)
                .toList();
    }

    public List<RadiationRule> activeItemRules() {
        return activeRules.stream()
                .filter(rule -> rule.type() == RadiationRuleType.ITEM)
                .toList();
    }

    public double maxActiveBlockRuleRadius() {
        double maxRadius = 0.0D;
        for (RadiationRule rule : activeRules) {
            if (rule.type() == RadiationRuleType.BLOCK) {
                maxRadius = Math.max(maxRadius, rule.radius());
            }
        }
        return maxRadius;
    }

    public double maxActiveItemRuleRadius() {
        double maxRadius = 0.0D;
        for (RadiationRule rule : activeRules) {
            if (rule.type() == RadiationRuleType.ITEM) {
                maxRadius = Math.max(maxRadius, rule.radius());
            }
        }
        return maxRadius;
    }

    public JsonObject toJson() {
        JsonObject rules = new JsonObject();
        rules.addProperty("loaded", loaded);
        rules.addProperty("checksum", checksum);
        rules.addProperty("validationMode", VALIDATION_MODE);
        rules.addProperty("itemRules", itemRules());
        rules.addProperty("blockRules", blockRules());
        rules.addProperty("fluidRules", fluidRules());
        rules.addProperty("disabledRules", disabledRules);
        rules.add("errors", validationResult.toJson().getAsJsonArray("errors"));
        rules.add("warnings", validationResult.toJson().getAsJsonArray("warnings"));
        rules.add("infos", validationResult.toJson().getAsJsonArray("infos"));
        return rules;
    }

    private int count(RadiationRuleType type) {
        int count = 0;
        for (RadiationRule rule : activeRules) {
            if (rule.type() == type) {
                count++;
            }
        }
        return count;
    }
}
