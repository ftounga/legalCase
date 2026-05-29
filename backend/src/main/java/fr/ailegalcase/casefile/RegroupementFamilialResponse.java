package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-214-03 : réponse de l'analyse d'éligibilité au regroupement familial
 * L. 434-1+ CESEDA.
 */
public record RegroupementFamilialResponse(
        UUID caseFileId,
        int dureeSejourRegulierMois,
        double ressourcesMensuellesNettes,
        int tailleLogementM2,
        int nombrePersonnesFoyer,
        String typeRegroupement,
        int membresFamilleARegrouper,
        String country,
        String verdict,
        double ressourcesRequises,
        int surfaceRequise,
        List<String> chipsCriteresNonRemplis,
        String baseJuridique
) {}
