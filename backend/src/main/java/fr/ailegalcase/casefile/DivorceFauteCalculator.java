package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SF-FA-09-01 : calculateur du divorce pour faute FR (art. 242-246, 266, 270 Cciv).
 *
 * <p>Le divorce pour faute représente environ 10-15 % des divorces français.
 * Il suppose la preuve de faits constitutifs d'une <b>violation grave ou renouvelée
 * des devoirs et obligations du mariage</b> (fidélité, assistance, communauté de
 * vie — art. 212 et 215 Cciv), <b>rendant intolérable le maintien de la vie
 * commune</b> (art. 242 Cciv). La charge de la preuve pèse sur le demandeur.
 *
 * <p>Le juge dispose d'une marge d'appréciation : il peut prononcer le divorce
 * aux torts exclusifs du défendeur, aux torts partagés (art. 245 Cciv) si la
 * partie adverse invoque à son tour des fautes, ou rejeter la demande.
 *
 * <p>Conséquences accessoires :
 * <ul>
 *   <li><b>Dommages-intérêts art. 266 Cciv</b> : conditionnés à un préjudice
 *       d'une particulière gravité résultant de la dissolution du mariage
 *       (fourchette indicative jurisprudentielle 1 000 - 10 000 € par chef
 *       de faute retenu).</li>
 *   <li><b>Prestation compensatoire art. 270 Cciv</b> : indépendante des torts,
 *       due si rupture crée une disparité dans les conditions de vie
 *       (cf. F-FA-08 / art. 271).</li>
 * </ul>
 *
 * <p>Scoring 0-100 :
 * <ul>
 *   <li>Nombre de fautes × 15, plafonné à 45 (3+ fautes = max)</li>
 *   <li>+30 si preuves documentaires (constats huissier, témoignages, mains
 *       courantes, jugements antérieurs)</li>
 *   <li>+25 si pas de risque de torts partagés</li>
 * </ul>
 *
 * <p>Verdicts : ELEVEE (≥ 75), MOYENNE (40-74), FAIBLE (&lt; 40).
 *
 * <p>Outil <b>single-country FR</b>. Le régime BE (art. 229 § 3 Code civil BE)
 * repose sur une logique différente ("désunion irrémédiable") et n'est pas
 * couvert par cette SF.
 */
public final class DivorceFauteCalculator {

    public static final int SEUIL_ELEVEE = 75;
    public static final int SEUIL_MOYENNE = 40;

    /** Poids par faute invoquée (plafonné à 3 chefs effectifs). */
    public static final int POIDS_PAR_FAUTE = 15;

    /** Plafond de contribution des fautes au score. */
    public static final int POIDS_FAUTES_MAX = 45;

    /** Bonus preuves documentaires. */
    public static final int POIDS_PREUVES = 30;

    /** Bonus absence de risque torts partagés. */
    public static final int POIDS_PAS_TORTS_PARTAGES = 25;

    /** Fourchette basse indicative DI art. 266 (par chef de faute retenu). */
    public static final BigDecimal DI_ART_266_PAR_FAUTE_MIN = new BigDecimal("1000");

    /** Fourchette haute indicative DI art. 266 (par chef de faute retenu). */
    public static final BigDecimal DI_ART_266_PAR_FAUTE_MAX = new BigDecimal("10000");

    /** Coefficient FR prestation compensatoire (cf. F-FA-08 / PrestationCompensatoireCalculator). */
    public static final BigDecimal COEFF_PRESTATION_COMPENSATOIRE = new BigDecimal("0.30");

    /** Durée de référence conventionnelle prestation compensatoire (années). */
    public static final int DUREE_REFERENCE_PRESTATION_ANS = 8;

    private static final String BASE_JURIDIQUE =
            "Art. 242-246 + 266 + 270 Cciv";

    private DivorceFauteCalculator() {
    }

