package fr.ailegalcase.casefile;

import fr.ailegalcase.analysis.AnalysisJob;
import fr.ailegalcase.analysis.AnalysisJobRepository;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.JobType;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * SF-289-09 — la référence de la « vague de pièces » est le PLUS RÉCENT entre
 * l'analyse initiale (CASE_ANALYSIS) et la synthèse enrichie (ENRICHED_ANALYSIS),
 * de sorte que le compteur « N nouvelles pièces » se vide après une enrichie.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PiecesWaveServiceTest {

    private static final UUID CASE_ID = UUID.randomUUID();

    @Mock private AnalysisJobRepository analysisJobRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private CaseFileRepository caseFileRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser;
    @Mock private Principal principal;

    private PiecesWaveService service;

    @BeforeEach
    void setUp() {
        service = new PiecesWaveService(analysisJobRepository, documentRepository,
                caseFileRepository, workspaceMemberRepository, currentUserResolver);

        Workspace ws = new Workspace();
        ws.setId(UUID.randomUUID());
        CaseFile cf = new CaseFile();
        cf.setId(CASE_ID);
        cf.setWorkspace(ws);
        User user = new User();
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(ws);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(caseFileRepository.findByIdAndDeletedAtIsNull(CASE_ID)).thenReturn(Optional.of(cf));
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        lenient().when(documentRepository.findByCaseFile_IdAndCreatedAtAfterOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of());
    }

    private AnalysisJob doneJob(Instant updatedAt) {
        AnalysisJob j = new AnalysisJob();
        j.setStatus(AnalysisStatus.DONE);
        j.setUpdatedAt(updatedAt);
        return j;
    }

    @Test
    void referenceIsEnrichedWhenMoreRecent_clearsCounterAfterEnriched() {
        Instant caseAt = Instant.parse("2026-06-01T10:00:00Z");
        Instant enrichedAt = Instant.parse("2026-06-05T10:00:00Z");
        when(analysisJobRepository.findByCaseFileIdAndJobType(CASE_ID, JobType.CASE_ANALYSIS))
                .thenReturn(Optional.of(doneJob(caseAt)));
        when(analysisJobRepository.findByCaseFileIdAndJobType(CASE_ID, JobType.ENRICHED_ANALYSIS))
                .thenReturn(Optional.of(doneJob(enrichedAt)));
        // Aucune pièce après l'enrichie (la référence est bien l'enrichie, pas CASE_ANALYSIS).
        when(documentRepository.findByCaseFile_IdAndCreatedAtAfterOrderByCreatedAtDesc(eq(CASE_ID), eq(enrichedAt)))
                .thenReturn(List.of());

        PiecesWaveResponse r = service.wave(CASE_ID, oidcUser, principal);

        assertThat(r.analyzedAt()).isEqualTo(enrichedAt);
        assertThat(r.pendingCount()).isZero();
    }

    @Test
    void referenceIsCaseAnalysisWhenNoEnriched() {
        Instant caseAt = Instant.parse("2026-06-01T10:00:00Z");
        when(analysisJobRepository.findByCaseFileIdAndJobType(CASE_ID, JobType.CASE_ANALYSIS))
                .thenReturn(Optional.of(doneJob(caseAt)));
        when(analysisJobRepository.findByCaseFileIdAndJobType(CASE_ID, JobType.ENRICHED_ANALYSIS))
                .thenReturn(Optional.empty());
        Document d = new Document();
        d.setOriginalFilename("nouvelle.pdf");
        when(documentRepository.findByCaseFile_IdAndCreatedAtAfterOrderByCreatedAtDesc(eq(CASE_ID), eq(caseAt)))
                .thenReturn(List.of(d));

        PiecesWaveResponse r = service.wave(CASE_ID, oidcUser, principal);

        assertThat(r.analyzedAt()).isEqualTo(caseAt);
        assertThat(r.pendingCount()).isEqualTo(1);
    }

    @Test
    void noAnalysisAtAll_returnsNone() {
        when(analysisJobRepository.findByCaseFileIdAndJobType(any(), any())).thenReturn(Optional.empty());

        PiecesWaveResponse r = service.wave(CASE_ID, oidcUser, principal);

        assertThat(r.pendingCount()).isZero();
        assertThat(r.analyzedAt()).isNull();
    }
}
