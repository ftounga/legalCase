package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-19-01 : requête de calcul d'éligibilité au changement de régime
 * d'exercice de l'autorité parentale (art. 372-373 Cciv).
 */
public record AutoriteParentaleRequest(
        String regimeExerciceActuel,
        String regimeExerciceDemande,
        String motifChangement,
        Boolean dangerCaracterise,
        List<String> preuvesProduites,
        List<Integer> ageEnfants,
        Boolean consentementAutreParent,
        Boolean interferenceVieEnfant,
        LocalDate dateRequete
) {}
