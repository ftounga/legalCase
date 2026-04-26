package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-FA-24-11 : réponse de l'API indivision successorale (art. 815 à 832-2 +
 * 1873-1 et s. Cciv).
 */
public record IndivisionSuccessoraleResponse(
        UUID caseFileId,
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
        List<String> messages,
        String country
) {}
