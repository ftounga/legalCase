package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SF-221-05 — calculateur du recours CCE en SUSPENSION ORDINAIRE (référé administratif)
 * devant le Conseil du contentieux des étrangers (Belgique).
 *
 * <p>Sources <i>(à vérifier par avocat BE 2026)</i> :
 * <ul>
 *   <li>Loi du 15/12/1980 art. 39/82 — demande de suspension de l'exécution d'un acte
 *       attaqué devant le CCE, greffée en principe sur la requête en annulation.</li>
 *   <li>Loi du 15/09/2006 (réforme du contentieux des étrangers) — conditions de la
 *       suspension : urgence + risque de préjudice grave difficilement réparable.</li>
 * </ul>
 *
 * <p><b>3e recours CCE distinct</b> des deux déjà livrés (audit § 6.2 : ne pas fusionner) :
 * <ul>
 *   <li>F-IM-31 = annulation, 30 j calendaires (légalité de l'acte) ;</li>
 *   <li>F-IM-32 = suspension en EXTRÊME urgence, 5 j ouvrables (éloignement imminent) ;</li>
 *   <li>cet outil = suspension ORDINAIRE : urgence (NON extrême) + préjudice grave
 *       difficilement réparable, greffée sur l'annulation 30 j.</li>
 * </ul>
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — un outil = une situation.
 */
public final class CceSuspensionBeCalculator {

    /** Délai calendaire de la requête en annulation, support de la suspension ordinaire. */
    public static final int DELAI_ANNULATION_JOURS = 30;

    /** tool_id de l'outil d'extrême urgence (F-IM-32) — cible du renvoi cross-outil. */
    public static final String RENVOI_EXTREME_URGENCE = "F-IM-32-cce-extreme-urgence-5j-be";

    private static final List<String> BASES_JURIDIQUES = List.of(
            "Loi du 15/12/1980 art. 39/82 (demande de suspension devant le CCE, greffée sur "
                    + "la requête en annulation) (à vérifier par avocat)",
            "Loi du 15/09/2006 (réforme du contentieux des étrangers — conditions de la "
                    + "suspension : urgence + préjudice grave difficilement réparable) "
                    + "(à vérifier par avocat)");

    private CceSuspensionBeCalculator() {
    }

    /** Surcharge utilisant la date du jour comme référence. */
    public static CceSuspensionBeResult compute(LocalDate dateNotificationDecision,
                                                Boolean recoursAnnulationIntroduit,
                                                Boolean urgenceInvocable,
                                                Boolean risquePrejudiceGraveDifficilementReparable,
                                                Boolean mesureEloignementImminente) {
        return compute(dateNotificationDecision, recoursAnnulationIntroduit, urgenceInvocable,
                risquePrejudiceGraveDifficilementReparable, mesureEloignementImminente,
                LocalDate.now());
    }

    /**
     * Calcule le verdict avec une date de référence injectée (testabilité).
     *
     * @param today date de référence (généralement {@code LocalDate.now()}).
     */
    public static CceSuspensionBeResult compute(LocalDate dateNotificationDecision,
                                                Boolean recoursAnnulationIntroduit,
                                                Boolean urgenceInvocable,
                                                Boolean risquePrejudiceGraveDifficilementReparable,
                                                Boolean mesureEloignementImminente,
                                                LocalDate today) {
        validate(dateNotificationDecision, today);

        boolean annulationIntroduite = Boolean.TRUE.equals(recoursAnnulationIntroduit);
        boolean urgence = Boolean.TRUE.equals(urgenceInvocable);
        boolean prejudice = Boolean.TRUE.equals(risquePrejudiceGraveDifficilementReparable);
        boolean eloignementImminent = Boolean.TRUE.equals(mesureEloignementImminente);

        int joursDepuisNotification =
                (int) ChronoUnit.DAYS.between(dateNotificationDecision, today);
        boolean dansDelaiAnnulation = joursDepuisNotification <= DELAI_ANNULATION_JOURS;

        List<String> messages = new ArrayList<>();
        CceSuspensionBeVerdict verdict;
        String renvoiOutil = null;

        if (eloignementImminent) {
            verdict = CceSuspensionBeVerdict.REORIENTER_EXTREME_URGENCE;
            renvoiOutil = RENVOI_EXTREME_URGENCE;
            messages.add("Une mesure d'éloignement est imminente : la suspension en EXTRÊME "
                    + "URGENCE (5 jours ouvrables, art. 39/82 et 39/85 Loi 15/12/1980) prime "
                    + "sur la suspension ordinaire. Réorienter vers l'outil « Recours CCE "
                    + "extrême urgence 5j » (F-IM-32) (à vérifier par avocat).");
        } else if (!urgence) {
            verdict = CceSuspensionBeVerdict.URGENCE_NON_DEMONTREE;
            messages.add("L'urgence n'est pas invoquée / démontrée : la demande de suspension "
                    + "ordinaire ne peut en principe prospérer (l'urgence est une condition "
                    + "autonome de la suspension — à vérifier par avocat).");
        } else if (!prejudice) {
            verdict = CceSuspensionBeVerdict.PREJUDICE_NON_DEMONTRE;
            messages.add("Le risque de préjudice grave difficilement réparable n'est pas "
                    + "démontré : la condition de la suspension n'est pas réunie (à vérifier "
                    + "par avocat).");
        } else if (!annulationIntroduite && !dansDelaiAnnulation) {
            verdict = CceSuspensionBeVerdict.HORS_DELAI_ANNULATION;
            messages.add("Le délai de 30 jours de la requête en annulation est dépassé ("
                    + joursDepuisNotification + " jours depuis la notification) et aucun recours "
                    + "en annulation n'a été introduit : la suspension, qui se greffe sur "
                    + "l'annulation, est compromise (à vérifier par avocat).");
        } else {
            verdict = CceSuspensionBeVerdict.CONDITIONS_REUNIES;
            messages.add("Les conditions de la suspension ordinaire paraissent réunies : "
                    + "urgence (non extrême) + risque de préjudice grave difficilement "
                    + "réparable, sur un recours en annulation "
                    + (annulationIntroduite ? "déjà introduit"
                            : "encore introductible (" + (DELAI_ANNULATION_JOURS
                                    - joursDepuisNotification) + " jour(s) restant(s) sur le "
                                    + "délai de 30 j)")
                    + ". La demande de suspension se greffe sur la requête en annulation "
                    + "(art. 39/82 Loi 15/12/1980 — à vérifier par avocat).");
        }

        messages.add("Jours écoulés depuis la notification de la décision attaquée : "
                + joursDepuisNotification + " jour(s) (délai d'annulation de référence : 30 j).");
        messages.add("Recours distinct de l'annulation 30j (F-IM-31) et de l'extrême urgence "
                + "5j ouvrables (F-IM-32).");

        return new CceSuspensionBeResult(
                dateNotificationDecision,
                annulationIntroduite,
                urgence,
                prejudice,
                eloignementImminent,
                verdict,
                joursDepuisNotification,
                renvoiOutil,
                BASES_JURIDIQUES,
                Collections.unmodifiableList(messages));
    }

    private static void validate(LocalDate dateNotificationDecision, LocalDate today) {
        if (dateNotificationDecision == null) {
            throw new IllegalArgumentException("dateNotificationDecision est requise");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        if (dateNotificationDecision.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateNotificationDecision ne peut pas être dans le futur");
        }
    }
}
