package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-214-17 : requête POST pour l'analyse de la procédure d'introduction de la
 * demande d'asile à l'OFPRA (GUDA / ADA, art. R. 521-1 et s. CESEDA).
 * Outil single-country FR.
 *
 * @param paysOrigine pays d'origine du demandeur (libellé libre) — utilisé pour
 *        détecter le risque de procédure accélérée (pays sûr, L. 531-24).
 *        Optionnel : null/blanc ⇒ pas de présomption de pays sûr.
 */
public record OfpraIntroductionRequest(
        LocalDate dateArriveeEnFrance,
        Boolean passageGudaEffectue,
        LocalDate datePassageGuda,
        Boolean adaRequise,
        String paysOrigine
) {}
