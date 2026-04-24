package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-DT-12-01 : résultat du calcul indicatif de dommages-intérêts pour
 * acte discriminatoire (art. L.1134-5 FR, Loi 10 mai 2007 BE).
 */
public record DiscriminationResult(
        BigDecimal salaireMensuelReference,
        String motifDiscrimination,
        String contexteActe,
        String country,
        BigDecimal fourchetteMin,
        BigDecimal fourchetteMediane,
        BigDecimal fourchetteMax,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
