package fr.ailegalcase.casefile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-FA-11-01 : calculateur pour le <b>divorce pour désunion irrémédiable</b>
 * BE — art. 229 §3 Code civil belge (Loi du 27/04/2007). Équivalent belge des
 * features FR F-FA-08 (altération définitive) et F-FA-10 (accepté). Procédure
 * unique BE fondée sur la séparation de fait, sans recherche de tort.
 *
 * <p>Présomption légale de désunion irrémédiable :
 * <ul>
 *   <li>Demande conjointe (consentue) : séparation ≥ <b>6 mois</b></li>
 *   <li>Demande unilatérale : séparation ≥ <b>1 an (12 mois)</b></li>
 * </ul>
 *
 * <p>Le délai est apprécié à la date d'assignation (introduction de la
 * demande) si fournie, sinon à la date du jour (simulation pré-procédure).
 *
 * <p>Scoring pondéré 0-100 :
 * <ul>
 *   <li>+40 si délai objectif atteint (≥ seuil applicable)</li>
 *   <li>+30 si preuves de séparation</li>
 *   <li>+15 si preuves documentaires (registre national, bail, factures)</li>
 *   <li>+15 si absence de tentatives de réconciliation</li>
 * </ul>
 *
 * <p>Verdicts : ELEVEE (≥ 75), MOYENNE (≥ 50), FAIBLE (&lt; 50).
 *
 * <p>Conditions réunies =
 * <code>delaiObjectifOk &amp;&amp; preuvesSeparation &amp;&amp;
 * (preuvesDocumentaires || !tentativesReconciliation)</code>.
 *
 * <p>Outil <b>single-country BE</b>. FR = F-FA-08 + F-FA-10 (procédures
 * distinctes en droit français).
 */
public final class DivorceDesunionIrremediableBeCalculator {

    /** Seuil de séparation pour demande conjointe (consentue). */
    public static final int SEUIL_CONSENTUE_MOIS = 6;

    /** Seuil de séparation pour demande unilatérale. */
    public static final int SEUIL_UNILATERALE_MOIS = 12;

    /** Seuil score pour verdict ELEVEE. */
    public static final int SEUIL_ELEVEE = 75;

    /** Seuil score pour verdict MOYENNE. */
    public static final int SEUIL_MOYENNE = 50;

    private static final String BASE_JURIDIQUE =
            "Art. 229 §3 Code civil belge + Loi 27/04/2007";

    private DivorceDesunionIrremediableBeCalculator() {
    }

    public static DivorceDesunionIrremediableBeResult compute(LocalDate dateSeparation,
                                                              boolean separationConsentue,
                                                              boolean preuvesSeparation,
                                                              boolean preuvesDocumentaires,
                                                              boolean tentativesReconciliation,
                                                              LocalDate dateAssignation) {
        return compute(dateSeparation, separationConsentue, preuvesSeparation,
                preuvesDocumentaires, tentativesReconciliation, dateAssignation,
                Clock.system(ZoneId.of("Europe/Brussels")));
    }

    public static DivorceDesunionIrremediableBeResult compute(LocalDate dateSeparation,
                                                              boolean separationConsentue,
                                                              boolean preuvesSeparation,
                                                              boolean preuvesDocumentaires,
                                                              boolean tentativesReconciliation,
                                                              LocalDate dateAssignation,
                                                              Clock clock) {
        validateInputs(dateSeparation, dateAssignation, clock);

        LocalDate today = LocalDate.now(clock);
        LocalDate dateAppreciation = dateAssignation != null ? dateAssignation : today;
        int dureeSeparationMois = (int) ChronoUnit.MONTHS.between(dateSeparation, dateAppreciation);
        if (dureeSeparationMois < 0) {
            dureeSeparationMois = 0;
        }

        int seuilSeparationMois = separationConsentue ? SEUIL_CONSENTUE_MOIS : SEUIL_UNILATERALE_MOIS;
        boolean delaiObjectifOk = dureeSeparationMois >= seuilSeparationMois;
        boolean absenceReconciliation = !tentativesReconciliation;
        boolean conditionsReunies = delaiObjectifOk
                && preuvesSeparation
                && (preuvesDocumentaires || absenceReconciliation);

        int scoreGlobal = computeScore(delaiObjectifOk, preuvesSeparation,
                preuvesDocumentaires, absenceReconciliation);
        String verdict = verdict(scoreGlobal);

        List<String> messages = buildMessages(verdict, conditionsReunies,
                separationConsentue, dureeSeparationMois, seuilSeparationMois,
                preuvesSeparation, preuvesDocumentaires, tentativesReconciliation);

        String formule = buildFormule(verdict, scoreGlobal, dureeSeparationMois,
                seuilSeparationMois, separationConsentue, conditionsReunies);

        return new DivorceDesunionIrremediableBeResult(
                dateSeparation,
                separationConsentue,
                preuvesSeparation,
                preuvesDocumentaires,
                tentativesReconciliation,
                dateAssignation,
                dureeSeparationMois,
                seuilSeparationMois,
                delaiObjectifOk,
                conditionsReunies,
                scoreGlobal,
                verdict,
                BASE_JURIDIQUE,
                formule,
                messages);
    }

