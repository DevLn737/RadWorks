package dev.radworks.diagnostics;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import dev.radworks.config.RadWorksConfig;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public final class NestedContainerDiagnostics {
    private static volatile Snapshot lastSnapshot;

    private NestedContainerDiagnostics() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static synchronized void store(Builder builder) {
        lastSnapshot = builder.build();
    }

    public static synchronized com.google.gson.JsonElement lastToJson() {
        if (lastSnapshot == null) {
            return JsonNull.INSTANCE;
        }
        return lastSnapshot.toJson();
    }

    public static final class Builder {
        private final List<Sample> samples = new ArrayList<>();
        private int nestedContainersChecked;
        private int nestedContainersSupported;
        private int nestedContainersUnsupported;
        private int nestedStacksExtracted;
        private int nestedRadioactiveMatches;
        private int nestedDepthLimitHits;
        private int nestedItemLimitHits;
        private int nestedMalformedContainers;

        public void nestedContainerChecked() {
            nestedContainersChecked++;
        }

        public void nestedContainerSupported() {
            nestedContainersSupported++;
        }

        public void nestedContainerUnsupported() {
            nestedContainersUnsupported++;
        }

        public void nestedStackExtracted() {
            nestedStacksExtracted++;
        }

        public void nestedRadioactiveMatch() {
            nestedRadioactiveMatches++;
        }

        public void nestedDepthLimitHit() {
            nestedDepthLimitHits++;
        }

        public void nestedItemLimitHit() {
            nestedItemLimitHits++;
        }

        public void nestedMalformedContainer() {
            nestedMalformedContainers++;
        }

        public void sample(
                String sourcePath,
                ResourceLocation containerItemId,
                String containerPath,
                Integer nestedDepth,
                String extractionMode,
                String skippedReason,
                ResourceLocation extractedItemId,
                Integer extractedCount) {
            int cap = RadWorksConfig.nestedContainerDiagnosticSampleCap();
            if (samples.size() >= cap) {
                return;
            }
            samples.add(new Sample(
                    sourcePath,
                    containerItemId,
                    containerPath,
                    nestedDepth,
                    extractionMode,
                    skippedReason,
                    extractedItemId,
                    extractedCount));
        }

        private Snapshot build() {
            return new Snapshot(
                    Instant.now(),
                    nestedContainersChecked,
                    nestedContainersSupported,
                    nestedContainersUnsupported,
                    nestedStacksExtracted,
                    nestedRadioactiveMatches,
                    nestedDepthLimitHits,
                    nestedItemLimitHits,
                    nestedMalformedContainers,
                    List.copyOf(samples));
        }
    }

    private record Sample(
            String sourcePath,
            ResourceLocation containerItemId,
            String containerPath,
            Integer nestedDepth,
            String extractionMode,
            String skippedReason,
            ResourceLocation extractedItemId,
            Integer extractedCount) {
        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("sourcePath", sourcePath);
            if (containerItemId != null) {
                json.addProperty("containerItemId", containerItemId.toString());
            }
            if (containerPath != null) {
                json.addProperty("containerPath", containerPath);
            }
            if (nestedDepth != null) {
                json.addProperty("nestedDepth", nestedDepth);
            }
            if (extractionMode != null) {
                json.addProperty("extractionMode", extractionMode);
            }
            if (skippedReason != null) {
                json.addProperty("skippedReason", skippedReason);
            }
            if (extractedItemId != null) {
                json.addProperty("extractedItemId", extractedItemId.toString());
            }
            if (extractedCount != null) {
                json.addProperty("extractedCount", extractedCount);
            }
            return json;
        }
    }

    private record Snapshot(
            Instant createdAt,
            int nestedContainersChecked,
            int nestedContainersSupported,
            int nestedContainersUnsupported,
            int nestedStacksExtracted,
            int nestedRadioactiveMatches,
            int nestedDepthLimitHits,
            int nestedItemLimitHits,
            int nestedMalformedContainers,
            List<Sample> samples) {
        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("createdAt", createdAt.toString());
            json.addProperty("nestedContainersChecked", nestedContainersChecked);
            json.addProperty("nestedContainersSupported", nestedContainersSupported);
            json.addProperty("nestedContainersUnsupported", nestedContainersUnsupported);
            json.addProperty("nestedStacksExtracted", nestedStacksExtracted);
            json.addProperty("nestedRadioactiveMatches", nestedRadioactiveMatches);
            json.addProperty("nestedDepthLimitHits", nestedDepthLimitHits);
            json.addProperty("nestedItemLimitHits", nestedItemLimitHits);
            json.addProperty("nestedMalformedContainers", nestedMalformedContainers);
            JsonArray samplesArray = new JsonArray();
            for (Sample sample : samples) {
                samplesArray.add(sample.toJson());
            }
            json.add("samples", samplesArray);
            return json;
        }
    }
}
