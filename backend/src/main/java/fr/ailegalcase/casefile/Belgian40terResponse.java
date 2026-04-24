package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record Belgian40terResponse(
        UUID caseFileId,
        String lienFamilial,
        boolean regroupantBelge,
        BigDecimal revenusMensuelsNetsEur,
        BigDecimal seuil120PctRisEur,
        boolean assuranceMaladie,
        boolean logementSuffisant,
        boolean menaceOrdrePublic,
        LocalDate dateDepotDemande,
        String country,
        boolean lienValide,
        boolean regroupantBelgeOk,
        boolean revenusSuffisantsOk,
        boolean assuranceOk,
        boolean logementOk,
        boolean pasMenace,
        BigDecimal differentielRevenus,
        int scoreGlobal,
        String verdictProbabiliteAcceptation,
        List<String> criteresNonRemplis,
        LocalDate dateExpirationInstructionSiDemande,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
