package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-FA-18-07 : calculateur de recevabilité de la <strong>possession d'état</strong>
 * comme mode de preuve / d'établissement de la filiation
 * (FR — art. 311-1 + 311-2 + 317 Cciv).
 *
 * <p>La possession d'état est un mode de preuve de la filiation par les faits.
 * Elle est constituée par un faisceau d'indices (art. 311-1) :</p>
 * <ul>
 *   <li><strong>Tractatus</strong> : traitement comme enfant (logé, nourri,
 *       éduqué, présenté à la famille).</li>
 *   <li><strong>Fama</strong> : la famille, la société, l'autorité publique le
 *       considèrent comme tel.</li>
 *   <li><strong>Nomen</strong> : porte le nom de famille du parent
 *       (facultatif depuis l'ordonnance n°2005-759 du 4 juillet 2005).</li>
 * </ul>
 *
 * <p>Conditions cardinales (art. 311-2) : la possession d'état doit être
 * <strong>continue, paisible, publique et non équivoque</strong>.</p>
 *
 * <p>Effets selon le dispositif :</p>
 * <ul>
 *   <li><strong>CONSTAT_NOTAIRE</strong> (art. 317) — possession d'état avant
 *       décès → acte de notoriété, force probante (5 ans pour contester).</li>
 *   <li><strong>PREUVE_JUSTICE</strong> — à l'occasion d'une action en
 *       recherche ou contestation (10 ans depuis la cessation).</li>
 * </ul>
 *
 * <p>Outil <strong>single-country FRANCE</strong>. La Belgique a un régime
 * distinct (CC art. 331-1) qui sera traité par une feature jumelle au backlog.</p>
 */
public final class PossessionEtatCalculator {

    /** Verdict de recevabilité de la possession d'état. */
    public enum VerdictRecevabilite {
        ELEVEE,
        MOYENNE,
        FAIBLE
    }

    /** Dispositif applicable selon les critères et la durée. */
    public enum DispositifApplicable {
        /** Acte de notoriété par notaire (art. 317). */
        CONSTAT_NOTAIRE,
        /** Preuve à l'occasion d'une action judiciaire (art. 311-1 + 311-2). */
        PREUVE_JUSTICE,
        /** Dispositif non applicable — possession d'état non caractérisée. */
        AUCUN
    }

    /** Durée minimale pour caractériser la "possession publique longue" (art. 317). */
    public static final int DUREE_MIN_CONSTAT_NOTAIRE_ANS = 5;

    /** Délai de contestation d'un acte de notoriété (art. 317 al. 2). */
    public static final int DELAI_CONTESTATION_ACTE_ANS = 5;

    /** Délai de contestation depuis la cessation de la possession d'état. */
    public static final int DELAI_CONTESTATION_CESSATION_ANS = 10;

    /** Base juridique consolidée. */
    private static final String BASE_JURIDIQUE = "Art. 311-1 + 311-2 + 317 Cciv";

    private PossessionEtatCalculator() {}

    /**
     * Évalue la recevabilité d'une possession d'état.
     *
     * @param dateDebutPossession    date de début alléguée (obligatoire)
     * @param dateFinPossession      date de fin / référence (obligatoire ; si possession en cours, date du jour)
     * @param tractatus              traitement comme enfant ?
     * @param fama                   réputation publique (famille, société, autorité) ?
     * @param nomen                  porte le nom du parent (facultatif depuis 2005) ?
     * @param continueCondition      possession continue (sans interruption notable) ?
     * @param paisible               possession paisible (sans contestation contemporaine) ?
     * @param nonEquivoque           possession non équivoque (lien clair, sans ambiguïté) ?
     * @param country                pays du workspace (FRANCE attendu)
     * @return résultat structuré
     * @throws IllegalArgumentException si paramètre invalide ou pays non supporté
     */
    public static PossessionEtatResult compute(LocalDate dateDebutPossession,
                                               LocalDate dateFinPossession,
                                               Boolean tractatus,
                                               Boolean fama,
                                               Boolean nomen,
                                               Boolean continueCondition,
                                               Boolean paisible,
                                               Boolean nonEquivoque,
                                               String country) {
        if (dateDebutPossession == null) {
            throw new IllegalArgumentException("Date de début de possession requise");
        }
        if (dateFinPossession == null) {
            throw new IllegalArgumentException("Date de fin de possession requise");
        }
        if (dateFinPossession.isBefore(dateDebutPossession)) {
            throw new IllegalArgumentException(
                    "Date de fin doit être postérieure ou égale à la date de début");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil non disponible pour le pays " + country
                            + " — l'équivalent BE (CC art. 331-1 — possession d'état comme mode "
                            + "de preuve résiduel, conditions et délais propres) sera traité dans "
                            + "une feature jumelle dédiée au backlog.");
        }
        if (tractatus == null) tractatus = false;
        if (fama == null) fama = false;
        if (nomen == null) nomen = false;
        if (continueCondition == null) continueCondition = false;
        if (paisible == null) paisible = false;
        if (nonEquivoque == null) nonEquivoque = false;

        // Durée en années (entiers + reste mensuel pour précision interne)
        long dureeMois = ChronoUnit.MONTHS.between(dateDebutPossession, dateFinPossession);
        int dureePossessionAnnees = (int) (dureeMois / 12);

        // Critères constitutifs (art. 311-1)
        int constitutifsCount = 0;
        if (tractatus) constitutifsCount++;
        if (fama) constitutifsCount++;
        if (nomen) constitutifsCount++;

        // Conditions cardinales (art. 311-2) — toutes obligatoires
        boolean conditionsRemplies = continueCondition && paisible && nonEquivoque;

        // Critères "essentiels" minimaux : tractatus + fama (le nomen est facultatif
        // depuis 2005 — ord. n°2005-759 du 4/7/2005).
        boolean essentielsRemplis = tractatus && fama;

        // Score (0 à 100)
        int score = 0;
        if (tractatus) score += 25;
        if (fama) score += 25;
        if (nomen) score += 10; // facultatif → poids moindre
        if (continueCondition) score += 15;
        if (paisible) score += 10;
        if (nonEquivoque) score += 10;
        // Bonus durée
        if (dureePossessionAnnees >= DUREE_MIN_CONSTAT_NOTAIRE_ANS) {
            score += 5;
        }
        // Plafond
        if (score > 100) score = 100;

        // Verdict
        VerdictRecevabilite verdict;
        if (!essentielsRemplis || !conditionsRemplies) {
            verdict = VerdictRecevabilite.FAIBLE;
        } else if (dureePossessionAnnees >= DUREE_MIN_CONSTAT_NOTAIRE_ANS && score >= 80) {
            verdict = VerdictRecevabilite.ELEVEE;
        } else if (dureePossessionAnnees >= 1 && score >= 60) {
            verdict = VerdictRecevabilite.MOYENNE;
        } else {
            verdict = VerdictRecevabilite.FAIBLE;
        }

        // Dispositif
        DispositifApplicable dispositif;
        if (verdict == VerdictRecevabilite.FAIBLE) {
            dispositif = DispositifApplicable.AUCUN;
        } else if (essentielsRemplis && conditionsRemplies
                && dureePossessionAnnees >= DUREE_MIN_CONSTAT_NOTAIRE_ANS) {
            dispositif = DispositifApplicable.CONSTAT_NOTAIRE;
        } else {
            dispositif = DispositifApplicable.PREUVE_JUSTICE;
        }

        // Listes critères remplis / manquants
        List<String> criteresRemplis = new ArrayList<>();
        List<String> criteresManquants = new ArrayList<>();
        addCritere(criteresRemplis, criteresManquants, tractatus,
                "Tractatus — traitement comme enfant (art. 311-1)");
        addCritere(criteresRemplis, criteresManquants, fama,
                "Fama — réputation publique (art. 311-1)");
        addCritere(criteresRemplis, criteresManquants, nomen,
                "Nomen — port du nom (art. 311-1, facultatif depuis 2005)");
        addCritere(criteresRemplis, criteresManquants, continueCondition,
                "Possession continue (art. 311-2)");
        addCritere(criteresRemplis, criteresManquants, paisible,
                "Possession paisible (art. 311-2)");
        addCritere(criteresRemplis, criteresManquants, nonEquivoque,
                "Possession non équivoque (art. 311-2)");
        if (dureePossessionAnnees >= DUREE_MIN_CONSTAT_NOTAIRE_ANS) {
            criteresRemplis.add("Durée ≥ 5 ans — possession publique longue (art. 317)");
        } else {
            criteresManquants.add("Durée < 5 ans (" + dureePossessionAnnees
                    + " an(s)) — la possession publique longue (art. 317) "
                    + "n'est pas caractérisée pour un acte de notoriété renforcé.");
        }

        String formule = String.format(Locale.ROOT,
                "Début=%s + Fin=%s + Durée=%d an(s) + Tractatus=%s + Fama=%s + Nomen=%s + "
                        + "Continue=%s + Paisible=%s + NonEquivoque=%s + Constitutifs=%d/3 + "
                        + "Conditions=%s → score %d → verdict %s → dispositif %s, "
                        + "%d critère(s) rempli(s), %d critère(s) manquant(s)",
                dateDebutPossession.toString(), dateFinPossession.toString(),
                dureePossessionAnnees, tractatus, fama, nomen,
                continueCondition, paisible, nonEquivoque,
                constitutifsCount, conditionsRemplies,
                score, verdict.name(), dispositif.name(),
                criteresRemplis.size(), criteresManquants.size());

        List<String> messages = buildMessages(verdict, dispositif, dureePossessionAnnees,
                tractatus, fama, nomen, continueCondition, paisible, nonEquivoque,
                essentielsRemplis, conditionsRemplies);

        return new PossessionEtatResult(
                dateDebutPossession,
                dateFinPossession,
                tractatus,
                fama,
                nomen,
                continueCondition,
                paisible,
                nonEquivoque,
                countryNormalized,
                verdict,
                dispositif,
                score,
                dureePossessionAnnees,
                DELAI_CONTESTATION_ACTE_ANS,
                DELAI_CONTESTATION_CESSATION_ANS,
                criteresRemplis,
                criteresManquants,
                BASE_JURIDIQUE,
                formule,
                messages
        );
    }

    private static void addCritere(List<String> remplis, List<String> manquants,
                                   boolean rempli, String libelle) {
        if (rempli) {
            remplis.add(libelle);
        } else {
            manquants.add(libelle);
        }
    }

    private static List<String> buildMessages(VerdictRecevabilite verdict,
                                              DispositifApplicable dispositif,
                                              int dureeAnnees,
                                              boolean tractatus,
                                              boolean fama,
                                              boolean nomen,
                                              boolean continueCondition,
                                              boolean paisible,
                                              boolean nonEquivoque,
                                              boolean essentielsRemplis,
                                              boolean conditionsRemplies) {
        List<String> msgs = new ArrayList<>();
        msgs.add("Possession d'état (art. 311-1 Cciv) — mode de preuve de la filiation "
                + "par les faits, constituée par un faisceau d'indices : tractatus + "
                + "fama + nomen.");
        msgs.add("Conditions cardinales (art. 311-2) : possession continue, paisible, "
                + "publique et non équivoque — toutes obligatoires.");
        msgs.add("Le port du nom (nomen) est FACULTATIF depuis l'ordonnance n°2005-759 "
                + "du 4 juillet 2005 — son absence ne fait pas obstacle si tractatus "
                + "et fama sont caractérisés.");

        msgs.add("Durée alléguée : " + dureeAnnees + " an(s).");

        if (!essentielsRemplis) {
            msgs.add("Critères essentiels NON remplis — il manque le tractatus et/ou "
                    + "la fama. La possession d'état ne peut être caractérisée sans "
                    + "ces deux éléments centraux du faisceau d'indices.");
        }
        if (!conditionsRemplies) {
            List<String> manquants = new ArrayList<>();
            if (!continueCondition) manquants.add("continue");
            if (!paisible) manquants.add("paisible");
            if (!nonEquivoque) manquants.add("non équivoque");
            msgs.add("Conditions cardinales NON remplies — il manque : "
                    + String.join(", ", manquants) + ". La possession d'état exige "
                    + "que TOUTES ces conditions soient réunies (art. 311-2).");
        }

        if (tractatus) {
            msgs.add("Tractatus établi — l'enfant a été traité comme tel (logé, nourri, "
                    + "éduqué, présenté). Élément central du faisceau d'indices.");
        }
        if (fama) {
            msgs.add("Fama établie — la famille, la société et l'autorité publique "
                    + "considèrent l'enfant comme tel.");
        }
        if (!nomen) {
            msgs.add("Nomen non porté — sans incidence directe depuis 2005, mais à "
                    + "documenter par les autres éléments du faisceau.");
        }

        switch (dispositif) {
            case CONSTAT_NOTAIRE -> msgs.add("Dispositif applicable : CONSTAT PAR NOTAIRE "
                    + "(art. 317 Cciv) — un acte de notoriété peut être établi. Force "
                    + "probante avec délai de contestation de "
                    + DELAI_CONTESTATION_ACTE_ANS + " ans à compter de l'établissement "
                    + "de l'acte. Voie privilégiée pour sécuriser la filiation hors procès.");
            case PREUVE_JUSTICE -> msgs.add("Dispositif applicable : PREUVE EN JUSTICE "
                    + "(art. 311-1 + 311-2 Cciv) — la possession d'état pourra être "
                    + "invoquée à l'occasion d'une action en recherche ou en "
                    + "contestation de filiation. Délai : "
                    + DELAI_CONTESTATION_CESSATION_ANS + " ans à compter de la "
                    + "cessation de la possession.");
            case AUCUN -> msgs.add("Dispositif AUCUN — la possession d'état n'est pas "
                    + "caractérisée. Étudier les voies alternatives (reconnaissance "
                    + "volontaire art. 316, action en recherche art. 327, contestation "
                    + "art. 332-334 Cciv) avant tout dépôt.");
        }

        switch (verdict) {
            case ELEVEE -> msgs.add("VERDICT ELEVEE — possession d'état solidement "
                    + "caractérisée (≥ 5 ans + tous les critères essentiels + conditions "
                    + "cardinales). Engagement de la procédure d'acte de notoriété "
                    + "fortement recommandé.");
            case MOYENNE -> msgs.add("VERDICT MOYENNE — possession d'état caractérisée "
                    + "mais durée insuffisante ou critères partiels. Voie judiciaire "
                    + "recommandée pour faire trancher la filiation par le juge ; "
                    + "renforcer les preuves matérielles avant assignation.");
            case FAIBLE -> msgs.add("VERDICT FAIBLE — possession d'état non caractérisée "
                    + "(critères essentiels manquants ou conditions cardinales non "
                    + "remplies). Une demande sur ce seul fondement serait écartée — "
                    + "envisager une autre voie (reconnaissance, recherche en paternité).");
        }

        msgs.add("Effets en cas de succès : la possession d'état permet d'établir la "
                + "filiation (art. 311-1) ou de la conforter dans une action contentieuse, "
                + "avec les droits-devoirs associés (autorité parentale art. 372, nom "
                + "art. 311-21, contribution à l'entretien art. 371-2, vocation "
                + "successorale art. 733).");
        msgs.add("Tribunal compétent (en cas de procédure judiciaire) : tribunal "
                + "judiciaire avec représentation obligatoire par avocat (art. 318 "
                + "Cciv applicable par renvoi).");
        msgs.add("Notaire compétent (en cas d'acte de notoriété art. 317) : notaire du "
                + "lieu de naissance ou du dernier domicile de l'enfant — l'acte est "
                + "dressé sur la foi de trois témoins au moins.");
        msgs.add("Base juridique : " + BASE_JURIDIQUE + ".");

        return msgs;
    }
}
