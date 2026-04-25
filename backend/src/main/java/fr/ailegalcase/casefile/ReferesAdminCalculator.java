package fr.ailegalcase.casefile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-IM-08-07 : calculateur unique pour les référés administratifs combinés FR :
 * <ul>
 *   <li>Référé suspension (art. L.521-1 CJA) — urgence + doutes sérieux légalité, audience sous 30 jours.</li>
 *   <li>Référé liberté (art. L.521-2 CJA) — atteinte grave à liberté fondamentale + urgence, statué sous 48h.</li>
 * </ul>
 *
 * <p>Outil <b>single-country FR</b> (procédure CJA). Combine deux scoring distincts puis recommande
 * la voie la plus pertinente (verdict consolidé).
 */
public final class ReferesAdminCalculator {

    public static final int DELAI_JUGE_TA_JOURS_L521_1 = 30;
    public static final int DELAI_JUGE_TA_HEURES_L521_2 = 48;

    public static final int BONUS_PAR_PREUVE = 5;
    public static final int BONUS_MAX = 30;
    public static final int SEUIL_PRIORITE_LIBERTE = 15;
    public static final int SEUIL_AUCUN_PROBABLE = 40;

    public static final Set<String> TYPES_REFERE = Set.of("SUSPENSION", "LIBERTE", "LES_DEUX");

    public static final Set<String> DECISIONS_CONTESTEES = Set.of(
            "OQTF",
            "RETRAIT_TITRE",
            "REFUS_TITRE",
            "IRTF",
            "AUTRE_MESURE_ADMIN");

    public static final Set<String> PREUVES_URGENCE_VALIDES = Set.of(
            "MENACE_VIE_PRIVEE",
            "TRANSFERT_IMMINENT",
            "IMPACT_SCOLARITE_ENFANTS",
            "RUPTURE_SOINS_MEDICAUX",
            "RISQUE_VIOLENCES_PAYS_ORIGINE",
            "AUTRE");

    public static final String VERDICT_SUSPENSION_PRIO = "REFERE_SUSPENSION_PRIORITAIRE";
    public static final String VERDICT_LIBERTE_PRIO = "REFERE_LIBERTE_PRIORITAIRE";
    public static final String VERDICT_LES_DEUX_CUMULES = "LES_DEUX_CUMULES";
    public static final String VERDICT_AUCUN_PROBABLE = "AUCUN_PROBABLE";

    private static final String BASE_JURIDIQUE =
            "Art. L.521-1 + L.521-2 CJA + jurisprudence CE";

    private ReferesAdminCalculator() {
    }

    public static ReferesAdminResult compute(String typeRefere,
                                             String decisionContestee,
                                             LocalDate dateNotificationDecision,
                                             boolean urgenceCaracterisee,
                                             boolean atteinteLiberteFondamentale,
                                             boolean doutesSerieuxLegalite,
                                             List<String> preuvesUrgence,
                                             boolean demandeurDejaPrived) {
        return compute(typeRefere, decisionContestee, dateNotificationDecision,
                urgenceCaracterisee, atteinteLiberteFondamentale, doutesSerieuxLegalite,
                preuvesUrgence, demandeurDejaPrived,
                Clock.system(ZoneId.of("Europe/Paris")));
    }

