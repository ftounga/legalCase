package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-214-09 : résultat de l'analyse de la catégorie d'OQTF L. 611-1 CESEDA.
 * Outil single-country FR.
 */
public record OqtfCategoriesResult(
        OqtfCategorieL611 categorieL611,
        String categorieLibelle,
        LocalDate dateNotificationOqtf,
        String motifOqtf,
        String baseJuridique,
        List<String> moyensDefense,
        String delaiRecours,
        Integer delaiRecoursJours,
        Integer delaiRecoursHeures,
        String procedureParallele
) {}
