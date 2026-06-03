package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

public record OrdonnanceProtectionRequest(
        LocalDate dateRequete,
        List<String> violencesAlleguees,
        List<String> preuvesViolences,
        Boolean dangerImmediat,
        Boolean presenceEnfants,
        List<Integer> ageEnfants,
        Boolean logementCommun,
        Boolean victimeFinanciairementDependante,
        Boolean demandeurDejaProtege,
        List<String> demandeMesures,
        // SF-222-05 : l'avocat envisage-t-il un DEC (suivi électronique du
        // contact) ? Branche conditionnelle voisine du BAR (danger immédiat).
        Boolean decEnvisage
) {}
