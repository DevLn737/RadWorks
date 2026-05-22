package dev.radworks.radiation;

public interface ForceSourceCandidateSink {
    ForceSourceCandidateSink NO_OP = candidate -> {
    };

    void observe(ForceSourceCandidate candidate);

    default boolean isEnabled() {
        return this != NO_OP;
    }
}

