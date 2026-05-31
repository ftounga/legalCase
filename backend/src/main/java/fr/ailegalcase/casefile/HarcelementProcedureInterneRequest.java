package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-218-27 : requête POST pour l'analyse de conformité de la procédure interne
 * de traitement d'un signalement de harcèlement (art. L.1153-5-1, L.2314-1,
 * L.1152-4, L.4121-1 CT, F-DT-59). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param effectif effectif de l'entreprise (requis, &gt; 0). À partir de 11
 *        salariés, le CSE est obligatoire et doit désigner un référent
 *        harcèlement sexuel (L.2314-1).
 * @param referentCseDesigne true si un référent harcèlement sexuel a été désigné
 *        parmi les membres du CSE (requis).
 * @param referentEmployeurDesigne true si un référent employeur a été désigné
 *        (obligatoire pour les entreprises ≥ 250 salariés, L.1153-5-1 ; défaut
 *        false).
 * @param informationAffichageRealisee true si l'information / l'affichage des
 *        salariés sur les sanctions et voies de recours a été réalisé (L.1152-4,
 *        L.1153-5 ; requis).
 * @param signalementRecu true si un signalement de harcèlement a été reçu
 *        (requis).
 * @param dateSignalement date de réception du signalement (optionnelle).
 * @param dateOuvertureEnquete date d'ouverture de l'enquête interne (optionnelle,
 *        ≥ dateSignalement).
 * @param enqueteContradictoire true si l'enquête a été menée de façon
 *        contradictoire et impartiale (défaut false).
 * @param mesuresConservatoiresPrises true si des mesures conservatoires
 *        (éloignement, suspension) ont été prises durant l'enquête (défaut false).
 */
public record HarcelementProcedureInterneRequest(
        Integer effectif,
        Boolean referentCseDesigne,
        Boolean referentEmployeurDesigne,
        Boolean informationAffichageRealisee,
        Boolean signalementRecu,
        LocalDate dateSignalement,
        LocalDate dateOuvertureEnquete,
        Boolean enqueteContradictoire,
        Boolean mesuresConservatoiresPrises
) {}
