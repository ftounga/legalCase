package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-220-02 : requête POST pour l'outil décisionnel "Portée territoriale du titre
 * à Mayotte" (F-IM-48-regime-mayotte-fr). Outil single-country FR.
 */
public record RegimeMayotteRequest(
        Boolean titreDelivreAMayotte,
        String typeTitre,
        Boolean projetDeplacementMetropole,
        LocalDate dateDelivrance
) {}
