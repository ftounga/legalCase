package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-214-09 : requête POST pour l'analyse de la catégorie d'OQTF L. 611-1
 * CESEDA. Outil single-country FR.
 */
public record OqtfCategoriesRequest(
        OqtfCategorieL611 categorieL611,
        LocalDate dateNotificationOqtf,
        String motifOqtf
) {}
