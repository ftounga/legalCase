package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * SF-207-01 : calculateur des délais de prescription des actions Travail BE.
 *
 * <p>Base juridique :
 * <ul>
 *   <li><b>Loi du 3 juillet 1978 relative aux contrats de travail, art. 15 al. 1</b> —
 *       les actions nées du contrat sont prescrites <b>1 an après la cessation
 *       du contrat</b>.</li>
 *   <li><b>Loi du 3 juillet 1978, art. 15 al. 2</b> — actions nées au cours
 *       du contrat : <b>5 ans</b> à compter du fait qui les a donné lieu,
 *       sans pouvoir excéder 1 an post-cessation pour les actions ex-contrat.</li>
 *   <li><b>CCT n° 109 art. 11</b> — action en motivation du licenciement /
 *       indemnité de licenciement manifestement déraisonnable : <b>1 an</b>
 *       à compter de la cessation du contrat.</li>
 *   <li><b>Code civil art. 2262bis</b> — arriérés de salaire : prescription
 *       quinquennale (5 ans) à compter de l'échéance de paiement.</li>
 * </ul>
 *
 * <p>Logique (table de la mini-spec) :
 * <table>
 *   <tr><th>typeCreance</th><th>Délai</th><th>Point de départ</th></tr>
 *   <tr><td>EX_CONTRAT_GENERAL</td><td>1 an</td><td>date de rupture</td></tr>
 *   <tr><td>EX_CONTRAT_CCT_109</td><td>1 an</td><td>date de rupture</td></tr>
 *   <tr><td>PENDANT_CONTRAT</td><td>5 ans</td><td>fait générateur (saisi via dateRupture)</td></tr>
 *   <tr><td>ARRIERES_SALAIRE</td><td>5 ans</td><td>échéance de paiement (saisie via dateRupture)</td></tr>
 * </table>
 *
 * <p>Verdict :
 * <ul>
 *   <li>{@code PRESCRIT} si {@code joursRestants ≤ 0}.</li>
 *   <li>{@code IMMINENT} si {@code 0 < joursRestants ≤ 30} (frontière inclusive).</li>
 *   <li>{@code NON_PRESCRIT} sinon.</li>
 * </ul>
 *
 * <p>Fuseau Europe/Brussels pour le calcul {@code today} par défaut.</p>
 *
 * <p>Outil <b>BE-only</b> par construction — aucune logique FR (cf. mémoire
 * {@code feedback_belgique_never_forget}). L'équivalent FR (L.1471-1 et s.
 * Code travail) est un régime juridiquement distinct.</p>
 */
public final class PrescriptionBeLitigeTravailCalculator {

    public static final String VERDICT_PRESCRIT = "PRESCRIT";
    public static final String VERDICT_IMMINENT = "IMMINENT";
    public static final String VERDICT_NON_PRESCRIT = "NON_PRESCRIT";

    /** Seuil de joursRestants au-dessous duquel on bascule en IMMINENT (inclusif). */
    public static final int SEUIL_IMMINENT_JOURS = 30;

    public static final String REGLE_1_AN_POST_RUPTURE = "1_AN_POST_RUPTURE_LOI_1978_ART_15";
    public static final String REGLE_1_AN_CCT_109 = "1_AN_CCT_109_ART_11";
    public static final String REGLE_5_ANS_PENDANT_CONTRAT = "5_ANS_PENDANT_CONTRAT";
    public static final String REGLE_5_ANS_ARRIERES_SALAIRE = "5_ANS_ARRIERES_SALAIRE";

    public static final String BASE_LOI_1978 = "Loi du 3 juillet 1978 art. 15";
    public static final String BASE_LOI_1978_ET_CCT_109 =
            "Loi du 3 juillet 1978 art. 15 ; CCT 109 art. 11";
    public static final String BASE_LOI_1978_ET_CC_2262BIS =
            "Loi du 3 juillet 1978 art. 15 al. 2 ; Code civil art. 2262bis";

    /** Fuseau horaire belge — cohérent avec les autres outils BE. */
    public static final ZoneId ZONE_BRUSSELS = ZoneId.of("Europe/Brussels");

    private PrescriptionBeLitigeTravailCalculator() {
    }

