package fr.ailegalcase.casefile;

/**
 * SF-214-39 : requête POST pour l'analyse du droit au séjour UE/EEE/Suisse en
 * France. Outil single-country FR.
 *
 * <p>{@code nationalite} est optionnelle (peut être null).</p>
 */
public record UeEeeSuisseSejourRequest(
        String nationalite,
        Boolean estCitoyenUE,
        Boolean membreFamilleNonUE,
        Integer dureeSejourMois,
        String activiteProfessionnelle
) {}