    private static int computeScore(boolean delaiObjectifOk,
                                    boolean preuvesSeparation,
                                    boolean preuvesDocumentaires,
                                    boolean absenceReconciliation) {
        int score = 0;
        if (delaiObjectifOk) score += 40;
        if (preuvesSeparation) score += 30;
        if (preuvesDocumentaires) score += 15;
        if (absenceReconciliation) score += 15;
        return Math.min(score, 100);
    }

    private static String verdict(int score) {
        if (score >= SEUIL_ELEVEE) return "ELEVEE";
        if (score >= SEUIL_MOYENNE) return "MOYENNE";
        return "FAIBLE";
    }

    private static void validateInputs(LocalDate dateSeparation,
                                       LocalDate dateAssignation,
                                       Clock clock) {
        if (dateSeparation == null) {
            throw new IllegalArgumentException("dateSeparation est requise");
        }
        LocalDate today = LocalDate.now(clock);
        if (dateSeparation.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateSeparation ne peut pas être dans le futur");
        }
        if (dateAssignation != null && dateAssignation.isBefore(dateSeparation)) {
            throw new IllegalArgumentException(
                    "dateAssignation ne peut pas être antérieure à dateSeparation");
        }
    }

    private static List<String> buildMessages(String verdict,
                                              boolean conditionsReunies,
                                              boolean separationConsentue,
                                              int dureeSeparationMois,
                                              int seuilSeparationMois,
                                              boolean preuvesSeparation,
                                              boolean preuvesDocumentaires,
                                              boolean tentativesReconciliation) {
        List<String> messages = new ArrayList<>();
        messages.add("Divorce pour désunion irrémédiable (art. 229 §3 Code civil belge, "
                + "Loi du 27/04/2007) : procédure objective, pas de recherche de tort, "
                + "pas de dommages-intérêts (différent du droit français art. 266 Cciv).");
        messages.add("Présomption légale automatique de désunion irrémédiable : "
                + "séparation de fait ≥ 6 mois (demande conjointe) ou ≥ 1 an "
                + "(demande unilatérale).");
        messages.add("Preuves recommandées : extraits de Registre national avec adresses "
                + "distinctes, baux distincts, factures eau/gaz/électricité à des "
                + "adresses différentes, attestations de témoins, courrier d'huissier "
                + "constatant la séparation de fait.");
        messages.add("Demande introduite par requête contradictoire ou citation devant "
                + "le tribunal de la famille (art. 1254 Code judiciaire belge). "
                + "Représentation par avocat non obligatoire mais recommandée.");
        messages.add("Conséquences : pension alimentaire entre ex-époux possible "
                + "(art. 301 CC belge, plafond 1/3 des revenus du débiteur, durée "
                + "limitée à la durée du mariage), partage des biens selon le régime "
                + "matrimonial, autorité parentale conjointe sauf décision contraire.");

        if (conditionsReunies) {
            messages.add("Conditions objectives réunies : présomption légale de désunion "
                    + "irrémédiable établie, demande recevable.");
        }
        if (!preuvesSeparation) {
            messages.add("Preuves de séparation insuffisantes : la désunion peut "
                    + "néanmoins être prouvée par toutes voies de droit (art. 229 §2), "
                    + "mais l'absence de présomption légale fragilise la demande.");
        }
        if (separationConsentue && dureeSeparationMois < seuilSeparationMois) {
            messages.add("Demande conjointe envisagée mais séparation < 6 mois : "
                    + "alternatives = attendre d'atteindre le seuil, ou opter pour le "
                    + "divorce par consentement mutuel (art. 230 CC, sans condition "
                    + "de durée mais nécessite des conventions complètes).");
        }
        if (!separationConsentue && dureeSeparationMois >= SEUIL_CONSENTUE_MOIS
                && dureeSeparationMois < SEUIL_UNILATERALE_MOIS) {
            messages.add("Séparation ≥ 6 mois mais < 1 an et demande unilatérale : "
                    + "envisager une demande conjointe avec l'autre époux pour "
                    + "déclencher la présomption immédiatement.");
        }
        if (tentativesReconciliation) {
            messages.add("Tentatives de réconciliation signalées : le juge peut "
                    + "considérer que la désunion n'est pas définitivement irrémédiable, "
                    + "spécialement si elles sont récentes.");
        }
        if ("FAIBLE".equals(verdict)) {
            messages.add("Probabilité faible : alternatives = divorce par consentement "
                    + "mutuel (art. 230 CC, conventions complètes obligatoires) "
                    + "ou attendre d'atteindre le seuil de séparation.");
        }
        return messages;
    }

    private static String buildFormule(String verdict,
                                       int scoreGlobal,
                                       int dureeSeparationMois,
                                       int seuilSeparationMois,
                                       boolean separationConsentue,
                                       boolean conditionsReunies) {
        String typeDemande = separationConsentue ? "consentue" : "unilatérale";
        String comparaison = dureeSeparationMois >= seuilSeparationMois ? "≥" : "<";
        String etablie = conditionsReunies
                ? "→ désunion irrémédiable établie"
                : "→ désunion à prouver par autres voies (art. 229 §2)";
        return String.format(
                "Séparation %d mois %s seuil %d mois (%s) %s — verdict %s "
                        + "(score %d/100, art. 229 §3 CC).",
                dureeSeparationMois, comparaison, seuilSeparationMois,
                typeDemande, etablie, verdict, scoreGlobal);
    }
}