    public static ReferesAdminResult compute(String typeRefere,
                                             String decisionContestee,
                                             LocalDate dateNotificationDecision,
                                             boolean urgenceCaracterisee,
                                             boolean atteinteLiberteFondamentale,
                                             boolean doutesSerieuxLegalite,
                                             List<String> preuvesUrgence,
                                             boolean demandeurDejaPrived,
                                             Clock clock) {
        validateInputs(typeRefere, decisionContestee, dateNotificationDecision,
                preuvesUrgence, clock);

        List<String> preuvesNormalized = preuvesUrgence != null
                ? List.copyOf(preuvesUrgence)
                : List.of();

        int bonus = Math.min(BONUS_MAX, preuvesNormalized.size() * BONUS_PAR_PREUVE);

        int scoreSuspension = computeBaseScore(urgenceCaracterisee, doutesSerieuxLegalite);
        scoreSuspension = Math.min(100, scoreSuspension + bonus);

        int scoreLiberte = computeBaseScore(atteinteLiberteFondamentale, urgenceCaracterisee);
        scoreLiberte = Math.min(100, scoreLiberte + bonus);

        boolean conditionsL521_1Ok = urgenceCaracterisee && doutesSerieuxLegalite;
        boolean conditionsL521_2Ok = urgenceCaracterisee && atteinteLiberteFondamentale;

        String verdict = computeVerdict(typeRefere, scoreSuspension, scoreLiberte,
                conditionsL521_1Ok, conditionsL521_2Ok);

        List<String> messages = buildMessages(typeRefere, decisionContestee, demandeurDejaPrived,
                conditionsL521_1Ok, conditionsL521_2Ok, scoreSuspension, scoreLiberte, verdict);

        String formule = buildFormule(scoreSuspension, scoreLiberte, verdict,
                conditionsL521_1Ok, conditionsL521_2Ok);

        return new ReferesAdminResult(
                typeRefere,
                decisionContestee,
                dateNotificationDecision,
                urgenceCaracterisee,
                atteinteLiberteFondamentale,
                doutesSerieuxLegalite,
                preuvesNormalized,
                demandeurDejaPrived,
                scoreSuspension,
                scoreLiberte,
                verdict,
                DELAI_JUGE_TA_JOURS_L521_1,
                DELAI_JUGE_TA_HEURES_L521_2,
                conditionsL521_1Ok,
                conditionsL521_2Ok,
                BASE_JURIDIQUE,
                formule,
                messages);
    }

    private static int computeBaseScore(boolean cond1, boolean cond2) {
        if (cond1 && cond2) return 100;
        if (cond1 || cond2) return 50;
        return 0;
    }

    private static String computeVerdict(String typeRefere,
                                         int scoreSuspension,
                                         int scoreLiberte,
                                         boolean conditionsL521_1Ok,
                                         boolean conditionsL521_2Ok) {
        // Cumul demandé explicitement et les deux conditions ok
        if ("LES_DEUX".equals(typeRefere) && conditionsL521_1Ok && conditionsL521_2Ok) {
            return VERDICT_LES_DEUX_CUMULES;
        }
        int max = Math.max(scoreSuspension, scoreLiberte);
        if (max < SEUIL_AUCUN_PROBABLE) {
            return VERDICT_AUCUN_PROBABLE;
        }
        // Liberté prioritaire si avantage de >= 15 points (48h plus rapide que 30j)
        if (scoreLiberte >= scoreSuspension + SEUIL_PRIORITE_LIBERTE) {
            return VERDICT_LIBERTE_PRIO;
        }
        return VERDICT_SUSPENSION_PRIO;
    }

