package fr.ailegalcase.timetracking;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.timetracking.entity.TimeEntry;
import fr.ailegalcase.timetracking.entity.UserBillingRate;
import fr.ailegalcase.timetracking.repository.TimeEntryRepository;
import fr.ailegalcase.timetracking.repository.UserBillingRateRepository;
import fr.ailegalcase.workspace.*;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class TimeTrackingControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private TimeEntryRepository timeEntryRepository;
    @Autowired private UserBillingRateRepository billingRateRepository;

    private OAuth2AuthenticationToken ownerAuth;
    private OAuth2AuthenticationToken memberAuth;
    private User owner;
    private User memberUser;
    private Workspace workspace;
    private CaseFile caseFile;

    @BeforeEach
    void setUp() {
        timeEntryRepository.deleteAll();
        billingRateRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        owner = createUser("owner-tt@example.com");
        authAccountRepository.save(buildAccount(owner, "GOOGLE", "google-owner-tt-sub"));

        memberUser = createUser("member-tt@example.com");
        authAccountRepository.save(buildAccount(memberUser, "GOOGLE", "google-member-tt-sub"));

        workspace = new Workspace();
        workspace.setName("TT-Test");
        workspace.setSlug("tt-test-" + System.currentTimeMillis());
        workspace.setOwner(owner);
        workspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
        workspaceRepository.save(workspace);

        WorkspaceMember ownerMember = new WorkspaceMember();
        ownerMember.setWorkspace(workspace);
        ownerMember.setUser(owner);
        ownerMember.setMemberRole("OWNER");
        ownerMember.setPrimary(true);
        workspaceMemberRepository.save(ownerMember);

        WorkspaceMember wsMember = new WorkspaceMember();
        wsMember.setWorkspace(workspace);
        wsMember.setUser(memberUser);
        wsMember.setMemberRole("MEMBER");
        wsMember.setPrimary(true);
        workspaceMemberRepository.save(wsMember);

        caseFile = new CaseFile();
        caseFile.setTitle("Dossier TT");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFile.setStatus("OPEN");
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(owner);
        caseFileRepository.save(caseFile);

        ownerAuth = buildGoogleAuth("google-owner-tt-sub", "owner-tt@example.com");
        memberAuth = buildGoogleAuth("google-member-tt-sub", "member-tt@example.com");
    }

    // ---- BillingRate ----

    // I-BR-01 : PUT billing-rate → 200 avec taux retourné
    @Test
    void putBillingRate_validRate_returns200() throws Exception {
        mockMvc.perform(put("/api/v1/workspace/billing-rate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ratePerHour": 250.00}
                                """)
                        .with(authentication(ownerAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratePerHour").value(250.00))
                .andExpect(jsonPath("$.effectiveFrom").isNotEmpty());
    }

    // I-BR-02 : PUT billing-rate avec ratePerHour = 0 → 400
    @Test
    void putBillingRate_zeroRate_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/workspace/billing-rate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ratePerHour": 0}
                                """)
                        .with(authentication(ownerAuth)))
                .andExpect(status().isBadRequest());
    }

    // I-BR-03 : GET billing-rate → taux actif retourné
    @Test
    void getBillingRate_withRateConfigured_returnsRate() throws Exception {
        saveBillingRate(owner, workspace, new BigDecimal("300.00"), LocalDate.now());

        mockMvc.perform(get("/api/v1/workspace/billing-rate")
                        .with(authentication(ownerAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratePerHour").value(300.00));
    }

    // I-BR-04 : GET billing-rate sans taux → 200 avec corps vide ou null
    @Test
    void getBillingRate_noRateConfigured_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/workspace/billing-rate")
                        .with(authentication(ownerAuth)))
                .andExpect(status().isOk());
    }

    // ---- TimeEntry ----

    // I-TE-01 : POST start → 201 avec stoppedAt null
    @Test
    void startTimer_nominal_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/{id}/time-entries/start", caseFile.getId())
                        .with(authentication(ownerAuth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.stoppedAt").doesNotExist());
    }

    // I-TE-02 : POST start (2e appel sans stop) → 409
    @Test
    void startTimer_secondStartWithoutStop_returns409() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/{id}/time-entries/start", caseFile.getId())
                        .with(authentication(ownerAuth)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/case-files/{id}/time-entries/start", caseFile.getId())
                        .with(authentication(ownerAuth)))
                .andExpect(status().isConflict());
    }

    // I-TE-03 : POST start sur dossier d'un autre workspace → 403
    @Test
    void startTimer_caseFileFromOtherWorkspace_returns403() throws Exception {
        User otherOwner = createUser("other-owner-tt@example.com");
        authAccountRepository.save(buildAccount(otherOwner, "GOOGLE", "google-other-owner-tt-sub"));

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("Other-WS");
        otherWorkspace.setSlug("other-ws-" + System.currentTimeMillis());
        otherWorkspace.setOwner(otherOwner);
        otherWorkspace.setLegalDomain("DROIT_DU_TRAVAIL");
        otherWorkspace.setPlanCode("STARTER");
        otherWorkspace.setStatus("ACTIVE");
        workspaceRepository.save(otherWorkspace);

        CaseFile otherCaseFile = new CaseFile();
        otherCaseFile.setTitle("Autre Dossier");
        otherCaseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        otherCaseFile.setStatus("OPEN");
        otherCaseFile.setWorkspace(otherWorkspace);
        otherCaseFile.setCreatedBy(otherOwner);
        caseFileRepository.save(otherCaseFile);

        mockMvc.perform(post("/api/v1/case-files/{id}/time-entries/start", otherCaseFile.getId())
                        .with(authentication(ownerAuth)))
                .andExpect(status().isForbidden());
    }

    // I-TE-04 : POST stop → 200 avec durationSeconds > 0
    @Test
    void stopTimer_nominal_returns200WithDuration() throws Exception {
        TimeEntry entry = saveTimeEntry(owner, workspace, caseFile,
                Instant.now().minusSeconds(3600), null);

        mockMvc.perform(post("/api/v1/time-entries/{id}/stop", entry.getId())
                        .with(authentication(ownerAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stoppedAt").isNotEmpty())
                .andExpect(jsonPath("$.durationSeconds").isNumber());
    }

    // I-TE-05 : POST stop déjà stoppé → 400
    @Test
    void stopTimer_alreadyStopped_returns400() throws Exception {
        TimeEntry entry = saveTimeEntry(owner, workspace, caseFile,
                Instant.now().minusSeconds(3600), Instant.now().minusSeconds(1800));
        entry.setDurationSeconds(1800);
        timeEntryRepository.save(entry);

        mockMvc.perform(post("/api/v1/time-entries/{id}/stop", entry.getId())
                        .with(authentication(ownerAuth)))
                .andExpect(status().isBadRequest());
    }

    // I-TE-06 : POST stop appartenant à un autre user → 403
    @Test
    void stopTimer_byOtherUser_returns403() throws Exception {
        TimeEntry entry = saveTimeEntry(owner, workspace, caseFile,
                Instant.now().minusSeconds(3600), null);

        mockMvc.perform(post("/api/v1/time-entries/{id}/stop", entry.getId())
                        .with(authentication(memberAuth)))
                .andExpect(status().isForbidden());
    }

    // I-TE-07 : GET time-entries → liste isolée par workspace
    @Test
    void listEntries_returnsEntriesForWorkspace() throws Exception {
        saveTimeEntry(owner, workspace, caseFile, Instant.now().minusSeconds(3600), Instant.now());

        mockMvc.perform(get("/api/v1/case-files/{id}/time-entries", caseFile.getId())
                        .with(authentication(ownerAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ---- TimeReport ----

    // I-TR-01 : GET time-report?month=2026-04 → agrégation correcte
    @Test
    void getTimeReport_validMonth_returnsAggregation() throws Exception {
        TimeEntry entry = saveTimeEntry(owner, workspace, caseFile,
                Instant.parse("2026-04-10T10:00:00Z"),
                Instant.parse("2026-04-10T11:00:00Z"));
        entry.setDurationSeconds(3600);
        timeEntryRepository.save(entry);

        saveBillingRate(owner, workspace, new BigDecimal("200.00"), LocalDate.of(2026, 1, 1));

        mockMvc.perform(get("/api/v1/workspace/time-report")
                        .param("month", "2026-04")
                        .with(authentication(ownerAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-04"))
                .andExpect(jsonPath("$.lines").isArray());
    }

    // I-TR-02 : GET time-report?month=invalid → 400
    @Test
    void getTimeReport_invalidMonth_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/workspace/time-report")
                        .param("month", "invalid")
                        .with(authentication(ownerAuth)))
                .andExpect(status().isBadRequest());
    }

    // I-TR-03 : GET time-report/export → Content-Type text/csv
    @Test
    void exportTimeReport_returnsTextCsv() throws Exception {
        mockMvc.perform(get("/api/v1/workspace/time-report/export")
                        .param("month", "2026-04")
                        .with(authentication(ownerAuth)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")));
    }

    // I-TR-04 : MEMBER tente d'accéder au rapport → 403
    @Test
    void getTimeReport_memberRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/workspace/time-report")
                        .param("month", "2026-04")
                        .with(authentication(memberAuth)))
                .andExpect(status().isForbidden());
    }

    // ---- Isolation workspace ----

    // I-ISO-01 : workspace A ne voit pas les entrées du workspace B dans le rapport
    @Test
    void timeReport_workspaceIsolation() throws Exception {
        User otherOwner = createUser("other-iso@example.com");
        authAccountRepository.save(buildAccount(otherOwner, "GOOGLE", "google-other-iso-sub"));

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("ISO-WS");
        otherWorkspace.setSlug("iso-ws-" + System.currentTimeMillis());
        otherWorkspace.setOwner(otherOwner);
        otherWorkspace.setLegalDomain("DROIT_DU_TRAVAIL");
        otherWorkspace.setPlanCode("STARTER");
        otherWorkspace.setStatus("ACTIVE");
        workspaceRepository.save(otherWorkspace);

        CaseFile otherCaseFile = new CaseFile();
        otherCaseFile.setTitle("Autre Dossier ISO");
        otherCaseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        otherCaseFile.setStatus("OPEN");
        otherCaseFile.setWorkspace(otherWorkspace);
        otherCaseFile.setCreatedBy(otherOwner);
        caseFileRepository.save(otherCaseFile);

        TimeEntry otherEntry = saveTimeEntry(otherOwner, otherWorkspace, otherCaseFile,
                Instant.parse("2026-04-10T10:00:00Z"),
                Instant.parse("2026-04-10T11:00:00Z"));
        otherEntry.setDurationSeconds(3600);
        timeEntryRepository.save(otherEntry);

        mockMvc.perform(get("/api/v1/workspace/time-report")
                        .param("month", "2026-04")
                        .with(authentication(ownerAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines").isEmpty());
    }

    // ---- Sans auth ----

    // I-AUTH-01 : accès sans auth → 401
    @Test
    void startTimer_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/{id}/time-entries/start", caseFile.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ---- Helpers ----

    private User createUser(String email) {
        User u = new User();
        u.setEmail(email);
        u.setStatus("ACTIVE");
        return userRepository.save(u);
    }

    private AuthAccount buildAccount(User user, String provider, String sub) {
        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider(provider);
        account.setProviderUserId(sub);
        return account;
    }

    private TimeEntry saveTimeEntry(User user, Workspace workspace, CaseFile caseFile,
                                    Instant startedAt, Instant stoppedAt) {
        TimeEntry entry = new TimeEntry();
        entry.setUser(user);
        entry.setWorkspace(workspace);
        entry.setCaseFile(caseFile);
        entry.setStartedAt(startedAt);
        entry.setStoppedAt(stoppedAt);
        if (stoppedAt != null) {
            entry.setDurationSeconds((int) (stoppedAt.getEpochSecond() - startedAt.getEpochSecond()));
        }
        return timeEntryRepository.save(entry);
    }

    private void saveBillingRate(User user, Workspace workspace, BigDecimal rate, LocalDate effectiveFrom) {
        UserBillingRate billingRate = new UserBillingRate();
        billingRate.setUser(user);
        billingRate.setWorkspace(workspace);
        billingRate.setRatePerHour(rate);
        billingRate.setEffectiveFrom(effectiveFrom);
        billingRateRepository.save(billingRate);
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
