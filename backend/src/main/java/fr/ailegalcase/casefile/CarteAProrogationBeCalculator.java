package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SF-221-01 — calculateur du délai de dépôt et des conditions de prorogation de
 * la carte A (séjour temporaire / limité) en Belgique.
 *
 * <p>Sources <i>(à vérifier par avocat BE 2026)</i> :
 * <ul>
 *   <li>Loi du 15/12/1980 art. 13 — durée et prorogation du séjour limité.</li>
 *   <li>AR du 08/10/1981 art. 33 — demande de prorogation introduite auprès de
 *       la commune (administration communale du lieu de résidence).</li>
 * </ul>
 *
 * <p>Fenêtre de dépôt recommandée : 30 à 45 jours calendaires avant l'expiration
 * de la carte A.
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — un outil = une situation : ne traite que
 * le maintien temporaire du même motif de séjour (distinct de F-IM-48 carte B,
 * de la délivrance initiale et du renouvellement single permit F-IM-25).
 */
public final class CarteAProrogationBeCalculator {

    /** Borne haute de la fenêtre de dépôt — 45 jours calendaires avant expiration. */
    public static final int FENETRE_OUVERTURE_JOURS = 45;

    /** Borne basse (limite recommandée) — 30 jours calendaires avant expiration. */
    public static final int FENETRE_LIMITE_JOURS = 30;

    private static final List<String> BASES_JURIDIQUES = List.of(
            "Loi du 15/12/1980 art. 13 (durée et prorogation du séjour limité) (à vérifier par avocat)",
            "AR du 08/10/1981 art. 33 (demande de prorogation auprès de la commune) (à vérifier par avocat)");

    private CarteAProrogationBeCalculator() {
    }

    /** Surcharge utilisant la date du jour comme référence. */
    public static CarteAProrogationBeResult compute(LocalDate dateExpirationCarteA,
                                                    Boolean motifSejourPersiste,
                                                    Boolean conditionsInitialesToujoursReunies,
                                                    Boolean demandeDeposee,
                                                    LocalDate dateDemande) {
        return compute(dateExpirationCarteA, motifSejourPersiste,
                conditionsInitialesToujoursReunies, demandeDeposee, dateDemande, LocalDate.now());
    }

