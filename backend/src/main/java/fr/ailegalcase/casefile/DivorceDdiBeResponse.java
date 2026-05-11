package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DivorceDdiBeResponse(
        UUID caseFileId,
        String country,
        LocalDate dateSeparation,
        String natureDemande,
        long joursSeparation,
        boolean preuvesDesunionDisponibles,
        boolean voie1Ouverte,
        boolean voie2Ouverte,
        boolean voie3Ouverte,
        String voieRecommandee,
        long joursRestantsVoie2,
        long joursRestantsVoie3,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
