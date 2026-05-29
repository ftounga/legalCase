package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-214-41 : requête POST pour l'analyse de validité d'un retrait de titre de
 * séjour pour fraude (art. L. 412-7 CESEDA). Outil single-country FR.
 *
 * @param dateRetrait date de la décision de retrait notifiée (requise).
 * @param motifRetrait motif invoqué par l'administration.
 * @param miseEnDemeurePrealable true si un contradictoire préalable a été conduit.
 * @param dateMiseEnDemeure date de la mise en demeure / invitation à présenter
 *        ses observations (optionnel).
 */
public record RetraitTitreFraudeRequest(
        LocalDate dateRetrait,
        RetraitTitreFraudeMotifEnum motifRetrait,
        boolean miseEnDemeurePrealable,
        LocalDate dateMiseEnDemeure
) {}
