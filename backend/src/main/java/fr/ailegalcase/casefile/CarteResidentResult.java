package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-214-23 : résultat de l'analyse d'éligibilité à la carte de résident 10 ans
 * L. 426-1 CESEDA. Outil single-country FR.
 */
public record CarteResidentResult(
        int dureeSejourRegulierAnnees,
        String typesTitresAnterieurs,
        String niveauIntegration,
        double ressourcesMensuellesNettes,
        boolean condamnationsPenalesGraves,
        String verdict,
        double smicMensuelNetReference,
        List<String> chipsCriteresNonRemplis,
        List<String> atouts,
        String baseJuridique
) {}
