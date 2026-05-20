package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-217-11 : requête HTTP pour l'endpoint d'analyse de dévolution / réserve
 * héréditaire belge. Le pays est dérivé du workspace côté service.
 */
public record SuccessionBeDevolutionReserveRequest(
        LocalDate dateDeces,
        SuccessionBeDevolutionReserveCalculator.EtatCivilDefuntBe etatCivilDefunt,
        SuccessionBeDevolutionReserveCalculator.RegimeMatrimonialDefuntBe regimeMatrimonialDefunt,
        Integer nombreEnfantsVivants,
        Integer nombreEnfantsPredecedesAvecDescendants,
        Boolean presenceParentsVivants,
        Boolean presenceFreresSoeursOuDescendants,
        BigDecimal masseSuccessoraleEur,
        BigDecimal libertesConsentiesEur,
        String commentaire
) {

    SuccessionBeDevolutionReserveInput toInput() {
        return new SuccessionBeDevolutionReserveInput(
                dateDeces,
                etatCivilDefunt,
                regimeMatrimonialDefunt,
                nombreEnfantsVivants,
                nombreEnfantsPredecedesAvecDescendants,
                presenceParentsVivants,
                presenceFreresSoeursOuDescendants,
                masseSuccessoraleEur,
                libertesConsentiesEur,
                commentaire
        );
    }
}
