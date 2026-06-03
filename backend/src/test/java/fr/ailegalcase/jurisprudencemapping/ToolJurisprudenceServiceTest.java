package fr.ailegalcase.jurisprudencemapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

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
 *
 * <p>SF-JU-01-FIX (2026-06-03) : le service délègue désormais à
 * {@code findDisplayableByToolAndBranch} (filtre chapeau vide + seuil de
 * confiance appliqué côté requête). Ces tests vérifient la délégation, le
 * passage du seuil/limite et le mapping DTO ; le filtre SQL réel est couvert par
 * {@link ToolJurisprudenceCitationFilterIT}.</p>
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
        when(repository.findDisplayableByToolAndBranch(
                eq("f-dt-30"), eq("anciennete-superieure-10-ans"), any(), any()))
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
        when(repository.findDisplayableByToolAndBranch(
                eq("f-dt-30"), eq("anciennete-superieure-10-ans"), any(), any()))
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
        when(repository.findDisplayableByToolAndBranch(
                eq("f-dt-30"), eq("branche-x"), any(), any()))
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
        when(repository.findDisplayableByToolAndBranch(any(), any(), any(), any()))
                .thenReturn(List.of(mapping));

        List<ToolJurisprudenceCitationResponse> result = service.findByToolAndBranch("f-dt-30", "branche-x");

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isInstanceOf(ToolJurisprudenceCitationResponse.class);
    }

    @Test
    void findByToolAndBranch_returnsEmpty_whenToolIdIsNull() {
        assertThat(service.findByToolAndBranch(null, "branche-x")).isEmpty();
    }

    @Test
    void findByToolAndBranch_returnsEmpty_whenBranchIsNull() {
        assertThat(service.findByToolAndBranch("f-dt-30", null)).isEmpty();
    }

    @Test
    void findByToolAndBranch_returnsEmpty_whenBranchIsBlank() {
        assertThat(service.findByToolAndBranch("f-dt-30", "   ")).isEmpty();
    }

    @Test
    void findByToolAndBranch_passesToolBranchSeuilEtLimiteTop3AuRepository() {
        when(repository.findDisplayableByToolAndBranch(any(), any(), any(), any()))
                .thenReturn(List.of());

        service.findByToolAndBranch("F-DT-30-tool", "branche-haute-anciennete");

        ArgumentCaptor<String> toolIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> branchCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<BigDecimal> minConfCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findDisplayableByToolAndBranch(
                toolIdCaptor.capture(), branchCaptor.capture(), minConfCaptor.capture(), pageableCaptor.capture());

        assertThat(toolIdCaptor.getValue()).isEqualTo("F-DT-30-tool");
        assertThat(branchCaptor.getValue()).isEqualTo("branche-haute-anciennete");
        // SF-JU-01-FIX : seuil d'affichage 0,60 et limite top 3
        assertThat(minConfCaptor.getValue()).isEqualByComparingTo(new BigDecimal("0.60"));
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(3);
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
        UUID citationId = UUID.randomUUID();
        ToolJurisprudenceMapping mapping = buildMapping("Cass. soc. 8 janv. 2025, n° 23-12.345",
                new BigDecimal("0.90"), LocalDate.of(2025, 1, 8));
        mapping.setId(citationId);
        when(repository.findById(citationId)).thenReturn(java.util.Optional.of(mapping));

        service.signalProblem("f-dt-30", citationId, "L'arrêt cité est obsolète à mon avis");

        ArgumentCaptor<JurisprudenceWatchFlag> captor = ArgumentCaptor.forClass(JurisprudenceWatchFlag.class);
        verify(flagRepository).save(captor.capture());
        JurisprudenceWatchFlag saved = captor.getValue();
        assertThat(saved.getSource()).isEqualTo(JurisprudenceWatchFlagSource.USER_SIGNAL);
        assertThat(saved.getStatut()).isEqualTo(JurisprudenceWatchFlagStatut.PENDING);
        assertThat(saved.getToolId()).isEqualTo("f-dt-30");
        assertThat(saved.getCommentUser()).isEqualTo("L'arrêt cité est obsolète à mon avis");
        assertThat(saved.getMappingActuel()).isSameAs(mapping);
    }

    @Test
    void signalProblem_throwsNotFound_whenCitationMissing() {
        UUID citationId = UUID.randomUUID();
        when(repository.findById(citationId)).thenReturn(java.util.Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.signalProblem("f-dt-30", citationId, "x"));
    }

    @Test
    void signalProblem_throwsNotFound_whenCitationArchived() {
        UUID citationId = UUID.randomUUID();
        ToolJurisprudenceMapping mapping = buildMapping("ref", new BigDecimal("0.90"), LocalDate.of(2024, 1, 1));
        mapping.setId(citationId);
        mapping.setArchived(true);
        when(repository.findById(citationId)).thenReturn(java.util.Optional.of(mapping));

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.signalProblem("f-dt-30", citationId, "x"));
    }

    @Test
    void signalProblem_throwsNotFound_whenToolIdMismatches() {
        UUID citationId = UUID.randomUUID();
        ToolJurisprudenceMapping mapping = buildMapping("ref", new BigDecimal("0.90"), LocalDate.of(2024, 1, 1));
        mapping.setId(citationId);
        mapping.setToolId("f-dt-30");
        when(repository.findById(citationId)).thenReturn(java.util.Optional.of(mapping));

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.signalProblem("autre-tool", citationId, null));
    }

    @Test
    void signalProblem_nullComment_savesFlagWithoutComment() {
        UUID citationId = UUID.randomUUID();
        ToolJurisprudenceMapping mapping = buildMapping("ref", new BigDecimal("0.90"), LocalDate.of(2024, 1, 1));
        mapping.setId(citationId);
        when(repository.findById(citationId)).thenReturn(java.util.Optional.of(mapping));

        service.signalProblem("f-dt-30", citationId, null);

        ArgumentCaptor<JurisprudenceWatchFlag> captor = ArgumentCaptor.forClass(JurisprudenceWatchFlag.class);
        verify(flagRepository).save(captor.capture());
        assertThat(captor.getValue().getCommentUser()).isNull();
    }
}
