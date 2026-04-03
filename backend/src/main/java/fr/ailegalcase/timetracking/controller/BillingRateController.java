package fr.ailegalcase.timetracking.controller;

import fr.ailegalcase.timetracking.dto.BillingRateRequest;
import fr.ailegalcase.timetracking.dto.BillingRateResponse;
import fr.ailegalcase.timetracking.service.BillingRateService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@RestController
public class BillingRateController {

    private final BillingRateService billingRateService;

    public BillingRateController(BillingRateService billingRateService) {
        this.billingRateService = billingRateService;
    }

    @PutMapping("/api/v1/workspace/members/{userId}/billing-rate")
    public BillingRateResponse setRateForMember(@PathVariable UUID userId,
                                                @Valid @RequestBody BillingRateRequest request,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return billingRateService.setRateForMember(userId, request, oidcUser, principal);
    }

    @GetMapping("/api/v1/workspace/members/billing-rates")
    public Map<UUID, BillingRateResponse> getMemberRates(@AuthenticationPrincipal OidcUser oidcUser,
                                                         Principal principal) {
        return billingRateService.getMemberRates(oidcUser, principal);
    }

    @GetMapping("/api/v1/workspace/billing-rate")
    public BillingRateResponse getCurrentRate(@AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return billingRateService.getCurrentRate(oidcUser, principal);
    }
}
