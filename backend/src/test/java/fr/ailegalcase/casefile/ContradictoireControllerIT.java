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
class ContradictoireControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private ContradictoireRoundRepository roundRepository;

    private OAuth2AuthenticationToken auth;
    private CaseFile caseFile;
    private Workspace workspace;
    private User user;

    @BeforeEach
    void setUp() {
        roundRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setEmail("contra-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-contra-sub");
        authAccountRepository.save(account);

        workspace = new Workspace();
        workspace.setName("CONTRA-TEST");
        workspace.setSlug("contra-slug-" + System.currentTimeMillis());
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
        caseFile.setTitle("Dossier contradictoire");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFile.setStatus("OPEN");
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFileRepository.save(caseFile);

        auth = buildGoogleAuth("google-contra-sub", "contra-test@example.com");
    }

    // I-01 : timeline vide → c'est à NOUS de saisir (round 1)
    @Test
    void timeline_empty_summaryAwaitsOurs() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/contradictoire-rounds", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rounds.length()").value(0))
                .andExpect(jsonPath("$.summary.currentRoundNumber").value(0))
                .andExpect(jsonPath("$.summary.awaitingParty").value("OURS"));
    }

    // I-02 : POST round adverse → 201, round 1
    @Test
    void create_adverseRound_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/{id}/contradictoire-rounds", caseFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"party":"ADVERSE","label":"Conclusions adverses","datedAt":"2026-06-14","responseDueAt":"2026-07-14"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.party").value("ADVERSE"))
                .andExpect(jsonPath("$.roundNumber").value(1))
                .andExpect(jsonPath("$.responseDueAt").value("2026-07-14"));
    }

    // I-03 : party manquant → 400
    @Test
    void create_missingParty_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/{id}/contradictoire-rounds", caseFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"datedAt":"2026-06-14"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-04 : échéance avant la date → 400
    @Test
    void create_dueBeforeDated_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/{id}/contradictoire-rounds", caseFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"party":"OURS","datedAt":"2026-06-14","responseDueAt":"2026-06-01"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-05 : timeline ordonnée + résumé (dernier round adverse → à nous)
    @Test
    void timeline_ordered_andSummaryComputed() throws Exception {
        saveRound(1, ContradictoireParty.OURS, null);
        saveRound(2, ContradictoireParty.ADVERSE, LocalDate.of(2026, 7, 14));

        mockMvc.perform(get("/api/v1/case-files/{id}/contradictoire-rounds", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rounds.length()").value(2))
                .andExpect(jsonPath("$.rounds[0].roundNumber").value(1))
                .andExpect(jsonPath("$.rounds[1].roundNumber").value(2))
                .andExpect(jsonPath("$.summary.currentRoundNumber").value(2))
                .andExpect(jsonPath("$.summary.awaitingParty").value("OURS"))
                .andExpect(jsonPath("$.summary.nextDeadline").value("2026-07-14"));
    }

    // I-06 : PUT → 200
    @Test
    void update_returns200() throws Exception {
        ContradictoireRound r = saveRound(1, ContradictoireParty.OURS, null);
        mockMvc.perform(put("/api/v1/case-files/{id}/contradictoire-rounds/{rid}", caseFile.getId(), r.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"party":"OURS","label":"Saisine déposée","datedAt":"2026-06-10"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Saisine déposée"));
    }

    // I-07 : DELETE → 204
    @Test
    void delete_returns204() throws Exception {
        ContradictoireRound r = saveRound(1, ContradictoireParty.OURS, null);
        mockMvc.perform(delete("/api/v1/case-files/{id}/contradictoire-rounds/{rid}", caseFile.getId(), r.getId())
                        .with(authentication(auth)))
                .andExpect(status().isNoContent());
    }

    // I-08 : dossier d'un autre workspace → 404
    @Test
    void timeline_caseFileFromOtherWorkspace_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/contradictoire-rounds", UUID.randomUUID())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-09 : round d'un AUTRE dossier (même URL) → 404 (isolation par dossier)
    @Test
    void update_roundFromAnotherCaseFile_returns404() throws Exception {
        CaseFile other = new CaseFile();
        other.setTitle("Autre dossier");
        other.setLegalDomain("DROIT_DU_TRAVAIL");
        other.setStatus("OPEN");
        other.setWorkspace(workspace);
        other.setCreatedBy(user);
        caseFileRepository.save(other);

        ContradictoireRound r = new ContradictoireRound();
        r.setCaseFile(other);
        r.setRoundNumber(1);
        r.setParty(ContradictoireParty.OURS);
        r.setDatedAt(LocalDate.of(2026, 6, 1));
        roundRepository.save(r);

        mockMvc.perform(put("/api/v1/case-files/{id}/contradictoire-rounds/{rid}", caseFile.getId(), r.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"party":"OURS","datedAt":"2026-06-10"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-10 : sans auth → 401
    @Test
    void timeline_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/contradictoire-rounds", caseFile.getId()))
                .andExpect(status().isUnauthorized());
    }

    private ContradictoireRound saveRound(int n, ContradictoireParty party, LocalDate dueAt) {
        ContradictoireRound r = new ContradictoireRound();
        r.setCaseFile(caseFile);
        r.setRoundNumber(n);
        r.setParty(party);
        r.setDatedAt(LocalDate.of(2026, 6, n));
        r.setResponseDueAt(dueAt);
        return roundRepository.save(r);
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
