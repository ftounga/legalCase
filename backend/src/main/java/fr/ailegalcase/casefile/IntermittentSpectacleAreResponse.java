package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-218-17 : réponse de l'analyse d'ouverture des droits à l'ARE de
 * l'intermittent du spectacle (annexes 8 et 10 Unedic — seuil 507 h / 12 mois,
 * F-DT-106). Outil <b>FRANCE UNIQUEMENT</b>.
 */
public record IntermittentSpectacleAreResponse(
        UUID caseFileId,
        IntermittentSpectacleAnnexe annexe,
        LocalDate dateFinContrat,
        int heuresTravaillees12Mois,
        int heuresCachets,
        int heuresFormationRetenues,
        int heuresTotalesRetenues,
        int seuilHeures,
        int heuresManquantes,
        int heuresExcedentaires,
        LocalDate dateProchainExamen,
        IntermittentSpectacleAreVerdict ouvertureDroits,
        IntermittentSpectacleAreStatut statut,
        String country,
        String baseJuridique
) {}
