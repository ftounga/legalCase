package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-29 : resultat brut produit par
 * {@link AtMpRenteCapitalBeChecker} - fonction <b>pure</b> (sans
 * dependance HTTP / persistance / horloge - l'horloge ne sert qu'a
 * proratiser la date de demande de conversion via
 * {@link java.time.LocalDate#now(java.time.ZoneId)} lorsque
 * {@code dateDemandeConversion} est null).
 *
 * <p>N'inclut pas le {@code caseFileId} (ajoute ensuite par
 * {@link AtMpRenteCapitalBeService} dans
 * {@link AtMpRenteCapitalBeResponse}).</p>
 *
 * <p>Outputs derives : verdict mode de versement, drapeau
 * capitalisationDoffice (IPP &lt; 19%), drapeau conversionPossible
 * (IPP &gt;= 19% + consolidation &gt;= 3 ans + demande), age a la
 * consolidation, coefficient d'age bareme AR 24/02/2005, montant
 * rente annuelle, montant capital de capitalisation (cas IPP &lt;
 * 19% ou conversion partielle 1/3 art. 45ter Loi 10/04/1971), base
 * juridique stable + avertissement avocat BE detaille.</p>
 */
public record AtMpRenteCapitalBeResult(
        // Inputs (snapshot)
        AtMpRenteCapitalBeOrigine origine,
        AtMpRenteCapitalBeStatutReconnaissance statutReconnaissance,
        LocalDate dateConsolidation,
        BigDecimal tauxIpp,
        BigDecimal remunerationBaseAnnuelle,
        LocalDate dateNaissance,
        boolean demandeConversionPartielle,
        LocalDate dateDemandeConversion,
        Integer ageConsolidationOverride,

        // Outputs analytiques
        AtMpRenteCapitalBeVerdict verdict,
        boolean capitalisationDoffice,
        boolean conversionPartiellePossible,
        Integer ageConsolidationCalcule,
        BigDecimal coefficientAge,
        BigDecimal renteAnnuelle,
        BigDecimal capitalCapitalisation,
        BigDecimal capitalConversionPartielle,
        BigDecimal renteResiduelleApresConversion,

        // Synthese + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
