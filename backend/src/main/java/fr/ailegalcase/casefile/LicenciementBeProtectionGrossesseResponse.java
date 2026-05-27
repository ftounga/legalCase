package fr.ailegalcase.casefile;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-213-05 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-protection-grossesse}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les autres
 * outils décisionnels BE (miroir
 * {@link LicenciementBeFormuleClaeysResponse} SF-213-04).</p>
 */
public record LicenciementBeProtectionGrossesseResponse(
        UUID caseFileId,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateDebutGrossesse,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateAccouchement,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateCongeMaterniteDebut,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateCongeMaterniteFinale,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateLicenciement,

        boolean grossesseNotifieeParEcrit,
        BigDecimal remunerationMensuelleBrute,
        String motifInvoqueParEmployeur,

        LicenciementBeProtectionGrossesseVerdict verdict,
        boolean licenciementDansLaPeriodeProtegee,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateDebutProtection,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateFinProtection,

        BigDecimal indemniteForfaitaire,
        boolean chargePreuveEmployeur,
        String baseJuridique,
        String formuleCalcul,
        String avertissement
) {}
