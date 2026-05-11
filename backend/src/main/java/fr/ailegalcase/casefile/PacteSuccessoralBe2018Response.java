package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PacteSuccessoralBe2018Response(
        UUID caseFileId,
        String country,
        LocalDate dateSignaturePacte,
        boolean acteAuthentique,
        boolean accordTousHeritiersReservataires,
        boolean equilibreDonationsRapportables,
        boolean presenceTousHeritiersReservataires,
        String verdict,
        List<String> motifsAnnulation,
        List<String> motifsContestabilite,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
