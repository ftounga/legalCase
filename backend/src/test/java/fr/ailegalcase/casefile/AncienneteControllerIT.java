package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.auth.*; import fr.ailegalcase.workspace.*;
import org.junit.jupiter.api.BeforeEach; import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.*; import org.springframework.security.oauth2.core.oidc.user.*;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant; import java.time.LocalDate; import java.util.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.security.oauth2.client.registration.google.client-id=test","spring.security.oauth2.client.registration.google.client-secret=test","anthropic.api-key=test"})
@AutoConfigureMockMvc
class AncienneteControllerIT {
    @Autowired MockMvc mockMvc; @Autowired UserRepository userRepository; @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository; @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository; @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;
    private OAuth2AuthenticationToken auth, authOther; private CaseFile travailCf, immCf;

    @BeforeEach void setUp() {
        long ts = System.nanoTime();
        User u1 = new User(); u1.setEmail("anc-"+ts+"@ex.com"); u1.setStatus("ACTIVE"); u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE"); a1.setProviderUserId("g-anc-"+ts); authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("AN"+ts); ws1.setSlug("ws-anc-"+ts); ws1.setOwner(u1); ws1.setLegalDomain("DROIT_DU_TRAVAIL"); ws1.setCountry("FRANCE"); ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE"); ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember(); m1.setWorkspace(ws1); m1.setUser(u1); m1.setMemberRole("OWNER"); m1.setPrimary(true); workspaceMemberRepository.save(m1);
        travailCf = new CaseFile(); travailCf.setTitle("AN"+ts); travailCf.setWorkspace(ws1); travailCf.setCreatedBy(u1); travailCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailCf.setStatus("OPEN"); travailCf = caseFileRepository.save(travailCf);
        auth = bAuth("g-anc-"+ts, "anc-"+ts+"@ex.com");

        User u2 = new User(); u2.setEmail("anc-o-"+ts+"@ex.com"); u2.setStatus("ACTIVE"); u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE"); a2.setProviderUserId("g-anc-o-"+ts); authAccountRepository.save(a2);
        Workspace ws2 = new Workspace(); ws2.setName("AO"+ts); ws2.setSlug("ws-anc-o-"+ts); ws2.setOwner(u2); ws2.setLegalDomain("DROIT_IMMIGRATION"); ws2.setCountry("FRANCE"); ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE"); ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember(); m2.setWorkspace(ws2); m2.setUser(u2); m2.setMemberRole("OWNER"); m2.setPrimary(true); workspaceMemberRepository.save(m2);
        immCf = new CaseFile(); immCf.setTitle("AO"+ts); immCf.setWorkspace(ws2); immCf.setCreatedBy(u2); immCf.setLegalDomain("DROIT_IMMIGRATION"); immCf.setStatus("OPEN"); immCf = caseFileRepository.save(immCf);
        authOther = bAuth("g-anc-o-"+ts, "anc-o-"+ts+"@ex.com");
    }

    @Test void POST_france_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/"+travailCf.getId()+"/anciennete").with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("conventionCode","METALLURGIE","dateEntree", LocalDate.now().minusYears(10).toString(),"salaireBase",3000,"congesContrat",25,"primeContrat",5))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conventionCode").value("METALLURGIE")).andExpect(jsonPath("$.congesTotalJours").isNumber());
    }
    @Test void POST_belgique_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/"+travailCf.getId()+"/anciennete").with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("conventionCode","CP200","dateEntree",LocalDate.now().minusYears(5).toString(),"salaireBase",2500,"congesContrat",20,"primeContrat",0))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.country").value("BELGIQUE"));
    }
    @Test void POST_unknownConvention_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/"+travailCf.getId()+"/anciennete").with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("conventionCode","INCONNU","dateEntree","2020-01-01","salaireBase",3000,"congesContrat",25,"primeContrat",0))))
                .andExpect(status().isBadRequest());
    }
    @Test void POST_wrongDomain_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/"+immCf.getId()+"/anciennete").with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("conventionCode","METALLURGIE","dateEntree","2020-01-01","salaireBase",3000,"congesContrat",25,"primeContrat",0))))
                .andExpect(status().isBadRequest());
    }
    @Test void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/"+travailCf.getId()+"/anciennete").with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("conventionCode","METALLURGIE","dateEntree","2020-01-01","salaireBase",3000,"congesContrat",25,"primeContrat",0))))
                .andExpect(status().isNotFound());
    }
    @Test void GET_afterPost_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/"+travailCf.getId()+"/anciennete").with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("conventionCode","SYNTEC","dateEntree",LocalDate.now().minusYears(7).toString(),"salaireBase",4000,"congesContrat",25,"primeContrat",3))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/case-files/"+travailCf.getId()+"/anciennete").with(authentication(auth)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conventionCode").value("SYNTEC"));
    }
    @Test void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/"+travailCf.getId()+"/anciennete").with(authentication(auth))).andExpect(status().isNotFound());
    }
    @Test void POST_upsert_replaces() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/"+travailCf.getId()+"/anciennete").with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("conventionCode","METALLURGIE","dateEntree","2020-01-01","salaireBase",3000,"congesContrat",25,"primeContrat",0)))).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/case-files/"+travailCf.getId()+"/anciennete").with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("conventionCode","CP124","dateEntree","2018-06-01","salaireBase",2800,"congesContrat",20,"primeContrat",2))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conventionCode").value("CP124"));
    }

    private OAuth2AuthenticationToken bAuth(String sub, String email) {
        Map<String,Object> c = Map.of("sub",sub,"email",email,"iss","https://accounts.google.com");
        OidcIdToken t = new OidcIdToken("token",Instant.now(),Instant.now().plusSeconds(3600),c);
        DefaultOidcUser u = new DefaultOidcUser(List.of(new OidcUserAuthority(t)),t,"sub");
        return new OAuth2AuthenticationToken(u,u.getAuthorities(),"google");
    }
}
