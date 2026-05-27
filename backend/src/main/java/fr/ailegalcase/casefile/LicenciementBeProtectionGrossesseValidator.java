package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * SF-213-05 : analyseur de la validité d'un licenciement pendant la
 * grossesse / maternité en droit belge — <b>Loi du 16 mars 1971 sur le
 * travail, art. 40</b> — et calculateur de l'indemnité forfaitaire de
 * 6 mois de rémunération brute. Fonction <b>pure</b>.
 *
 * <h2>Source juridique</h2>
 * <p>Loi du 16/03/1971 sur le travail, art. 40 :
 * <ul>
 *   <li>Protection contre le licenciement : du début de la grossesse jusqu'à
 *       <b>1 mois après la fin du congé de maternité</b>.</li>
 *   <li>Interdiction absolue de licencier sauf <b>motif étranger à la
 *       grossesse</b> — la preuve incombe à l'employeur.</li>
 *   <li>Si le motif est lié à la grossesse : licenciement <b>nul</b> +
 *       <b>indemnité forfaitaire = 6 mois de rémunération brute</b> (al. 3),
 *       cumulable avec la réparation des dommages prouvés.</li>
 * </ul>
 *
 * <h2>Logique de l'analyseur (4 verdicts)</h2>
 * <ol>
 *   <li><b>{@code dateDebutProtection = dateDebutGrossesse}</b>.</li>
 *   <li><b>{@code dateFinProtection}</b> :
 *     <ul>
 *       <li>Si {@code dateCongeMaterniteFinale != null} →
 *           {@code dateCongeMaterniteFinale + 1 mois} (calcul exact art. 40 al. 1).</li>
 *       <li>Sinon si {@code dateAccouchement != null} →
 *           {@code dateAccouchement + 3 mois} (approximation : congé maternité
 *           BE standard = 15 sem soit ~3.5 mois, plus 1 mois post-congé donne
 *           ~4.5 mois ; cette approximation simplifie le calcul à 3 mois
 *           post-accouchement — privilégier {@code dateCongeMaterniteFinale}
 *           pour un calcul exact).</li>
 *       <li>Sinon → {@code null} + avertissement « Date fin protection
 *           indéterminée » ; la présomption ouvre la fenêtre côté droite
 *           ({@code licenciementDansLaPeriodeProtegee} se réduit alors à
 *           {@code dateLicenciement >= dateDebutProtection}).</li>
 *     </ul>
 *   </li>
 *   <li><b>{@code licenciementDansLaPeriodeProtegee}</b> :
 *       {@code dateLicenciement ∈ [dateDebutProtection, dateFinProtection]}
 *       (bornes incluses).</li>
 *   <li><b>Verdict</b> :
 *     <table>
 *       <caption>Matrice de verdict</caption>
 *       <tr><th>Condition</th><th>Verdict</th></tr>
 *       <tr><td>!licenciementDansLaPeriodeProtegee</td>
 *           <td>{@code HORS_PERIODE_PROTECTION}</td></tr>
 *       <tr><td>protected &amp;&amp; notifiéParEcrit
 *               &amp;&amp; dateLicenciement ≤ dateDebutGrossesse + 10 sem</td>
 *           <td>{@code PROTECTION_PRESUMEE}</td></tr>
 *       <tr><td>protected &amp;&amp; !notifiéParEcrit</td>
 *           <td>{@code PROTECTION_APPLICABLE_NON_NOTIFIEE}</td></tr>
 *       <tr><td>protected (autres)</td>
 *           <td>{@code PROTECTION_APPLICABLE}</td></tr>
 *     </table>
 *   </li>
 * </ol>
 *
 * <h2>Indemnité forfaitaire (art. 40 al. 3)</h2>
 * <ul>
 *   <li>Verdict ≠ {@code HORS_PERIODE_PROTECTION} →
 *       {@code indemniteForfaitaire = remunerationMensuelleBrute × 6}
 *       (BigDecimal HALF_UP 2 décimales).</li>
 *   <li>Verdict {@code HORS_PERIODE_PROTECTION} → {@code null}
 *       (pas d'indemnité due, à distinguer de zéro pour clarté JSON).</li>
 * </ul>
 *
 * <h2>Charge de la preuve</h2>
 * <p>Inversée (l'employeur doit prouver le motif étranger) dans les 3 cas
 * où la protection s'applique. Pour {@code PROTECTION_PRESUMEE}, cette
 * inversion est <b>renforcée</b> par la fenêtre des 10 semaines post
 * notification — l'employeur doit prouver que le motif est totalement
 * indépendant de la grossesse.</p>
 *
 * <h2>Hors scope SF-213-05</h2>
 * <ul>
 *   <li>Calcul des dommages supplémentaires prouvés (cumulables) — à la
 *       charge de l'avocat, indiqué via {@code avertissement}.</li>
 *   <li>Maternité adoptante (régime distinct).</li>
 *   <li>Congé de paternité (20 jours) — outil P3 distinct.</li>
 * </ul>
 *
 * @see LicenciementBeProtectionGrossesseVerdict
 */
public final class LicenciementBeProtectionGrossesseValidator {

    private static final String BASE_JURIDIQUE =
            "Loi 16/03/1971 sur le travail art. 40 — protection contre licenciement grossesse/maternité";

    private static final String AVERTISSEMENT_FIN_PROTECTION_INDETERMINEE =
            "Date fin protection indéterminée — préciser dateAccouchement ou "
                    + "dateCongeMaterniteFinale pour calcul exact (Loi 16/03/1971 art. 40 al. 1 : "
                    + "protection jusqu'à 1 mois après la fin du congé maternité). Présomption "
                    + "ouverte appliquée (fenêtre droite indéfinie).";

    private static final String AVERTISSEMENT_ACCOUCHEMENT_APPROX =
            "Date de fin de protection calculée comme dateAccouchement + 3 mois — "
                    + "approximation (congé maternité BE standard = 15 semaines, soit ~3.5 mois ; "
                    + "ajouter 1 mois post-congé donnerait ~4.5 mois). Renseigner "
                    + "dateCongeMaterniteFinale pour un calcul exact art. 40 al. 1.";

    private static final String AVERTISSEMENT_DOMMAGES =
            "L'indemnité forfaitaire de 6 mois (art. 40 al. 3) est cumulable avec la "
                    + "réparation des dommages prouvés. Cette charge incombe à l'avocat et n'est "
                    + "pas calculée automatiquement par l'outil.";

    /** Nombre de mois d'indemnité forfaitaire (art. 40 al. 3 Loi 16/03/1971). */
    private static final int MOIS_INDEMNITE_FORFAITAIRE = 6;

    /** Fenêtre de présomption de licenciement lié à la grossesse (10 semaines). */
    private static final int SEMAINES_FENETRE_PRESOMPTION = 10;

    /** Échelle BigDecimal pour les montants (2 décimales). */
    private static final int SCALE_MONTANTS = 2;

    private LicenciementBeProtectionGrossesseValidator() {
    }

    public static LicenciementBeProtectionGrossesseResult validate(
            LicenciementBeProtectionGrossesseRequest request) {
        validateInputs(request);

        LocalDate dateDebutProtection = request.dateDebutGrossesse();
        LocalDate dateFinProtection = computeDateFinProtection(request);

        boolean licenciementDansLaPeriodeProtegee = isInProtectedWindow(
                request.dateLicenciement(), dateDebutProtection, dateFinProtection);

        LicenciementBeProtectionGrossesseVerdict verdict = resolveVerdict(
                request, dateDebutProtection, licenciementDansLaPeriodeProtegee);

        BigDecimal indemniteForfaitaire = computeIndemnite(request, verdict);

        boolean chargePreuveEmployeur =
                verdict != LicenciementBeProtectionGrossesseVerdict.HORS_PERIODE_PROTECTION;

        String formuleCalcul = buildFormuleCalcul(request, verdict, indemniteForfaitaire);
        String avertissement = buildAvertissement(request, dateFinProtection);

        return new LicenciementBeProtectionGrossesseResult(
                request.dateDebutGrossesse(),
                request.dateAccouchement(),
                request.dateCongeMaterniteDebut(),
                request.dateCongeMaterniteFinale(),
                request.dateLicenciement(),
                request.grossesseNotifieeParEcrit(),
                request.remunerationMensuelleBrute(),
                request.motifInvoqueParEmployeur(),
                verdict,
                licenciementDansLaPeriodeProtegee,
                dateDebutProtection,
                dateFinProtection,
                indemniteForfaitaire,
                chargePreuveEmployeur,
                BASE_JURIDIQUE,
                formuleCalcul,
                avertissement);
    }

    // ── Calculs internes ────────────────────────────────────────────────────

    private static LocalDate computeDateFinProtection(
            LicenciementBeProtectionGrossesseRequest request) {
        if (request.dateCongeMaterniteFinale() != null) {
            // Calcul exact art. 40 al. 1 : fin congé maternité + 1 mois.
            return request.dateCongeMaterniteFinale().plusMonths(1);
        }
        if (request.dateAccouchement() != null) {
            // Approximation : congé maternité standard 15 sem ≈ 3.5 mois + 1 mois
            // → ~4.5 mois. La mini-spec retient 3 mois post-accouchement pour
            // simplification (cf. avertissement remonté à l'avocat).
            return request.dateAccouchement().plusMonths(3);
        }
        // Indéterminée → présomption ouverte côté droite.
        return null;
    }

    private static boolean isInProtectedWindow(
            LocalDate dateLicenciement,
            LocalDate dateDebutProtection,
            LocalDate dateFinProtection) {
        if (dateLicenciement.isBefore(dateDebutProtection)) {
            return false;
        }
        if (dateFinProtection == null) {
            // Fenêtre droite indéfinie : tout licenciement ≥ début grossesse est
            // présumé dans la fenêtre (charge à l'employeur de prouver la sortie).
            return true;
        }
        return !dateLicenciement.isAfter(dateFinProtection);
    }

    private static LicenciementBeProtectionGrossesseVerdict resolveVerdict(
            LicenciementBeProtectionGrossesseRequest request,
            LocalDate dateDebutProtection,
            boolean licenciementDansLaPeriodeProtegee) {

        if (!licenciementDansLaPeriodeProtegee) {
            return LicenciementBeProtectionGrossesseVerdict.HORS_PERIODE_PROTECTION;
        }

        LocalDate dateFinPresomption =
                dateDebutProtection.plusWeeks(SEMAINES_FENETRE_PRESOMPTION);

        boolean dansFenetrePresomption =
                !request.dateLicenciement().isAfter(dateFinPresomption);

        if (request.grossesseNotifieeParEcrit() && dansFenetrePresomption) {
            return LicenciementBeProtectionGrossesseVerdict.PROTECTION_PRESUMEE;
        }
        if (!request.grossesseNotifieeParEcrit()) {
            return LicenciementBeProtectionGrossesseVerdict.PROTECTION_APPLICABLE_NON_NOTIFIEE;
        }
        return LicenciementBeProtectionGrossesseVerdict.PROTECTION_APPLICABLE;
    }

    private static BigDecimal computeIndemnite(
            LicenciementBeProtectionGrossesseRequest request,
            LicenciementBeProtectionGrossesseVerdict verdict) {
        if (verdict == LicenciementBeProtectionGrossesseVerdict.HORS_PERIODE_PROTECTION) {
            return null;
        }
        return request.remunerationMensuelleBrute()
                .multiply(BigDecimal.valueOf(MOIS_INDEMNITE_FORFAITAIRE))
                .setScale(SCALE_MONTANTS, RoundingMode.HALF_UP);
    }

    private static String buildFormuleCalcul(
            LicenciementBeProtectionGrossesseRequest request,
            LicenciementBeProtectionGrossesseVerdict verdict,
            BigDecimal indemnite) {
        if (verdict == LicenciementBeProtectionGrossesseVerdict.HORS_PERIODE_PROTECTION) {
            return "Aucune indemnité forfaitaire — licenciement hors période de protection "
                    + "(Loi 16/03/1971 art. 40).";
        }
        return String.format(
                "%s € × %d mois = %s € (indemnité forfaitaire art. 40 al. 3 Loi 16/03/1971)",
                stripTrailingZeros(request.remunerationMensuelleBrute()
                        .setScale(SCALE_MONTANTS, RoundingMode.HALF_UP)),
                MOIS_INDEMNITE_FORFAITAIRE,
                stripTrailingZeros(indemnite));
    }

    private static String buildAvertissement(
            LicenciementBeProtectionGrossesseRequest request,
            LocalDate dateFinProtection) {
        if (dateFinProtection == null) {
            return AVERTISSEMENT_FIN_PROTECTION_INDETERMINEE + " " + AVERTISSEMENT_DOMMAGES;
        }
        if (request.dateCongeMaterniteFinale() == null && request.dateAccouchement() != null) {
            return AVERTISSEMENT_ACCOUCHEMENT_APPROX + " " + AVERTISSEMENT_DOMMAGES;
        }
        return AVERTISSEMENT_DOMMAGES;
    }

    private static String stripTrailingZeros(BigDecimal bd) {
        BigDecimal stripped = bd.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0).toPlainString() : stripped.toPlainString();
    }

    private static void validateInputs(LicenciementBeProtectionGrossesseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.dateDebutGrossesse() == null) {
            throw new IllegalArgumentException("dateDebutGrossesse requise");
        }
        if (request.dateLicenciement() == null) {
            throw new IllegalArgumentException("dateLicenciement requise");
        }
        if (request.dateLicenciement().isBefore(request.dateDebutGrossesse())) {
            throw new IllegalArgumentException(
                    "Date incohérente : dateLicenciement antérieure à dateDebutGrossesse");
        }
        if (request.remunerationMensuelleBrute() == null
                || request.remunerationMensuelleBrute().signum() <= 0) {
            throw new IllegalArgumentException(
                    "remunerationMensuelleBrute requise et strictement positive");
        }
        if (request.motifInvoqueParEmployeur() != null
                && request.motifInvoqueParEmployeur().isBlank()) {
            throw new IllegalArgumentException(
                    "motifInvoqueParEmployeur ne peut pas être vide quand il est présent");
        }
        if (request.dateAccouchement() != null
                && request.dateAccouchement().isBefore(request.dateDebutGrossesse())) {
            throw new IllegalArgumentException(
                    "Date incohérente : dateAccouchement antérieure à dateDebutGrossesse");
        }
        if (request.dateCongeMaterniteDebut() != null && request.dateCongeMaterniteFinale() != null
                && request.dateCongeMaterniteFinale().isBefore(request.dateCongeMaterniteDebut())) {
            throw new IllegalArgumentException(
                    "Date incohérente : dateCongeMaterniteFinale antérieure à dateCongeMaterniteDebut");
        }
    }
}
