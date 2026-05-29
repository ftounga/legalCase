package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-214-39 : réponse de l'analyse du droit au séjour UE/EEE/Suisse en France.
 * Outil single-country FR.
 */
public record UeEeeSuisseSejourResponse(
        UUID caseFileId,
        String nationalite,
        boolean estCitoyenUE,
        boolean membreFamilleNonUE,
        int dureeSejourMois,
        String activiteProfessionnelle,
        String country,
        boolean droitSejourAutomatique3Mois,
        boolean droitSejourPlus5Ans,
        String titreObtenu,
        List<String> conditionsRespectees,
        String situationMembreNonUE,
        String baseJuridique
) {}
