package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-220-06 : requête POST pour l'outil décisionnel « contestation / radiation
 * d'un signalement SIS aux fins de non-admission »
 * (F-IM-52-signalement-sis-fr, Règl. UE 2018/1860 / CESEDA L.312-3).
 * Outil single-country FR.
 *
 * <p>{@code etatSignalant} et {@code motifSignalement} sont des codes enum
 * validés en amont (whitelist) :
 * <ul>
 *   <li>{@code etatSignalant} : FRANCE / AUTRE_ETAT_MEMBRE / INCONNU</li>
 *   <li>{@code motifSignalement} : IRTF / MESURE_ELOIGNEMENT_ETRANGERE /
 *       MENACE_ORDRE_PUBLIC / AUTRE</li>
 * </ul>
 * </p>
 */
public record SignalementSisRequest(
        Boolean signalementConnu,
        String etatSignalant,
        String motifSignalement,
        Boolean titreSejourValide,
        LocalDate dateSignalement
) {}
