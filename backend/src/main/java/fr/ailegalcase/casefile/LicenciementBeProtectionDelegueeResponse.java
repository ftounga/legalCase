package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-213-08 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-protection-deleguee}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les autres
 * outils décisionnels BE de la F-213 (miroir
 * {@link HarcelementBeProcedureFormelleResponse} SF-213-07).</p>
 */
public record LicenciementBeProtectionDelegueeResponse(
        UUID caseFileId,

        LicenciementBeStatutProtegeEnum statutProtege,
        Integer ancienneteAnnees,
        BigDecimal remunerationAnnuelleBrute,
        LocalDate dateLicenciement,
        LocalDate dateElectionOuMandat,
        boolean demandeReintegrationDansTrente,
        boolean employeurRefuseReintegration,
        boolean circonstancesAggravantes,

        LicenciementBeProtectionDelegueeVerdict verdict,
        boolean licenciementDansProtection,
        LocalDate dateFinProtection,
        BigDecimal indemniteForfaitaire,
        Integer anneesForfait,
        LocalDate dateLimiteDemandeReintegration,
        Integer joursRestantsReintegration,
        boolean delaiReintegrationDepasse,
        String baseJuridique,
        String avertissement
) {
}
