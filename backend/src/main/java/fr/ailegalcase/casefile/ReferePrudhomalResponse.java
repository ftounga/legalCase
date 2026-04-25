package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReferePrudhomalResponse(
        UUID caseFileId,
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
        List<String> messages,
        String country
) {}
