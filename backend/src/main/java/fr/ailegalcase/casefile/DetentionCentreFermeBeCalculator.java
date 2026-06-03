package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SF-221-04 — calculateur de la détention administrative en centre fermé et de la
 * fenêtre de requête de mise en liberté devant la chambre du conseil (Belgique).
 *
 * <p>Sources <i>(à vérifier par avocat BE 2026 — durées max et prolongations
 * indicatives, fenêtre de requête indicative)</i> :
 * <ul>
 *   <li>Loi du 15/12/1980 art. 7 al. 3 / 27 / 29 / 74/5 — maintien en centre fermé selon
 *       la base légale de l'éloignement / du refoulement / de l'accès au territoire.</li>
 *   <li>AR du 02/08/2002 — régime et fonctionnement des centres fermés.</li>
 *   <li>Loi du 15/12/1980 art. 71 et s. — requête de mise en liberté devant la chambre du
 *       conseil (juridiction JUDICIAIRE statuant sur la légalité de la détention).
 *       Fenêtre indicative de 5 jours depuis la notification de la décision de détention.</li>
 * </ul>
 *
 * <p>Une situation fusionnée : la détention ET son recours. La chambre du conseil est
 * DISTINCTE du CCE (recours administratifs F-IM-31 / F-IM-32 / F-IM-57).
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — un outil = une situation.
 */
public final class DetentionCentreFermeBeCalculator {

    /** Fenêtre indicative de requête devant la chambre du conseil — 5 jours calendaires. */
    public static final int FENETRE_REQUETE_JOURS = 5;

    private static final List<String> BASES_JURIDIQUES = List.of(
            "Loi du 15/12/1980 art. 7 al. 3 / 27 / 29 / 74/5 (maintien en centre fermé) "
                    + "(à vérifier par avocat)",
            "AR du 02/08/2002 (régime des centres fermés) (à vérifier par avocat)",
            "Loi du 15/12/1980 art. 71 et s. (requête de mise en liberté devant la chambre "
                    + "du conseil — juridiction judiciaire, distincte du CCE) (à vérifier par avocat)");

    private DetentionCentreFermeBeCalculator() {
    }

    /** Surcharge utilisant la date du jour comme référence. */
    public static DetentionCentreFermeBeResult compute(LocalDate dateDebutDetention,
                                                       DetentionBaseLegale baseLegaleDetention,
                                                       Boolean prolongationNotifiee,
                                                       LocalDate dateProlongation,
                                                       Boolean requeteMiseEnLiberteDeposee,
                                                       LocalDate dateNotificationDecisionDetention) {
        return compute(dateDebutDetention, baseLegaleDetention, prolongationNotifiee,
                dateProlongation, requeteMiseEnLiberteDeposee, dateNotificationDecisionDetention,
                LocalDate.now());
    }

