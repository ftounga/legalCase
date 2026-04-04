package fr.ailegalcase.referential;

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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class ReferentialControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;

    private OAuth2AuthenticationToken auth;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User user = new User();
        user.setEmail("ref-it-" + ts + "@example.com");
        user.setStatus("ACTIVE");
        user = userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-ref-it-" + ts);
        authAccountRepository.save(account);

        Workspace ws = new Workspace();
        ws.setName("REF IT WS");
        ws.setSlug("ref-it-ws-" + ts);
        ws.setOwner(user);
        ws.setLegalDomain("DROIT_DU_TRAVAIL");
        ws.setPlanCode("STARTER");
        ws.setStatus("ACTIVE");
        ws = workspaceRepository.save(ws);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(ws);
        member.setUser(user);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        auth = buildGoogleAuth("google-ref-it-" + ts, "ref-it-" + ts + "@example.com");
    }

    @Test
    void GET_referentials_DROIT_DU_TRAVAIL_retourne_200_avec_types_litiges() throws Exception {
        mockMvc.perform(get("/api/v1/referentials")
                        .param("domain", "DROIT_DU_TRAVAIL")
                        .with(authentication(auth))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.domain").value("DROIT_DU_TRAVAIL"))
                .andExpect(jsonPath("$.sections.LITIGATION_TYPE", hasSize(greaterThanOrEqualTo(7))))
                .andExpect(jsonPath("$.sections.BAREME_MACRON", hasSize(greaterThanOrEqualTo(3))));
    }

    @Test
    void GET_referentials_DROIT_IMMIGRATION_contient_REGULARISATION_EXCEPTIONNELLE() throws Exception {
        mockMvc.perform(get("/api/v1/referentials")
                        .param("domain", "DROIT_IMMIGRATION")
                        .with(authentication(auth))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.domain").value("DROIT_IMMIGRATION"))
                .andExpect(jsonPath("$.sections.IMMIGRATION_JALONS[*].key",
                        hasItem("REGULARISATION_EXCEPTIONNELLE")))
                .andExpect(jsonPath("$.sections.IMMIGRATION_PIECES[*].key",
                        hasItem("REGULARISATION_EXCEPTIONNELLE")));
    }

    @Test
    void GET_referentials_DROIT_FAMILLE_retourne_taux_pension_et_coefficients() throws Exception {
        mockMvc.perform(get("/api/v1/referentials")
                        .param("domain", "DROIT_FAMILLE")
                        .with(authentication(auth))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.PENSION_TAUX", hasSize(2)))
                .andExpect(jsonPath("$.sections.PRESTATION_COEFF", hasSize(2)));
    }

    @Test
    void GET_referentials_sans_domain_retourne_400() throws Exception {
        mockMvc.perform(get("/api/v1/referentials")
                        .with(authentication(auth))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_referentials_domain_invalide_retourne_400() throws Exception {
        mockMvc.perform(get("/api/v1/referentials")
                        .param("domain", "DROIT_INCONNU")
                        .with(authentication(auth))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_referentials_non_authentifie_retourne_401() throws Exception {
        mockMvc.perform(get("/api/v1/referentials")
                        .param("domain", "DROIT_DU_TRAVAIL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
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
