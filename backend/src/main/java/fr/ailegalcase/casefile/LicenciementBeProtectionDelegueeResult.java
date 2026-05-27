package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-213-08 : résultat brut produit par
 * {@link LicenciementBeProtectionDelegueeValidator} — fonction <b>pure</b>.
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link LicenciementBeProtectionDelegueeService} dans
 * {@link LicenciementBeProtectionDelegueeResponse}).</p>
 *
 * <p>{@code indemniteForfaitaire} et {@code anneesForfait} sont {@code null}
 * lorsque le verdict est {@link
 * LicenciementBeProtectionDelegueeVerdict#HORS_PERIODE_PROTECTION}.
 * {@code avertissement} est {@code null} si aucune alerte n'est levée.</p>
 */
public record LicenciementBeProtectionDelegueeResult(
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
