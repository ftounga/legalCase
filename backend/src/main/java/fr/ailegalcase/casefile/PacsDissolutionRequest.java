package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.PacsDissolutionCalculator.CreanceType;
import fr.ailegalcase.casefile.PacsDissolutionCalculator.ModeDissolution;
import fr.ailegalcase.casefile.PacsDissolutionCalculator.RegimeBiens;

import java.time.LocalDate;
import java.util.List;

public record PacsDissolutionRequest(
        LocalDate dateConclusionPacs,
        ModeDissolution modeDissolution,
        LocalDate dateDissolution,
        Integer dureeUnionAnnees,
        RegimeBiens regimeBiens,
        Boolean patrimoineCommunSignificatif,
        List<CreanceType> creancesAlleguees,
        Integer enfantsCommuns,
        LocalDate dateNotificationPartenaire
) {}
