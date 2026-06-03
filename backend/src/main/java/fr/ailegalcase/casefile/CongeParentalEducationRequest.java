package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-218-45 : requête POST pour l'analyse du congé parental d'éducation
 * (art. L.1225-47 à L.1225-60 CT, F-DT-78). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param ancienneteMois ancienneté du salarié à la date de naissance / adoption,
 *        en mois (requis, &ge; 0 ; &ge; 12 requis pour l'éligibilité, L.1225-47).
 * @param modalite modalité d'exercice (requis) ∈ {TEMPS_PLEIN, TEMPS_PARTIEL}.
 * @param nombreEnfants nombre d'enfants concernés (requis, &ge; 1).
 * @param dateNaissanceOuAdoption date de naissance ou d'arrivée de l'enfant au
 *        foyer (requis).
 */
public record CongeParentalEducationRequest(
        Integer ancienneteMois,
        CongeParentalEducationModalite modalite,
        Integer nombreEnfants,
        LocalDate dateNaissanceOuAdoption
) {}
