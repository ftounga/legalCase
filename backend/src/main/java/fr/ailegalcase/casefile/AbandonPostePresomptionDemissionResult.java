package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-206-01 : résultat structuré de l'analyse de contestation d'une présomption
 * de démission par abandon de poste (FRANCE).
 */
public record AbandonPostePresomptionDemissionResult(
        List<AbandonPostePresomptionDemissionCalculator.MotifContestation> motifsContestation,
        int scoreContestation,
        AbandonPostePresomptionDemissionCalculator.Verdict verdict,
        LocalDate dateExpirationDelai,
        List<String> basesJuridiques,
        List<String> messages,
        String country
) {}
