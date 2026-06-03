package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-220-06 : résultat de l'analyse de contestation / radiation d'un signalement
 * SIS aux fins de non-admission (Règl. UE 2018/1860 / CESEDA L.312-3,
 * F-IM-52-signalement-sis-fr). Outil single-country FR.
 */
public record SignalementSisResult(
        Boolean signalementConnu,
        String etatSignalant,
        String motifSignalement,
        Boolean titreSejourValide,
        String actionPossible,
        List<String> demarches,
        String autoriteCompetente,
        List<String> basesJuridiques,
        List<String> messages
) {}
