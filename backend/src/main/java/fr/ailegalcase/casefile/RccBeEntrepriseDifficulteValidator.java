package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * SF-219-03 : analyseur d'éligibilité au RCC BE entreprise en difficulté /
 * restructuration — fonction <b>pure</b> (sans dépendance HTTP / persistance
 * / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 26 décembre 2013</b> concernant l'introduction d'un statut
 *       unique entre ouvriers et employés en ce qui concerne les délais de
 *       préavis et le jour de carence ainsi que de mesures d'accompagnement
 *       — base légale du repositionnement du RCC après réforme du pacte
 *       de solidarité entre les générations.</li>
 *   <li><b>CCT n° 17 du 19/12/1974</b> (Conseil National du Travail) —
 *       instaure le régime d'indemnité complémentaire aux travailleurs âgés
 *       en cas de licenciement.</li>
 *   <li><b>AR du 03/05/2007</b> fixant le RCC — cadre général.</li>
 *   <li><b>AR de reconnaissance entreprise en difficulté / restructuration</b>
 *       (M.B. cas par cas) — fixe l'âge réduit dérogatoire de chaque plan
 *       agréé par le ministre de l'Emploi.</li>
 *   <li><b>CCT sectorielle ad hoc</b> — assortit la reconnaissance et fixe
 *       les conditions complémentaires d'ouverture du régime dérogatoire.</li>
 * </ul>
 *
 * <h2>Conditions cumulatives (toutes requises)</h2>
 * <ol>
 *   <li><b>Reconnaissance ministérielle</b> de l'entreprise comme étant en
 *       difficulté ({@link RccBeEntrepriseDifficulteTypeEnum#EN_DIFFICULTE})
 *       OU en restructuration
 *       ({@link RccBeEntrepriseDifficulteTypeEnum#EN_RESTRUCTURATION}) —
 *       sans reconnaissance, aucun régime dérogatoire n'est ouvert.</li>
 *   <li>Âge à la fin du contrat ≥ <b>âge réduit du plan reconnu</b>
 *       (typiquement 55 ans, paramétrable selon la décision ministre).</li>
 *   <li>Carrière professionnelle totale ≥ <b>10 ans</b> (jours ETP).</li>
 *   <li>Ancienneté secteur / entreprise reconnue ≥ <b>5 ans</b>.</li>
 *   <li>Licenciement effectif — la démission est exclue du dispositif
 *       (CCT 17 art. 3).</li>
 * </ol>
 *
 * <h2>Hiérarchie des raisons d'inéligibilité</h2>
 * <p>Lorsque plusieurs conditions échouent, l'ordre de priorité retenu est :
 * <ol>
 *   <li>{@code DEMISSION} (showstopper — CCT 17) ;</li>
 *   <li>{@code RECONNAISSANCE_ABSENTE} (showstopper — pas de plan) ;</li>
 *   <li>{@code AGE_INSUFFISANT} ;</li>
 *   <li>{@code CARRIERE_INSUFFISANTE} ;</li>
 *   <li>{@code ANCIENNETE_INSUFFISANTE}.</li>
 * </ol>
 * Le détail des 5 conditions reste toujours retourné via les flags
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
public final class RccBeEntrepriseDifficulteValidator {

    static final String BASE_JURIDIQUE =
            "Loi du 26/12/2013 (pacte de solidarité entre les générations) ; "
                    + "CCT n° 17 du 19/12/1974 (CNT — RCC) ; "
                    + "AR du 03/05/2007 (RCC) ; "
                    + "AR de reconnaissance entreprise en difficulté / "
                    + "restructuration (M.B. cas par cas) + CCT sectorielle ad hoc";

    static final String AVERT_INDEMNITE_INDICATIVE =
            "Le calcul de l'indemnité complémentaire est indicatif — montant "
                    + "définitif à confirmer par calcul ONSS / ONEM en tenant "
                    + "compte des plafonds, du précompte professionnel et de "
                    + "l'âge légal de la pension. L'âge réduit dérogatoire et "
                    + "la durée de validité du plan dépendent de la décision "
                    + "ministérielle de reconnaissance — à vérifier au M.B.";

    /** Seuil minimum de carrière (jours ETP) RCC entreprise difficulté. */
    private static final int CARRIERE_MIN_ANNEES = 10;

    /** Seuil minimum d'ancienneté sectorielle / entreprise. */
    private static final int ANCIENNETE_MIN_ANNEES = 5;

    /** Coefficient indemnité complémentaire mensuelle (CCT n° 17). */
    private static final BigDecimal COEFFICIENT_INDEMNITE =
            new BigDecimal("0.50");

    private RccBeEntrepriseDifficulteValidator() {
    }

    public static RccBeEntrepriseDifficulteResult analyze(
            RccBeEntrepriseDifficulteRequest request) {
        validateInputs(request);

        boolean licenciementOk = Boolean.TRUE.equals(request.licenciementEffectif());
        boolean reconnaissanceOk =
                request.typeReconnaissance() != null
                        && request.typeReconnaissance()
                        != RccBeEntrepriseDifficulteTypeEnum.NON_RECONNUE;
        boolean ageOk = reconnaissanceOk
                && request.ageFinContrat() >= request.ageReduitPlan();
        boolean carriereOk = request.anneesCarriereTotale() >= CARRIERE_MIN_ANNEES;
        boolean ancienneteOk =
                request.anneesAncienneteSecteur() >= ANCIENNETE_MIN_ANNEES;

        // Verdict avec priorité :
        //   démission > reconnaissance > âge > carrière > ancienneté > éligible
        RccBeEntrepriseDifficulteVerdict verdict;
        if (!licenciementOk) {
            verdict = RccBeEntrepriseDifficulteVerdict.INELIGIBLE_DEMISSION;
        } else if (!reconnaissanceOk) {
            verdict = RccBeEntrepriseDifficulteVerdict
                    .INELIGIBLE_RECONNAISSANCE_ABSENTE;
        } else if (!ageOk) {
            verdict = RccBeEntrepriseDifficulteVerdict.INELIGIBLE_AGE_INSUFFISANT;
        } else if (!carriereOk) {
            verdict = RccBeEntrepriseDifficulteVerdict
                    .INELIGIBLE_CARRIERE_INSUFFISANTE;
        } else if (!ancienneteOk) {
            verdict = RccBeEntrepriseDifficulteVerdict
                    .INELIGIBLE_ANCIENNETE_INSUFFISANTE;
        } else {
            verdict = RccBeEntrepriseDifficulteVerdict
                    .ELIGIBLE_RCC_ENTREPRISE_DIFFICULTE;
        }
        boolean eligible = verdict == RccBeEntrepriseDifficulteVerdict
                .ELIGIBLE_RCC_ENTREPRISE_DIFFICULTE;

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

        String synthese = buildSynthese(
                verdict, request, reconnaissanceOk, ageOk, carriereOk,
                ancienneteOk, licenciementOk);

        return new RccBeEntrepriseDifficulteResult(
                request.typeReconnaissance(),
                request.ageReduitPlan(),
                request.ageFinContrat(),
                request.anneesCarriereTotale(),
                request.anneesAncienneteSecteur(),
                request.dateFinContrat(),
                licenciementOk,
                request.remunerationNetteMensuelleReference(),
                request.allocationChomageMensuelleEstimee(),
                verdict,
                eligible,
                reconnaissanceOk,
                ageOk,
                carriereOk,
                ancienneteOk,
                licenciementOk,
                indemniteComplementaire,
                synthese,
                BASE_JURIDIQUE,
                avertissement);
    }

    // ── Synthèse en langage avocat ─────────────────────────────────────────

    private static String buildSynthese(
            RccBeEntrepriseDifficulteVerdict verdict,
            RccBeEntrepriseDifficulteRequest request,
            boolean reconnaissanceOk,
            boolean ageOk,
            boolean carriereOk,
            boolean ancienneteOk,
            boolean licenciementOk) {
        return switch (verdict) {
            case ELIGIBLE_RCC_ENTREPRISE_DIFFICULTE -> "Conditions cumulatives "
                    + "remplies (reconnaissance "
                    + reconnaissanceLabel(request.typeReconnaissance())
                    + ", âge ≥ " + request.ageReduitPlan() + " ans (plan "
                    + "reconnu), carrière ≥ 10 ans, ancienneté secteur ≥ 5 ans, "
                    + "licenciement effectif) — RCC entreprise en difficulté / "
                    + "restructuration ouvert.";
            case INELIGIBLE_DEMISSION -> "Démission : le RCC entreprise en "
                    + "difficulté est réservé au licenciement à l'initiative "
                    + "de l'employeur (CCT n° 17, art. 3).";
            case INELIGIBLE_RECONNAISSANCE_ABSENTE -> "Entreprise non reconnue "
                    + "en difficulté ou en restructuration par le ministre de "
                    + "l'Emploi : sans cette reconnaissance, aucun régime "
                    + "dérogatoire d'âge réduit n'est ouvert. Vérifier l'AR "
                    + "/ CCT sectorielle au Moniteur belge.";
            case INELIGIBLE_AGE_INSUFFISANT -> "Âge à la fin du contrat < "
                    + request.ageReduitPlan() + " ans (seuil du plan reconnu) : "
                    + "condition d'âge non remplie. "
                    + (carriereOk && ancienneteOk
                            ? "Les conditions de carrière et d'ancienneté sont remplies."
                            : "D'autres conditions ne sont pas non plus remplies.");
            case INELIGIBLE_CARRIERE_INSUFFISANTE -> "Carrière professionnelle "
                    + "< 10 ans : condition de carrière non remplie. "
                    + (ageOk && ancienneteOk
                            ? "Les conditions d'âge et d'ancienneté sont remplies."
                            : "D'autres conditions ne sont pas non plus remplies.");
            case INELIGIBLE_ANCIENNETE_INSUFFISANTE -> "Ancienneté sectorielle "
                    + "/ dans l'entreprise reconnue < 5 ans : condition "
                    + "d'ancienneté non remplie. "
                    + (ageOk && carriereOk
                            ? "Les conditions d'âge et de carrière sont remplies."
                            : "D'autres conditions ne sont pas non plus remplies.");
        };
    }

    private static String reconnaissanceLabel(
            RccBeEntrepriseDifficulteTypeEnum t) {
        if (t == null) {
            return "absente";
        }
        return switch (t) {
            case EN_DIFFICULTE -> "« entreprise en difficulté »";
            case EN_RESTRUCTURATION -> "« entreprise en restructuration »";
            case NON_RECONNUE -> "absente";
        };
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    private static void validateInputs(RccBeEntrepriseDifficulteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.typeReconnaissance() == null) {
            throw new IllegalArgumentException("typeReconnaissance est requis");
        }
        if (request.ageReduitPlan() == null) {
            throw new IllegalArgumentException("ageReduitPlan est requis");
        }
        if (request.ageReduitPlan() < 0) {
            throw new IllegalArgumentException(
                    "ageReduitPlan ne peut pas être négatif");
        }
        if (request.ageFinContrat() == null) {
            throw new IllegalArgumentException("ageFinContrat est requis");
        }
        if (request.ageFinContrat() < 0) {
            throw new IllegalArgumentException(
                    "ageFinContrat ne peut pas être négatif");
        }
        if (request.anneesCarriereTotale() == null) {
            throw new IllegalArgumentException(
                    "anneesCarriereTotale est requise");
        }
        if (request.anneesCarriereTotale() < 0) {
            throw new IllegalArgumentException(
                    "anneesCarriereTotale ne peut pas être négative");
        }
        if (request.anneesAncienneteSecteur() == null) {
            throw new IllegalArgumentException(
                    "anneesAncienneteSecteur est requise");
        }
        if (request.anneesAncienneteSecteur() < 0) {
            throw new IllegalArgumentException(
                    "anneesAncienneteSecteur ne peut pas être négative");
        }
        if (request.dateFinContrat() == null) {
            throw new IllegalArgumentException("dateFinContrat est requise");
        }
        if (request.licenciementEffectif() == null) {
            throw new IllegalArgumentException("licenciementEffectif est requis");
        }
        // Cohérence métier : ancienneté ≤ carrière totale
        if (request.anneesAncienneteSecteur() > request.anneesCarriereTotale()) {
            throw new IllegalArgumentException(
                    "anneesAncienneteSecteur ne peut pas être supérieure "
                            + "à anneesCarriereTotale (l'ancienneté secteur "
                            + "est nécessairement incluse dans la carrière "
                            + "totale)");
        }
        // Cohérence financière : valeurs négatives strictement interdites
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
