package fr.ailegalcase.casefile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-208-02 : calculateur du recours contre une décision de transfert Dublin —
 * CESEDA L.572-1+ et Règlement (UE) 604/2013 art. 27 (recours juridictionnel
 * effectif), art. 29 (délai transfert effectif 6 mois).
 *
 * <p>Calcule :
 * <ul>
 *   <li>L'expiration du délai de recours (7 j calendaires, L.572-4)</li>
 *   <li>La date limite de transfert effectif (notification + 6 mois — Reg. 604/2013 art. 29)</li>
 *   <li>Le statut du délai (DISPONIBLE / URGENT / EXPIRE / RECOURS_FORME)</li>
 *   <li>L'effet suspensif (automatique en France, L.572-4)</li>
 * </ul>
 *
 * <p>Outil <b>single-country FR</b>.
 */
public final class DublinRecoursCalculator {

    /** Délai de recours contre décision de transfert Dublin (CESEDA L.572-4). */
    public static final int DELAI_RECOURS_JOURS = 7;

    /** Délai de transfert effectif maximum (Reg. 604/2013 art. 29 §1). */
    public static final int DELAI_TRANSFERT_EFFECTIF_MOIS = 6;

    /** Seuil URGENT. */
    public static final int SEUIL_URGENT_JOURS = 2;

    /** Motifs de transfert valides (Reg. 604/2013 chap. III). */
    public static final Set<String> MOTIFS_VALIDES = Set.of(
            "DEMANDE_ASILE_AUTRE_ETAT",
            "VISA_DELIVRE_AUTRE_ETAT",
            "ENTREE_IRREGULIERE_AUTRE_ETAT",
            "MEMBRE_FAMILLE_AUTRE_ETAT",
            "AUTRE"
    );

    public static final String STATUT_DISPONIBLE = "DISPONIBLE";
    public static final String STATUT_URGENT = "URGENT";
    public static final String STATUT_EXPIRE = "EXPIRE";
    public static final String STATUT_RECOURS_FORME = "RECOURS_FORME";

    public static final String EFFET_SUSPENSIF_AUTO = "AUTOMATIQUE";

    private static final String BASE_JURIDIQUE =
            "CESEDA L.572-1 à L.572-9 + Règlement (UE) 604/2013 art. 27 + 29 ; CJUE Mengesteab C-670/16";

    private DublinRecoursCalculator() {
    }

    public static DublinRecoursResult compute(LocalDate dateNotificationDecisionTransfert,
                                              String etatMembreResponsable,
                                              String motifTransfert,
                                              boolean recoursForme,
                                              LocalDate dateRecours) {
        return compute(dateNotificationDecisionTransfert, etatMembreResponsable, motifTransfert,
                recoursForme, dateRecours, Clock.system(ZoneId.of("Europe/Paris")));
    }

