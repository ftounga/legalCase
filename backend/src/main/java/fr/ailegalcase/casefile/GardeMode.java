package fr.ailegalcase.casefile;

import java.util.List;

/**
 * Mode de garde avec ses caractéristiques par pays.
 */
public record GardeMode(
        String code,
        String label,
        String country,
        String description,
        String repartitionType,
        List<String> periodesParentA,
        List<String> periodesParentB,
        String vacancesRegle,
        String baseJuridique
) {}
