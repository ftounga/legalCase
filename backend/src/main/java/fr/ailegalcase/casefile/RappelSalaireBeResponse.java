package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-213-02 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/rappel-salaire-be}.
 */
public record RappelSalaireBeResponse(
        UUID caseFileId,
        BigDecimal montantBrut,
        LocalDate dateDebutPeriode,
        LocalDate dateFinPeriode,
        LocalDate dateRupture,
        LocalDate dateActionEnvisagee,
        RappelSalaireBeTypeArriereEnum typeArriere,
        BigDecimal interetsCourus,
        String tauxMoratoire,
        BigDecimal totalReclame,
        LocalDate dateLimitePrescription,
        long joursRestantsAvantPrescription,
        RappelSalaireBeStatutPrescription statutPrescription,
        String baseJuridique,
        String formuleCalcul,
        String avertissement
) {}
