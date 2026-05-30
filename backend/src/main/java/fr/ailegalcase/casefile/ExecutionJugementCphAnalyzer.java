package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-218-03 : analyseur de l'exécution forcée d'un jugement du Conseil de
 * prud'hommes (CPH). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Deux situations sont distinguées (invariant CLAUDE.md — un outil = une
 * situation métier) :
 * <ul>
 *   <li><b>Employeur in bonis</b> : recouvrement direct par voie d'exécution
 *       (signification préalable obligatoire, exécution provisoire de droit des
 *       créances salariales art. R. 1454-28 / 514 CPC, commandement de payer,
 *       mandatement huissier, mesures conservatoires) → verdict
 *       {@code EXECUTION_DIRECTE}.</li>
 *   <li><b>Employeur en procédure collective</b> (redressement / liquidation
 *       judiciaire) : les créances salariales sont garanties par l'AGS dans la
 *       limite des plafonds (L. 3253-6 et s. Code travail). La voie n'est plus
 *       l'exécution directe mais la déclaration de créance au mandataire et la
 *       saisine du CGEA → verdict {@code RELAIS_AGS}. Si la date d'ouverture de
 *       la procédure est inconnue, le calcul de la garantie est impossible →
 *       {@code BLOQUE_INFO_MANQUANTE}.</li>
 * </ul>
 *
 * <p>Sources :
 * <ul>
 *   <li>art. 514 CPC — exécution provisoire de droit des décisions de première
 *       instance ;</li>
 *   <li>R. 1454-28 CPC — exécution provisoire de droit prud'homale des créances
 *       salariales (limite 9 mois de salaire moyen) ;</li>
 *   <li>L. 3253-6 à L. 3253-21 Code travail — garantie AGS, plafonds, CGEA ;</li>
 *   <li>L. 3253-8 Code travail — super-privilège des 60 derniers jours de salaire ;</li>
 *   <li>D. 3253-5 Code travail — plafonds 6 / 5 / 4 × PMSS selon l'ancienneté.</li>
 * </ul>
 */
public final class ExecutionJugementCphAnalyzer {

    private static final String BASE_JURIDIQUE =
            "art. 514 CPC (exécution provisoire de droit) ; R. 1454-28 CPC "
                    + "(exécution provisoire de droit des créances salariales "
                    + "prud'homales) ; L. 3253-6 à L. 3253-21 Code travail (garantie "
                    + "AGS, plafonds, CGEA) ; L. 3253-8 Code travail (super-privilège "
                    + "des 60 derniers jours de salaire) ; D. 3253-5 Code travail "
                    + "(plafonds 6 / 5 / 4 × PMSS selon l'ancienneté du contrat)";

    private ExecutionJugementCphAnalyzer() {
    }

    /** Surcharge utilisant la date du jour système comme référence. */
    public static ExecutionJugementCphResult analyze(LocalDate dateJugement,
                                                     Double montantCondamnation,
                                                     Boolean executionProvisoireOrdonnee,
                                                     ExecutionJugementCphSituationEmployeur situationEmployeur,
                                                     LocalDate dateOuvertureProcedureCollective,
                                                     Integer ancienneteContratMois,
                                                     Double creancesSuperPrivilegiees) {
        return analyze(dateJugement, montantCondamnation, executionProvisoireOrdonnee,
                situationEmployeur, dateOuvertureProcedureCollective, ancienneteContratMois,
                creancesSuperPrivilegiees, LocalDate.now());
    }

    /**
     * Analyse l'exécution forcée et détermine le verdict, l'éligibilité AGS, les
     * plafonds et la checklist.
     *
     * @param today date de référence injectée (testabilité).
     */
    public static ExecutionJugementCphResult analyze(LocalDate dateJugement,
                                                     Double montantCondamnation,
                                                     Boolean executionProvisoireOrdonnee,
                                                     ExecutionJugementCphSituationEmployeur situationEmployeur,
                                                     LocalDate dateOuvertureProcedureCollective,
                                                     Integer ancienneteContratMois,
                                                     Double creancesSuperPrivilegiees,
                                                     LocalDate today) {
        validate(dateJugement, montantCondamnation, situationEmployeur, today);

        boolean executionProvisoire = executionProvisoireOrdonnee == null || executionProvisoireOrdonnee;
        boolean procedureCollective =
                situationEmployeur == ExecutionJugementCphSituationEmployeur.REDRESSEMENT
                        || situationEmployeur == ExecutionJugementCphSituationEmployeur.LIQUIDATION;

        boolean dateOuvertureManquante = procedureCollective && dateOuvertureProcedureCollective == null;

        ExecutionJugementCphVerdict verdict;
        boolean agsEligible;
        boolean relaisAgsRecommande;
        int coefficientPlafond = 0;
        double plafondEuros = 0.0;
        double plafondMensuelSs = 0.0;

        if (!procedureCollective) {
            verdict = ExecutionJugementCphVerdict.EXECUTION_DIRECTE;
            agsEligible = false;
            relaisAgsRecommande = false;
        } else if (dateOuvertureManquante) {
            verdict = ExecutionJugementCphVerdict.BLOQUE_INFO_MANQUANTE;
            agsEligible = true;
            relaisAgsRecommande = false;
        } else {
            verdict = ExecutionJugementCphVerdict.RELAIS_AGS;
            agsEligible = true;
            relaisAgsRecommande = true;
            coefficientPlafond = AgsBareme.coefficientPlafond(ancienneteContratMois);
            plafondEuros = AgsBareme.plafondEuros(coefficientPlafond);
            plafondMensuelSs = AgsBareme.AGS_PLAFOND_MENSUEL_SS;
        }

        List<ExecutionJugementCphChecklistItem> checklist = buildChecklist(
                situationEmployeur, executionProvisoire, procedureCollective, dateOuvertureManquante);

        return new ExecutionJugementCphResult(
                dateJugement,
                montantCondamnation,
                executionProvisoire,
                situationEmployeur,
                procedureCollective ? dateOuvertureProcedureCollective : null,
                ancienneteContratMois,
                creancesSuperPrivilegiees,
                verdict,
                agsEligible,
                relaisAgsRecommande,
                coefficientPlafond,
                plafondEuros,
                plafondMensuelSs,
                checklist,
                BASE_JURIDIQUE);
    }

