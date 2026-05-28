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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SF-219-23 : tests d'intégration de l'endpoint <i>refus d'aménagements
 * raisonnables handicap BE</i> (Loi 10/05/2007 art. 14 + art. 17 +
 * art. 28 + CCT n° 95 + Directive 2000/78/CE art. 5).
 *
 * <p>Couvre : POST BE 200 (verdicts CONFORME_AMENAGEMENT_ACCORDE /
 * DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE /
 * DISCRIMINATION_PRESUMEE_NON_MOTIVATION /
 * CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE /
 * REPRESAILLES_PRESUMEES_LICENCIEMENT /
 * FRAGILE_QUALIFICATION_HANDICAP_INCERTAINE / A_ANALYSER), POST FR
 * 404 (gate BE strict), POST caseFile autre workspace 404, POST
 * dossier non DROIT_DU_TRAVAIL 400, GET 404 sans POST, GET retour
 * persisté, validations Bean Validation 400.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class DiscriminationBeHandicapAmenagementControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/discrimination-be-handicap-amenagement";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authBe;
    private OAuth2AuthenticationToken authFr;
    private CaseFile beCaseFile;
    private CaseFile beFamilleCaseFile;
    private CaseFile frCaseFile;
    private CaseFile beOtherWorkspaceCaseFile;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // Workspace BE DROIT_DU_TRAVAIL → cible
        User uBe = save(new User(), u -> { u.setEmail("dhcp-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-dhcp-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE DHCP " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE DHCP " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE FAM " + ts, "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-dhcp-be-" + ts, "dhcp-be-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("dhcp-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-dhcp-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR DHCP " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR DHCP " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-dhcp-fr-" + ts, "dhcp-fr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> { u.setEmail("dhcp-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-dhcp-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 DHCP " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 DHCP " + ts, "DROIT_DU_TRAVAIL");
    }

    /** Body nominal — aménagement accordé. */
    private Map<String, Object> bodyAccorde() {
        Map<String, Object> m = new HashMap<>();
        m.put("statutHandicap", "RECONNU_OFFICIEL");
        m.put("dateDemandeAmenagement", "2025-03-01");
        m.put("typeAmenagementDemande", "Aménagement poste ergonomique + télétravail 2 j/sem");
        m.put("coutEstimeAmenagement", 3500.00);
        m.put("subsidesDemandes", true);
        m.put("effectifEntreprise", 120);
        m.put("chiffreAffairesAnnuel", 8000000.00);
        m.put("reponseEmployeur", "ACCORDE");
        m.put("motivationDetailleeFournie", true);
        m.put("avisSeppFavorable", true);
        m.put("chargeDisproportionneeInvoquee", false);
        m.put("devisExterneFourni", false);
        m.put("mesuresAlternativesProposees", false);
        m.put("sanctionSubie", "AUCUNE");
        m.put("dateSanction", null);
        m.put("salaireMensuelBrut", 3200.00);
        m.put("procedureUniaSaisie", false);
        return m;
    }

    @Test
    void POST_workspaceBe_amenagementAccorde_returns200AndPersists() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAccorde())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONFORME_AMENAGEMENT_ACCORDE"))
                .andExpect(jsonPath("$.handicapQualifie").value(true))
                .andExpect(jsonPath("$.demandeFormalisee").value(true))
                .andExpect(jsonPath("$.refusCaracterise").value(false))
                .andExpect(jsonPath("$.representaillesPresumees").value(false))
                .andExpect(jsonPath("$.indemniteForfaitaire6Mois").value(19200.00))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("Loi du 10/05/2007")));
    }

    @Test
    void POST_workspaceBe_licenciementApresDemande_returnsRepresailles() throws Exception {
        Map<String, Object> body = bodyAccorde();
        body.put("reponseEmployeur", "REFUSE_MOTIVE_DETAILLE");
        body.put("chargeDisproportionneeInvoquee", true);
        body.put("devisExterneFourni", true);
        body.put("sanctionSubie", "LICENCIEMENT");
        body.put("dateSanction", "2025-05-15");
        body.put("procedureUniaSaisie", true);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("REPRESAILLES_PRESUMEES_LICENCIEMENT"))
                .andExpect(jsonPath("$.representaillesPresumees").value(true));
    }

    @Test
    void POST_workspaceBe_refusNonMotive_returnsDiscriminationPresumee() throws Exception {
        Map<String, Object> body = bodyAccorde();
        body.put("reponseEmployeur", "REFUSE_NON_MOTIVE");
        body.put("motivationDetailleeFournie", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("DISCRIMINATION_PRESUMEE_NON_MOTIVATION"))
                .andExpect(jsonPath("$.refusCaracterise").value(true));
    }

    @Test
    void POST_workspaceBe_chargeDisproportionneeDemontreeTpe_returnsConforme() throws Exception {
        Map<String, Object> body = bodyAccorde();
        body.put("reponseEmployeur", "REFUSE_MOTIVE_DETAILLE");
        body.put("effectifEntreprise", 5);
        body.put("coutEstimeAmenagement", 85000.00);
        body.put("chiffreAffairesAnnuel", 500000.00);
        body.put("subsidesDemandes", true);
        body.put("devisExterneFourni", true);
        body.put("chargeDisproportionneeInvoquee", true);
        body.put("avisSeppFavorable", false);
        body.put("mesuresAlternativesProposees", true);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE"))
                .andExpect(jsonPath("$.chargeDisproportionneeDemontree").value(true));
    }

    @Test
    void POST_workspaceBe_refusGrandeEntrepriseSansSubsides_returnsDiscriminationInjustifiee() throws Exception {
        Map<String, Object> body = bodyAccorde();
        body.put("reponseEmployeur", "REFUSE_MOTIVE_DETAILLE");
        body.put("effectifEntreprise", 500);
        body.put("chiffreAffairesAnnuel", 50000000.00);
        body.put("subsidesDemandes", false);
        body.put("devisExterneFourni", false);
        body.put("chargeDisproportionneeInvoquee", true);
        body.put("mesuresAlternativesProposees", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE"))
                .andExpect(jsonPath("$.chargeDisproportionneeDemontree").value(false));
    }

    @Test
    void POST_workspaceBe_statutConteste_returnsFragile() throws Exception {
        Map<String, Object> body = bodyAccorde();
        body.put("statutHandicap", "CONTESTE");
        body.put("reponseEmployeur", "REFUSE_MOTIVE_DETAILLE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("FRAGILE_QUALIFICATION_HANDICAP_INCERTAINE"))
                .andExpect(jsonPath("$.handicapQualifie").value(false));
    }

    @Test
    void POST_workspaceBe_statutIndetermine_returnsAanalyser() throws Exception {
        Map<String, Object> body = bodyAccorde();
        body.put("statutHandicap", "INDETERMINE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_ANALYSER"))
                .andExpect(jsonPath("$.raison").value("CONFIGURATION_AMBIGUE"));
    }

    @Test
    void POST_workspaceBe_reponseEnAttente_returnsAanalyser() throws Exception {
        Map<String, Object> body = bodyAccorde();
        body.put("reponseEmployeur", "EN_ATTENTE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_ANALYSER"));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAccorde())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAccorde())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dossierPasDroitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(String.format(URL, beFamilleCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAccorde())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAccorde())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONFORME_AMENAGEMENT_ACCORDE"))
                .andExpect(jsonPath("$.statutHandicap").value("RECONNU_OFFICIEL"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_workspaceFr_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_statutHandicapManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyAccorde();
        body.remove("statutHandicap");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_effectifManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyAccorde();
        body.remove("effectifEntreprise");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_effectifNegatif_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyAccorde();
        body.put("effectifEntreprise", -1);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_reponseEmployeurManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyAccorde();
        body.remove("reponseEmployeur");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_sanctionSubieManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyAccorde();
        body.remove("sanctionSubie");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_coutNegatif_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyAccorde();
        body.put("coutEstimeAmenagement", -100);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_salaireNegatif_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyAccorde();
        body.put("salaireMensuelBrut", -100);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ---- helpers (alignés sur EgaliteFemmesHommesBeControllerIT) ----

    private User save(User u, java.util.function.Consumer<User> init) {
        init.accept(u);
        return userRepository.save(u);
    }

    private void saveAuth(User user, String providerUserId) {
        AuthAccount a = new AuthAccount();
        a.setUser(user); a.setProvider("GOOGLE"); a.setProviderUserId(providerUserId);
        authAccountRepository.save(a);
    }

    private Workspace saveWs(User owner, String name, String legalDomain, String country) {
        Workspace ws = new Workspace();
        ws.setName(name); ws.setSlug(name.toLowerCase().replace(' ', '-'));
        ws.setOwner(owner); ws.setLegalDomain(legalDomain); ws.setCountry(country);
        ws.setPlanCode("STARTER"); ws.setStatus("ACTIVE");
        return workspaceRepository.save(ws);
    }

    private void saveMember(User user, Workspace ws) {
        WorkspaceMember m = new WorkspaceMember();
        m.setWorkspace(ws); m.setUser(user); m.setMemberRole("OWNER"); m.setPrimary(true);
        workspaceMemberRepository.save(m);
    }

    private CaseFile saveCf(User user, Workspace ws, String title, String domain) {
        CaseFile cf = new CaseFile();
        cf.setTitle(title); cf.setWorkspace(ws); cf.setCreatedBy(user);
        cf.setLegalDomain(domain); cf.setStatus("OPEN");
        return caseFileRepository.save(cf);
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
