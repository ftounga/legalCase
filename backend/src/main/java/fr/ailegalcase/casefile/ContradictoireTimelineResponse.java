package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * F-282 — la frise du cycle contradictoire : la liste ordonnée des rounds +
 * un résumé calculé (round courant, à qui le tour, prochaine échéance) destiné
 * au fil rouge de l'en-tête (stepper).
 */
public record ContradictoireTimelineResponse(
        List<ContradictoireRoundResponse> rounds,
        Summary summary
) {
    /**
     * @param currentRoundNumber numéro du dernier round connu (0 si aucun)
     * @param awaitingParty       partie attendue pour le prochain jeu (le « tour »)
     * @param nextDeadline        échéance du prochain dépôt attendu (le cas échéant)
     */
    public record Summary(
            int currentRoundNumber,
            ContradictoireParty awaitingParty,
            LocalDate nextDeadline
    ) {}
}
