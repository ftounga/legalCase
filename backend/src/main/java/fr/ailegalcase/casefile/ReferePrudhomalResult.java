package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-DT-34-01 : résultat de l'analyse du référé prud'homal FR (R.1454-1 et s. CT).
 */
public record ReferePrudhomalResult(
        String typeRefere,
        String natureCreance,
        BigDecimal montantProvisionDemandeeEur,
        boolean absenceContestationSerieuse,
        List<String> preuvesUrgenceProduites,
        boolean dommageImmediatCarac,
        boolean tresorerieEmployeurDouteuse,
        LocalDate dateMiseEnDemeure,
        Integer ancienneteContratMois,
        int scoreSuccess,
        String verdictRecommandation,
        int delaiAudienceJoursPrevisionnel,
        int delaiOrdonnanceJoursPrevisionnel,
        BigDecimal montantProvisionRecommandeEur,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
