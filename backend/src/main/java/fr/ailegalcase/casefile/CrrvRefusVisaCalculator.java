package fr.ailegalcase.casefile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-208-03 : calculateur du recours hiérarchique devant la Commission de recours
 * contre les refus de visa (CRRV) — CESEDA L.312-1 à L.312-3 + D.312-3 +
 * arrêté du 10 mars 2011.
 *
 * <p>Calcule :
 * <ul>
 *   <li>L'expiration du délai de recours hiérarchique (2 mois post-notification, D.312-3)</li>
 *   <li>Le statut du délai (DISPONIBLE / URGENT / EXPIRE / RECOURS_FORME)</li>
 *   <li>Les messages : préalable obligatoire CRRV avant TA Nantes (R.312-3 CJA)</li>
 * </ul>
 *
 * <p>Outil <b>single-country FR</b>.
 */
public final class CrrvRefusVisaCalculator {

    /** Délai de recours hiérarchique CRRV (CESEDA D.312-3 — 2 mois). */
    public static final int DELAI_RECOURS_MOIS = 2;

    /** Seuil URGENT (jours restants). */
    public static final int SEUIL_URGENT_JOURS = 7;

    /** Types de visa supportés. */
    public static final Set<String> TYPES_VISA_VALIDES = Set.of(
            "COURT_SEJOUR",
            "LONG_SEJOUR",
            "REGROUPEMENT_FAMILIAL",
            "ETUDIANT",
            "AUTRE"
    );

    public static final String STATUT_DISPONIBLE = "DISPONIBLE";
    public static final String STATUT_URGENT = "URGENT";
    public static final String STATUT_EXPIRE = "EXPIRE";
    public static final String STATUT_RECOURS_FORME = "RECOURS_FORME";

    private static final String BASE_JURIDIQUE =
            "CESEDA L.312-1 à L.312-3 ; D.312-3 ; arrêté 10/03/2011 ; CJA R.312-3 (TA Nantes)";

    private CrrvRefusVisaCalculator() {
    }

    public static CrrvRefusVisaResult compute(LocalDate dateNotificationRefus,
                                              String typeVisa,
                                              String motifRefus,
                                              boolean recoursForme,
                                              LocalDate dateRecours) {
        return compute(dateNotificationRefus, typeVisa, motifRefus, recoursForme, dateRecours,
                Clock.system(ZoneId.of("Europe/Paris")));
    }