    /**
     * Calcule le verdict de prescription Travail BE pour une rupture / créance
     * donnée. Fonction pure : aucun effet de bord, déterministe à inputs égaux.
     *
     * @param request payload normalisé (validé en amont par le service)
     * @return résultat structuré (verdict + date limite + jours restants +
     *         règle appliquée + base juridique + formule de calcul)
     * @throws IllegalArgumentException si {@code typeCreance} est invalide,
     *         {@code dateRupture} manquante, ou {@code dateActionEnvisagee}
     *         strictement antérieure à {@code dateRupture}.
     */
    public static PrescriptionBeLitigeTravailResult compute(PrescriptionBeLitigeTravailRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête de prescription requise");
        }
        PrescriptionBeLitigeTravailTypeCreance type = parseTypeCreance(request.typeCreance());

        LocalDate dateRupture = request.dateRupture();
        if (dateRupture == null) {
            throw new IllegalArgumentException(
                    "dateRupture est requise (date de rupture du contrat ou date d'échéance / fait générateur)");
        }

        LocalDate dateAction = request.dateActionEnvisagee() != null
                ? request.dateActionEnvisagee()
                : LocalDate.now(ZONE_BRUSSELS);

        if (dateAction.isBefore(dateRupture)) {
            throw new IllegalArgumentException(
                    "dateActionEnvisagee doit être postérieure ou égale à dateRupture");
        }

        int delaiAnnees;
        String regleAppliquee;
        String baseJuridique;
        String labelDelaiHumain;
        switch (type) {
            case EX_CONTRAT_GENERAL -> {
                delaiAnnees = 1;
                regleAppliquee = REGLE_1_AN_POST_RUPTURE;
                baseJuridique = BASE_LOI_1978;
                labelDelaiHumain = "1 an";
            }
            case EX_CONTRAT_CCT_109 -> {
                delaiAnnees = 1;
                regleAppliquee = REGLE_1_AN_CCT_109;
                baseJuridique = BASE_LOI_1978_ET_CCT_109;
                labelDelaiHumain = "1 an";
            }
            case PENDANT_CONTRAT -> {
                delaiAnnees = 5;
                regleAppliquee = REGLE_5_ANS_PENDANT_CONTRAT;
                baseJuridique = BASE_LOI_1978;
                labelDelaiHumain = "5 ans";
            }
            case ARRIERES_SALAIRE -> {
                delaiAnnees = 5;
                regleAppliquee = REGLE_5_ANS_ARRIERES_SALAIRE;
                baseJuridique = BASE_LOI_1978_ET_CC_2262BIS;
                labelDelaiHumain = "5 ans";
            }
            default -> throw new IllegalArgumentException("typeCreance non supporté : " + type);
        }

        LocalDate dateLimite = dateRupture.plusYears(delaiAnnees);
        long joursRestants = ChronoUnit.DAYS.between(dateAction, dateLimite);
        String verdict = computeVerdict(joursRestants);
        String formule = buildFormule(dateRupture, labelDelaiHumain, dateLimite, joursRestants);

        return new PrescriptionBeLitigeTravailResult(
                dateRupture,
                type.name(),
                dateAction,
                verdict,
                dateLimite,
                joursRestants,
                regleAppliquee,
                baseJuridique,
                formule);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static PrescriptionBeLitigeTravailTypeCreance parseTypeCreance(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("typeCreance est requis");
        }
        try {
            return PrescriptionBeLitigeTravailTypeCreance.valueOf(code.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("typeCreance invalide : " + code
                    + ". Valeurs : EX_CONTRAT_GENERAL, EX_CONTRAT_CCT_109, PENDANT_CONTRAT, ARRIERES_SALAIRE");
        }
    }

    private static String computeVerdict(long joursRestants) {
        if (joursRestants <= 0) {
            return VERDICT_PRESCRIT;
        }
        if (joursRestants <= SEUIL_IMMINENT_JOURS) {
            return VERDICT_IMMINENT;
        }
        return VERDICT_NON_PRESCRIT;
    }

    private static String buildFormule(LocalDate dateRupture, String labelDelai,
                                       LocalDate dateLimite, long joursRestants) {
        return "dateRupture (" + dateRupture + ") + " + labelDelai
                + " = dateLimite (" + dateLimite + ") ; joursRestants = " + joursRestants;
    }
}
