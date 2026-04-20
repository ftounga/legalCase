package fr.ailegalcase.casefile;

import fr.ailegalcase.referential.LegalReferentialService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Calcule les fourchettes d'indemnités selon pays et type de rupture.
 *
 * <p>SF-139-01 : référentiels lus via {@link LegalReferentialService} (DB only).
 * Le calculator reste une fonction pure : il reçoit le barème Macron et/ou le
 * range CCT 109 en paramètre. Le service appelant est responsable du lookup DB.
 */
public final class IndemniteComparatifCalculator {

    static final Set<String> TYPES_RUPTURE_FR = Set.of(
            "LICENCIEMENT", "LICENCIEMENT_ECONOMIQUE");
    static final Set<String> TYPES_RUPTURE_BE = Set.of(
            "LICENCIEMENT_ORDINAIRE");

    private IndemniteComparatifCalculator() {}

    public static IndemniteComparatifResult calculate(
            String country,
            String typeRupture,
            int anciennete,
            int age,
            BigDecimal salaireMensuel,
            IndemniteBareme macronBareme,
            LegalReferentialService.Cct109Range cctRange
    ) {
        if (!"FRANCE".equals(country) && !"BELGIQUE".equals(country)) {
            throw new IllegalArgumentException("Pays non supporté : " + country);
        }
        validateTypeRupture(country, typeRupture);

        return "FRANCE".equals(country)
                ? calculateFrance(typeRupture, anciennete, age, salaireMensuel, macronBareme)
                : calculateBelgique(typeRupture, anciennete, age, salaireMensuel, cctRange);
    }

    private static void validateTypeRupture(String country, String typeRupture) {
        if (typeRupture == null || typeRupture.isBlank()) {
            throw new IllegalArgumentException("typeRupture requis");
        }
        Set<String> allowed = "FRANCE".equals(country) ? TYPES_RUPTURE_FR : TYPES_RUPTURE_BE;
        if (!allowed.contains(typeRupture)) {
            throw new IllegalArgumentException("typeRupture '" + typeRupture + "' invalide pour " + country);
        }
    }

    private static IndemniteComparatifResult calculateFrance(String typeRupture, int anciennete, int age,
                                                              BigDecimal salaire, IndemniteBareme bareme) {
        if (bareme == null) {
            throw new IllegalStateException("Barème Macron non trouvé en DB (INDEMNITE_BAREMES/MACRON)");
        }

        BigDecimal[] fourchette = computeFourchetteFrance(bareme, age);

        List<String> messages = new ArrayList<>();
        if ("LICENCIEMENT_ECONOMIQUE".equals(typeRupture)) {
            messages.add("Licenciement économique : vérifier l'indemnité conventionnelle (souvent plus favorable), les obligations de PSE et la priorité de réembauche.");
        } else {
            messages.add("Licenciement : la fourchette représente les dommages-intérêts potentiels si le licenciement est jugé sans cause réelle et sérieuse.");
        }

        return new IndemniteComparatifResult(
                "FRANCE", typeRupture, anciennete, age, salaire,
                "MACRON",
                bareme.plancherMois(), bareme.plafondMois(),
                fourchette[0], fourchette[1], fourchette[2],
                salaire.multiply(fourchette[0]).setScale(2, RoundingMode.HALF_UP),
                salaire.multiply(fourchette[1]).setScale(2, RoundingMode.HALF_UP),
                salaire.multiply(fourchette[2]).setScale(2, RoundingMode.HALF_UP),
                null,
                "Barème Macron (art. L. 1235-3 Code du travail) — entreprises ≥ 11 salariés",
                age >= 50
                        ? "Salarié de 50 ans ou plus : les juridictions accordent souvent une indemnité dans la partie haute de la fourchette."
                        : "Fourchette indicative basée sur les tendances jurisprudentielles observées aux prud'hommes.",
                messages);
    }

