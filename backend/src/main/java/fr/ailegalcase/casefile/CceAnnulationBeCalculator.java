package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * SF-215-13 — calculateur du délai de recours en annulation devant le Conseil du
 * Contentieux des Étrangers (CCE) : 30 jours <b>calendaires</b> à compter de la
 * notification de la décision de l'Office des Étrangers ou du CGRA
 * (art. 39/82 §4 al. 1 de la Loi du 15/12/1980 sur l'accès au territoire, le
 * séjour, l'établissement et l'éloignement des étrangers).
 *
 * <p><b>Jours calendaires</b> — pas jours ouvrables : ce calculateur n'utilise
 * volontairement PAS {@code BelgianBusinessDaysCalculator}. La distinction est
 * critique avec le recours en extrême urgence (5 jours ouvrables, F-IM-32).
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> (droit des étrangers belge). Aucun rapport
 * avec un dispositif de crédit à la consommation.
 *
 * <p>Sources : Loi 15/12/1980 art. 39/2, 39/56 à 39/85 ; loi 15/09/2006 portant
 * création du Conseil du Contentieux des Étrangers.
 */
public final class CceAnnulationBeCalculator {

    /** Délai du recours en annulation CCE — 30 jours calendaires (art. 39/82 §4 al. 1). */
    public static final int DELAI_RECOURS_JOURS = 30;

    /** Seuil (inclusif) en jours restants en deçà duquel le statut bascule en URGENT. */
    static final int SEUIL_URGENT_JOURS = 10;

    private static final String BASE_JURIDIQUE =
            "Loi du 15/12/1980 art. 39/2 §2 et 39/82 §4 al. 1 (recours en annulation CCE — "
                    + "30 jours calendaires) + loi du 15/09/2006 créant le Conseil du "
                    + "Contentieux des Étrangers";

    private static final String DELAIS_MEMOIRE =
            "Requête + mémoire administratif + mémoire en réplique 8 jours (art. 39/63 al. 4)"; // (à vérifier)

    private CceAnnulationBeCalculator() {
    }

    /** Surcharge utilisant la date du jour système (UTC) comme référence. */
    public static CceAnnulationBeResult compute(LocalDate dateNotificationDecision,
                                                CceAnnulationBeTypeDecisionEnum typeDecision,
                                                Boolean recoursForme,
                                                LocalDate dateRecours) {
        return compute(dateNotificationDecision, typeDecision, recoursForme, dateRecours,
                LocalDate.now());
    }

    /**
     * Calcule le délai et le statut du recours en annulation CCE.
     *
     * @param today date de référence injectée (testabilité) — typiquement {@code LocalDate.now()}.
     */
    public static CceAnnulationBeResult compute(LocalDate dateNotificationDecision,
                                                CceAnnulationBeTypeDecisionEnum typeDecision,
                                                Boolean recoursForme,
                                                LocalDate dateRecours,
                                                LocalDate today) {
        validate(dateNotificationDecision, typeDecision, recoursForme, dateRecours, today);

        boolean formed = recoursForme;

        LocalDate dateLimiteRecours = dateNotificationDecision.plusDays(DELAI_RECOURS_JOURS);
        long joursRestants = ChronoUnit.DAYS.between(today, dateLimiteRecours);

        CceAnnulationBeStatut statut;
        if (formed) {
            statut = CceAnnulationBeStatut.RECOURS_FORME;
        } else if (joursRestants <= 0) {
            statut = CceAnnulationBeStatut.EXPIRE;
        } else if (joursRestants <= SEUIL_URGENT_JOURS) {
            statut = CceAnnulationBeStatut.URGENT;
        } else {
            statut = CceAnnulationBeStatut.DISPONIBLE;
        }

        String recommandation = buildRecommandation(statut);

        return new CceAnnulationBeResult(
                dateNotificationDecision,
                typeDecision,
                formed,
                dateRecours,
                dateLimiteRecours,
                joursRestants,
                statut,
                recommandation,
                DELAIS_MEMOIRE,
                BASE_JURIDIQUE
        );
    }

    private static String buildRecommandation(CceAnnulationBeStatut statut) {
        if (statut == CceAnnulationBeStatut.URGENT || statut == CceAnnulationBeStatut.EXPIRE) {
            return "Basculer vers recours en extrême urgence (F-IM-32) si OQT exécutoire";
        }
        if (statut == CceAnnulationBeStatut.RECOURS_FORME) {
            return "Recours déjà introduit — suivre l'instruction CCE (mémoires art. 39/63).";
        }
        return "Délai de recours en annulation ouvert — introduire la requête dans les 30 jours "
                + "calendaires de la notification (art. 39/82 §4 al. 1).";
    }

    private static void validate(LocalDate dateNotificationDecision,
                                 CceAnnulationBeTypeDecisionEnum typeDecision,
                                 Boolean recoursForme,
                                 LocalDate dateRecours,
                                 LocalDate today) {
        if (dateNotificationDecision == null) {
            throw new IllegalArgumentException("dateNotificationDecision est requise");
        }
        if (typeDecision == null) {
            throw new IllegalArgumentException("typeDecision est requis");
        }
        if (recoursForme == null) {
            throw new IllegalArgumentException("recoursForme est requis");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        if (dateNotificationDecision.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateNotificationDecision ne peut pas être dans le futur");
        }
        if (Boolean.TRUE.equals(recoursForme) && dateRecours == null) {
            throw new IllegalArgumentException(
                    "dateRecours est requise lorsque recoursForme est vrai");
        }
        if (dateRecours != null && dateRecours.isBefore(dateNotificationDecision)) {
            throw new IllegalArgumentException(
                    "dateRecours ne peut pas être antérieure à dateNotificationDecision");
        }
    }
}
