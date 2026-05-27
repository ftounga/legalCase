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
 * SF-213-06 : endpoints REST de l'outil d'analyse de validité d'une
 * transaction de fin de contrat en droit belge (art. 2044 Code civil
 * belge + Loi 03/07/1978 art. 6).
 *
 * <ul>
 *   <li>POST — créer / mettre à jour l'analyse pour ce dossier (upsert
 *       unicité sur {@code case_file_id}).</li>
 *   <li>GET — récupérer la dernière analyse persistée (404 si aucune).</li>
 * </ul>
 *
 * <p>Gate strict BELGIQUE côté service — 404 pour préserver l'isolation
 * BE-only (la France a un outil distinct {@code transaction-fin-contrat}
 * couvert par F-DT-31, avec un régime juridique différent).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/decision-tools/transaction-be-travail")
public class TransactionBeTravailController {

    private final TransactionBeTravailService service;

    public TransactionBeTravailController(TransactionBeTravailService service) {
        this.service = service;
    }

    @PostMapping
    public TransactionBeTravailResponse analyze(
            @PathVariable UUID caseFileId,
            @Valid @RequestBody TransactionBeTravailRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public TransactionBeTravailResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
