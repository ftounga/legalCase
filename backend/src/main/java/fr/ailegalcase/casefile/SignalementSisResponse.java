package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-220-06 : réponse de l'analyse de contestation / radiation d'un signalement
 * SIS aux fins de non-admission (Règl. UE 2018/1860 / CESEDA L.312-3,
 * F-IM-52-signalement-sis-fr). Outil single-country FR.
 */
public record SignalementSisResponse(
        UUID caseFileId,
        Boolean signalementConnu,
        String etatSignalant,
        String motifSignalement,
        Boolean titreSejourValide,
        LocalDate dateSignalement,
        String country,
        String actionPossible,
        List<String> demarches,
        String autoriteCompetente,
        List<String> basesJuridiques,
        List<String> messages
) {}
