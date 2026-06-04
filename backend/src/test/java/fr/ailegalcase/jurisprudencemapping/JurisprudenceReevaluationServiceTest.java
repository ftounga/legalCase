package fr.ailegalcase.jurisprudencemapping;

import fr.ailegalcase.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * F-JU-06 / SF-JU-06-02 — tests unitaires de {@link JurisprudenceReevaluationService}.
 */
class JurisprudenceReevaluationServiceTest {

    private ToolJurisprudenceMappingRepository mappingRepo;
    private JurisprudenceAuditLogRepository auditRepo;
    private JurisprudenceRelevanceGate relevanceGate;
    private PlatformTransactionManager txManager;
    private JurisprudenceReevaluationService service;

    @BeforeEach
    void setUp() {
        mappingRepo = mock(ToolJurisprudenceMappingRepository.class);
        auditRepo = mock(JurisprudenceAuditLogRepository.class);
        relevanceGate = mock(JurisprudenceRelevanceGate.class);
        txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        // 2ᵉ passe : pertinent par défaut (les cas hors-sujet l'overrident par ref)
        when(relevanceGate.assess(any(), any(), any(), any()))
                .thenReturn(new JurisprudenceRelevanceGate.RelevanceVerdict(true, "pertinent"));
        service = new JurisprudenceReevaluationService(mappingRepo, auditRepo, relevanceGate,
                txManager, new SyncTaskExecutor());
    }

    @Test
    void runReevaluation_archivesEmptyLowConfAndOffTopic_keepsValid() {
        ToolJurisprudenceMapping valide = mapping("VALID", "Chapeau pertinent et fiable.", new BigDecimal("0.90"));
        ToolJurisprudenceMapping vide = mapping("EMPTY", "", new BigDecimal("0.90"));
        ToolJurisprudenceMapping lowConf = mapping("LOW", "Chapeau présent.", new BigDecimal("0.55"));
        ToolJurisprudenceMapping horsSujet = mapping("OFFTOPIC", "Restauration ferroviaire — prime ancienneté.", new BigDecimal("0.72"));
        when(mappingRepo.findByArchivedFalse()).thenReturn(List.of(valide, vide, lowConf, horsSujet));
        // la 2ᵉ passe juge OFFTOPIC hors-sujet (les autres : VALID pertinent ; EMPTY/LOW rejetés avant la 2ᵉ passe)
        when(relevanceGate.assess(any(), eq("Cass. soc. OFFTOPIC"), any(), any()))
                .thenReturn(new JurisprudenceRelevanceGate.RelevanceVerdict(false, "convention sectorielle sans rapport"));

        var report = service.runReevaluation(triggerUser());

        assertThat(report.evaluated()).isEqualTo(4);
        assertThat(report.archivedEmptyChapeau()).isEqualTo(1);
        assertThat(report.archivedLowConfidence()).isEqualTo(1);
        assertThat(report.archivedOffTopic()).isEqualTo(1);
        assertThat(report.kept()).isEqualTo(1);

        // 3 mappings archivés → 3 save mapping + 3 audit log AUTO_ARCHIVE
        verify(mappingRepo, times(3)).save(any());
        verify(auditRepo, times(3)).save(any());
        assertThat(valide.isArchived()).isFalse();
        assertThat(vide.isArchived()).isTrue();
        assertThat(lowConf.isArchived()).isTrue();
        assertThat(horsSujet.isArchived()).isTrue();
    }

    @Test
    void runReevaluation_emptyDb_isNoOp() {
        when(mappingRepo.findByArchivedFalse()).thenReturn(List.of());
        var report = service.runReevaluation(triggerUser());
        assertThat(report.evaluated()).isZero();
        verify(mappingRepo, times(0)).save(any());
    }

    @Test
    void startReevaluation_returnsTotalAndRunsAsync() {
        when(mappingRepo.findByArchivedFalse())
                .thenReturn(List.of(mapping("A", "Chapeau ok.", new BigDecimal("0.90"))));
        var started = service.startReevaluation(triggerUser());
        // SyncTaskExecutor → le job a tourné ; total renvoyé = nb actifs au lancement
        assertThat(started.totalAEvaluer()).isEqualTo(1);
    }

    @Test
    void subjectOf_stripsToolCodePrefixAndDashes() {
        ToolJurisprudenceMapping m = mapping("X", "c", new BigDecimal("0.9"));
        m.setToolId("F-DT-09-comparateur-indemnites");
        m.setBrancheCalculId("default");
        assertThat(service.subjectOf(m)).isEqualTo("comparateur indemnites");
    }

    @Test
    void subjectOf_appendsBrancheWhenNotDefault() {
        ToolJurisprudenceMapping m = mapping("X", "c", new BigDecimal("0.9"));
        m.setToolId("F-DT-08-licenciement-validity");
        m.setBrancheCalculId("licenciement-economique");
        assertThat(service.subjectOf(m)).contains("licenciement validity").contains("licenciement economique");
    }

    private ToolJurisprudenceMapping mapping(String suffix, String chapeau, BigDecimal confidence) {
        ToolJurisprudenceMapping m = new ToolJurisprudenceMapping();
        m.setToolId("f-dt-09-comparateur-indemnites");
        m.setBrancheCalculId("default");
        m.setArretRef("Cass. soc. " + suffix);
        m.setJuridiction("Cour de cassation, chambre sociale");
        m.setDateArret(LocalDate.of(2024, 1, 1));
        m.setNumeroPourvoi("23-12.345");
        m.setLienLegifrance("https://www.legifrance.gouv.fr/juri/id/" + suffix);
        m.setChapeauOfficiel(chapeau);
        m.setLastVerifiedAt(Instant.parse("2026-05-01T03:00:00Z"));
        m.setConfidenceScore(confidence);
        m.setArchived(false);
        return m;
    }

    private User triggerUser() {
        User u = new User();
        u.setEmail("admin@legalcase.fr");
        return u;
    }
}
