package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.AnalysisType;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
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
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CaseFileExportServiceTest {

    @Mock private CaseFileRepository caseFileRepository;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private CaseNoteRepository caseNoteRepository;
    @Mock private CaseDeadlineRepository caseDeadlineRepository;
    @Mock private CaseAnalysisRepository caseAnalysisRepository;
    @Mock private OidcUser oidcUser;

    private CaseFileExportService service;

    private UUID workspaceId;
    private UUID caseFileId;
    private CaseFile caseFile;
    private Workspace workspace;
    private User user;

    @BeforeEach
    void setUp() {
        service = new CaseFileExportService(
                caseFileRepository,
                currentUserResolver,
                workspaceMemberRepository,
                documentRepository,
                caseNoteRepository,
                caseDeadlineRepository,
                caseAnalysisRepository,
                new ObjectMapper()
        );

        workspaceId = UUID.randomUUID();
        caseFileId = UUID.randomUUID();

        user = new User();
        user.setEmail("test@example.com");

        workspace = new Workspace();
        workspace.setId(workspaceId);
        workspace.setName("Test Workspace");

        caseFile = new CaseFile();
        caseFile.setId(caseFileId);
        caseFile.setTitle("Dossier Test");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFile.setStatus("OPEN");
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedAt(Instant.now());
        caseFile.setUpdatedAt(Instant.now());

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setPrimary(true);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
    }

    // U-01: ZIP généré contient dossier.json
    @Test
    void export_emptyCaseFile_zipContainsDossierJson() throws Exception {
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(caseFile));
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(caseFile)).thenReturn(List.of());
        when(caseNoteRepository.findByCaseFileIdOrderByCreatedAtDesc(caseFileId)).thenReturn(List.of());
        when(caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(caseFileId)).thenReturn(List.of());
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, AnalysisStatus.DONE)).thenReturn(Optional.empty());

        CaseFileExportResult result = service.export(caseFileId, oidcUser, "google", null);

        assertThat(result.zipBytes()).isNotEmpty();
        assertThat(zipEntryNames(result.zipBytes())).contains("dossier.json");
    }

    // U-02: ZIP sans synthese.json si aucune analyse DONE
    @Test
    void export_noAnalysis_zipDoesNotContainSyntheseJson() throws Exception {
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(caseFile));
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(caseFile)).thenReturn(List.of());
        when(caseNoteRepository.findByCaseFileIdOrderByCreatedAtDesc(caseFileId)).thenReturn(List.of());
        when(caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(caseFileId)).thenReturn(List.of());
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, AnalysisStatus.DONE)).thenReturn(Optional.empty());

        CaseFileExportResult result = service.export(caseFileId, oidcUser, "google", null);

        assertThat(zipEntryNames(result.zipBytes())).doesNotContain("synthese.json");
    }

    // U-03: ZIP contient synthese.json si analyse DONE présente
    @Test
    void export_withAnalysis_zipContainsSyntheseJson() throws Exception {
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setId(UUID.randomUUID());
        analysis.setVersion(1);
        analysis.setAnalysisType(AnalysisType.STANDARD);
        analysis.setAnalysisStatus(AnalysisStatus.DONE);
        analysis.setAnalysisResult("{\"summary\":\"test\"}");
        analysis.setModelUsed("gpt-4");
        analysis.setCreatedAt(Instant.now());
        analysis.setUpdatedAt(Instant.now());

        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(caseFile));
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(caseFile)).thenReturn(List.of());
        when(caseNoteRepository.findByCaseFileIdOrderByCreatedAtDesc(caseFileId)).thenReturn(List.of());
        when(caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(caseFileId)).thenReturn(List.of());
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, AnalysisStatus.DONE)).thenReturn(Optional.of(analysis));

        CaseFileExportResult result = service.export(caseFileId, oidcUser, "google", null);

        assertThat(zipEntryNames(result.zipBytes())).contains("synthese.json");
    }

    // U-04: 404 si dossier inexistant
    @Test
    void export_unknownCaseFile_throws404() {
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.export(caseFileId, oidcUser, "google", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // U-05: 403 si dossier appartient à un autre workspace
    @Test
    void export_differentWorkspace_throws403() {
        UUID otherWorkspaceId = UUID.randomUUID();
        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setId(otherWorkspaceId);
        caseFile.setWorkspace(otherWorkspace);

        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(caseFile));

        assertThatThrownBy(() -> service.export(caseFileId, oidcUser, "google", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    // U-06: sanitize title
    @Test
    void sanitize_specialChars_returnsCleanFilename() {
        assertThat(CaseFileExportService.sanitize("Licenciement Dupont & Fils!")).isEqualTo("licenciement-dupont---fils-");
        assertThat(CaseFileExportService.sanitize("dossier-123_test")).isEqualTo("dossier-123_test");
    }

    private List<String> zipEntryNames(byte[] zipBytes) throws Exception {
        List<String> names = new java.util.ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                names.add(entry.getName());
                zis.closeEntry();
            }
        }
        return names;
    }
}
