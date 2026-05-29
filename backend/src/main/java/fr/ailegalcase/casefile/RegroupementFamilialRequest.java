package fr.ailegalcase.casefile;

/**
 * SF-214-03 : requête POST pour l'analyse d'éligibilité au regroupement familial
 * L. 434-1+ CESEDA. Outil single-country FR.
 */
public record RegroupementFamilialRequest(
        Integer dureeSejourRegulierMois,
        Double ressourcesMensuellesNettes,
        Integer tailleLogementM2,
        Integer nombrePersonnesFoyer,
        String typeRegroupement,
        Integer membresFamilleARegrouper
) {}
