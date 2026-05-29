package fr.ailegalcase.casefile;

import fr.ailegalcase.shared.BelgianBusinessDaysCalculator;

import java.time.LocalDate;

/**
 * SF-215-15 — calculateur du délai de recours en <b>extrême urgence</b> devant le
 * Conseil du Contentieux des Étrangers (CCE) : 5 jours <b>ouvrables</b> à compter
 * de l'acte exécutoire imminent (art. 39/82 §4 al. 2-3 de la Loi du 15/12/1980).
 *
 * <p><b>Jours ouvrables belges</b> — à la différence du recours en annulation
 * (F-IM-31, 30 jours calendaires), ce délai se compte en jours ouvrables. Le
 * calculateur réutilise {@link BelgianBusinessDaysCalculator} (source unique du
 * calendrier ouvrable belge — pas de duplication).
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> (droit des étrangers belge). Aucun rapport
 * avec un dispositif de crédit à la consommation.
 *
 * <p>Sources : Loi 15/12/1980 art. 39/82 §4 al. 2-3 ; loi 15/09/2006 portant
 * création du Conseil du Contentieux des Étrangers ; AR 11/06/2018.
 */
public final class CceExtremeUrgenceBeCalculator {

    /** Délai du recours en extrême urgence CCE — 5 jours ouvrables (art. 39/82 §4 al. 2-3). */
    public static final int DELAI_RECOURS_JOURS_OUVRABLES = 5;

    /** Seuil (inclusif) en jours ouvrables restants en deçà duquel le statut bascule CRITIQUE. */
    static final int SEUIL_CRITIQUE_JOURS = 2;

    /**
     * Tolérance amont : l'acte exécutoire peut être daté jusqu'à 7 jours dans le
     * futur (rapatriement programmé annoncé). Au-delà, l'acte n'est pas encore
     * « imminent » au sens de l'extrême urgence.
     */
    static final int MAX_FUTUR_JOURS = 7;

    private static final String BASE_JURIDIQUE =
            "Loi du 15/12/1980 art. 39/82 §4 al. 2-3 (recours en extrême urgence CCE — "
                    + "5 jours ouvrables, suspension d'extrême urgence d'une mesure d'éloignement "
                    + "ou de refoulement) + loi du 15/09/2006 créant le Conseil du Contentieux "
                    + "des Étrangers + AR 11/06/2018";

    private CceExtremeUrgenceBeCalculator() {
    }

    /** Surcharge utilisant la date du jour système comme référence. */
    public static CceExtremeUrgenceBeResult compute(LocalDate dateActeExecutoire,
                                                    CceExtremeUrgenceBeTypeActeEnum typeActe,
                                                    Boolean recoursForme,
                                                    LocalDate dateRecours) {
        return compute(dateActeExecutoire, typeActe, recoursForme, dateRecours, LocalDate.now());
    }

    /**
     * Calcule le délai et le statut du recours en extrême urgence CCE.
     *
     * @param today date de référence injectée (testabilité) — typiquement {@code LocalDate.now()}.
     */
    public static CceExtremeUrgenceBeResult compute(LocalDate dateActeExecutoire,
                                                    CceExtremeUrgenceBeTypeActeEnum typeActe,
                                                    Boolean recoursForme,
                                                    LocalDate dateRecours,
                                                    LocalDate today) {
        validate(dateActeExecutoire, typeActe, recoursForme, dateRecours, today);

        boolean formed = recoursForme;

        LocalDate dateLimiteRecours = BelgianBusinessDaysCalculator.addBusinessDays(
                dateActeExecutoire, DELAI_RECOURS_JOURS_OUVRABLES);

        // countBusinessDays exige to >= from ; si la limite est déjà passée, le délai est expiré
        // (0 jour ouvrable restant — borne basse à 0 sans lever d'exception).
        long joursOuvrablesRestants = dateLimiteRecours.isBefore(today)
                ? 0L
                : BelgianBusinessDaysCalculator.countBusinessDays(today, dateLimiteRecours);

        CceExtremeUrgenceBeStatut statut;
        if (formed) {
            statut = CceExtremeUrgenceBeStatut.RECOURS_FORME;
        } else if (joursOuvrablesRestants <= 0) {
            statut = CceExtremeUrgenceBeStatut.EXPIRE;
        } else if (joursOuvrablesRestants <= SEUIL_CRITIQUE_JOURS) {
            statut = CceExtremeUrgenceBeStatut.CRITIQUE;
        } else {
            statut = CceExtremeUrgenceBeStatut.DISPONIBLE;
        }

        // L'audience CCE en extrême urgence est fixée dans les 48h (jours ouvrables) après dépôt.
        LocalDate audienceEstimee = BelgianBusinessDaysCalculator.addBusinessDays(dateLimiteRecours, 2);

        String actionImmediate = buildActionImmediate(statut);
        String recommandation = buildRecommandation(statut);

        return new CceExtremeUrgenceBeResult(
                dateActeExecutoire,
                typeActe,
                formed,
                dateRecours,
                dateLimiteRecours,
                joursOuvrablesRestants,
                statut,
                audienceEstimee,
                actionImmediate,
                recommandation,
                BASE_JURIDIQUE
        );
    }

