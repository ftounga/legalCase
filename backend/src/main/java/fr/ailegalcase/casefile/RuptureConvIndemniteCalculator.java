package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * SF-132-01 : calculateur dédié à l'indemnité minimum légale de licenciement
 * applicable à une rupture conventionnelle (France).
 *
 * Formule art. R1234-2 Code du travail :
 *   - 1/4 de mois de salaire par année × min(10, ancienneté)
 *   - + 1/3 de mois de salaire par année × max(0, ancienneté - 10)
 *
 * Seuil d'ouverture : 1 an d'ancienneté (approximation conservée depuis
 * l'ancien {@code IndemniteComparatifCalculator.computeIndemniteLegaleLicenciement} ;
 * le seuil légal strict est 8 mois continus, art. L1234-9).
 */
public final class RuptureConvIndemniteCalculator {

    private static final BigDecimal QUARTER = new BigDecimal(4);
    private static final BigDecimal THIRD = new BigDecimal(3);

    private RuptureConvIndemniteCalculator() {}

    public static RuptureConvIndemniteResult computeMinimumLegal(int ancienneteAnnees, BigDecimal salaireMensuel) {
        if (salaireMensuel == null) {
            throw new IllegalArgumentException("salaireMensuel requis");
        }

        List<String> messages = List.of(
                "L'indemnité spécifique de rupture conventionnelle doit être au moins égale à l'indemnité légale de licenciement (art. L1237-13).",
                "Vérifier l'indemnité conventionnelle si la convention collective prévoit une indemnité plus favorable."
        );
        String baseJuridique = "Art. R1234-2 Code du travail";

        if (ancienneteAnnees < 1 || salaireMensuel.signum() <= 0) {
            return new RuptureConvIndemniteResult(
                    Math.max(ancienneteAnnees, 0),
                    salaireMensuel.signum() < 0 ? BigDecimal.ZERO : salaireMensuel,
                    BigDecimal.ZERO,
                    "Indemnité légale non due (ancienneté inférieure à 1 an ou salaire non renseigné).",
                    baseJuridique,
                    messages);
        }

        int part1Years = Math.min(10, ancienneteAnnees);
        int part2Years = Math.max(0, ancienneteAnnees - 10);

        BigDecimal part1 = salaireMensuel.multiply(new BigDecimal(part1Years))
                .divide(QUARTER, 4, RoundingMode.HALF_UP);
        BigDecimal part2 = salaireMensuel.multiply(new BigDecimal(part2Years))
                .divide(THIRD, 4, RoundingMode.HALF_UP);
        BigDecimal indemnite = part1.add(part2).setScale(2, RoundingMode.HALF_UP);

        String formule = part2Years > 0
                ? String.format("¼ × %d ans × %s € + ⅓ × %d ans × %s €",
                        part1Years, salaireMensuel.toPlainString(),
                        part2Years, salaireMensuel.toPlainString())
                : String.format("¼ × %d ans × %s €",
                        part1Years, salaireMensuel.toPlainString());

        return new RuptureConvIndemniteResult(
                ancienneteAnnees, salaireMensuel, indemnite, formule, baseJuridique, messages);
    }
}
