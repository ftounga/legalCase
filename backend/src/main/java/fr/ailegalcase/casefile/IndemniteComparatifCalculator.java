package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Calcule les fourchettes d'indemnités selon pays et type de rupture.
 */
public final class IndemniteComparatifCalculator {

    static final Set<String> TYPES_RUPTURE_FR = Set.of(
            "LICENCIEMENT", "LICENCIEMENT_ECONOMIQUE");
    static final Set<String> TYPES_RUPTURE_BE = Set.of(
            "LICENCIEMENT_ORDINAIRE", "RUPTURE_AMIABLE");

    private IndemniteComparatifCalculator() {}

    public static IndemniteComparatifResult calculate(
            String country,
            String typeRupture,
            int ancienneteAnnees,
            int age,
            BigDecimal salaireMensuel
    ) {
        if (!"FRANCE".equals(country) && !"BELGIQUE".equals(country)) {
            throw new IllegalArgumentException("Pays non supporté : " + country);
        }
        validateTypeRupture(country, typeRupture);

        return "FRANCE".equals(country)
                ? calculateFrance(typeRupture, ancienneteAnnees, age, salaireMensuel)
                : calculateBelgique(typeRupture, ancienneteAnnees, age, salaireMensuel);
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

    private static IndemniteComparatifResult calculateFrance(String typeRupture, int anciennete, int age, BigDecimal salaire) {
        // SF-132-02 : la branche RUPTURE_CONVENTIONNELLE a été extraite vers
        // RuptureConvIndemniteCalculator (outil décisionnel dédié, F-132).
        // Ce calculateur ne gère plus que le barème Macron pour la France.
        IndemniteBareme bareme = IndemniteJurisprudentielReferentiel.getBaremeMacron(anciennete);
        BigDecimal[] fourchette = IndemniteJurisprudentielReferentiel.getFourchetteFrance(anciennete, age);

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

    private static IndemniteComparatifResult calculateBelgique(String typeRupture, int anciennete, int age, BigDecimal salaire) {
        if ("RUPTURE_AMIABLE".equals(typeRupture)) {
            return IndemniteComparatifResult.negociationLibre(
                    "BELGIQUE", typeRupture, anciennete, age, salaire,
                    "Rupture amiable (sans cadre légal spécifique)",
                    "Négociation libre entre les parties.",
                    List.of(
                            "Aucun barème ne s'impose en rupture amiable belge.",
                            "Le salarié conserve le droit à l'indemnité compensatoire de préavis si la rupture n'est pas effective (cf. F-DT-05)."
                    ));
        }

        // LICENCIEMENT_ORDINAIRE : fourchette CCT 109
        BigDecimal[] fourchette = IndemniteJurisprudentielReferentiel.getFourchetteBelgique(anciennete, age);
        BigDecimal diviseur = new BigDecimal("4.33");
        BigDecimal plancherMois = IndemniteJurisprudentielReferentiel.getCct109MinSemaines()
                .divide(diviseur, 2, RoundingMode.HALF_UP);
        BigDecimal plafondMois = IndemniteJurisprudentielReferentiel.getCct109MaxSemaines()
                .divide(diviseur, 2, RoundingMode.HALF_UP);

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
}
