package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReferePrudhomalRequest(
        String typeRefere,
        String natureCreance,
        BigDecimal montantProvisionDemandeeEur,
        Boolean absenceContestationSerieuse,
        List<String> preuvesUrgenceProduites,
        Boolean dommageImmediatCarac,
        Boolean tresorerieEmployeurDouteuse,
        LocalDate dateMiseEnDemeure,
        Integer ancienneteContratMois
) {}
