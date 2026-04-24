package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Belgian40terRequest(
        String lienFamilial,
        Boolean regroupantBelge,
        BigDecimal revenusMensuelsNetsEur,
        BigDecimal seuil120PctRisEur,
        Boolean assuranceMaladie,
        Boolean logementSuffisant,
        Boolean menaceOrdrePublic,
        LocalDate dateDepotDemande
) {}
