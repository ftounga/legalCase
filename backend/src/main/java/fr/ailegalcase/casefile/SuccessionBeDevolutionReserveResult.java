package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-217-11 : résultat structuré de l'arbre de dévolution / réserve belge.
 */
public record SuccessionBeDevolutionReserveResult(
        SuccessionBeDevolutionReserveCalculator.Verdict verdict,
        List<SuccessionBeDevolutionReserveCalculator.Heritier> heritiers,
        BigDecimal reserveGlobaleEur,
        String reserveGlobaleFraction,
        BigDecimal quotiteDisponibleEur,
        String quotiteDisponibleFraction,
        BigDecimal libertesConsentiesEur,
        BigDecimal depassementQuotiteEur,
        List<String> basesJuridiques,
        List<String> messages,
        String country
) {}
