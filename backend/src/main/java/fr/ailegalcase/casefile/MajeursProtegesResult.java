package fr.ailegalcase.casefile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-25-01 : résultat du calcul d'éligibilité d'une mesure de protection
 * des majeurs (sauvegarde de justice + habilitation familiale en priorité).
 *
 * <p>SF-FA-25-03 : extension pour curatelle simple (art. 440 al. 1) et
 * renforcée (art. 472) — ajout des champs {@code incapaciteGestionQuotidienne}
 * (input) + {@code eligible} et {@code criteresNonRemplis} (sortie).
 *
 * <p>SF-FA-25-04 : extension pour tutelle (art. 440 al. 3) — ajout du champ
 * {@code altertationGrave} (input) qui distingue la tutelle (altération
 * grave et durable empêchant la personne de pourvoir seule à ses intérêts)
 * de la curatelle.
 *
 * <p>SF-FA-25-05 : extension pour mandat de protection future
 * (art. 477-494) — ajout des champs {@code mandatPrealableSigne} (input
 * pivot art. 477) et {@code formeMandatProtection} (NOTARIE art. 489 vs
 * SOUS_SEING_PRIVE art. 492). Clôt F-FA-25 6/6 régimes.
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
        boolean incapaciteGestionQuotidienne,
        boolean altertationGrave,
        boolean mandatPrealableSigne,
        String formeMandatProtection,
        int scoreEligibilite,
        String regimeOptimalRecommande,
        String verdictAcceptabiliteJaf,
        int delaiProcedureMoisPrevisionnel,
        boolean auditionPersonneObligatoire,
        boolean expertisePsyComplementaireRecommandee,
        boolean eligible,
        List<String> criteresNonRemplis,
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
            @JsonProperty("incapaciteGestionQuotidienne") boolean incapaciteGestionQuotidienne,
            @JsonProperty("altertationGrave") boolean altertationGrave,
            @JsonProperty("mandatPrealableSigne") boolean mandatPrealableSigne,
            @JsonProperty("formeMandatProtection") String formeMandatProtection,
            @JsonProperty("scoreEligibilite") int scoreEligibilite,
            @JsonProperty("regimeOptimalRecommande") String regimeOptimalRecommande,
            @JsonProperty("verdictAcceptabiliteJaf") String verdictAcceptabiliteJaf,
            @JsonProperty("delaiProcedureMoisPrevisionnel") int delaiProcedureMoisPrevisionnel,
            @JsonProperty("auditionPersonneObligatoire") boolean auditionPersonneObligatoire,
            @JsonProperty("expertisePsyComplementaireRecommandee") boolean expertisePsyComplementaireRecommandee,
            @JsonProperty("eligible") boolean eligible,
            @JsonProperty("criteresNonRemplis") List<String> criteresNonRemplis,
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
        this.incapaciteGestionQuotidienne = incapaciteGestionQuotidienne;
        this.altertationGrave = altertationGrave;
        this.mandatPrealableSigne = mandatPrealableSigne;
        this.formeMandatProtection = formeMandatProtection;
        this.scoreEligibilite = scoreEligibilite;
        this.regimeOptimalRecommande = regimeOptimalRecommande;
        this.verdictAcceptabiliteJaf = verdictAcceptabiliteJaf;
        this.delaiProcedureMoisPrevisionnel = delaiProcedureMoisPrevisionnel;
        this.auditionPersonneObligatoire = auditionPersonneObligatoire;
        this.expertisePsyComplementaireRecommandee = expertisePsyComplementaireRecommandee;
        this.eligible = eligible;
        this.criteresNonRemplis = criteresNonRemplis != null
                ? List.copyOf(criteresNonRemplis) : List.of();
        this.baseJuridique = baseJuridique;
        this.formule = formule;
        this.messages = messages != null ? List.copyOf(messages) : List.of();
    }
}
