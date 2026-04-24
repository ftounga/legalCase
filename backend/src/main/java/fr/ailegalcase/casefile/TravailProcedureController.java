package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * SF-136-02 : endpoint REST pour la lecture du calendrier procédural travail
 * (FR prud'hommes / appel social / cassation sociale ; BE tribunal du travail
 * / cour du travail / cassation).
 *
 * <p>Expose {@link TravailProcedureService#getJalons(UUID, String, String,
 * OidcUser, Principal)} via {@code GET
 * /api/v1/case-files/{caseFileId}/travail-procedure-jalons}. Stateless —
 * aucune persistance créée par cet endpoint, la liste est lue en temps réel
 * via {@code LegalReferentialService.getTravailProcedureJalons}.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/travail-procedure-jalons")
public class TravailProcedureController {

    private final TravailProcedureService service;

    public TravailProcedureController(TravailProcedureService service) {
        this.service = service;
    }

    @GetMapping
    public List<TravailProcedureJalonResponse> getJalons(
            @PathVariable UUID caseFileId,
            @RequestParam("typeProcedure") String typeProcedure,
            @RequestParam(value = "dateDeclencheur", required = false) String dateDeclencheur,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.getJalons(caseFileId, typeProcedure, dateDeclencheur, oidcUser, principal);
    }
}
