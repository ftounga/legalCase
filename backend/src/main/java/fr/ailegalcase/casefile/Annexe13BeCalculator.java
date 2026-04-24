package fr.ailegalcase.casefile;

import fr.ailegalcase.shared.BelgianBusinessDaysCalculator;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-IM-08-05 : calculateur pour l'ordre de quitter le territoire belge (OQT) notifié par
 * annexe 13 de l'AR du 08/10/1981 d'exécution de la Loi du 15/12/1980 sur les étrangers.
 *
 * <p>Calcule :
 * <ul>
 *   <li>L'expiration du délai de départ imposé (notification + delaiDepartImposeJours)</li>
 *   <li>L'expiration du délai de recours en annulation devant le CCE (30 jours calendaires — art. 39/2 §2)</li>
 *   <li>L'expiration du délai de recours en extrême urgence (5 jours ouvrables — art. 39/82 §4)</li>
 *   <li>Les jours restants et le statut du recours en annulation (DISPONIBLE / URGENT / EXPIRE / RECOURS_FORME)</li>
 *   <li>Les dates prévisionnelles d'audience (J+30) et de décision (J+90) si recours formé</li>
 *   <li>Les référés disponibles (DEMANDE_SUSPENSION_ART_39_2 toujours + RECOURS_EXTREME_URGENCE_39_82 si transfert imminent)</li>
 * </ul>
 *
 * <p>Outil <b>single-country BE</b> : l'OQTF française est une procédure juridiquement distincte,
 * couverte par SF-IM-08-01/03.
 */
public final class Annexe13BeCalculator {

    /** Délai du recours en annulation devant le CCE (art. 39/2 §2 Loi 15/12/1980). */
    public static final int DELAI_RECOURS_ANNULATION_JOURS = 30;

    /** Délai du recours en extrême urgence (art. 39/82 §4 Loi 15/12/1980) — jours ouvrables. */
    public static final int DELAI_RECOURS_EXTREME_URGENCE_JOURS_OUVRABLES = 5;

    /** Délai d'audience prévisionnel CCE après recours (estimation). */
    public static final int AUDIENCE_JOURS = 30;

    /** Délai de décision prévisionnel CCE après recours (moyenne). */
    public static final int DECISION_JOURS = 90;

    /** Seuil en jours pour passer en URGENT. */
    public static final int SEUIL_URGENT_JOURS = 3;

    /** Motifs OQT valides (art. 7 Loi 15/12/1980). */
    public static final Set<String> MOTIFS_VALIDES = Set.of(
            "SEJOUR_IRREGULIER_ART_7",
            "REFUS_SEJOUR_APRES_DEMANDE",
            "FIN_SEJOUR_REGULIER",
            "AUTRE"
    );

    /** Types de recours devant le CCE. */
    public static final Set<String> TYPES_RECOURS_VALIDES = Set.of(
            "ANNULATION_30J",
            "EXTREME_URGENCE_5JO"
    );

    public static final String STATUT_DISPONIBLE = "DISPONIBLE";
    public static final String STATUT_URGENT = "URGENT";
    public static final String STATUT_EXPIRE = "EXPIRE";
    public static final String STATUT_RECOURS_FORME = "RECOURS_FORME";

    public static final String REFERE_SUSPENSION = "DEMANDE_SUSPENSION_ART_39_2";
    public static final String REFERE_EXTREME_URGENCE = "RECOURS_EXTREME_URGENCE_39_82";

    private static final String BASE_JURIDIQUE =
            "Loi 15/12/1980 art. 7 + art. 39/2 §2 + art. 39/82 §4 + annexe 13 AR 08/10/1981";

    private Annexe13BeCalculator() {
    }

    public static Annexe13BeResult compute(LocalDate dateNotificationAnnexe13,
                                           int delaiDepartImposeJours,
                                           String motifOqt,
                                           boolean transfertImminent,
                                           boolean recoursForme,
                                           LocalDate dateRecours,
                                           String typeRecours) {
        return compute(dateNotificationAnnexe13, delaiDepartImposeJours, motifOqt,
                transfertImminent, recoursForme, dateRecours, typeRecours,
                Clock.system(ZoneId.of("Europe/Brussels")));
    }

