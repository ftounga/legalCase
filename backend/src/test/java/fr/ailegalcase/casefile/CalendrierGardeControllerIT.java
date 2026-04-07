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
import java.time.Instant; import java.util.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.security.oauth2.client.registration.google.client-id=test","spring.security.oauth2.client.registration.google.client-secret=test","anthropic.api-key=test"})
@AutoConfigureMockMvc
class CalendrierGardeControllerIT {
    @Autowired MockMvc mockMvc; @Autowired UserRepository userRepository; @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository; @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository; @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;
    private OAuth2AuthenticationToken auth, authOther; private CaseFile famCf, travCf;

    @BeforeEach void setUp() {
        long ts = System.nanoTime();
        User u1 = new User(); u1.setEmail("cg-"+ts+"@ex.com"); u1.setStatus("ACTIVE"); u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE"); a1.setProviderUserId("g-cg-"+ts); authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("CG"+ts); ws1.setSlug("ws-cg-"+ts); ws1.setOwner(u1); ws1.setLegalDomain("DROIT_FAMILLE"); ws1.setCountry("FRANCE"); ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE"); ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember(); m1.setWorkspace(ws1); m1.setUser(u1); m1.setMemberRole("OWNER"); m1.setPrimary(true); workspaceMemberRepository.save(m1);
        famCf = new CaseFile(); famCf.setTitle("CG"+ts); famCf.setWorkspace(ws1); famCf.setCreatedBy(u1); famCf.setLegalDomain("DROIT_FAMILLE"); famCf.setStatus("OPEN"); famCf = caseFileRepository.save(famCf);
        auth = buildAuth("g-cg-"+ts, "cg-"+ts+"@ex.com");

        User u2 = new User(); u2.setEmail("cg-o-"+ts+"@ex.com"); u2.setStatus("ACTIVE"); u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE"); a2.setProviderUserId("g-cg-o-"+ts); authAccountRepository.save(a2);
        Workspace ws2 = new Workspace(); ws2.setName("CO"+ts); ws2.setSlug("ws-cg-o-"+ts); ws2.setOwner(u2); ws2.setLegalDomain("DROIT_DU_TRAVAIL"); ws2.setCountry("FRANCE"); ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE"); ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember(); m2.setWorkspace(ws2); m2.setUser(u2); m2.setMemberRole("OWNER"); m2.setPrimary(true); workspaceMemberRepository.save(m2);
        travCf = new CaseFile(); travCf.setTitle("CO"+ts); travCf.setWorkspace(ws2); travCf.setCreatedBy(u2); travCf.setLegalDomain("DROIT_DU_TRAVAIL"); travCf.setStatus("OPEN"); travCf = caseFileRepository.save(travCf);
        authOther = buildAuth("g-cg-o-"+ts, "cg-o-"+ts+"@ex.com");
    }

    @Test void POST_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/"+famCf.getId()+"/calendrier-garde").with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("gardeCode","ALTERNEE_FR","parentANom","Marie","parentBNom","Pierre"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.gardeCode").value("ALTERNEE_FR")).andExpect(jsonPath("$.joursParAnParentA").value(182));
    }
    @Test void POST_belgique_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/"+famCf.getId()+"/calendrier-garde").with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("gardeCode","ALTERNEE_BE","parentANom","S","parentBNom","M"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.country").value("BELGIQUE"));
    }
    @Test void POST_unknownCode_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/"+famCf.getId()+"/calendrier-garde").with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("gardeCode","X","parentANom","A","parentBNom","B"))))
                .andExpect(status().isBadRequest());
    }
    @Test void POST_wrongDomain_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/"+travCf.getId()+"/calendrier-garde").with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("gardeCode","ALTERNEE_FR","parentANom","A","parentBNom","B"))))
                .andExpect(status().isBadRequest());
    }
    @Test void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/"+famCf.getId()+"/calendrier-garde").with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("gardeCode","ALTERNEE_FR","parentANom","A","parentBNom","B"))))
                .andExpect(status().isNotFound());
    }
    @Test void GET_afterPost_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/"+famCf.getId()+"/calendrier-garde").with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("gardeCode","DVH_CLASSIQUE_FR","parentANom","A","parentBNom","B")))).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/case-files/"+famCf.getId()+"/calendrier-garde").with(authentication(auth)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.gardeCode").value("DVH_CLASSIQUE_FR"));
    }
    @Test void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/"+famCf.getId()+"/calendrier-garde").with(authentication(auth))).andExpect(status().isNotFound());
    }
    @Test void POST_upsert() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/"+famCf.getId()+"/calendrier-garde").with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("gardeCode","ALTERNEE_FR","parentANom","A","parentBNom","B")))).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/case-files/"+famCf.getId()+"/calendrier-garde").with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("gardeCode","SECONDAIRE_BE","parentANom","X","parentBNom","Y"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.gardeCode").value("SECONDAIRE_BE"));
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String,Object> c = Map.of("sub",sub,"email",email,"iss","https://accounts.google.com");
        OidcIdToken t = new OidcIdToken("token",Instant.now(),Instant.now().plusSeconds(3600),c);
        DefaultOidcUser u = new DefaultOidcUser(List.of(new OidcUserAuthority(t)),t,"sub");
        return new OAuth2AuthenticationToken(u,u.getAuthorities(),"google");
    }
}
