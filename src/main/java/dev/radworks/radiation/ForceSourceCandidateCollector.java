package dev.radworks.radiation;

import java.util.ArrayList;
import java.util.List;

public final class ForceSourceCandidateCollector implements ForceSourceCandidateSink {
    private final List<ForceSourceCandidate> candidates = new ArrayList<>();
    private final RadiationTargetKind defaultTargetKind;

    public ForceSourceCandidateCollector(RadiationTargetKind defaultTargetKind) {
        this.defaultTargetKind = defaultTargetKind;
    }

    @Override
    public void observe(ForceSourceCandidate candidate) {
        if (candidate == null) {
            return;
        }
        ForceSourceCandidate resolved = candidate;
        if (candidate.targetKind() == null) {
            resolved = new ForceSourceCandidate(
                    candidate.candidateKind(),
                    candidate.sourceType(),
                    candidate.blockId(),
                    candidate.itemId(),
                    candidate.fluidId(),
                    candidate.position(),
                    candidate.carrierEntityType(),
                    candidate.carrierEntityId(),
                    candidate.containerItemId(),
                    candidate.containerPath(),
                    candidate.carrierBlockId(),
                    defaultTargetKind,
                    candidate.count(),
                    candidate.amountMb(),
                    candidate.distance(),
                    candidate.respectsShieldingHint(),
                    candidate.nested(),
                    candidate.nestedDepth(),
                    candidate.extractionMode(),
                    candidate.candidateReason());
        }
        candidates.add(resolved);
    }

    public List<ForceSourceCandidate> snapshot() {
        return List.copyOf(candidates);
    }
}
