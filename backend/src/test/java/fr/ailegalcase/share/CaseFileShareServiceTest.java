package fr.ailegalcase.share;

import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.AnalysisType;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class CaseFileShareServiceTest {

    @Mock private CaseFileShareRepository shareRepository;
    @Mock private CaseFileRepository caseFileRepository;
    @Mock private CaseAnalysisRepository caseAnalysisRepository;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private OidcUser oidcUser;

    private CaseFileShareService service;

    private static final UUID WS_ID   = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CF_ID   = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SHARE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final String FRONTEND_URL = "https://staging.legalcase.ng-itconsulting.com";

    @BeforeEach
    void setUp() {
        service = new CaseFileShareService(shareRepository, caseFileRepository,
                caseAnalysisRepository, currentUserResolver, workspaceMemberRepository, FRONTEND_URL);
    }

    private void mockUserAndCaseFile() {
        User user = new User();
        Workspace workspace = new Workspace();
        workspace.setId(WS_ID);
        CaseFile caseFile = new CaseFile();
        caseFile.setId(CF_ID);
        caseFile.setTitle("Dossier Test");
        caseFile.setWorkspace(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setUser(user);
        member.setWorkspace(workspace);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(CF_ID)).thenReturn(Optional.of(caseFile));
        lenient().when(shareRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // T-01 : POST → token 64 chars hex, expiresAt = now + expiresInDays
    @Test
    void createShare_validRequest_tokenAndExpiryCorrect() {
        mockUserAndCaseFile();

        ShareResponse response = service.createShare(CF_ID, 7, oidcUser, "GOOGLE", null);

        ArgumentCaptor<CaseFileShare> captor = ArgumentCaptor.forClass(CaseFileShare.class);
        verify(shareRepository).save(captor.capture());

        CaseFileShare saved = captor.getValue();
        assertThat(saved.getToken()).hasSize(64).matches("[0-9a-f]+");
        assertThat(saved.getExpiresAt())
                .isCloseTo(Instant.now().plus(7, ChronoUnit.DAYS), within(5, ChronoUnit.SECONDS));
        assertThat(response.shareUrl()).startsWith(FRONTEND_URL + "/share/");
    }

    // T-02 : Dossier introuvable → 404
    @Test
    void createShare_caseFileNotFound_throws404() {
        User user = new User();
        Workspace workspace = new Workspace();
        workspace.setId(WS_ID);
        WorkspaceMember member = new WorkspaceMember();
        member.setUser(user);
        member.setWorkspace(workspace);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(CF_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createShare(CF_ID, 7, oidcUser, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(NOT_FOUND));
    }

    // T-03 : GET public token valide → PublicShareResponse avec synthèse
    @Test
    void getPublicShare_validToken_returnsSynthesis() {
        CaseFile caseFile = new CaseFile();
        caseFile.setId(CF_ID);
        caseFile.setTitle("Dossier Test");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");

        CaseFileShare share = new CaseFileShare();
        share.setToken("abc123");
        share.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        share.setCaseFile(caseFile);

        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setId(UUID.randomUUID());
        analysis.setVersion(1);
        analysis.setAnalysisType(AnalysisType.STANDARD);
        analysis.setAnalysisStatus(AnalysisStatus.DONE);
        analysis.setUpdatedAt(Instant.now());

        when(shareRepository.findByToken("abc123")).thenReturn(Optional.of(share));
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                CF_ID, AnalysisStatus.DONE)).thenReturn(Optional.of(analysis));

        PublicShareResponse response = service.getPublicShare("abc123");

        assertThat(response.caseFileTitle()).isEqualTo("Dossier Test");
        assertThat(response.legalDomain()).isEqualTo("DROIT_DU_TRAVAIL");
        assertThat(response.synthesis()).isNotNull();
    }

    // T-04 : GET public token expiré → 404
    @Test
    void getPublicShare_expiredToken_throws404() {
        CaseFileShare share = new CaseFileShare();
        share.setToken("expired");
        share.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        share.setCaseFile(new CaseFile());

        when(shareRepository.findByToken("expired")).thenReturn(Optional.of(share));

        assertThatThrownBy(() -> service.getPublicShare("expired"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(NOT_FOUND));
    }

    // T-05 : GET public token révoqué → 404
    @Test
    void getPublicShare_revokedToken_throws404() {
        CaseFileShare share = new CaseFileShare();
        share.setToken("revoked");
        share.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        share.setRevokedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        share.setCaseFile(new CaseFile());

        when(shareRepository.findByToken("revoked")).thenReturn(Optional.of(share));

        assertThatThrownBy(() -> service.getPublicShare("revoked"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(NOT_FOUND));
    }

    // T-06 : DELETE → revoked_at set
    @Test
    void revokeShare_setsRevokedAt() {
        mockUserAndCaseFile();

        CaseFileShare share = new CaseFileShare();
        share.setId(SHARE_ID);
        when(shareRepository.findByIdAndCaseFileId(SHARE_ID, CF_ID)).thenReturn(Optional.of(share));

        service.revokeShare(CF_ID, SHARE_ID, oidcUser, "GOOGLE", null);

        assertThat(share.getRevokedAt()).isNotNull();
        assertThat(share.getRevokedAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
    }

    // T-07 : listActiveShares → délègue au repository avec now
    @Test
    void listActiveShares_returnsRepositoryResult() {
        mockUserAndCaseFile();

        CaseFileShare share = new CaseFileShare();
        share.setId(SHARE_ID);
        share.setToken("tok");
        share.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        when(shareRepository.findActiveByCaseFileId(eq(CF_ID), any(Instant.class)))
                .thenReturn(List.of(share));

        List<ShareResponse> result = service.listActiveShares(CF_ID, oidcUser, "GOOGLE", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).shareUrl()).contains("tok");
    }

    // T-08 : Isolation workspace — dossier appartenant à un autre workspace → 404
    @Test
    void createShare_caseFileBelongsToOtherWorkspace_throws404() {
        User user = new User();
        Workspace myWorkspace = new Workspace();
        myWorkspace.setId(WS_ID);
        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setId(UUID.randomUUID());

        CaseFile otherCaseFile = new CaseFile();
        otherCaseFile.setId(CF_ID);
        otherCaseFile.setWorkspace(otherWorkspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setUser(user);
        member.setWorkspace(myWorkspace);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(CF_ID)).thenReturn(Optional.of(otherCaseFile));

        assertThatThrownBy(() -> service.createShare(CF_ID, 7, oidcUser, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(NOT_FOUND));
    }
}
