package fr.ailegalcase.casefile;

import java.time.LocalDate;

public record AesFamilleRequest(
        LocalDate dateEntreeFrance,
        Integer dureePresenceMois,
        Boolean conjointFrancaisOuRegulier,
        Integer enfantsScolarisesFrance,
        Integer dureeScolaritePlusAncienEnfantAnnees,
        Boolean preuvesInsertion,
        Boolean menaceOrdrePublic,
        LocalDate dateDepotDemande
) {}
