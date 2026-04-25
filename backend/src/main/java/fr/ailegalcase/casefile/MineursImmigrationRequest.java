package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-IM-19-01 : requête pour l'analyse d'éligibilité d'un mineur étranger
 * à l'un des 4 dispositifs (MNA, L.435-3, DCEM, TIR). Outil single-country FR.
 *
 * @param dispositifVise     enum : MNA_ORDONNANCE_JE / TITRE_SEJOUR_L435_3 / DCEM / TIR
 * @param dateNaissance      date de naissance du mineur (obligatoire, dans le passé)
 * @param dateEntreeFrance   date d'entrée en France (requise pour L.435-3, ≥ dateNaissance)
 * @param parentRegulier     au moins un parent en situation régulière (L.435-3)
 * @param isolementAvere     pas d'adulte référent (MNA)
 * @param motifOrdrePublic   motif d'ordre public (bloquant pour DCEM)
 * @param nationalite        nationalité (informationnel, utile pour TIR apatride)
 */
public record MineursImmigrationRequest(
        String dispositifVise,
        LocalDate dateNaissance,
        LocalDate dateEntreeFrance,
        Boolean parentRegulier,
        Boolean isolementAvere,
        Boolean motifOrdrePublic,
        String nationalite
) {}
