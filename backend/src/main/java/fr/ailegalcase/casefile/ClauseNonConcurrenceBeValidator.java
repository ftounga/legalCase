package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * SF-213-01 : validateur de clause de non-concurrence BE — fonction pure.
 *
 * <p>Analyse la validité d'une clause selon le droit belge du travail
 * (<b>Loi du 03/07/1978 art. 65</b> + <b>CCT n°13 du 24/02/1971</b> modifiée)
 * et calcule l'indemnité légale obligatoire (= ½ rémunération brute × durée
 * de la clause). Cette indemnité doit être payée dans les 15 jours suivant
 * la fin du contrat.</p>
 *
 * <h2>Règles de validité</h2>
 * <table summary="Conditions">
 *   <tr><th>Condition</th><th>Verdict</th><th>Raison</th></tr>
 *   <tr><td>Rémunération ≤ seuil indexé</td><td>NULLE</td>
 *       <td>REMUNERATION_INSUFFISANTE</td></tr>
 *   <tr><td>Durée &gt; 12 mois</td><td>NULLE</td>
 *       <td>DUREE_EXCESSIVE</td></tr>
 *   <tr><td>Zone NON_SPECIFIEE</td><td>NULLE</td>
 *       <td>ZONE_GEOGRAPHIQUE_NON_SPECIFIEE</td></tr>
 *   <tr><td>Zone BELGIQUE_ET_ETRANGER + !activitéInternationale</td>
 *       <td>PARTIELLEMENT_NULLE</td><td>ZONE_GEOGRAPHIQUE_NON_JUSTIFIEE</td></tr>
 *   <tr><td>Toutes conditions OK</td><td>VALIDE</td><td>(null)</td></tr>
 * </table>
 *
 * <h2>Calcul de l'indemnité légale</h2>
 * <pre>
 *   indemniteLegale = (remunerationAnnuelleBrute / 12) * (dureeMois / 2)
 * </pre>
 * <p>Calculée dans tous les cas (y compris verdict NULLE/PARTIELLEMENT_NULLE)
 * pour permettre une simulation du coût en cas de litige.</p>
 *
 * <p>Single-country BELGIQUE — l'équivalent français (F-DT-24) suit un régime
 * distinct (montant libre, pas de plancher légal de ½ rémunération).</p>
 */
public final class ClauseNonConcurrenceBeValidator {

    /** Seuil de rémunération annuelle brute 2024 (CCT 13, indexé annuellement) — €. */
    public static final BigDecimal SEUIL_REMUNERATION_2024_EUR = new BigDecimal("73571.00");

    /** Durée maximale légale d'une clause de non-concurrence BE (mois). */
    public static final int DUREE_MAX_MOIS = 12;

    private static final String BASE_JURIDIQUE =
            "Loi 03/07/1978 art. 65 ; CCT n°13 du 24/02/1971 (modifiée)";

    private static final String AVERTISSEMENT_DEFAUT =
            "Seuil 2024 : 73 571 € — à vérifier selon l'année de signature du contrat. "
                    + "Indemnité légale = ½ rémunération brute correspondant à la durée de la clause, "
                    + "payable dans les 15 jours suivant la fin du contrat.";

    private ClauseNonConcurrenceBeValidator() {
    }

    public static ClauseNonConcurrenceBeResult validate(ClauseNonConcurrenceBeRequest request) {
        validateInputs(request);

        BigDecimal seuil = request.salaireAnnuelSeuil() != null && request.salaireAnnuelSeuil().signum() > 0
                ? request.salaireAnnuelSeuil()
                : SEUIL_REMUNERATION_2024_EUR;
        BigDecimal remuneration = request.remunerationAnnuelleBrute().setScale(2, RoundingMode.HALF_UP);
        int dureeMois = request.dureeMois();
        ClauseNonConcurrenceBeZoneEnum zone = request.zoneGeographique();
        boolean activiteInternationale = Boolean.TRUE.equals(request.activiteInternationaleProuvee());

        ClauseNonConcurrenceBeVerdict verdict;
        ClauseNonConcurrenceBeRaisonNullite raison;

        // Évaluation des conditions de validité, dans l'ordre de la mini-spec.
        if (remuneration.compareTo(seuil) <= 0) {
            verdict = ClauseNonConcurrenceBeVerdict.NULLE;
            raison = ClauseNonConcurrenceBeRaisonNullite.REMUNERATION_INSUFFISANTE;
        } else if (dureeMois > DUREE_MAX_MOIS) {
            verdict = ClauseNonConcurrenceBeVerdict.NULLE;
            raison = ClauseNonConcurrenceBeRaisonNullite.DUREE_EXCESSIVE;
        } else if (zone == ClauseNonConcurrenceBeZoneEnum.NON_SPECIFIEE) {
            verdict = ClauseNonConcurrenceBeVerdict.NULLE;
            raison = ClauseNonConcurrenceBeRaisonNullite.ZONE_GEOGRAPHIQUE_NON_SPECIFIEE;
        } else if (zone == ClauseNonConcurrenceBeZoneEnum.BELGIQUE_ET_ETRANGER && !activiteInternationale) {
            verdict = ClauseNonConcurrenceBeVerdict.PARTIELLEMENT_NULLE;
            raison = ClauseNonConcurrenceBeRaisonNullite.ZONE_GEOGRAPHIQUE_NON_JUSTIFIEE;
        } else {
            verdict = ClauseNonConcurrenceBeVerdict.VALIDE;
            raison = null;
        }

        BigDecimal indemniteLegale = computeIndemniteLegale(remuneration, dureeMois);
        String formule = formatFormule(remuneration, dureeMois, indemniteLegale);

        return new ClauseNonConcurrenceBeResult(
                remuneration,
                dureeMois,
                zone,
                activiteInternationale,
                seuil.setScale(2, RoundingMode.HALF_UP),
                verdict,
                raison,
                indemniteLegale,
                formule,
                BASE_JURIDIQUE,
                AVERTISSEMENT_DEFAUT);
    }

    /**
     * indemniteLegale = (remunerationAnnuelleBrute / 12) * (dureeMois / 2)
     * <p>Calcul réalisé en {@code BigDecimal} avec arrondi {@code HALF_UP} à 2
     * décimales — garantit la stabilité du round trip JSON.</p>
     */
    private static BigDecimal computeIndemniteLegale(BigDecimal remunerationAnnuelle, int dureeMois) {
        // ½ × (rémunération mensuelle = annuelle / 12) × dureeMois
        // équivalent à : annuelle × dureeMois / 24
        return remunerationAnnuelle
                .multiply(BigDecimal.valueOf(dureeMois))
                .divide(new BigDecimal("24"), 2, RoundingMode.HALF_UP);
    }

    private static String formatFormule(BigDecimal remuneration, int dureeMois, BigDecimal indemnite) {
        return String.format(
                "(%s € / 12) × (%d / 2) = %s €",
                formatMontant(remuneration),
                dureeMois,
                formatMontant(indemnite));
    }

    private static String formatMontant(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static void validateInputs(ClauseNonConcurrenceBeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.remunerationAnnuelleBrute() == null
                || request.remunerationAnnuelleBrute().signum() <= 0) {
            throw new IllegalArgumentException(
                    "remunerationAnnuelleBrute requise et strictement positive");
        }
        if (request.dureeMois() == null || request.dureeMois() < 1) {
            throw new IllegalArgumentException("dureeMois requise et ≥ 1");
        }
        if (request.zoneGeographique() == null) {
            throw new IllegalArgumentException("zoneGeographique requise");
        }
    }
}
