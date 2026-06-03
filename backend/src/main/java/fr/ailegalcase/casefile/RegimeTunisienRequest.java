package fr.ailegalcase.casefile;

/**
 * SF-220-01 : requête POST pour l'outil décisionnel "Régime franco-tunisien
 * (accord du 17/03/1988)" (F-IM-47-regime-tunisien-fr). Outil single-country FR.
 */
public record RegimeTunisienRequest(
        String categorie,
        Integer dureeSejourEnvisageeMois,
        Boolean titreEnCours,
        Boolean dejaResident
) {}