    public static DublinRecoursResult compute(LocalDate dateNotificationDecisionTransfert,
                                              String etatMembreResponsable,
                                              String motifTransfert,
                                              boolean recoursForme,
                                              LocalDate dateRecours,
                                              Clock clock) {
        validateInputs(dateNotificationDecisionTransfert, motifTransfert, recoursForme,
                dateRecours, clock);

        LocalDate today = LocalDate.now(clock);
        LocalDate dateExpirationRecours =
                dateNotificationDecisionTransfert.plusDays(DELAI_RECOURS_JOURS);
        LocalDate dateLimiteTransfertEffectif =
                dateNotificationDecisionTransfert.plusMonths(DELAI_TRANSFERT_EFFECTIF_MOIS);

        String statut;
        long joursRestants;
        if (recoursForme) {
            statut = STATUT_RECOURS_FORME;
            joursRestants = 0;
        } else {
            joursRestants = ChronoUnit.DAYS.between(today, dateExpirationRecours);
            if (joursRestants < 0) {
                statut = STATUT_EXPIRE;
            } else if (joursRestants <= SEUIL_URGENT_JOURS) {
                statut = STATUT_URGENT;
            } else {
                statut = STATUT_DISPONIBLE;
            }
        }

        List<String> messages = buildMessages(motifTransfert, etatMembreResponsable, statut);
        String formule = buildFormule(motifTransfert, etatMembreResponsable, statut,
                dateNotificationDecisionTransfert, dateExpirationRecours,
                dateLimiteTransfertEffectif, joursRestants, recoursForme, dateRecours);

        return new DublinRecoursResult(
                dateNotificationDecisionTransfert,
                etatMembreResponsable,
                motifTransfert,
                recoursForme,
                dateRecours,
                dateExpirationRecours,
                dateLimiteTransfertEffectif,
                joursRestants,
                statut,
                EFFET_SUSPENSIF_AUTO,
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    private static void validateInputs(LocalDate dateNotificationDecisionTransfert,
                                       String motifTransfert,
                                       boolean recoursForme,
                                       LocalDate dateRecours,
                                       Clock clock) {
        if (dateNotificationDecisionTransfert == null) {
            throw new IllegalArgumentException("dateNotificationDecisionTransfert est requise");
        }
        LocalDate today = LocalDate.now(clock);
        if (dateNotificationDecisionTransfert.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateNotificationDecisionTransfert ne peut pas être dans le futur");
        }
        if (motifTransfert == null || motifTransfert.isBlank()) {
            throw new IllegalArgumentException("motifTransfert est requis");
        }
        if (!MOTIFS_VALIDES.contains(motifTransfert)) {
            throw new IllegalArgumentException(
                    "motifTransfert inconnu — valeurs attendues : " + MOTIFS_VALIDES);
        }
        if (recoursForme) {
            if (dateRecours == null) {
                throw new IllegalArgumentException("dateRecours est requise si recoursForme=true");
            }
            if (dateRecours.isBefore(dateNotificationDecisionTransfert)) {
                throw new IllegalArgumentException(
                        "dateRecours ne peut pas être antérieure à dateNotificationDecisionTransfert");
            }
        } else if (dateRecours != null && dateRecours.isBefore(dateNotificationDecisionTransfert)) {
            throw new IllegalArgumentException(
                    "dateRecours ne peut pas être antérieure à dateNotificationDecisionTransfert");
        }
    }

    private static List<String> buildMessages(String motif, String etatMembre, String statut) {
        List<String> messages = new ArrayList<>();
        messages.add("Recours suspensif 7 jours calendaires devant le Tribunal administratif "
                + "(CESEDA L.572-4). L'exécution du transfert est suspendue jusqu'à ce que le TA "
                + "ait statué.");
        messages.add("Délai de transfert effectif maximum : 6 mois à compter de l'acceptation par "
                + "l'État membre responsable (Règlement Dublin III art. 29 §1). Au-delà, "
                + "responsabilité retombe sur la France.");
        messages.add("Vérifier la validité de la prise en charge / reprise par l'État membre "
                + "(criterias Reg. 604/2013 chap. III) et la régularité des actes de procédure.");
        messages.add("CJUE Mengesteab (C-670/16) : recours juridictionnel effectif inclut "
                + "contestation des critères de détermination + délais.");

        if (etatMembre != null && !etatMembre.isBlank()) {
            messages.add("État membre désigné comme responsable : " + etatMembre + ". "
                    + "Vérifier qu'il s'agit bien d'un État membre Dublin (UE + Norvège, "
                    + "Islande, Suisse, Liechtenstein).");
        }

        switch (motif) {
            case "DEMANDE_ASILE_AUTRE_ETAT" -> messages.add(
                    "Demande d'asile antérieure dans l'État membre désigné — vérifier la preuve "
                            + "Eurodac et l'art. 18 §1 b/c/d Reg. 604/2013.");
            case "VISA_DELIVRE_AUTRE_ETAT" -> messages.add(
                    "Visa délivré par l'État membre désigné — art. 12 Reg. 604/2013. Vérifier "
                            + "expiration du visa (perd compétence après 6 mois).");
            case "ENTREE_IRREGULIERE_AUTRE_ETAT" -> messages.add(
                    "Entrée irrégulière par l'État membre désigné — art. 13 §1 Reg. 604/2013. "
                            + "Compétence cesse 12 mois après franchissement.");
            case "MEMBRE_FAMILLE_AUTRE_ETAT" -> messages.add(
                    "Membre de famille dans l'État membre désigné — art. 9 à 11 Reg. 604/2013. "
                            + "Faire valoir clause humanitaire art. 17 §2.");
            case "AUTRE" -> messages.add(
                    "Vérifier attentivement le critère de détermination invoqué par "
                            + "l'administration et les pièces fondant la décision.");
            default -> { /* unreachable */ }
        }

        if (STATUT_URGENT.equals(statut)) {
            messages.add("DÉLAI URGENT : il reste 2 jours ou moins avant expiration du délai de "
                    + "recours suspensif — déposer le recours sans délai.");
        } else if (STATUT_EXPIRE.equals(statut)) {
            messages.add("DÉLAI EXPIRÉ : recours suspensif forclos. Envisager un référé liberté "
                    + "(L.521-2 CJA) ou la demande de l'effet utile via la clause humanitaire "
                    + "(art. 17 §2 Reg. 604/2013).");
        }
        return messages;
    }

    private static String buildFormule(String motif, String etatMembre, String statut,
                                       LocalDate notif,
                                       LocalDate dateExpiration,
                                       LocalDate dateLimiteTransfert,
                                       long joursRestants,
                                       boolean recoursForme,
                                       LocalDate dateRecours) {
        if (recoursForme) {
            return String.format(
                    "Décision de transfert Dublin vers %s (motif %s) notifiée le %s — recours "
                            + "suspensif formé le %s. Délai limite transfert effectif : %s.",
                    etatMembre, motif, notif, dateRecours, dateLimiteTransfert);
        }
        return String.format(
                "Décision de transfert Dublin vers %s (motif %s) notifiée le %s — délai de recours "
                        + "suspensif expire le %s (%d jours restants, statut %s). Transfert effectif "
                        + "doit intervenir avant le %s (Reg. 604/2013 art. 29).",
                etatMembre, motif, notif, dateExpiration, joursRestants, statut, dateLimiteTransfert);
    }
}
