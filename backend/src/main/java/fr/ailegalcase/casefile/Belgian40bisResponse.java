package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record Belgian40bisResponse(
        UUID caseFileId,
        String lienFamilial,
        boolean regroupantCitoyenUe,
        String regroupantActiviteCategorie,
        boolean ressourcesSuffisantes,
        boolean assuranceMaladieUe,
        boolean logementSuffisant,
        boolean menaceOrdrePublic,
        LocalDate dateDepotDemande,
        String country,
        boolean lienValide,
        boolean regroupantValide,
        boolean ressourcesOk,
        boolean assuranceOk,
        boolean logementOk,
        boolean pasMenace,
        int scoreGlobal,
        String verdictProbabilite,
        List<String> criteresNonRemplis,
        LocalDate dateExpirationInstruction,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
