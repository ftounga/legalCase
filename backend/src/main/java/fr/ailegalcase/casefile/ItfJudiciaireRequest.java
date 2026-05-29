package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-214-37 : requête POST pour l'analyse d'une interdiction du territoire
 * français (ITF) prononcée par un juge pénal (C. pén. 131-30). Outil
 * single-country FR.
 *
 * @param dateCondamnation date de la condamnation pénale (requise).
 * @param dureeITFAnnees durée de l'ITF en années (0 si définitive / non renseignée).
 * @param infractionPrincipale infraction principale ayant motivé l'ITF (≤ 200 car., optionnel).
 * @param condamnationDefinitive true si la condamnation est définitive.
 * @param dateEcheanceRecoursPenal date d'expiration du délai de recours pénal (optionnel).
 */
public record ItfJudiciaireRequest(
        LocalDate dateCondamnation,
        int dureeITFAnnees,
        String infractionPrincipale,
        boolean condamnationDefinitive,
        LocalDate dateEcheanceRecoursPenal
) {}