    public static DivorceFauteResult compute(List<String> fautesInvoquees,
                                             boolean preuvesDocumentaires,
                                             boolean tortsAdverseInvoques,
                                             int dureeMariageAnnees,
                                             BigDecimal revenusAnnuelsDemandeurEur,
                                             BigDecimal revenusAnnuelsDefendeurEur,
                                             LocalDate dateDepotAssignation) {
        List<String> fautes = validateAndNormalizeFautes(fautesInvoquees);
        validateNumericInputs(dureeMariageAnnees, revenusAnnuelsDemandeurEur, revenusAnnuelsDefendeurEur);

        int nombreFautes = fautes.size();
        boolean solidariteFautesOk = nombreFautes >= 1 && preuvesDocumentaires;
        boolean risqueTortsPartages = tortsAdverseInvoques;

        String verdictTortsEstimes = verdictTorts(solidariteFautesOk, risqueTortsPartages);

        int scoreGlobal = computeScore(nombreFautes, preuvesDocumentaires, risqueTortsPartages);
        String verdict = verdictProbabilite(scoreGlobal);

        BigDecimal[] di = computeDommagesInterets(nombreFautes, solidariteFautesOk, verdictTortsEstimes);
        BigDecimal diMin = di[0];
        BigDecimal diMax = di[1];

        BigDecimal[] prestation = computePrestationCompensatoire(
                revenusAnnuelsDemandeurEur, revenusAnnuelsDefendeurEur, dureeMariageAnnees);
        BigDecimal prestMin = prestation[0];
        BigDecimal prestMax = prestation[1];

        List<String> criteresNonRemplis = buildCriteresNonRemplis(
                nombreFautes, preuvesDocumentaires, risqueTortsPartages);

        List<String> messages = buildMessages(
                verdict, verdictTortsEstimes, nombreFautes, preuvesDocumentaires,
                risqueTortsPartages, diMax, prestMax);

        String formule = buildFormule(
                verdict, scoreGlobal, nombreFautes, preuvesDocumentaires,
                risqueTortsPartages, verdictTortsEstimes, diMin, diMax,
                prestMin, prestMax);

        return new DivorceFauteResult(
                fautes,
                preuvesDocumentaires,
                tortsAdverseInvoques,
                dureeMariageAnnees,
                revenusAnnuelsDemandeurEur,
                revenusAnnuelsDefendeurEur,
                dateDepotAssignation,
                nombreFautes,
                solidariteFautesOk,
                risqueTortsPartages,
                scoreGlobal,
                verdict,
                verdictTortsEstimes,
                diMin,
                diMax,
                prestMin,
                prestMax,
                criteresNonRemplis,
                formule,
                BASE_JURIDIQUE,
                messages
        );
    }

    private static List<String> validateAndNormalizeFautes(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> valides = Arrays.stream(DivorceFauteType.values())
                .map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> deduplicated = new LinkedHashSet<>();
        for (String code : raw) {
            if (code == null || code.isBlank()) {
                continue;
            }
            String trimmed = code.trim();
            if (!valides.contains(trimmed)) {
                throw new IllegalArgumentException("Faute inconnue : " + trimmed
                        + ". Valeurs : " + valides);
            }
            deduplicated.add(trimmed);
        }
        return new ArrayList<>(deduplicated);
    }

    private static void validateNumericInputs(int dureeMariageAnnees,
                                              BigDecimal revenusAnnuelsDemandeurEur,
                                              BigDecimal revenusAnnuelsDefendeurEur) {
        if (dureeMariageAnnees < 0) {
            throw new IllegalArgumentException("dureeMariageAnnees doit être positif ou nul");
        }
        if (revenusAnnuelsDemandeurEur != null && revenusAnnuelsDemandeurEur.signum() < 0) {
            throw new IllegalArgumentException("revenusAnnuelsDemandeurEur ne peut être négatif");
        }
        if (revenusAnnuelsDefendeurEur != null && revenusAnnuelsDefendeurEur.signum() < 0) {
            throw new IllegalArgumentException("revenusAnnuelsDefendeurEur ne peut être négatif");
        }
    }

