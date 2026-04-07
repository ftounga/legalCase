package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

public record CalendrierGardeResponse(
        UUID caseFileId, String gardeCode, String gardeLabel, String country,
        String parentANom, String parentBNom, String repartitionType,
        List<String> semaineTypeParentA, List<String> semaineTypeParentB,
        String vacancesRegle, int joursParAnParentA, int joursParAnParentB,
        String baseJuridique, String commentaire
) {}
