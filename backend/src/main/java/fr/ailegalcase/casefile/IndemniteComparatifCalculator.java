package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcule les fourchettes d'indemnités jurisprudentielles à partir du profil salarié.
 */
public final class IndemniteComparatifCalculator {

    private IndemniteComparatifCalculator() {}

    public static IndemniteComparatifResult calculate(
            String country,
            int ancienneteAnnees,
            int age,
            BigDecimal salaireMensuel
    ) {
        if (!"FRANCE".equals(country) && !"BELGIQUE".equals(country)) {
            throw new IllegalArgumentException("Pays non supporté : " + country);
        }

        if ("FRANCE".equals(country)) {
            return calculateFrance(ancienneteAnnees, age, salaireMensuel);
        } else {
            return calculateBelgique(ancienneteAnnees, age, salaireMensuel);
        }
    }

    private static IndemniteComparatifResult calculateFrance(int anciennete, int age, BigDecimal salaire) {
        IndemniteBareme bareme = IndemniteJurisprudentielReferentiel.getBaremeMacron(anciennete);
        BigDecimal[] fourchette = IndemniteJurisprudentielReferentiel.getFourchetteFrance(anciennete, age);

        return new IndemniteComparatifResult(
                "FRANCE",
                anciennete, age, salaire,
                bareme.plancherMois(), bareme.plafondMois(),
                fourchette[0], fourchette[1], fourchette[2],
                salaire.multiply(fourchette[0]).setScale(2, RoundingMode.HALF_UP),
                salaire.multiply(fourchette[1]).setScale(2, RoundingMode.HALF_UP),
                salaire.multiply(fourchette[2]).setScale(2, RoundingMode.HALF_UP),
                "Barème Macron (art. L. 1235-3 Code du travail) — entreprises ≥ 11 salariés",
                age >= 50
                        ? "Salarié de 50 ans ou plus : les juridictions accordent souvent une indemnité dans la partie haute de la fourchette."
                        : "Fourchette indicative basée sur les tendances jurisprudentielles observées aux prud'hommes."
        );
    }

    private static IndemniteComparatifResult calculateBelgique(int anciennete, int age, BigDecimal salaire) {
        BigDecimal[] fourchette = IndemniteJurisprudentielReferentiel.getFourchetteBelgique(anciennete, age);

        // CCT 109 : 3-17 semaines → en mois
        BigDecimal diviseur = new BigDecimal("4.33");
        BigDecimal plancherMois = IndemniteJurisprudentielReferentiel.getCct109MinSemaines()
                .divide(diviseur, 2, RoundingMode.HALF_UP);
        BigDecimal plafondMois = IndemniteJurisprudentielReferentiel.getCct109MaxSemaines()
                .divide(diviseur, 2, RoundingMode.HALF_UP);

        return new IndemniteComparatifResult(
                "BELGIQUE",
                anciennete, age, salaire,
                plancherMois, plafondMois,
                fourchette[0], fourchette[1], fourchette[2],
                salaire.multiply(fourchette[0]).setScale(2, RoundingMode.HALF_UP),
                salaire.multiply(fourchette[1]).setScale(2, RoundingMode.HALF_UP),
                salaire.multiply(fourchette[2]).setScale(2, RoundingMode.HALF_UP),
                "CCT n° 109 du 12 février 2014 — indemnité pour licenciement manifestement déraisonnable (3 à 17 semaines)",
                anciennete >= 10
                        ? "Ancienneté significative : les tribunaux du travail tendent vers la partie haute de la fourchette CCT 109."
                        : "Fourchette indicative basée sur les tendances jurisprudentielles des tribunaux du travail."
        );
    }
}