    private static IndemniteComparatifResult calculateBelgique(String typeRupture, int anciennete, int age,
                                                                BigDecimal salaire, LegalReferentialService.Cct109Range cctRange) {
        if (cctRange == null) {
            throw new IllegalStateException("Barème CCT 109 non trouvé en DB (INDEMNITE_BAREMES/CCT109)");
        }

        BigDecimal[] fourchette = computeFourchetteBelgique(cctRange, anciennete, age);
        BigDecimal diviseur = new BigDecimal("4.33");
        BigDecimal plancherMois = cctRange.minSemaines().divide(diviseur, 2, RoundingMode.HALF_UP);
        BigDecimal plafondMois = cctRange.maxSemaines().divide(diviseur, 2, RoundingMode.HALF_UP);

        List<String> messages = List.of(
                "Indemnité pour licenciement manifestement déraisonnable (CCT 109)."
        );

        return new IndemniteComparatifResult(
                "BELGIQUE", typeRupture, anciennete, age, salaire,
                "CCT_109",
                plancherMois, plafondMois,
                fourchette[0], fourchette[1], fourchette[2],
                salaire.multiply(fourchette[0]).setScale(2, RoundingMode.HALF_UP),
                salaire.multiply(fourchette[1]).setScale(2, RoundingMode.HALF_UP),
                salaire.multiply(fourchette[2]).setScale(2, RoundingMode.HALF_UP),
                null,
                "CCT n° 109 du 12 février 2014 — indemnité pour licenciement manifestement déraisonnable (3 à 17 semaines)",
                anciennete >= 10
                        ? "Ancienneté significative : les tribunaux du travail tendent vers la partie haute de la fourchette CCT 109."
                        : "Fourchette indicative basée sur les tendances jurisprudentielles des tribunaux du travail.",
                messages);
    }

    /**
     * Fourchette jurisprudentielle observée (en mois de salaire) pour la France.
     * Basée sur le barème Macron plancher/plafond, ajustée par âge.
     */
    public static BigDecimal[] computeFourchetteFrance(IndemniteBareme bareme, int age) {
        BigDecimal plancher = bareme.plancherMois();
        BigDecimal plafond = bareme.plafondMois();

        BigDecimal range = plafond.subtract(plancher);
        BigDecimal ageBonus = age >= 50 ? new BigDecimal("0.15")
                : age >= 40 ? new BigDecimal("0.08")
                : BigDecimal.ZERO;

        BigDecimal basse = plancher.add(range.multiply(new BigDecimal("0.25")));
        BigDecimal mediane = plancher.add(range.multiply(new BigDecimal("0.55").add(ageBonus)));
        BigDecimal haute = plancher.add(range.multiply(new BigDecimal("0.85").add(ageBonus)));

        haute = haute.min(plafond);
        mediane = mediane.min(haute);

        return new BigDecimal[]{basse, mediane, haute};
    }

    /**
     * Fourchette jurisprudentielle pour la Belgique (en mois de salaire), basée
     * sur le range CCT 109 converti en mois (÷ 4.33).
     */
    public static BigDecimal[] computeFourchetteBelgique(LegalReferentialService.Cct109Range cctRange,
                                                          int ancienneteAnnees, int age) {
        BigDecimal diviseur = new BigDecimal("4.33");
        BigDecimal minMois = cctRange.minSemaines().divide(diviseur, 2, RoundingMode.HALF_UP);
        BigDecimal maxMois = cctRange.maxSemaines().divide(diviseur, 2, RoundingMode.HALF_UP);

        BigDecimal ancienneteBonus = ancienneteAnnees >= 10 ? new BigDecimal("0.3")
                : ancienneteAnnees >= 5 ? new BigDecimal("0.15")
                : BigDecimal.ZERO;
        BigDecimal ageBonus = age >= 50 ? new BigDecimal("0.25")
                : age >= 40 ? new BigDecimal("0.1")
                : BigDecimal.ZERO;

        BigDecimal range = maxMois.subtract(minMois);
        BigDecimal basse = minMois.add(range.multiply(new BigDecimal("0.15")));
        BigDecimal mediane = minMois.add(range.multiply(new BigDecimal("0.4").add(ancienneteBonus).add(ageBonus)));
        BigDecimal haute = minMois.add(range.multiply(new BigDecimal("0.75").add(ancienneteBonus).add(ageBonus)));

        haute = haute.min(maxMois);
        mediane = mediane.min(haute);

        return new BigDecimal[]{basse, mediane, haute};
    }
}
