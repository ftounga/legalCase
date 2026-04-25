package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-DT-13-01 : résultat structuré du calcul de risque de requalification d'un licenciement
 * économique (FR — art. L.1233-3 + L.1233-4 + L.1233-5 + L.1233-45).
 */
public record LicenciementEconomiqueResult(
        LicenciementEconomiqueCalculator.MotifEconomique motifEconomiqueInvoque,
        List<LicenciementEconomiqueCalculator.PreuveMotif> preuvesMotif,
        List<LicenciementEconomiqueCalculator.CritereOrdre> criteresOrdreAppliques,
        int salarieAge,
        int salarieAncienneteMois,
        int salarieChargesFamille,
        LicenciementEconomiqueCalculator.QualitesProf salarieQualitesProf,
        List<LicenciementEconomiqueCalculator.TentativeReclassement> tentativesReclassement,
        boolean prioriteReembauchePropose,
        boolean congeReclassementPropose,
        LocalDate dateNotification,
        String country,
        int scoreCausalite,
        int scoreCriteresOrdre,
        int scoreReclassement,
        int scoreGlobal,
        LicenciementEconomiqueCalculator.VerdictRisque verdictRisqueRequalification,
        List<LicenciementEconomiqueCalculator.CritereOrdre> criteresOrdreManquants,
        boolean criteresOrdreObligatoiresOk,
        boolean obligationReclassementRespectee,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
