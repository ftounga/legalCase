package fr.ailegalcase.casefile;

import java.time.LocalDate;

public record Belgian40bisRequest(
        String lienFamilial,
        Boolean regroupantCitoyenUe,
        String regroupantActiviteCategorie,
        Boolean ressourcesSuffisantes,
        Boolean assuranceMaladieUe,
        Boolean logementSuffisant,
        Boolean menaceOrdrePublic,
        LocalDate dateDepotDemande
) {}
