package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-214-05 : analyseur de l'éligibilité à la VPF liens personnels et familiaux
 * (L. 423-23 CESEDA — ancien L. 313-11 7°, recodification 2021 ; art. 8 CEDH).
 * Outil single-country FR.
 *
 * <p>Source juridique :
 * <ul>
 *   <li>L. 423-23 CESEDA — titre de séjour « vie privée et familiale » lorsque les
 *       liens personnels et familiaux en France sont tels que le refus porterait une
 *       atteinte disproportionnée au droit au respect de la vie privée et familiale</li>
 *   <li>Article 8 CEDH — droit au respect de la vie privée et familiale</li>
 *   <li>CEDH Boultif c. Suisse, 2 août 2001 — critères de proportionnalité</li>
 *   <li>CE 19 janvier 2011, n° 334018 — appréciation de la proportionnalité</li>
 * </ul>
 *
 * <p>L'analyse combine deux dimensions :
 * <ol>
 *   <li>un critère de durée de résidence (≥ 5 ans / 60 mois, ou ≥ 3 ans / 36 mois
 *       si entrée en France pendant la minorité — appréciation jurisprudentielle) ;</li>
 *   <li>un score d'intensité des liens personnels et familiaux.</li>
 * </ol>
 */
public final class VpfLiensPersonnelsAnalyzer {

    public static final String VERDICT_ELIGIBLE_PROBABLE = "ELIGIBLE_PROBABLE";
    public static final String VERDICT_ELIGIBLE_SOUS_RESERVE = "ELIGIBLE_SOUS_RESERVE";
    public static final String VERDICT_NON_ELIGIBLE = "NON_ELIGIBLE";
    public static final String VERDICT_DOSSIER_A_CONSOLIDER = "DOSSIER_A_CONSOLIDER";

    public static final String NIVEAU_FORT = "FORT";
    public static final String NIVEAU_MOYEN = "MOYEN";
    public static final String NIVEAU_FAIBLE = "FAIBLE";

    public static final Set<String> NIVEAUX_VALIDES = Set.of(
            NIVEAU_FORT, NIVEAU_MOYEN, NIVEAU_FAIBLE);

    /** Codes de critères non remplis exposés sous forme de chips. */
    public static final String CHIP_DUREE_RESIDENCE_INSUFFISANTE = "DUREE_RESIDENCE_INSUFFISANTE";
    public static final String CHIP_INTENSITE_LIENS_FAIBLE = "INTENSITE_LIENS_FAIBLE";
    public static final String CHIP_CONDAMNATION_PENALE = "CONDAMNATION_PENALE";
    public static final String CHIP_ATTACHES_PAYS_ORIGINE = "ATTACHES_PAYS_ORIGINE";

    /** Durée de résidence principale (critère jurisprudentiel) — 5 ans. */
    public static final int DUREE_RESIDENCE_PRINCIPALE_MOIS = 60;

    /** Durée de résidence assouplie en cas d'entrée pendant la minorité — 3 ans. */
    public static final int DUREE_RESIDENCE_MINEUR_MOIS = 36;

    /** Barème du score d'intensité des liens. */
    public static final int PTS_ENFANTS_EN_FRANCE = 2;
    public static final int PTS_CONJOINT_EN_FRANCE = 2;
    public static final int PTS_PARENTS_EN_FRANCE = 1;
    public static final int PTS_ENTREE_MINEUR = 2;
    public static final int PTS_INTEGRATION_FORTE = 1;
    public static final int PTS_CONDAMNATION_PENALE = -2;

    /** Seuil de score à partir duquel les liens sont jugés intenses. */
    public static final int SEUIL_SCORE_LIENS_INTENSES = 4;

    /** Seuil de score minimal pour une éligibilité sous réserve. */
    public static final int SEUIL_SCORE_MINIMAL = 2;

    private static final String BASE_JURIDIQUE =
            "CESEDA L. 423-23 (vie privée et familiale, ancien L. 313-11 7°) ; "
            + "art. 8 CEDH ; CEDH Boultif c. Suisse 02/08/2001 ; "
            + "CE 19 janvier 2011 n° 334018 (proportionnalité du refus)";