    public static CrrvRefusVisaResult compute(LocalDate dateNotificationRefus,
                                              String typeVisa,
                                              String motifRefus,
                                              boolean recoursForme,
                                              LocalDate dateRecours,
                                              Clock clock) {
        validateInputs(dateNotificationRefus, typeVisa, recoursForme, dateRecours, clock);

        LocalDate today = LocalDate.now(clock);
        LocalDate dateExpirationRecoursCrrv = dateNotificationRefus.plusMonths(DELAI_RECOURS_MOIS);

        String statut;
        long joursRestants;
        if (recoursForme) {
            statut = STATUT_RECOURS_FORME;
            joursRestants = 0;
        } else {
            joursRestants = ChronoUnit.DAYS.between(today, dateExpirationRecoursCrrv);
            if (joursRestants < 0) {
                statut = STATUT_EXPIRE;
            } else if (joursRestants <= SEUIL_URGENT_JOURS) {
                statut = STATUT_URGENT;
            } else {
                statut = STATUT_DISPONIBLE;
            }
        }

        List<String> messages = buildMessages(typeVisa, statut);
        String formule = buildFormule(typeVisa, motifRefus, statut, dateNotificationRefus,
                dateExpirationRecoursCrrv, joursRestants, recoursForme, dateRecours);

        return new CrrvRefusVisaResult(
                dateNotificationRefus,
                typeVisa,
                motifRefus,
                recoursForme,
                dateRecours,
                dateExpirationRecoursCrrv,
                joursRestants,
                statut,
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    private static void validateInputs(LocalDate dateNotificationRefus,
                                       String typeVisa,
                                       boolean recoursForme,
                                       LocalDate dateRecours,
                                       Clock clock) {
        if (dateNotificationRefus == null) {
            throw new IllegalArgumentException("dateNotificationRefus est requise");
        }
        LocalDate today = LocalDate.now(clock);
        if (dateNotificationRefus.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateNotificationRefus ne peut pas être dans le futur");
        }
        if (typeVisa == null || typeVisa.isBlank()) {
            throw new IllegalArgumentException("typeVisa est requis");
        }
        if (!TYPES_VISA_VALIDES.contains(typeVisa)) {
            throw new IllegalArgumentException(
                    "typeVisa inconnu — valeurs attendues : " + TYPES_VISA_VALIDES);
        }
        if (recoursForme) {
            if (dateRecours == null) {
                throw new IllegalArgumentException("dateRecours est requise si recoursForme=true");
            }
            if (dateRecours.isBefore(dateNotificationRefus)) {
                throw new IllegalArgumentException(
                        "dateRecours ne peut pas être antérieure à dateNotificationRefus");
            }
        } else if (dateRecours != null && dateRecours.isBefore(dateNotificationRefus)) {
            throw new IllegalArgumentException(
                    "dateRecours ne peut pas être antérieure à dateNotificationRefus");
        }
    }

    private static List<String> buildMessages(String typeVisa, String statut) {
        List<String> messages = new ArrayList<>();
        messages.add("Délai recours CRRV : 2 mois à compter de la notification du refus consulaire "
                + "(CESEDA D.312-3).");
        messages.add("Préalable OBLIGATOIRE à la saisine du Tribunal administratif. Sans recours "
                + "CRRV préalable, le recours juridictionnel devant le TA Nantes "
                + "(R.312-3 CJA, compétence exclusive) est irrecevable.");
        messages.add("Recours non suspensif — l'absence de visa subsiste pendant l'examen.");
        messages.add("Délai imparti à la CRRV pour statuer : 2 mois (silence vaut rejet implicite, "
                + "ouvrant à son tour le délai TA Nantes 2 mois).");
        messages.add("Pièces : décision de refus, formulaire CRRV, justificatifs au fond, "
                + "passeport/CNI, traductions assermentées si applicable.");

        switch (typeVisa) {
            case "COURT_SEJOUR" -> messages.add(
                    "Visa court séjour (Schengen) — invoquer absence de menace ordre public, "
                            + "ressources suffisantes (Code Schengen art. 32).");
            case "LONG_SEJOUR" -> messages.add(
                    "Visa long séjour (≥ 3 mois) — vérifier que le motif (étudiant, salarié, "
                            + "VPF, etc.) est documenté + ressources / hébergement / lien avec la "
                            + "France.");
            case "REGROUPEMENT_FAMILIAL" -> messages.add(
                    "Regroupement familial — invoquer art. 8 CEDH (vie familiale) + critères "
                            + "L.434-2 CESEDA. Décision préalable OFII si applicable.");
            case "ETUDIANT" -> messages.add(
                    "Visa étudiant — fournir attestation pré-inscription, ressources "
                            + "(615 €/mois min), hébergement, sérieux du parcours.");
            case "AUTRE" -> messages.add(
                    "Vérifier le motif exact du refus consulaire et la disposition CESEDA / "
                            + "Code des visas invoquée.");
            default -> { /* unreachable */ }
        }

        if (STATUT_URGENT.equals(statut)) {
            messages.add("DÉLAI URGENT : il reste 7 jours ou moins avant expiration du délai de "
                    + "recours CRRV — déposer sans délai.");
        } else if (STATUT_EXPIRE.equals(statut)) {
            messages.add("DÉLAI EXPIRÉ : recours CRRV forclos. La saisine TA Nantes ne sera "
                    + "recevable que si le préalable CRRV est régularisé (impossible hors délai). "
                    + "Envisager une nouvelle demande de visa.");
        }
        return messages;
    }

    private static String buildFormule(String typeVisa, String motifRefus, String statut,
                                       LocalDate notif,
                                       LocalDate dateExpiration,
                                       long joursRestants,
                                       boolean recoursForme,
                                       LocalDate dateRecours) {
        if (recoursForme) {
            return String.format(
                    "Refus visa %s notifié le %s (motif: %s) — recours CRRV formé le %s. "
                            + "Délai TA Nantes : 2 mois après décision CRRV ou silence (rejet implicite).",
                    typeVisa, notif, motifRefus != null ? motifRefus : "n.r.", dateRecours);
        }
        return String.format(
                "Refus visa %s notifié le %s (motif: %s) — délai recours CRRV expire le %s "
                        + "(%d jours restants, statut %s). Préalable obligatoire avant TA Nantes.",
                typeVisa, notif, motifRefus != null ? motifRefus : "n.r.",
                dateExpiration, joursRestants, statut);
    }
}