    public static Annexe13BeResult compute(LocalDate dateNotificationAnnexe13,
                                           int delaiDepartImposeJours,
                                           String motifOqt,
                                           boolean transfertImminent,
                                           boolean recoursForme,
                                           LocalDate dateRecours,
                                           String typeRecours,
                                           Clock clock) {
        validateInputs(dateNotificationAnnexe13, delaiDepartImposeJours, motifOqt,
                recoursForme, dateRecours, typeRecours, clock);

        LocalDate today = LocalDate.now(clock);
        LocalDate dateExpirationDelaiDepart = dateNotificationAnnexe13.plusDays(delaiDepartImposeJours);
        LocalDate dateExpirationRecoursAnnulation =
                dateNotificationAnnexe13.plusDays(DELAI_RECOURS_ANNULATION_JOURS);
        LocalDate dateExpirationRecoursExtremeUrgence =
                addBusinessDays(dateNotificationAnnexe13, DELAI_RECOURS_EXTREME_URGENCE_JOURS_OUVRABLES);

        String statutRecoursAnnulation;
        long joursRestantsAvantExpirationAnnulation;
        LocalDate dateAudiencePrevisionnelle = null;
        LocalDate dateDecisionPrevisionnelle = null;

        if (recoursForme) {
            statutRecoursAnnulation = STATUT_RECOURS_FORME;
            joursRestantsAvantExpirationAnnulation = 0L;
            dateAudiencePrevisionnelle = dateRecours.plusDays(AUDIENCE_JOURS);
            dateDecisionPrevisionnelle = dateRecours.plusDays(DECISION_JOURS);
        } else {
            joursRestantsAvantExpirationAnnulation =
                    ChronoUnit.DAYS.between(today, dateExpirationRecoursAnnulation);
            if (joursRestantsAvantExpirationAnnulation < 0) {
                statutRecoursAnnulation = STATUT_EXPIRE;
            } else if (joursRestantsAvantExpirationAnnulation <= SEUIL_URGENT_JOURS) {
                statutRecoursAnnulation = STATUT_URGENT;
            } else {
                statutRecoursAnnulation = STATUT_DISPONIBLE;
            }
        }

        List<String> referedDisponibles = new ArrayList<>();
        referedDisponibles.add(REFERE_SUSPENSION);
        if (transfertImminent) {
            referedDisponibles.add(REFERE_EXTREME_URGENCE);
        }

        List<String> messages = buildMessages(motifOqt, statutRecoursAnnulation, transfertImminent);
        String formule = buildFormule(motifOqt, statutRecoursAnnulation, dateNotificationAnnexe13,
                delaiDepartImposeJours, dateExpirationDelaiDepart, dateExpirationRecoursAnnulation,
                joursRestantsAvantExpirationAnnulation, transfertImminent, recoursForme,
                dateRecours, typeRecours, dateAudiencePrevisionnelle, dateDecisionPrevisionnelle);

        return new Annexe13BeResult(
                dateNotificationAnnexe13,
                delaiDepartImposeJours,
                motifOqt,
                transfertImminent,
                recoursForme,
                dateRecours,
                typeRecours,
                dateExpirationDelaiDepart,
                dateExpirationRecoursAnnulation,
                dateExpirationRecoursExtremeUrgence,
                joursRestantsAvantExpirationAnnulation,
                statutRecoursAnnulation,
                dateAudiencePrevisionnelle,
                dateDecisionPrevisionnelle,
                referedDisponibles,
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    /**
     * Ajoute {@code n} jours ouvrables belges à une date (au sens de
     * {@link BelgianBusinessDaysCalculator#isBusinessDay(LocalDate)}). Le jour {@code start} est
     * exclu. Le résultat est le n-ième jour ouvrable strict après {@code start}.
     */
    static LocalDate addBusinessDays(LocalDate start, int n) {
        if (n <= 0) {
            return start;
        }
        LocalDate cursor = start;
        int remaining = n;
        while (remaining > 0) {
            cursor = cursor.plusDays(1);
            if (BelgianBusinessDaysCalculator.isBusinessDay(cursor)) {
                remaining--;
            }
        }
        return cursor;
    }

    private static void validateInputs(LocalDate dateNotificationAnnexe13,
                                       int delaiDepartImposeJours,
                                       String motifOqt,
                                       boolean recoursForme,
                                       LocalDate dateRecours,
                                       String typeRecours,
                                       Clock clock) {
        if (dateNotificationAnnexe13 == null) {
            throw new IllegalArgumentException("dateNotificationAnnexe13 est requise");
        }
        LocalDate today = LocalDate.now(clock);
        if (dateNotificationAnnexe13.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateNotificationAnnexe13 ne peut pas être dans le futur");
        }
        if (delaiDepartImposeJours < 0) {
            throw new IllegalArgumentException("delaiDepartImposeJours doit être ≥ 0");
        }
        if (motifOqt == null || motifOqt.isBlank()) {
            throw new IllegalArgumentException("motifOqt est requis");
        }
        if (!MOTIFS_VALIDES.contains(motifOqt)) {
            throw new IllegalArgumentException(
                    "motifOqt inconnu — valeurs attendues : " + MOTIFS_VALIDES);
        }
        if (recoursForme) {
            if (dateRecours == null) {
                throw new IllegalArgumentException(
                        "dateRecours est requise si recoursForme=true");
            }
            if (dateRecours.isBefore(dateNotificationAnnexe13)) {
                throw new IllegalArgumentException(
                        "dateRecours ne peut pas être antérieure à dateNotificationAnnexe13");
            }
            if (typeRecours == null || typeRecours.isBlank()) {
                throw new IllegalArgumentException(
                        "typeRecours est requis si recoursForme=true");
            }
            if (!TYPES_RECOURS_VALIDES.contains(typeRecours)) {
                throw new IllegalArgumentException(
                        "typeRecours inconnu — valeurs attendues : " + TYPES_RECOURS_VALIDES);
            }
        } else if (dateRecours != null && dateRecours.isBefore(dateNotificationAnnexe13)) {
            throw new IllegalArgumentException(
                    "dateRecours ne peut pas être antérieure à dateNotificationAnnexe13");
        }
    }

    private static List<String> buildMessages(String motifOqt, String statut, boolean transfertImminent) {
        List<String> messages = new ArrayList<>();
        messages.add("Recours devant le Conseil du contentieux des étrangers (CCE) — pas de "
                + "demande d'asile à introduire, c'est un recours distinct.");
        messages.add("Recours en annulation : 30 jours calendaires (art. 39/2 §2 Loi 15/12/1980). "
                + "Pas d'effet suspensif automatique — demander la suspension séparément "
                + "(art. 39/82).");
        if (transfertImminent) {
            messages.add("URGENT : recours en extrême urgence (art. 39/82 §4 Loi 15/12/1980) à "
                    + "former dans les 5 jours ouvrables. Décision CCE sous 72h.");
        }
        messages.add("Aide juridique 2e ligne (pro deo) : vérifier éligibilité (revenus + "
                + "procédure BAJ), pro deo possible.");

        switch (motifOqt) {
            case "SEJOUR_IRREGULIER_ART_7" -> messages.add(
                    "OQT fondé sur l'art. 7 Loi 15/12/1980 (séjour irrégulier). Contester en "
                            + "démontrant un droit au séjour ou des circonstances exceptionnelles "
                            + "(art. 9bis).");
            case "REFUS_SEJOUR_APRES_DEMANDE" -> messages.add(
                    "OQT consécutif au refus d'une demande de titre de séjour — contester la "
                            + "décision de refus simultanément (annexe attachée).");
            case "FIN_SEJOUR_REGULIER" -> messages.add(
                    "Contester en prouvant le droit au séjour ou demander renouvellement avant "
                            + "expiration.");
            case "AUTRE" -> messages.add(
                    "Vérifier attentivement le motif invoqué par l'Office des étrangers et la "
                            + "disposition de la Loi 15/12/1980 citée.");
            default -> { /* unreachable */ }
        }

        if (STATUT_URGENT.equals(statut)) {
            messages.add("DÉLAI URGENT : il reste moins de 3 jours avant expiration du délai de "
                    + "recours en annulation — déposer le recours sans délai.");
        } else if (STATUT_EXPIRE.equals(statut)) {
            messages.add("DÉLAI EXPIRÉ : recours en annulation forclos. Envisager uniquement le "
                    + "recours en extrême urgence (art. 39/82 §4) si transfert imminent, ou une "
                    + "nouvelle demande de séjour (art. 9bis/9ter).");
        }
        return messages;
    }

    private static String buildFormule(String motifOqt, String statut,
                                       LocalDate dateNotification,
                                       int delaiDepartImposeJours,
                                       LocalDate dateExpirationDelaiDepart,
                                       LocalDate dateExpirationRecoursAnnulation,
                                       long joursRestants,
                                       boolean transfertImminent,
                                       boolean recoursForme,
                                       LocalDate dateRecours,
                                       String typeRecours,
                                       LocalDate dateAudience,
                                       LocalDate dateDecision) {
        if (recoursForme) {
            return String.format(
                    "Annexe 13 BE motif %s notifiée le %s (délai départ %d j → %s) — recours %s "
                            + "formé le %s devant le CCE. Audience prévisionnelle %s (J+30), "
                            + "décision prévisionnelle %s (J+90).",
                    motifOqt, dateNotification, delaiDepartImposeJours, dateExpirationDelaiDepart,
                    typeRecours, dateRecours, dateAudience, dateDecision);
        }
        return String.format(
                "Annexe 13 BE motif %s notifiée le %s (délai départ %d j → %s) — délai de recours "
                        + "en annulation (art. 39/2 §2) expire le %s (%d jours restants, statut %s)."
                        + "%s",
                motifOqt, dateNotification, delaiDepartImposeJours, dateExpirationDelaiDepart,
                dateExpirationRecoursAnnulation, joursRestants, statut,
                transfertImminent ? " Transfert imminent : extrême urgence 5 j ouvrables (art. 39/82 §4) disponible." : "");
    }
}
