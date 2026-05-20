package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-217-11 : input du calcul de dévolution / réserve héréditaire belge.
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis ici.</p>
 */
public record SuccessionBeDevolutionReserveInput(
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
) {}
