package fr.ailegalcase.casefile;

import java.time.LocalDate;

public record PacteSuccessoralBe2018Request(
        LocalDate dateSignaturePacte,
        Boolean acteAuthentique,
        Boolean accordTousHeritiersReservataires,
        Boolean equilibreDonationsRapportables,
        Boolean presenceTousHeritiersReservataires
) {}
