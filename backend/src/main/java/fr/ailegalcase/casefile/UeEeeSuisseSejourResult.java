package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-214-39 : résultat de l'analyse du droit au séjour UE/EEE/Suisse en France.
 * Outil single-country FR.
 */
public record UeEeeSuisseSejourResult(
        String nationalite,
        boolean estCitoyenUE,
        boolean membreFamilleNonUE,
        int dureeSejourMois,
        String activiteProfessionnelle,
        boolean droitSejourAutomatique3Mois,
        boolean droitSejourPlus5Ans,
        String titreObtenu,
        List<String> conditionsRespectees,
        String situationMembreNonUE,
        String baseJuridique
) {}
