package fr.ailegalcase.casefile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-208-01 : calculateur du contentieux de la rétention administrative
 * (JLD — Juge des libertés et de la détention) — CESEDA L.741+ et L.743+.
 *
 * <p>Calcule :
 * <ul>
 *   <li>L'expiration du délai pour saisir le JLD (48 h après notification du placement)</li>
 *   <li>La date d'audience JLD ordinaire (J+5, L.743-9)</li>
 *   <li>L'expiration du délai d'appel contre la décision JLD (24 h)</li>
 *   <li>Le statut du délai (DISPONIBLE / URGENT / EXPIRE / RECOURS_FORME)</li>
 * </ul>
 *
 * <p>Outil <b>single-country FR</b>.
 */
public final class JldRetentionCalculator {

    /** Délai de saisine du JLD : 48 heures (≈ 2 jours calendaires) depuis le placement (L.741-1). */
    public static final int DELAI_SAISINE_JLD_JOURS = 2;

    /** Délai d'audience ordinaire JLD (L.743-9). */
    public static final int DELAI_AUDIENCE_JLD_JOURS = 5;

    /** Délai d'appel contre l'ordonnance JLD (24 h, L.743-21). */
    public static final int DELAI_APPEL_JOURS = 1;

    /** Seuil URGENT (jours restants). */
    public static final int SEUIL_URGENT_JOURS = 1;

    /** Motifs de placement valides. */
    public static final Set<String> MOTIFS_VALIDES = Set.of(
            "EXECUTION_OQTF",
            "ITF",
            "INTERDICTION_TERRITOIRE",
            "DUBLIN_TRANSFERT",
            "AUTRE"
    );

    public static final String STATUT_DISPONIBLE = "DISPONIBLE";
    public static final String STATUT_URGENT = "URGENT";
    public static final String STATUT_EXPIRE = "EXPIRE";
    public static final String STATUT_RECOURS_FORME = "RECOURS_FORME";

    private static final String BASE_JURIDIQUE =
            "CESEDA L.741-1, L.742-1, L.743-9, L.743-21 ; loi 26/01/2024 (durée max 90 j)";

    private JldRetentionCalculator() {
    }

    public static JldRetentionResult compute(LocalDate dateNotificationPlacement,
                                             String motifPlacement,
                                             boolean recoursForme,
                                             LocalDate dateRecours) {
        return compute(dateNotificationPlacement, motifPlacement, recoursForme, dateRecours,
                Clock.system(ZoneId.of("Europe/Paris")));
    }

