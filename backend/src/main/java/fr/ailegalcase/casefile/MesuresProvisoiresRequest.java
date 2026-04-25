package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-12-01 : requête de calcul des mesures provisoires (AOMP, art. 254 Cciv).
 */
public record MesuresProvisoiresRequest(
        LocalDate dateAudienceAOMP,
        BigDecimal revenusEpouxDemandeurEur,
        BigDecimal revenusEpouxDefendeurEur,
        String logementCommunDescription,
        String logementProprietaire,
        List<EnfantMineurInfo> enfantsMineurs,
        String souhaitResidenceEnfants,
        Boolean violencesAlleguees,
        Boolean patrimoineCommunIsignificatif,
        Boolean demandeMesureConservatoire
) {
    public record EnfantMineurInfo(String prenom, Integer age) {}
}
