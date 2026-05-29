package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-214-35 : réponse de l'analyse de validité d'une mesure d'assignation à
 * résidence (L. 731-1 CESEDA). Outil single-country FR.
 */
public record AssignationResidenceResponse(
        UUID caseFileId,
        LocalDate dateNotificationAssignation,
        int dureeAssignationJours,
        AssignationResidenceMotifEnum motifAssignation,
        String obligationsPresentation,
        int dureeTotaleAutoriseeJours,
        LocalDate dateEcheanceAssignation,
        boolean renouvellementPossible,
        List<String> motifsContestation,
        boolean recoursPossible,
        int delaiRecoursTaHeures,
        AssignationResidenceStatut statut,
        String recommandation,
        String country,
        String baseJuridique
) {}
