package fr.ailegalcase.casefile;

import java.time.LocalDate;

public record Belgian9bisRequest(
        LocalDate dateEntreeBelgique,
        Integer dureePresenceMois,
        Boolean circonstancesExceptionnelles,
        Boolean liensFamiliauxBe,
        Boolean liensProfessionnels,
        Boolean scolariteEnfantsBe,
        Boolean menaceOrdrePublic,
        LocalDate dateDepotDemande
) {}
