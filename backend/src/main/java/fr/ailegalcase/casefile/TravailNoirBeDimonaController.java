package fr.ailegalcase.casefile;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-219-26 : endpoints REST de l'outil <i>Travail noir BE —
 * DIMONA, requalification et sanctions</i> (Loi-programme du
 * 24/12/2002 art. 167-184 + AR du 05/11/2002 + Code pénal social
 * art. 181 niveau 4).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier
 *       (upsert unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver
 * l'isolation BE-only. Le régime français du travail dissimulé
 * (Code du travail art. L. 8221-1 et s., DPAE et non DIMONA, parquet
 * et non auditorat, amende 45 000 € pp / 225 000 € pm art. L. 8224-1,
 * URSSAF redressement majoration 25 %) est distinct dans ses quanta
 * et sa procédure. Restitution séparée par outil.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/travail-noir-be-dimona")
public class TravailNoirBeDimonaController {

    private final TravailNoirBeDimonaService service;

    public TravailNoirBeDimonaController(
            TravailNoirBeDimonaService service) {
        this.service = service;
    }

    @PostMapping
    public TravailNoirBeDimonaResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody TravailNoirBeDimonaRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public TravailNoirBeDimonaResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
