package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-214-25 : requête POST pour l'analyse des démarches ANEF (administration
 * numérique des étrangers en France) et des recours en cas de panne du dépôt
 * dématérialisé. Outil single-country FR.
 *
 * @param typeTitreConcerne       type de titre concerné par la démarche (libre).
 * @param dateExpirationTitre     date d'expiration du titre en cours (requise) —
 *        pivot du calcul d'urgence (< 30 j ⇒ URGENT).
 * @param panneeANEFSignalee      true si une panne / indisponibilité de la
 *        plateforme ANEF est signalée par le demandeur.
 * @param dateTentativeDepot      date de la tentative de dépôt dématérialisé
 *        ayant échoué (optionnelle).
 * @param demandeAdresseePrefecture true si une demande a déjà été adressée à la
 *        préfecture (LRAR / dépôt physique) — bascule en RECOURS_POSSIBLE.
 */
public record AnefProcedureRequest(
        String typeTitreConcerne,
        LocalDate dateExpirationTitre,
        Boolean panneeANEFSignalee,
        LocalDate dateTentativeDepot,
        Boolean demandeAdresseePrefecture
) {}
