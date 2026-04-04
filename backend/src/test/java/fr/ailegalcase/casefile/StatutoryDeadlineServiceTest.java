package fr.ailegalcase.casefile;

import fr.ailegalcase.analysis.CaseAnalysis;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StatutoryDeadlineServiceTest {

    private final CaseDeadlineRepository repo = mock(CaseDeadlineRepository.class);
    private final StatutoryDeadlineService service = new StatutoryDeadlineService(repo);

    private CaseAnalysis makeAnalysis(CaseFile caseFile) {
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setId(UUID.randomUUID());
        analysis.setCaseFile(caseFile);
        return analysis;
    }

    private CaseFile makeCaseFile(Instant createdAt) {
        CaseFile cf = new CaseFile();
        cf.setId(UUID.randomUUID());
        cf.setCreatedAt(createdAt);
        return cf;
    }

    private CaseFile makeCaseFile(Instant createdAt, String legalDomain) {
        CaseFile cf = makeCaseFile(createdAt);
        cf.setLegalDomain(legalDomain);
        return cf;
    }

    @Test
    void knownType_withReferenceDate_persistsCorrectDeadline() {
        CaseFile caseFile = makeCaseFile(Instant.parse("2020-01-01T00:00:00Z"));
        CaseAnalysis analysis = makeAnalysis(caseFile);
        when(repo.findByCaseFileIdOrderByDueDateAsc(caseFile.getId())).thenReturn(List.of());

        String json = """
                {"type_litige_detecte": "LICENCIEMENT_SANS_CAUSE_REELLE",
                 "date_reference_prescription": "2025-11-15"}
                """;
        service.createStatutoryDeadlines(analysis, json);

        ArgumentCaptor<CaseDeadline> captor = ArgumentCaptor.forClass(CaseDeadline.class);
        verify(repo).save(captor.capture());
        CaseDeadline saved = captor.getValue();
        assertThat(saved.getSource()).isEqualTo("STATUTORY");
        assertThat(saved.getDueDate()).isEqualTo(LocalDate.of(2026, 11, 15)); // +1 year
        assertThat(saved.getLabel()).contains("Licenciement sans cause réelle");
        assertThat(saved.getLabel()).contains("Art. L1471-1");
    }

    @Test
    void knownType_missingReferenceDate_fallsBackToCaseFileCreatedAt() {
        Instant createdAt = Instant.parse("2024-03-10T10:00:00Z");
        CaseFile caseFile = makeCaseFile(createdAt);
        CaseAnalysis analysis = makeAnalysis(caseFile);
        when(repo.findByCaseFileIdOrderByDueDateAsc(caseFile.getId())).thenReturn(List.of());

        String json = """
                {"type_litige_detecte": "HEURES_SUPPLEMENTAIRES"}
                """;
        service.createStatutoryDeadlines(analysis, json);

        ArgumentCaptor<CaseDeadline> captor = ArgumentCaptor.forClass(CaseDeadline.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getDueDate()).isEqualTo(LocalDate.of(2027, 3, 10)); // +3 years
    }

    @Test
    void unknownType_noDeadlineCreated() {
        CaseFile caseFile = makeCaseFile(Instant.now());
        CaseAnalysis analysis = makeAnalysis(caseFile);

        String json = """
                {"type_litige_detecte": "RUPTURE_CONVENTIONNELLE",
                 "date_reference_prescription": "2025-01-01"}
                """;
        service.createStatutoryDeadlines(analysis, json);

        verify(repo, never()).save(any());
    }

    @Test
    void nullTypeField_noDeadlineCreated() {
        CaseFile caseFile = makeCaseFile(Instant.now());
        CaseAnalysis analysis = makeAnalysis(caseFile);

        String json = """
                {"type_litige_detecte": null}
                """;
        service.createStatutoryDeadlines(analysis, json);

        verify(repo, never()).save(any());
    }

    @Test
    void malformedJson_failOpen_noException() {
        CaseFile caseFile = makeCaseFile(Instant.now());
        CaseAnalysis analysis = makeAnalysis(caseFile);

        // Should not throw
        service.createStatutoryDeadlines(analysis, "not valid json {{{");

        verify(repo, never()).save(any());
    }

    // IM-01 : date_expiration_titre présente → deadline créée avec alertThresholds="90,30,7"
    @Test
    void immigration_withExpirationDate_persistsDeadlineWithAlertThresholds() {
        CaseFile caseFile = makeCaseFile(Instant.parse("2024-01-01T00:00:00Z"), "DROIT_IMMIGRATION");
        CaseAnalysis analysis = makeAnalysis(caseFile);
        when(repo.findByCaseFileIdOrderByDueDateAsc(caseFile.getId())).thenReturn(List.of());

        String json = """
                {"date_expiration_titre": "2026-06-30", "type_titre_sejour": "CARTE_RESIDENT"}
                """;
        service.createStatutoryDeadlines(analysis, json);

        ArgumentCaptor<CaseDeadline> captor = ArgumentCaptor.forClass(CaseDeadline.class);
        verify(repo).save(captor.capture());
        CaseDeadline saved = captor.getValue();
        assertThat(saved.getSource()).isEqualTo("STATUTORY");
        assertThat(saved.getDueDate()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(saved.getLabel()).isEqualTo("Expiration titre de séjour");
        assertThat(saved.getAlertThresholds()).isEqualTo("90,30,7");
        assertThat(saved.getDocumentType()).isEqualTo("CARTE_RESIDENT");
    }

    // IM-02 : date_expiration_titre absente → aucune deadline créée
    @Test
    void immigration_missingExpirationDate_noDeadlineCreated() {
        CaseFile caseFile = makeCaseFile(Instant.now(), "DROIT_IMMIGRATION");
        CaseAnalysis analysis = makeAnalysis(caseFile);

        String json = """
                {"type_titre_sejour": "TITRE_SEJOUR_TEMPORAIRE"}
                """;
        service.createStatutoryDeadlines(analysis, json);

        verify(repo, never()).save(any());
    }

    // PROC-01 : type procédure connu + date dépôt valide → jalons créés avec dates calculées
    @Test
    void immigration_procedureConnue_creesJalons() {
        CaseFile caseFile = makeCaseFile(Instant.parse("2024-01-01T00:00:00Z"), "DROIT_IMMIGRATION");
        CaseAnalysis analysis = makeAnalysis(caseFile);
        when(repo.findByCaseFileIdOrderByDueDateAsc(caseFile.getId())).thenReturn(List.of());

        String json = """
                {"type_procedure_detectee": "DEMANDE_ASILE_OFPRA", "date_depot_procedure": "2026-01-01"}
                """;
        service.createStatutoryDeadlines(analysis, json);

        ArgumentCaptor<CaseDeadline> captor = ArgumentCaptor.forClass(CaseDeadline.class);
        verify(repo, times(2)).save(captor.capture());
        List<CaseDeadline> saved = captor.getAllValues();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getDueDate()).isEqualTo(java.time.LocalDate.of(2026, 1, 22)); // +21 jours
        assertThat(saved.get(1).getDueDate()).isEqualTo(java.time.LocalDate.of(2026, 6, 30));  // +180 jours
        assertThat(saved.get(0).getDocumentType()).isEqualTo("DEMANDE_ASILE_OFPRA");
        assertThat(saved.get(0).getSource()).isEqualTo("STATUTORY");
    }

    // PROC-02 : type procédure inconnu → aucune deadline créée
    @Test
    void immigration_procedureInconnue_noDeadline() {
        CaseFile caseFile = makeCaseFile(Instant.now(), "DROIT_IMMIGRATION");
        CaseAnalysis analysis = makeAnalysis(caseFile);

        String json = """
                {"type_procedure_detectee": "NATURALISATION", "date_depot_procedure": "2026-01-01"}
                """;
        service.createStatutoryDeadlines(analysis, json);

        verify(repo, never()).save(any());
    }

    // PROC-03 : date_depot absente → aucune deadline créée
    @Test
    void immigration_dateDepotAbsente_noDeadline() {
        CaseFile caseFile = makeCaseFile(Instant.now(), "DROIT_IMMIGRATION");
        CaseAnalysis analysis = makeAnalysis(caseFile);

        String json = """
                {"type_procedure_detectee": "RECOURS_CNDA"}
                """;
        service.createStatutoryDeadlines(analysis, json);

        verify(repo, never()).save(any());
    }

    // PROC-04 : date_depot invalide → aucune deadline, pas d'exception
    @Test
    void immigration_dateDepotInvalide_failOpen() {
        CaseFile caseFile = makeCaseFile(Instant.now(), "DROIT_IMMIGRATION");
        CaseAnalysis analysis = makeAnalysis(caseFile);

        String json = """
                {"type_procedure_detectee": "RECOURS_CNDA", "date_depot_procedure": "not-a-date"}
                """;
        service.createStatutoryDeadlines(analysis, json);

        verify(repo, never()).save(any());
    }

    // IM-03 : type_titre_sejour absent → documentType par défaut = "TITRE_SEJOUR"
    @Test
    void immigration_missingTypeTitreSejour_defaultsDocumentType() {
        CaseFile caseFile = makeCaseFile(Instant.parse("2024-01-01T00:00:00Z"), "DROIT_IMMIGRATION");
        CaseAnalysis analysis = makeAnalysis(caseFile);
        when(repo.findByCaseFileIdOrderByDueDateAsc(caseFile.getId())).thenReturn(List.of());

        String json = """
                {"date_expiration_titre": "2027-03-15"}
                """;
        service.createStatutoryDeadlines(analysis, json);

        ArgumentCaptor<CaseDeadline> captor = ArgumentCaptor.forClass(CaseDeadline.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getDocumentType()).isEqualTo("TITRE_SEJOUR");
    }

    @Test
    void existingStatutoryDeadline_isDeletedBeforeInsert() {
        CaseFile caseFile = makeCaseFile(Instant.now());
        CaseAnalysis analysis = makeAnalysis(caseFile);

        CaseDeadline existing = new CaseDeadline();
        existing.setSource("STATUTORY");
        existing.setLabel("Prescription — Harcèlement moral (Art. L1152-1)");

        when(repo.findByCaseFileIdOrderByDueDateAsc(caseFile.getId())).thenReturn(List.of(existing));

        String json = """
                {"type_litige_detecte": "HARCELEMENT_MORAL",
                 "date_reference_prescription": "2020-06-01"}
                """;
        service.createStatutoryDeadlines(analysis, json);

        verify(repo).delete(existing);
        verify(repo).save(any(CaseDeadline.class));
    }
}
