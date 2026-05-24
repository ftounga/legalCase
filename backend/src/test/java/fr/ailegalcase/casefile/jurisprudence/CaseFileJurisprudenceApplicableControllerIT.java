package fr.ailegalcase.casefile.jurisprudence;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.audit.AuditLogRepository;
import fr.ailegalcase.billing.SubscriptionRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.jurisprudencemapping.ToolJurisprudenceMapping;
import fr.ailegalcase.jurisprudencemapping.ToolJurisprudenceMappingRepository;
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
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-JU-02 / SF-JU-02-02 — tests d'intégration de
 * {@link CaseFileJurisprudenceApplicableController}.
 *
 * <p>Couverture : 200 entries vides V1 (aucun {@code ToolUsageContributor}
 * implémenté), 401 sans auth, 404 dossier inconnu, 404 dossier d'un autre
 * workspace. La table {@code tool_jurisprudence_mappings} est seedée pour
 * vérifier qu'elle reste lisible mais retourne vide tant qu'aucun
 * contributor n'est branché (comportement neutre attendu V1).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class CaseFileJurisprudenceApplicableControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private ToolJurisprudenceMappingRepository mappingRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private OAuth2AuthenticationToken auth;
    private CaseFile caseFile;
    private Workspace workspace;
    private User user;

    private static final String BASE = "/api/v1/case-files/{caseFileId}/jurisprudence-applicable";

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        mappingRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        auditLogRepository.deleteAll();
        subscriptionRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        user = new User();
        user.setEmail("ju02-02-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-ju02-02-sub");
        authAccountRepository.save(account);

        workspace = new Workspace();
        workspace.setName("ju02-02-test@example.com");
        workspace.setSlug("ju02-02-slug-" + System.currentTimeMillis());
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
        caseFile.setTitle("Dossier Test Jurisprudence Applicable");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFile.setStatus("OPEN");
        caseFileRepository.save(caseFile);

        // Seed quelques mappings F-JU-01 — ils ne remonteront pas V1 car
        // aucun ToolUsageContributor n'est branché ; on vérifie le
        // comportement neutre attendu (CA SF-JU-02-01 Note 1).
        ToolJurisprudenceMapping mapping = new ToolJurisprudenceMapping();
        mapping.setToolId("f-dt-30-indemnite-licenciement-macron");
        mapping.setBrancheCalculId("default");
        mapping.setArretRef("Cass. soc. 8 janv. 2025, n° 23-12.345");
        mapping.setJuridiction("Cour de cassation, chambre sociale");
        mapping.setDateArret(LocalDate.of(2025, 1, 8));
        mapping.setNumeroPourvoi("23-12.345");
        mapping.setLienLegifrance("https://www.legifrance.gouv.fr/juri/id/JURITEXT000xxx");
        mapping.setChapeauOfficiel("Le barème Macron s'applique sans exception.");
        mapping.setLastVerifiedAt(Instant.now());
        mapping.setConfidenceScore(new BigDecimal("0.95"));
        mapping.setArchived(false);
        mappingRepository.save(mapping);

        auth = buildGoogleAuth("google-ju02-02-sub", "ju02-02-test@example.com");
    }

    @Test
    void get_nominalNoContributorV1_returns200EmptyEntries() throws Exception {
        // V1 : aucun outil n'implémente ToolUsageContributor → liste vide
        // (cf. SF-JU-02-01 Note 1) — comportement neutre attendu.
        mockMvc.perform(get(BASE, caseFile.getId()).with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries").isArray())
                .andExpect(jsonPath("$.entries.length()").value(0));
    }

    @Test
    void get_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get(BASE, caseFile.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void get_unknownCaseFile_returns404() throws Exception {
        mockMvc.perform(get(BASE, UUID.randomUUID()).with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_otherWorkspace_returns404() throws Exception {
        OAuth2AuthenticationToken otherAuth = buildOtherWorkspaceUser();
        mockMvc.perform(get(BASE, caseFile.getId()).with(authentication(otherAuth)))
                .andExpect(status().isNotFound());
    }

    /** Crée un second utilisateur / workspace et renvoie son jeton d'authentification. */
    private OAuth2AuthenticationToken buildOtherWorkspaceUser() {
        User otherUser = new User();
        otherUser.setEmail("ju02-02-other@example.com");
        otherUser.setStatus("ACTIVE");
        userRepository.save(otherUser);

        AuthAccount otherAccount = new AuthAccount();
        otherAccount.setUser(otherUser);
        otherAccount.setProvider("GOOGLE");
        otherAccount.setProviderUserId("google-ju02-02-other-sub");
        authAccountRepository.save(otherAccount);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("ju02-02-other@example.com");
        otherWorkspace.setSlug("ju02-02-other-slug-" + System.currentTimeMillis());
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

        return buildGoogleAuth("google-ju02-02-other-sub", "ju02-02-other@example.com");
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
