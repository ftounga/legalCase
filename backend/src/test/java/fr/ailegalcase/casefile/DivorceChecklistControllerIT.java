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
class DivorceChecklistControllerIT {
    @Autowired MockMvc mockMvc; @Autowired UserRepository userRepository; @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository; @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository; @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;
    private OAuth2AuthenticationToken auth, authOther; private CaseFile famCf, travCf;

    @BeforeEach void setUp() {
        long ts = System.nanoTime();
        User u1 = new User(); u1.setEmail("dc-"+ts+"@ex.com"); u1.setStatus("ACTIVE"); u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE"); a1.setProviderUserId("g-dc-"+ts); authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("DC"+ts); ws1.setSlug("ws-dc-"+ts); ws1.setOwner(u1); ws1.setLegalDomain("DROIT_FAMILLE"); ws1.setCountry("FRANCE"); ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE"); ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember(); m1.setWorkspace(ws1); m1.setUser(u1); m1.setMemberRole("OWNER"); m1.setPrimary(true); workspaceMemberRepository.save(m1);
        famCf = new CaseFile(); famCf.setTitle("DC"+ts); famCf.setWorkspace(ws1); famCf.setCreatedBy(u1); famCf.setLegalDomain("DROIT_FAMILLE"); famCf.setStatus("OPEN"); famCf = caseFileRepository.save(famCf);
        auth = bAuth("g-dc-"+ts, "dc-"+ts+"@ex.com");
        User u2 = new User(); u2.setEmail("dc-o-"+ts+"@ex.com"); u2.setStatus("ACTIVE"); u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE"); a2.setProviderUserId("g-dc-o-"+ts); authAccountRepository.save(a2);
        Workspace ws2 = new Workspace(); ws2.setName("DO"+ts); ws2.setSlug("ws-dc-o-"+ts); ws2.setOwner(u2); ws2.setLegalDomain("DROIT_DU_TRAVAIL"); ws2.setCountry("FRANCE"); ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE"); ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember(); m2.setWorkspace(ws2); m2.setUser(u2); m2.setMemberRole("OWNER"); m2.setPrimary(true); workspaceMemberRepository.save(m2);
        travCf = new CaseFile(); travCf.setTitle("DO"+ts); travCf.setWorkspace(ws2); travCf.setCreatedBy(u2); travCf.setLegalDomain("DROIT_DU_TRAVAIL"); travCf.setStatus("OPEN"); travCf = caseFileRepository.save(travCf);
        authOther = bAuth("g-dc-o-"+ts, "dc-o-"+ts+"@ex.com");
    }

    @Test void POST_france_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/"+famCf.getId()+"/divorce-checklist").with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("country","FRANCE","etapeStatuts",Map.of("FR_CHOIX_AVOCATS","FAIT"),"pieceStatuts",Map.of("FR_ACTE_MARIAGE","PRESENTE")))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.etapesCompletees").value(1)).andExpect(jsonPath("$.piecesPresentes").value(1));
    }
    @Test void POST_belgique_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/"+famCf.getId()+"/divorce-checklist").with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("country","BELGIQUE","etapeStatuts",Map.of(),"pieceStatuts",Map.of()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.country").value("BELGIQUE"));
    }
    @Test void POST_invalidCountry_400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/"+famCf.getId()+"/divorce-checklist").with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("country","X","etapeStatuts",Map.of(),"pieceStatuts",Map.of())))).andExpect(status().isBadRequest());
    }
    @Test void POST_wrongDomain_400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/"+travCf.getId()+"/divorce-checklist").with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("country","FRANCE","etapeStatuts",Map.of(),"pieceStatuts",Map.of())))).andExpect(status().isBadRequest());
    }
    @Test void POST_otherWs_404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/"+famCf.getId()+"/divorce-checklist").with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("country","FRANCE","etapeStatuts",Map.of(),"pieceStatuts",Map.of())))).andExpect(status().isNotFound());
    }
    @Test void GET_afterPost_200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/"+famCf.getId()+"/divorce-checklist").with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("country","FRANCE","etapeStatuts",Map.of(),"pieceStatuts",Map.of())))).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/case-files/"+famCf.getId()+"/divorce-checklist").with(authentication(auth))).andExpect(status().isOk());
    }
    @Test void GET_withoutPost_404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/"+famCf.getId()+"/divorce-checklist").with(authentication(auth))).andExpect(status().isNotFound());
    }
    @Test void POST_upsert() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/"+famCf.getId()+"/divorce-checklist").with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("country","FRANCE","etapeStatuts",Map.of(),"pieceStatuts",Map.of())))).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/case-files/"+famCf.getId()+"/divorce-checklist").with(authentication(auth)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("country","BELGIQUE","etapeStatuts",Map.of(),"pieceStatuts",Map.of()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.country").value("BELGIQUE"));
    }

    private OAuth2AuthenticationToken bAuth(String sub, String email) {
        Map<String,Object> c = Map.of("sub",sub,"email",email,"iss","https://accounts.google.com");
        OidcIdToken t = new OidcIdToken("token",Instant.now(),Instant.now().plusSeconds(3600),c);
        DefaultOidcUser u = new DefaultOidcUser(List.of(new OidcUserAuthority(t)),t,"sub");
        return new OAuth2AuthenticationToken(u,u.getAuthorities(),"google");
    }
}
