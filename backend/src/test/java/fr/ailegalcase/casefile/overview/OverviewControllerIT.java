package fr.ailegalcase.casefile.overview;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.casefile.CaseDeadline;
import fr.ailegalcase.casefile.CaseDeadlineRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.casefile.ContradictoireParty;
import fr.ailegalcase.casefile.ContradictoireRound;
import fr.ailegalcase.casefile.ContradictoireRoundRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import fr.ailegalcase.workspace.WorkspaceRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-289 / SF-289-01 — IT de l'endpoint d'agrégation « Vue d'ensemble » (lecture seule).
 * Vérifie : 200 forme complète, isolation workspace (404), absence d'auth (401).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class OverviewControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private CaseDeadlineRepository deadlineRepository;
    @Autowired private ContradictoireRoundRepository roundRepository;

    private OAuth2AuthenticationToken auth;
    private OAuth2AuthenticationToken otherAuth;
    private CaseFile caseFile;

    @BeforeEach
    void setUp() {
        roundRepository.deleteAll();
        deadlineRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        // ── Workspace A (propriétaire du dossier) ─────────────────────────────
        User user = newUser("overview-test@example.com");
        newAuthAccount(user, "google-overview-sub");
        Workspace workspace = newWorkspace(user, "OVERVIEW-TEST", "overview-slug-");
        newMember(workspace, user);

        caseFile = new CaseFile();
        caseFile.setTitle("Dossier Vue d'ensemble");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFile.setStatus("OPEN");
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFileRepository.save(caseFile);

        // ── Workspace B (intrus) ──────────────────────────────────────────────
        User other = newUser("overview-other@example.com");
        newAuthAccount(other, "google-overview-other-sub");
        Workspace otherWs = newWorkspace(other, "OVERVIEW-OTHER", "overview-other-slug-");
        newMember(otherWs, other);

        auth = buildGoogleAuth("google-overview-sub", "overview-test@example.com");
        otherAuth = buildGoogleAuth("google-overview-other-sub", "overview-other@example.com");
    }

    // 200 — dossier neuf : forme complète, états vides honnêtes (rien d'inventé).
    @Test
    void overview_emptyCaseFile_returns200WithEmptyShape() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/overview", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pilotage").exists())
                .andExpect(jsonPath("$.pilotage.analysisStale").value(false))
                .andExpect(jsonPath("$.pilotage.nextDeadline").doesNotExist())
                .andExpect(jsonPath("$.attention.length()").value(0))
                .andExpect(jsonPath("$.attentionTotal").value(0))
                .andExpect(jsonPath("$.fil.length()").value(0));
    }

    // 200 — forme complète avec un round adverse + une échéance.
    @Test
    void overview_withRoundAndDeadline_aggregates() throws Exception {
        LocalDate today = LocalDate.now();
        saveDeadline("Conclusions à déposer", today.plusDays(3), "MANUAL", null);
        saveRound(1, ContradictoireParty.ADVERSE, today.minusDays(2), today.plusDays(20));

        mockMvc.perform(get("/api/v1/case-files/{id}/overview", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                // pilotage : prochain couperet = l'échéance la plus proche (CRITICAL).
                .andExpect(jsonPath("$.pilotage.nextDeadline.label").value("Conclusions à déposer"))
                .andExpect(jsonPath("$.pilotage.nextDeadline.urgency").value("CRITICAL"))
                .andExpect(jsonPath("$.pilotage.awaitingParty").value("OURS"))
                // attention : l'échéance CRITICAL remonte.
                .andExpect(jsonPath("$.attention[0].type").value("ECHEANCE"))
                .andExpect(jsonPath("$.attentionTotal").value(1))
                // fil : le round (ECHANGE, passé) + l'échéance manuelle future (ECHEANCE)
                // + l'échéance de réponse du round (CONTRADICTOIRE → ECHEANCE future),
                // triés par date croissante.
                .andExpect(jsonPath("$.fil.length()").value(3))
                .andExpect(jsonPath("$.fil[0].voie").value("ECHANGE"))
                .andExpect(jsonPath("$.fil[0].acteur").value("ADVERSE"))
                .andExpect(jsonPath("$.fil[0].action.kind").value("GENERATE_REPLY"))
                .andExpect(jsonPath("$.fil[0].temps").value("PASSE"))
                .andExpect(jsonPath("$.fil[1].voie").value("ECHEANCE"))
                .andExpect(jsonPath("$.fil[1].temps").value("FUTUR"));
    }

    // 404 — dossier d'un AUTRE workspace (isolation).
    @Test
    void overview_otherWorkspaceUser_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/overview", caseFile.getId())
                        .with(authentication(otherAuth)))
                .andExpect(status().isNotFound());
    }

    // 404 — dossier inexistant.
    @Test
    void overview_unknownCaseFile_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/overview", UUID.randomUUID())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // 401 — sans auth.
    @Test
    void overview_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/overview", caseFile.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User newUser(String email) {
        User u = new User();
        u.setEmail(email);
        u.setStatus("ACTIVE");
        return userRepository.save(u);
    }

    private void newAuthAccount(User user, String providerUserId) {
        AuthAccount a = new AuthAccount();
        a.setUser(user);
        a.setProvider("GOOGLE");
        a.setProviderUserId(providerUserId);
        authAccountRepository.save(a);
    }

    private Workspace newWorkspace(User owner, String name, String slugPrefix) {
        Workspace w = new Workspace();
        w.setName(name);
        w.setSlug(slugPrefix + System.nanoTime());
        w.setOwner(owner);
        w.setLegalDomain("DROIT_DU_TRAVAIL");
        w.setPlanCode("STARTER");
        w.setStatus("ACTIVE");
        return workspaceRepository.save(w);
    }

    private void newMember(Workspace workspace, User user) {
        WorkspaceMember m = new WorkspaceMember();
        m.setWorkspace(workspace);
        m.setUser(user);
        m.setMemberRole("OWNER");
        m.setPrimary(true);
        workspaceMemberRepository.save(m);
    }

    private void saveDeadline(String label, LocalDate dueDate, String source, String aiStatus) {
        CaseDeadline d = new CaseDeadline();
        d.setCaseFile(caseFile);
        d.setLabel(label);
        d.setDueDate(dueDate);
        d.setSource(source);
        d.setAiStatus(aiStatus);
        deadlineRepository.save(d);
    }

    private void saveRound(int number, ContradictoireParty party, LocalDate datedAt, LocalDate responseDueAt) {
        ContradictoireRound r = new ContradictoireRound();
        r.setCaseFile(caseFile);
        r.setRoundNumber(number);
        r.setParty(party);
        r.setLabel("Conclusions adverses");
        r.setDatedAt(datedAt);
        r.setResponseDueAt(responseDueAt);
        roundRepository.save(r);
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
