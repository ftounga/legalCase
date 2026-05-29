package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * SF-214-13 — calculateur du délai de dépôt du renouvellement du titre de séjour :
 * la demande doit être déposée dans les 2 mois précédant l'expiration du titre
 * (art. R. 433-1 CESEDA). Le dépôt dans les délais ouvre droit à un récépissé
 * valant maintien du séjour régulier (R. 433-2). Outil single-country FR.
 *
 * <p>Le délai est calculé en mois calendaires : {@code dateExpirationTitre − 2 mois}
 * (date optimale, R. 433-1) et {@code dateExpirationTitre − 1 mois} (seuil
 * impératif). Le statut bascule en URGENT lorsqu'il reste moins de 30 jours avant
 * la date optimale.
 *
 * <p>Sources :
 * <ul>
 *   <li>R. 433-1 CESEDA — délai de dépôt du renouvellement (2 mois avant expiration) ;</li>
 *   <li>R. 433-2 CESEDA — effet du récépissé (maintien du séjour régulier) ;</li>
 *   <li>jurisprudence TA — conséquences du dépôt tardif (pas de récépissé) ;</li>
 *   <li>CE 25 octobre 2004 n° 258806 — obligation de dépôt dans les délais.</li>
 * </ul>
 */
public final class RenouvellementDelaiCalculator {

    /** Délai optimal de dépôt — 2 mois calendaires avant expiration (art. R. 433-1). */
    public static final int DELAI_OPTIMAL_MOIS = 2;

    /** Seuil impératif — 1 mois calendaire avant expiration. */
    public static final int DELAI_IMPERATIF_MOIS = 1;

    /** Seuil (exclusif) en jours restants en deçà duquel le statut bascule en URGENT. */
    static final int SEUIL_URGENT_JOURS = 30;

    /** Seuil (exclusif) en jours restants en deçà duquel le statut bascule en A_DEPOSER. */
    static final int SEUIL_A_DEPOSER_JOURS = 60;

    /** Tolérance (jours) au-delà de l'expiration en deçà de laquelle aucun retard n'est signalé. */
    static final int TOLERANCE_RETARD_JOURS = 15;

    private static final String BASE_JURIDIQUE =
            "Art. R. 433-1 CESEDA (dépôt du renouvellement dans les 2 mois précédant "
                    + "l'expiration du titre) ; R. 433-2 CESEDA (récépissé valant maintien du "
                    + "séjour régulier en cas de dépôt dans les délais)";

    private RenouvellementDelaiCalculator() {
    }

    /** Surcharge utilisant la date du jour système comme référence. */
    public static RenouvellementDelaiResult compute(LocalDate dateExpirationTitre,
                                                    LocalDate dateDepotDossier,
                                                    String typeTitre) {
        return compute(dateExpirationTitre, dateDepotDossier, typeTitre, LocalDate.now());
    }

    /**
     * Calcule les échéances et le statut du dépôt de renouvellement.
     *
     * @param today date de référence injectée (testabilité) — typiquement {@code LocalDate.now()}.
     */
    public static RenouvellementDelaiResult compute(LocalDate dateExpirationTitre,
                                                    LocalDate dateDepotDossier,
                                                    String typeTitre,
                                                    LocalDate today) {
        validate(dateExpirationTitre, today);

        LocalDate dateOptimalDepot = dateExpirationTitre.minusMonths(DELAI_OPTIMAL_MOIS);
        LocalDate dateDepotImperatif = dateExpirationTitre.minusMonths(DELAI_IMPERATIF_MOIS);

        boolean depose = dateDepotDossier != null;
        boolean expire = today.isAfter(dateExpirationTitre);

        Long joursRestantsAvantOptimal;
        Long joursRestantsAvantImperatif;
        RenouvellementDelaiStatut statut;

        if (depose) {
            // Le dépôt effectué est prioritaire : plus de décompte de délai.
            joursRestantsAvantOptimal = null;
            joursRestantsAvantImperatif = null;
            statut = RenouvellementDelaiStatut.DEPOSE;
        } else {
            long joursOptimal = ChronoUnit.DAYS.between(today, dateOptimalDepot);
            long joursImperatif = ChronoUnit.DAYS.between(today, dateDepotImperatif);
            joursRestantsAvantOptimal = joursOptimal;
            joursRestantsAvantImperatif = joursImperatif;
            if (expire) {
                statut = RenouvellementDelaiStatut.EXPIRE;
            } else if (joursOptimal < SEUIL_URGENT_JOURS) {
                statut = RenouvellementDelaiStatut.A_DEPOSER_URGENT;
            } else if (joursOptimal < SEUIL_A_DEPOSER_JOURS) {
                statut = RenouvellementDelaiStatut.A_DEPOSER;
            } else {
                statut = RenouvellementDelaiStatut.EN_AVANCE;
            }
        }

        boolean risqueIrruption = expire && !depose;
        boolean alerteRetard = depose
                && dateDepotDossier.isAfter(dateExpirationTitre.plusDays(TOLERANCE_RETARD_JOURS));

        String recommandation = buildRecommandation(statut, alerteRetard);

        return new RenouvellementDelaiResult(
                dateExpirationTitre,
                dateDepotDossier,
                typeTitre,
                dateOptimalDepot,
                dateDepotImperatif,
                joursRestantsAvantOptimal,
                joursRestantsAvantImperatif,
                statut,
                risqueIrruption,
                alerteRetard,
                recommandation,
                BASE_JURIDIQUE
        );
    }

    private static String buildRecommandation(RenouvellementDelaiStatut statut, boolean alerteRetard) {
        if (statut == RenouvellementDelaiStatut.DEPOSE) {
            return alerteRetard
                    ? "Demande de renouvellement déposée hors délai (plus de 15 jours après "
                            + "l'expiration) : le dépôt reste recevable mais sans continuité du "
                            + "récépissé — documenter la régularité du séjour sur la période d'interruption."
                    : "Demande de renouvellement déposée : conserver le récépissé valant maintien "
                            + "du séjour régulier (art. R. 433-2 CESEDA) et suivre la délivrance du "
                            + "nouveau titre.";
        }
        return switch (statut) {
            case EXPIRE -> "Titre expiré sans dépôt de renouvellement : séjour devenu irrégulier — "
                    + "déposer la demande sans délai et documenter les circonstances afin de limiter "
                    + "l'interruption des droits (récépissé non garanti en cas de dépôt tardif).";
            case A_DEPOSER_URGENT -> "Échéance imminente : déposer la demande de renouvellement "
                    + "avant le seuil impératif (1 mois avant l'expiration) pour sécuriser la "
                    + "délivrance du récépissé (art. R. 433-1 CESEDA).";
            case A_DEPOSER -> "Préparer et déposer la demande de renouvellement dans les 2 mois "
                    + "précédant l'expiration du titre (art. R. 433-1 CESEDA).";
            case EN_AVANCE -> "Renouvellement à anticiper : la fenêtre optimale de dépôt s'ouvre "
                    + "2 mois avant l'expiration du titre (art. R. 433-1 CESEDA).";
            case DEPOSE -> ""; // traité ci-dessus
        };
    }

    private static void validate(LocalDate dateExpirationTitre, LocalDate today) {
        if (dateExpirationTitre == null) {
            throw new IllegalArgumentException("dateExpirationTitre est requise");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
    }
}
