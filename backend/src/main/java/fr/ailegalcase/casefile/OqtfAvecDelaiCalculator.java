package fr.ailegalcase.casefile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-IM-08-01 : calculateur pour OQTF assortie d'un délai de départ volontaire (art. L.614-5 CESEDA).
 *
 * <p>Cas le plus fréquent en contentieux éloignement. Calcule :
 * <ul>
 *   <li>L'expiration du délai de départ volontaire (30 jours calendaires — L.614-5)</li>
 *   <li>L'expiration du délai de recours devant le TA (30 jours — L.614-5 al. 2, jours francs R.776-18)</li>
 *   <li>Le statut du délai (DISPONIBLE / URGENT / EXPIRE / RECOURS_FORME)</li>
 *   <li>Les dates prévisionnelles d'audience (J+90) et de décision (J+180)</li>
 *   <li>Les référés disponibles (L.521-1 suspension, L.521-2 liberté)</li>
 * </ul>
 *
 * <p>Outil <b>single-country FR</b> : l'annexe 13 belge est une procédure juridiquement distincte,
 * couverte par SF-IM-08-05.
 */
public final class OqtfAvecDelaiCalculator {

    /** Délai de départ volontaire (art. L.614-5 CESEDA). */
    public static final int DDV_JOURS_CALENDAIRES = 30;

    /** Délai de recours devant le TA (L.614-5 al. 2 — aligné sur 30 jours calendaires pour simplification). */
    public static final int DELAI_RECOURS_JOURS = 30;

    /** Délai d'audience TA prévisionnel (R.776-13-4 CJA — 3 mois). */
    public static final int AUDIENCE_JOURS = 90;

    /** Délai de décision TA prévisionnel (R.776-14 CJA — 6 mois). */
    public static final int DECISION_JOURS = 180;

    /** Seuil en jours pour passer en URGENT. */
    public static final int SEUIL_URGENT_JOURS = 7;

    /** Motifs OQTF valides (art. L.611-1 CESEDA). */
    public static final Set<String> MOTIFS_VALIDES = Set.of(
            "REFUS_TITRE",
            "EXPIRATION_TITRE",
            "SEJOUR_IRREGULIER",
            "RETRAIT_TITRE",
            "AUTRE"
    );

    public static final String STATUT_DISPONIBLE = "DISPONIBLE";
    public static final String STATUT_URGENT = "URGENT";
    public static final String STATUT_EXPIRE = "EXPIRE";
    public static final String STATUT_RECOURS_FORME = "RECOURS_FORME";

    public static final String REFERE_SUSPENSION = "REFERE_SUSPENSION_L521_1";
    public static final String REFERE_LIBERTE = "REFERE_LIBERTE_L521_2";

    private static final String BASE_JURIDIQUE = "Art. L.614-5, L.614-6, R.776-18 CJA";

    private OqtfAvecDelaiCalculator() {
    }

    public static OqtfAvecDelaiResult compute(LocalDate dateNotificationOqtf,
                                              String motifOqtf,
                                              boolean recoursForme,
                                              LocalDate dateRecours) {
        return compute(dateNotificationOqtf, motifOqtf, recoursForme, dateRecours,
                Clock.system(ZoneId.of("Europe/Paris")));
    }

