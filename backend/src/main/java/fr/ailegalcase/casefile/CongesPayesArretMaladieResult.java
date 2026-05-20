package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-206-03 : résultat structuré du calcul de rappel de congés payés acquis
 * pendant un arrêt maladie (FRANCE).
 */
public record CongesPayesArretMaladieResult(
        BigDecimal joursCpAcquis,
        BigDecimal joursCpRappel,
        BigDecimal valorisationIndicativeEur,
        LocalDate dateLimiteAction,
        boolean actionEncoreOuverte,
        CongesPayesArretMaladieCalculator.Verdict verdict,
        List<String> basesJuridiques,
        List<String> messages,
        String country
) {}
