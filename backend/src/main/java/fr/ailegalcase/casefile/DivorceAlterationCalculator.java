package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-FA-08-01 : calculateur pour le <b>divorce pour altération définitive du
 * lien conjugal</b>, art. 237-238 Cciv (réforme loi 23/03/2019). Le plus
 * fréquent des divorces contentieux non fautifs. Conditions objectives :
 * cessation effective de communauté de vie ≥ 1 an au moment de l'assignation,
 * sans volonté de reprise.
 *
 * <p>Le délai est apprécié à la date de l'assignation si elle existe, sinon à
 * la date du jour (simulation pré-procédure).
 *
 * <p>Scoring pondéré 0-100 :
 * <ul>
 *   <li>+40 si délai objectif ≥ 1 an</li>
 *   <li>+25 si preuves documentaires de séparation</li>
 *   <li>+20 si absence de tentative de réconciliation</li>
 *   <li>+15 bonus si durée de mariage ≥ 5 ans (facteur de stabilité)</li>
 * </ul>
 *
 * <p>Verdicts : ELEVEE (≥ 75), MOYENNE (40-74), FAIBLE (&lt; 40).
 *
 * <p>Prestation compensatoire estimée (art. 270 Cciv) :
 * <ul>
 *   <li>facteur = 0,3 × dureeMariageAnnees (plafonné à 10)</li>
 *   <li>fourchetteMin = |revenus1 - revenus2| × facteur × 0,5</li>
 *   <li>fourchetteMax = |revenus1 - revenus2| × facteur × 1,5</li>
 * </ul>
 *
 * <p>Outil <b>single-country FR</b>. BE = F-FA-11 (procédure distincte).
 */
public final class DivorceAlterationCalculator {

    /** Délai objectif minimum de séparation (art. 238 Cciv après loi 2019). */
    public static final double DELAI_OBJECTIF_ANNEES = 1.0;

    /** Durée de mariage déclenchant le bonus (foyer stable). */
    public static final int DUREE_MARIAGE_BONUS_ANNEES = 5;

    /** Facteur d'années de mariage plafonné. */
    public static final int FACTEUR_MARIAGE_PLAFOND = 10;

    /** Seuil score pour verdict ELEVEE. */
    public static final int SEUIL_ELEVEE = 75;

    /** Seuil score pour verdict MOYENNE. */
    public static final int SEUIL_MOYENNE = 40;

    private static final String BASE_JURIDIQUE = "Art. 237-238 Cciv (loi 23/03/2019)";

    private DivorceAlterationCalculator() {
    }

    public static DivorceAlterationResult compute(LocalDate dateCessationVieCommune,
                                                  boolean preuvesSeparationDocumentaires,
                                                  boolean tentativesReconciliation,
                                                  int dureeMariageAnnees,
                                                  BigDecimal revenusAnnuelsEpoux1Eur,
                                                  BigDecimal revenusAnnuelsEpoux2Eur,
                                                  boolean patrimoineCommunSignificatif,
                                                  LocalDate dateAssignation) {
        return compute(dateCessationVieCommune, preuvesSeparationDocumentaires,
                tentativesReconciliation, dureeMariageAnnees,
                revenusAnnuelsEpoux1Eur, revenusAnnuelsEpoux2Eur,
                patrimoineCommunSignificatif, dateAssignation,
                Clock.system(ZoneId.of("Europe/Paris")));
    }

