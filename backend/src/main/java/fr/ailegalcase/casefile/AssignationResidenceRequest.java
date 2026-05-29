package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-214-35 : requête POST pour l'analyse de validité d'une mesure d'assignation
 * à résidence (L. 731-1 CESEDA). Outil single-country FR.
 *
 * @param dateNotificationAssignation date de notification de l'arrêté (requise).
 * @param dureeAssignationJours durée notifiée en jours (1..135).
 * @param motifAssignation motif de la mesure.
 * @param obligationsPresentation fréquence de pointage / obligations (optionnel).
 */
public record AssignationResidenceRequest(
        LocalDate dateNotificationAssignation,
        int dureeAssignationJours,
        AssignationResidenceMotifEnum motifAssignation,
        String obligationsPresentation
) {}
