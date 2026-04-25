package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-DT-32-01 : requête HTTP pour l'endpoint de conformité documents de fin de contrat.
 *
 * <p>Le pays est dérivé du workspace côté service.</p>
 */
public record DocumentsFinContratRequest(
        LocalDate dateFinContrat,
        Boolean certificatTravailRemis,
        LocalDate dateCertificatTravail,
        Boolean attestationFranceTravailRemise,
        LocalDate dateAttestationFranceTravail,
        Boolean souldeToutCompteSigne,
        LocalDate dateSouldeToutCompte,
        BigDecimal salaireMensuelBrutEur
) {

    DocumentsFinContratInput toInput() {
        return new DocumentsFinContratInput(
                dateFinContrat,
                certificatTravailRemis,
                dateCertificatTravail,
                attestationFranceTravailRemise,
                dateAttestationFranceTravail,
                souldeToutCompteSigne,
                dateSouldeToutCompte,
                salaireMensuelBrutEur,
                null
        );
    }
}
