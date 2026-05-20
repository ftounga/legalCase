package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-207-04 : projection complète (snapshot) renvoyée au client pour l'outil
 * de déclaration AT Fedris.
 */
public record AtFedrisDeclarationResponse(
        UUID caseFileId,
        // Inputs normalisés (pré-fill UI)
        LocalDate dateAccident,
        LocalDate dateConnaissanceEmployeur,
        LocalDate dateActionEnvisagee,
        LocalDate dateDeclarationEffectuee,
        // Outputs calculés
        String verdict,
        LocalDate dateLimiteDeclaration,
        long joursRestants,
        String regleAppliquee,
        String baseJuridique,
        String formuleCalcul,
        String consequencesNonRespect,
        // Workspace context (toujours BELGIQUE pour cet outil)
        String country
) {}
