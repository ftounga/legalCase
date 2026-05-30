package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-218-21 : réponse de l'analyse du régime du stagiaire (gratification
 * minimale + requalification en CDI, art. L.124-1 et s. du code de l'éducation,
 * F-DT-109). Outil <b>FRANCE UNIQUEMENT</b>.
 */
public record StagiaireGratificationResponse(
        UUID caseFileId,
        LocalDate dateDebutStage,
        LocalDate dateFinStage,
        int nombreJoursPresence,
        BigDecimal heuresPresence,
        long dureeStageJours,
        boolean seuilAtteint,
        boolean gratificationObligatoire,
        BigDecimal tauxHoraireApplique,
        BigDecimal gratificationMinimaleDue,
        BigDecimal gratificationVerseeTotale,
        BigDecimal rappelGratification,
        boolean depassementDureeMax,
        boolean missionsHorsProjetPedagogique,
        boolean posteTravailPermanent,
        StagiaireRisqueRequalification risqueRequalification,
        List<String> motifs,
        StagiaireGratificationVerdict verdictGlobal,
        String country,
        String baseJuridique
) {}
