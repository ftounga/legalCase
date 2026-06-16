package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.workspace.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F-284 / SF-284-01 — IT de l'endpoint d'agrégation échéancier (lecture seule).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class EcheancierControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private CaseDeadlineRepository deadlineRepository;
    @Autowired private ContradictoireRoundRepository roundRepository;

    private OAuth2AuthenticationToken auth;
    private CaseFile caseFile;

    @BeforeEach
    void setUp() {
        roundRepository.deleteAll();
        deadlineRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("eche-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-eche-sub");
        authAccountRepository.save(account);

        Workspace workspace = new Workspace();
        workspace.setName("ECHE-TEST");
        workspace.setSlug("eche-slug-" + System.currentTimeMillis());
        workspace.setOwner(user);
        workspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        caseFile = new CaseFile();
        caseFile.setTitle("Dossier Échéancier");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFile.setStatus("OPEN");
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFileRepository.save(caseFile);

        auth = buildGoogleAuth("google-eche-sub", "eche-test@example.com");
    }

    // AC5 : dossier sans délai ni round → 200, items vide, nextItem null, counts à 0
    @Test
    void empty_returns200WithEmptyAggregate() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/echeancier", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.nextItem").doesNotExist())
                .andExpect(jsonPath("$.counts.total").value(0))
                .andExpect(jsonPath("$.counts.overdue").value(0));
    }

    // AC1 : 1 délai MANUAL J-3 + 1 round réponse J-20 → trié, nextItem CRITICAL, counts cohérents
    @Test
    void aggregatesDeadlinesAndRoundsSortedByUrgency() throws Exception {
        LocalDate today = LocalDate.now();
        saveDeadline("Délai proche", today.plusDays(3), "MANUAL", null);
        saveRound(2, "Conclusions adverses n°2", today.plusDays(20));

        mockMvc.perform(get("/api/v1/case-files/{id}/echeancier", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].label").value("Délai proche"))
                .andExpect(jsonPath("$.items[0].kind").value("DEADLINE"))
                .andExpect(jsonPath("$.items[0].urgency").value("CRITICAL"))
                .andExpect(jsonPath("$.items[1].kind").value("CONTRADICTOIRE"))
                .andExpect(jsonPath("$.items[1].urgency").value("UPCOMING"))
                .andExpect(jsonPath("$.items[1].source").value("CONTRADICTOIRE"))
                .andExpect(jsonPath("$.nextItem.daysUntil").value(3))
                .andExpect(jsonPath("$.counts.critical").value(1))
                .andExpect(jsonPath("$.counts.upcoming").value(1))
                .andExpect(jsonPath("$.counts.total").value(2));
    }

    // F-289 (fix bruit) : les sources non-délais (pièces / checks / pistes injectées
    // par F-192/193/194 dans case_deadlines) sont EXCLUES de l'échéancier ; seuls les
    // vrais délais (MANUAL/STATUTORY/AI accepté/contradictoire) restent.
    @Test
    void excludesNonDeadlineSources_pieceCheckPiste() throws Exception {
        LocalDate today = LocalDate.now();
        saveDeadline("Vrai délai légal", today.plusDays(10), "STATUTORY", null);
        saveDeadline("Demander au client : Contrat de travail", today.plusDays(30), "PIECE_A_DEMANDER", null);
        saveDeadline("À vérifier : entretien préalable", today.plusDays(40), "PROCEDURE_CHECK_TO_CHECK", null);
        saveDeadline("Stratégie : contester la faute grave", today.plusDays(50), "PISTE_RETENUE", null);

        mockMvc.perform(get("/api/v1/case-files/{id}/echeancier", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].label").value("Vrai délai légal"))
                .andExpect(jsonPath("$.items[0].source").value("STATUTORY"))
                .andExpect(jsonPath("$.counts.total").value(1));
    }

    // AC2 : un délai AI PENDING n'apparaît pas ; AI ACCEPTED apparaît
    @Test
    void excludesAiPendingButKeepsAiAccepted() throws Exception {
        LocalDate today = LocalDate.now();
        saveDeadline("IA en attente", today.plusDays(5), "AI", "PENDING");
        saveDeadline("IA acceptée", today.plusDays(10), "AI", "ACCEPTED");

        mockMvc.perform(get("/api/v1/case-files/{id}/echeancier", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].label").value("IA acceptée"))
                .andExpect(jsonPath("$.items[0].source").value("AI"));
    }

    // AC3 : délai dépassé → urgency OVERDUE, daysUntil négatif, counts.overdue=1
    @Test
    void overdueDeadlineIsFlagged() throws Exception {
        saveDeadline("Délai dépassé", LocalDate.now().minusDays(2), "MANUAL", null);

        mockMvc.perform(get("/api/v1/case-files/{id}/echeancier", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].urgency").value("OVERDUE"))
                .andExpect(jsonPath("$.items[0].daysUntil").value(-2))
                .andExpect(jsonPath("$.counts.overdue").value(1));
    }

    // AC4 : dossier d'un autre workspace → 404 (isolation)
    @Test
    void otherWorkspaceCaseFile_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/echeancier", UUID.randomUUID())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // Sans auth → 401
    @Test
    void withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/echeancier", caseFile.getId()))
                .andExpect(status().isUnauthorized());
    }

    // Round sans responseDueAt → ignoré (fail-open)
    @Test
    void roundWithoutResponseDueAtIsIgnored() throws Exception {
        saveRound(1, "Saisine", null);

        mockMvc.perform(get("/api/v1/case-files/{id}/echeancier", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    // SF-290-04 : l'échéance de réponse d'un round est satisfaite par un round postérieur
    // → seule l'échéance du DERNIER round subsiste (round 1 dépassé n'apparaît plus).
    @Test
    void roundResponseDeadlineSatisfiedByLaterRound() throws Exception {
        LocalDate today = LocalDate.now();
        saveRound(1, "Nos conclusions de saisine", today.minusDays(54)); // échéance passée (J+54 dépassé)
        saveRound(2, "Conclusions adverses", today.plusDays(20));        // round postérieur = réponse arrivée

        mockMvc.perform(get("/api/v1/case-files/{id}/echeancier", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))   // round 1 (satisfait) exclu
                .andExpect(jsonPath("$.items[0].source").value("CONTRADICTOIRE"))
                .andExpect(jsonPath("$.items[0].urgency").value("UPCOMING"))
                .andExpect(jsonPath("$.counts.overdue").value(0));  // plus d'OVERDUE fantôme
    }

    // SF-290-04 : si le dernier round n'a pas d'échéance de réponse, aucune échéance de round n'apparaît.
    @Test
    void lastRoundWithoutDueAt_noRoundEcheance() throws Exception {
        LocalDate today = LocalDate.now();
        saveRound(1, "Nos conclusions", today.minusDays(54)); // satisfait par le round 2
        saveRound(2, "Notre réplique", null);                 // dernier round, sans échéance

        mockMvc.perform(get("/api/v1/case-files/{id}/echeancier", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    private CaseDeadline saveDeadline(String label, LocalDate dueDate, String source, String aiStatus) {
        CaseDeadline d = new CaseDeadline();
        d.setCaseFile(caseFile);
        d.setLabel(label);
        d.setDueDate(dueDate);
        d.setSource(source);
        d.setAiStatus(aiStatus);
        return deadlineRepository.save(d);
    }

    private ContradictoireRound saveRound(int number, String label, LocalDate responseDueAt) {
        ContradictoireRound r = new ContradictoireRound();
        r.setCaseFile(caseFile);
        r.setRoundNumber(number);
        r.setParty(ContradictoireParty.ADVERSE);
        r.setLabel(label);
        r.setDatedAt(LocalDate.now());
        r.setResponseDueAt(responseDueAt);
        return roundRepository.save(r);
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