    public static DivorceAlterationResult compute(LocalDate dateCessationVieCommune,
                                                  boolean preuvesSeparationDocumentaires,
                                                  boolean tentativesReconciliation,
                                                  int dureeMariageAnnees,
                                                  BigDecimal revenusAnnuelsEpoux1Eur,
                                                  BigDecimal revenusAnnuelsEpoux2Eur,
                                                  boolean patrimoineCommunSignificatif,
                                                  LocalDate dateAssignation,
                                                  Clock clock) {
        validateInputs(dateCessationVieCommune, dureeMariageAnnees,
                revenusAnnuelsEpoux1Eur, revenusAnnuelsEpoux2Eur,
                dateAssignation, clock);

        LocalDate today = LocalDate.now(clock);
        LocalDate dateApreciationDelai = dateAssignation != null ? dateAssignation : today;
        long jours = ChronoUnit.DAYS.between(dateCessationVieCommune, dateApreciationDelai);
        double dureeSeparationAnnees = jours / 365.25;
        // Arrondi 2 décimales
        dureeSeparationAnnees = BigDecimal.valueOf(dureeSeparationAnnees)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();

        boolean delaiObjectifOk = dureeSeparationAnnees >= DELAI_OBJECTIF_ANNEES;
        boolean absencePreuveReconciliation = !tentativesReconciliation;
        boolean conditionsReunies = delaiObjectifOk
                && preuvesSeparationDocumentaires
                && absencePreuveReconciliation;

        int scoreGlobal = computeScore(delaiObjectifOk, preuvesSeparationDocumentaires,
                absencePreuveReconciliation, dureeMariageAnnees);
        String verdict = verdict(scoreGlobal);

        List<String> criteresNonRemplis = buildCriteresNonRemplis(
                delaiObjectifOk, preuvesSeparationDocumentaires,
                absencePreuveReconciliation, dureeSeparationAnnees);

        BigDecimal[] fourchette = computePrestationCompensatoireFourchette(
                revenusAnnuelsEpoux1Eur, revenusAnnuelsEpoux2Eur, dureeMariageAnnees);

        List<String> messages = buildMessages(verdict, conditionsReunies,
                dureeSeparationAnnees, patrimoineCommunSignificatif,
                fourchette[0], fourchette[1]);

        String formule = buildFormule(verdict, scoreGlobal, dureeSeparationAnnees,
                delaiObjectifOk, preuvesSeparationDocumentaires,
                absencePreuveReconciliation, dureeMariageAnnees,
                fourchette[0], fourchette[1]);

        return new DivorceAlterationResult(
                dateCessationVieCommune,
                preuvesSeparationDocumentaires,
                tentativesReconciliation,
                dureeMariageAnnees,
                revenusAnnuelsEpoux1Eur,
                revenusAnnuelsEpoux2Eur,
                patrimoineCommunSignificatif,
                dateAssignation,
                dureeSeparationAnnees,
                delaiObjectifOk,
                absencePreuveReconciliation,
                conditionsReunies,
                scoreGlobal,
                verdict,
                criteresNonRemplis,
                fourchette[0],
                fourchette[1],
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    private static int computeScore(boolean delaiObjectifOk,
                                    boolean preuvesSeparationDocumentaires,
                                    boolean absencePreuveReconciliation,
                                    int dureeMariageAnnees) {
        int score = 0;
        if (delaiObjectifOk) score += 40;
        if (preuvesSeparationDocumentaires) score += 25;
        if (absencePreuveReconciliation) score += 20;
        if (dureeMariageAnnees >= DUREE_MARIAGE_BONUS_ANNEES) score += 15;
        return Math.min(score, 100);
    }

    private static String verdict(int score) {
        if (score >= SEUIL_ELEVEE) return "ELEVEE";
        if (score >= SEUIL_MOYENNE) return "MOYENNE";
        return "FAIBLE";
    }

    private static BigDecimal[] computePrestationCompensatoireFourchette(BigDecimal r1,
                                                                         BigDecimal r2,
                                                                         int dureeMariageAnnees) {
        if (r1 == null || r2 == null) {
            return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
        }
        BigDecimal differentiel = r1.subtract(r2).abs();
        double facteur = 0.3 * Math.min(dureeMariageAnnees, FACTEUR_MARIAGE_PLAFOND);
        BigDecimal base = differentiel.multiply(BigDecimal.valueOf(facteur));
        BigDecimal min = base.multiply(BigDecimal.valueOf(0.5)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal max = base.multiply(BigDecimal.valueOf(1.5)).setScale(2, RoundingMode.HALF_UP);
        return new BigDecimal[]{min, max};
    }

    private static void validateInputs(LocalDate dateCessationVieCommune,
                                       int dureeMariageAnnees,
                                       BigDecimal revenus1,
                                       BigDecimal revenus2,
                                       LocalDate dateAssignation,
                                       Clock clock) {
        if (dateCessationVieCommune == null) {
            throw new IllegalArgumentException("dateCessationVieCommune est requise");
        }
        LocalDate today = LocalDate.now(clock);
        if (dateCessationVieCommune.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateCessationVieCommune ne peut pas être dans le futur");
        }
        if (dureeMariageAnnees < 0) {
            throw new IllegalArgumentException("dureeMariageAnnees doit être positif ou nul");
        }
        if (revenus1 != null && revenus1.signum() < 0) {
            throw new IllegalArgumentException("revenusAnnuelsEpoux1Eur doit être positif ou nul");
        }
        if (revenus2 != null && revenus2.signum() < 0) {
            throw new IllegalArgumentException("revenusAnnuelsEpoux2Eur doit être positif ou nul");
        }
        if (dateAssignation != null && dateAssignation.isBefore(dateCessationVieCommune)) {
            throw new IllegalArgumentException(
                    "dateAssignation ne peut pas être antérieure à dateCessationVieCommune");
        }
    }

    private static List<String> buildCriteresNonRemplis(boolean delaiObjectifOk,
                                                        boolean preuvesSeparationDocumentaires,
                                                        boolean absencePreuveReconciliation,
                                                        double dureeSeparationAnnees) {
        List<String> criteres = new ArrayList<>();
        if (!delaiObjectifOk) {
            criteres.add("Délai de séparation inférieur à 1 an (art. 238 Cciv) : "
                    + String.format("%.2f", dureeSeparationAnnees) + " année(s) écoulée(s)");
        }
        if (!preuvesSeparationDocumentaires) {
            criteres.add("Preuves documentaires de séparation insuffisantes "
                    + "(attestations hébergement séparé, bail, factures EDF, avis impôt)");
        }
        if (!absencePreuveReconciliation) {
            criteres.add("Tentatives de réconciliation signalées : affaiblit la "
                    + "caractérisation de la cessation définitive");
        }
        return criteres;
    }

    private static List<String> buildMessages(String verdict,
                                              boolean conditionsReunies,
                                              double dureeSeparationAnnees,
                                              boolean patrimoineCommunSignificatif,
                                              BigDecimal prestMin,
                                              BigDecimal prestMax) {
        List<String> messages = new ArrayList<>();
        messages.add("Divorce pour altération définitive du lien conjugal : procédure "
                + "objective, pas de recherche de tort, pas de dommages-intérêts art. 266 "
                + "Cciv (réservé au divorce pour faute).");
        messages.add("Preuves à constituer : attestations d'hébergement séparé, bail distinct, "
                + "factures à noms séparés, avis d'imposition distincts, attestations "
                + "de témoins, main courante, SMS/mails cohérents.");
        messages.add("Prérequis recommandé : F-FA-12 mesures provisoires (art. 254 Cciv) "
                + "pour régler les questions urgentes (résidence enfants, pension, usage "
                + "logement) pendant la procédure.");
        messages.add("Prestation compensatoire possible (art. 270 Cciv) : appréciation "
                + "globale par le juge selon la durée du mariage, l'âge, l'état de santé, "
                + "la qualification et la situation professionnelle, les conséquences "
                + "des choix professionnels faits pendant la vie commune, le patrimoine "
                + "estimé ou prévisible après liquidation.");
        if (conditionsReunies) {
            messages.add("Conditions objectives réunies : demande recevable, divorce "
                    + "prononcé sauf à ce que le défendeur démontre la reprise de la vie "
                    + "commune.");
        }
        if (dureeSeparationAnnees >= 3.0) {
            messages.add("Séparation ≥ 3 ans : le lien conjugal est considéré comme "
                    + "durablement rompu, présomption forte de bien-fondé.");
        }
        if (patrimoineCommunSignificatif) {
            messages.add("Patrimoine commun significatif : prévoir une liquidation "
                    + "concomitante (art. 267 Cciv) — outils F-FA-04/F-FA-05 pertinents.");
        }
        if (prestMin != null && prestMin.signum() > 0) {
            messages.add("Estimation prestation compensatoire : "
                    + prestMin.toPlainString() + " € à " + prestMax.toPlainString()
                    + " € (indicatif, barème jurisprudentiel différentiel × durée mariage).");
        }
        if ("FAIBLE".equals(verdict)) {
            messages.add("Probabilité faible : alternatives = divorce par consentement "
                    + "mutuel (art. 229-1 Cciv) ou attendre d'atteindre le délai d'un an.");
        }
        return messages;
    }

    private static String buildFormule(String verdict,
                                       int scoreGlobal,
                                       double dureeSeparationAnnees,
                                       boolean delaiObjectifOk,
                                       boolean preuvesSeparationDocumentaires,
                                       boolean absencePreuveReconciliation,
                                       int dureeMariageAnnees,
                                       BigDecimal prestMin,
                                       BigDecimal prestMax) {
        StringBuilder sb = new StringBuilder();
        sb.append("Divorce altération définitive (art. 237 Cciv) : probabilité ")
                .append(verdict)
                .append(" (score ").append(scoreGlobal).append("/100)");

        List<String> parts = new ArrayList<>();
        parts.add(String.format("séparation %.2f an(s) %s", dureeSeparationAnnees,
                delaiObjectifOk ? "≥ 1 an OK" : "< 1 an KO"));
        parts.add("preuves " + (preuvesSeparationDocumentaires ? "OK" : "KO"));
        parts.add("absence réconciliation " + (absencePreuveReconciliation ? "OK" : "KO"));
        parts.add("mariage " + dureeMariageAnnees + " an(s)"
                + (dureeMariageAnnees >= DUREE_MARIAGE_BONUS_ANNEES ? " (bonus)" : ""));
        sb.append(" — ").append(String.join(", ", parts)).append(".");

        if (prestMin != null && prestMin.signum() > 0) {
            sb.append(" Prestation compensatoire estimée : ")
                    .append(prestMin.toPlainString()).append(" € à ")
                    .append(prestMax.toPlainString()).append(" €.");
        }
        return sb.toString();
    }
}