    private static int computeScore(int nombreFautes,
                                    boolean preuvesDocumentaires,
                                    boolean risqueTortsPartages) {
        int scoreFautes = Math.min(nombreFautes * POIDS_PAR_FAUTE, POIDS_FAUTES_MAX);
        int scorePreuves = preuvesDocumentaires ? POIDS_PREUVES : 0;
        int scorePasTortsPartages = risqueTortsPartages ? 0 : POIDS_PAS_TORTS_PARTAGES;
        return Math.min(scoreFautes + scorePreuves + scorePasTortsPartages, 100);
    }

    private static String verdictProbabilite(int score) {
        if (score >= SEUIL_ELEVEE) return "ELEVEE";
        if (score >= SEUIL_MOYENNE) return "MOYENNE";
        return "FAIBLE";
    }

    private static String verdictTorts(boolean solidariteFautesOk, boolean risqueTortsPartages) {
        if (solidariteFautesOk && !risqueTortsPartages) {
            return "EXCLUSIF_DEFENDEUR";
        }
        if (solidariteFautesOk && risqueTortsPartages) {
            return "PARTAGES";
        }
        return "IMPREDICTIBLE";
    }

    private static BigDecimal[] computeDommagesInterets(int nombreFautes,
                                                        boolean solidariteFautesOk,
                                                        String verdictTortsEstimes) {
        if (!solidariteFautesOk || "IMPREDICTIBLE".equals(verdictTortsEstimes) || nombreFautes <= 0) {
            return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
        }
        BigDecimal nb = BigDecimal.valueOf(nombreFautes);
        BigDecimal min = DI_ART_266_PAR_FAUTE_MIN.multiply(nb);
        BigDecimal max = DI_ART_266_PAR_FAUTE_MAX.multiply(nb);

        if ("PARTAGES".equals(verdictTortsEstimes)) {
            min = min.divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP);
            max = max.divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP);
        }
        return new BigDecimal[]{
                min.setScale(2, RoundingMode.HALF_UP),
                max.setScale(2, RoundingMode.HALF_UP)
        };
    }

    /**
     * Prestation compensatoire indicative — même formule que F-FA-08 :
     * écart de revenus × coeff FR × 12 × durée de référence. Fourchette ±15 %.
     * Le JAF reste libre (art. 271 Cciv).
     */
    private static BigDecimal[] computePrestationCompensatoire(BigDecimal revenusDemandeur,
                                                               BigDecimal revenusDefendeur,
                                                               int dureeMariageAnnees) {
        if (revenusDemandeur == null || revenusDefendeur == null || dureeMariageAnnees <= 0) {
            return new BigDecimalRange(BigDecimal.ZERO, BigDecimal.ZERO).toArray();
        }
        BigDecimal ecart = revenusDemandeur.subtract(revenusDefendeur).abs();
        if (ecart.signum() <= 0) {
            return new BigDecimalRange(BigDecimal.ZERO, BigDecimal.ZERO).toArray();
        }
        // écart annuel × coeff × durée de référence
        BigDecimal montantCentral = ecart
                .multiply(COEFF_PRESTATION_COMPENSATOIRE)
                .multiply(BigDecimal.valueOf(DUREE_REFERENCE_PRESTATION_ANS));
        BigDecimal min = montantCentral.multiply(new BigDecimal("0.85")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal max = montantCentral.multiply(new BigDecimal("1.15")).setScale(2, RoundingMode.HALF_UP);
        return new BigDecimal[]{min, max};
    }

    private static List<String> buildCriteresNonRemplis(int nombreFautes,
                                                        boolean preuvesDocumentaires,
                                                        boolean risqueTortsPartages) {
        List<String> out = new ArrayList<>();
        if (nombreFautes == 0) {
            out.add("Aucune faute recevable invoquée (liste vide)");
        }
        if (!preuvesDocumentaires) {
            out.add("Preuves documentaires insuffisantes (constats huissier, témoignages, mains courantes, jugements antérieurs)");
        }
        if (risqueTortsPartages) {
            out.add("Risque de torts partagés (art. 245 Cciv) : la partie adverse peut invoquer des fautes en réplique");
        }
        return out;
    }

    private static List<String> buildMessages(String verdict,
                                              String verdictTorts,
                                              int nombreFautes,
                                              boolean preuvesDocumentaires,
                                              boolean risqueTortsPartages,
                                              BigDecimal diMax,
                                              BigDecimal prestMax) {
        List<String> messages = new ArrayList<>();
        messages.add("Charge de la preuve sur le demandeur (art. 9 CPC). Les fautes doivent constituer une violation grave ou renouvelée des devoirs du mariage rendant intolérable le maintien de la vie commune (art. 242 Cciv).");
        messages.add("Preuves recommandées : constats d'huissier, témoignages (attestations art. 202 CPC), mains courantes, plaintes, jugements pénaux antérieurs, SMS/emails (avec précautions RGPD et loyauté de la preuve).");
        messages.add("Dommages-intérêts art. 266 Cciv : réservés aux cas où la dissolution du mariage a des conséquences d'une particulière gravité. Fourchette indicative — le juge apprécie souverainement le préjudice moral et matériel.");
        messages.add("Prestation compensatoire art. 270 Cciv : indépendante des torts. Elle est due si la rupture crée une disparité dans les conditions de vie respectives. Voir outil F-FA-08 pour une analyse dédiée (art. 271).");
        messages.add("Le JAF peut prononcer le divorce aux torts exclusifs, aux torts partagés (art. 245 Cciv), ou débouter le demandeur si les fautes ne sont pas établies.");

        if (risqueTortsPartages) {
            messages.add("Risque torts partagés : anticiper la demande reconventionnelle adverse et préparer la défense sur les faits allégués.");
        }
        if (nombreFautes >= 3) {
            messages.add("Nombre élevé de fautes invoquées : renforce la démonstration de « violation renouvelée » (art. 242 Cciv).");
        }
        if (!preuvesDocumentaires) {
            messages.add("Sans preuves documentaires, l'issue devient incertaine : privilégier un divorce par consentement mutuel (art. 229-1 Cciv) ou pour acceptation du principe de la rupture (art. 233 Cciv) si l'autre partie est d'accord.");
        }
        if ("ELEVEE".equals(verdict) && "EXCLUSIF_DEFENDEUR".equals(verdictTorts)) {
            messages.add("Probabilité élevée de divorce aux torts exclusifs du défendeur : DI art. 266 indicatifs jusqu'à "
                    + formatMontant(diMax) + " € et prestation compensatoire jusqu'à " + formatMontant(prestMax) + " € (art. 270).");
        }
        return messages;
    }

    private static String buildFormule(String verdict,
                                       int scoreGlobal,
                                       int nombreFautes,
                                       boolean preuvesDocumentaires,
                                       boolean risqueTortsPartages,
                                       String verdictTortsEstimes,
                                       BigDecimal diMin, BigDecimal diMax,
                                       BigDecimal prestMin, BigDecimal prestMax) {
        StringBuilder sb = new StringBuilder();
        sb.append("Divorce pour faute (art. 242 Cciv) : probabilité ")
                .append(verdict).append(" (score ").append(scoreGlobal).append("/100), ")
                .append("verdict torts estimé ").append(verdictTortsEstimes).append(". ");

        sb.append(nombreFautes).append(" faute(s) invoquée(s), preuves ")
                .append(preuvesDocumentaires ? "OK" : "KO")
                .append(", risque torts partagés ")
                .append(risqueTortsPartages ? "OUI" : "NON")
                .append(". ");

        sb.append("DI art. 266 indicatif [").append(formatMontant(diMin))
                .append(" ; ").append(formatMontant(diMax)).append("] €. ");
        sb.append("Prestation compensatoire art. 270 indicative [")
                .append(formatMontant(prestMin)).append(" ; ")
                .append(formatMontant(prestMax)).append("] €.");
        return sb.toString();
    }

    private static String formatMontant(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private record BigDecimalRange(BigDecimal min, BigDecimal max) {
        BigDecimal[] toArray() { return new BigDecimal[]{min, max}; }
    }
}
