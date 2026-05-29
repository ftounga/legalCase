package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-214-23 : réponse de l'analyse d'éligibilité à la carte de résident 10 ans
 * L. 426-1 CESEDA. Outil single-country FR.
 */
public record CarteResidentResponse(
        UUID caseFileId,
        int dureeSejourRegulierAnnees,
        String typesTitresAnterieurs,
        String niveauIntegration,
        double ressourcesMensuellesNettes,
        boolean condamnationsPenalesGraves,
        String country,
        String verdict,
        double smicMensuelNetReference,
        List<String> chipsCriteresNonRemplis,
        List<String> atouts,
        String baseJuridique
) {}
