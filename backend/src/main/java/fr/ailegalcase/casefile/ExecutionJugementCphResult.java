package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-218-03 : résultat interne business de l'analyse de l'exécution forcée d'un
 * jugement du Conseil de prud'hommes (CPH). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Produit la checklist des démarches d'exécution (signification préalable,
 * exécution provisoire de droit des créances salariales art. R. 1454-28 / 514
 * CPC, commandement de payer, mandatement huissier, mesures conservatoires) et
 * détecte l'éligibilité à la garantie AGS lorsque l'employeur est en
 * redressement ou liquidation judiciaire (L. 3253-6 et s. Code travail).
 *
 * @param dateJugement date du jugement CPH à exécuter.
 * @param montantCondamnation montant total des condamnations (€).
 * @param executionProvisoireOrdonnee true si l'exécution provisoire est acquise.
 * @param situationEmployeur situation de l'employeur débiteur.
 * @param dateOuvertureProcedureCollective date d'ouverture de la procédure
 *        collective (null si IN_BONIS).
 * @param ancienneteContratMois ancienneté du contrat (mois) — null si non fournie.
 * @param creancesSuperPrivilegiees montant super-privilégié (null si non fourni).
 * @param verdict orientation de l'exécution.
 * @param agsEligible true si la garantie AGS est mobilisable.
 * @param relaisAgsRecommande true si le relais AGS (déclaration de créance,
 *        saisine CGEA) est recommandé.
 * @param agsCoefficientPlafond coefficient de plafond AGS retenu (4 / 5 / 6),
 *        0 si non applicable.
 * @param agsPlafondEuros plafond AGS en euros (coefficient × PMSS), 0 si non
 *        applicable.
 * @param agsPlafondMensuelSs valeur du PMSS retenue (à actualiser annuellement),
 *        0 si non applicable.
 * @param checklist checklist des démarches d'exécution.
 * @param baseJuridique fondements juridiques applicables.
 */
public record ExecutionJugementCphResult(
        LocalDate dateJugement,
        double montantCondamnation,
        boolean executionProvisoireOrdonnee,
        ExecutionJugementCphSituationEmployeur situationEmployeur,
        LocalDate dateOuvertureProcedureCollective,
        Integer ancienneteContratMois,
        Double creancesSuperPrivilegiees,
        ExecutionJugementCphVerdict verdict,
        boolean agsEligible,
        boolean relaisAgsRecommande,
        int agsCoefficientPlafond,
        double agsPlafondEuros,
        double agsPlafondMensuelSs,
        List<ExecutionJugementCphChecklistItem> checklist,
        String baseJuridique
) {}
