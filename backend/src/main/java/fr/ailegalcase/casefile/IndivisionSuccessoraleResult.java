package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-24-11 : résultat de l'analyse d'une indivision successorale (art. 815 à
 * 832-2 + 1873-1 et s. Cciv). Outil single-country FR (DROIT_FAMILLE).
 */
public record IndivisionSuccessoraleResult(
        LocalDate dateOuvertureSuccession,
        String typeIndivision,
        int nbHeritiers,
        BigDecimal valeurPatrimoineIndivisEur,
        BigDecimal valeurBienOccupeEur,
        boolean consentementsTous,
        boolean occupationExclusive,
        boolean actesAdministrationContestes,
        boolean demandePartage,
        int dureeIndivisionMois,
        String verdictGestion,
        String dispositifRecommande,
        boolean indemniteOccupationDue,
        BigDecimal indemniteOccupationDueEur,
        BigDecimal fraisGestionEstimesEur,
        int scoreConflictualite,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
