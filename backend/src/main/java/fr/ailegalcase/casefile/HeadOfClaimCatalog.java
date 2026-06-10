package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.HeadOfClaim.HeadCategory;

import java.util.List;

/**
 * F-262 SF-262-01 — Catalogue <strong>code-based</strong> des chefs de demande,
 * indexé par (domaine juridique, pays). Pas de table, pas de migration : MVP
 * versionnable. Passage en {@code legal_referentials} = backlog si maintenance
 * opérateur requise.
 *
 * <p>Vague <strong>TRAVAIL FR</strong> ({@code DROIT_DU_TRAVAIL} / {@code FRANCE})
 * remplie. Les autres domaines/pays retournent une liste vide (vagues
 * immigration / famille futures).</p>
 *
 * <p>Deux familles de chefs :</p>
 * <ul>
 *   <li><strong>Transverses</strong> ({@code toolId == null}) — toujours
 *       applicables au stade contentieux (art. 700, dépens, intérêts,
 *       capitalisation, exécution provisoire).</li>
 *   <li><strong>Indemnitaires outillés</strong> — mappés à un outil décisionnel
 *       Travail FR existant. L'applicabilité dérive de la visibilité de l'outil ;
 *       le statut « couvert » dérive de son calcul (présence d'une tile dashboard).</li>
 * </ul>
 */
public final class HeadOfClaimCatalog {

    private HeadOfClaimCatalog() {
    }

    private static final String DOMAIN_TRAVAIL = "DROIT_DU_TRAVAIL";
    private static final String COUNTRY_FR = "FRANCE";

    /**
     * Chefs <strong>transverses</strong> Travail FR — toujours applicables au
     * stade contentieux (jugement au fond / appel). {@code toolId == null}.
     */
    private static final List<HeadOfClaim> TRAVAIL_FR_TRANSVERSE = List.of(
            new HeadOfClaim(
                    "article-700-cpc",
                    "Frais irrépétibles (article 700 CPC)",
                    "Article 700 du Code de procédure civile",
                    HeadCategory.TRANSVERSE,
                    null),
            new HeadOfClaim(
                    "depens",
                    "Dépens",
                    "Article 696 du Code de procédure civile",
                    HeadCategory.TRANSVERSE,
                    null),
            new HeadOfClaim(
                    "interets-taux-legal",
                    "Intérêts au taux légal",
                    "Articles 1231-6 et 1231-7 du Code civil",
                    HeadCategory.TRANSVERSE,
                    null),
            new HeadOfClaim(
                    "capitalisation-interets",
                    "Capitalisation des intérêts (anatocisme)",
                    "Article 1343-2 du Code civil",
                    HeadCategory.TRANSVERSE,
                    null),
            new HeadOfClaim(
                    "execution-provisoire",
                    "Exécution provisoire",
                    "Article 514 du Code de procédure civile",
                    HeadCategory.TRANSVERSE,
                    null)
    );

    /**
     * Chefs <strong>indemnitaires outillés</strong> Travail FR — chaque chef est
     * mappé à un {@code toolId} d'un outil décisionnel Travail FR <em>existant</em>
     * (vérifié dans {@code CaseFileDashboardService#assembleTiles} et les règles
     * {@code decision_tool_visibility_rules} DROIT_DU_TRAVAIL/FRANCE).
     */
    private static final List<HeadOfClaim> TRAVAIL_FR_OUTILLE = List.of(
            new HeadOfClaim(
                    "dommages-interets-licenciement-sans-cause",
                    "Dommages-intérêts pour licenciement sans cause réelle et sérieuse",
                    "Article L. 1235-3 du Code du travail (barème Macron)",
                    HeadCategory.INDEMNITAIRE_OUTILLE,
                    "F-DT-09-comparateur-indemnites"),
            new HeadOfClaim(
                    "indemnite-legale-licenciement",
                    "Indemnité légale (ou conventionnelle) de licenciement",
                    "Articles L. 1234-9 et R. 1234-2 du Code du travail",
                    HeadCategory.INDEMNITAIRE_OUTILLE,
                    "F-DT-07-anciennete-conges-prime"),
            new HeadOfClaim(
                    "indemnite-compensatrice-preavis",
                    "Indemnité compensatrice de préavis",
                    "Articles L. 1234-1 et L. 1234-5 du Code du travail",
                    HeadCategory.INDEMNITAIRE_OUTILLE,
                    "F-DT-25-indemnite-preavis"),
            new HeadOfClaim(
                    "indemnite-compensatrice-conges-payes",
                    "Indemnité compensatrice de congés payés",
                    "Articles L. 3141-28 et L. 3141-24 du Code du travail",
                    HeadCategory.INDEMNITAIRE_OUTILLE,
                    "F-DT-26-conges-payes-indemnite"),
            new HeadOfClaim(
                    "rappel-heures-supplementaires",
                    "Rappel d'heures supplémentaires",
                    "Articles L. 3121-28 et L. 3121-36 du Code du travail",
                    HeadCategory.INDEMNITAIRE_OUTILLE,
                    "F-DT-19-heures-sup"),
            new HeadOfClaim(
                    "rappel-salaire",
                    "Rappel de salaire",
                    "Article L. 3242-1 du Code du travail",
                    HeadCategory.INDEMNITAIRE_OUTILLE,
                    "F-DT-20-rappel-salaire"),
            new HeadOfClaim(
                    "indemnite-forfaitaire-travail-dissimule",
                    "Indemnité forfaitaire pour travail dissimulé",
                    "Article L. 8223-1 du Code du travail",
                    HeadCategory.INDEMNITAIRE_OUTILLE,
                    "F-DT-21-travail-dissimule")
    );

    private static final List<HeadOfClaim> TRAVAIL_FR;
    static {
        TRAVAIL_FR = java.util.stream.Stream
                .concat(TRAVAIL_FR_TRANSVERSE.stream(), TRAVAIL_FR_OUTILLE.stream())
                .toList();
    }

    /**
     * Retourne le catalogue des chefs de demande pour un (domaine, pays) donné.
     * Liste vide si la combinaison n'est pas couverte par la vague courante.
     *
     * @param legalDomain domaine juridique du dossier (ex. {@code DROIT_DU_TRAVAIL}).
     * @param country     pays du workspace (ex. {@code FRANCE}).
     * @return liste immuable des chefs (jamais {@code null}).
     */
    public static List<HeadOfClaim> forDomainAndCountry(String legalDomain, String country) {
        if (DOMAIN_TRAVAIL.equals(legalDomain) && COUNTRY_FR.equals(country)) {
            return TRAVAIL_FR;
        }
        return List.of();
    }
}
