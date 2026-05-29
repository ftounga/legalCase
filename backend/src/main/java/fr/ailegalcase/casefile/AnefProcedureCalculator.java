package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-214-25 — calculateur du guide des démarches ANEF (administration numérique
 * des étrangers en France) et des recours en cas de panne du dépôt dématérialisé.
 * Outil single-country FR.
 *
 * <p>Logique de statut (priorité dans cet ordre) :
 * <ol>
 *   <li>panne signalée + demande déjà adressée à la préfecture ⇒
 *       {@link AnefProcedureStatut#RECOURS_POSSIBLE} ;</li>
 *   <li>panne signalée (sans recours / démarche préfecture encore engagée) ⇒
 *       {@link AnefProcedureStatut#PANNE_EN_COURS} ;</li>
 *   <li>pas de panne, échéance du titre à moins de 30 jours ⇒
 *       {@link AnefProcedureStatut#URGENT} ;</li>
 *   <li>sinon ⇒ {@link AnefProcedureStatut#NORMAL}.</li>
 * </ol>
 *
 * <p>En cas de panne, des étapes alternatives sont générées (preuve de la panne,
 * LRAR à la préfecture, dépôt physique, recours pour faute). Le délai du recours
 * pour faute de l'administration est de 2 ans (responsabilité administrative).
 *
 * <p>Sources :
 * <ul>
 *   <li>R. 311-2-2 CESEDA — modalités dématérialisées ANEF ;</li>
 *   <li>arrêté du 27/04/2021 — obligations de dématérialisation ;</li>
 *   <li>L. 114-9 CRPA — délai substitutif en cas d'impossibilité technique ;</li>
 *   <li>CE 16 juillet 2014 n° 375479 (analogie) — responsabilité de
 *       l'administration pour faute ;</li>
 *   <li>jurisprudence TA — dépôt physique admis en cas de panne ANEF avérée.</li>
 * </ul>
 */
public final class AnefProcedureCalculator {

    /** Seuil (en jours) en deçà duquel l'échéance du titre rend la démarche URGENT. */
    static final int SEUIL_URGENT_JOURS = 30;

    /** Délai du recours pour faute de l'administration (responsabilité administrative) — 2 ans. */
    public static final int DELAI_RECOURS_FOR_FAUTE_ANNEES = 2;

    private static final String BASE_JURIDIQUE =
            "R. 311-2-2 CESEDA (modalités dématérialisées ANEF) ; arrêté du 27/04/2021 "
                    + "(obligations de dématérialisation) ; L. 114-9 CRPA (délai substitutif en "
                    + "cas d'impossibilité technique) ; CE 16 juillet 2014 n° 375479 (responsabilité "
                    + "de l'administration pour faute) ; jurisprudence TA (dépôt physique admis en "
                    + "cas de panne ANEF avérée)";

    private AnefProcedureCalculator() {
    }

    /** Surcharge utilisant la date du jour système comme référence. */
    public static AnefProcedureResult compute(String typeTitreConcerne,
                                              LocalDate dateExpirationTitre,
                                              Boolean panneeANEFSignalee,
                                              LocalDate dateTentativeDepot,
                                              Boolean demandeAdresseePrefecture) {
        return compute(typeTitreConcerne, dateExpirationTitre, panneeANEFSignalee,
                dateTentativeDepot, demandeAdresseePrefecture, LocalDate.now());
    }

    /**
     * Calcule le statut, les étapes et la recommandation de la démarche ANEF.
     *
     * @param today date de référence injectée (testabilité) — typiquement {@code LocalDate.now()}.
     */
    public static AnefProcedureResult compute(String typeTitreConcerne,
                                              LocalDate dateExpirationTitre,
                                              Boolean panneeANEFSignalee,
                                              LocalDate dateTentativeDepot,
                                              Boolean demandeAdresseePrefecture,
                                              LocalDate today) {
        validate(dateExpirationTitre, dateTentativeDepot, today);

        boolean panne = Boolean.TRUE.equals(panneeANEFSignalee);
        boolean demandePrefecture = Boolean.TRUE.equals(demandeAdresseePrefecture);

        long joursAvantExpiration = ChronoUnit.DAYS.between(today, dateExpirationTitre);

        AnefProcedureStatut statut = determineStatut(panne, demandePrefecture, joursAvantExpiration);

        List<String> etapesStandard = buildEtapesStandard();
        List<String> etapesAlternatives = panne ? buildEtapesAlternatives() : List.of();
        String recommandation = buildRecommandation(statut, joursAvantExpiration);

        return new AnefProcedureResult(
                typeTitreConcerne,
                dateExpirationTitre,
                panne,
                dateTentativeDepot,
                demandePrefecture,
                joursAvantExpiration,
                statut,
                etapesStandard,
                etapesAlternatives,
                DELAI_RECOURS_FOR_FAUTE_ANNEES,
                recommandation,
                BASE_JURIDIQUE
        );
    }

    private static AnefProcedureStatut determineStatut(boolean panne,
                                                       boolean demandePrefecture,
                                                       long joursAvantExpiration) {
        if (panne) {
            // La panne est prioritaire. Si une démarche a déjà été adressée à la
            // préfecture, le recours pour faute devient envisageable.
            return demandePrefecture
                    ? AnefProcedureStatut.RECOURS_POSSIBLE
                    : AnefProcedureStatut.PANNE_EN_COURS;
        }
        if (joursAvantExpiration < SEUIL_URGENT_JOURS) {
            return AnefProcedureStatut.URGENT;
        }
        return AnefProcedureStatut.NORMAL;
    }

    private static List<String> buildEtapesStandard() {
        List<String> etapes = new ArrayList<>();
        etapes.add("1. Créer / activer le compte sur l'administration numérique des étrangers en France (ANEF)");
        etapes.add("2. Renseigner la demande en ligne (renouvellement / première demande) et joindre les pièces");
        etapes.add("3. Régler la taxe et soumettre la demande dématérialisée");
        etapes.add("4. Suivre l'instruction et télécharger l'attestation de prolongation / récépissé");
        return etapes;
    }

    private static List<String> buildEtapesAlternatives() {
        List<String> etapes = new ArrayList<>();
        etapes.add("1. Constituer la preuve de la panne (captures d'écran horodatées, message d'erreur ANEF)");
        etapes.add("2. Adresser une demande par lettre recommandée avec accusé de réception (LRAR) à la préfecture");
        etapes.add("3. Déposer physiquement la demande complète en préfecture (sur RDV ou guichet)");
        etapes.add("4. Engager un recours pour faute de l'administration en cas de préjudice (délai 2 ans)");
        return etapes;
    }

    private static String buildRecommandation(AnefProcedureStatut statut, long joursAvantExpiration) {
        return switch (statut) {
            case NORMAL -> "Démarche ANEF standard — suivre le parcours dématérialisé en ligne "
                    + "dans les délais.";
            case URGENT -> "Échéance du titre imminente (" + joursAvantExpiration + " j) — finaliser "
                    + "et soumettre la demande ANEF sans délai pour éviter la rupture de droit au séjour.";
            case PANNE_EN_COURS -> "Panne ANEF signalée — constituer la preuve de l'indisponibilité et "
                    + "engager immédiatement la procédure alternative (LRAR + dépôt physique en préfecture) "
                    + "pour interrompre le risque de forclusion.";
            case RECOURS_POSSIBLE -> "Demande déjà adressée à la préfecture malgré la panne ANEF — en cas de "
                    + "préjudice (rupture de droit, perte d'emploi), un recours pour faute de l'administration "
                    + "est envisageable dans le délai de 2 ans (responsabilité administrative).";
        };
    }

    private static void validate(LocalDate dateExpirationTitre,
                                 LocalDate dateTentativeDepot,
                                 LocalDate today) {
        if (dateExpirationTitre == null) {
            throw new IllegalArgumentException("dateExpirationTitre est requise");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        if (dateTentativeDepot != null && dateTentativeDepot.isAfter(today)) {
            throw new IllegalArgumentException("dateTentativeDepot ne peut pas être dans le futur");
        }
    }
}
