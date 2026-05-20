package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * SF-207-06 : calculateur d'éligibilité au Régime de Chômage avec Complément
 * d'entreprise (RCC, ex-prépension belge).
 *
 * <p>Base juridique :
 * <ul>
 *   <li><b>CCT n°17</b> — régime général (âge ≥ 60 ans, carrière ≥ 40 ans)
 *       et variante "longue carrière" (âge ≥ 59 ans + flag longue carrière) ;</li>
 *   <li><b>CCT n°17/13</b> — régime métiers lourds (âge ≥ 58 ans, carrière
 *       ≥ 35 ans, métier lourd reconnu) ;</li>
 *   <li><b>AR du 03/05/2007</b> art. 3 (modalités générales) et art. 8
 *       (régime entreprise en difficulté — âge ≥ 60 ans + arrêté ministériel
 *       de reconnaissance).</li>
 * </ul>
 *
 * <p>Logique : évaluation parallèle des 4 régimes
 * (cf. {@link RccBeConditionsRegime}) puis :
 * <ul>
 *   <li>verdict {@link RccBeConditionsResult.Verdict#ELIGIBLE} dès qu'au
 *       moins un régime matche ; {@code regimeApplicable} déterminé par la
 *       priorité <b>GENERAL &gt; LONGUE_CARRIERE &gt; METIERS_LOURDS &gt;
 *       ENTREPRISE_DIFFICULTE</b> (du plus stable juridiquement au plus
 *       conjoncturel) ;</li>
 *   <li>verdict {@link RccBeConditionsResult.Verdict#INCERTAIN} si aucun
 *       régime ne matche mais l'âge ≥ 55 ans ET la carrière ≥ 30 ans
 *       (proche des seuils — l'avocat doit affiner ou attendre) ;</li>
 *   <li>verdict {@link RccBeConditionsResult.Verdict#NON_ELIGIBLE} sinon.</li>
 * </ul>
 *
 * <p>Pour INCERTAIN/NON_ELIGIBLE : {@code conditionsManquantes} détaille
 * les conditions non remplies dans le régime LE PLUS PROCHE (distance
 * euclidienne âge × carrière par rapport aux seuils du régime). En cas
 * d'égalité parfaite, la priorité régime tranche.</p>
 *
 * <p>Outil <b>BE-only</b> par construction — aucun équivalent FR direct
 * (mémoire {@code feedback_belgique_never_forget}). L'âge est calculé en
 * années pleines au fuseau {@link ZoneId#of(String) Europe/Brussels} à la
 * date de licenciement envisagée (default today).</p>
 */
public final class RccBeConditionsCalculator {

    /** Fuseau horaire belge — cohérent avec les autres outils BE. */
    private static final ZoneId ZONE_BRUSSELS = ZoneId.of("Europe/Brussels");

    // Seuils — régime GENERAL (CCT 17 art. 3)
    public static final int AGE_GENERAL = 60;
    public static final int CARRIERE_GENERAL = 40;

    // Seuils — régime METIERS_LOURDS (CCT 17/13)
    public static final int AGE_METIERS_LOURDS = 58;
    public static final int CARRIERE_METIERS_LOURDS = 35;

    // Seuils — régime LONGUE_CARRIERE (CCT 17 art. 3, variante)
    public static final int AGE_LONGUE_CARRIERE = 59;
    public static final int CARRIERE_LONGUE_CARRIERE = 40;

    // Seuils — régime ENTREPRISE_DIFFICULTE (AR 03/05/2007 art. 8)
    public static final int AGE_ENTREPRISE_DIFFICULTE = 60;

    // Seuils INCERTAIN — proche des seuils, l'avocat doit affiner
    public static final int SEUIL_INCERTAIN_AGE = 55;
    public static final int SEUIL_INCERTAIN_CARRIERE = 30;

    public static final String BASE_JURIDIQUE_COMPLETE =
            "CCT n°17 (régime général + longue carrière) ; "
                    + "CCT n°17/13 (régime métiers lourds) ; "
                    + "AR du 03/05/2007 art. 3 et 8 "
                    + "(modalités générales + entreprise en difficulté)";

    /**
     * Ordre d'évaluation des régimes — déterminé par la priorité de
     * sélection de {@code regimeApplicable} (le 1ᵉʳ matching = applicable).
     * Le même ordre est conservé dans {@code regimesEligibles}.
     */
    static final List<RccBeConditionsRegime> EVALUATION_ORDER = List.of(
            RccBeConditionsRegime.GENERAL,
            RccBeConditionsRegime.LONGUE_CARRIERE,
            RccBeConditionsRegime.METIERS_LOURDS,
            RccBeConditionsRegime.ENTREPRISE_DIFFICULTE
    );

    private RccBeConditionsCalculator() {
    }

    /**
     * Calcule l'éligibilité au RCC BE pour les inputs donnés. Fonction pure :
     * aucun effet de bord, déterministe à inputs égaux (sauf la résolution
     * de "aujourd'hui" si {@code dateLicenciementEnvisagee} est null).
     *
     * @param request payload (les contrôles Bean Validation et inter-champs
     *                ont déjà été passés en amont par le service ; le
     *                calculateur valide en plus la non-nullité défensive).
     * @return résultat structuré (verdict + régime applicable + régimes
     *         éligibles + âge à la date + conditions manquantes + base
     *         juridique + formule de calcul).
     * @throws IllegalArgumentException si {@code request} est null ou si
     *         {@code dateNaissance} / {@code anneesCarriereProfessionnelle}
     *         est manquant (défense en profondeur — Bean Validation amont).
     */
    public static RccBeConditionsResult compute(RccBeConditionsRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête RCC BE requise");
        }
        if (request.dateNaissance() == null) {
            throw new IllegalArgumentException("dateNaissance est requise");
        }
        if (request.anneesCarriereProfessionnelle() == null) {
            throw new IllegalArgumentException("anneesCarriereProfessionnelle est requise");
        }

        // Normalisations défensives — Bean Validation amont a déjà bornées
        // les valeurs ; on protège néanmoins le calculateur appelé directement.
        boolean metierLourd = Boolean.TRUE.equals(request.metierLourd());
        boolean longueCarriere = Boolean.TRUE.equals(request.longueCarriere());
        boolean entrepriseEnDifficulte = Boolean.TRUE.equals(request.entrepriseEnDifficulte());

        LocalDate dateLicenciement = request.dateLicenciementEnvisagee() != null
                ? request.dateLicenciementEnvisagee()
                : LocalDate.now(ZONE_BRUSSELS);

        int age = computeAge(request.dateNaissance(), dateLicenciement);
        int carriere = request.anneesCarriereProfessionnelle();

        // ---- Évaluation des 4 régimes parallèles ------------------------
        List<RccBeConditionsRegime> regimesEligibles = new ArrayList<>();
        for (RccBeConditionsRegime regime : EVALUATION_ORDER) {
            if (matches(regime, age, carriere, metierLourd, longueCarriere, entrepriseEnDifficulte)) {
                regimesEligibles.add(regime);
            }
        }

        // ---- Verdict + régime applicable --------------------------------
        RccBeConditionsResult.Verdict verdict;
        RccBeConditionsRegime regimeApplicable;
        List<RccBeConditionsCondition> conditionsManquantes;

        if (!regimesEligibles.isEmpty()) {
            verdict = RccBeConditionsResult.Verdict.ELIGIBLE;
            // EVALUATION_ORDER est aussi l'ordre de priorité : le premier matché
            // (donc le 1ᵉʳ de la liste) est le régime applicable.
            regimeApplicable = regimesEligibles.get(0);
            conditionsManquantes = List.of();
        } else if (age >= SEUIL_INCERTAIN_AGE && carriere >= SEUIL_INCERTAIN_CARRIERE) {
            verdict = RccBeConditionsResult.Verdict.INCERTAIN;
            regimeApplicable = null;
            conditionsManquantes = manquantesDuRegimeLePlusProche(
                    age, carriere, metierLourd, longueCarriere, entrepriseEnDifficulte);
        } else {
            verdict = RccBeConditionsResult.Verdict.NON_ELIGIBLE;
            regimeApplicable = null;
            conditionsManquantes = manquantesDuRegimeLePlusProche(
                    age, carriere, metierLourd, longueCarriere, entrepriseEnDifficulte);
        }

        String formule = buildFormule(age, carriere, verdict, regimeApplicable, regimesEligibles,
                metierLourd, longueCarriere, entrepriseEnDifficulte);

        return new RccBeConditionsResult(
                request.dateNaissance(),
                carriere,
                metierLourd,
                longueCarriere,
                entrepriseEnDifficulte,
                dateLicenciement,
                verdict,
                regimeApplicable,
                List.copyOf(regimesEligibles),
                age,
                carriere,
                conditionsManquantes,
                BASE_JURIDIQUE_COMPLETE,
                formule
        );
    }

    // =========================================================================
    // Helpers — évaluation des régimes
    // =========================================================================

    /**
     * Indique si {@code regime} matche pour les paramètres donnés. Aucun
     * effet de bord, intégralement déterministe.
     */
    private static boolean matches(RccBeConditionsRegime regime, int age, int carriere,
                                   boolean metierLourd, boolean longueCarriere,
                                   boolean entrepriseEnDifficulte) {
        return switch (regime) {
            case GENERAL -> age >= AGE_GENERAL && carriere >= CARRIERE_GENERAL;
            case METIERS_LOURDS -> age >= AGE_METIERS_LOURDS
                    && carriere >= CARRIERE_METIERS_LOURDS
                    && metierLourd;
            case LONGUE_CARRIERE -> age >= AGE_LONGUE_CARRIERE
                    && carriere >= CARRIERE_LONGUE_CARRIERE
                    && longueCarriere;
            case ENTREPRISE_DIFFICULTE -> age >= AGE_ENTREPRISE_DIFFICULTE
                    && entrepriseEnDifficulte;
        };
    }

    /**
     * Construit la liste des conditions non remplies pour un régime donné.
     * Ordre stable aligné sur l'enum {@link RccBeConditionsCondition}.
     */
    private static List<RccBeConditionsCondition> manquantesPour(RccBeConditionsRegime regime,
                                                                 int age, int carriere,
                                                                 boolean metierLourd,
                                                                 boolean longueCarriere,
                                                                 boolean entrepriseEnDifficulte) {
        List<RccBeConditionsCondition> out = new ArrayList<>();
        switch (regime) {
            case GENERAL -> {
                if (age < AGE_GENERAL) out.add(RccBeConditionsCondition.AGE_GENERAL_60);
                if (carriere < CARRIERE_GENERAL) out.add(RccBeConditionsCondition.CARRIERE_40);
            }
            case METIERS_LOURDS -> {
                if (age < AGE_METIERS_LOURDS) out.add(RccBeConditionsCondition.AGE_METIERS_LOURDS_58);
                if (carriere < CARRIERE_METIERS_LOURDS) out.add(RccBeConditionsCondition.CARRIERE_METIERS_LOURDS_35);
                if (!metierLourd) out.add(RccBeConditionsCondition.METIER_LOURD_FLAG);
            }
            case LONGUE_CARRIERE -> {
                // Réutilise AGE_GENERAL_60 pour l'âge < 59 ans (seuil le plus
                // commun ; LONGUE_CARRIERE_FLAG est la condition spécifique de
                // ce régime). On préfère ne pas créer une 8ᵉ valeur d'enum
                // "AGE_LONGUE_CARRIERE_59" pour ne pas alourdir l'API.
                if (age < AGE_LONGUE_CARRIERE) out.add(RccBeConditionsCondition.AGE_GENERAL_60);
                if (carriere < CARRIERE_LONGUE_CARRIERE) out.add(RccBeConditionsCondition.CARRIERE_40);
                if (!longueCarriere) out.add(RccBeConditionsCondition.LONGUE_CARRIERE_FLAG);
            }
            case ENTREPRISE_DIFFICULTE -> {
                if (age < AGE_ENTREPRISE_DIFFICULTE) out.add(RccBeConditionsCondition.AGE_GENERAL_60);
                if (!entrepriseEnDifficulte) out.add(RccBeConditionsCondition.ENTREPRISE_DIFFICULTE_FLAG);
            }
        }
        return out;
    }

    /**
     * Pour les verdicts INCERTAIN/NON_ELIGIBLE : sélectionne le régime LE
     * PLUS PROCHE (distance euclidienne sur les seuils âge × carrière du
     * régime) et renvoie ses conditions manquantes. En cas d'égalité de
     * distance, la priorité {@link #EVALUATION_ORDER} tranche.
     */
    private static List<RccBeConditionsCondition> manquantesDuRegimeLePlusProche(
            int age, int carriere, boolean metierLourd,
            boolean longueCarriere, boolean entrepriseEnDifficulte) {
        Map<RccBeConditionsRegime, Double> distances = new EnumMap<>(RccBeConditionsRegime.class);
        for (RccBeConditionsRegime regime : EVALUATION_ORDER) {
            distances.put(regime, distance(regime, age, carriere));
        }
        // EVALUATION_ORDER → en cas d'égalité, on prend le premier rencontré
        // (donc le plus prioritaire), comportement déterministe.
        RccBeConditionsRegime plusProche = EVALUATION_ORDER.get(0);
        double best = distances.get(plusProche);
        for (RccBeConditionsRegime regime : EVALUATION_ORDER) {
            double d = distances.get(regime);
            if (d < best) {
                best = d;
                plusProche = regime;
            }
        }
        return manquantesPour(plusProche, age, carriere, metierLourd, longueCarriere,
                entrepriseEnDifficulte);
    }

    /**
     * Distance euclidienne entre {@code (age, carriere)} et le couple de
     * seuils du régime ; on ne prend en compte que les composantes où le
     * seuil n'est pas atteint (sinon distance = 0 sur cet axe). Les flags
     * booléens n'entrent pas dans la distance — ils sont rappelés dans
     * {@code conditionsManquantes} mais ne déplacent pas la sélection.
     */
    private static double distance(RccBeConditionsRegime regime, int age, int carriere) {
        int seuilAge;
        int seuilCarriere;
        switch (regime) {
            case GENERAL -> { seuilAge = AGE_GENERAL; seuilCarriere = CARRIERE_GENERAL; }
            case METIERS_LOURDS -> { seuilAge = AGE_METIERS_LOURDS; seuilCarriere = CARRIERE_METIERS_LOURDS; }
            case LONGUE_CARRIERE -> { seuilAge = AGE_LONGUE_CARRIERE; seuilCarriere = CARRIERE_LONGUE_CARRIERE; }
            case ENTREPRISE_DIFFICULTE -> { seuilAge = AGE_ENTREPRISE_DIFFICULTE; seuilCarriere = 0; }
            default -> { seuilAge = AGE_GENERAL; seuilCarriere = CARRIERE_GENERAL; }
        }
        double dAge = age >= seuilAge ? 0 : (seuilAge - age);
        double dCar = carriere >= seuilCarriere ? 0 : (seuilCarriere - carriere);
        return Math.sqrt(dAge * dAge + dCar * dCar);
    }

    // =========================================================================
    // Helpers — âge + formule
    // =========================================================================

    /** Calcule l'âge en années pleines à {@code dateLicenciement}, défensif. */
    private static int computeAge(LocalDate dateNaissance, LocalDate dateLicenciement) {
        if (dateLicenciement.isBefore(dateNaissance)) {
            // Ce cas est rejeté par le service ; défense en profondeur ici.
            return 0;
        }
        return Period.between(dateNaissance, dateLicenciement).getYears();
    }

    /**
     * Compose une formule textuelle synthétique (≤ 500 car. en pratique),
     * incluse dans la réponse et persistée pour explication UI.
     */
    private static String buildFormule(int age, int carriere,
                                       RccBeConditionsResult.Verdict verdict,
                                       RccBeConditionsRegime regimeApplicable,
                                       List<RccBeConditionsRegime> regimesEligibles,
                                       boolean metierLourd, boolean longueCarriere,
                                       boolean entrepriseEnDifficulte) {
        StringBuilder sb = new StringBuilder();
        sb.append("Âge à la date envisagée : ").append(age).append(" ans ; ");
        sb.append("carrière : ").append(carriere).append(" ans");
        if (metierLourd) sb.append(" ; métier lourd");
        if (longueCarriere) sb.append(" ; longue carrière");
        if (entrepriseEnDifficulte) sb.append(" ; entreprise en difficulté");
        sb.append(". ");
        switch (verdict) {
            case ELIGIBLE -> {
                sb.append("Régime ").append(regimeApplicable.name()).append(" applicable");
                if (regimesEligibles.size() > 1) {
                    sb.append(" (régimes éligibles : ");
                    for (int i = 0; i < regimesEligibles.size(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(regimesEligibles.get(i).name());
                    }
                    sb.append(" — priorité GENERAL > LONGUE_CARRIERE > METIERS_LOURDS "
                            + "> ENTREPRISE_DIFFICULTE)");
                }
                sb.append('.');
            }
            case INCERTAIN -> sb.append("Aucun régime ne matche complètement, mais l'âge ≥ 55 ans "
                    + "et la carrière ≥ 30 ans : proche des seuils, l'avocat doit affiner les données "
                    + "ou attendre.");
            case NON_ELIGIBLE -> sb.append("Aucun régime RCC applicable, et les seuils restent éloignés "
                    + "(âge < 55 ans ou carrière < 30 ans) : procédure de fin de carrière standard.");
        }
        return sb.toString();
    }
}
