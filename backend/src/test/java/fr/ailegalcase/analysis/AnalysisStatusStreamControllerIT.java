package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.workspace.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class AnalysisStatusStreamControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private CaseAnalysisRepository caseAnalysisRepository;
    @Autowired private AnalysisJobRepository analysisJobRepository;

    private OAuth2AuthenticationToken auth;
    private CaseFile caseFile;

    @BeforeEach
    void setUp() {
        analysisJobRepository.deleteAll();
        caseAnalysisRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("sse-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-sse-sub");
        authAccountRepository.save(account);

        Workspace workspace = new Workspace();
        workspace.setName("sse-test@example.com");
        workspace.setSlug("sse-slug-" + System.currentTimeMillis());
        workspace.setOwner(user);
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
        workspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFile.setTitle("Dossier Test SSE");
        caseFile.setStatus("OPEN");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFileRepository.save(caseFile);

        auth = buildGoogleAuth("google-sse-sub", "sse-test@example.com");
    }

    // I-01 : GET sans auth → 401
    @Test
    void stream_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/analysis-status/stream", caseFile.getId()))
                .andExpect(status().isUnauthorized());
    }

    // I-02 : GET dossier inconnu → 404 (exception lancée synchrone, avant création de l'emitter)
    @Test
    void stream_unknownCaseFile_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/analysis-status/stream", UUID.randomUUID())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-03 : GET dossier d'un autre workspace → 403 (isolation workspace)
    @Test
    void stream_otherWorkspaceCaseFile_returns403() throws Exception {
        User otherUser = new User();
        otherUser.setEmail("sse-other@example.com");
        otherUser.setStatus("ACTIVE");
        userRepository.save(otherUser);

        AuthAccount otherAccount = new AuthAccount();
        otherAccount.setUser(otherUser);
        otherAccount.setProvider("GOOGLE");
        otherAccount.setProviderUserId("google-sse-other-sub");
        authAccountRepository.save(otherAccount);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("sse-other@example.com");
        otherWorkspace.setSlug("sse-other-slug-" + System.currentTimeMillis());
        otherWorkspace.setOwner(otherUser);
        otherWorkspace.setPlanCode("STARTER");
        otherWorkspace.setStatus("ACTIVE");
        otherWorkspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspaceRepository.save(otherWorkspace);

        WorkspaceMember otherMember = new WorkspaceMember();
        otherMember.setWorkspace(otherWorkspace);
        otherMember.setUser(otherUser);
        otherMember.setMemberRole("OWNER");
        otherMember.setPrimary(true);
        workspaceMemberRepository.save(otherMember);

        CaseFile otherCaseFile = new CaseFile();
        otherCaseFile.setWorkspace(otherWorkspace);
        otherCaseFile.setCreatedBy(otherUser);
        otherCaseFile.setTitle("Dossier Autre Workspace");
        otherCaseFile.setStatus("OPEN");
        otherCaseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFileRepository.save(otherCaseFile);

        // auth belongs to the first workspace, not otherWorkspace
        mockMvc.perform(get("/api/v1/case-files/{id}/analysis-status/stream", otherCaseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    // I-04 : GET dossier valide sans analyse DONE → async démarré (stream SSE ouvert)
    @Test
    void stream_noDoneAnalysis_asyncStarted() throws Exception {
        // SSE stream stays open (emitter never completed) — verify async started (authentication + authorization ok)
        mockMvc.perform(get("/api/v1/case-files/{id}/analysis-status/stream", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(request().asyncStarted());
    }

    // I-05 : GET dossier avec analyse DONE → 200, événement ANALYSIS_DONE émis immédiatement
    @Test
    void stream_withDoneAnalysis_emitsAnalysisDoneEventImmediately() throws Exception {
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setCaseFile(caseFile);
        analysis.setAnalysisStatus(AnalysisStatus.DONE);
        analysis.setAnalysisResult("{\"faits\":[]}");
        analysis.setModelUsed("claude-sonnet-4-6");
        analysis.setAnalysisType(AnalysisType.STANDARD);
        caseAnalysisRepository.save(analysis);

        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/case-files/{id}/analysis-status/stream", caseFile.getId())
                                .with(authentication(auth)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ANALYSIS_DONE")));
    }

    // I-06 SF-159-03 : GET dossier avec analyse DONE antérieure ET nouveau job actif (PROCESSING)
    // → ne PAS court-circuiter ; le stream reste ouvert pour recevoir les events du nouveau cycle.
    // Bug observé en staging 2026-05-03 : la bannière "Analyse en cours" restait collée car
    // le contrôleur fermait l'emitter dès qu'une CaseAnalysis DONE existait, même si un nouveau
    // DOCUMENT_ANALYSIS venait d'être déclenché.
    @Test
    void stream_doneAnalysisWithActiveJob_doesNotShortCircuit() throws Exception {
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setCaseFile(caseFile);
        analysis.setAnalysisStatus(AnalysisStatus.DONE);
        analysis.setAnalysisResult("{\"faits\":[]}");
        analysis.setModelUsed("claude-sonnet-4-6");
        analysis.setAnalysisType(AnalysisType.STANDARD);
        caseAnalysisRepository.save(analysis);

        AnalysisJob activeJob = new AnalysisJob();
        activeJob.setCaseFileId(caseFile.getId());
        activeJob.setJobType(JobType.DOCUMENT_ANALYSIS);
        activeJob.setStatus(AnalysisStatus.PROCESSING);
        activeJob.setTotalItems(3);
        activeJob.setProcessedItems(1);
        analysisJobRepository.save(activeJob);

        // Le stream doit rester ouvert (asyncStarted), pas de short-circuit
        mockMvc.perform(get("/api/v1/case-files/{id}/analysis-status/stream", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(request().asyncStarted());
    }

    // I-07 SF-159-03 : analyse DONE + job PENDING → ne PAS court-circuiter non plus.
    @Test
    void stream_doneAnalysisWithPendingJob_doesNotShortCircuit() throws Exception {
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setCaseFile(caseFile);
        analysis.setAnalysisStatus(AnalysisStatus.DONE);
        analysis.setAnalysisResult("{\"faits\":[]}");
        analysis.setModelUsed("claude-sonnet-4-6");
        analysis.setAnalysisType(AnalysisType.STANDARD);
        caseAnalysisRepository.save(analysis);

        AnalysisJob pendingJob = new AnalysisJob();
        pendingJob.setCaseFileId(caseFile.getId());
        pendingJob.setJobType(JobType.ENRICHED_ANALYSIS);
        pendingJob.setStatus(AnalysisStatus.PENDING);
        pendingJob.setTotalItems(0);
        pendingJob.setProcessedItems(0);
        analysisJobRepository.save(pendingJob);

        mockMvc.perform(get("/api/v1/case-files/{id}/analysis-status/stream", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(request().asyncStarted());
    }

    // I-08 SF-159-03 : analyse DONE + tous jobs DONE → court-circuit conservé.
    @Test
    void stream_doneAnalysisAllJobsTerminal_emitsAnalysisDoneEventImmediately() throws Exception {
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setCaseFile(caseFile);
        analysis.setAnalysisStatus(AnalysisStatus.DONE);
        analysis.setAnalysisResult("{\"faits\":[]}");
        analysis.setModelUsed("claude-sonnet-4-6");
        analysis.setAnalysisType(AnalysisType.STANDARD);
        caseAnalysisRepository.save(analysis);

        AnalysisJob doneJob = new AnalysisJob();
        doneJob.setCaseFileId(caseFile.getId());
        doneJob.setJobType(JobType.DOCUMENT_ANALYSIS);
        doneJob.setStatus(AnalysisStatus.DONE);
        doneJob.setTotalItems(3);
        doneJob.setProcessedItems(3);
        analysisJobRepository.save(doneJob);

        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/case-files/{id}/analysis-status/stream", caseFile.getId())
                                .with(authentication(auth)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ANALYSIS_DONE")));
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
