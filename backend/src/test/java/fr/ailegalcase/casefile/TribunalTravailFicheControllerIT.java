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
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class TribunalTravailFicheControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken belgianAuth;
    private OAuth2AuthenticationToken frenchAuth;
    private CaseFile belgianCaseFile;
    private CaseFile frenchCaseFile;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // Belgian workspace + case file
        User userBe = new User();
        userBe.setEmail("tt-be-" + ts + "@example.com");
        userBe.setStatus("ACTIVE");
        userBe = userRepository.save(userBe);

        AuthAccount accBe = new AuthAccount();
        accBe.setUser(userBe);
        accBe.setProvider("GOOGLE");
        accBe.setProviderUserId("google-tt-be-" + ts);
        authAccountRepository.save(accBe);

        Workspace wsBe = new Workspace();
        wsBe.setName("Cabinet BE " + ts);
        wsBe.setSlug("ws-be-" + ts);
        wsBe.setOwner(userBe);
        wsBe.setLegalDomain("DROIT_DU_TRAVAIL");
        wsBe.setCountry("BELGIQUE");
        wsBe.setPlanCode("STARTER");
        wsBe.setStatus("ACTIVE");
        wsBe = workspaceRepository.save(wsBe);

        WorkspaceMember memberBe = new WorkspaceMember();
        memberBe.setWorkspace(wsBe);
        memberBe.setUser(userBe);
        memberBe.setMemberRole("OWNER");
        memberBe.setPrimary(true);
        workspaceMemberRepository.save(memberBe);

        belgianCaseFile = new CaseFile();
        belgianCaseFile.setTitle("Dossier Dupont BE " + ts);
        belgianCaseFile.setWorkspace(wsBe);
        belgianCaseFile.setCreatedBy(userBe);
        belgianCaseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        belgianCaseFile.setStatus("OPEN");
        belgianCaseFile = caseFileRepository.save(belgianCaseFile);

        belgianAuth = buildGoogleAuth("google-tt-be-" + ts, "tt-be-" + ts + "@example.com");

        // French workspace + case file
        User userFr = new User();
        userFr.setEmail("tt-fr-" + ts + "@example.com");
        userFr.setStatus("ACTIVE");
        userFr = userRepository.save(userFr);

        AuthAccount accFr = new AuthAccount();
        accFr.setUser(userFr);
        accFr.setProvider("GOOGLE");
        accFr.setProviderUserId("google-tt-fr-" + ts);
        authAccountRepository.save(accFr);

        Workspace wsFr = new Workspace();
        wsFr.setName("Cabinet FR " + ts);
        wsFr.setSlug("ws-fr-" + ts);
        wsFr.setOwner(userFr);
        wsFr.setLegalDomain("DROIT_DU_TRAVAIL");
        wsFr.setCountry("FRANCE");
        wsFr.setPlanCode("STARTER");
        wsFr.setStatus("ACTIVE");
        wsFr = workspaceRepository.save(wsFr);

        WorkspaceMember memberFr = new WorkspaceMember();
        memberFr.setWorkspace(wsFr);
        memberFr.setUser(userFr);
        memberFr.setMemberRole("OWNER");
        memberFr.setPrimary(true);
        workspaceMemberRepository.save(memberFr);

        frenchCaseFile = new CaseFile();
        frenchCaseFile.setTitle("Dossier Martin FR " + ts);
        frenchCaseFile.setWorkspace(wsFr);
        frenchCaseFile.setCreatedBy(userFr);
        frenchCaseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        frenchCaseFile.setStatus("OPEN");
        frenchCaseFile = caseFileRepository.save(frenchCaseFile);

        frenchAuth = buildGoogleAuth("google-tt-fr-" + ts, "tt-fr-" + ts + "@example.com");
    }

    @Test
    void GET_fiche_belge_retourne_200() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + belgianCaseFile.getId() + "/tribunal-travail-fiche")
                        .with(authentication(belgianAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requerant").exists())
                .andExpect(jsonPath("$.procedureInfo").exists())
                .andExpect(jsonPath("$.contratInfo").exists());
    }

    @Test
    void PUT_upsert_fiche_belge_retourne_200() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "requerant", Map.of("nom", "Dupont", "prenom", "Jean", "domicile", "Bruxelles", "registreNational", "123456"),
                "defendeur", Map.of("nom", "ACME SA", "siegeSocial", "Anvers", "numeroBce", "0123.456.789"),
                "procedureInfo", Map.of("tribunal", "Bruxelles", "division", "Francophone", "langue", "FR"),
                "contratInfo", Map.of("typeContrat", "EMPLOYE", "dateDebut", "2020-01-15", "dateFin", "2024-06-30", "motifRupture", "Licenciement"),
                "demandes", List.of(Map.of("label", "Indemnité compensatoire de préavis", "montant", 15576.92)),
                "exposeDesMoyens", "Le licenciement est manifestement déraisonnable au sens de la CCT 109."
        ));

        mockMvc.perform(put("/api/v1/case-files/" + belgianCaseFile.getId() + "/tribunal-travail-fiche")
                        .with(authentication(belgianAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.requerant.nom").value("Dupont"))
                .andExpect(jsonPath("$.procedureInfo.langue").value("FR"))
                .andExpect(jsonPath("$.contratInfo.typeContrat").value("EMPLOYE"));
    }

    @Test
    void GET_fiche_dossier_francais_retourne_400() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + frenchCaseFile.getId() + "/tribunal-travail-fiche")
                        .with(authentication(frenchAuth)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_fiche_dossier_autre_workspace_retourne_404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + belgianCaseFile.getId() + "/tribunal-travail-fiche")
                        .with(authentication(frenchAuth)))
                .andExpect(status().isNotFound());
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
