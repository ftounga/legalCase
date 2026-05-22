package fr.ailegalcase.jurisprudencemapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * F-JU-01 / SF-JU-01-01 — tests unitaires de {@link ToolJurisprudenceService}.
 */
class ToolJurisprudenceServiceTest {

    private ToolJurisprudenceMappingRepository repository;
    private JurisprudenceWatchFlagRepository flagRepository;
    private ToolJurisprudenceService service;

    @BeforeEach
    void setUp() {
        repository = mock(ToolJurisprudenceMappingRepository.class);
        flagRepository = mock(JurisprudenceWatchFlagRepository.class);
        service = new ToolJurisprudenceService(repository, flagRepository);
    }

    @Test
    void findByToolAndBranch_returnsEmpty_whenNoMappingExists() {
        when(repository.findTop3ByToolIdAndBrancheCalculIdAndArchivedFalseOrderByConfidenceScoreDescDateArretDesc(
                "f-dt-30", "anciennete-superieure-10-ans"))
                .thenReturn(List.of());

        List<ToolJurisprudenceCitationResponse> result = service.findByToolAndBranch(
                "f-dt-30", "anciennete-superieure-10-ans");

        assertThat(result).isEmpty();
    }

    @Test
    void findByToolAndBranch_returns1Arret_whenSingleMapping() {
        ToolJurisprudenceMapping mapping = buildMapping(
                "Cass. soc. 8 janv. 2025, n° 23-12.345", new BigDecimal("0.92"),
                LocalDate.of(2025, 1, 8));
        when(repository.findTop3ByToolIdAndBrancheCalculIdAndArchivedFalseOrderByConfidenceScoreDescDateArretDesc(
                "f-dt-30", "anciennete-superieure-10-ans"))
                .thenReturn(List.of(mapping));

        List<ToolJurisprudenceCitationResponse> result = service.findByToolAndBranch(
                "f-dt-30", "anciennete-superieure-10-ans");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).arretRef()).isEqualTo("Cass. soc. 8 janv. 2025, n° 23-12.345");
        assertThat(result.get(0).confidenceScore()).isEqualByComparingTo(new BigDecimal("0.92"));
    }

    @Test
    void findByToolAndBranch_returns3ArretsSortedByConfidence_whenMultipleMappings() {
        ToolJurisprudenceMapping high = buildMapping("Arret 1", new BigDecimal("0.95"), LocalDate.of(2025, 3, 1));
        ToolJurisprudenceMapping mid = buildMapping("Arret 2", new BigDecimal("0.85"), LocalDate.of(2024, 6, 1));
        ToolJurisprudenceMapping low = buildMapping("Arret 3", new BigDecimal("0.75"), LocalDate.of(2023, 1, 1));
        when(repository.findTop3ByToolIdAndBrancheCalculIdAndArchivedFalseOrderByConfidenceScoreDescDateArretDesc(
                "f-dt-30", "branche-x"))
                .thenReturn(List.of(high, mid, low));

        List<ToolJurisprudenceCitationResponse> result = service.findByToolAndBranch(
                "f-dt-30", "branche-x");

        assertThat(result).hasSize(3);
        assertThat(result).extracting(ToolJurisprudenceCitationResponse::arretRef)
                .containsExactly("Arret 1", "Arret 2", "Arret 3");
    }

    @Test
    void findByToolAndBranch_returnsImmutableDtos_notEntities() {
        ToolJurisprudenceMapping mapping = buildMapping("Arret", new BigDecimal("0.80"), LocalDate.of(2024, 1, 1));
        when(repository.findTop3ByToolIdAndBrancheCalculIdAndArchivedFalseOrderByConfidenceScoreDescDateArretDesc(
                any(), any()))
                .thenReturn(List.of(mapping));

        List<ToolJurisprudenceCitationResponse> result = service.findByToolAndBranch("f-dt-30", "branche-x");

        // record Java = immutable par construction, vérifie que le service mappe bien
        // l'entité vers le DTO (pas l'entité retournée telle quelle)
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isInstanceOf(ToolJurisprudenceCitationResponse.class);
    }

    @Test
    void findByToolAndBranch_returnsEmpty_whenToolIdIsNull() {
        List<ToolJurisprudenceCitationResponse> result = service.findByToolAndBranch(null, "branche-x");

        assertThat(result).isEmpty();
    }

    @Test
    void findByToolAndBranch_returnsEmpty_whenBranchIsNull() {
        List<ToolJurisprudenceCitationResponse> result = service.findByToolAndBranch("f-dt-30", null);

        assertThat(result).isEmpty();
    }

    @Test
    void findByToolAndBranch_returnsEmpty_whenBranchIsBlank() {
        List<ToolJurisprudenceCitationResponse> result = service.findByToolAndBranch("f-dt-30", "   ");

        assertThat(result).isEmpty();
    }

    @Test
    void findByToolAndBranch_passesToolIdAndBranchToRepository() {
        when(repository.findTop3ByToolIdAndBrancheCalculIdAndArchivedFalseOrderByConfidenceScoreDescDateArretDesc(
                any(), any()))
                .thenReturn(List.of());

        service.findByToolAndBranch("F-DT-30-tool", "branche-haute-anciennete");

        ArgumentCaptor<String> toolIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> branchCaptor = ArgumentCaptor.forClass(String.class);
        verify(repository).findTop3ByToolIdAndBrancheCalculIdAndArchivedFalseOrderByConfidenceScoreDescDateArretDesc(
                toolIdCaptor.capture(), branchCaptor.capture());
        assertThat(toolIdCaptor.getValue()).isEqualTo("F-DT-30-tool");
        assertThat(branchCaptor.getValue()).isEqualTo("branche-haute-anciennete");
    }

    private ToolJurisprudenceMapping buildMapping(String arretRef, BigDecimal confidenceScore, LocalDate dateArret) {
        ToolJurisprudenceMapping mapping = new ToolJurisprudenceMapping();
        mapping.setId(UUID.randomUUID());
        mapping.setToolId("f-dt-30");
        mapping.setBrancheCalculId("anciennete-superieure-10-ans");
        mapping.setArretRef(arretRef);
        mapping.setJuridiction("Cour de cassation, chambre sociale");
        mapping.setDateArret(dateArret);
        mapping.setNumeroPourvoi("23-12.345");
        mapping.setLienLegifrance("https://www.legifrance.gouv.fr/juri/id/JURITEXT000049XXXXXX");
        mapping.setChapeauOfficiel("Selon l'article L. 1235-3 du code du travail, ...");
        mapping.setLastVerifiedAt(Instant.parse("2026-05-01T03:00:00Z"));
        mapping.setConfidenceScore(confidenceScore);
        mapping.setArchived(false);
        return mapping;
    }

    // ─── SF-JU-01-04 — signalProblem ───────────────────────────────────────

    @Test
    void signalProblem_createsFlagWithUserSignalSource() {
        java.util.UUID citationId = java.util.UUID.randomUUID();
        ToolJurisprudenceMapping mapping = buildMapping("Cass. soc. 8 janv. 2025, n° 23-12.345",
                new java.math.BigDecimal("0.90"), java.time.LocalDate.of(2025, 1, 8));
        mapping.setId(citationId);
        org.mockito.Mockito.when(repository.findById(citationId)).thenReturn(java.util.Optional.of(mapping));

        service.signalProblem("f-dt-30", citationId, "L'arrêt cité est obsolète à mon avis");

        org.mockito.ArgumentCaptor<JurisprudenceWatchFlag> captor =
                org.mockito.ArgumentCaptor.forClass(JurisprudenceWatchFlag.class);
        org.mockito.Mockito.verify(flagRepository).save(captor.capture());
        JurisprudenceWatchFlag saved = captor.getValue();
        assertThat(saved.getSource()).isEqualTo(JurisprudenceWatchFlagSource.USER_SIGNAL);
        assertThat(saved.getStatut()).isEqualTo(JurisprudenceWatchFlagStatut.PENDING);
        assertThat(saved.getToolId()).isEqualTo("f-dt-30");
        assertThat(saved.getCommentUser()).isEqualTo("L'arrêt cité est obsolète à mon avis");
        assertThat(saved.getMappingActuel()).isSameAs(mapping);
    }

    @Test
    void signalProblem_throwsNotFound_whenCitationMissing() {
        java.util.UUID citationId = java.util.UUID.randomUUID();
        org.mockito.Mockito.when(repository.findById(citationId)).thenReturn(java.util.Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.signalProblem("f-dt-30", citationId, "x"));
    }

    @Test
    void signalProblem_throwsNotFound_whenCitationArchived() {
        java.util.UUID citationId = java.util.UUID.randomUUID();
        ToolJurisprudenceMapping mapping = buildMapping("ref", new java.math.BigDecimal("0.90"),
                java.time.LocalDate.of(2024, 1, 1));
        mapping.setId(citationId);
        mapping.setArchived(true);
        org.mockito.Mockito.when(repository.findById(citationId)).thenReturn(java.util.Optional.of(mapping));

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.signalProblem("f-dt-30", citationId, "x"));
    }

    @Test
    void signalProblem_throwsNotFound_whenToolIdMismatches() {
        java.util.UUID citationId = java.util.UUID.randomUUID();
        ToolJurisprudenceMapping mapping = buildMapping("ref", new java.math.BigDecimal("0.90"),
                java.time.LocalDate.of(2024, 1, 1));
        mapping.setId(citationId);
        mapping.setToolId("f-dt-30");
        org.mockito.Mockito.when(repository.findById(citationId)).thenReturn(java.util.Optional.of(mapping));

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.signalProblem("autre-tool", citationId, null));
    }

    @Test
    void signalProblem_nullComment_savesFlagWithoutComment() {
        java.util.UUID citationId = java.util.UUID.randomUUID();
        ToolJurisprudenceMapping mapping = buildMapping("ref", new java.math.BigDecimal("0.90"),
                java.time.LocalDate.of(2024, 1, 1));
        mapping.setId(citationId);
        org.mockito.Mockito.when(repository.findById(citationId)).thenReturn(java.util.Optional.of(mapping));

        service.signalProblem("f-dt-30", citationId, null);

        org.mockito.ArgumentCaptor<JurisprudenceWatchFlag> captor =
                org.mockito.ArgumentCaptor.forClass(JurisprudenceWatchFlag.class);
        org.mockito.Mockito.verify(flagRepository).save(captor.capture());
        assertThat(captor.getValue().getCommentUser()).isNull();
    }
}
