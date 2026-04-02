package fr.ailegalcase.timetracking.controller;

import fr.ailegalcase.timetracking.dto.BillingRateRequest;
import fr.ailegalcase.timetracking.dto.BillingRateResponse;
import fr.ailegalcase.timetracking.service.BillingRateService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/workspace/billing-rate")
public class BillingRateController {

    private final BillingRateService billingRateService;

    public BillingRateController(BillingRateService billingRateService) {
        this.billingRateService = billingRateService;
    }

    @PutMapping
    public BillingRateResponse setRate(@Valid @RequestBody BillingRateRequest request,
                                       @AuthenticationPrincipal OidcUser oidcUser,
                                       Principal principal) {
        return billingRateService.setRate(request, oidcUser, principal);
    }

    @GetMapping
    public BillingRateResponse getCurrentRate(@AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return billingRateService.getCurrentRate(oidcUser, principal);
    }
}