    /**
     * Calcule le verdict de prorogation avec une date de référence injectée (testabilité).
     *
     * @param today date de référence (généralement {@code LocalDate.now()}).
     */
    public static CarteAProrogationBeResult compute(LocalDate dateExpirationCarteA,
                                                    Boolean motifSejourPersiste,
                                                    Boolean conditionsInitialesToujoursReunies,
                                                    Boolean demandeDeposee,
                                                    LocalDate dateDemande,
                                                    LocalDate today) {
        validate(dateExpirationCarteA, motifSejourPersiste,
                conditionsInitialesToujoursReunies, demandeDeposee, dateDemande, today);

        boolean motifPersiste = motifSejourPersiste;
        boolean conditionsReunies = conditionsInitialesToujoursReunies;
        boolean deposee = demandeDeposee;

        LocalDate dateOuvertureFenetre = dateExpirationCarteA.minusDays(FENETRE_OUVERTURE_JOURS);
        LocalDate dateLimiteRecommandee = dateExpirationCarteA.minusDays(FENETRE_LIMITE_JOURS);
        long joursAvantExpiration = ChronoUnit.DAYS.between(today, dateExpirationCarteA);

        // Fenêtre RECOMMANDÉE de dépôt : entre 45 et 30 jours avant l'expiration (inclus).
        boolean dansFenetreRecommandee =
                !today.isBefore(dateOuvertureFenetre) && !today.isAfter(dateLimiteRecommandee);
        // Au-delà de la limite recommandée (< 30 j) mais carte non encore expirée.
        boolean apresLimiteNonExpiree =
                today.isAfter(dateLimiteRecommandee) && joursAvantExpiration > 0;

        List<String> messages = new ArrayList<>();

        CarteAProrogationBeVerdict verdict;
        if (deposee) {
            verdict = CarteAProrogationBeVerdict.DEMANDE_DEPOSEE;
            messages.add("Une demande de prorogation a déjà été déposée"
                    + (dateDemande != null ? " le " + dateDemande + "." : "."));
        } else if (!motifPersiste || !conditionsReunies) {
            verdict = CarteAProrogationBeVerdict.CONDITIONS_NON_REUNIES;
            if (!motifPersiste) {
                messages.add("Le motif de séjour ne persiste plus : la prorogation au même"
                        + " motif n'est en principe pas justifiée.");
            }
            if (!conditionsReunies) {
                messages.add("Les conditions initiales du séjour ne sont plus réunies :"
                        + " la prorogation est compromise.");
            }
        } else if (joursAvantExpiration <= 0) {
            verdict = CarteAProrogationBeVerdict.EXPIREE;
            messages.add("La carte A est expirée et aucune demande n'a été déposée :"
                    + " risque de séjour irrégulier — régularisation à examiner d'urgence.");
        } else if (dansFenetreRecommandee) {
            verdict = CarteAProrogationBeVerdict.PROROGEABLE;
            messages.add("Conditions de prorogation réunies et fenêtre de dépôt recommandée"
                    + " ouverte. Déposer la demande à la commune avant le " + dateLimiteRecommandee
                    + " (30 à 45 jours avant l'expiration).");
        } else if (apresLimiteNonExpiree) {
            verdict = CarteAProrogationBeVerdict.A_DEPOSER_URGENT;
            messages.add("La limite recommandée (30 jours avant expiration) est dépassée mais"
                    + " la carte n'est pas encore expirée : déposer la demande de prorogation"
                    + " à la commune sans délai.");
        } else {
            verdict = CarteAProrogationBeVerdict.PROROGEABLE;
            messages.add("Conditions de prorogation réunies. La fenêtre de dépôt recommandée"
                    + " s'ouvre le " + dateOuvertureFenetre + " (30 à 45 jours avant l'expiration).");
        }

        if (verdict != CarteAProrogationBeVerdict.DEMANDE_DEPOSEE) {
            if (joursAvantExpiration > 0) {
                messages.add("Il reste " + joursAvantExpiration + " jour(s) avant l'expiration"
                        + " de la carte A (" + dateExpirationCarteA + ").");
            }
            if (joursAvantExpiration > FENETRE_OUVERTURE_JOURS) {
                messages.add("La fenêtre de dépôt recommandée s'ouvre le " + dateOuvertureFenetre + ".");
            }
        }

        return new CarteAProrogationBeResult(
                dateExpirationCarteA,
                motifPersiste,
                conditionsReunies,
                deposee,
                deposee ? dateDemande : null,
                verdict,
                joursAvantExpiration,
                dateOuvertureFenetre,
                dateLimiteRecommandee,
                BASES_JURIDIQUES,
                Collections.unmodifiableList(messages));
    }

    private static void validate(LocalDate dateExpirationCarteA,
                                 Boolean motifSejourPersiste,
                                 Boolean conditionsInitialesToujoursReunies,
                                 Boolean demandeDeposee,
                                 LocalDate dateDemande,
                                 LocalDate today) {
        if (dateExpirationCarteA == null) {
            throw new IllegalArgumentException("dateExpirationCarteA est requise");
        }
        if (motifSejourPersiste == null) {
            throw new IllegalArgumentException("motifSejourPersiste est requis");
        }
        if (conditionsInitialesToujoursReunies == null) {
            throw new IllegalArgumentException("conditionsInitialesToujoursReunies est requis");
        }
        if (demandeDeposee == null) {
            throw new IllegalArgumentException("demandeDeposee est requis");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        if (Boolean.TRUE.equals(demandeDeposee) && dateDemande == null) {
            throw new IllegalArgumentException(
                    "dateDemande est requise lorsque demandeDeposee=true");
        }
    }
}
