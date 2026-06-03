package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-218-45 : résultat interne business de l'analyse du congé parental
 * d'éducation (art. L.1225-47 à L.1225-60 CT, F-DT-78). Outil <b>FRANCE
 * UNIQUEMENT</b>.
 *
 * @param statut verdict d'éligibilité (ELIGIBLE / NON_ELIGIBLE).
 * @param ancienneteMois ancienneté retenue (mois).
 * @param modaliteRetenue modalité d'exercice retenue.
 * @param nombreEnfants nombre d'enfants concernés.
 * @param dateNaissanceOuAdoption date de naissance / d'arrivée de l'enfant.
 * @param dateFinMax date de fin maximale du droit (3e anniversaire de l'enfant),
 *        null si non éligible.
 * @param dureeMaxMois durée maximale du congé en mois (= 36 si éligible), 0 si
 *        non éligible.
 * @param protectionReintegration true — réintégration garantie dans le précédent
 *        emploi ou un emploi similaire (L.1225-55).
 * @param mentionPreparE true — information PreParE (CAF), montant non calculé.
 * @param notes notes / points de vigilance identifiés.
 * @param baseJuridique fondements juridiques applicables.
 */
public record CongeParentalEducationResult(
        CongeParentalEducationStatut statut,
        int ancienneteMois,
        CongeParentalEducationModalite modaliteRetenue,
        int nombreEnfants,
        LocalDate dateNaissanceOuAdoption,
        LocalDate dateFinMax,
        int dureeMaxMois,
        boolean protectionReintegration,
        boolean mentionPreparE,
        List<String> notes,
        String baseJuridique
) {}
