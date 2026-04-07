package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

public record ImmigrationWorkRightResponse(
        UUID caseFileId,
        String titreType,
        String titreLabel,
        String country,
        String droitTravail,
        String conditions,
        List<String> obligationsEmployeur,
        String baseJuridique
) {}
