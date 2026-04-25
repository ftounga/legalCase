package fr.ailegalcase.casefile;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/**
 * SF-DT-30-01 : requête d'analyse de protection des représentants du personnel (FR).
 *
 * <p>Le pays n'est pas transmis dans le body — il est dérivé de
 * {@code caseFile.getWorkspace().getCountry()} côté service.</p>
 */
public record ProtectionRpRequest(
        ProtectionRpCalculator.StatutProtege statutProtege,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateExpirationMandat,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate datePresumeeRupture,
        ProtectionRpCalculator.ProcedureSuivie procedureSuivie,
        ProtectionRpCalculator.MotifLicenciement motifLicenciement,
        Double salaireMensuelBrutEur
) {}