    /**
     * Calcule le verdict avec une date de référence injectée (testabilité).
     *
     * @param today date de référence (généralement {@code LocalDate.now()}).
     */
    public static DetentionCentreFermeBeResult compute(LocalDate dateDebutDetention,
                                                       DetentionBaseLegale baseLegaleDetention,
                                                       Boolean prolongationNotifiee,
                                                       LocalDate dateProlongation,
                                                       Boolean requeteMiseEnLiberteDeposee,
                                                       LocalDate dateNotificationDecisionDetention,
                                                       LocalDate today) {
        validate(dateDebutDetention, baseLegaleDetention, prolongationNotifiee, dateProlongation,
                requeteMiseEnLiberteDeposee, dateNotificationDecisionDetention, today);

        boolean prolongation = prolongationNotifiee;
        boolean requeteDeposee = requeteMiseEnLiberteDeposee;

        int dureeDetentionJours = (int) ChronoUnit.DAYS.between(dateDebutDetention, today);

        // Point de départ de la fenêtre de requête : la prolongation rouvre la fenêtre,
        // sinon la notification de la décision de détention.
        LocalDate pointDepartFenetre = prolongation ? dateProlongation
                : dateNotificationDecisionDetention;

        LocalDate dateLimiteRequete = null;
        Integer joursRestantsRequete = null;
        long joursDepuisDepart = -1;
        if (pointDepartFenetre != null) {
            dateLimiteRequete = pointDepartFenetre.plusDays(FENETRE_REQUETE_JOURS);
            joursDepuisDepart = ChronoUnit.DAYS.between(pointDepartFenetre, today);
            joursRestantsRequete = (int) Math.max(0, FENETRE_REQUETE_JOURS - joursDepuisDepart);
        }

        List<String> messages = new ArrayList<>();

        DetentionCentreFermeBeVerdict verdict;
        if (requeteDeposee) {
            verdict = DetentionCentreFermeBeVerdict.REQUETE_DEPOSEE;
            messages.add("Une requête de mise en liberté a déjà été introduite devant la "
                    + "chambre du conseil. Suivre la fixation de l'audience et la décision.");
        } else if (prolongation) {
            verdict = DetentionCentreFermeBeVerdict.PROLONGATION_A_CONTESTER;
            messages.add("Une prolongation de la détention a été notifiée le " + dateProlongation
                    + " : une nouvelle requête de mise en liberté peut être introduite devant "
                    + "la chambre du conseil. Date limite indicative : " + dateLimiteRequete
                    + " (fenêtre indicative de 5 jours depuis la prolongation — à vérifier).");
        } else if (pointDepartFenetre != null && joursDepuisDepart <= FENETRE_REQUETE_JOURS) {
            verdict = DetentionCentreFermeBeVerdict.REQUETE_OUVERTE;
            messages.add("La fenêtre indicative de 5 jours pour saisir la chambre du conseil "
                    + "est encore ouverte : il reste " + joursRestantsRequete + " jour(s) "
                    + "(date limite indicative : " + dateLimiteRequete + ", à vérifier).");
        } else if (pointDepartFenetre != null) {
            verdict = DetentionCentreFermeBeVerdict.REQUETE_TARDIVE;
            messages.add("La fenêtre indicative de 5 jours depuis la notification de la "
                    + "décision de détention (" + pointDepartFenetre + ") est dépassée : la "
                    + "recevabilité d'une nouvelle requête est à vérifier — la requête de mise "
                    + "en liberté devant la chambre du conseil reste toutefois en principe "
                    + "ouverte à tout moment du maintien (à vérifier par avocat).");
        } else {
            verdict = DetentionCentreFermeBeVerdict.DETENTION_EN_COURS;
            messages.add("Maintien en centre fermé constaté (durée écoulée : "
                    + dureeDetentionJours + " jour(s)). La date de notification de la décision "
                    + "de détention n'est pas connue : une requête de mise en liberté peut être "
                    + "introduite devant la chambre du conseil (juridiction judiciaire, "
                    + "distincte du CCE).");
        }

        messages.add("Base légale du maintien : " + baseLegaleLabel(baseLegaleDetention)
                + " (à vérifier par avocat).");
        messages.add("Durée de détention écoulée : " + dureeDetentionJours + " jour(s) depuis le "
                + dateDebutDetention + ".");

        return new DetentionCentreFermeBeResult(
                dateDebutDetention,
                baseLegaleDetention,
                prolongation,
                prolongation ? dateProlongation : null,
                requeteDeposee,
                dateNotificationDecisionDetention,
                verdict,
                dureeDetentionJours,
                dateLimiteRequete,
                joursRestantsRequete,
                BASES_JURIDIQUES,
                Collections.unmodifiableList(messages));
    }

    private static String baseLegaleLabel(DetentionBaseLegale base) {
        return switch (base) {
            case ART_7 -> "art. 7 al. 3 Loi 15/12/1980 (maintien en vue de l'éloignement)";
            case ART_27 -> "art. 27 Loi 15/12/1980 (exécution forcée de l'éloignement)";
            case ART_29 -> "art. 29 Loi 15/12/1980 (refoulement)";
            case ART_74_5 -> "art. 74/5 Loi 15/12/1980 (maintien à la frontière)";
            case AUTRE -> "autre base légale de maintien (à préciser)";
        };
    }

    private static void validate(LocalDate dateDebutDetention,
                                 DetentionBaseLegale baseLegaleDetention,
                                 Boolean prolongationNotifiee,
                                 LocalDate dateProlongation,
                                 Boolean requeteMiseEnLiberteDeposee,
                                 LocalDate dateNotificationDecisionDetention,
                                 LocalDate today) {
        if (dateDebutDetention == null) {
            throw new IllegalArgumentException("dateDebutDetention est requise");
        }
        if (baseLegaleDetention == null) {
            throw new IllegalArgumentException("baseLegaleDetention est requise");
        }
        if (prolongationNotifiee == null) {
            throw new IllegalArgumentException("prolongationNotifiee est requis");
        }
        if (requeteMiseEnLiberteDeposee == null) {
            throw new IllegalArgumentException("requeteMiseEnLiberteDeposee est requis");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        if (dateDebutDetention.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateDebutDetention ne peut pas être dans le futur");
        }
        if (Boolean.TRUE.equals(prolongationNotifiee) && dateProlongation == null) {
            throw new IllegalArgumentException(
                    "dateProlongation est requise lorsque prolongationNotifiee=true");
        }
        if (Boolean.TRUE.equals(requeteMiseEnLiberteDeposee)
                && dateNotificationDecisionDetention == null) {
            throw new IllegalArgumentException(
                    "dateNotificationDecisionDetention est requise lorsque "
                            + "requeteMiseEnLiberteDeposee=true");
        }
    }
}
