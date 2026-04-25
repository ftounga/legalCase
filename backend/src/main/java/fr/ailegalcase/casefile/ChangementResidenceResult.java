package fr.ailegalcase.casefile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-19-03 : résultat du calcul d'acceptabilité d'un changement de résidence
 * (art. 373-2 al. 3 Cciv).
 *
 * <p>Outil single-country FR — DROIT_FAMILLE.
 */
public record ChangementResidenceResult(
        LocalDate dateChangementPrevu,
        int distanceKm,
        String raisonChangement,
        boolean consentementAutreParent,
        boolean informePrealablement,
        int delaiInformationJours,
        String modeResidenceActuel,
        List<Integer> ageEnfants,
        boolean scolariteImpactee,
        boolean modificationDvhDemandee,
        int scoreAcceptabilite,
        String verdictProbabiliteAcceptation,
        boolean obligationInformationRespectee,
        boolean expertisePsyEnfantRecommandee,
        boolean delaiPreavisLegalOk,
        String baseJuridique,
        String formule,
        List<String> messages
) {
    @JsonCreator
    public ChangementResidenceResult(
            @JsonProperty("dateChangementPrevu") LocalDate dateChangementPrevu,
            @JsonProperty("distanceKm") int distanceKm,
            @JsonProperty("raisonChangement") String raisonChangement,
            @JsonProperty("consentementAutreParent") boolean consentementAutreParent,
            @JsonProperty("informePrealablement") boolean informePrealablement,
            @JsonProperty("delaiInformationJours") int delaiInformationJours,
            @JsonProperty("modeResidenceActuel") String modeResidenceActuel,
            @JsonProperty("ageEnfants") List<Integer> ageEnfants,
            @JsonProperty("scolariteImpactee") boolean scolariteImpactee,
            @JsonProperty("modificationDvhDemandee") boolean modificationDvhDemandee,
            @JsonProperty("scoreAcceptabilite") int scoreAcceptabilite,
            @JsonProperty("verdictProbabiliteAcceptation") String verdictProbabiliteAcceptation,
            @JsonProperty("obligationInformationRespectee") boolean obligationInformationRespectee,
            @JsonProperty("expertisePsyEnfantRecommandee") boolean expertisePsyEnfantRecommandee,
            @JsonProperty("delaiPreavisLegalOk") boolean delaiPreavisLegalOk,
            @JsonProperty("baseJuridique") String baseJuridique,
            @JsonProperty("formule") String formule,
            @JsonProperty("messages") List<String> messages) {
        this.dateChangementPrevu = dateChangementPrevu;
        this.distanceKm = distanceKm;
        this.raisonChangement = raisonChangement;
        this.consentementAutreParent = consentementAutreParent;
        this.informePrealablement = informePrealablement;
        this.delaiInformationJours = delaiInformationJours;
        this.modeResidenceActuel = modeResidenceActuel;
        this.ageEnfants = ageEnfants != null ? List.copyOf(ageEnfants) : List.of();
        this.scolariteImpactee = scolariteImpactee;
        this.modificationDvhDemandee = modificationDvhDemandee;
        this.scoreAcceptabilite = scoreAcceptabilite;
        this.verdictProbabiliteAcceptation = verdictProbabiliteAcceptation;
        this.obligationInformationRespectee = obligationInformationRespectee;
        this.expertisePsyEnfantRecommandee = expertisePsyEnfantRecommandee;
        this.delaiPreavisLegalOk = delaiPreavisLegalOk;
        this.baseJuridique = baseJuridique;
        this.formule = formule;
        this.messages = messages != null ? List.copyOf(messages) : List.of();
    }
}
