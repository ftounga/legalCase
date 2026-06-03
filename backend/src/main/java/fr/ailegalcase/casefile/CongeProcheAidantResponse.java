package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * SF-218-47 : réponse de l'analyse du congé de proche aidant (art. L.3142-16 à
 * L.3142-27 CT, F-DT-79). Outil <b>FRANCE UNIQUEMENT</b>.
 */
public record CongeProcheAidantResponse(
        UUID caseFileId,
        CongeProcheAidantStatut statut,
        CongeProcheAidantLien lienPersonneAidee,
        boolean personneAideeResideFrance,
        int dureeSouhaiteeMois,
        int dureeMaxMois,
        Integer dureeRetenueMois,
        boolean ajpaDemandee,
        BigDecimal ajpaJournaliere,
        BigDecimal estimationAjpa,
        boolean protectionEmploi,
        boolean nonImputableCongesPayes,
        List<String> notes,
        String country,
        String baseJuridique
) {}
