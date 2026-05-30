package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-218-03 : requête POST pour l'analyse de l'exécution forcée d'un jugement
 * CPH (art. 514 CPC ; R. 1454-28 CPC ; L. 3253-6 et s. Code travail). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * @param dateJugement date du jugement CPH à exécuter (requise, non future).
 * @param montantCondamnation montant total des condamnations en faveur du
 *        salarié (€, strictement positif, requis).
 * @param executionProvisoireOrdonnee true si l'exécution provisoire est acquise
 *        (de droit pour les créances salariales — art. R. 1454-28 CPC) —
 *        défaut true.
 * @param situationEmployeur situation de l'employeur débiteur (IN_BONIS /
 *        REDRESSEMENT / LIQUIDATION, requis).
 * @param dateOuvertureProcedureCollective date d'ouverture de la procédure
 *        collective — requise si REDRESSEMENT / LIQUIDATION, ignorée sinon.
 * @param ancienneteContratMois ancienneté du contrat (mois) à la date
 *        d'ouverture de la procédure — détermine le coefficient de plafond AGS
 *        (4 / 5 / 6 × PMSS) ; optionnel (défaut : coefficient maximal protecteur).
 * @param creancesSuperPrivilegiees montant des 60 derniers jours de salaire
 *        (super-privilège, L. 3253-8 CT) — optionnel (≥ 0).
 */
public record ExecutionJugementCphRequest(
        LocalDate dateJugement,
        Double montantCondamnation,
        Boolean executionProvisoireOrdonnee,
        ExecutionJugementCphSituationEmployeur situationEmployeur,
        LocalDate dateOuvertureProcedureCollective,
        Integer ancienneteContratMois,
        Double creancesSuperPrivilegiees
) {}