    public static final int SITUATION_FAMILIALE_MAX_LENGTH = 300;

    private VpfLiensPersonnelsAnalyzer() {}

    /**
     * Analyse l'éligibilité à la VPF liens personnels et familiaux.
     *
     * @param dureeResidenceFranceMois    durée de résidence en France en mois (≥ 0)
     * @param entreeEnFranceMineur        entrée en France pendant la minorité
     * @param enfantsEnFrance             présence d'enfant(s) scolarisé(s) en France
     * @param conjointEnFrance            présence d'un conjoint en séjour régulier en France
     * @param parentsEnFrance             présence de parents en France
     * @param situationFamilialeAlEtranger description libre des attaches au pays d'origine (≤ 300)
     * @param niveauIntegration           FORT | MOYEN | FAIBLE
     * @param ancienneConvictionPenale    condamnation pénale grave antérieure
     * @return résultat de l'analyse
     */
    public static VpfLiensPersonnelsResult analyze(int dureeResidenceFranceMois,
                                                   boolean entreeEnFranceMineur,
                                                   boolean enfantsEnFrance,
                                                   boolean conjointEnFrance,
                                                   boolean parentsEnFrance,
                                                   String situationFamilialeAlEtranger,
                                                   String niveauIntegration,
                                                   boolean ancienneConvictionPenale) {
        validateInputs(dureeResidenceFranceMois, situationFamilialeAlEtranger, niveauIntegration);

        // Score d'intensité des liens
        int score = 0;
        if (enfantsEnFrance) {
            score += PTS_ENFANTS_EN_FRANCE;
        }
        if (conjointEnFrance) {
            score += PTS_CONJOINT_EN_FRANCE;
        }
        if (parentsEnFrance) {
            score += PTS_PARENTS_EN_FRANCE;
        }
        if (entreeEnFranceMineur) {
            score += PTS_ENTREE_MINEUR;
        }
        if (NIVEAU_FORT.equals(niveauIntegration)) {
            score += PTS_INTEGRATION_FORTE;
        }
        if (ancienneConvictionPenale) {
            score += PTS_CONDAMNATION_PENALE;
        }

        // Critère de durée : 5 ans, ou 3 ans si entrée pendant la minorité
        int seuilDureeMois = entreeEnFranceMineur
                ? DUREE_RESIDENCE_MINEUR_MOIS
                : DUREE_RESIDENCE_PRINCIPALE_MOIS;
        boolean dureeResidenceRemplie = dureeResidenceFranceMois >= seuilDureeMois;

        List<String> chips = new ArrayList<>();
        if (!dureeResidenceRemplie) {
            chips.add(CHIP_DUREE_RESIDENCE_INSUFFISANTE);
        }
        if (score < SEUIL_SCORE_MINIMAL) {
            chips.add(CHIP_INTENSITE_LIENS_FAIBLE);
        }
        if (ancienneConvictionPenale) {
            chips.add(CHIP_CONDAMNATION_PENALE);
        }
        boolean attachesPaysOrigine = situationFamilialeAlEtranger != null
                && !situationFamilialeAlEtranger.isBlank();
        if (attachesPaysOrigine) {
            chips.add(CHIP_ATTACHES_PAYS_ORIGINE);
        }

        String verdict = determineVerdict(dureeResidenceRemplie, score, ancienneConvictionPenale);
        List<String> recommandations = buildRecommandations(
                dureeResidenceRemplie, score, enfantsEnFrance, conjointEnFrance,
                parentsEnFrance, entreeEnFranceMineur, ancienneConvictionPenale);

        return new VpfLiensPersonnelsResult(
                dureeResidenceFranceMois,
                entreeEnFranceMineur,
                enfantsEnFrance,
                conjointEnFrance,
                parentsEnFrance,
                situationFamilialeAlEtranger,
                niveauIntegration,
                ancienneConvictionPenale,
                score,
                dureeResidenceRemplie,
                verdict,
                chips,
                recommandations,
                BASE_JURIDIQUE);
    }

