package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-207-01 : projection complète (snapshot) renvoyée au client pour l'outil
 * de prescription Travail BE.
 */
public record PrescriptionBeLitigeTravailResponse(
        UUID caseFileId,
        // Inputs normalisés (pré-fill UI)
        LocalDate dateRupture,
        String typeCreance,
        LocalDate dateActionEnvisagee,
        // Outputs calculés
        String verdict,
        LocalDate dateLimitePrescription,
        long joursRestants,
        String regleAppliquee,
        String baseJuridique,
        String formuleCalcul,
        // Workspace context (toujours BELGIQUE pour cet outil)
        String country
) {}