    public static OqtfAvecDelaiResult compute(LocalDate dateNotificationOqtf,
                                              String motifOqtf,
                                              boolean recoursForme,
                                              LocalDate dateRecours,
                                              Clock clock) {
        validateInputs(dateNotificationOqtf, motifOqtf, recoursForme, dateRecours, clock);

        LocalDate today = LocalDate.now(clock);
        LocalDate dateExpirationDdv = dateNotificationOqtf.plusDays(DDV_JOURS_CALENDAIRES);
        LocalDate dateExpirationDelaiRecours = dateNotificationOqtf.plusDays(DELAI_RECOURS_JOURS);

        String statut;
        int joursRestants;
        LocalDate dateAudience = null;
        LocalDate dateDecision = null;

        if (recoursForme) {
            statut = STATUT_RECOURS_FORME;
            joursRestants = 0;
            dateAudience = dateRecours.plusDays(AUDIENCE_JOURS);
            dateDecision = dateRecours.plusDays(DECISION_JOURS);
        } else {
            long joursRestantsLong = ChronoUnit.DAYS.between(today, dateExpirationDelaiRecours);
            joursRestants = (int) joursRestantsLong;
            if (joursRestantsLong < 0) {
                statut = STATUT_EXPIRE;
            } else if (joursRestantsLong <= SEUIL_URGENT_JOURS) {
                statut = STATUT_URGENT;
            } else {
                statut = STATUT_DISPONIBLE;
            }
        }

        List<String> referedDisponibles = List.of(REFERE_SUSPENSION, REFERE_LIBERTE);
        List<String> messages = buildMessages(motifOqtf, statut);
        String formule = buildFormule(motifOqtf, statut, dateNotificationOqtf,
                dateExpirationDelaiRecours, joursRestants, recoursForme, dateRecours,
                dateAudience, dateDecision);

        return new OqtfAvecDelaiResult(
                dateNotificationOqtf,
                motifOqtf,
                recoursForme,
                dateRecours,
                dateExpirationDdv,
                dateExpirationDelaiRecours,
                joursRestants,
                statut,
                dateAudience,
                dateDecision,
                referedDisponibles,
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    private static void validateInputs(LocalDate dateNotificationOqtf,
                                       String motifOqtf,
                                       boolean recoursForme,
                                       LocalDate dateRecours,
                                       Clock clock) {
        if (dateNotificationOqtf == null) {
            throw new IllegalArgumentException("dateNotificationOqtf est requise");
        }
        LocalDate today = LocalDate.now(clock);
        if (dateNotificationOqtf.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateNotificationOqtf ne peut pas être dans le futur");
        }
        if (motifOqtf == null || motifOqtf.isBlank()) {
            throw new IllegalArgumentException("motifOqtf est requis");
        }
        if (!MOTIFS_VALIDES.contains(motifOqtf)) {
            throw new IllegalArgumentException(
                    "motifOqtf inconnu — valeurs attendues : " + MOTIFS_VALIDES);
        }
        if (recoursForme) {
            if (dateRecours == null) {
                throw new IllegalArgumentException(
                        "dateRecours est requise si recoursForme=true");
            }
            if (dateRecours.isBefore(dateNotificationOqtf)) {
                throw new IllegalArgumentException(
                        "dateRecours ne peut pas être antérieure à dateNotificationOqtf");
            }
        } else if (dateRecours != null && dateRecours.isBefore(dateNotificationOqtf)) {
            throw new IllegalArgumentException(
                    "dateRecours ne peut pas être antérieure à dateNotificationOqtf");
        }
    }

    private static List<String> buildMessages(String motifOqtf, String statut) {
        List<String> messages = new ArrayList<>();
        messages.add("Recours devant le TA suspensif (art. L.614-5 al. 2 CESEDA) — "
                + "l'éloignement ne peut être exécuté tant que le TA n'a pas statué.");
        messages.add("Référé suspension (art. L.521-1 CJA) disponible en complément si urgence.");
        messages.add("Référé liberté (art. L.521-2 CJA) disponible si atteinte grave à une liberté "
                + "fondamentale — décision en 48h.");
        messages.add("Délai de 30 jours = jours francs (R.776-18 CJA) : le jour de notification "
                + "et le jour d'échéance ne comptent pas. Compter exactement en prenant une marge "
                + "de sécurité.");
        messages.add("Droit à interprète (R.614-5) et droit à l'assistance d'un conseil — "
                + "mentionner sur le recours.");

        switch (motifOqtf) {
            case "REFUS_TITRE" -> messages.add(
                    "L'OQTF est motivée par le refus de titre — contester les deux décisions "
                            + "simultanément (L.614-6).");
            case "SEJOUR_IRREGULIER" -> messages.add(
                    "Pas de décision administrative préalable à contester. Focus sur l'absence "
                            + "de menace à l'ordre public et les liens personnels/familiaux.");
            case "EXPIRATION_TITRE" -> messages.add(
                    "L'OQTF repose sur l'expiration du titre (L.611-1 3°) — vérifier que la "
                            + "demande de renouvellement a bien été déposée dans les délais et "
                            + "soulever tout vice dans l'instruction.");
            case "RETRAIT_TITRE" -> messages.add(
                    "L'OQTF découle du retrait du titre (L.611-1 4°) — contester le retrait "
                            + "lui-même (motif et procédure) en plus de l'OQTF.");
            case "AUTRE" -> messages.add(
                    "Vérifier attentivement le motif invoqué par l'administration et l'article "
                            + "L.611-1 CESEDA cité dans la décision.");
            default -> { /* unreachable */ }
        }

        if (STATUT_URGENT.equals(statut)) {
            messages.add("DÉLAI URGENT : il reste moins de 7 jours avant expiration du délai de "
                    + "recours — déposer le recours sans délai.");
        } else if (STATUT_EXPIRE.equals(statut)) {
            messages.add("DÉLAI EXPIRÉ : recours de plein contentieux forclos. Envisager un "
                    + "référé liberté (L.521-2) en cas d'éloignement imminent et d'atteinte "
                    + "grave aux droits fondamentaux.");
        }
        return messages;
    }

    private static String buildFormule(String motifOqtf, String statut,
                                       LocalDate dateNotification,
                                       LocalDate dateExpirationRecours,
                                       int joursRestants,
                                       boolean recoursForme,
                                       LocalDate dateRecours,
                                       LocalDate dateAudience,
                                       LocalDate dateDecision) {
        if (recoursForme) {
            return String.format(
                    "OQTF L.614-5 motif %s notifiée le %s — recours formé le %s. "
                            + "Audience prévisionnelle %s (J+90), décision prévisionnelle %s (J+180).",
                    motifOqtf, dateNotification, dateRecours, dateAudience, dateDecision);
        }
        return String.format(
                "OQTF L.614-5 motif %s notifiée le %s — délai de recours expire le %s "
                        + "(%d jours restants, statut %s). Référés L.521-1 et L.521-2 disponibles.",
                motifOqtf, dateNotification, dateExpirationRecours, joursRestants, statut);
    }
}
