package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-214-09 : réponse de l'analyse de la catégorie d'OQTF L. 611-1 CESEDA.
 * Outil single-country FR.
 */
public record OqtfCategoriesResponse(
        UUID caseFileId,
        OqtfCategorieL611 categorieL611,
        String categorieLibelle,
        LocalDate dateNotificationOqtf,
        String motifOqtf,
        String country,
        String baseJuridique,
        List<String> moyensDefense,
        String delaiRecours,
        Integer delaiRecoursJours,
        Integer delaiRecoursHeures,
        String procedureParallele
) {}
