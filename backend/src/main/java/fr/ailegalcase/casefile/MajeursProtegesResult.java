package fr.ailegalcase.casefile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-25-01 : résultat du calcul d'éligibilité d'une mesure de protection
 * des majeurs (sauvegarde de justice + habilitation familiale en priorité).
 *
 * <p>Outil single-country FR — DROIT_FAMILLE.
 */
public record MajeursProtegesResult(
        String regimeProtectionDemande,
        boolean altertationFacultesMentales,
        boolean altertationFacultesPhysiques,
        boolean certificatMedicalCirconstancie,
        LocalDate dateCertificatMedical,
        boolean consentementPersonneAProteger,
        String demandeurFamilial,
        List<String> actesEnvisages,
        boolean urgencePatrimoniale,
        boolean patrimoineSignificatif,
        boolean isolementSocial,
        int scoreEligibilite,
        String regimeOptimalRecommande,
        String verdictAcceptabiliteJaf,
        int delaiProcedureMoisPrevisionnel,
        boolean auditionPersonneObligatoire,
        boolean expertisePsyComplementaireRecommandee,
        String baseJuridique,
        String formule,
        List<String> messages
) {
    @JsonCreator
    public MajeursProtegesResult(
            @JsonProperty("regimeProtectionDemande") String regimeProtectionDemande,
            @JsonProperty("altertationFacultesMentales") boolean altertationFacultesMentales,
            @JsonProperty("altertationFacultesPhysiques") boolean altertationFacultesPhysiques,
            @JsonProperty("certificatMedicalCirconstancie") boolean certificatMedicalCirconstancie,
            @JsonProperty("dateCertificatMedical") LocalDate dateCertificatMedical,
            @JsonProperty("consentementPersonneAProteger") boolean consentementPersonneAProteger,
            @JsonProperty("demandeurFamilial") String demandeurFamilial,
            @JsonProperty("actesEnvisages") List<String> actesEnvisages,
            @JsonProperty("urgencePatrimoniale") boolean urgencePatrimoniale,
            @JsonProperty("patrimoineSignificatif") boolean patrimoineSignificatif,
            @JsonProperty("isolementSocial") boolean isolementSocial,
            @JsonProperty("scoreEligibilite") int scoreEligibilite,
            @JsonProperty("regimeOptimalRecommande") String regimeOptimalRecommande,
            @JsonProperty("verdictAcceptabiliteJaf") String verdictAcceptabiliteJaf,
            @JsonProperty("delaiProcedureMoisPrevisionnel") int delaiProcedureMoisPrevisionnel,
            @JsonProperty("auditionPersonneObligatoire") boolean auditionPersonneObligatoire,
            @JsonProperty("expertisePsyComplementaireRecommandee") boolean expertisePsyComplementaireRecommandee,
            @JsonProperty("baseJuridique") String baseJuridique,
            @JsonProperty("formule") String formule,
            @JsonProperty("messages") List<String> messages) {
        this.regimeProtectionDemande = regimeProtectionDemande;
        this.altertationFacultesMentales = altertationFacultesMentales;
        this.altertationFacultesPhysiques = altertationFacultesPhysiques;
        this.certificatMedicalCirconstancie = certificatMedicalCirconstancie;
        this.dateCertificatMedical = dateCertificatMedical;
        this.consentementPersonneAProteger = consentementPersonneAProteger;
        this.demandeurFamilial = demandeurFamilial;
        this.actesEnvisages = actesEnvisages != null
                ? List.copyOf(actesEnvisages) : List.of();
        this.urgencePatrimoniale = urgencePatrimoniale;
        this.patrimoineSignificatif = patrimoineSignificatif;
        this.isolementSocial = isolementSocial;
        this.scoreEligibilite = scoreEligibilite;
        this.regimeOptimalRecommande = regimeOptimalRecommande;
        this.verdictAcceptabiliteJaf = verdictAcceptabiliteJaf;
        this.delaiProcedureMoisPrevisionnel = delaiProcedureMoisPrevisionnel;
        this.auditionPersonneObligatoire = auditionPersonneObligatoire;
        this.expertisePsyComplementaireRecommandee = expertisePsyComplementaireRecommandee;
        this.baseJuridique = baseJuridique;
        this.formule = formule;
        this.messages = messages != null ? List.copyOf(messages) : List.of();
    }
}
