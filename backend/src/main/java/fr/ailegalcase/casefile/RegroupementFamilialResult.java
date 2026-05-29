package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-214-03 : résultat de l'analyse d'éligibilité au regroupement familial
 * L. 434-1+ CESEDA. Outil single-country FR.
 */
public record RegroupementFamilialResult(
        int dureeSejourRegulierMois,
        double ressourcesMensuellesNettes,
        int tailleLogementM2,
        int nombrePersonnesFoyer,
        String typeRegroupement,
        int membresFamilleARegrouper,
        String verdict,
        double ressourcesRequises,
        int surfaceRequise,
        List<String> chipsCriteresNonRemplis,
        String baseJuridique
) {}
