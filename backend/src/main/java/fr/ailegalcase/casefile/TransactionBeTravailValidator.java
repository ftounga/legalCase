package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SF-213-06 : analyseur de la validité d'une transaction de fin de contrat
 * en droit belge — fonction <b>pure</b>.
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Art. 2044 et s. du Code civil belge</b> : définition de la
 *       transaction — contrat par lequel les parties terminent une
 *       contestation née ou préviennent une contestation à naître, par
 *       des <b>concessions réciproques</b>. Sans concessions de
 *       l'employeur, il s'agit d'une quittance, pas d'une transaction.</li>
 *   <li><b>Loi du 3 juillet 1978 art. 6</b> : les droits qui découlent
 *       de dispositions impératives (droit du travail BE) ne peuvent
 *       être abdiqués par avance. Une renonciation à un droit d'ordre
 *       public est nulle (clause partiellement nulle, le reste de la
 *       transaction peut subsister).</li>
 * </ul>
 *
 * <h2>Matrice de verdict (ordre d'évaluation)</h2>
 * <table>
 *   <caption>Logique du validateur — ordre d'évaluation strict</caption>
 *   <tr><th>Condition</th><th>Verdict</th><th>Raison</th></tr>
 *   <tr><td>{@code !concessionsEmployeurDescrites}</td>
 *       <td>{@link TransactionBeTravailVerdict#INVALIDE}</td>
 *       <td>{@link TransactionBeTravailRaisonInvalidite#ABSENCE_CONCESSIONS}</td></tr>
 *   <tr><td>{@code renonciationOrdrePublicDetectee}</td>
 *       <td>{@link TransactionBeTravailVerdict#INVALIDE_PARTIELLE}</td>
 *       <td>{@link TransactionBeTravailRaisonInvalidite#RENONCIATION_ORDRE_PUBLIC}</td></tr>
 *   <tr><td>{@code !mentionContestation}</td>
 *       <td>{@link TransactionBeTravailVerdict#A_COMPLETER}</td>
 *       <td>{@link TransactionBeTravailRaisonInvalidite#LITIGE_NON_VISE}</td></tr>
 *   <tr><td>{@code renonciationsListees.isEmpty()}</td>
 *       <td>{@link TransactionBeTravailVerdict#A_COMPLETER}</td>
 *       <td>{@link TransactionBeTravailRaisonInvalidite#LITIGE_NON_VISE}</td></tr>
 *   <tr><td>Toutes conditions OK</td>
 *       <td>{@link TransactionBeTravailVerdict#VALIDE}</td>
 *       <td>{@code null}</td></tr>
 * </table>
 *
 * <h2>Ratio et avertissement de lésion</h2>
 * <p>Si {@code indemniteLegaleEtimee != null && > 0} :
 * {@code ratio = montantTransactionBrut / indemniteLegaleEtimee × 100},
 * arrondi HALF_UP à 1 décimale. Si {@code ratio < 50 %}, un avertissement
 * de risque de lésion / vice du consentement est porté. Sinon
 * {@code ratioPourcentage = null} et {@code avertissement = null}
 * (l'avocat conserve la responsabilité de l'analyse approfondie).</p>
 *
 * <h2>Checklist renonciations — simplification V1</h2>
 * <p>Pour chaque entrée de {@code renonciationsListees}, un
 * {@link TransactionBeTravailChecklistItem} est créé. V1 stratégie
 * conservatrice :
 * <ul>
 *   <li>Si {@code renonciationOrdrePublicDetectee == true} → tous les
 *       items {@code valide=false} (signal global que la transaction
 *       comporte au moins une renonciation suspecte).</li>
 *   <li>Sinon → tous {@code valide=true}.</li>
 * </ul>
 * L'analyse fine clause par clause (lecture sémantique du document) est
 * <b>hors scope SF-213-06</b> selon la mini-spec — sera couverte par un
 * outil ultérieur basé sur la cartographie des dispositions impératives
 * de la Loi 03/07/1978.</p>
 */
public final class TransactionBeTravailValidator {

    static final String BASE_JURIDIQUE =
            "Art. 2044 Code civil belge ; Loi 03/07/1978 art. 6 (dispositions impératives)";

    static final String AVERTISSEMENT_RATIO_LESION_TEMPLATE =
            "Montant de la transaction inférieur à 50 %% de l'indemnité légale estimée "
                    + "(ratio = %s %%) — risque de lésion / vice du consentement à analyser";

    /** Seuil de ratio (en pourcentage) en-deçà duquel l'avertissement est déclenché. */
    private static final BigDecimal SEUIL_RATIO_AVERTISSEMENT = new BigDecimal("50.0");

    /** Échelle du ratio pourcentage (1 décimale). */
    private static final int SCALE_RATIO = 1;

    /** Échelle interne du calcul de ratio avant arrondi final (4 décimales). */
    private static final int SCALE_INTERNE = 4;

    private TransactionBeTravailValidator() {
    }

    public static TransactionBeTravailResult validate(TransactionBeTravailRequest request) {
        validateInputs(request);

        TransactionBeTravailVerdict verdict;
        TransactionBeTravailRaisonInvalidite raison;

        // Ordre d'évaluation strict — la première condition vraie l'emporte.
        if (!request.concessionsEmployeurDescrites()) {
            verdict = TransactionBeTravailVerdict.INVALIDE;
            raison = TransactionBeTravailRaisonInvalidite.ABSENCE_CONCESSIONS;
        } else if (request.renonciationOrdrePublicDetectee()) {
            verdict = TransactionBeTravailVerdict.INVALIDE_PARTIELLE;
            raison = TransactionBeTravailRaisonInvalidite.RENONCIATION_ORDRE_PUBLIC;
        } else if (!request.mentionContestation()) {
            verdict = TransactionBeTravailVerdict.A_COMPLETER;
            raison = TransactionBeTravailRaisonInvalidite.LITIGE_NON_VISE;
        } else if (request.renonciationsListees().isEmpty()) {
            // Checklist vide → incomplet (même raison LITIGE_NON_VISE — la
            // mini-spec ne distingue pas une raison dédiée CHECKLIST_VIDE
            // pour V1, on reste sur LITIGE_NON_VISE qui regroupe les
            // éléments formels manquants).
            verdict = TransactionBeTravailVerdict.A_COMPLETER;
            raison = TransactionBeTravailRaisonInvalidite.LITIGE_NON_VISE;
        } else {
            verdict = TransactionBeTravailVerdict.VALIDE;
            raison = null;
        }

        BigDecimal ratio = computeRatio(
                request.montantTransactionBrut(), request.indemniteLegaleEtimee());
        String avertissement = buildAvertissement(ratio);
        List<TransactionBeTravailChecklistItem> checklist = buildChecklist(
                request.renonciationsListees(), request.renonciationOrdrePublicDetectee());

        return new TransactionBeTravailResult(
                request.montantTransactionBrut(),
                request.indemniteLegaleEtimee(),
                request.concessionsEmployeurDescrites(),
                List.copyOf(request.renonciationsListees()),
                request.renonciationOrdrePublicDetectee(),
                request.mentionContestation(),
                verdict,
                raison,
                ratio,
                avertissement,
                checklist,
                BASE_JURIDIQUE);
    }

    // ── Calculs internes ────────────────────────────────────────────────────

    private static BigDecimal computeRatio(BigDecimal montant, BigDecimal indemnite) {
        if (indemnite == null || indemnite.signum() <= 0) {
            return null;
        }
        return montant.multiply(BigDecimal.valueOf(100))
                .divide(indemnite, SCALE_INTERNE, RoundingMode.HALF_UP)
                .setScale(SCALE_RATIO, RoundingMode.HALF_UP);
    }

    private static String buildAvertissement(BigDecimal ratio) {
        if (ratio == null) {
            return null;
        }
        if (ratio.compareTo(SEUIL_RATIO_AVERTISSEMENT) >= 0) {
            return null;
        }
        // Format français : virgule + 1 décimale.
        String ratioStr = ratio.toPlainString().replace('.', ',');
        return String.format(AVERTISSEMENT_RATIO_LESION_TEMPLATE, ratioStr);
    }

    private static List<TransactionBeTravailChecklistItem> buildChecklist(
            List<String> renonciations, boolean ordrePublicDetectee) {
        if (renonciations == null || renonciations.isEmpty()) {
            return Collections.emptyList();
        }
        List<TransactionBeTravailChecklistItem> items = new ArrayList<>(renonciations.size());
        // Stratégie conservatrice V1 : si renonciation ordre public détectée,
        // tous les items sont invalides (signal global). Sinon tous valides.
        // L'analyse fine clause par clause est hors scope SF-213-06.
        boolean tousValides = !ordrePublicDetectee;
        for (String r : renonciations) {
            items.add(new TransactionBeTravailChecklistItem(r, tousValides));
        }
        return items;
    }

    private static void validateInputs(TransactionBeTravailRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.montantTransactionBrut() == null
                || request.montantTransactionBrut().signum() <= 0) {
            throw new IllegalArgumentException(
                    "montantTransactionBrut requis et strictement positif");
        }
        if (request.renonciationsListees() == null) {
            throw new IllegalArgumentException(
                    "renonciationsListees requise (peut être vide)");
        }
        if (request.indemniteLegaleEtimee() != null
                && request.indemniteLegaleEtimee().signum() < 0) {
            throw new IllegalArgumentException(
                    "indemniteLegaleEtimee ne peut pas être négative");
        }
    }
}
