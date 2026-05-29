package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-214-43 : requête POST pour l'analyse des obligations de l'employeur recrutant
 * un travailleur étranger hors UE (autorisation de travail, L. 5221-1 Code du
 * travail). Outil single-country FR.
 *
 * @param typeContrat type de contrat proposé (CDI / CDD / INTERIM).
 * @param posteProposes intitulé du poste proposé (≤ 200 caractères).
 * @param nationaliteCandidat nationalité du candidat (détermine si une autorisation
 *        de travail préalable est requise).
 * @param dureeContratMois durée du contrat en mois — optionnel (sans objet pour un CDI).
 * @param refusAutorisation indique qu'un refus d'autorisation de travail a été notifié.
 * @param dateRefusAutorisation date de notification du refus — base du délai de recours TA.
 */
public record AutorisationTravailEmployeurRequest(
        AutorisationTravailEmployeurTypeContrat typeContrat,
        String posteProposes,
        String nationaliteCandidat,
        Integer dureeContratMois,
        boolean refusAutorisation,
        LocalDate dateRefusAutorisation
) {}