    private static List<ExecutionJugementCphChecklistItem> buildChecklist(
            ExecutionJugementCphSituationEmployeur situationEmployeur,
            boolean executionProvisoire,
            boolean procedureCollective,
            boolean dateOuvertureManquante) {
        List<ExecutionJugementCphChecklistItem> items = new ArrayList<>();

        // Préalable commun : signification du jugement.
        items.add(new ExecutionJugementCphChecklistItem(
                "Faire signifier le jugement à la partie adverse par commissaire de justice "
                        + "(préalable obligatoire à toute exécution forcée)",
                true,
                false,
                "art. 503 CPC ; art. 514 CPC"));

        items.add(new ExecutionJugementCphChecklistItem(
                executionProvisoire
                        ? "Exécution provisoire de droit des créances salariales acquise : "
                                + "exécuter sans attendre l'expiration des voies de recours "
                                + "(limite 9 mois de salaire moyen)"
                        : "Vérifier l'exécution provisoire : elle est de DROIT pour les créances "
                                + "salariales prud'homales (limite 9 mois de salaire moyen)",
                true,
                false,
                "art. R. 1454-28 CPC ; art. 514 CPC"));

        if (!procedureCollective) {
            // Employeur in bonis : voie d'exécution directe.
            items.add(new ExecutionJugementCphChecklistItem(
                    "Délivrer un commandement de payer valant éventuellement saisie",
                    true,
                    false,
                    "art. L. 111-2 et s. CPCE"));

            items.add(new ExecutionJugementCphChecklistItem(
                    "Mandater un commissaire de justice pour les mesures d'exécution "
                            + "(saisie-attribution, saisie-vente)",
                    true,
                    false,
                    "art. L. 122-1 CPCE"));

            items.add(new ExecutionJugementCphChecklistItem(
                    "Envisager des mesures conservatoires si un risque d'insolvabilité est "
                            + "pressenti (saisie conservatoire, sûreté judiciaire)",
                    false,
                    false,
                    "art. L. 511-1 CPCE"));
        } else {
            // Employeur en procédure collective : relais AGS / CGEA.
            items.add(new ExecutionJugementCphChecklistItem(
                    dateOuvertureManquante
                            ? "Renseigner la date d'ouverture de la procédure collective : "
                                    + "indispensable pour déterminer la période garantie et les "
                                    + "plafonds AGS (information actuellement manquante)"
                            : "Déclarer la créance salariale au mandataire judiciaire / "
                                    + "liquidateur dans les délais de la procédure collective",
                    true,
                    dateOuvertureManquante,
                    "L. 3253-15 et s. Code travail ; L. 622-24 Code de commerce"));

            items.add(new ExecutionJugementCphChecklistItem(
                    "Saisir le CGEA (AGS) pour la prise en charge des créances salariales "
                            + "garanties dans la limite des plafonds applicables",
                    true,
                    false,
                    "L. 3253-6 à L. 3253-21 Code travail"));

            items.add(new ExecutionJugementCphChecklistItem(
                    "Faire valoir le super-privilège des 60 derniers jours de salaire "
                            + "(paiement prioritaire et accéléré)",
                    false,
                    false,
                    "L. 3253-2 ; L. 3253-8 Code travail"));
        }

        return items;
    }

    private static void validate(LocalDate dateJugement,
                                 Double montantCondamnation,
                                 ExecutionJugementCphSituationEmployeur situationEmployeur,
                                 LocalDate today) {
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        if (dateJugement == null) {
            throw new IllegalArgumentException("dateJugement est requise");
        }
        if (dateJugement.isAfter(today)) {
            throw new IllegalArgumentException("dateJugement ne peut pas être dans le futur");
        }
        if (montantCondamnation == null || montantCondamnation <= 0) {
            throw new IllegalArgumentException("montantCondamnation doit être strictement positif");
        }
        if (situationEmployeur == null) {
            throw new IllegalArgumentException("situationEmployeur est requise");
        }
    }
}
