package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SF-217-11 : tests d'intégration de l'endpoint d'analyse de dévolution /
 * réserve héréditaire belge (Vague 3 Famille BE — BELGIQUE).
 *
 * <p>Couvre : POST nominal (RESERVE_RESPECTEE), QUOTITE_DEPASSEE, DEVOLUTION_ETABLIE,
 * recalcul (upsert), GET après POST, 400 (date manquante, masse négative,
 * commentaire trop long), 401, 403 (autre workspace), 404 (dossier inexistant,
 * GET avant POST), 422 (domaine ≠ famille, pays ≠ BELGIQUE).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class SuccessionBeDevolutionReserveControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authBe;
    private OAuth2AuthenticationToken authOther;
    private CaseFile familleBeCf;
    private CaseFile travailBeCf;
    private CaseFile familleFrCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // Workspace A — BE / droit de la famille
        User u1 = new User(); u1.setEmail("succbe-" + ts + "@ex.com"); u1.setStatus("ACTIVE"); u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE"); a1.setProviderUserId("g-succbe-" + ts); authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("Succ BE " + ts); ws1.setSlug("ws-succbe-" + ts); ws1.setOwner(u1);
        ws1.setLegalDomain("DROIT_FAMILLE"); ws1.setCountry("BELGIQUE"); ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE"); ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember(); m1.setWorkspace(ws1); m1.setUser(u1); m1.setMemberRole("OWNER"); m1.setPrimary(true); workspaceMemberRepository.save(m1);
        familleBeCf = new CaseFile(); familleBeCf.setTitle("Succ BE " + ts); familleBeCf.setWorkspace(ws1); familleBeCf.setCreatedBy(u1);
        familleBeCf.setLegalDomain("DROIT_FAMILLE"); familleBeCf.setStatus("OPEN"); familleBeCf = caseFileRepository.save(familleBeCf);
        // Dossier droit du travail dans le même workspace BE (pour le 422 domaine).
        travailBeCf = new CaseFile(); travailBeCf.setTitle("Travail " + ts); travailBeCf.setWorkspace(ws1); travailBeCf.setCreatedBy(u1);
        travailBeCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailBeCf.setStatus("OPEN"); travailBeCf = caseFileRepository.save(travailBeCf);
        authBe = buildAuth("g-succbe-" + ts, "succbe-" + ts + "@ex.com");

        // Workspace B — FR / droit de la famille (autre workspace + 422 pays)
        User u2 = new User(); u2.setEmail("succfr-" + ts + "@ex.com"); u2.setStatus("ACTIVE"); u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE"); a2.setProviderUserId("g-succfr-" + ts); authAccountRepository.save(a2);
        Workspace ws2 = new Workspace(); ws2.setName("Succ FR " + ts); ws2.setSlug("ws-succfr-" + ts); ws2.setOwner(u2);
        ws2.setLegalDomain("DROIT_FAMILLE"); ws2.setCountry("FRANCE"); ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE"); ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember(); m2.setWorkspace(ws2); m2.setUser(u2); m2.setMemberRole("OWNER"); m2.setPrimary(true); workspaceMemberRepository.save(m2);
        familleFrCf = new CaseFile(); familleFrCf.setTitle("Succ FR " + ts); familleFrCf.setWorkspace(ws2); familleFrCf.setCreatedBy(u2);
        familleFrCf.setLegalDomain("DROIT_FAMILLE"); familleFrCf.setStatus("OPEN"); familleFrCf = caseFileRepository.save(familleFrCf);
        authOther = buildAuth("g-succfr-" + ts, "succfr-" + ts + "@ex.com");
    }

    @Test
    void POST_reserveRespectee_returns200() throws Exception {
        // Marié, 2 enfants, masse 400 000 €, libéralités 100 000 € → RESERVE_RESPECTEE
        Map<String, Object> body = nominal();
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RESERVE_RESPECTEE"))
                .andExpect(jsonPath("$.reserveGlobaleEur").value(200000))
                .andExpect(jsonPath("$.quotiteDisponibleEur").value(200000))
                .andExpect(jsonPath("$.depassementQuotiteEur").value(0))
                .andExpect(jsonPath("$.reserveGlobaleFraction").value("1/2"))
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.calculatedAt").exists())
                .andExpect(jsonPath("$.heritiers[0].code").value("CONJOINT_SURVIVANT"))
                .andExpect(jsonPath("$.dateDeces").value("2025-03-12"));
    }

    @Test
    void POST_quotiteDepassee_returns200() throws Exception {
        // Veuf, 1 enfant, masse 100 000, libéralités 60 000 → QUOTITE_DEPASSEE
        Map<String, Object> body = nominal();
        body.put("etatCivilDefunt", "VEUF");
        body.put("regimeMatrimonialDefunt", null);
        body.put("nombreEnfantsVivants", 1);
        body.put("masseSuccessoraleEur", 100000);
        body.put("libertesConsentiesEur", 60000);
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("QUOTITE_DEPASSEE"))
                .andExpect(jsonPath("$.depassementQuotiteEur").value(10000));
    }

    @Test
    void POST_devolutionEtablie_returns200() throws Exception {
        // Célibataire, parents vivants → DEVOLUTION_ETABLIE
        Map<String, Object> body = nominal();
        body.put("etatCivilDefunt", "CELIBATAIRE");
        body.put("regimeMatrimonialDefunt", null);
        body.put("nombreEnfantsVivants", 0);
        body.put("presenceParentsVivants", true);
        body.put("masseSuccessoraleEur", 100000);
        body.put("libertesConsentiesEur", 0);
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("DEVOLUTION_ETABLIE"))
                .andExpect(jsonPath("$.reserveGlobaleEur").value(0))
                .andExpect(jsonPath("$.quotiteDisponibleEur").value(100000));
    }

    @Test
    void POST_qualificationIncomplete_marieSansRegime_returns200() throws Exception {
        Map<String, Object> body = nominal();
        body.put("regimeMatrimonialDefunt", null);
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("QUALIFICATION_INCOMPLETE"));
    }

    @Test
    void POST_recalcul_ecraseResultatPrecedent() throws Exception {
        Map<String, Object> body1 = nominal();
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RESERVE_RESPECTEE"));

        Map<String, Object> body2 = nominal();
        body2.put("libertesConsentiesEur", 250000); // > quotité disponible 200 000
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("QUOTITE_DEPASSEE"));

        mockMvc.perform(get(url(familleBeCf.getId())).with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("QUOTITE_DEPASSEE"))
                .andExpect(jsonPath("$.depassementQuotiteEur").value(50000));
    }

    @Test
    void GET_apresPost_returnsSnapshot() throws Exception {
        Map<String, Object> body = nominal();
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
        mockMvc.perform(get(url(familleBeCf.getId())).with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateDeces").value("2025-03-12"))
                .andExpect(jsonPath("$.etatCivilDefunt").value("MARIE"))
                .andExpect(jsonPath("$.regimeMatrimonialDefunt").value("COMMUNAUTE_LEGALE"))
                .andExpect(jsonPath("$.nombreEnfantsVivants").value(2))
                .andExpect(jsonPath("$.masseSuccessoraleEur").value(400000))
                .andExpect(jsonPath("$.libertesConsentiesEur").value(100000))
                .andExpect(jsonPath("$.verdict").value("RESERVE_RESPECTEE"))
                .andExpect(jsonPath("$.country").value("BELGIQUE"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(url(familleBeCf.getId())).with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dossierInexistant_returns404() throws Exception {
        mockMvc.perform(post(url(UUID.randomUUID()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominal())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_autreWorkspace_returns403() throws Exception {
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominal())))
                .andExpect(status().isForbidden());
    }

    @Test
    void POST_dossierTravail_returns422() throws Exception {
        mockMvc.perform(post(url(travailBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominal())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void POST_workspaceFrance_returns422() throws Exception {
        mockMvc.perform(post(url(familleFrCf.getId()))
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominal())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void POST_dateDecesAbsente_returns400() throws Exception {
        Map<String, Object> body = nominal();
        body.remove("dateDeces");
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_masseNegative_returns400() throws Exception {
        Map<String, Object> body = nominal();
        body.put("masseSuccessoraleEur", -100);
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_commentaireTropLong_returns400() throws Exception {
        Map<String, Object> body = nominal();
        body.put("commentaire", "x".repeat(1001));
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_nonAuthentifie_returns401() throws Exception {
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominal())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void GET_nonAuthentifie_returns401() throws Exception {
        mockMvc.perform(get(url(familleBeCf.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void POST_isolationWorkspace_autreWorkspaceNePeutPasLireResultat() throws Exception {
        // 1. Le workspace BE crée son résultat
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominal())))
                .andExpect(status().isOk());
        // 2. Un membre du workspace FR ne peut pas le lire (403 — dossier d'un autre workspace)
        mockMvc.perform(get(url(familleBeCf.getId())).with(authentication(authOther)))
                .andExpect(status().isForbidden());
        // 3. Un membre du workspace FR ne peut pas non plus déclencher le calcul
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominal())))
                .andExpect(status().isForbidden());
    }

    private String url(UUID caseFileId) {
        return "/api/v1/case-files/" + caseFileId + "/succession-be-devolution-reserve";
    }

    /** Situation nominale (marié 2 enfants, masse 400 000 €, libéralités 100 000 €). */
    private Map<String, Object> nominal() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dateDeces", "2025-03-12");
        m.put("etatCivilDefunt", "MARIE");
        m.put("regimeMatrimonialDefunt", "COMMUNAUTE_LEGALE");
        m.put("nombreEnfantsVivants", 2);
        m.put("nombreEnfantsPredecedesAvecDescendants", 0);
        m.put("presenceParentsVivants", false);
        m.put("presenceFreresSoeursOuDescendants", false);
        m.put("masseSuccessoraleEur", 400000);
        m.put("libertesConsentiesEur", 100000);
        m.put("commentaire", null);
        return m;
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
