package fr.ailegalcase.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.workspace.Workspace;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class PromoCodeAdminControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private PromoCodeRepository promoCodeRepository;
    @Autowired private PromoCodeRedemptionRepository redemptionRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private ObjectMapper objectMapper;

    private OAuth2AuthenticationToken superAdminAuth;
    private OAuth2AuthenticationToken regularAuth;
    private UUID superAdminId;
    private UUID seedWorkspaceId;

    @BeforeEach
    void setUp() {
        redemptionRepository.deleteAll();
        promoCodeRepository.deleteAll();
        // Ne pas supprimer users/workspaces : d'autres tests partagent
        // potentiellement ces données et le suffix UUID isole nos inserts.

        String suffix = "-" + UUID.randomUUID();
        User superAdmin = newUser("admin" + suffix + "@example.com", true);
        newAuthAccount(superAdmin, "google-admin" + suffix);
        superAdminId = superAdmin.getId();

        User regular = newUser("regular" + suffix + "@example.com", false);
        newAuthAccount(regular, "google-regular" + suffix);

        // Workspace partagé pour les redemptions du listing (FK obligatoire).
        Workspace ws = new Workspace();
        ws.setName("Admin IT WS");
        ws.setSlug("admin-it-ws-" + UUID.randomUUID());
        ws.setOwner(superAdmin);
        ws.setPlanCode("FREE");
        ws.setStatus("ACTIVE");
        ws.setLegalDomain("DROIT_DU_TRAVAIL");
        seedWorkspaceId = workspaceRepository.save(ws).getId();

        superAdminAuth = buildGoogleAuth("google-admin" + suffix, "admin" + suffix + "@example.com");
        regularAuth = buildGoogleAuth("google-regular" + suffix, "regular" + suffix + "@example.com");
    }

    // C1 — Création nominale par super-admin → 201
    @Test
    void createCode_superAdmin_returns201WithBody() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "code", "ACE2026",
                "type", "TRIAL_EXTENSION",
                "valueDays", 30,
                "partnerLabel", "ACE",
                "maxUses", 100,
                "expiresAt", Instant.now().plus(30, ChronoUnit.DAYS).toString()));

        mockMvc.perform(post("/api/v1/super-admin/promo-codes")
                        .with(authentication(superAdminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("ACE2026"))
                .andExpect(jsonPath("$.type").value("TRIAL_EXTENSION"))
                .andExpect(jsonPath("$.valueDays").value(30))
                .andExpect(jsonPath("$.maxUses").value(100))
                .andExpect(jsonPath("$.usesCount").value(0))
                .andExpect(jsonPath("$.active").value(true));
    }

    // C1 — Le code est uppercase + trim côté backend
    @Test
    void createCode_trimsAndUppercases() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "code", "  ace2026  ",
                "type", "TRIAL_EXTENSION",
                "valueDays", 30,
                "partnerLabel", "ACE",
                "maxUses", 100,
                "expiresAt", Instant.now().plus(30, ChronoUnit.DAYS).toString()));

        mockMvc.perform(post("/api/v1/super-admin/promo-codes")
                        .with(authentication(superAdminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("ACE2026"));
    }

    // C2 — Création d'un code existant → 409 PROMO_CODE_DUPLICATE
    @Test
    void createCode_duplicate_returns409() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "code", "ACE2026",
                "type", "TRIAL_EXTENSION",
                "valueDays", 30,
                "partnerLabel", "ACE",
                "maxUses", 100,
                "expiresAt", Instant.now().plus(30, ChronoUnit.DAYS).toString()));

        mockMvc.perform(post("/api/v1/super-admin/promo-codes")
                        .with(authentication(superAdminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/super-admin/promo-codes")
                        .with(authentication(superAdminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROMO_CODE_DUPLICATE"));
    }

    // C3 — Création par non-super-admin → 403 FORBIDDEN_SUPER_ADMIN
    @Test
    void createCode_nonSuperAdmin_returns403() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "code", "ACE2026",
                "type", "TRIAL_EXTENSION",
                "valueDays", 30,
                "partnerLabel", "ACE",
                "maxUses", 100,
                "expiresAt", Instant.now().plus(30, ChronoUnit.DAYS).toString()));

        mockMvc.perform(post("/api/v1/super-admin/promo-codes")
                        .with(authentication(regularAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_SUPER_ADMIN"));
    }

    // Validation Bean — body invalide → 400
    @Test
    void createCode_missingExpiresAt_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "code", "ACE2026",
                "type", "TRIAL_EXTENSION",
                "valueDays", 30,
                "partnerLabel", "ACE",
                "maxUses", 100));

        mockMvc.perform(post("/api/v1/super-admin/promo-codes")
                        .with(authentication(superAdminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCode_valueDaysAbove365_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "code", "TOOBIG",
                "type", "TRIAL_EXTENSION",
                "valueDays", 1000,
                "partnerLabel", "ACE",
                "maxUses", 100,
                "expiresAt", Instant.now().plus(30, ChronoUnit.DAYS).toString()));

        mockMvc.perform(post("/api/v1/super-admin/promo-codes")
                        .with(authentication(superAdminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCode_trialExtensionWithoutValueDays_returns409() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "code", "NOVALUE",
                "type", "TRIAL_EXTENSION",
                "partnerLabel", "ACE",
                "maxUses", 100,
                "expiresAt", Instant.now().plus(30, ChronoUnit.DAYS).toString()));

        mockMvc.perform(post("/api/v1/super-admin/promo-codes")
                        .with(authentication(superAdminAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROMO_CODE_TYPE_NOT_SUPPORTED_YET"));
    }

    // C4 — Listing trié createdAt DESC avec usesCount recalculé
    @Test
    void listCodes_superAdmin_returnsSortedListWithRecalculatedUsesCount() throws Exception {
        seedCode("OLD", PromoCodeType.TRIAL_EXTENSION, 10, 100, true, Instant.now().minus(2, ChronoUnit.DAYS));
        PromoCode newer = seedCode("NEW", PromoCodeType.TRIAL_EXTENSION, 20, 100, true,
                Instant.now().minus(1, ChronoUnit.HOURS));

        // 2 redemptions distinctes sur "NEW" pour vérifier le recalcul (chaque
        // redemption sur un workspace différent pour rester cohérent avec
        // l'invariant anti-abus côté PostgreSQL).
        Workspace ws2 = new Workspace();
        ws2.setName("List IT WS 2");
        ws2.setSlug("list-it-ws2-" + UUID.randomUUID());
        ws2.setOwner(userRepository.findAll().get(0));
        ws2.setPlanCode("FREE");
        ws2.setStatus("ACTIVE");
        ws2.setLegalDomain("DROIT_DU_TRAVAIL");
        UUID ws2Id = workspaceRepository.save(ws2).getId();

        seedRedemption(newer.getId(), "NEW", seedWorkspaceId);
        seedRedemption(newer.getId(), "NEW", ws2Id);

        MvcResult res = mockMvc.perform(get("/api/v1/super-admin/promo-codes")
                        .with(authentication(superAdminAuth))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("NEW"))
                .andExpect(jsonPath("$[0].usesCount").value(2))
                .andExpect(jsonPath("$[1].code").value("OLD"))
                .andExpect(jsonPath("$[1].usesCount").value(0))
                .andReturn();
    }

    @Test
    void listCodes_nonSuperAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/super-admin/promo-codes")
                        .with(authentication(regularAuth))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // C5 — Désactivation flippe active=false ; idempotent
    @Test
    void deactivateCode_active_flipsToInactive_thenIdempotent() throws Exception {
        PromoCode code = seedCode("DEACT", PromoCodeType.TRIAL_EXTENSION, 30, 100, true,
                Instant.now());

        mockMvc.perform(post("/api/v1/super-admin/promo-codes/" + code.getId() + "/deactivate")
                        .with(authentication(superAdminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/api/v1/super-admin/promo-codes/" + code.getId() + "/deactivate")
                        .with(authentication(superAdminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deactivateCode_unknownId_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/super-admin/promo-codes/" + UUID.randomUUID() + "/deactivate")
                        .with(authentication(superAdminAuth)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMO_CODE_NOT_FOUND"));
    }

    @Test
    void deactivateCode_nonSuperAdmin_returns403() throws Exception {
        PromoCode code = seedCode("DEACT2", PromoCodeType.TRIAL_EXTENSION, 30, 100, true,
                Instant.now());

        mockMvc.perform(post("/api/v1/super-admin/promo-codes/" + code.getId() + "/deactivate")
                        .with(authentication(regularAuth)))
                .andExpect(status().isForbidden());
    }

    // Auth — sans authentification → 401
    @Test
    void createCode_noAuth_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "code", "ANON",
                "type", "TRIAL_EXTENSION",
                "valueDays", 30,
                "partnerLabel", "ACE",
                "maxUses", 100,
                "expiresAt", Instant.now().plus(30, ChronoUnit.DAYS).toString()));

        mockMvc.perform(post("/api/v1/super-admin/promo-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // ===== Helpers =====

    private User newUser(String email, boolean superAdmin) {
        User u = new User();
        u.setEmail(email);
        u.setStatus("ACTIVE");
        u.setSuperAdmin(superAdmin);
        return userRepository.save(u);
    }

    private void newAuthAccount(User user, String sub) {
        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId(sub);
        authAccountRepository.save(account);
    }

    private PromoCode seedCode(String code, PromoCodeType type, Integer valueDays,
                               int maxUses, boolean active, Instant createdAt) {
        PromoCode c = new PromoCode();
        c.setCode(code);
        c.setType(type);
        c.setValueDays(valueDays);
        c.setPartnerLabel("ACE");
        c.setMaxUses(maxUses);
        c.setUsesCount(0);
        c.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        c.setActive(active);
        c.setCreatedAt(createdAt);
        c.setCreatedByUserId(superAdminId);
        return promoCodeRepository.save(c);
    }

    private void seedRedemption(UUID promoCodeId, String codeAtRedemption, UUID workspaceId) {
        PromoCodeRedemption r = new PromoCodeRedemption();
        r.setWorkspaceId(workspaceId);
        r.setPromoCodeId(promoCodeId);
        r.setCodeAtRedemption(codeAtRedemption);
        r.setType(PromoCodeType.TRIAL_EXTENSION);
        r.setValueAppliedDays(30);
        r.setAppliedByUserId(superAdminId);
        r.setRedeemedAt(Instant.now());
        redemptionRepository.save(r);
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
