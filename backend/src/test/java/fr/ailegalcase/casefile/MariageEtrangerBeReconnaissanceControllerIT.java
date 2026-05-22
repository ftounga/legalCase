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
import java.time.LocalDate;
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
 * SF-217-16 : tests d'intégration des endpoints de reconnaissance d'un
 * mariage / divorce étranger en Belgique — incluant le talaq (BELGIQUE).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class MariageEtrangerBeReconnaissanceControllerIT {

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
    /** Dossier BE / droit de la famille — cas nominal. */
    private CaseFile familleBeCf;
    /** Dossier BE / droit du travail — domaine non famille (422). */
    private CaseFile travailBeCf;
    /** Dossier FR / droit de la famille — pays non Belgique (422). */
    private CaseFile familleFrCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // Workspace A — BELGIQUE / droit de la famille
        User u1 = new User(); u1.setEmail("mer-be-" + ts + "@ex.com"); u1.setStatus("ACTIVE"); u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE"); a1.setProviderUserId("g-mer-be-" + ts); authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("MER BE " + ts); ws1.setSlug("ws-mer-be-" + ts); ws1.setOwner(u1);
        ws1.setLegalDomain("DROIT_FAMILLE"); ws1.setCountry("BELGIQUE"); ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE"); ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember(); m1.setWorkspace(ws1); m1.setUser(u1); m1.setMemberRole("OWNER"); m1.setPrimary(true); workspaceMemberRepository.save(m1);
        familleBeCf = new CaseFile(); familleBeCf.setTitle("MER famille BE " + ts); familleBeCf.setWorkspace(ws1); familleBeCf.setCreatedBy(u1);
        familleBeCf.setLegalDomain("DROIT_FAMILLE"); familleBeCf.setStatus("OPEN"); familleBeCf = caseFileRepository.save(familleBeCf);
        travailBeCf = new CaseFile(); travailBeCf.setTitle("MER travail BE " + ts); travailBeCf.setWorkspace(ws1); travailBeCf.setCreatedBy(u1);
        travailBeCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailBeCf.setStatus("OPEN"); travailBeCf = caseFileRepository.save(travailBeCf);
        authBe = buildAuth("g-mer-be-" + ts, "mer-be-" + ts + "@ex.com");

        // Workspace B — FRANCE / droit de la famille
        User u2 = new User(); u2.setEmail("mer-fr-" + ts + "@ex.com"); u2.setStatus("ACTIVE"); u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE"); a2.setProviderUserId("g-mer-fr-" + ts); authAccountRepository.save(a2);
        Workspace ws2 = new Workspace(); ws2.setName("MER FR " + ts); ws2.setSlug("ws-mer-fr-" + ts); ws2.setOwner(u2);
        ws2.setLegalDomain("DROIT_FAMILLE"); ws2.setCountry("FRANCE"); ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE"); ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember(); m2.setWorkspace(ws2); m2.setUser(u2); m2.setMemberRole("OWNER"); m2.setPrimary(true); workspaceMemberRepository.save(m2);
        familleFrCf = new CaseFile(); familleFrCf.setTitle("MER famille FR " + ts); familleFrCf.setWorkspace(ws2); familleFrCf.setCreatedBy(u2);
        familleFrCf.setLegalDomain("DROIT_FAMILLE"); familleFrCf.setStatus("OPEN"); familleFrCf = caseFileRepository.save(familleFrCf);
        authOther = buildAuth("g-mer-fr-" + ts, "mer-fr-" + ts + "@ex.com");
    }

    @Test
    void POST_mariageCivilFondEtFormeOk_returns200_reconnaissanceDePleinDroit() throws Exception {
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(baseBodyMariageCivilMa())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RECONNAISSANCE_DE_PLEIN_DROIT"))
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.calculatedAt").exists())
                .andExpect(jsonPath("$.basesJuridiques.length()").isNotEmpty())
                .andExpect(jsonPath("$.actesAProduire.length()").isNotEmpty());
    }

    @Test
    void POST_polygamie_returns200_refusOrdrePublic() throws Exception {
        Map<String, Object> body = baseBodyMariageCivilMa();
        body.put("natureActe", "MARIAGE_POLYGAME");
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC"))
                .andExpect(jsonPath("$.motifsRefus[?(@.code == 'POLYGAMIE_ORDRE_PUBLIC')]").isNotEmpty());
    }

    @Test
    void POST_mariageReligieuxNonCivil_returns200_refusOrdrePublic() throws Exception {
        Map<String, Object> body = baseBodyMariageCivilMa();
        body.put("natureActe", "MARIAGE_RELIGIEUX_NON_CIVIL");
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC"))
                .andExpect(jsonPath("$.motifsRefus[?(@.code == 'MARIAGE_RELIGIEUX_NON_CIVIL')]").isNotEmpty());
    }

    @Test
    void POST_talaqTousFavorables_returns200_reconnaissancePossibleSousConditions() throws Exception {
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(baseBodyTalaqFavorable())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS"));
    }

    @Test
    void POST_talaqConsentementEpouseAbsent_returns200_refusOrdrePublic() throws Exception {
        Map<String, Object> body = baseBodyTalaqFavorable();
        body.put("consentementEpouse", false);
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC"))
                .andExpect(jsonPath("$.motifsRefus[?(@.code == 'TALAQ_CONSENTEMENT_EPOUSE_ABSENT')]").isNotEmpty());
    }

    @Test
    void POST_talaqSansBooleansTalaq_returns400() throws Exception {
        Map<String, Object> body = baseBodyMariageCivilMa();
        body.put("natureActe", "TALAQ_REPUDIATION");
        // Pas de booleans talaq fournis → 400.
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_divorceJudiciaireFR_returns200_reconnaissanceDePleinDroit() throws Exception {
        Map<String, Object> body = baseBodyMariageCivilMa();
        body.put("natureActe", "DIVORCE_JUDICIAIRE_ETRANGER");
        body.put("paysOrigine", "FR");
        body.put("nationaliteAuMoinsUnePartie", "UE");
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RECONNAISSANCE_DE_PLEIN_DROIT"));
    }

    @Test
    void POST_divorceJudiciaireMA_returns200_exequaturRequis() throws Exception {
        Map<String, Object> body = baseBodyMariageCivilMa();
        body.put("natureActe", "DIVORCE_JUDICIAIRE_ETRANGER");
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("EXEQUATUR_REQUIS"));
    }

    @Test
    void POST_mariageCivilFondKo_returns200_refusOrdrePublic() throws Exception {
        Map<String, Object> body = baseBodyMariageCivilMa();
        body.put("conformiteDroitFondPersonnel", false);
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC"))
                .andExpect(jsonPath("$.motifsRefus[?(@.code == 'FOND_DROIT_PERSONNEL_NON_CONFORME')]").isNotEmpty());
    }

    @Test
    void POST_mariageCivilFormeKo_returns200_reconnaissancePossibleSousConditions() throws Exception {
        Map<String, Object> body = baseBodyMariageCivilMa();
        body.put("conformiteFormeLocusRegitActum", false);
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS"))
                .andExpect(jsonPath("$.motifsReserve[?(@.code == 'FORME_LOCUS_REGIT_ACTUM_NON_CONFORME')]").isNotEmpty());
    }

    @Test
    void POST_recalcul_ecraseResultatPrecedent() throws Exception {
        // 1er calcul : mariage civil OK
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(baseBodyMariageCivilMa())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RECONNAISSANCE_DE_PLEIN_DROIT"));

        // Recalcul : polygamie
        Map<String, Object> body2 = baseBodyMariageCivilMa();
        body2.put("natureActe", "MARIAGE_POLYGAME");
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC"));

        mockMvc.perform(get(url(familleBeCf.getId())).with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC"));
    }

    @Test
    void GET_apresPost_returnsSnapshotInputsInclus() throws Exception {
        Map<String, Object> body = baseBodyMariageCivilMa();
        body.put("commentaire", "Couple binational, vérifier convention bilatérale BE-Maroc");
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
        mockMvc.perform(get(url(familleBeCf.getId())).with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.natureActe").value("MARIAGE_CIVIL_ETRANGER"))
                .andExpect(jsonPath("$.paysOrigine").value("MA"))
                .andExpect(jsonPath("$.dateActe").exists())
                .andExpect(jsonPath("$.commentaire").value(
                        "Couple binational, vérifier convention bilatérale BE-Maroc"))
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
                        .content(objectMapper.writeValueAsString(baseBodyMariageCivilMa())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_autreWorkspace_returns403() throws Exception {
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(baseBodyMariageCivilMa())))
                .andExpect(status().isForbidden());
    }

    @Test
    void GET_autreWorkspace_returns403() throws Exception {
        mockMvc.perform(get(url(familleBeCf.getId())).with(authentication(authOther)))
                .andExpect(status().isForbidden());
    }

    @Test
    void POST_dossierNonFamille_returns422() throws Exception {
        mockMvc.perform(post(url(travailBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(baseBodyMariageCivilMa())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void POST_paysNonBelgique_returns422() throws Exception {
        mockMvc.perform(post(url(familleFrCf.getId()))
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(baseBodyMariageCivilMa())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void POST_paysOrigineNonIso2_returns400() throws Exception {
        Map<String, Object> body = baseBodyMariageCivilMa();
        body.put("paysOrigine", "FRA"); // 3 lettres → 400
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateActeFuture_returns400() throws Exception {
        Map<String, Object> body = baseBodyMariageCivilMa();
        body.put("dateActe", LocalDate.now().plusDays(2).toString());
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_commentaireTropLong_returns400() throws Exception {
        Map<String, Object> body = baseBodyMariageCivilMa();
        body.put("commentaire", "x".repeat(1001));
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_corpsAbsent_returns400() throws Exception {
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_nonAuthentifie_returns401() throws Exception {
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(baseBodyMariageCivilMa())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void GET_nonAuthentifie_returns401() throws Exception {
        mockMvc.perform(get(url(familleBeCf.getId())))
                .andExpect(status().isUnauthorized());
    }

    private String url(UUID caseFileId) {
        return "/api/v1/case-files/" + caseFileId + "/mariage-etranger-be-reconnaissance";
    }

    /** Body de référence : mariage civil marocain, fond + forme OK. */
    private Map<String, Object> baseBodyMariageCivilMa() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("natureActe", "MARIAGE_CIVIL_ETRANGER");
        m.put("paysOrigine", "MA");
        m.put("dateActe", "2018-06-12");
        m.put("residenceHabituelleAuMoinsUnePartie", "BELGIQUE");
        m.put("nationaliteAuMoinsUnePartie", "HORS_UE");
        m.put("conformiteDroitFondPersonnel", true);
        m.put("conformiteFormeLocusRegitActum", true);
        m.put("conventionBilateraleApplicable", false);
        return m;
    }

    /** Body de référence : talaq marocain avec tous les booleans favorables. */
    private Map<String, Object> baseBodyTalaqFavorable() {
        Map<String, Object> m = baseBodyMariageCivilMa();
        m.put("natureActe", "TALAQ_REPUDIATION");
        m.put("dateActe", "2024-08-15");
        m.put("consentementEpouse", true);
        m.put("epousePresente", true);
        m.put("procedureContradictoire", true);
        m.put("decisionEcriteOfficielle", true);
        return m;
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
