package fr.ailegalcase.casefile;

import java.util.List;

/**
 * F-283 / SF-283-01 — la frise des phases procédurales : la liste ordonnée des
 * transitions + la phase courante calculée (la plus récente), destinée à la mise
 * en exergue dans la frise et au fil rouge.
 *
 * @param phases       transitions ordonnées (date d'entrée croissante puis ordre référentiel)
 * @param currentPhase phase courante (dernière entrée), {@code null} si aucune
 */
public record CasePhaseTimelineResponse(
        List<CasePhaseResponse> phases,
        CasePhaseType currentPhase
) {}
