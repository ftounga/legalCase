package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * SF-207-07 : calculateur de l'<b>indemnité complémentaire RCC</b> (Régime
 * de Chômage avec Complément d'entreprise) due par l'employeur belge au
 * salarié RCC, conformément à la CCT n°17 art. 5.
 *
 * <p>Base juridique :
 * <ul>
 *   <li><b>CCT n°17 art. 5</b> — l'indemnité complémentaire est égale à
 *       la moitié de la différence entre la rémunération nette de
 *       référence et l'allocation de chômage :
 *       {@code indemnité = (remunerationNetteReference − allocationOnem) / 2}.
 *       Plancher 0 (l'employeur ne réclame jamais au salarié si l'allocation
 *       ONEM est supérieure à la rémunération nette de référence).</li>
 *   <li><b>Loi du 3 juillet 1978</b> — fondement substantiel du contrat
 *       de travail.</li>
 *   <li><b>AR du 3 mai 2007</b> — modalités RCC (notamment art. 3 et 8).</li>
 *   <li><b>CCT sectorielles</b> — si l'utilisateur indique un plancher
 *       sectoriel (montant fixe €/mois), l'indemnité ne peut être
 *       inférieure à ce plancher (max applicable).</li>
 * </ul>
 *
 * <p>Logique :
 * <ol>
 *   <li>{@code indemniteAvantPlancher = max((remunNette − allocOnem) / 2, 0)} ;</li>
 *   <li>{@code indemnite = max(indemniteAvantPlancher, planchersSectoriel ?? 0)} ;</li>
 *   <li>{@code planchersApplique = (planchersSectoriel != null && planchersSectoriel > indemniteAvantPlancher)} ;</li>
 *   <li>{@code dateAgeLegalPension = dateNaissance + ageLegalPension années} (défaut 66) ;</li>
 *   <li>{@code moisRestants = max(MONTHS entre dateDebutRcc et dateAgeLegalPension, 0)} ;</li>
 *   <li>{@code montantTotal = indemnite × moisRestants}.</li>
 * </ol>
 *
 * <p>Tous calculs en {@link BigDecimal} (précision centimes,
 * {@link RoundingMode#HALF_EVEN}, 2 décimales) — conformément au plan de
 * test de la mini-spec.</p>
 *
 * <p>Outil <b>BE-only</b> par construction — aucun équivalent FR direct
 * (mémoire {@code feedback_belgique_never_forget}). Les régimes français
 * d'aménagement de fin de carrière sont distincts.</p>
 */
public final class RccBeIndemniteCalculator {

    /** Âge légal par défaut de la pension (2026 — 67 ans à partir de 2030). */
    public static final int AGE_LEGAL_PENSION_DEFAUT = 66;

    /** Précision monétaire (centimes). */
    public static final int SCALE_MONETAIRE = 2;

    /** Mode d'arrondi monétaire — HALF_EVEN (banker's rounding). */
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public static final String BASE_JURIDIQUE_COMPLETE =
            "CCT 17 art. 5 ; loi du 3 juillet 1978 ; arrêté royal du 3 mai 2007";

    private static final BigDecimal DEUX = new BigDecimal("2");

    private RccBeIndemniteCalculator() {
    }

    /**
     * Calcule l'indemnité complémentaire RCC pour les inputs donnés. Fonction
     * pure : aucun effet de bord, déterministe à inputs égaux.
     *
     * @param request payload (les contrôles Bean Validation et inter-champs
     *                ont déjà été passés en amont par le service ; le
     *                calculateur valide en plus la non-nullité défensive).
     * @return résultat structuré (indemnité mensuelle + total + base juridique
     *         + formule de calcul).
     * @throws IllegalArgumentException si {@code request} est null ou si un
     *         champ requis est manquant (défense en profondeur — Bean
     *         Validation amont).
     */
    public static RccBeIndemniteResult compute(RccBeIndemniteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête RCC BE indemnité requise");
        }
        if (request.remunerationNetteReference() == null) {
            throw new IllegalArgumentException("remunerationNetteReference est requise");
        }
        if (request.allocationOnemMensuelle() == null) {
            throw new IllegalArgumentException("allocationOnemMensuelle est requise");
        }
        if (request.dateNaissanceSalarie() == null) {
            throw new IllegalArgumentException("dateNaissanceSalarie est requise");
        }
        if (request.dateDebutRcc() == null) {
            throw new IllegalArgumentException("dateDebutRcc est requise");
        }

        // Normalisations défensives
        BigDecimal remunNette = scale(request.remunerationNetteReference());
        BigDecimal allocOnem = scale(request.allocationOnemMensuelle());
        BigDecimal planchersSectoriel = request.planchersSectoriel() != null
                ? scale(request.planchersSectoriel())
                : null;
        int ageLegalPension = request.ageLegalPension() != null
                ? request.ageLegalPension()
                : AGE_LEGAL_PENSION_DEFAUT;

        // ---- 1) Indemnité avant plancher = (remunNette - allocOnem) / 2, ≥ 0
        BigDecimal indemniteAvantPlancher = remunNette
                .subtract(allocOnem)
                .divide(DEUX, SCALE_MONETAIRE, ROUNDING);
        if (indemniteAvantPlancher.signum() < 0) {
            indemniteAvantPlancher = BigDecimal.ZERO.setScale(SCALE_MONETAIRE, ROUNDING);
        }

        // ---- 2) Indemnité finale = max(indemniteAvantPlancher, planchersSectoriel)
        BigDecimal indemnite;
        boolean planchersApplique;
        if (planchersSectoriel != null && planchersSectoriel.compareTo(indemniteAvantPlancher) > 0) {
            indemnite = planchersSectoriel;
            planchersApplique = true;
        } else {
            indemnite = indemniteAvantPlancher;
            planchersApplique = false;
        }

        // ---- 3) Date à laquelle le salarié atteint l'âge légal de la pension
        LocalDate dateAgeLegalPension = request.dateNaissanceSalarie().plusYears(ageLegalPension);

        // ---- 4) Mois restants entre dateDebutRcc et dateAgeLegalPension (≥ 0)
        long moisRestants = ChronoUnit.MONTHS.between(request.dateDebutRcc(), dateAgeLegalPension);
        if (moisRestants < 0) {
            moisRestants = 0;
        }

        // ---- 5) Montant total = indemnite × moisRestants
        BigDecimal montantTotal = indemnite
                .multiply(BigDecimal.valueOf(moisRestants))
                .setScale(SCALE_MONETAIRE, ROUNDING);

        String formule = buildFormule(remunNette, allocOnem, indemniteAvantPlancher,
                indemnite, planchersApplique, planchersSectoriel,
                moisRestants, montantTotal);

        return new RccBeIndemniteResult(
                remunNette,
                allocOnem,
                request.dateNaissanceSalarie(),
                request.dateDebutRcc(),
                ageLegalPension,
                planchersSectoriel,
                indemniteAvantPlancher,
                indemnite,
                planchersApplique,
                dateAgeLegalPension,
                moisRestants,
                montantTotal,
                BASE_JURIDIQUE_COMPLETE,
                formule
        );
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Normalise un montant en 2 décimales HALF_EVEN. */
    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(SCALE_MONETAIRE, ROUNDING);
    }

    /**
     * Construit la formule textuelle synthétique (≤ 500 car. en pratique),
     * incluse dans la réponse et persistée pour explication UI.
     */
    private static String buildFormule(BigDecimal remunNette,
                                       BigDecimal allocOnem,
                                       BigDecimal indemniteAvantPlancher,
                                       BigDecimal indemnite,
                                       boolean planchersApplique,
                                       BigDecimal planchersSectoriel,
                                       long moisRestants,
                                       BigDecimal montantTotal) {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(remunNette).append(" € − ").append(allocOnem)
                .append(" €) / 2 = ").append(indemniteAvantPlancher).append(" € / mois");
        if (planchersApplique) {
            sb.append(" ; plancher sectoriel ").append(planchersSectoriel)
                    .append(" € appliqué → indemnité = ").append(indemnite).append(" € / mois");
        } else if (planchersSectoriel != null) {
            sb.append(" ; plancher sectoriel ").append(planchersSectoriel)
                    .append(" € non applicable (≤ indemnité CCT 17)");
        }
        sb.append(" × ").append(moisRestants).append(" mois = ")
                .append(montantTotal).append(" €.");
        return sb.toString();
    }
}
