package fr.ailegalcase.jurisprudencemapping;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.audit.AuditLogRepository;
import fr.ailegalcase.billing.SubscriptionRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-JU-01 / SF-JU-01-01 — tests d'intégration de {@link ToolJurisprudenceController}.
 *
 * <p>L'endpoint n'a pas d'isolation workspace (table globale) — seule
 * l'authentification est vérifiée. Le setup crée néanmoins un user + workspace
 * minimal pour permettre l'authentification OAuth2.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class ToolJurisprudenceControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private ToolJurisprudenceMappingRepository mappingRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private OAuth2AuthenticationToken auth;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        mappingRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        auditLogRepository.deleteAll();
        subscriptionRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        User user = new User();
        user.setEmail("tool-jurisp-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-tool-jurisp-sub");
        authAccountRepository.save(account);

        Workspace workspace = new Workspace();
        workspace.setName("tool-jurisp-test@example.com");
        workspace.setSlug("tool-jurisp-slug-" + System.currentTimeMillis());
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

        auth = buildGoogleAuth("google-tool-jurisp-sub", "tool-jurisp-test@example.com");
    }

    @Test
    void getCitations_returns200WithEmptyList_whenNoMapping() throws Exception {
        mockMvc.perform(get("/api/v1/tools/{toolId}/jurisprudence-citations", "f-dt-30")
                        .param("branch", "anciennete-superieure-10-ans")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getCitations_returns200With3Arrets_whenMappingsSeeded() throws Exception {
        saveMapping("f-dt-30", "anciennete-superieure-10-ans",
                "Cass. soc. 8 janv. 2025, n° 23-12.345", new BigDecimal("0.95"),
                LocalDate.of(2025, 1, 8));
        saveMapping("f-dt-30", "anciennete-superieure-10-ans",
                "Cass. soc. 12 mars 2024, n° 22-XXX", new BigDecimal("0.85"),
                LocalDate.of(2024, 3, 12));
        saveMapping("f-dt-30", "anciennete-superieure-10-ans",
                "Cass. soc. 5 sept. 2023, n° 21-YYY", new BigDecimal("0.75"),
                LocalDate.of(2023, 9, 5));

        mockMvc.perform(get("/api/v1/tools/{toolId}/jurisprudence-citations", "f-dt-30")
                        .param("branch", "anciennete-superieure-10-ans")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].arretRef").value("Cass. soc. 8 janv. 2025, n° 23-12.345"))
                .andExpect(jsonPath("$[0].confidenceScore").value(0.95))
                .andExpect(jsonPath("$[2].arretRef").value("Cass. soc. 5 sept. 2023, n° 21-YYY"));
    }

    @Test
    void getCitations_returnsMaxThree_evenWhenFiveMappingsExist() throws Exception {
        for (int i = 0; i < 5; i++) {
            saveMapping("f-dt-30", "branche-x",
                    "Arret " + i, new BigDecimal("0." + (90 - i * 5)),
                    LocalDate.of(2024, 1, 1).plusDays(i));
        }

        mockMvc.perform(get("/api/v1/tools/{toolId}/jurisprudence-citations", "f-dt-30")
                        .param("branch", "branche-x")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void getCitations_excludesArchivedMappings() throws Exception {
        ToolJurisprudenceMapping active = saveMapping("f-dt-30", "branche-x",
                "Active", new BigDecimal("0.80"), LocalDate.of(2024, 1, 1));
        ToolJurisprudenceMapping archived = saveMapping("f-dt-30", "branche-x",
                "Archived", new BigDecimal("0.95"), LocalDate.of(2025, 1, 1));
        archived.setArchived(true);
        mappingRepository.save(archived);

        mockMvc.perform(get("/api/v1/tools/{toolId}/jurisprudence-citations", "f-dt-30")
                        .param("branch", "branche-x")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].arretRef").value("Active"));
    }

    @Test
    void getCitations_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/tools/{toolId}/jurisprudence-citations", "f-dt-30")
                        .param("branch", "anciennete-superieure-10-ans"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCitations_returns400_whenToolIdInvalidFormat() throws Exception {
        mockMvc.perform(get("/api/v1/tools/{toolId}/jurisprudence-citations", "tool with spaces!")
                        .param("branch", "branche-x")
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCitations_returns200WithEmptyList_whenBranchParamMissing() throws Exception {
        saveMapping("f-dt-30", "branche-x",
                "Arret", new BigDecimal("0.80"), LocalDate.of(2024, 1, 1));

        mockMvc.perform(get("/api/v1/tools/{toolId}/jurisprudence-citations", "f-dt-30")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getCitations_returns200_evenIfToolIdNotInToolRegistry() throws Exception {
        // l'endpoint ne valide pas l'existence dans le TOOL_REGISTRY frontend —
        // l'absence de mapping produit simplement une liste vide
        mockMvc.perform(get("/api/v1/tools/{toolId}/jurisprudence-citations", "outil-inexistant")
                        .param("branch", "branche-x")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private ToolJurisprudenceMapping saveMapping(String toolId, String brancheCalculId,
                                                  String arretRef, BigDecimal confidenceScore,
                                                  LocalDate dateArret) {
        ToolJurisprudenceMapping mapping = new ToolJurisprudenceMapping();
        mapping.setToolId(toolId);
        mapping.setBrancheCalculId(brancheCalculId);
        mapping.setArretRef(arretRef);
        mapping.setJuridiction("Cour de cassation, chambre sociale");
        mapping.setDateArret(dateArret);
        mapping.setNumeroPourvoi("23-12.345");
        mapping.setLienLegifrance("https://www.legifrance.gouv.fr/juri/id/X");
        mapping.setChapeauOfficiel("Chapeau test.");
        mapping.setLastVerifiedAt(Instant.parse("2026-05-01T03:00:00Z"));
        mapping.setConfidenceScore(confidenceScore);
        mapping.setArchived(false);
        return mappingRepository.save(mapping);
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
