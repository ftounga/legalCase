package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-214-35 : résultat interne business de l'analyse de validité d'une mesure
 * d'assignation à résidence — durée maximale de 135 jours (45 j renouvelable
 * 2 fois, L. 731-1 CESEDA) et moyens de contestation devant le TA.
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> — distinct de la rétention administrative
 * (F-IM-22) dont l'assignation à résidence est l'alternative.
 *
 * @param dureeTotaleAutoriseeJours plafond légal cumulé (135 j, L. 731-1).
 * @param dateEcheanceAssignation date de notification + durée d'assignation.
 * @param renouvellementPossible true si la durée notifiée &lt; 135 j.
 * @param motifsContestation moyens de contestation devant le TA.
 * @param recoursPossible true — recours TA toujours ouvert.
 * @param delaiRecoursTaHeures délai de saisine du TA (48 h).
 */
public record AssignationResidenceResult(
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
        String baseJuridique
) {}
