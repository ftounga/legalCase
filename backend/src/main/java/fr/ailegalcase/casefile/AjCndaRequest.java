package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-214-19 : requête POST pour l'analyse d'éligibilité à l'aide juridictionnelle
 * (AJ) devant la CNDA et le calcul des délais (loi n° 91-647, L. 532-4 CESEDA).
 * Outil single-country FR.
 *
 * @param dateDecisionOFPRA date de notification de la décision de l'OFPRA — point
 *        de départ des délais (requise).
 * @param ressourcesMensuellesNettes ressources mensuelles nettes du demandeur (€).
 * @param procedureAcceleree true si la demande a été examinée en procédure accélérée
 *        (délai de recours CNDA réduit à 15 j, L. 532-4 CESEDA).
 * @param demandeAJDeposee true si la demande d'AJ a déjà été déposée.
 * @param dateDepotAJ date de dépôt de la demande d'AJ (optionnelle).
 */
public record AjCndaRequest(
        LocalDate dateDecisionOFPRA,
        double ressourcesMensuellesNettes,
        boolean procedureAcceleree,
        boolean demandeAJDeposee,
        LocalDate dateDepotAJ
) {}
