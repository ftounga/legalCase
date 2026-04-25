package fr.ailegalcase.casefile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-12-01 : résultat de l'analyse des mesures provisoires (art. 254-256
 * Cciv) prononcées à l'audience d'orientation et sur mesures provisoires
 * (AOMP). Outil single-country FR — BE = F-FA-11.
 */
public record MesuresProvisoiresResult(
        LocalDate dateAudienceAOMP,
        BigDecimal revenusEpouxDemandeurEur,
        BigDecimal revenusEpouxDefendeurEur,
        String logementCommunDescription,
        String logementProprietaire,
        List<EnfantMineurInfo> enfantsMineurs,
        String souhaitResidenceEnfants,
        boolean violencesAlleguees,
        boolean patrimoineCommunIsignificatif,
        boolean demandeMesureConservatoire,
        BigDecimal differentielRevenus,
        BigDecimal pensionAlimentairePropose,
        String attributionLogementRecommande,
        String residenceEnfantsRecommande,
        BigDecimal contributionCharges,
        boolean mesureConservatoireRecommande,
        int scoreCohesionMesures,
        String verdictAcceptabilite,
        String formule,
        String baseJuridique,
        List<String> messages
) {
    public record EnfantMineurInfo(String prenom, int age) {
        @JsonCreator
        public EnfantMineurInfo(@JsonProperty("prenom") String prenom,
                                @JsonProperty("age") int age) {
            this.prenom = prenom;
            this.age = age;
        }
    }
}
