package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * Arbre de décision : à partir de critères (pays, nationalité UE, motif, durée, situation familiale),
 * retourne une liste ordonnée de titres de séjour recommandés.
 */
public final class ImmigrationTitleDecisionEngine {

    private ImmigrationTitleDecisionEngine() {}

    /**
     * Résout les titres recommandés en fonction des critères.
     *
     * @param country           FRANCE ou BELGIQUE
     * @param nationaliteUE     true si ressortissant UE/EEE/Suisse
     * @param motif             TRAVAIL, ETUDES, FAMILLE, ASILE, AUTRE
     * @param duree             COURT_SEJOUR (< 1 an) ou LONG_SEJOUR (>= 1 an)
     * @param situationFamiliale CELIBATAIRE, MARIE, PACS_COHABITATION (nullable)
     * @return liste ordonnée de recommandations (max 3), vide si aucun titre applicable
     */
    public static List<TitleRecommendation> resolve(
            String country,
            boolean nationaliteUE,
            String motif,
            String duree,
            String situationFamiliale
    ) {
        if (!ImmigrationTitleReferentiel.isCountryValid(country)) {
            return List.of();
        }
        if (!ImmigrationTitleReferentiel.isMotifValid(motif)) {
            return List.of();
        }

        // Ressortissants UE/EEE/Suisse : libre circulation, pas de titre nécessaire pour court séjour
        if (nationaliteUE && "COURT_SEJOUR".equals(duree)) {
            return List.of();
        }

        if ("FRANCE".equals(country)) {
            return resolveFrance(nationaliteUE, motif, duree, situationFamiliale);
        } else {
            return resolveBelgique(nationaliteUE, motif, duree, situationFamiliale);
        }
    }

    private static List<TitleRecommendation> resolveFrance(
            boolean nationaliteUE, String motif, String duree, String situationFamiliale) {

        List<TitleRecommendation> results = new ArrayList<>();

        // UE long séjour : carte de résident directement accessible
        if (nationaliteUE) {
            addIfExists(results, "CARTE_RESIDENT");
            return cap(results);
        }

        switch (motif) {
            case "TRAVAIL" -> {
                if ("COURT_SEJOUR".equals(duree)) {
                    addIfExists(results, "VLS_TS_SALARIE");
                } else {
                    addIfExists(results, "VLS_TS_SALARIE");
                    addIfExists(results, "CST_SALARIE");
                    addIfExists(results, "CARTE_PLURIANNUELLE");
                }
            }
            case "ETUDES" -> {
                addIfExists(results, "VLS_TS_ETUDIANT");
                if ("LONG_SEJOUR".equals(duree)) {
                    addIfExists(results, "APS");
                }
            }
            case "FAMILLE" -> {
                addIfExists(results, "CST_VPF");
                if ("LONG_SEJOUR".equals(duree)) {
                    addIfExists(results, "CARTE_RESIDENT");
                }
            }
            case "ASILE" -> {
                addIfExists(results, "RECEPISSE_ASILE");
            }
            case "AUTRE" -> {
                if ("LONG_SEJOUR".equals(duree)) {
                    addIfExists(results, "CARTE_RESIDENT");
                    addIfExists(results, "APS");
                } else {
                    addIfExists(results, "APS");
                }
            }
        }

        return cap(results);
    }

    private static List<TitleRecommendation> resolveBelgique(
            boolean nationaliteUE, String motif, String duree, String situationFamiliale) {

        List<TitleRecommendation> results = new ArrayList<>();

        // UE long séjour : carte E+ (assimilée carte B dans notre référentiel)
        if (nationaliteUE) {
            addIfExists(results, "CARTE_B");
            return cap(results);
        }

        switch (motif) {
            case "TRAVAIL" -> {
                addIfExists(results, "PERMIS_UNIQUE");
                if ("LONG_SEJOUR".equals(duree)) {
                    addIfExists(results, "CARTE_A_TRAVAIL");
                    addIfExists(results, "CARTE_B");
                }
            }
            case "ETUDES" -> {
                addIfExists(results, "CARTE_A_ETUDES");
            }
            case "FAMILLE" -> {
                addIfExists(results, "CARTE_A_FAMILLE");
                if ("LONG_SEJOUR".equals(duree)) {
                    addIfExists(results, "CARTE_B");
                }
            }
            case "ASILE" -> {
                addIfExists(results, "ATTESTATION_IMMATRICULATION");
                addIfExists(results, "ANNEXE_15");
            }
            case "AUTRE" -> {
                if ("LONG_SEJOUR".equals(duree)) {
                    addIfExists(results, "CARTE_B");
                    addIfExists(results, "CARTE_C");
                } else {
                    addIfExists(results, "ANNEXE_15");
                }
            }
        }

        return cap(results);
    }

    private static void addIfExists(List<TitleRecommendation> list, String code) {
        TitleRecommendation title = ImmigrationTitleReferentiel.getByCode(code);
        if (title != null) {
            list.add(title);
        }
    }

    /** Limite à 3 résultats maximum. */
    private static List<TitleRecommendation> cap(List<TitleRecommendation> list) {
        return list.size() <= 3 ? List.copyOf(list) : List.copyOf(list.subList(0, 3));
    }
}
