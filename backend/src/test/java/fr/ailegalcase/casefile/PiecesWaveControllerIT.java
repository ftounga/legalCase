package fr.ailegalcase.casefile;

import fr.ailegalcase.analysis.AnalysisJob;
import fr.ailegalcase.analysis.AnalysisJobRepository;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.JobType;
import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.workspace.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F-283 / SF-283-02 — tests d'intégration de la vague de pièces (delta de pièces
 * depuis la dernière analyse + isolation workspace).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class PiecesWaveControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private AnalysisJobRepository analysisJobRepository;

    private OAuth2AuthenticationToken auth;
    private CaseFile caseFile;
    private Workspace workspace;
    private User user;

    @BeforeEach
    void setUp() {
        analysisJobRepository.deleteAll();
        documentRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setEmail("wave-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-wave-sub");
        authAccountRepository.save(account);

        workspace = new Workspace();
        workspace.setName("WAVE-TEST");
        workspace.setSlug("wave-slug-" + System.currentTimeMillis());
        workspace.setOwner(user);
        workspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        caseFile = new CaseFile();
        caseFile.setTitle("Dossier vague");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFile.setStatus("OPEN");
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFileRepository.save(caseFile);

        auth = buildGoogleAuth("google-wave-sub", "wave-test@example.com");
    }

    // I-01 : pas d'analyse réussie → pas de vague (count 0, analyzedAt null)
    @Test
    void wave_noAnalysis_returnsEmpty() throws Exception {
        saveDocument("piece-1.pdf");

        mockMvc.perform(get("/api/v1/case-files/{id}/pieces-wave", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analyzedAt").doesNotExist())
                .andExpect(jsonPath("$.pendingCount").value(0))
                .andExpect(jsonPath("$.pendingPieces.length()").value(0));
    }

    // I-02 : pièces ajoutées après l'analyse → vague de N pièces, plus récentes d'abord
    @Test
    void wave_piecesAfterAnalysis_returnsPendingWave() throws Exception {
        saveDoneAnalysis();
        sleep();
        saveDocument("vague-a.pdf");
        sleep();
        saveDocument("vague-b.pdf");

        mockMvc.perform(get("/api/v1/case-files/{id}/pieces-wave", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analyzedAt").exists())
                .andExpect(jsonPath("$.pendingCount").value(2))
                .andExpect(jsonPath("$.pendingPieces.length()").value(2))
                // tri desc : la plus récente (vague-b) en tête
                .andExpect(jsonPath("$.pendingPieces[0].filename").value("vague-b.pdf"))
                .andExpect(jsonPath("$.pendingPieces[1].filename").value("vague-a.pdf"));
    }

    // I-03 : pièces ajoutées AVANT l'analyse → pas en attente (count 0)
    @Test
    void wave_piecesBeforeAnalysis_returnsEmpty() throws Exception {
        saveDocument("ancienne.pdf");
        sleep();
        saveDoneAnalysis();

        mockMvc.perform(get("/api/v1/case-files/{id}/pieces-wave", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingCount").value(0));
    }

    // I-04 : dossier d'un autre workspace → 404
    @Test
    void wave_caseFileFromOtherWorkspace_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/pieces-wave", UUID.randomUUID())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-05 : sans auth → 401
    @Test
    void wave_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/pieces-wave", caseFile.getId()))
                .andExpect(status().isUnauthorized());
    }

    private void saveDoneAnalysis() {
        AnalysisJob job = new AnalysisJob();
        job.setCaseFileId(caseFile.getId());
        job.setJobType(JobType.CASE_ANALYSIS);
        job.setStatus(AnalysisStatus.DONE);
        job.setTotalItems(1);
        job.setProcessedItems(1);
        analysisJobRepository.save(job);
    }

    private Document saveDocument(String filename) {
        Document d = new Document();
        d.setCaseFile(caseFile);
        d.setUploadedBy(user);
        d.setOriginalFilename(filename);
        d.setContentType("application/pdf");
        d.setFileSize(1024L);
        d.setStorageKey("key/" + UUID.randomUUID());
        return documentRepository.save(d);
    }

    private void sleep() {
        try {
            Thread.sleep(15);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private OAuth2AuthenticationToken buildGoogleAuth(String sub, String email) {
        Map<String, Object> claims = Map.of(
                "sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