    private static String determineVerdict(boolean dureeResidenceRemplie,
                                           int score,
                                           boolean ancienneConvictionPenale) {
        // Une condamnation pénale grave qui ne laisse aucun lien intense résiduel
        // rend l'atteinte non disproportionnée → refus.
        if (ancienneConvictionPenale && score <= 0) {
            return VERDICT_NON_ELIGIBLE;
        }
        // Sans durée de résidence suffisante, le dossier ne tient pas encore : il faut
        // le consolider (durée ou intensité des liens à étayer).
        if (!dureeResidenceRemplie) {
            return VERDICT_DOSSIER_A_CONSOLIDER;
        }
        // Durée OK + liens très intenses sans condamnation lourde → éligibilité probable.
        if (score >= SEUIL_SCORE_LIENS_INTENSES && !ancienneConvictionPenale) {
            return VERDICT_ELIGIBLE_PROBABLE;
        }
        // Durée OK + intensité minimale des liens → éligibilité sous réserve
        // d'appréciation de proportionnalité.
        if (score >= SEUIL_SCORE_MINIMAL) {
            return VERDICT_ELIGIBLE_SOUS_RESERVE;
        }
        // Durée OK mais liens faibles → dossier à consolider.
        return VERDICT_DOSSIER_A_CONSOLIDER;
    }

    private static List<String> buildRecommandations(boolean dureeResidenceRemplie,
                                                     int score,
                                                     boolean enfantsEnFrance,
                                                     boolean conjointEnFrance,
                                                     boolean parentsEnFrance,
                                                     boolean entreeEnFranceMineur,
                                                     boolean ancienneConvictionPenale) {
        List<String> reco = new ArrayList<>();
        if (!dureeResidenceRemplie) {
            reco.add("Rassembler les justificatifs de présence continue en France "
                    + "(quittances, attestations, bulletins, factures) pour établir l'ancienneté de résidence.");
        }
        if (enfantsEnFrance) {
            reco.add("Produire les certificats de scolarité et bulletins des enfants scolarisés en France.");
        }
        if (conjointEnFrance) {
            reco.add("Joindre l'acte de mariage / PACS et le titre de séjour du conjoint en France.");
        }
        if (parentsEnFrance) {
            reco.add("Fournir les justificatifs de présence régulière des parents en France et des liens entretenus.");
        }
        if (entreeEnFranceMineur) {
            reco.add("Établir la date d'entrée pendant la minorité (visa, certificat de scolarité, suivi médical).");
        }
        if (score < SEUIL_SCORE_LIENS_INTENSES) {
            reco.add("Documenter l'intensité des liens personnels (attestations de proches, "
                    + "preuves d'insertion sociale, associative et professionnelle).");
        }
        if (ancienneConvictionPenale) {
            reco.add("Produire les pièces du dossier pénal (jugement, justificatifs de réinsertion) "
                    + "pour relativiser la menace à l'ordre public dans l'analyse de proportionnalité.");
        }
        if (reco.isEmpty()) {
            reco.add("Constituer le mémoire de proportionnalité (art. 8 CEDH) "
                    + "synthétisant la durée de résidence et l'intensité des liens en France.");
        }
        return reco;
    }

    private static void validateInputs(int dureeResidenceFranceMois,
                                       String situationFamilialeAlEtranger,
                                       String niveauIntegration) {
        if (dureeResidenceFranceMois < 0) {
            throw new IllegalArgumentException("dureeResidenceFranceMois ne peut pas être négatif");
        }
        if (situationFamilialeAlEtranger != null
                && situationFamilialeAlEtranger.length() > SITUATION_FAMILIALE_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "situationFamilialeAlEtranger ne peut pas dépasser "
                    + SITUATION_FAMILIALE_MAX_LENGTH + " caractères");
        }
        if (niveauIntegration == null || !NIVEAUX_VALIDES.contains(niveauIntegration)) {
            throw new IllegalArgumentException(
                    "niveauIntegration inconnu — valeurs attendues : " + NIVEAUX_VALIDES);
        }
    }
}
