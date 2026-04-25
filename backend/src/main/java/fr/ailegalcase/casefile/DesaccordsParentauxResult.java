package fr.ailegalcase.casefile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-19-05 : résultat du calcul d'éligibilité au recours JAF en cas de
 * désaccord parental (art. 373-2-10 Cciv).
 *
 * <p>Outil single-country FR — DROIT_FAMILLE.
 */
public record DesaccordsParentauxResult(
        String domaineDesaccord,
        String intensiteDesaccord,
        List<String> tentativesMediation,
        List<Integer> ageEnfantsConcernes,
        boolean interetSuperieurInvoque,
        boolean expertiseDejaRealisee,
        boolean urgence,
        LocalDate dateRequete,
        int scoreEligibiliteJaf,
        String verdictProbabiliteAcceptation,
        boolean mediationPrealableRecommandee,
        boolean auditionEnfantsRecommandee,
        boolean expertisePsyRecommandee,
        int delaiTraitementJoursPrevisionnel,
        String baseJuridique,
        String formule,
        List<String> messages
) {
    @JsonCreator
    public DesaccordsParentauxResult(
            @JsonProperty("domaineDesaccord") String domaineDesaccord,
            @JsonProperty("intensiteDesaccord") String intensiteDesaccord,
            @JsonProperty("tentativesMediation") List<String> tentativesMediation,
            @JsonProperty("ageEnfantsConcernes") List<Integer> ageEnfantsConcernes,
            @JsonProperty("interetSuperieurInvoque") boolean interetSuperieurInvoque,
            @JsonProperty("expertiseDejaRealisee") boolean expertiseDejaRealisee,
            @JsonProperty("urgence") boolean urgence,
            @JsonProperty("dateRequete") LocalDate dateRequete,
            @JsonProperty("scoreEligibiliteJaf") int scoreEligibiliteJaf,
            @JsonProperty("verdictProbabiliteAcceptation") String verdictProbabiliteAcceptation,
            @JsonProperty("mediationPrealableRecommandee") boolean mediationPrealableRecommandee,
            @JsonProperty("auditionEnfantsRecommandee") boolean auditionEnfantsRecommandee,
            @JsonProperty("expertisePsyRecommandee") boolean expertisePsyRecommandee,
            @JsonProperty("delaiTraitementJoursPrevisionnel") int delaiTraitementJoursPrevisionnel,
            @JsonProperty("baseJuridique") String baseJuridique,
            @JsonProperty("formule") String formule,
            @JsonProperty("messages") List<String> messages) {
        this.domaineDesaccord = domaineDesaccord;
        this.intensiteDesaccord = intensiteDesaccord;
        this.tentativesMediation = tentativesMediation != null
                ? List.copyOf(tentativesMediation) : List.of();
        this.ageEnfantsConcernes = ageEnfantsConcernes != null
                ? List.copyOf(ageEnfantsConcernes) : List.of();
        this.interetSuperieurInvoque = interetSuperieurInvoque;
        this.expertiseDejaRealisee = expertiseDejaRealisee;
        this.urgence = urgence;
        this.dateRequete = dateRequete;
        this.scoreEligibiliteJaf = scoreEligibiliteJaf;
        this.verdictProbabiliteAcceptation = verdictProbabiliteAcceptation;
        this.mediationPrealableRecommandee = mediationPrealableRecommandee;
        this.auditionEnfantsRecommandee = auditionEnfantsRecommandee;
        this.expertisePsyRecommandee = expertisePsyRecommandee;
        this.delaiTraitementJoursPrevisionnel = delaiTraitementJoursPrevisionnel;
        this.baseJuridique = baseJuridique;
        this.formule = formule;
        this.messages = messages != null ? List.copyOf(messages) : List.of();
    }
}
