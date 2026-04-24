package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record Annexe13BeResponse(
        UUID caseFileId,
        LocalDate dateNotificationAnnexe13,
        int delaiDepartImposeJours,
        String motifOqt,
        boolean transfertImminent,
        boolean recoursForme,
        LocalDate dateRecours,
        String typeRecours,
        String country,
        LocalDate dateExpirationDelaiDepart,
        LocalDate dateExpirationRecoursAnnulation,
        LocalDate dateExpirationRecoursExtremeUrgence,
        long joursRestantsAvantExpirationAnnulation,
        String statutRecoursAnnulation,
        LocalDate dateAudiencePrevisionnelle,
        LocalDate dateDecisionPrevisionnelle,
        List<String> referedDisponibles,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
