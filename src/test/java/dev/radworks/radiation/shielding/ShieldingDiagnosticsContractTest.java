package dev.radworks.radiation.shielding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.radworks.radiation.RadiationRuleValidationResult;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ShieldingDiagnosticsContractTest {
    @Test
    void shieldingReportIsStructuredAndNonFatalForOptionalCandidates() {
        ShieldingDiagnostics.Report report = ShieldingDiagnostics.report();
        assertNotNull(report);
        assertEquals(5, report.entries().size(), "expected one dev entry plus four optional entries");

        Set<String> ids = report.entries().stream().map(ShieldingDiagnostics.EntryStatus::id).collect(Collectors.toSet());
        assertTrue(ids.contains("minecraft:iron_block"));
        assertTrue(ids.contains("tfmg:raw_lead_block"));
        assertTrue(ids.contains("tfmg:lead_block"));
        assertTrue(ids.contains("tfmg:lead_ore"));
        assertTrue(ids.contains("createnuclear:reinforced_glass"));

        List<ShieldingDiagnostics.EntryStatus> optional = report.entries().stream()
                .filter(entry -> !entry.required())
                .toList();
        assertEquals(4, optional.size(), "expected four optional external candidates");
        for (ShieldingDiagnostics.EntryStatus entry : optional) {
            assertTrue(Set.of("present", "missing_optional_mod", "not_registered").contains(entry.status()),
                    "unexpected optional status: " + entry.status());
        }

        for (RadiationRuleValidationResult.Issue warning : report.warnings()) {
            assertEquals("UNKNOWN_REGISTRY_ID", warning.category(), "unexpected warning category");
        }

        // Clean-dev contract: optional candidates should surface as INFO when absent, not as fatal diagnostics.
        // This test intentionally asserts structure/category semantics, not specific optional-mod installation state.
        boolean hasOptionalInfo = report.infos().stream().anyMatch(info -> Set.of(
                "MISSING_OPTIONAL_MOD",
                "OPTIONAL_SHIELDING_BLOCK_NOT_REGISTERED",
                "OPTIONAL_SHIELDING_BLOCK_PRESENT").contains(info.category()));
        assertTrue(hasOptionalInfo, "expected optional shielding candidate info diagnostics");
        assertFalse(report.entries().isEmpty(), "shielding candidate list should never be empty");
    }
}
