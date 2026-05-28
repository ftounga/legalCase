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
 * SF-219-30 : endpoints REST de l'outil <i>Saisine du Conseiller en
 * Prévention Aspects Psychosociaux (CPAP) — procédure RPS interne
 * BE</i> (Loi du 04/08/1996 art. 32sexies + AR du 10/04/2014).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier
 *       (upsert unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver
 * l'isolation BE-only. La France a un régime distinct (Code du
 * travail FR art. L. 4121-1 et suivants obligation employeur de
 * sécurité ; référent harcèlement art. L. 1153-5-1 ; document unique
 * d'évaluation des risques art. R. 4121-1). Restitution séparée par
 * outil.</p>
 *
 * <p>Distinct de SF-213-07
 * {@code harcelement-be-procedure-formelle} (checklist détaillée d'une
 * demande formelle déjà déposée + protection 12 mois art. 32sexies)
 * et de F-DT-30 {@code harcelement-licenciement-nul-section} (nullité
 * licenciement représailles) : SF-219-30 couvre la <b>conformité
 * procédurale de la saisine</b> elle-même (entretien préalable,
 * formalisme écrit, délais 10 j / 3 mois / 2 mois) en amont de ces
 * deux outils.</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/bien-etre-rps-conseiller-prevention")
public class BienEtreRpsConseillerPreventionController {

    private final BienEtreRpsConseillerPreventionService service;

    public BienEtreRpsConseillerPreventionController(
            BienEtreRpsConseillerPreventionService service) {
        this.service = service;
    }

    @PostMapping
    public BienEtreRpsConseillerPreventionResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody BienEtreRpsConseillerPreventionRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public BienEtreRpsConseillerPreventionResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
