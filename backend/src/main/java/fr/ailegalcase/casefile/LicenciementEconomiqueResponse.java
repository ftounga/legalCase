package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-DT-13-01 : réponse de l'endpoint {@code /licenciement-economique}.
 */
public record LicenciementEconomiqueResponse(
        UUID caseFileId,
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
        List<String> messages,
        String country
) {}