    private static List<String> buildMessages(String typeRefere,
                                              String decisionContestee,
                                              boolean demandeurDejaPrived,
                                              boolean conditionsL521_1Ok,
                                              boolean conditionsL521_2Ok,
                                              int scoreSuspension,
                                              int scoreLiberte,
                                              String verdict) {
        List<String> messages = new ArrayList<>();
        messages.add("L.521-2 statué dans les 48h (art. L.522-1 CJA) — voie la plus rapide en cas d'urgence absolue.");
        messages.add("L.521-1 audience sous 30 jours (R.522-1 CJA) — voie standard quand doutes sérieux sur la légalité.");
        messages.add("Conditions L.521-1 cumulatives : urgence caractérisée + doutes sérieux sur la légalité de la décision.");
        messages.add("Conditions L.521-2 cumulatives : atteinte grave et manifestement illégale à une liberté fondamentale + urgence.");

        if (!conditionsL521_1Ok && ("SUSPENSION".equals(typeRefere) || "LES_DEUX".equals(typeRefere))) {
            messages.add("Conditions cumulatives L.521-1 NON réunies — référé suspension peu probable. Renforcer le dossier sur l'urgence ET les doutes sérieux.");
        }
        if (!conditionsL521_2Ok && ("LIBERTE".equals(typeRefere) || "LES_DEUX".equals(typeRefere))) {
            messages.add("Conditions cumulatives L.521-2 NON réunies — référé liberté peu probable. Caractériser précisément la liberté fondamentale en jeu.");
        }

        switch (decisionContestee) {
            case "OQTF" -> messages.add(
                    "Décision contestée : OQTF. Recours en plein contentieux (L.614-5 CESEDA) déjà suspensif — référé en complément si éloignement imminent.");
            case "RETRAIT_TITRE" -> messages.add(
                    "Décision contestée : retrait de titre. Argumenter aussi sur le motif et la procédure du retrait (L.611-1 4°).");
            case "REFUS_TITRE" -> messages.add(
                    "Décision contestée : refus de titre. Référé en complément du recours pour excès de pouvoir (REP).");
            case "IRTF" -> messages.add(
                    "Décision contestée : IRTF. Atteinte grave aux liens familiaux et droit à la vie privée — terrain favorable au L.521-2.");
            case "AUTRE_MESURE_ADMIN" -> messages.add(
                    "Décision contestée : autre mesure administrative. Vérifier la qualification précise et l'exécution forcée éventuelle.");
            default -> { /* validated */ }
        }

        if (demandeurDejaPrived) {
            messages.add("Demandeur déjà privé de liberté (rétention) — urgence présumée caractérisée. Privilégier L.521-2 (48h).");
        }

        switch (verdict) {
            case VERDICT_LIBERTE_PRIO -> messages.add(
                    "Verdict : référé liberté prioritaire — déposer L.521-2 immédiatement (statué sous 48h).");
            case VERDICT_SUSPENSION_PRIO -> messages.add(
                    "Verdict : référé suspension prioritaire — déposer L.521-1 (audience sous 30 jours).");
            case VERDICT_LES_DEUX_CUMULES -> messages.add(
                    "Verdict : cumuler les deux référés (L.521-1 + L.521-2) — chaque voie indépendante.");
            case VERDICT_AUCUN_PROBABLE -> messages.add(
                    "Verdict : aucun référé probable en l'état — renforcer le dossier (preuves d'urgence, caractérisation de la liberté en jeu, vices de légalité).");
            default -> { /* unreachable */ }
        }

        messages.add("Compétence du juge des référés au TA territorialement compétent (R.312-1 CJA).");
        return messages;
    }

    private static String buildFormule(int scoreSuspension,
                                       int scoreLiberte,
                                       String verdict,
                                       boolean conditionsL521_1Ok,
                                       boolean conditionsL521_2Ok) {
        return String.format(
                "L.521-1 score %d (cumulatives %s) + L.521-2 score %d (cumulatives %s) → verdict %s",
                scoreSuspension,
                conditionsL521_1Ok ? "OK" : "KO",
                scoreLiberte,
                conditionsL521_2Ok ? "OK" : "KO",
                verdict);
    }

    private static void validateInputs(String typeRefere,
                                       String decisionContestee,
                                       LocalDate dateNotificationDecision,
                                       List<String> preuvesUrgence,
                                       Clock clock) {
        if (typeRefere == null || typeRefere.isBlank()) {
            throw new IllegalArgumentException("typeRefere est requis");
        }
        if (!TYPES_REFERE.contains(typeRefere)) {
            throw new IllegalArgumentException(
                    "typeRefere inconnu — valeurs attendues : " + TYPES_REFERE);
        }
        if (decisionContestee == null || decisionContestee.isBlank()) {
            throw new IllegalArgumentException("decisionContestee est requise");
        }
        if (!DECISIONS_CONTESTEES.contains(decisionContestee)) {
            throw new IllegalArgumentException(
                    "decisionContestee inconnue — valeurs attendues : " + DECISIONS_CONTESTEES);
        }
        if (dateNotificationDecision == null) {
            throw new IllegalArgumentException("dateNotificationDecision est requise");
        }
        LocalDate today = LocalDate.now(clock);
        if (dateNotificationDecision.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateNotificationDecision ne peut pas être dans le futur");
        }
        if (preuvesUrgence != null) {
            for (String p : preuvesUrgence) {
                if (p == null || !PREUVES_URGENCE_VALIDES.contains(p)) {
                    throw new IllegalArgumentException(
                            "preuveUrgence inconnue : " + p
                                    + " — valeurs attendues : " + PREUVES_URGENCE_VALIDES);
                }
            }
        }
    }
}
