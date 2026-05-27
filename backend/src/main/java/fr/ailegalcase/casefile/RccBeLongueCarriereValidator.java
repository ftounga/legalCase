package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * SF-219-02 : analyseur d'éligibilité au RCC BE longue carrière — fonction
 * <b>pure</b> (sans dépendance HTTP / persistance / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 26 décembre 2013</b> concernant l'introduction d'un statut
 *       unique entre ouvriers et employés en ce qui concerne les délais de
 *       préavis et le jour de carence ainsi que de mesures d'accompagnement
 *       — base légale du repositionnement du RCC après réforme du pacte de
 *       solidarité entre les générations.</li>
 *   <li><b>CCT n° 17 du 19/12/1974</b> (Conseil National du Travail) —
 *       instaure le régime d'indemnité complémentaire aux travailleurs âgés
 *       en cas de licenciement.</li>
 *   <li><b>AR du 03/05/2007</b> fixant le RCC, <b>art. 3</b> — régime
 *       spécifique <b>longue carrière</b> : conditions d'âge ≥ 59 ans à la
 *       fin du contrat et carrière professionnelle ≥ 40 ans (jours équivalents
 *       temps plein).</li>
 * </ul>
 *
 * <h2>Conditions cumulatives (toutes requises)</h2>
 * <ol>
 *   <li>Âge à la fin du contrat ≥ <b>59 ans</b>.</li>
 *   <li>Carrière professionnelle totale ≥ <b>40 ans</b> (jours ETP).</li>
 *   <li>Licenciement effectif — la démission est exclue du dispositif
 *       (CCT 17 art. 3).</li>
 * </ol>
 *
 * <h2>Hiérarchie des raisons d'inéligibilité</h2>
 * <p>Lorsque plusieurs conditions échouent, l'ordre de priorité retenu est :
 * <ol>
 *   <li>{@code DEMISSION} (showstopper) ;</li>
 *   <li>{@code AGE_INSUFFISANT} ;</li>
 *   <li>{@code CARRIERE_INSUFFISANTE}.</li>
 * </ol>
 * Le détail des 3 conditions reste toujours retourné via les flags
 * {@code conditionXxxRemplie} pour permettre à l'UI de présenter le bilan
 * complet à l'avocat.</p>
 *
 * <h2>Indemnité complémentaire indicative</h2>
 * <p>Calcul indicatif <b>uniquement si éligible et si les données financières
 * sont fournies</b> :
 * {@code 0,5 × (remunerationNetteMensuelleReference - allocationChomageMensuelleEstimee)}.
 * Le résultat est arrondi au cent près (HALF_UP). Plancher à zéro si la
 * différence est négative ou nulle (allocation déjà supérieure ou égale à la
 * référence). Cette valeur sert d'estimation à charge de l'employeur,
 * <b>l'ONEM</b> gérant la composante chômage. À confirmer par calcul ONSS /
 * ONEM réel — d'où l'avertissement systématique.</p>
 */
public final class RccBeLongueCarriereValidator {

    static final String BASE_JURIDIQUE =
            "Loi du 26/12/2013 (pacte de solidarité entre les générations) ; "
                    + "CCT n° 17 du 19/12/1974 (CNT — RCC) ; "
                    + "AR du 03/05/2007 art. 3 (régime longue carrière 59+/40)";

    static final String AVERT_INDEMNITE_INDICATIVE =
            "Le calcul de l'indemnité complémentaire est indicatif — montant "
                    + "définitif à confirmer par calcul ONSS / ONEM en tenant "
                    + "compte des plafonds, du précompte professionnel et de "
                    + "l'âge légal de la pension.";

    /** Seuil d'âge légal RCC longue carrière (art. 3 AR 03/05/2007). */
    private static final int AGE_MIN_ANNEES = 59;

    /** Seuil de carrière (jours ETP) RCC longue carrière. */
    private static final int CARRIERE_MIN_ANNEES = 40;

    /** Coefficient indemnité complémentaire mensuelle (CCT n° 17). */
    private static final BigDecimal COEFFICIENT_INDEMNITE =
            new BigDecimal("0.50");

    private RccBeLongueCarriereValidator() {
    }

    public static RccBeLongueCarriereResult analyze(
            RccBeLongueCarriereRequest request) {
        validateInputs(request);

        boolean licenciementOk = Boolean.TRUE.equals(request.licenciementEffectif());
        boolean ageOk = request.ageFinContrat() >= AGE_MIN_ANNEES;
        boolean carriereOk = request.anneesCarriereTotale() >= CARRIERE_MIN_ANNEES;

        // Verdict avec priorité : démission > âge > carrière > éligible
        RccBeLongueCarriereVerdict verdict;
        if (!licenciementOk) {
            verdict = RccBeLongueCarriereVerdict.INELIGIBLE_DEMISSION;
        } else if (!ageOk) {
            verdict = RccBeLongueCarriereVerdict.INELIGIBLE_AGE_INSUFFISANT;
        } else if (!carriereOk) {
            verdict = RccBeLongueCarriereVerdict.INELIGIBLE_CARRIERE_INSUFFISANTE;
        } else {
            verdict = RccBeLongueCarriereVerdict.ELIGIBLE_RCC_LONGUE_CARRIERE;
        }
        boolean eligible =
                verdict == RccBeLongueCarriereVerdict.ELIGIBLE_RCC_LONGUE_CARRIERE;

        // Indemnité complémentaire indicative — uniquement si éligible
        // et si les 2 montants financiers sont fournis.
        BigDecimal indemniteComplementaire = null;
        String avertissement = null;
        if (eligible
                && request.remunerationNetteMensuelleReference() != null
                && request.allocationChomageMensuelleEstimee() != null) {
            BigDecimal diff = request.remunerationNetteMensuelleReference()
                    .subtract(request.allocationChomageMensuelleEstimee());
            if (diff.signum() <= 0) {
                indemniteComplementaire = BigDecimal.ZERO.setScale(2);
            } else {
                indemniteComplementaire = diff.multiply(COEFFICIENT_INDEMNITE)
                        .setScale(2, RoundingMode.HALF_UP);
            }
            avertissement = AVERT_INDEMNITE_INDICATIVE;
        }

        String synthese = buildSynthese(verdict, ageOk, carriereOk, licenciementOk);

        return new RccBeLongueCarriereResult(
                request.ageFinContrat(),
                request.anneesCarriereTotale(),
                request.dateFinContrat(),
                licenciementOk,
                request.remunerationNetteMensuelleReference(),
                request.allocationChomageMensuelleEstimee(),
                verdict,
                eligible,
                ageOk,
                carriereOk,
                licenciementOk,
                indemniteComplementaire,
                synthese,
                BASE_JURIDIQUE,
                avertissement);
    }

    // ── Synthèse en langage avocat ─────────────────────────────────────────

    private static String buildSynthese(
            RccBeLongueCarriereVerdict verdict,
            boolean ageOk,
            boolean carriereOk,
            boolean licenciementOk) {
        return switch (verdict) {
            case ELIGIBLE_RCC_LONGUE_CARRIERE -> "Conditions cumulatives "
                    + "remplies (âge ≥ 59 ans, carrière ≥ 40 ans, "
                    + "licenciement effectif) — RCC longue carrière ouvert "
                    + "(AR 03/05/2007 art. 3).";
            case INELIGIBLE_DEMISSION -> "Démission : le RCC longue carrière "
                    + "est réservé au licenciement à l'initiative de "
                    + "l'employeur (CCT n° 17, art. 3).";
            case INELIGIBLE_AGE_INSUFFISANT -> "Âge à la fin du contrat < 59 ans : "
                    + "condition d'âge non remplie (AR 03/05/2007 art. 3). "
                    + (carriereOk
                            ? "La condition de carrière est remplie."
                            : "La condition de carrière n'est pas non plus remplie.");
            case INELIGIBLE_CARRIERE_INSUFFISANTE -> "Carrière professionnelle "
                    + "< 40 ans : condition de carrière non remplie "
                    + "(AR 03/05/2007 art. 3). "
                    + (ageOk
                            ? "La condition d'âge est remplie."
                            : "La condition d'âge n'est pas non plus remplie.");
        };
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    private static void validateInputs(RccBeLongueCarriereRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.ageFinContrat() == null) {
            throw new IllegalArgumentException("ageFinContrat est requis");
        }
        if (request.ageFinContrat() < 0) {
            throw new IllegalArgumentException(
                    "ageFinContrat ne peut pas être négatif");
        }
        if (request.anneesCarriereTotale() == null) {
            throw new IllegalArgumentException("anneesCarriereTotale est requise");
        }
        if (request.anneesCarriereTotale() < 0) {
            throw new IllegalArgumentException(
                    "anneesCarriereTotale ne peut pas être négative");
        }
        if (request.dateFinContrat() == null) {
            throw new IllegalArgumentException("dateFinContrat est requise");
        }
        if (request.licenciementEffectif() == null) {
            throw new IllegalArgumentException("licenciementEffectif est requis");
        }
        // Cohérence financière : si l'une des deux valeurs est fournie,
        // l'autre est attendue pour le calcul indicatif. On n'échoue pas
        // (couple optionnel), mais on rejette les valeurs strictement
        // négatives qui n'auraient aucun sens métier.
        if (request.remunerationNetteMensuelleReference() != null
                && request.remunerationNetteMensuelleReference().signum() < 0) {
            throw new IllegalArgumentException(
                    "remunerationNetteMensuelleReference ne peut pas être négative");
        }
        if (request.allocationChomageMensuelleEstimee() != null
                && request.allocationChomageMensuelleEstimee().signum() < 0) {
            throw new IllegalArgumentException(
                    "allocationChomageMensuelleEstimee ne peut pas être négative");
        }
    }
}
