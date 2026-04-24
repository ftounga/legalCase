package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * SF-DT-21-01 : calculateur de l'indemnité forfaitaire pour travail dissimulé
 * — art. L.8223-1 Code du travail.
 *
 * Formule : indemnité = 6 mois × salaire mensuel de référence.
 *
 * Le salaire de référence est typiquement le plus favorable entre le salaire moyen
 * des 12 derniers mois et celui des 3 derniers mois (jurisprudence constante).
 * Le choix est laissé à l'avocat — l'outil ne réalise pas ce calcul.
 */
public final class IndemniteTravailDissimuleCalculator {

    private static final BigDecimal SIX_MONTHS = new BigDecimal("6");
    private static final String ARTICLE = "Art. L.8223-1 Code du travail";

    private static final List<String> MESSAGES = List.of(
            "Indemnité forfaitaire cumulable avec les indemnités de rupture (licenciement, préavis, congés payés) selon la jurisprudence constante (Cass. soc., ch. mixte, 26 mars 2010).",
            "Condition d'application : rupture de la relation de travail ET infraction caractérisée (intention de l'employeur, méconnaissance L.8221-3 non-déclaration URSSAF ou L.8221-5 dissimulation d'heures).",
            "Non cumulable avec l'indemnité forfaitaire pour défaut de visite médicale (art. L.4624-1)."
    );

    private IndemniteTravailDissimuleCalculator() {}

    public static TravailDissimuleResult compute(BigDecimal salaireMensuelReference) {
        if (salaireMensuelReference == null || salaireMensuelReference.signum() <= 0) {
            throw new IllegalArgumentException("Salaire mensuel de référence requis et strictement positif");
        }

        BigDecimal salaireRounded = salaireMensuelReference.setScale(2, RoundingMode.HALF_UP);
        BigDecimal indemnite = salaireRounded.multiply(SIX_MONTHS)
                .setScale(2, RoundingMode.HALF_UP);

        String formule = String.format("6 mois × %s € = %s €",
                formatMontant(salaireRounded),
                formatMontant(indemnite));

        return new TravailDissimuleResult(salaireRounded, indemnite, formule, ARTICLE, MESSAGES);
    }

    private static String formatMontant(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
