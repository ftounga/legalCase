package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-220-05 : requête POST pour l'outil décisionnel « validité d'une mesure de
 * déchéance de nationalité » (F-IM-51-decheance-nationalite-fr, Cciv 25 / 25-1).
 * Outil single-country FR.
 *
 * <p>{@code motif} est un code enum validé en amont
 * (TERRORISME / ATTEINTE_INTERETS_NATION / FRAUDE_ACQUISITION / AUTRE).</p>
 */
public record DecheanceNationaliteRequest(
        String motif,
        Boolean binational,
        LocalDate dateAcquisitionNationalite,
        LocalDate dateFaits,
        Boolean mesurePrononcee,
        LocalDate dateDecret
) {}
