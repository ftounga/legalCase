package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-19-03 : requête de calcul d'acceptabilité d'un changement de résidence
 * de l'enfant suite au déménagement d'un parent (art. 373-2 al. 3 Cciv).
 */
public record ChangementResidenceRequest(
        LocalDate dateChangementPrevu,
        Integer distanceKm,
        String raisonChangement,
        Boolean consentementAutreParent,
        Boolean informePrealablement,
        Integer delaiInformationJours,
        String modeResidenceActuel,
        List<Integer> ageEnfants,
        Boolean scolariteImpactee,
        Boolean modificationDvhDemandee
) {}
