package fr.ailegalcase.casefile;

import java.util.List;

/**
 * Référentiel statique des critères de validité de la rupture conventionnelle (F-DT-10).
 *
 * Scope V2 initial : France uniquement (art. L1237-11 et suivants du Code du travail).
 * La somme des poids est 100 → le score du risque s'obtient directement sans normalisation.
 */
public final class RuptureConvCritereReferentiel {

    private RuptureConvCritereReferentiel() {}

    private static final List<RuptureConvCritere> ALL = List.of(

            new RuptureConvCritere("RC_CONSENTEMENT", "Consentement libre et éclairé", "FRANCE",
                    "Le consentement du salarié est-il exempt de vice (pression, dol, violence morale, erreur) ? "
                            + "Des échanges, pièces ou circonstances laissent-ils présumer une contrainte ?",
                    25, true,
                    "Art. L1237-11, 1er alinéa du Code du travail ; Cass. soc. 29 janv. 2020, n° 18-24.558"),

            new RuptureConvCritere("RC_DELAI_RETRACTATION", "Délai de rétractation de 15 jours calendaires respecté",
                    "FRANCE",
                    "Un délai de 15 jours calendaires a-t-il été respecté entre la signature de la convention et "
                            + "la demande d'homologation adressée à l'administration ?",
                    20, true,
                    "Art. L1237-13, 2e alinéa du Code du travail"),

            new RuptureConvCritere("RC_HOMOLOGATION", "Homologation par la DREETS (ex-DIRECCTE)", "FRANCE",
                    "La demande d'homologation a-t-elle été déposée auprès de la DREETS compétente et "
                            + "l'homologation a-t-elle été obtenue (ou réputée acquise après 15 jours ouvrables sans réponse) ?",
                    25, true,
                    "Art. L1237-14 du Code du travail"),

            new RuptureConvCritere("RC_ASSISTANCE", "Assistance possible et documentée", "FRANCE",
                    "Le salarié a-t-il été informé de sa possibilité de se faire assister (avocat, conseiller du salarié, "
                            + "représentant du personnel) lors des entretiens, et cette assistance est-elle documentée ?",
                    10, false,
                    "Art. L1237-12, dernier alinéa du Code du travail"),

            new RuptureConvCritere("RC_INDEMNITE", "Indemnité spécifique supérieure ou égale à l'indemnité légale",
                    "FRANCE",
                    "L'indemnité spécifique de rupture conventionnelle est-elle au moins égale à l'indemnité légale "
                            + "de licenciement (¼ de mois par année les 10 premières années + ⅓ au-delà) ?",
                    15, true,
                    "Art. L1237-13, 1er alinéa du Code du travail"),

            new RuptureConvCritere("RC_ENTRETIENS", "Au moins un entretien préalable tenu et documenté", "FRANCE",
                    "Un entretien préalable a-t-il été organisé entre l'employeur et le salarié avant signature, "
                            + "et existe-t-il une trace écrite (compte-rendu, correspondance) ?",
                    5, false,
                    "Art. L1237-12, 1er alinéa du Code du travail")
    );

    public static List<RuptureConvCritere> getByCountry(String country) {
        if (country == null) return List.of();
        String normalized = country.toUpperCase();
        return ALL.stream().filter(c -> c.country().equals(normalized)).toList();
    }

    public static RuptureConvCritere getByCode(String code) {
        if (code == null) return null;
        String normalized = code.toUpperCase();
        return ALL.stream().filter(c -> c.code().equals(normalized)).findFirst().orElse(null);
    }

    public static List<RuptureConvCritere> getAll() {
        return ALL;
    }

    public static boolean isCountryValid(String country) {
        return "FRANCE".equals(country);
    }
}
