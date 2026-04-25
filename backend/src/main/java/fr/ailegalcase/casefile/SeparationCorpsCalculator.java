package fr.ailegalcase.casefile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-FA-21-01 : calculateur "Séparation de corps + conversion en divorce"
 * (art. 296 et s. + art. 306 Code civil + Loi n° 2004-439 du 26/05/2004).
 *
 * <p>La séparation de corps est une procédure matrimoniale autonome qui peut
 * être prononcée par les mêmes 4 modes que le divorce (art. 296 Cciv) :
 * <ul>
 *   <li>{@code CONSENTEMENT_MUTUEL} — art. 296 + 229-1 Cciv</li>
 *   <li>{@code ACCEPTATION_PRINCIPE} — art. 296 par renvoi à l'art. 233</li>
 *   <li>{@code FAUTE} — art. 296 par renvoi à l'art. 242</li>
 *   <li>{@code ALTERATION_DEFINITIVE} — art. 296 par renvoi à l'art. 237</li>
 * </ul>
 *
 * <p>Conversion en divorce — art. 306 Cciv (issu loi 26/05/2004) :
 * lorsque la séparation de corps a duré <b>au moins deux ans</b>, le jugement
 * de séparation est converti de plein droit en jugement de divorce sur la
 * demande de l'un des époux. La demande de réconciliation
 * (art. 305 Cciv) bloque la conversion.
 *
 * <p>Scoring 0-100 (verdict {@link #verdict(int, boolean, boolean)}) :
 * <ul>
 *   <li>+40 si délai 2 ans atteint (critère légal art. 306)</li>
 *   <li>+25 si consentement mutuel à la conversion</li>
 *   <li>+20 si pas de demande de réconciliation formulée (art. 305)</li>
 *   <li>+15 si pas d'enfants mineurs OU si consentement mutuel à la conversion
 *       (présence enfants ne bloque pas si accord)</li>
 * </ul>
 *
 * <p>Verdicts :
 * <ul>
 *   <li>{@code RECONCILIATION_BLOQUE} — réconciliation art. 305 (priorité)</li>
 *   <li>{@code PREMATUREE} — délai 2 ans non atteint, ou score &lt; 70</li>
 *   <li>{@code POSSIBLE} — score ≥ 70 + délai 2 ans atteint + pas réconciliation</li>
 * </ul>
 *
 * <p>Outil <b>single-country FRANCE</b>. La séparation de corps belge
 * (art. 213 et s. Code civil BE) suit un régime distinct — backlog.
 */
public final class SeparationCorpsCalculator {

    /** Délai légal de conversion automatique (art. 306 Cciv). */
    public static final int DELAI_CONVERSION_ANNEES = 2;

    /** Seuil score pour verdict POSSIBLE. */
    public static final int SEUIL_POSSIBLE = 70;

    public static final String MODE_CONSENTEMENT_MUTUEL = "CONSENTEMENT_MUTUEL";
    public static final String MODE_ACCEPTATION_PRINCIPE = "ACCEPTATION_PRINCIPE";
    public static final String MODE_FAUTE = "FAUTE";
    public static final String MODE_ALTERATION_DEFINITIVE = "ALTERATION_DEFINITIVE";

    public static final String VERDICT_POSSIBLE = "POSSIBLE";
    public static final String VERDICT_PREMATUREE = "PREMATUREE";
    public static final String VERDICT_RECONCILIATION_BLOQUE = "RECONCILIATION_BLOQUE";

    private static final List<String> MODES_VALIDES = List.of(
            MODE_CONSENTEMENT_MUTUEL, MODE_ACCEPTATION_PRINCIPE,
            MODE_FAUTE, MODE_ALTERATION_DEFINITIVE);

    private static final String BASE_JURIDIQUE =
            "Art. 296 et s. + 306 Code civil + Loi n° 2004-439 du 26/05/2004";

    private SeparationCorpsCalculator() {
    }

    public static SeparationCorpsResult compute(String modeProcedure,
                                                LocalDate dateJugementSeparationCorps,
                                                LocalDate dateRequeteConversion,
                                                int dureeSeparationAnnees,
                                                boolean consentementMutuelConversion,
                                                boolean patrimoineCommun,
                                                int enfantsMineurs,
                                                boolean demandeReconciliationFormulee) {
        return compute(modeProcedure, dateJugementSeparationCorps, dateRequeteConversion,
                dureeSeparationAnnees, consentementMutuelConversion, patrimoineCommun,
                enfantsMineurs, demandeReconciliationFormulee,
                Clock.system(ZoneId.of("Europe/Paris")));
    }

    public static SeparationCorpsResult compute(String modeProcedure,
                                                LocalDate dateJugementSeparationCorps,
                                                LocalDate dateRequeteConversion,
                                                int dureeSeparationAnnees,
                                                boolean consentementMutuelConversion,
                                                boolean patrimoineCommun,
                                                int enfantsMineurs,
                                                boolean demandeReconciliationFormulee,
                                                Clock clock) {
        validateInputs(modeProcedure, dateJugementSeparationCorps, dateRequeteConversion,
                dureeSeparationAnnees, enfantsMineurs, clock);

        boolean dureeSeparationOk = dureeSeparationAnnees >= DELAI_CONVERSION_ANNEES;
        boolean delaiConversion2AnsAtteint = dureeSeparationOk;
        boolean conversionAutomatiquePossible =
                dureeSeparationOk && !demandeReconciliationFormulee;

        int score = computeScore(dureeSeparationOk, consentementMutuelConversion,
                demandeReconciliationFormulee, enfantsMineurs);

        String verdict = verdict(score, dureeSeparationOk, demandeReconciliationFormulee);

        List<String> messages = buildMessages(modeProcedure, dureeSeparationOk,
                conversionAutomatiquePossible, demandeReconciliationFormulee,
                consentementMutuelConversion, enfantsMineurs, patrimoineCommun);

        String formule = buildFormule(modeProcedure, dureeSeparationAnnees,
                consentementMutuelConversion, demandeReconciliationFormulee,
                verdict, score);

        return new SeparationCorpsResult(
                modeProcedure,
                dateJugementSeparationCorps,
                dateRequeteConversion,
                dureeSeparationAnnees,
                consentementMutuelConversion,
                patrimoineCommun,
                enfantsMineurs,
                demandeReconciliationFormulee,
                dureeSeparationOk,
                delaiConversion2AnsAtteint,
                conversionAutomatiquePossible,
                score,
                verdict,
                BASE_JURIDIQUE,
                formule,
                messages);
    }

    private static int computeScore(boolean dureeSeparationOk,
                                    boolean consentementMutuelConversion,
                                    boolean demandeReconciliationFormulee,
                                    int enfantsMineurs) {
        int score = 0;
        if (dureeSeparationOk) score += 40;
        if (consentementMutuelConversion) score += 25;
        if (!demandeReconciliationFormulee) score += 20;
        if (enfantsMineurs == 0 || consentementMutuelConversion) score += 15;
        return Math.min(score, 100);
    }

    private static String verdict(int score, boolean dureeSeparationOk,
                                  boolean demandeReconciliationFormulee) {
        if (demandeReconciliationFormulee) return VERDICT_RECONCILIATION_BLOQUE;
        if (!dureeSeparationOk) return VERDICT_PREMATUREE;
        if (score >= SEUIL_POSSIBLE) return VERDICT_POSSIBLE;
        return VERDICT_PREMATUREE;
    }

    private static void validateInputs(String modeProcedure,
                                       LocalDate dateJugementSeparationCorps,
                                       LocalDate dateRequeteConversion,
                                       int dureeSeparationAnnees,
                                       int enfantsMineurs,
                                       Clock clock) {
        if (modeProcedure == null) {
            throw new IllegalArgumentException("modeProcedure est requis");
        }
        if (!MODES_VALIDES.contains(modeProcedure)) {
            throw new IllegalArgumentException("modeProcedure inconnu : " + modeProcedure);
        }
        if (dureeSeparationAnnees < 0) {
            throw new IllegalArgumentException("dureeSeparationAnnees doit être positif ou nul");
        }
        if (enfantsMineurs < 0) {
            throw new IllegalArgumentException("enfantsMineurs doit être positif ou nul");
        }
        LocalDate today = LocalDate.now(clock);
        if (dateJugementSeparationCorps != null && dateJugementSeparationCorps.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateJugementSeparationCorps ne peut pas être dans le futur");
        }
        if (dateRequeteConversion != null && dateRequeteConversion.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateRequeteConversion ne peut pas être dans le futur");
        }
        if (dateJugementSeparationCorps != null && dateRequeteConversion != null
                && dateRequeteConversion.isBefore(dateJugementSeparationCorps)) {
            throw new IllegalArgumentException(
                    "dateRequeteConversion ne peut pas être antérieure à dateJugementSeparationCorps");
        }
    }

    private static List<String> buildMessages(String modeProcedure,
                                              boolean dureeSeparationOk,
                                              boolean conversionAutomatiquePossible,
                                              boolean demandeReconciliationFormulee,
                                              boolean consentementMutuelConversion,
                                              int enfantsMineurs,
                                              boolean patrimoineCommun) {
        List<String> messages = new ArrayList<>();
        messages.add("Séparation de corps : procédure matrimoniale autonome (art. 296 Cciv) — "
                + "elle relâche le devoir de cohabitation mais maintient le mariage.");
        messages.add("Conversion automatique en divorce art. 306 Cciv si délai 2 ans atteint, "
                + "à la demande de l'un des époux — sauf demande de réconciliation art. 305.");

        if (demandeReconciliationFormulee) {
            messages.add("Réconciliation art. 305 Cciv : la demande bloque la conversion. "
                    + "La séparation de corps cesse, le mariage reprend ses pleins effets.");
        }
        if (!dureeSeparationOk) {
            messages.add("Délai de 2 ans non atteint : la conversion ne peut être prononcée "
                    + "automatiquement avant cette échéance (art. 306 al. 1 Cciv).");
        }
        if (conversionAutomatiquePossible) {
            messages.add("Conversion possible : déposer une requête en conversion devant le JAF "
                    + "(art. 1136 et s. CPC). Le juge ne peut refuser sauf preuve d'une "
                    + "reprise effective de la vie commune.");
        }
        if (consentementMutuelConversion && dureeSeparationOk) {
            messages.add("Consentement mutuel à la conversion : voie la plus rapide, divorce "
                    + "prononcé sur acte d'avocat (art. 229-1 par renvoi).");
        }
        if (enfantsMineurs > 0) {
            messages.add("Présence d'enfants mineurs (" + enfantsMineurs + ") : audition possible "
                    + "(art. 388-1 Cciv) et révision des mesures relatives aux enfants après "
                    + "conversion en divorce (résidence, autorité parentale, contribution).");
        }
        if (patrimoineCommun) {
            messages.add("Patrimoine commun : la séparation de corps emporte séparation de biens "
                    + "(art. 302 Cciv) — vérifier la liquidation du régime matrimonial déjà "
                    + "réalisée lors de la séparation, ou à effectuer lors de la conversion.");
        }

        if (MODE_CONSENTEMENT_MUTUEL.equals(modeProcedure)) {
            messages.add("Mode consentement mutuel art. 296 + 229-1 : voie la plus rapide initialement.");
        } else if (MODE_FAUTE.equals(modeProcedure)) {
            messages.add("Mode faute art. 242 : la conversion ne préjuge pas des torts retenus "
                    + "lors de la séparation, sauf accord exprès lors de la conversion (art. 306 al. 2).");
        } else if (MODE_ALTERATION_DEFINITIVE.equals(modeProcedure)) {
            messages.add("Mode altération définitive art. 237 : la séparation continue à courir "
                    + "indépendamment de la cessation de cohabitation antérieure.");
        } else if (MODE_ACCEPTATION_PRINCIPE.equals(modeProcedure)) {
            messages.add("Mode acceptation du principe art. 233 par renvoi : PV signé irrévocable.");
        }

        return messages;
    }

    private static String buildFormule(String modeProcedure,
                                       int dureeSeparationAnnees,
                                       boolean consentementMutuelConversion,
                                       boolean demandeReconciliationFormulee,
                                       String verdict,
                                       int score) {
        StringBuilder sb = new StringBuilder();
        sb.append("Séparation de corps prononcée (mode ").append(modeProcedure).append(")");
        sb.append(" + ").append(dureeSeparationAnnees).append(" an(s)");
        if (consentementMutuelConversion) {
            sb.append(" + consentement mutuel");
        }
        if (demandeReconciliationFormulee) {
            sb.append(" + réconciliation art. 305");
        }
        switch (verdict) {
            case VERDICT_POSSIBLE -> sb.append(" → conversion possible art. 306");
            case VERDICT_PREMATUREE -> sb.append(" → conversion prématurée");
            case VERDICT_RECONCILIATION_BLOQUE -> sb.append(" → conversion bloquée par réconciliation");
            default -> sb.append(" → ").append(verdict);
        }
        sb.append(" (score ").append(score).append("/100).");
        return sb.toString();
    }
}
