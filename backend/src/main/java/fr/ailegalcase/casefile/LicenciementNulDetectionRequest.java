package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-DT-16-01 : requête HTTP pour l'endpoint de détection licenciement nul.
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record LicenciementNulDetectionRequest(
        LocalDate dateNotificationLicenciement,
        Boolean salarieEnceinte,
        LocalDate dateAccouchement,
        Boolean salarieAccidentTravail,
        LocalDate dateConsolidationAT,
        Boolean salarieHarceleAvere,
        Boolean salarieDiscriminationAlleguee,
        Boolean salarieMotifLanceurAlerte,
        Boolean salarieMandatRepresentant,
        Boolean salarieActionJustice,
        BigDecimal salaireMensuelBrutEur,
        Integer ancienneteAnnees
) {

    LicenciementNulDetectionInput toInput() {
        return new LicenciementNulDetectionInput(
                dateNotificationLicenciement,
                salarieEnceinte,
                dateAccouchement,
                salarieAccidentTravail,
                dateConsolidationAT,
                salarieHarceleAvere,
                salarieDiscriminationAlleguee,
                salarieMotifLanceurAlerte,
                salarieMandatRepresentant,
                salarieActionJustice,
                salaireMensuelBrutEur,
                ancienneteAnnees
        );
    }
}
