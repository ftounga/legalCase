package fr.ailegalcase.casefile;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-DT-13-01 : requête d'analyse de risque de requalification d'un licenciement économique.
 *
 * <p>Le pays n'est pas transmis dans le body — il est dérivé de
 * {@code caseFile.getWorkspace().getCountry()} côté service.</p>
 */
public record LicenciementEconomiqueRequest(
        LicenciementEconomiqueCalculator.MotifEconomique motifEconomiqueInvoque,
        List<LicenciementEconomiqueCalculator.PreuveMotif> preuvesMotif,
        List<LicenciementEconomiqueCalculator.CritereOrdre> criteresOrdreAppliques,
        Integer salarieAge,
        Integer salarieAncienneteMois,
        Integer salarieChargesFamille,
        LicenciementEconomiqueCalculator.QualitesProf salarieQualitesProf,
        List<LicenciementEconomiqueCalculator.TentativeReclassement> tentativesReclassement,
        Boolean prioriteReembauchePropose,
        Boolean congeReclassementPropose,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateNotification
) {}
