package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * Référentiel des barèmes d'indemnités de licenciement : barème Macron (FR) et CCT 109 (BE).
 * Inclut des fourchettes jurisprudentielles observées.
 */
public final class IndemniteJurisprudentielReferentiel {

    private IndemniteJurisprudentielReferentiel() {}

    /**
     * Barème Macron (art. L. 1235-3 Code du travail) — plancher/plafond en mois de salaire brut.
     * Applicable aux entreprises de 11 salariés et plus.
     */
    private static final List<IndemniteBareme> BAREME_MACRON = List.of(
            new IndemniteBareme(0,  bd("0"),    bd("1")),
            new IndemniteBareme(1,  bd("1"),    bd("2")),
            new IndemniteBareme(2,  bd("3"),    bd("3.5")),
            new IndemniteBareme(3,  bd("3"),    bd("4")),
            new IndemniteBareme(4,  bd("3"),    bd("5")),
            new IndemniteBareme(5,  bd("3"),    bd("6")),
            new IndemniteBareme(6,  bd("3"),    bd("7")),
            new IndemniteBareme(7,  bd("3"),    bd("8")),
            new IndemniteBareme(8,  bd("3"),    bd("8")),
            new IndemniteBareme(9,  bd("3"),    bd("9")),
            new IndemniteBareme(10, bd("3"),    bd("10")),
            new IndemniteBareme(11, bd("3"),    bd("10.5")),
            new IndemniteBareme(12, bd("3"),    bd("11")),
            new IndemniteBareme(13, bd("3"),    bd("11.5")),
            new IndemniteBareme(14, bd("3"),    bd("12")),
            new IndemniteBareme(15, bd("3"),    bd("13")),
            new IndemniteBareme(16, bd("3"),    bd("13.5")),
            new IndemniteBareme(17, bd("3"),    bd("14")),
            new IndemniteBareme(18, bd("3"),    bd("14.5")),
            new IndemniteBareme(19, bd("3"),    bd("15")),
            new IndemniteBareme(20, bd("3"),    bd("15.5")),
            new IndemniteBareme(21, bd("3"),    bd("16")),
            new IndemniteBareme(22, bd("3"),    bd("16.5")),
            new IndemniteBareme(23, bd("3"),    bd("17")),
            new IndemniteBareme(24, bd("3"),    bd("17.5")),
            new IndemniteBareme(25, bd("3"),    bd("18")),
            new IndemniteBareme(26, bd("3"),    bd("18.5")),
            new IndemniteBareme(27, bd("3"),    bd("19")),
            new IndemniteBareme(28, bd("3"),    bd("19.5")),
            new IndemniteBareme(29, bd("3"),    bd("20"))
    );

    /**
     * Barème CCT 109 (Belgique) — indemnité pour licenciement manifestement déraisonnable.
     * Fourchette fixe : 3 à 17 semaines de rémunération.
     */
    private static final BigDecimal CCT109_MIN_SEMAINES = bd("3");
    private static final BigDecimal CCT109_MAX_SEMAINES = bd("17");

    public static IndemniteBareme getBaremeMacron(int ancienneteAnnees) {
        int capped = Math.min(ancienneteAnnees, 29);
        return BAREME_MACRON.stream()
                .filter(b -> b.ancienneteAnnees() == capped)
                .findFirst()
                .orElse(BAREME_MACRON.get(BAREME_MACRON.size() - 1));
    }

    public static List<IndemniteBareme> getAllBaremeMacron() {
        return BAREME_MACRON;
    }

    public static BigDecimal getCct109MinSemaines() {
        return CCT109_MIN_SEMAINES;
    }

    public static BigDecimal getCct109MaxSemaines() {
        return CCT109_MAX_SEMAINES;
    }

    /**
     * Fourchette jurisprudentielle observée (en mois de salaire) pour la France.
     * Basée sur l'ancienneté et l'âge, ajustée au-dessus du barème Macron plancher.
     */
    public static BigDecimal[] getFourchetteFrance(int ancienneteAnnees, int age) {
        IndemniteBareme bareme = getBaremeMacron(ancienneteAnnees);
        BigDecimal plancher = bareme.plancherMois();
        BigDecimal plafond = bareme.plafondMois();

        // Jurisprudence : médiane ~60% de la fourchette Macron, ajustée par âge
        BigDecimal range = plafond.subtract(plancher);
        BigDecimal ageBonus = age >= 50 ? bd("0.15") : age >= 40 ? bd("0.08") : bd("0");

        BigDecimal basse = plancher.add(range.multiply(bd("0.25")));
        BigDecimal mediane = plancher.add(range.multiply(bd("0.55").add(ageBonus)));
        BigDecimal haute = plancher.add(range.multiply(bd("0.85").add(ageBonus)));

        // Cap à plafond Macron
        haute = haute.min(plafond);
        mediane = mediane.min(haute);

        return new BigDecimal[]{basse, mediane, haute};
    }

    /**
     * Fourchette jurisprudentielle pour la Belgique (en mois de salaire).
     * CCT 109 : 3-17 semaines → converti en mois (~0.69-3.92 mois).
     */
    public static BigDecimal[] getFourchetteBelgique(int ancienneteAnnees, int age) {
        // Conversion semaines → mois (÷ 4.33)
        BigDecimal diviseur = bd("4.33");
        BigDecimal minMois = CCT109_MIN_SEMAINES.divide(diviseur, 2, java.math.RoundingMode.HALF_UP);
        BigDecimal maxMois = CCT109_MAX_SEMAINES.divide(diviseur, 2, java.math.RoundingMode.HALF_UP);

        // Ajustement jurisprudentiel par ancienneté et âge
        BigDecimal ancienneteBonus = ancienneteAnnees >= 10 ? bd("0.3") : ancienneteAnnees >= 5 ? bd("0.15") : bd("0");
        BigDecimal ageBonus = age >= 50 ? bd("0.25") : age >= 40 ? bd("0.1") : bd("0");

        BigDecimal range = maxMois.subtract(minMois);
        BigDecimal basse = minMois.add(range.multiply(bd("0.15")));
        BigDecimal mediane = minMois.add(range.multiply(bd("0.4").add(ancienneteBonus).add(ageBonus)));
        BigDecimal haute = minMois.add(range.multiply(bd("0.75").add(ancienneteBonus).add(ageBonus)));

        haute = haute.min(maxMois);
        mediane = mediane.min(haute);

        return new BigDecimal[]{basse, mediane, haute};
    }

    private static BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
