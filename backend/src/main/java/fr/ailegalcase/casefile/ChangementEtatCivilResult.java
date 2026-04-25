package fr.ailegalcase.casefile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-26-01 : résultat du calcul de changement d'état civil
 * (art. 60 / 61-3-1 / 61-5 Cciv). Outil single-country FR — DROIT_FAMILLE.
 */
public record ChangementEtatCivilResult(
        String typeChangement,
        String motifInvoque,
        List<String> preuvesProduites,
        boolean majeurDemandeur,
        boolean consentementParental,
        boolean datesDocsConcordants,
        boolean dejaChangeAuparavant,
        LocalDate dateNaissanceDemandeur,
        String departementDeclaration,
        String competenceProcedure,
        int delaiInstructionMoisPrevisionnel,
        int scoreAcceptabilite,
        String verdictAcceptabilite,
        List<String> documentsRequisManquants,
        String baseJuridique,
        String formule,
        List<String> messages
) {
    @JsonCreator
    public ChangementEtatCivilResult(
            @JsonProperty("typeChangement") String typeChangement,
            @JsonProperty("motifInvoque") String motifInvoque,
            @JsonProperty("preuvesProduites") List<String> preuvesProduites,
            @JsonProperty("majeurDemandeur") boolean majeurDemandeur,
            @JsonProperty("consentementParental") boolean consentementParental,
            @JsonProperty("datesDocsConcordants") boolean datesDocsConcordants,
            @JsonProperty("dejaChangeAuparavant") boolean dejaChangeAuparavant,
            @JsonProperty("dateNaissanceDemandeur") LocalDate dateNaissanceDemandeur,
            @JsonProperty("departementDeclaration") String departementDeclaration,
            @JsonProperty("competenceProcedure") String competenceProcedure,
            @JsonProperty("delaiInstructionMoisPrevisionnel") int delaiInstructionMoisPrevisionnel,
            @JsonProperty("scoreAcceptabilite") int scoreAcceptabilite,
            @JsonProperty("verdictAcceptabilite") String verdictAcceptabilite,
            @JsonProperty("documentsRequisManquants") List<String> documentsRequisManquants,
            @JsonProperty("baseJuridique") String baseJuridique,
            @JsonProperty("formule") String formule,
            @JsonProperty("messages") List<String> messages) {
        this.typeChangement = typeChangement;
        this.motifInvoque = motifInvoque;
        this.preuvesProduites = preuvesProduites != null
                ? List.copyOf(preuvesProduites) : List.of();
        this.majeurDemandeur = majeurDemandeur;
        this.consentementParental = consentementParental;
        this.datesDocsConcordants = datesDocsConcordants;
        this.dejaChangeAuparavant = dejaChangeAuparavant;
        this.dateNaissanceDemandeur = dateNaissanceDemandeur;
        this.departementDeclaration = departementDeclaration;
        this.competenceProcedure = competenceProcedure;
        this.delaiInstructionMoisPrevisionnel = delaiInstructionMoisPrevisionnel;
        this.scoreAcceptabilite = scoreAcceptabilite;
        this.verdictAcceptabilite = verdictAcceptabilite;
        this.documentsRequisManquants = documentsRequisManquants != null
                ? List.copyOf(documentsRequisManquants) : List.of();
        this.baseJuridique = baseJuridique;
        this.formule = formule;
        this.messages = messages != null ? List.copyOf(messages) : List.of();
    }
}