    public static JldRetentionResult compute(LocalDate dateNotificationPlacement,
                                             String motifPlacement,
                                             boolean recoursForme,
                                             LocalDate dateRecours,
                                             Clock clock) {
        validateInputs(dateNotificationPlacement, motifPlacement, recoursForme, dateRecours, clock);

        LocalDate today = LocalDate.now(clock);
        LocalDate dateExpirationSaisineJld = dateNotificationPlacement.plusDays(DELAI_SAISINE_JLD_JOURS);
        LocalDate dateAudienceJld = dateNotificationPlacement.plusDays(DELAI_AUDIENCE_JLD_JOURS);

        String statut;
        long joursRestants;
        LocalDate dateExpirationRecoursAppel = null;

        if (recoursForme) {
            statut = STATUT_RECOURS_FORME;
            joursRestants = 0;
            dateExpirationRecoursAppel = dateRecours.plusDays(DELAI_APPEL_JOURS);
        } else {
            joursRestants = ChronoUnit.DAYS.between(today, dateExpirationSaisineJld);
            if (joursRestants < 0) {
                statut = STATUT_EXPIRE;
            } else if (joursRestants <= SEUIL_URGENT_JOURS) {
                statut = STATUT_URGENT;
            } else {
                statut = STATUT_DISPONIBLE;
            }
        }

        List<String> messages = buildMessages(motifPlacement, statut);
        String formule = buildFormule(motifPlacement, statut, dateNotificationPlacement,
                dateExpirationSaisineJld, dateAudienceJld, joursRestants, recoursForme,
                dateRecours, dateExpirationRecoursAppel);

        return new JldRetentionResult(
                dateNotificationPlacement,
                motifPlacement,
                recoursForme,
                dateRecours,
                dateExpirationSaisineJld,
                dateAudienceJld,
                dateExpirationRecoursAppel,
                joursRestants,
                statut,
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    private static void validateInputs(LocalDate dateNotificationPlacement,
                                       String motifPlacement,
                                       boolean recoursForme,
                                       LocalDate dateRecours,
                                       Clock clock) {
        if (dateNotificationPlacement == null) {
            throw new IllegalArgumentException("dateNotificationPlacement est requise");
        }
        LocalDate today = LocalDate.now(clock);
        if (dateNotificationPlacement.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateNotificationPlacement ne peut pas être dans le futur");
        }
        if (motifPlacement == null || motifPlacement.isBlank()) {
            throw new IllegalArgumentException("motifPlacement est requis");
        }
        if (!MOTIFS_VALIDES.contains(motifPlacement)) {
            throw new IllegalArgumentException(
                    "motifPlacement inconnu — valeurs attendues : " + MOTIFS_VALIDES);
        }
        if (recoursForme) {
            if (dateRecours == null) {
                throw new IllegalArgumentException("dateRecours est requise si recoursForme=true");
            }
            if (dateRecours.isBefore(dateNotificationPlacement)) {
                throw new IllegalArgumentException(
                        "dateRecours ne peut pas être antérieure à dateNotificationPlacement");
            }
        } else if (dateRecours != null && dateRecours.isBefore(dateNotificationPlacement)) {
            throw new IllegalArgumentException(
                    "dateRecours ne peut pas être antérieure à dateNotificationPlacement");
        }
    }

    private static List<String> buildMessages(String motif, String statut) {
        List<String> messages = new ArrayList<>();
        messages.add("Saisine JLD dans les 48 h suivant la notification du placement (CESEDA L.741-1+).");
        messages.add("Audience JLD ordinaire à J+5 (L.743-9). Le JLD statue sur le maintien en rétention.");
        messages.add("Appel contre l'ordonnance JLD : 24 h (L.743-21). Effet non suspensif sauf demande motivée.");
        messages.add("Durée maximale de rétention : 90 jours (loi 26/01/2024 — anciennement 60 j).");
        messages.add("Droit à interprète, conseil et avocat dès le placement (R.741-1+). À mentionner dans le recours.");

        switch (motif) {
            case "EXECUTION_OQTF" -> messages.add(
                    "Placement pour exécution d'OQTF — vérifier la régularité de l'OQTF antérieure et "
                            + "le respect du délai de départ volontaire (L.612-1).");
            case "ITF" -> messages.add(
                    "Placement pour exécution d'une interdiction du territoire français (ITF) — "
                            + "vérifier la peine prononcée et le quantum.");
            case "INTERDICTION_TERRITOIRE" -> messages.add(
                    "Interdiction administrative du territoire — vérifier la motivation et la "
                            + "proportionnalité de la mesure.");
            case "DUBLIN_TRANSFERT" -> messages.add(
                    "Placement pour exécution d'un transfert Dublin — recours suspensif 7 j "
                            + "(L.572-4) à former en parallèle (voir outil Dublin).");
            case "AUTRE" -> messages.add(
                    "Vérifier attentivement le motif invoqué par l'administration et les pièces "
                            + "fondant le placement.");
            default -> { /* unreachable */ }
        }

        if (STATUT_URGENT.equals(statut)) {
            messages.add("DÉLAI URGENT : il reste moins de 24 h avant expiration du délai de "
                    + "saisine JLD — saisir sans délai.");
        } else if (STATUT_EXPIRE.equals(statut)) {
            messages.add("DÉLAI EXPIRÉ : saisine JLD initiale forclose. Envisager un référé liberté "
                    + "(L.521-2 CJA) si atteinte grave aux droits fondamentaux.");
        }
        return messages;
    }

    private static String buildFormule(String motif, String statut,
                                       LocalDate notif,
                                       LocalDate dateExpirationSaisine,
                                       LocalDate dateAudience,
                                       long joursRestants,
                                       boolean recoursForme,
                                       LocalDate dateRecours,
                                       LocalDate dateExpirationAppel) {
        if (recoursForme) {
            return String.format(
                    "Placement en rétention notifié le %s (motif %s) — saisine JLD effectuée le %s. "
                            + "Audience JLD %s. Appel possible jusqu'au %s.",
                    notif, motif, dateRecours, dateAudience, dateExpirationAppel);
        }
        return String.format(
                "Placement en rétention notifié le %s (motif %s) — saisine JLD à effectuer avant le "
                        + "%s (%d jours restants, statut %s). Audience JLD prévisionnelle %s.",
                notif, motif, dateExpirationSaisine, joursRestants, statut, dateAudience);
    }
}
