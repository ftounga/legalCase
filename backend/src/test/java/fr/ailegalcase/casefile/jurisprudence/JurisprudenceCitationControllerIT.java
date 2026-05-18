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
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-242 / SF-242-01 — tests d'intégration de {@link JurisprudenceCitationController} :
 * CRUD complet, validations {@code 400}, dossier / citation inexistants {@code 404},
 * isolation workspace {@code 404} (CA1, CA2, CA6).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class JurisprudenceCitationControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private JurisprudenceCitationRepository citationRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private OAuth2AuthenticationToken auth;
    private CaseFile caseFile;
    private Workspace workspace;
    private User user;

    private static final String BASE = "/api/v1/case-files/{caseFileId}/jurisprudence-citations";

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        citationRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        auditLogRepository.deleteAll();
        subscriptionRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        user = new User();
        user.setEmail("citation-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-citation-sub");
        authAccountRepository.save(account);

        workspace = new Workspace();
        workspace.setName("citation-test@example.com");
        workspace.setSlug("citation-slug-" + System.currentTimeMillis());
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
        caseFile.setTitle("Dossier Test Citations");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFile.setStatus("OPEN");
        caseFileRepository.save(caseFile);

        auth = buildGoogleAuth("google-citation-sub", "citation-test@example.com");
    }

    // ── GET ──────────────────────────────────────────────────────────────────

    @Test
    void list_noCitations_returns200EmptyList() throws Exception {
        mockMvc.perform(get(BASE, caseFile.getId()).with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citations.length()").value(0));
    }

    @Test
    void list_withCitations_returns200OrderedByPointJuridique() throws Exception {
        UUID c1 = createCitation(1, "Forclusion", "Cass. soc. 8 juin 2017, n° 15-28.599", null);
        UUID c0 = createCitation(0, "Entretien préalable",
                "Cass. soc. 12 oct. 2022, n° 21-12345", "Portée A");

        mockMvc.perform(get(BASE, caseFile.getId()).with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citations.length()").value(2))
                // tri par index de point juridique croissant : point 0 puis point 1
                .andExpect(jsonPath("$.citations[0].id").value(c0.toString()))
                .andExpect(jsonPath("$.citations[0].pointJuridiqueIndex").value(0))
                .andExpect(jsonPath("$.citations[1].id").value(c1.toString()))
                .andExpect(jsonPath("$.citations[1].pointJuridiqueIndex").value(1));
    }

    @Test
    void list_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get(BASE, caseFile.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_unknownCaseFile_returns404() throws Exception {
        mockMvc.perform(get(BASE, UUID.randomUUID()).with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_otherWorkspace_returns404() throws Exception {
        OAuth2AuthenticationToken otherAuth = buildOtherWorkspaceUser();
        mockMvc.perform(get(BASE, caseFile.getId()).with(authentication(otherAuth)))
                .andExpect(status().isNotFound());
    }

    // ── POST ─────────────────────────────────────────────────────────────────

    @Test
    void create_validRequest_returns201WithCitation() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "pointJuridiqueIndex", 0,
                "pointJuridiqueTexte", "Absence d'entretien préalable",
                "reference", "Cass. soc. 12 oct. 2022, n° 21-12345",
                "portee", "L'absence d'entretien préalable rend le licenciement irrégulier."));

        mockMvc.perform(post(BASE, caseFile.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.pointJuridiqueIndex").value(0))
                .andExpect(jsonPath("$.reference").value("Cass. soc. 12 oct. 2022, n° 21-12345"))
                .andExpect(jsonPath("$.portee").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        assertThat(citationRepository.findByCaseFileIdOrderByPointJuridiqueIndexAscCreatedAtAsc(
                caseFile.getId())).hasSize(1);
    }

    @Test
    void create_withoutPortee_returns201NullPortee() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "pointJuridiqueIndex", 2,
                "pointJuridiqueTexte", "Point juridique sans portée",
                "reference", "CE 30 juin 2017, n° 398445"));

        mockMvc.perform(post(BASE, caseFile.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.portee").doesNotExist());
    }

    @Test
    void create_missingReference_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "pointJuridiqueIndex", 0,
                "pointJuridiqueTexte", "Un point"));

        mockMvc.perform(post(BASE, caseFile.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_blankReference_returns400() throws Exception {
        String body = """
                {"pointJuridiqueIndex":0,"pointJuridiqueTexte":"Un point","reference":"   "}""";

        mockMvc.perform(post(BASE, caseFile.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_referenceTooLong_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "pointJuridiqueIndex", 0,
                "pointJuridiqueTexte", "Un point",
                "reference", "x".repeat(256)));

        mockMvc.perform(post(BASE, caseFile.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_porteeTooLong_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "pointJuridiqueIndex", 0,
                "pointJuridiqueTexte", "Un point",
                "reference", "Cass. soc. 12 oct. 2022",
                "portee", "x".repeat(501)));

        mockMvc.perform(post(BASE, caseFile.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_negativeIndex_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "pointJuridiqueIndex", -1,
                "pointJuridiqueTexte", "Un point",
                "reference", "Cass. soc. 12 oct. 2022"));

        mockMvc.perform(post(BASE, caseFile.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_missingPointJuridiqueIndex_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "pointJuridiqueTexte", "Un point",
                "reference", "Cass. soc. 12 oct. 2022"));

        mockMvc.perform(post(BASE, caseFile.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_blankPointJuridiqueTexte_returns400() throws Exception {
        String body = """
                {"pointJuridiqueIndex":0,"pointJuridiqueTexte":"  ","reference":"Cass. soc."}""";

        mockMvc.perform(post(BASE, caseFile.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_pointJuridiqueTexteTooLong_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "pointJuridiqueIndex", 0,
                "pointJuridiqueTexte", "x".repeat(2001),
                "reference", "Cass. soc. 12 oct. 2022"));

        mockMvc.perform(post(BASE, caseFile.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_unknownCaseFile_returns404() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "pointJuridiqueIndex", 0,
                "pointJuridiqueTexte", "Un point",
                "reference", "Cass. soc. 12 oct. 2022"));

        mockMvc.perform(post(BASE, UUID.randomUUID())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_otherWorkspace_returns404() throws Exception {
        OAuth2AuthenticationToken otherAuth = buildOtherWorkspaceUser();
        String body = objectMapper.writeValueAsString(Map.of(
                "pointJuridiqueIndex", 0,
                "pointJuridiqueTexte", "Un point",
                "reference", "Cass. soc. 12 oct. 2022"));

        mockMvc.perform(post(BASE, caseFile.getId())
                        .with(authentication(otherAuth))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
        assertThat(citationRepository.findByCaseFileIdOrderByPointJuridiqueIndexAscCreatedAtAsc(
                caseFile.getId())).isEmpty();
    }

    // ── PUT ──────────────────────────────────────────────────────────────────

    @Test
    void update_validRequest_returns200WithUpdatedFields() throws Exception {
        UUID id = createCitation(0, "Un point", "Cass. soc. ancienne", "Portée ancienne");
        String body = objectMapper.writeValueAsString(Map.of(
                "reference", "Cass. soc. 12 oct. 2022, n° 21-12345",
                "portee", "Portée actualisée"));

        mockMvc.perform(put(BASE + "/{citationId}", caseFile.getId(), id)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.reference").value("Cass. soc. 12 oct. 2022, n° 21-12345"))
                .andExpect(jsonPath("$.portee").value("Portée actualisée"))
                // le rattachement au point juridique est figé
                .andExpect(jsonPath("$.pointJuridiqueIndex").value(0))
                .andExpect(jsonPath("$.pointJuridiqueTexte").value("Un point"));
    }

    @Test
    void update_clearPortee_returns200NullPortee() throws Exception {
        UUID id = createCitation(0, "Un point", "Cass. soc.", "Portée à effacer");
        String body = objectMapper.writeValueAsString(Map.of("reference", "Cass. soc. révisée"));

        mockMvc.perform(put(BASE + "/{citationId}", caseFile.getId(), id)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portee").doesNotExist());
    }

    @Test
    void update_missingReference_returns400() throws Exception {
        UUID id = createCitation(0, "Un point", "Cass. soc.", null);
        String body = objectMapper.writeValueAsString(Map.of("portee", "Portée seule"));

        mockMvc.perform(put(BASE + "/{citationId}", caseFile.getId(), id)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_unknownCitation_returns404() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("reference", "Cass. soc."));

        mockMvc.perform(put(BASE + "/{citationId}", caseFile.getId(), UUID.randomUUID())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_unknownCaseFile_returns404() throws Exception {
        UUID id = createCitation(0, "Un point", "Cass. soc.", null);
        String body = objectMapper.writeValueAsString(Map.of("reference", "Cass. soc. révisée"));

        mockMvc.perform(put(BASE + "/{citationId}", UUID.randomUUID(), id)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_otherWorkspace_returns404() throws Exception {
        UUID id = createCitation(0, "Un point", "Cass. soc.", null);
        OAuth2AuthenticationToken otherAuth = buildOtherWorkspaceUser();
        String body = objectMapper.writeValueAsString(Map.of("reference", "Cass. soc. piratée"));

        mockMvc.perform(put(BASE + "/{citationId}", caseFile.getId(), id)
                        .with(authentication(otherAuth))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    @Test
    void delete_existingCitation_returns204AndRemovesIt() throws Exception {
        UUID id = createCitation(0, "Un point", "Cass. soc.", null);

        mockMvc.perform(delete(BASE + "/{citationId}", caseFile.getId(), id)
                        .with(authentication(auth)))
                .andExpect(status().isNoContent());

        assertThat(citationRepository.findById(id)).isEmpty();
    }

    @Test
    void delete_unknownCitation_returns404() throws Exception {
        mockMvc.perform(delete(BASE + "/{citationId}", caseFile.getId(), UUID.randomUUID())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_unknownCaseFile_returns404() throws Exception {
        UUID id = createCitation(0, "Un point", "Cass. soc.", null);
        mockMvc.perform(delete(BASE + "/{citationId}", UUID.randomUUID(), id)
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_otherWorkspace_returns404AndKeepsCitation() throws Exception {
        UUID id = createCitation(0, "Un point", "Cass. soc.", null);
        OAuth2AuthenticationToken otherAuth = buildOtherWorkspaceUser();

        mockMvc.perform(delete(BASE + "/{citationId}", caseFile.getId(), id)
                        .with(authentication(otherAuth)))
                .andExpect(status().isNotFound());
        assertThat(citationRepository.findById(id)).isPresent();
    }

    // ── fixtures ─────────────────────────────────────────────────────────────

    /** Crée une citation via l'API et renvoie son id (vérifie le flux POST nominal). */
    private UUID createCitation(int index, String pointTexte, String reference, String portee)
            throws Exception {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("pointJuridiqueIndex", index);
        payload.put("pointJuridiqueTexte", pointTexte);
        payload.put("reference", reference);
        if (portee != null) {
            payload.put("portee", portee);
        }
        MvcResult result = mockMvc.perform(post(BASE, caseFile.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        Map<?, ?> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), Map.class);
        return UUID.fromString((String) response.get("id"));
    }

    /** Crée un second utilisateur / workspace et renvoie son jeton d'authentification. */
    private OAuth2AuthenticationToken buildOtherWorkspaceUser() {
        User otherUser = new User();
        otherUser.setEmail("other-citation@example.com");
        otherUser.setStatus("ACTIVE");
        userRepository.save(otherUser);

        AuthAccount otherAccount = new AuthAccount();
        otherAccount.setUser(otherUser);
        otherAccount.setProvider("GOOGLE");
        otherAccount.setProviderUserId("google-other-citation-sub");
        authAccountRepository.save(otherAccount);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("other-citation@example.com");
        otherWorkspace.setSlug("other-citation-slug-" + System.currentTimeMillis());
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

        return buildGoogleAuth("google-other-citation-sub", "other-citation@example.com");
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
