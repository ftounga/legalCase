package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * SF-215-17 — calculateur de l'Annexe 13quinquies (OQT + interdiction d'entrée,
 * art. 74/11 de la Loi du 15/12/1980 sur l'accès au territoire, le séjour,
 * l'établissement et l'éloignement des étrangers).
 *
 * <p>Calcule :
 * <ul>
 *   <li>la <b>durée de l'interdiction d'entrée</b> (3 / 5 / 8 ans selon le motif —
 *       art. 74/11 §2) ;</li>
 *   <li>la date de fin de l'interdiction ;</li>
 *   <li>la date à partir de laquelle une <b>levée anticipée</b> peut être demandée
 *       (≥ 2/3 du délai écoulé — art. 74/12) ;</li>
 *   <li>le délai de <b>recours en annulation</b> devant le Conseil du Contentieux
 *       des Étrangers (CCE) — 30 jours <b>calendaires</b> à compter de la
 *       notification (art. 39/82 §4 al. 1).</li>
 * </ul>
 *
 * <p><b>Jours calendaires</b> — pas jours ouvrables : ce calculateur n'utilise
 * volontairement PAS {@code BelgianBusinessDaysCalculator}, à l'identique du
 * recours en annulation 30 jours (F-IM-31, SF-215-13). La distinction est critique
 * avec le recours en extrême urgence (5 jours ouvrables, F-IM-32).
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> (droit des étrangers belge). Aucun rapport
 * avec un dispositif de crédit à la consommation.
 *
 * <p>Sources : Loi 15/12/1980 art. 74/11 (interdiction d'entrée), 74/12 (levée),
 * 39/82 §4 al. 1 (recours en annulation CCE) ; AR 08/10/1981 (Annexe 13quinquies).
 */
public final class Annexe13quinquiesBeCalculator {

    /** Délai du recours en annulation CCE — 30 jours calendaires (art. 39/82 §4 al. 1). */
    public static final int DELAI_RECOURS_JOURS = 30;

    /** Seuil (inclusif) en jours restants en deçà duquel le statut bascule en URGENT. */
    static final int SEUIL_URGENT_JOURS = 10;

    private static final String BASE_JURIDIQUE =
            "Loi du 15/12/1980 art. 74/11 (interdiction d'entrée — durée 3/5/8 ans), "
                    + "art. 74/12 (levée / suspension de l'interdiction) et art. 39/82 §4 al. 1 "
                    + "(recours en annulation CCE — 30 jours calendaires) ; AR 08/10/1981 "
                    + "(Annexe 13quinquies)";

    private Annexe13quinquiesBeCalculator() {
    }

    /** Surcharge utilisant la date du jour système (UTC) comme référence. */
    public static Annexe13quinquiesBeResult compute(LocalDate dateNotificationAnnexe,
                                                    Annexe13quinquiesBeMotifEnum motifInterdictionEntree,
                                                    Boolean precedentSejour,
                                                    Boolean recoursForme,
                                                    LocalDate dateRecours) {
        return compute(dateNotificationAnnexe, motifInterdictionEntree, precedentSejour,
                recoursForme, dateRecours, LocalDate.now());
    }

    /**
     * Calcule la durée de l'interdiction, les dates dérivées et le statut du recours.
     *
     * @param today date de référence injectée (testabilité) — typiquement {@code LocalDate.now()}.
     */
    public static Annexe13quinquiesBeResult compute(LocalDate dateNotificationAnnexe,
                                                    Annexe13quinquiesBeMotifEnum motifInterdictionEntree,
                                                    Boolean precedentSejour,
                                                    Boolean recoursForme,
                                                    LocalDate dateRecours,
                                                    LocalDate today) {
        validate(dateNotificationAnnexe, motifInterdictionEntree, precedentSejour, recoursForme,
                dateRecours, today);

        boolean precedent = precedentSejour;
        boolean formed = recoursForme;

        int dureeInterdiction = dureeInterdiction(motifInterdictionEntree, precedent);

        LocalDate dateFinInterdiction = dateNotificationAnnexe.plusYears(dureeInterdiction);
        // Levée anticipée possible dès que 2/3 du délai sont écoulés (art. 74/12).
        long moisLevePrecoce = Math.round((2.0 / 3.0) * dureeInterdiction * 12);
        LocalDate datePossibleLevePrecoce = dateNotificationAnnexe.plusMonths(moisLevePrecoce);

        LocalDate dateLimiteRecoursAnnulation =
                dateNotificationAnnexe.plusDays(DELAI_RECOURS_JOURS);
        long joursRestantsRecours =
                ChronoUnit.DAYS.between(today, dateLimiteRecoursAnnulation);

        Annexe13quinquiesBeStatut statutRecours;
        if (formed) {
            statutRecours = Annexe13quinquiesBeStatut.FORME;
        } else if (joursRestantsRecours <= 0) {
            statutRecours = Annexe13quinquiesBeStatut.EXPIRE;
        } else if (joursRestantsRecours <= SEUIL_URGENT_JOURS) {
            statutRecours = Annexe13quinquiesBeStatut.URGENT;
        } else {
            statutRecours = Annexe13quinquiesBeStatut.DISPONIBLE;
        }

        List<String> conditionsLevee = List.of(
                "Aucune nouvelle infraction ni nouveau séjour irrégulier durant l'interdiction (art. 74/12).",
                "Élément d'intégration ou de réinsertion durable dans le pays d'origine ou la situation familiale.",
                "Raisons humanitaires graves justifiant le retour anticipé sur le territoire (art. 74/12 §1).",
                "Demande introduite depuis l'étranger auprès du poste diplomatique / consulaire belge compétent."
        );

        String recommandation = buildRecommandation(statutRecours, dureeInterdiction);

        return new Annexe13quinquiesBeResult(
                dateNotificationAnnexe,
                motifInterdictionEntree,
                precedent,
                dureeInterdiction,
                dateFinInterdiction,
                datePossibleLevePrecoce,
                dateLimiteRecoursAnnulation,
                joursRestantsRecours,
                formed,
                dateRecours,
                statutRecours,
                conditionsLevee,
                recommandation,
                BASE_JURIDIQUE
        );
    }

    /**
     * Durée de l'interdiction d'entrée en années — art. 74/11 §2 (à vérifier par avocat BE).
     */
    private static int dureeInterdiction(Annexe13quinquiesBeMotifEnum motif, boolean precedentSejour) {
        return switch (motif) {
            // art. 74/11 §2 (à vérifier par avocat BE)
            case SEJOUR_IRREGULIER -> precedentSejour ? 5 : 3;
            // art. 74/11 §2 (à vérifier par avocat BE)
            case MENACE_ORDRE_PUBLIC, RAISONS_SECURITE_NATIONALE -> 5;
            // art. 74/11 §2 (à vérifier par avocat BE) — maximum
            case ATTEINTE_INTERET_UE, DECISION_JUDICIAIRE -> 8;
        };
    }

    private static String buildRecommandation(Annexe13quinquiesBeStatut statut, int dureeInterdiction) {
        if (statut == Annexe13quinquiesBeStatut.URGENT || statut == Annexe13quinquiesBeStatut.EXPIRE) {
            return "Délai de recours en annulation CCE (30 jours calendaires) "
                    + (statut == Annexe13quinquiesBeStatut.EXPIRE ? "DÉPASSÉ" : "presque épuisé")
                    + " — examiner une demande de levée / suspension de l'interdiction d'entrée "
                    + "(art. 74/12) ; basculer vers le recours en extrême urgence (F-IM-32) si "
                    + "une mesure d'éloignement est imminente.";
        }
        if (statut == Annexe13quinquiesBeStatut.FORME) {
            return "Recours en annulation déjà introduit devant le CCE — suivre l'instruction "
                    + "(mémoires art. 39/63). Interdiction d'entrée de " + dureeInterdiction + " an(s).";
        }
        return "Délai de recours en annulation CCE ouvert — introduire la requête dans les 30 jours "
                + "calendaires de la notification (art. 39/82 §4 al. 1). Interdiction d'entrée de "
                + dureeInterdiction + " an(s) ; levée anticipée envisageable à partir des 2/3 du "
                + "délai (art. 74/12).";
    }

    private static void validate(LocalDate dateNotificationAnnexe,
                                 Annexe13quinquiesBeMotifEnum motifInterdictionEntree,
                                 Boolean precedentSejour,
                                 Boolean recoursForme,
                                 LocalDate dateRecours,
                                 LocalDate today) {
        if (dateNotificationAnnexe == null) {
            throw new IllegalArgumentException("dateNotificationAnnexe est requise");
        }
        if (motifInterdictionEntree == null) {
            throw new IllegalArgumentException("motifInterdictionEntree est requis");
        }
        if (precedentSejour == null) {
            throw new IllegalArgumentException("precedentSejour est requis");
        }
        if (recoursForme == null) {
            throw new IllegalArgumentException("recoursForme est requis");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        // Tolérance d'1 jour (fuseaux horaires) — au-delà, date future rejetée.
        if (dateNotificationAnnexe.isAfter(today.plusDays(1))) {
            throw new IllegalArgumentException(
                    "dateNotificationAnnexe ne peut pas être dans le futur");
        }
        if (Boolean.TRUE.equals(recoursForme) && dateRecours == null) {
            throw new IllegalArgumentException(
                    "dateRecours est requise lorsque recoursForme est vrai");
        }
        if (dateRecours != null && dateRecours.isBefore(dateNotificationAnnexe)) {
            throw new IllegalArgumentException(
                    "dateRecours ne peut pas être antérieure à dateNotificationAnnexe");
        }
    }
}
