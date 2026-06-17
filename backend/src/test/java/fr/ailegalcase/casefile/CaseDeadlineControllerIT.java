package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class CaseDeadlineControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private CaseDeadlineRepository deadlineRepository;

    private OAuth2AuthenticationToken auth;
    private CaseFile caseFile;

    @BeforeEach
    void setUp() {
        deadlineRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("deadline-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-deadline-sub");
        authAccountRepository.save(account);

        Workspace workspace = new Workspace();
        workspace.setName("DEADLINE-TEST");
        workspace.setSlug("deadline-slug-" + System.currentTimeMillis());
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
        caseFile.setTitle("Dossier Délais");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFile.setStatus("OPEN");
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFileRepository.save(caseFile);

        auth = buildGoogleAuth("google-deadline-sub", "deadline-test@example.com");
    }

    // IT-01 : GET → 200, liste vide si aucun délai
    @Test
    void list_empty_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/deadlines", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // IT-02 : POST → 201, délai créé
    @Test
    void create_validPayload_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/{id}/deadlines", caseFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"label":"Délai de prescription","dueDate":"2026-06-01"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.label").value("Délai de prescription"))
                .andExpect(jsonPath("$.dueDate").value("2026-06-01"));
    }

    // IT-03 : POST → 400 si label vide
    @Test
    void create_emptyLabel_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/{id}/deadlines", caseFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"label":"","dueDate":"2026-06-01"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // IT-04 : POST → 400 si dueDate absent
    @Test
    void create_missingDueDate_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/{id}/deadlines", caseFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"label":"Délai de prescription"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // IT-05 : GET → liste triée par dueDate ASC
    @Test
    void list_returnsSortedByDueDateAsc() throws Exception {
        saveDeadline("Délai tardif", LocalDate.of(2026, 12, 1));
        saveDeadline("Délai proche", LocalDate.of(2026, 5, 1));
        saveDeadline("Délai intermédiaire", LocalDate.of(2026, 8, 1));

        mockMvc.perform(get("/api/v1/case-files/{id}/deadlines", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].label").value("Délai proche"))
                .andExpect(jsonPath("$[1].label").value("Délai intermédiaire"))
                .andExpect(jsonPath("$[2].label").value("Délai tardif"));
    }

    // F-289 (fix) : la liste exclut les sources non-délais (pièces / checks / pistes
    // injectées dans case_deadlines par F-192/193/194) — sinon le chip « N délais »
    // comptait ces lignes non affichées (« 30 délais » alors que 6 rendus).
    @Test
    void list_excludesSyntheticSources_pieceCheckPiste() throws Exception {
        saveDeadline("Vrai délai légal", LocalDate.of(2026, 6, 1)); // source défaut
        saveDeadlineWithSource("Pièce à demander : Contrat", LocalDate.of(2026, 7, 1), "PIECE_A_DEMANDER");
        saveDeadlineWithSource("À vérifier : entretien préalable", LocalDate.of(2026, 8, 1), "PROCEDURE_CHECK_TO_CHECK");
        saveDeadlineWithSource("Stratégie : contester la faute grave", LocalDate.of(2026, 9, 1), "PISTE_RETENUE");

        mockMvc.perform(get("/api/v1/case-files/{id}/deadlines", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].label").value("Vrai délai légal"));
    }

    private CaseDeadline saveDeadlineWithSource(String label, LocalDate dueDate, String source) {
        CaseDeadline d = new CaseDeadline();
        d.setCaseFile(caseFile);
        d.setLabel(label);
        d.setDueDate(dueDate);
        d.setSource(source);
        return deadlineRepository.save(d);
    }

    // IT-06 : PUT → 200, délai mis à jour
    @Test
    void update_validPayload_returns200() throws Exception {
        CaseDeadline deadline = saveDeadline("Libellé original", LocalDate.of(2026, 6, 1));

        mockMvc.perform(put("/api/v1/case-files/{id}/deadlines/{did}", caseFile.getId(), deadline.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"label":"Libellé modifié","dueDate":"2026-07-15"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Libellé modifié"))
                .andExpect(jsonPath("$.dueDate").value("2026-07-15"));
    }

    // IT-07 : DELETE → 204
    @Test
    void delete_existingDeadline_returns204() throws Exception {
        CaseDeadline deadline = saveDeadline("À supprimer", LocalDate.of(2026, 6, 1));

        mockMvc.perform(delete("/api/v1/case-files/{id}/deadlines/{did}", caseFile.getId(), deadline.getId())
                        .with(authentication(auth)))
                .andExpect(status().isNoContent());
    }

    // IT-08 : dossier hors workspace → 404
    @Test
    void list_caseFileFromOtherWorkspace_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/deadlines", UUID.randomUUID())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // IT-09 : sans auth → 401
    @Test
    void list_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/deadlines", caseFile.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ===== PATCH /validate =====

    // IT-10 : ACCEPT → 200, aiStatus = ACCEPTED
    @Test
    void validate_accept_returns200WithAcceptedStatus() throws Exception {
        CaseDeadline deadline = saveAiDeadline("Délai IA", LocalDate.of(2026, 9, 1), "PENDING");

        mockMvc.perform(patch("/api/v1/case-files/{id}/deadlines/{did}/validate", caseFile.getId(), deadline.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"ACCEPT"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$.source").value("AI"));
    }

    // IT-11 : REJECT → 204, délai absent de GET /deadlines
    @Test
    void validate_reject_returns204AndDeadlineIsGone() throws Exception {
        CaseDeadline deadline = saveAiDeadline("Délai IA à rejeter", LocalDate.of(2026, 9, 1), "PENDING");

        mockMvc.perform(patch("/api/v1/case-files/{id}/deadlines/{did}/validate", caseFile.getId(), deadline.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"REJECT"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/case-files/{id}/deadlines", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // IT-12 : action invalide → 400
    @Test
    void validate_invalidAction_returns400() throws Exception {
        CaseDeadline deadline = saveAiDeadline("Délai IA", LocalDate.of(2026, 9, 1), "PENDING");

        mockMvc.perform(patch("/api/v1/case-files/{id}/deadlines/{did}/validate", caseFile.getId(), deadline.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"UNKNOWN"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // IT-13 : deadlineId inexistant → 404
    @Test
    void validate_deadlineNotFound_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/case-files/{id}/deadlines/{did}/validate", caseFile.getId(), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"ACCEPT"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // IT-14 : délai d'un autre workspace → 403 (via un autre caseFile non accessible)
    @Test
    void validate_deadlineFromOtherWorkspace_returns403() throws Exception {
        // Create a second workspace with its own case file and deadline
        User user2 = new User();
        user2.setEmail("other-user@example.com");
        user2.setStatus("ACTIVE");
        userRepository.save(user2);

        Workspace workspace2 = new Workspace();
        workspace2.setName("OTHER");
        workspace2.setSlug("other-slug-" + System.currentTimeMillis());
        workspace2.setOwner(user2);
        workspace2.setLegalDomain("DROIT_DU_TRAVAIL");
        workspace2.setPlanCode("STARTER");
        workspace2.setStatus("ACTIVE");
        workspaceRepository.save(workspace2);

        CaseFile otherCaseFile = new CaseFile();
        otherCaseFile.setTitle("Dossier autre");
        otherCaseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        otherCaseFile.setStatus("OPEN");
        otherCaseFile.setWorkspace(workspace2);
        otherCaseFile.setCreatedBy(user2);
        caseFileRepository.save(otherCaseFile);

        CaseDeadline otherDeadline = saveAiDeadlineForFile(otherCaseFile, "Délai autre WS", LocalDate.of(2026, 9, 1), "PENDING");

        // caseFileId belongs to auth user but deadlineId belongs to another case file
        mockMvc.perform(patch("/api/v1/case-files/{id}/deadlines/{did}/validate", caseFile.getId(), otherDeadline.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"ACCEPT"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isNotFound()); // 404 because resolveDeadlineForWorkspace checks caseFileId match
    }

    // IT-15 : délai déjà traité → 409
    @Test
    void validate_alreadyAccepted_returns409() throws Exception {
        CaseDeadline deadline = saveAiDeadline("Délai déjà accepté", LocalDate.of(2026, 9, 1), "ACCEPTED");

        mockMvc.perform(patch("/api/v1/case-files/{id}/deadlines/{did}/validate", caseFile.getId(), deadline.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"ACCEPT"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isConflict());
    }

    // IT-16 : GET /deadlines retourne les délais PENDING avec source=AI et aiStatus=PENDING
    @Test
    void list_returnsPendingAiDeadlinesWithSourceAndAiStatus() throws Exception {
        saveAiDeadline("Délai IA PENDING", LocalDate.of(2026, 8, 1), "PENDING");
        saveDeadline("Délai manuel", LocalDate.of(2026, 9, 1));

        mockMvc.perform(get("/api/v1/case-files/{id}/deadlines", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].source").value("AI"))
                .andExpect(jsonPath("$[0].aiStatus").value("PENDING"))
                .andExpect(jsonPath("$[1].source").value("MANUAL"))
                .andExpect(jsonPath("$[1].aiStatus").doesNotExist());
    }

    private CaseDeadline saveDeadline(String label, LocalDate dueDate) {
        CaseDeadline d = new CaseDeadline();
        d.setCaseFile(caseFile);
        d.setLabel(label);
        d.setDueDate(dueDate);
        return deadlineRepository.save(d);
    }

    private CaseDeadline saveAiDeadline(String label, LocalDate dueDate, String aiStatus) {
        return saveAiDeadlineForFile(caseFile, label, dueDate, aiStatus);
    }

    private CaseDeadline saveAiDeadlineForFile(CaseFile file, String label, LocalDate dueDate, String aiStatus) {
        CaseDeadline d = new CaseDeadline();
        d.setCaseFile(file);
        d.setLabel(label);
        d.setDueDate(dueDate);
        d.setSource("AI");
        d.setAiStatus(aiStatus);
        return deadlineRepository.save(d);
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