    private static String buildActionImmediate(CceExtremeUrgenceBeStatut statut) {
        switch (statut) {
            case CRITIQUE:
                return "ACTION IMMÉDIATE — fenêtre quasi fermée : introduire sans délai la requête "
                        + "en suspension d'extrême urgence (art. 39/82 §4) devant le CCE et solliciter "
                        + "les mesures provisoires d'extrême urgence pour suspendre l'éloignement.";
            case EXPIRE:
                return "ACTION IMMÉDIATE — délai de 5 jours ouvrables dépassé : examiner la recevabilité "
                        + "résiduelle (force majeure / réintroduction) et, à défaut, la voie de la demande "
                        + "de mesures provisoires d'extrême urgence si une nouvelle mesure d'éloignement survient.";
            default:
                return null;
        }
    }

    private static String buildRecommandation(CceExtremeUrgenceBeStatut statut) {
        if (statut == CceExtremeUrgenceBeStatut.RECOURS_FORME) {
            return "Recours en extrême urgence déjà introduit — préparer l'audience CCE "
                    + "(plaidoirie d'extrême urgence sous 48h) et le dossier de suspension.";
        }
        if (statut == CceExtremeUrgenceBeStatut.DISPONIBLE) {
            return "Délai du recours en extrême urgence ouvert — introduire la requête en suspension "
                    + "dans les 5 jours ouvrables de l'acte exécutoire (art. 39/82 §4 al. 2-3).";
        }
        // CRITIQUE / EXPIRE : la recommandation est portée par actionImmediate.
        return "Recours en extrême urgence — situation à traiter en priorité absolue (voir action immédiate).";
    }

    private static void validate(LocalDate dateActeExecutoire,
                                 CceExtremeUrgenceBeTypeActeEnum typeActe,
                                 Boolean recoursForme,
                                 LocalDate dateRecours,
                                 LocalDate today) {
        if (dateActeExecutoire == null) {
            throw new IllegalArgumentException("dateActeExecutoire est requise");
        }
        if (typeActe == null) {
            throw new IllegalArgumentException("typeActe est requis");
        }
        if (recoursForme == null) {
            throw new IllegalArgumentException("recoursForme est requis");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        if (dateActeExecutoire.isAfter(today.plusDays(MAX_FUTUR_JOURS))) {
            throw new IllegalArgumentException(
                    "dateActeExecutoire ne peut pas être à plus de " + MAX_FUTUR_JOURS
                            + " jours dans le futur (acte non encore imminent)");
        }
        if (Boolean.TRUE.equals(recoursForme) && dateRecours == null) {
            throw new IllegalArgumentException(
                    "dateRecours est requise lorsque recoursForme est vrai");
        }
        if (dateRecours != null && dateRecours.isBefore(dateActeExecutoire)) {
            throw new IllegalArgumentException(
                    "dateRecours ne peut pas être antérieure à dateActeExecutoire");
        }
    }
}
