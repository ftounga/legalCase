package fr.ailegalcase.casefile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-19-01 : résultat du calcul d'éligibilité au changement de régime
 * d'exercice de l'autorité parentale (art. 372-373 Cciv).
 *
 * <p>Outil single-country FR — DROIT_FAMILLE.
 */
public record AutoriteParentaleResult(
        String regimeExerciceActuel,
        String regimeExerciceDemande,
        String motifChangement,
        boolean dangerCaracterise,
        List<String> preuvesProduites,
        List<Integer> ageEnfants,
        boolean consentementAutreParent,
        boolean interferenceVieEnfant,
        LocalDate dateRequete,
        int scoreEligibilite,
        String verdictProbabiliteAcceptation,
        boolean expertiseRecommandee,
        boolean auditionEnfantsRecommandee,
        String baseJuridique,
        String formule,
        List<String> messages
) {
    @JsonCreator
    public AutoriteParentaleResult(
            @JsonProperty("regimeExerciceActuel") String regimeExerciceActuel,
            @JsonProperty("regimeExerciceDemande") String regimeExerciceDemande,
            @JsonProperty("motifChangement") String motifChangement,
            @JsonProperty("dangerCaracterise") boolean dangerCaracterise,
            @JsonProperty("preuvesProduites") List<String> preuvesProduites,
            @JsonProperty("ageEnfants") List<Integer> ageEnfants,
            @JsonProperty("consentementAutreParent") boolean consentementAutreParent,
            @JsonProperty("interferenceVieEnfant") boolean interferenceVieEnfant,
            @JsonProperty("dateRequete") LocalDate dateRequete,
            @JsonProperty("scoreEligibilite") int scoreEligibilite,
            @JsonProperty("verdictProbabiliteAcceptation") String verdictProbabiliteAcceptation,
            @JsonProperty("expertiseRecommandee") boolean expertiseRecommandee,
            @JsonProperty("auditionEnfantsRecommandee") boolean auditionEnfantsRecommandee,
            @JsonProperty("baseJuridique") String baseJuridique,
            @JsonProperty("formule") String formule,
            @JsonProperty("messages") List<String> messages) {
        this.regimeExerciceActuel = regimeExerciceActuel;
        this.regimeExerciceDemande = regimeExerciceDemande;
        this.motifChangement = motifChangement;
        this.dangerCaracterise = dangerCaracterise;
        this.preuvesProduites = preuvesProduites != null ? List.copyOf(preuvesProduites) : List.of();
        this.ageEnfants = ageEnfants != null ? List.copyOf(ageEnfants) : List.of();
        this.consentementAutreParent = consentementAutreParent;
        this.interferenceVieEnfant = interferenceVieEnfant;
        this.dateRequete = dateRequete;
        this.scoreEligibilite = scoreEligibilite;
        this.verdictProbabiliteAcceptation = verdictProbabiliteAcceptation;
        this.expertiseRecommandee = expertiseRecommandee;
        this.auditionEnfantsRecommandee = auditionEnfantsRecommandee;
        this.baseJuridique = baseJuridique;
        this.formule = formule;
        this.messages = messages != null ? List.copyOf(messages) : List.of();
    }
}
