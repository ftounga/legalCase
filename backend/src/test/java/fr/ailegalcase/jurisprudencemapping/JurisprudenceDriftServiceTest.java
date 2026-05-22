package fr.ailegalcase.jurisprudencemapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JurisprudenceDriftServiceTest {

    private ToolJurisprudenceMappingRepository mappingRepo;
    private JurisprudenceAuditLogRepository auditRepo;
    private ToolBranchRegistryAggregator aggregator;
    private JurisprudenceDriftService service;

    @BeforeEach
    void setUp() {
        mappingRepo = mock(ToolJurisprudenceMappingRepository.class);
        auditRepo = mock(JurisprudenceAuditLogRepository.class);
        aggregator = mock(ToolBranchRegistryAggregator.class);
        service = new JurisprudenceDriftService(mappingRepo, auditRepo, aggregator);
    }

    @Test
    void runDriftScan_noActiveMappings_isNoOp() {
        when(mappingRepo.findAll()).thenReturn(List.of());

        JurisprudenceDriftRunSummary summary = service.runDriftScan();

        assertThat(summary.mappingsActifsTotal()).isZero();
        assertThat(summary.orphelinsArchives()).isZero();
        assertThat(summary.aborted()).isFalse();
        verify(mappingRepo, never()).save(any());
        verify(auditRepo, never()).save(any());
    }

    @Test
    void runDriftScan_emptyRegistryWithActiveMappings_abortsSafely() {
        ToolJurisprudenceMapping m1 = buildMapping("t1", "b1");
        when(mappingRepo.findAll()).thenReturn(List.of(m1));
        when(aggregator.allKnownBranches()).thenReturn(Set.of());

        JurisprudenceDriftRunSummary summary = service.runDriftScan();

        assertThat(summary.aborted()).isTrue();
        assertThat(summary.abortReason()).contains("ToolBranchRegistry vide");
        assertThat(summary.orphelinsArchives()).isZero();
        verify(mappingRepo, never()).save(any());
        verify(auditRepo, never()).save(any());
        assertThat(m1.isArchived()).isFalse();
    }

    @Test
    void runDriftScan_allBranchesKnown_zeroArchive() {
        ToolJurisprudenceMapping m1 = buildMapping("t1", "b1");
        ToolJurisprudenceMapping m2 = buildMapping("t2", "b2");
        when(mappingRepo.findAll()).thenReturn(List.of(m1, m2));
        when(aggregator.allKnownBranches()).thenReturn(Set.of("t1:b1", "t2:b2"));

        JurisprudenceDriftRunSummary summary = service.runDriftScan();

        assertThat(summary.aborted()).isFalse();
        assertThat(summary.orphelinsArchives()).isZero();
        verify(mappingRepo, never()).save(any());
    }

    @Test
    void runDriftScan_orphanMapping_archivedAndAuditLogged() {
        ToolJurisprudenceMapping kept = buildMapping("t1", "b1");
        ToolJurisprudenceMapping orphan = buildMapping("t2", "branche-supprimee");
        when(mappingRepo.findAll()).thenReturn(List.of(kept, orphan));
        when(aggregator.allKnownBranches()).thenReturn(Set.of("t1:b1"));

        JurisprudenceDriftRunSummary summary = service.runDriftScan();

        assertThat(summary.orphelinsArchives()).isEqualTo(1);
        assertThat(summary.aborted()).isFalse();
        assertThat(orphan.isArchived()).isTrue();
        assertThat(kept.isArchived()).isFalse();
        verify(mappingRepo, times(1)).save(orphan);
        verify(auditRepo, times(1)).save(any(JurisprudenceAuditLog.class));
    }

    @Test
    void runDriftScan_alreadyArchived_isIgnored() {
        ToolJurisprudenceMapping archived = buildMapping("t1", "b1");
        archived.setArchived(true);
        when(mappingRepo.findAll()).thenReturn(List.of(archived));
        // pas besoin de stub aggregator car aborted early sur "0 mappings actifs"

        JurisprudenceDriftRunSummary summary = service.runDriftScan();

        assertThat(summary.mappingsActifsTotal()).isZero();
        verify(mappingRepo, never()).save(any());
    }

    private ToolJurisprudenceMapping buildMapping(String toolId, String brancheCalculId) {
        ToolJurisprudenceMapping m = new ToolJurisprudenceMapping();
        m.setId(UUID.randomUUID());
        m.setToolId(toolId);
        m.setBrancheCalculId(brancheCalculId);
        m.setArretRef("ref");
        m.setJuridiction("Cour de cassation");
        m.setDateArret(LocalDate.of(2024, 1, 1));
        m.setNumeroPourvoi("n");
        m.setLienLegifrance("url");
        m.setChapeauOfficiel("ch");
        m.setLastVerifiedAt(Instant.now());
        m.setConfidenceScore(new BigDecimal("0.90"));
        m.setArchived(false);
        return m;
    }
}
