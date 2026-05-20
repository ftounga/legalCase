package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record CaseAnalysisResponse(
        UUID id,
        int version,
        String analysisType,
        String status,
        List<TimelineEntry> timeline,
        List<AnalysisItem> faits,
        List<AnalysisItem> pointsJuridiques,
        List<AnalysisItem> risques,
        List<String> questionsOuvertes,
        List<String> piecesManquantes,
        List<String> pointsProcedure,
        String riskLevel,
        Integer riskScore,
        String modelUsed,
        Instant updatedAt,
        List<AnalysisDocumentEntry> analysisDocuments,
        CompensationCalculator.CompensationEstimate compensationEstimate,
        BelgianCompensationCalculator.BelgianCompensationEstimate belgianCompensationEstimate,
        PensionAlimentaireCalculator.PensionAlimentaireEstimate pensionAlimentaireEstimate,
        PrestationCompensatoireCalculator.PrestationCompensatoireEstimate prestationCompensatoireEstimate,
        LiquidationCommunauteResult liquidationCommunaute,
        TravailExtractedData travailExtractedData,
        ImmigrationExtractedData immigrationExtractedData,
        LicenciementValidityDetection licenciementValidityDetection,
        RuptureConvValidityDetection ruptureConvValidityDetection,
        List<PieceManquanteEntry> piecesManquantesDetails,
        // F-150 : événements factuels immigration détectés (liste vide hors domaine immigration).
        List<fr.ailegalcase.immigration.ImmigrationTriggerEvent> immigrationTriggerEvents,
        // F-151 : scenarii stratégiques immigration (liste vide si aucun choix stratégique ouvert).
        List<fr.ailegalcase.immigration.ImmigrationStrategyScenario> immigrationStrategyScenarios,
        // F-152 : validité divorce consentement mutuel (famille, null hors domaine famille).
        DivorceConsentementValidityDetection divorceConsentementValidityDetection,
        DivorceConsentementScoring divorceConsentementScoring,
        // F-202 : flags décisionnels niveau 3 Famille BE (FR ajoutés ultérieurement par F-200).
        // Null hors domaine famille.
        FamilleExtractedData familleExtractedData
) {

    /**
     * Constructeur rétrocompat 29-args (pré-F-202) — délègue avec
     * {@code familleExtractedData = null}.
     */
    public CaseAnalysisResponse(UUID id, int version, String analysisType, String status,
                                List<TimelineEntry> timeline,
                                List<AnalysisItem> faits, List<AnalysisItem> pointsJuridiques,
                                List<AnalysisItem> risques, List<String> questionsOuvertes,
                                List<String> piecesManquantes, List<String> pointsProcedure,
                                String riskLevel, Integer riskScore, String modelUsed,
                                Instant updatedAt, List<AnalysisDocumentEntry> analysisDocuments,
                                CompensationCalculator.CompensationEstimate compensationEstimate,
                                BelgianCompensationCalculator.BelgianCompensationEstimate belgianCompensationEstimate,
                                PensionAlimentaireCalculator.PensionAlimentaireEstimate pensionAlimentaireEstimate,
                                PrestationCompensatoireCalculator.PrestationCompensatoireEstimate prestationCompensatoireEstimate,
                                LiquidationCommunauteResult liquidationCommunaute,
                                TravailExtractedData travailExtractedData,
                                ImmigrationExtractedData immigrationExtractedData,
                                LicenciementValidityDetection licenciementValidityDetection,
                                RuptureConvValidityDetection ruptureConvValidityDetection,
                                List<PieceManquanteEntry> piecesManquantesDetails,
                                List<fr.ailegalcase.immigration.ImmigrationTriggerEvent> immigrationTriggerEvents,
                                List<fr.ailegalcase.immigration.ImmigrationStrategyScenario> immigrationStrategyScenarios,
                                DivorceConsentementValidityDetection divorceConsentementValidityDetection,
                                DivorceConsentementScoring divorceConsentementScoring) {
        this(id, version, analysisType, status, timeline, faits, pointsJuridiques, risques,
                questionsOuvertes, piecesManquantes, pointsProcedure, riskLevel, riskScore, modelUsed,
                updatedAt, analysisDocuments, compensationEstimate, belgianCompensationEstimate,
                pensionAlimentaireEstimate, prestationCompensatoireEstimate, liquidationCommunaute,
                travailExtractedData, immigrationExtractedData, licenciementValidityDetection,
                ruptureConvValidityDetection, piecesManquantesDetails, immigrationTriggerEvents,
                immigrationStrategyScenarios, divorceConsentementValidityDetection,
                divorceConsentementScoring, null);
    }

    /** Constructeur rétrocompat sans trigger events (pré-F-150). */
    public CaseAnalysisResponse(UUID id, int version, String analysisType, String status,
                                List<TimelineEntry> timeline,
                                List<AnalysisItem> faits, List<AnalysisItem> pointsJuridiques,
                                List<AnalysisItem> risques, List<String> questionsOuvertes,
                                List<String> piecesManquantes, List<String> pointsProcedure,
                                String riskLevel, Integer riskScore, String modelUsed,
                                Instant updatedAt, List<AnalysisDocumentEntry> analysisDocuments,
                                CompensationCalculator.CompensationEstimate compensationEstimate,
                                BelgianCompensationCalculator.BelgianCompensationEstimate belgianCompensationEstimate,
                                PensionAlimentaireCalculator.PensionAlimentaireEstimate pensionAlimentaireEstimate,
                                PrestationCompensatoireCalculator.PrestationCompensatoireEstimate prestationCompensatoireEstimate,
                                LiquidationCommunauteResult liquidationCommunaute,
                                TravailExtractedData travailExtractedData,
                                ImmigrationExtractedData immigrationExtractedData,
                                LicenciementValidityDetection licenciementValidityDetection,
                                RuptureConvValidityDetection ruptureConvValidityDetection,
                                List<PieceManquanteEntry> piecesManquantesDetails) {
        this(id, version, analysisType, status, timeline, faits, pointsJuridiques, risques,
                questionsOuvertes, piecesManquantes, pointsProcedure, riskLevel, riskScore, modelUsed,
                updatedAt, analysisDocuments, compensationEstimate, belgianCompensationEstimate,
                pensionAlimentaireEstimate, prestationCompensatoireEstimate, liquidationCommunaute,
                travailExtractedData, immigrationExtractedData, licenciementValidityDetection,
                ruptureConvValidityDetection, piecesManquantesDetails, List.of(), List.of(),
                null, null);
    }

    public record PieceManquanteEntry(String texte, String critereCode) {}

    public record TravailExtractedData(
            String conventionCollective, String dateEntree, Double salaireBrutMensuel,
            String typeContrat, String poste, String motifLicenciement, String dateLicenciement,
            Integer congesContractuels, Double primeAncienneteContractuelle,
            // SF-DT-04-04 : identité salarié + employeur pour pré-remplissage fiches prud'homale (FR) et
            // requête tribunal du travail (BE). siretEmployeur renseigné côté FR uniquement,
            // bceEmployeur côté BE uniquement (champs à formats distincts).
            String nomSalarie, String prenomSalarie, String adresseSalarie,
            String nomEmployeur, String adresseEmployeur,
            String siretEmployeur, String bceEmployeur,
            String representantEmployeur,
            // SF-130-01 : true si salaireBrutMensuel a été déduit d'un net via conversion × 1,30
            Boolean salaireEstDeduit,
            // SF-155-04-00-BE-travail : 5 champs IA pour pré-fill outils décisionnels F-DT-11/15/19.
            // Tous nullables — la plupart des dossiers travail BE ne portent pas ces concepts FR.
            String motifNullitePressenti,
            String origineInaptitudePressentie,
            String avisMedecinTravailDate,
            DetectedAnswer reclassementRespecteDetected,
            HeuresSupMentionnees heuresSupMentionneesDansDossier,
            // SF-166-01 : 8 flags décisionnels niveau 3 — Travail FR uniquement, default false.
            // Permettent à F-IA-04 de basculer F-DT-20/21/24/30/31/33/34/35 en CONTEXTUAL (SF-166-02).
            // Dossiers BE : tous false (régimes BE équivalents distincts → backlog jumeau).
            boolean rappelSalaireDetecte,
            boolean travailDissimuleDetecte,
            boolean clauseNonConcurrenceDetectee,
            boolean statutProtegeDetecte,
            boolean transactionEnvisagee,
            boolean atMpDetecte,
            boolean urgenceProcedurale,
            boolean contestationAreEnvisagee,
            // === Flags BE (F-204) ===
            // F-204 : 5 flags décisionnels niveau 3 — Travail BELGIQUE uniquement, default false.
            // Permettent à F-IA-04 de basculer F-DT-11/12/15/19/27 (BE) en CONTEXTUAL.
            // Dossiers FR : tous false (les régimes FR équivalents sont gérés par les flags FR ci-dessus
            // ou par F-205 ultérieur).
            boolean harcelementBeDetecte,
            boolean discriminationBeDetectee,
            boolean inaptitudeMedicaleBeDetectee,
            boolean heuresSupMentionneesBe,
            boolean motifGraveBeEnvisage,
            // === F-205 (P1+P2 flags FR) ===
            // F-205 : 23 flags décisionnels niveau 3 additionnels — Travail FRANCE uniquement, default false.
            // Préparent l'arrivée des outils manquants P1 (F-206) et P2 (F-212) listés dans
            // docs/features/F-191/audit-travail-fr-exhaustif.md (Tableau C). Aucune règle de visibilité
            // ne consomme encore ces flags : F-206/F-212 livreront les migrations decision_tool_visibility_rules.
            // Dossiers BE : tous false (régimes BE équivalents gérés par F-204 ou sans équivalent direct
            // — CSP/CRP, prise d'acte, résiliation judiciaire, présomption abandon de poste loi 21/12/2022
            // sont des mécanismes purement français).
            // Note : `salarie_protege_detecte` du brief F-205 est volontairement OMIS — déjà couvert par
            // `statutProtegeDetecte` (F-166).
            boolean abandonPosteDetecte,
            boolean arretMaladieLongDetecte,
            boolean priseActeEnvisagee,
            boolean resiliationJudiciaireEnvisagee,
            boolean forfaitJoursDetecte,
            boolean transfertEntrepriseDetecte,
            boolean fauteInexcusableEnvisagee,
            boolean csCrpEnvisage,
            boolean cspPropose,
            boolean mutationRefusee,
            boolean modificationContratRefusee,
            boolean fauteGraveEnvisagee,
            boolean fauteLourdeEnvisagee,
            boolean cddRequalificationEnvisagee,
            boolean interimRequalificationEnvisagee,
            boolean forfaitJoursValiditeContestee,
            boolean prescriptionProcheDetectee,
            boolean ruptureAmiableNegociee,
            boolean entretienPreavisObtenu,
            boolean cseConsultationDemandee,
            boolean irpElectionDemandee,
            boolean inspectionTravailSaisie,
            boolean mediationJudiciaireEnvisagee,
            // SF-246-01 : 6 champs IA procéduraux pour pré-fill F-DT-36 (nullité de
            // procédure de licenciement, Travail FR uniquement, nullables). La nullité
            // de procédure FR (entretien préalable, délai 5 j ouvrables, lettre motivée)
            // n'a pas d'équivalent direct côté BE — ces champs restent null pour la BE.
            Boolean convocationEntretienDetectee,
            String dateConvocationEntretienDetectee,
            String dateEntretienPrealableDetectee,
            DetectedAnswer entretienPrealableTenuDetected,
            Boolean lettreLicenciementEcriteDetectee,
            DetectedAnswer lettreLicenciementMotiveeDetected,
            DetectedAnswer motivationLettreSuffisanteDetected,
            // SF-246-02 : 3 champs IA pour pré-fill F-DT-24 clause de non-concurrence
            // (Travail FR uniquement, nullables). Durée bornée [0, 600] mois, zone
            // géographique tronquée à 500 car., contrepartie en euros bruts mensuels
            // (> 0). La clause de non-concurrence BE (CCT 1bis) relève d'un régime
            // distinct — ces champs restent null pour un dossier travail belge.
            Integer nonConcurrenceDureeMois,
            String nonConcurrenceZoneGeographique,
            Double nonConcurrenceContrepartieMontantEur,
            // SF-246-05 : âge du demandeur pour pré-fill F-DT-29 crédit-temps fin de
            // carrière (Travail BELGIQUE uniquement, nullable). Entier borné [0, 100] ;
            // hors plage → null. Le crédit-temps fin de carrière (CCT 103, AR 29/10/1997)
            // est un dispositif belge — ce champ reste null pour un dossier travail FR.
            Integer ageDemandeurAnnees,
            // SF-246-13 : 2 champs IA complétant le détail de la clause de
            // non-concurrence pour pré-fill F-DT-24 (Travail FR uniquement, nullables).
            // Date de prise d'effet de la clause au format ISO YYYY-MM-DD (≈ date de
            // fin / rupture du contrat) ; secteur d'activité normalisé sur l'enum
            // NonConcurrenceCalculator.SecteurActivite. Restent null pour un dossier
            // travail belge (clause BE = régime CCT 1bis distinct).
            String nonConcurrenceDatePriseEffet,
            String nonConcurrenceSecteurActivite,
            // SF-207-01 : 2 champs IA Travail BE pour pré-fill F-207-01 prescription
            // Travail BE. dateRuptureContrat : date de rupture du contrat (ISO
            // YYYY-MM-DD) — base du calcul du délai de 1 an (Loi 03/07/1978 art. 15
            // al. 1 + CCT 109 art. 11). motifRupture : motif de rupture détecté
            // (texte libre — licenciement, démission, faute grave, RCC...), utilisé
            // par F-IA-04 pour déduire le typeCreance pré-rempli côté frontend.
            // Tous nullables — Travail BE uniquement, restent null pour un dossier FR.
            // Réutilisables par les autres SF F-207 (C4, contestation, AT, RCC, outplacement).
            String dateRuptureContrat,
            String motifRupture,
            // SF-207-02 : 6 champs IA Travail BE additionnels pour pré-fill F-207-02
            // C4 ONEM checklist (AR 25/11/1991 art. 92 — 10 mentions obligatoires).
            // Tous nullables — Travail BE uniquement, restent null pour un dossier FR.
            // raisonSocialeEmployeur / numeroBce duplicent partiellement nomEmployeur /
            // bceEmployeur mais sont rattachés explicitement au document C4 (la mini-spec
            // SF-207-02 impose le naming spécifique). categorieOnem est le code du C4
            // (ex. "9" = faute grave). motifExplicite est la formulation littérale du
            // motif inscrite sur le C4 (≥ 5 car.). preavisPresteJours est le préavis
            // effectivement presté en jours (obligatoire si pas faute grave).
            // dernierSalaireMensuelBrut est le dernier salaire brut mensuel.
            String raisonSocialeEmployeur,
            String numeroBce,
            String categorieOnem,
            String motifExplicite,
            Integer preavisPresteJours,
            java.math.BigDecimal dernierSalaireMensuelBrut,
            // SF-246-22 : type de procédure travail et date déclencheur pour pré-fill
            // F-136 travail-procedure (FR+BE). 6 codes whitelistés (3 FR + 3 BE) :
            // PRUDHOMMES_FR, APPEL_CA_SOCIALE_FR, CASSATION_SOCIALE_FR,
            // TRIBUNAL_TRAVAIL_BE, COUR_TRAVAIL_BE, CASSATION_BE.
            // Codes hors whitelist exclus par extractTravailData().
            // dateDeclencheurProcedure : format ISO YYYY-MM-DD ou null.
            String procedureTravailDetectee,
            String dateDeclencheurProcedure) {

        /**
         * F-234 SF-234-01 : Builder pattern pour {@link TravailExtractedData}.
         * Permet la construction nommée et la propagation automatique de valeurs par défaut sûres
         * (null pour Object/Wrapper, false pour boolean) sans avoir à maintenir un constructeur
         * rétrocompat à chaque ajout de flag IA.
         */
        public static Builder builder() {
            return new Builder();
        }

        /** F-234 SF-234-01 : copie un record existant pour ajuster un sous-ensemble de champs. */
        public Builder toBuilder() {
            return new Builder()
                    .conventionCollective(conventionCollective)
                    .dateEntree(dateEntree)
                    .salaireBrutMensuel(salaireBrutMensuel)
                    .typeContrat(typeContrat)
                    .poste(poste)
                    .motifLicenciement(motifLicenciement)
                    .dateLicenciement(dateLicenciement)
                    .congesContractuels(congesContractuels)
                    .primeAncienneteContractuelle(primeAncienneteContractuelle)
                    .nomSalarie(nomSalarie)
                    .prenomSalarie(prenomSalarie)
                    .adresseSalarie(adresseSalarie)
                    .nomEmployeur(nomEmployeur)
                    .adresseEmployeur(adresseEmployeur)
                    .siretEmployeur(siretEmployeur)
                    .bceEmployeur(bceEmployeur)
                    .representantEmployeur(representantEmployeur)
                    .salaireEstDeduit(salaireEstDeduit)
                    .motifNullitePressenti(motifNullitePressenti)
                    .origineInaptitudePressentie(origineInaptitudePressentie)
                    .avisMedecinTravailDate(avisMedecinTravailDate)
                    .reclassementRespecteDetected(reclassementRespecteDetected)
                    .heuresSupMentionneesDansDossier(heuresSupMentionneesDansDossier)
                    .rappelSalaireDetecte(rappelSalaireDetecte)
                    .travailDissimuleDetecte(travailDissimuleDetecte)
                    .clauseNonConcurrenceDetectee(clauseNonConcurrenceDetectee)
                    .statutProtegeDetecte(statutProtegeDetecte)
                    .transactionEnvisagee(transactionEnvisagee)
                    .atMpDetecte(atMpDetecte)
                    .urgenceProcedurale(urgenceProcedurale)
                    .contestationAreEnvisagee(contestationAreEnvisagee)
                    .harcelementBeDetecte(harcelementBeDetecte)
                    .discriminationBeDetectee(discriminationBeDetectee)
                    .inaptitudeMedicaleBeDetectee(inaptitudeMedicaleBeDetectee)
                    .heuresSupMentionneesBe(heuresSupMentionneesBe)
                    .motifGraveBeEnvisage(motifGraveBeEnvisage)
                    .abandonPosteDetecte(abandonPosteDetecte)
                    .arretMaladieLongDetecte(arretMaladieLongDetecte)
                    .priseActeEnvisagee(priseActeEnvisagee)
                    .resiliationJudiciaireEnvisagee(resiliationJudiciaireEnvisagee)
                    .forfaitJoursDetecte(forfaitJoursDetecte)
                    .transfertEntrepriseDetecte(transfertEntrepriseDetecte)
                    .fauteInexcusableEnvisagee(fauteInexcusableEnvisagee)
                    .csCrpEnvisage(csCrpEnvisage)
                    .cspPropose(cspPropose)
                    .mutationRefusee(mutationRefusee)
                    .modificationContratRefusee(modificationContratRefusee)
                    .fauteGraveEnvisagee(fauteGraveEnvisagee)
                    .fauteLourdeEnvisagee(fauteLourdeEnvisagee)
                    .cddRequalificationEnvisagee(cddRequalificationEnvisagee)
                    .interimRequalificationEnvisagee(interimRequalificationEnvisagee)
                    .forfaitJoursValiditeContestee(forfaitJoursValiditeContestee)
                    .prescriptionProcheDetectee(prescriptionProcheDetectee)
                    .ruptureAmiableNegociee(ruptureAmiableNegociee)
                    .entretienPreavisObtenu(entretienPreavisObtenu)
                    .cseConsultationDemandee(cseConsultationDemandee)
                    .irpElectionDemandee(irpElectionDemandee)
                    .inspectionTravailSaisie(inspectionTravailSaisie)
                    .mediationJudiciaireEnvisagee(mediationJudiciaireEnvisagee)
                    .convocationEntretienDetectee(convocationEntretienDetectee)
                    .dateConvocationEntretienDetectee(dateConvocationEntretienDetectee)
                    .dateEntretienPrealableDetectee(dateEntretienPrealableDetectee)
                    .entretienPrealableTenuDetected(entretienPrealableTenuDetected)
                    .lettreLicenciementEcriteDetectee(lettreLicenciementEcriteDetectee)
                    .lettreLicenciementMotiveeDetected(lettreLicenciementMotiveeDetected)
                    .motivationLettreSuffisanteDetected(motivationLettreSuffisanteDetected)
                    .nonConcurrenceDureeMois(nonConcurrenceDureeMois)
                    .nonConcurrenceZoneGeographique(nonConcurrenceZoneGeographique)
                    .nonConcurrenceContrepartieMontantEur(nonConcurrenceContrepartieMontantEur)
                    .ageDemandeurAnnees(ageDemandeurAnnees)
                    .nonConcurrenceDatePriseEffet(nonConcurrenceDatePriseEffet)
                    .nonConcurrenceSecteurActivite(nonConcurrenceSecteurActivite)
                    .dateRuptureContrat(dateRuptureContrat)
                    .motifRupture(motifRupture)
                    .raisonSocialeEmployeur(raisonSocialeEmployeur)
                    .numeroBce(numeroBce)
                    .categorieOnem(categorieOnem)
                    .motifExplicite(motifExplicite)
                    .preavisPresteJours(preavisPresteJours)
                    .dernierSalaireMensuelBrut(dernierSalaireMensuelBrut)
                    .procedureTravailDetectee(procedureTravailDetectee)
                    .dateDeclencheurProcedure(dateDeclencheurProcedure);
        }

        public static final class Builder {
            private String conventionCollective;
            private String dateEntree;
            private Double salaireBrutMensuel;
            private String typeContrat;
            private String poste;
            private String motifLicenciement;
            private String dateLicenciement;
            private Integer congesContractuels;
            private Double primeAncienneteContractuelle;
            private String nomSalarie;
            private String prenomSalarie;
            private String adresseSalarie;
            private String nomEmployeur;
            private String adresseEmployeur;
            private String siretEmployeur;
            private String bceEmployeur;
            private String representantEmployeur;
            private Boolean salaireEstDeduit;
            private String motifNullitePressenti;
            private String origineInaptitudePressentie;
            private String avisMedecinTravailDate;
            private DetectedAnswer reclassementRespecteDetected;
            private HeuresSupMentionnees heuresSupMentionneesDansDossier;
            private boolean rappelSalaireDetecte;
            private boolean travailDissimuleDetecte;
            private boolean clauseNonConcurrenceDetectee;
            private boolean statutProtegeDetecte;
            private boolean transactionEnvisagee;
            private boolean atMpDetecte;
            private boolean urgenceProcedurale;
            private boolean contestationAreEnvisagee;
            private boolean harcelementBeDetecte;
            private boolean discriminationBeDetectee;
            private boolean inaptitudeMedicaleBeDetectee;
            private boolean heuresSupMentionneesBe;
            private boolean motifGraveBeEnvisage;
            private boolean abandonPosteDetecte;
            private boolean arretMaladieLongDetecte;
            private boolean priseActeEnvisagee;
            private boolean resiliationJudiciaireEnvisagee;
            private boolean forfaitJoursDetecte;
            private boolean transfertEntrepriseDetecte;
            private boolean fauteInexcusableEnvisagee;
            private boolean csCrpEnvisage;
            private boolean cspPropose;
            private boolean mutationRefusee;
            private boolean modificationContratRefusee;
            private boolean fauteGraveEnvisagee;
            private boolean fauteLourdeEnvisagee;
            private boolean cddRequalificationEnvisagee;
            private boolean interimRequalificationEnvisagee;
            private boolean forfaitJoursValiditeContestee;
            private boolean prescriptionProcheDetectee;
            private boolean ruptureAmiableNegociee;
            private boolean entretienPreavisObtenu;
            private boolean cseConsultationDemandee;
            private boolean irpElectionDemandee;
            private boolean inspectionTravailSaisie;
            private boolean mediationJudiciaireEnvisagee;
            private Boolean convocationEntretienDetectee;
            private String dateConvocationEntretienDetectee;
            private String dateEntretienPrealableDetectee;
            private DetectedAnswer entretienPrealableTenuDetected;
            private Boolean lettreLicenciementEcriteDetectee;
            private DetectedAnswer lettreLicenciementMotiveeDetected;
            private DetectedAnswer motivationLettreSuffisanteDetected;
            private Integer nonConcurrenceDureeMois;
            private String nonConcurrenceZoneGeographique;
            private Double nonConcurrenceContrepartieMontantEur;
            private Integer ageDemandeurAnnees;
            private String nonConcurrenceDatePriseEffet;
            private String nonConcurrenceSecteurActivite;
            private String dateRuptureContrat;
            private String motifRupture;
            // SF-207-02 — 6 champs IA Travail BE pour pré-fill F-207-02 C4 ONEM checklist.
            private String raisonSocialeEmployeur;
            private String numeroBce;
            private String categorieOnem;
            private String motifExplicite;
            private Integer preavisPresteJours;
            private java.math.BigDecimal dernierSalaireMensuelBrut;
            // SF-246-22 — type de procédure travail + date déclencheur pour pré-fill F-136.
            private String procedureTravailDetectee;
            private String dateDeclencheurProcedure;

            private Builder() {}

            public Builder conventionCollective(String v) { this.conventionCollective = v; return this; }
            public Builder dateEntree(String v) { this.dateEntree = v; return this; }
            public Builder salaireBrutMensuel(Double v) { this.salaireBrutMensuel = v; return this; }
            public Builder typeContrat(String v) { this.typeContrat = v; return this; }
            public Builder poste(String v) { this.poste = v; return this; }
            public Builder motifLicenciement(String v) { this.motifLicenciement = v; return this; }
            public Builder dateLicenciement(String v) { this.dateLicenciement = v; return this; }
            public Builder congesContractuels(Integer v) { this.congesContractuels = v; return this; }
            public Builder primeAncienneteContractuelle(Double v) { this.primeAncienneteContractuelle = v; return this; }
            public Builder nomSalarie(String v) { this.nomSalarie = v; return this; }
            public Builder prenomSalarie(String v) { this.prenomSalarie = v; return this; }
            public Builder adresseSalarie(String v) { this.adresseSalarie = v; return this; }
            public Builder nomEmployeur(String v) { this.nomEmployeur = v; return this; }
            public Builder adresseEmployeur(String v) { this.adresseEmployeur = v; return this; }
            public Builder siretEmployeur(String v) { this.siretEmployeur = v; return this; }
            public Builder bceEmployeur(String v) { this.bceEmployeur = v; return this; }
            public Builder representantEmployeur(String v) { this.representantEmployeur = v; return this; }
            public Builder salaireEstDeduit(Boolean v) { this.salaireEstDeduit = v; return this; }
            public Builder motifNullitePressenti(String v) { this.motifNullitePressenti = v; return this; }
            public Builder origineInaptitudePressentie(String v) { this.origineInaptitudePressentie = v; return this; }
            public Builder avisMedecinTravailDate(String v) { this.avisMedecinTravailDate = v; return this; }
            public Builder reclassementRespecteDetected(DetectedAnswer v) { this.reclassementRespecteDetected = v; return this; }
            public Builder heuresSupMentionneesDansDossier(HeuresSupMentionnees v) { this.heuresSupMentionneesDansDossier = v; return this; }
            public Builder rappelSalaireDetecte(boolean v) { this.rappelSalaireDetecte = v; return this; }
            public Builder travailDissimuleDetecte(boolean v) { this.travailDissimuleDetecte = v; return this; }
            public Builder clauseNonConcurrenceDetectee(boolean v) { this.clauseNonConcurrenceDetectee = v; return this; }
            public Builder statutProtegeDetecte(boolean v) { this.statutProtegeDetecte = v; return this; }
            public Builder transactionEnvisagee(boolean v) { this.transactionEnvisagee = v; return this; }
            public Builder atMpDetecte(boolean v) { this.atMpDetecte = v; return this; }
            public Builder urgenceProcedurale(boolean v) { this.urgenceProcedurale = v; return this; }
            public Builder contestationAreEnvisagee(boolean v) { this.contestationAreEnvisagee = v; return this; }
            public Builder harcelementBeDetecte(boolean v) { this.harcelementBeDetecte = v; return this; }
            public Builder discriminationBeDetectee(boolean v) { this.discriminationBeDetectee = v; return this; }
            public Builder inaptitudeMedicaleBeDetectee(boolean v) { this.inaptitudeMedicaleBeDetectee = v; return this; }
            public Builder heuresSupMentionneesBe(boolean v) { this.heuresSupMentionneesBe = v; return this; }
            public Builder motifGraveBeEnvisage(boolean v) { this.motifGraveBeEnvisage = v; return this; }
            public Builder abandonPosteDetecte(boolean v) { this.abandonPosteDetecte = v; return this; }
            public Builder arretMaladieLongDetecte(boolean v) { this.arretMaladieLongDetecte = v; return this; }
            public Builder priseActeEnvisagee(boolean v) { this.priseActeEnvisagee = v; return this; }
            public Builder resiliationJudiciaireEnvisagee(boolean v) { this.resiliationJudiciaireEnvisagee = v; return this; }
            public Builder forfaitJoursDetecte(boolean v) { this.forfaitJoursDetecte = v; return this; }
            public Builder transfertEntrepriseDetecte(boolean v) { this.transfertEntrepriseDetecte = v; return this; }
            public Builder fauteInexcusableEnvisagee(boolean v) { this.fauteInexcusableEnvisagee = v; return this; }
            public Builder csCrpEnvisage(boolean v) { this.csCrpEnvisage = v; return this; }
            public Builder cspPropose(boolean v) { this.cspPropose = v; return this; }
            public Builder mutationRefusee(boolean v) { this.mutationRefusee = v; return this; }
            public Builder modificationContratRefusee(boolean v) { this.modificationContratRefusee = v; return this; }
            public Builder fauteGraveEnvisagee(boolean v) { this.fauteGraveEnvisagee = v; return this; }
            public Builder fauteLourdeEnvisagee(boolean v) { this.fauteLourdeEnvisagee = v; return this; }
            public Builder cddRequalificationEnvisagee(boolean v) { this.cddRequalificationEnvisagee = v; return this; }
            public Builder interimRequalificationEnvisagee(boolean v) { this.interimRequalificationEnvisagee = v; return this; }
            public Builder forfaitJoursValiditeContestee(boolean v) { this.forfaitJoursValiditeContestee = v; return this; }
            public Builder prescriptionProcheDetectee(boolean v) { this.prescriptionProcheDetectee = v; return this; }
            public Builder ruptureAmiableNegociee(boolean v) { this.ruptureAmiableNegociee = v; return this; }
            public Builder entretienPreavisObtenu(boolean v) { this.entretienPreavisObtenu = v; return this; }
            public Builder cseConsultationDemandee(boolean v) { this.cseConsultationDemandee = v; return this; }
            public Builder irpElectionDemandee(boolean v) { this.irpElectionDemandee = v; return this; }
            public Builder inspectionTravailSaisie(boolean v) { this.inspectionTravailSaisie = v; return this; }
            public Builder mediationJudiciaireEnvisagee(boolean v) { this.mediationJudiciaireEnvisagee = v; return this; }
            public Builder convocationEntretienDetectee(Boolean v) { this.convocationEntretienDetectee = v; return this; }
            public Builder dateConvocationEntretienDetectee(String v) { this.dateConvocationEntretienDetectee = v; return this; }
            public Builder dateEntretienPrealableDetectee(String v) { this.dateEntretienPrealableDetectee = v; return this; }
            public Builder entretienPrealableTenuDetected(DetectedAnswer v) { this.entretienPrealableTenuDetected = v; return this; }
            public Builder lettreLicenciementEcriteDetectee(Boolean v) { this.lettreLicenciementEcriteDetectee = v; return this; }
            public Builder lettreLicenciementMotiveeDetected(DetectedAnswer v) { this.lettreLicenciementMotiveeDetected = v; return this; }
            public Builder motivationLettreSuffisanteDetected(DetectedAnswer v) { this.motivationLettreSuffisanteDetected = v; return this; }
            public Builder nonConcurrenceDureeMois(Integer v) { this.nonConcurrenceDureeMois = v; return this; }
            public Builder nonConcurrenceZoneGeographique(String v) { this.nonConcurrenceZoneGeographique = v; return this; }
            public Builder nonConcurrenceContrepartieMontantEur(Double v) { this.nonConcurrenceContrepartieMontantEur = v; return this; }
            public Builder ageDemandeurAnnees(Integer v) { this.ageDemandeurAnnees = v; return this; }
            public Builder nonConcurrenceDatePriseEffet(String v) { this.nonConcurrenceDatePriseEffet = v; return this; }
            public Builder nonConcurrenceSecteurActivite(String v) { this.nonConcurrenceSecteurActivite = v; return this; }
            public Builder dateRuptureContrat(String v) { this.dateRuptureContrat = v; return this; }
            public Builder motifRupture(String v) { this.motifRupture = v; return this; }
            public Builder raisonSocialeEmployeur(String v) { this.raisonSocialeEmployeur = v; return this; }
            public Builder numeroBce(String v) { this.numeroBce = v; return this; }
            public Builder categorieOnem(String v) { this.categorieOnem = v; return this; }
            public Builder motifExplicite(String v) { this.motifExplicite = v; return this; }
            public Builder preavisPresteJours(Integer v) { this.preavisPresteJours = v; return this; }
            public Builder dernierSalaireMensuelBrut(java.math.BigDecimal v) { this.dernierSalaireMensuelBrut = v; return this; }
            public Builder procedureTravailDetectee(String v) { this.procedureTravailDetectee = v; return this; }
            public Builder dateDeclencheurProcedure(String v) { this.dateDeclencheurProcedure = v; return this; }

            public TravailExtractedData build() {
                return new TravailExtractedData(
                        conventionCollective, dateEntree, salaireBrutMensuel,
                        typeContrat, poste, motifLicenciement, dateLicenciement,
                        congesContractuels, primeAncienneteContractuelle,
                        nomSalarie, prenomSalarie, adresseSalarie,
                        nomEmployeur, adresseEmployeur,
                        siretEmployeur, bceEmployeur,
                        representantEmployeur, salaireEstDeduit,
                        motifNullitePressenti, origineInaptitudePressentie,
                        avisMedecinTravailDate,
                        reclassementRespecteDetected,
                        heuresSupMentionneesDansDossier,
                        rappelSalaireDetecte, travailDissimuleDetecte, clauseNonConcurrenceDetectee,
                        statutProtegeDetecte, transactionEnvisagee, atMpDetecte,
                        urgenceProcedurale, contestationAreEnvisagee,
                        harcelementBeDetecte, discriminationBeDetectee, inaptitudeMedicaleBeDetectee,
                        heuresSupMentionneesBe, motifGraveBeEnvisage,
                        abandonPosteDetecte, arretMaladieLongDetecte, priseActeEnvisagee,
                        resiliationJudiciaireEnvisagee, forfaitJoursDetecte, transfertEntrepriseDetecte,
                        fauteInexcusableEnvisagee, csCrpEnvisage, cspPropose,
                        mutationRefusee, modificationContratRefusee, fauteGraveEnvisagee,
                        fauteLourdeEnvisagee, cddRequalificationEnvisagee, interimRequalificationEnvisagee,
                        forfaitJoursValiditeContestee, prescriptionProcheDetectee, ruptureAmiableNegociee,
                        entretienPreavisObtenu, cseConsultationDemandee, irpElectionDemandee,
                        inspectionTravailSaisie, mediationJudiciaireEnvisagee,
                        convocationEntretienDetectee, dateConvocationEntretienDetectee,
                        dateEntretienPrealableDetectee, entretienPrealableTenuDetected,
                        lettreLicenciementEcriteDetectee, lettreLicenciementMotiveeDetected,
                        motivationLettreSuffisanteDetected,
                        nonConcurrenceDureeMois, nonConcurrenceZoneGeographique,
                        nonConcurrenceContrepartieMontantEur,
                        ageDemandeurAnnees,
                        nonConcurrenceDatePriseEffet, nonConcurrenceSecteurActivite,
                        dateRuptureContrat, motifRupture,
                        raisonSocialeEmployeur, numeroBce, categorieOnem,
                        motifExplicite, preavisPresteJours, dernierSalaireMensuelBrut,
                        procedureTravailDetectee, dateDeclencheurProcedure);
            }
        }
    }

    /**
     * SF-155-04-00-BE-travail : agrégat des heures supplémentaires mentionnées dans le dossier
     * (bulletins de paie, décomptes, attestations), pour pré-remplir F-DT-19 heures sup.
     * Tous les champs nullables — chaque catégorie est indépendante.
     */
    public record HeuresSupMentionnees(
            Integer totalDeclarees25pct,
            Integer totalDeclarees50pct,
            Integer horsContingent) {}

    public record DetectedAnswer(String reponse, String justification) {}

    public record LicenciementValidityDetection(Map<String, DetectedAnswer> detections) {
        public LicenciementValidityDetection {
            detections = detections == null ? Map.of() : Map.copyOf(detections);
        }
    }

    public record RuptureConvValidityDetection(Map<String, DetectedAnswer> detections) {
        public RuptureConvValidityDetection {
            detections = detections == null ? Map.of() : Map.copyOf(detections);
        }
    }

    /**
     * F-152 SF-152-01 : détection IA des critères de validité du divorce par
     * consentement mutuel (art. 229 Cciv, 7 critères).
     */
    public record DivorceConsentementValidityDetection(Map<String, DetectedAnswer> detections) {
        public DivorceConsentementValidityDetection {
            detections = detections == null ? Map.of() : Map.copyOf(detections);
        }
    }

    /** F-152 SF-152-01 : scoring calculé 0-100 + verdict risque annulation. */
    public record DivorceConsentementScoring(
            int score,
            String verdict,
            List<String> criteresValides,
            List<String> criteresNonValides,
            List<String> criteresInconnus
    ) {
        public DivorceConsentementScoring {
            criteresValides = criteresValides == null ? List.of() : List.copyOf(criteresValides);
            criteresNonValides = criteresNonValides == null ? List.of() : List.copyOf(criteresNonValides);
            criteresInconnus = criteresInconnus == null ? List.of() : List.copyOf(criteresInconnus);
        }
    }

    static final Set<String> DIVORCE_CONSENTEMENT_CRITERE_CODES = Set.of(
            "DC_MAJORITE", "DC_CONSENTEMENT_LIBRE", "DC_CONVENTION_EQUITABLE",
            "DC_ENFANT_MINEUR_ENTENDU", "DC_DELAI_REFLEXION_15J",
            "DC_NOTAIRE_DEPOT", "DC_INDEPENDANCE_AVOCATS"
    );

    static final Set<String> RUPTURE_CONV_CRITERE_CODES = Set.of(
            "RC_CONSENTEMENT", "RC_DELAI_RETRACTATION", "RC_HOMOLOGATION",
            "RC_ASSISTANCE", "RC_INDEMNITE", "RC_ENTRETIENS"
    );

    static final Set<String> LICENCIEMENT_CRITERE_CODES = Set.of(
            "FR_CONVOCATION", "FR_ENTRETIEN", "FR_DELAI_NOTIFICATION", "FR_MOTIVATION",
            "FR_MOTIF_REEL", "FR_PROCEDURE_DISCIPLINAIRE", "FR_ORDRE_LICENCIEMENT",
            "BE_NOTIFICATION", "BE_PREAVIS", "BE_MOTIVATION", "BE_AUDITION",
            "BE_NON_DISCRIMINATION", "BE_PROTECTION_SPECIALE", "BE_INDEMNITE_MANIFESTE"
    );

    /** SF-155-04-00-BE-travail : codes de motif de nullité FR pour pré-fill F-DT-11. */
    static final Set<String> MOTIFS_NULLITE_CODES = Set.of(
            "DISCRIMINATION", "HARCELEMENT_MORAL", "HARCELEMENT_SEXUEL",
            "RETORSION", "SYNDICAL", "MATERNITE_PATERNITE", "ACCIDENT_MP"
    );

    /** SF-155-04-00-BE-travail : codes d'origine d'inaptitude FR pour pré-fill F-DT-15. */
    static final Set<String> ORIGINE_INAPTITUDE_CODES = Set.of(
            "ACCIDENT_TRAVAIL", "MALADIE_PROFESSIONNELLE", "MALADIE_ORDINAIRE"
    );

    /**
     * SF-246-13 : codes de secteur d'activité pour pré-fill F-DT-24 (clause de
     * non-concurrence). Liste alignée sur l'enum {@code NonConcurrenceCalculator.SecteurActivite}
     * et sur {@code SECTEUR_ACTIVITE_OPTIONS} du front (non-concurrence.model.ts)
     * pour un pré-fill direct sans mapping intermédiaire. Un code hors liste
     * renvoyé par le LLM est ramené à {@code null} par {@code normalizeEnumCode}.
     */
    static final Set<String> SECTEUR_ACTIVITE_CODES = Set.of(
            "INFORMATIQUE", "COMMERCE", "INDUSTRIE", "SERVICES", "AUTRE"
    );

    /**
     * SF-155-04-00-BE-immig-FR : codes de motif OQTF FR pour pré-fill F-IM-08-02.
     * Liste alignée sur l'enum {@code MotifOqtf} du front (oqtf-avec-delai.model.ts)
     * pour un pré-fill direct sans mapping intermédiaire.
     */
    static final Set<String> MOTIFS_OQTF_FR_CODES = Set.of(
            "REFUS_TITRE", "EXPIRATION_TITRE", "SEJOUR_IRREGULIER", "RETRAIT_TITRE", "AUTRE"
    );

    /**
     * SF-155-04-00-BE-immig-FR : regex permissive pour valider l'horodatage de l'OQTF
     * sans délai (format ISO partiel YYYY-MM-DDTHH:mm ou YYYY-MM-DDTHH:mm:ss).
     */
    private static final java.util.regex.Pattern OQTF_SANS_DELAI_DATETIME_PATTERN =
            java.util.regex.Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?$");

    static final int MAX_JUSTIFICATION_LENGTH = 500;

    /**
     * SF-246-02 : durée maximale plausible d'une clause de non-concurrence en mois
     * (50 ans). Au-delà, la valeur est jugée aberrante par {@code boundedIntOrNull}
     * et ramenée à {@code null} — invariant mini-spec « garde de plage [0, 600] ».
     */
    static final int MAX_NON_CONCURRENCE_DUREE_MOIS = 600;

    /** SF-246-02 : longueur maximale de la zone géographique de la clause de non-concurrence. */
    static final int MAX_NON_CONCURRENCE_ZONE_LENGTH = 500;

    /**
     * SF-246-05 : âge maximal plausible du demandeur d'un crédit-temps fin de carrière.
     * Toute valeur hors de la plage {@code [0, 100]} est jugée aberrante par
     * {@code boundedIntOrNull} et ramenée à {@code null} — invariant mini-spec.
     */
    static final int MAX_AGE_DEMANDEUR_ANNEES = 100;

    public record ImmigrationExtractedData(
            String dateExpirationTitre, String typeTitreSejour,
            String typeProcedureDetectee, String dateDepotProcedure,
            String typeTitreSejourCode, Boolean nationaliteUe,
            String typeRecoursCode, String dateNotificationDecisionContestee,
            /** SF-IM-01-04 : type de checklist pré-sélectionné (F-IM-01). */
            String inferredChecklistType,
            // SF-155-04-00-BE-immig-FR : 5 champs IA pour pré-fill outils OQTF FR F-IM-08-02 / F-IM-08-04.
            // Tous nullables — la plupart des dossiers immigration BE ne portent pas ces concepts FR.
            String dateNotificationOqtf,
            String motifOqtfCode,
            DetectedAnswer recoursFormeDetected,
            String dateHeureNotificationOqtfSansDelai,
            Boolean placementCraDetected,
            // SF-155-04-00-BE-immig-BE : 4 champs IA pour pré-fill F-IM-08 Annexe 13 BE.
            // Tous nullables et BELGIQUE uniquement — dossiers FRANCE laissent ces 4 champs à null
            // (l'OQTF française est traitée par les 5 champs FR ci-dessus).
            String dateNotificationAnnexe13,
            Integer delaiDepartImposeJours,
            String motifOqtCodeBe,
            Boolean transfertImminentDetected,
            // === Flags FR (F-201) ===
            // F-201 : 9 flags décisionnels niveau 3 — Immigration FRANCE uniquement, default false.
            // Permettent à F-IA-04 de basculer 10 outils Immigration FR ALWAYS_ON → CONTEXTUAL.
            // Régime algérien : utilise nationalite='Algérienne' déjà extrait, pas de nouveau flag.
            // Dossiers BE : tous false (régimes BE équivalents distincts → F-203).
            boolean aesMetiersTensionEligibleDetecte,
            boolean aesFamilialEligibleDetecte,
            boolean aesHumanitaireEligibleDetecte,
            boolean aesEtudiantEligibleDetecte,
            boolean changementStatutEnvisageDetecte,
            boolean procedureAsileDetectee,
            boolean naturalisationEnvisageeDetectee,
            boolean clientMineurDetecte,
            boolean mesureEloignementDetectee,
            // === Flags BE (F-203) ===
            // F-203 : 5 flags décisionnels niveau 3 — Immigration BELGIQUE uniquement, default false.
            // Permettent à F-IA-04 de basculer 5 outils Immigration BE ALWAYS_ON → CONTEXTUAL.
            // Dossiers FR : tous false (régimes FR équivalents distincts → F-201).
            boolean procedure9bisEnvisagee,
            boolean procedure9terMedicaleDetectee,
            boolean regroupement40bisDetecte,
            boolean regroupement40terDetecte,
            boolean oqtAnnexe13Detectee,
            // === F-235 nationalite ===
            // Champ texte libre (ex. "Algérienne", "Tunisienne", "Française") consommé par
            // DecisionToolVisibilityService pour activer les outils conditionnés à une
            // nationalité bilatérale (F-IM-17 régime franco-algérien). Nullable —
            // dossiers où l'IA n'identifie pas la nationalité ou non-immigration : null.
            // Distinct de nationaliteUe (booléen UE vs pays tiers).
            String nationalite,
            // SF-246-04 : date de l'ordonnance de protection JAF (Cciv 515-9) pour pré-fill F-IM-24
            // victime de violences L.425-6. Immigration FRANCE uniquement, nullable — dossier BE : null.
            String dateOrdonnanceProtectionJaf,
            // SF-246-16 : identité requérant + référence décision contestée pour pré-fill F-IM-06 recours.
            // Tous nullables — absents si l'IA ne peut les extraire des pièces.
            /** Nom de famille du requérant (texte libre). */
            String nomRequerant,
            /** Prénom du requérant (texte libre). */
            String prenomRequerant,
            /** Date de la décision contestée au format YYYY-MM-DD. */
            String dateDecisionContestee,
            /** Référence ou numéro de la décision contestée (texte libre). */
            String referenceDecision,
            // SF-246-17 : pré-fill des outils dublin-recours (F-IM-22) et crrv-refus-visa (F-IM-23).
            // FRANCE uniquement, tous nullables. Absents si l'IA ne peut identifier la décision.
            /** État membre UE responsable de la demande d'asile (Dublin III, art. 3 Règl. 604/2013). Texte libre ≤ 200 car. */
            String dublinEtatMembreResponsable,
            /** Motif de transfert Dublin normalisé — l'un des 5 codes (null si non identifiable). */
            String dublinMotifTransfert,
            /** Type de visa refusé CRRV — l'un des 5 codes TypeVisaCrrv (null si non identifiable). */
            String crrvTypeVisa,
            /** Motif du refus de visa en texte libre ≤ 500 car. (null si absent des pièces). */
            String crrvMotifRefus,
            // SF-246-18 : pré-fill outils AES Immigration FR (aes-etudiant / aes-famille /
            // aes-humanitaire / aes-metiers-tension). FRANCE uniquement — null pour dossiers BE.
            /** Date d'entrée en France extraite du passeport / visa d'entrée (YYYY-MM-DD, non future). */
            String aesDateEntreeFrance,
            /** Mois entiers écoulés depuis aesDateEntreeFrance jusqu'à aujourd'hui (calculé côté extracteur). */
            Integer aesDureePresenceMois,
            /** Années d'études consécutives en France (extrait du certificat de scolarité). */
            Integer aesAnneesScolariteConsecutives,
            /**
             * Niveau d'études le plus élevé mentionné dans les pièces.
             * Whitelist : LYCEE / BAC_PLUS_1_2 / BAC_PLUS_3_4 / BAC_PLUS_5_PLUS.
             */
            String aesNiveauEtudes,
            /** Durée de scolarité en France de l'enfant le plus anciennement inscrit (années entières). */
            Integer aesDureeScolaritePlusAncienEnfantAnnees,
            /**
             * Motif humanitaire dominant extrait des pièces.
             * Whitelist 6 codes : RISQUES_AU_RETOUR / ISOLEMENT_TOTAL / VICTIME_VIOLENCES /
             * VICTIME_TRAITE / SITUATION_MEDICALE_PRECAIRE_HORS_L425_9 / AUTRE_HUMANITAIRE.
             */
            String aesMotifHumanitaire,
            /** Mois de travail salarié dans les 24 derniers mois (0–24, extrait des bulletins de paie). */
            Integer aesMoisActiviteSalariee,
            /** Code ROME ou libellé du métier en tension (texte libre, extrait de l'attestation employeur). */
            String aesCodeMetier,
            // SF-246-19 : pré-fill statut & dispositifs Immigration FR
            // (changement-statut / naturalisation / mineurs / régime algérien / asile avancé / mesures d'éloignement).
            // FRANCE uniquement — null pour dossiers BE. Tous nullables.
            /** Titre de séjour envisagé par le demandeur (même whitelist que titreActuel). */
            String changementTitreEnvisage,
            /** Rémunération du contrat visé en euros bruts annuels (> 0, ≤ 500 000). */
            Integer changementRemunerationEur,
            /** Durée de résidence régulière en France en années entières (voie DECRET, ASCENDANT). */
            Integer natDureeResidenceReguliereAnnees,
            /** Durée du mariage en années entières (voie MARIAGE). */
            Integer natDureeMariageAnnees,
            /** Âge du demandeur en années entières (voie ASCENDANT). */
            Integer natAgeDemandeur,
            /** Date de naissance du mineur au format YYYY-MM-DD (non future, extraction acte de naissance). */
            String mineursDateNaissance,
            /** Durée de présence régulière en France en mois entiers (régime algérien CRA — accord 1968). */
            Integer algerienPresenceReguliereMois,
            /** Date de la décision antérieure sur demande d'asile (YYYY-MM-DD, non future). */
            String asileDateDecisionAnterieure,
            /** Durée de présence irrégulière en France en mois entiers (IRTF art. L.612-6+). */
            Integer eloiDureePresenceIrreguliereMois,
            /**
             * Motif de menace pour l'ordre public / sécurité.
             * Whitelist 5 codes : ORDRE_PUBLIC / SECURITE_ETAT / TERRORISME / RECIDIVE_GRAVE / AUTRE.
             */
            String eloiMotifMenace) {

        /**
         * F-234 SF-234-01 : Builder pattern pour {@link ImmigrationExtractedData}.
         */
        public static Builder builder() {
            return new Builder();
        }

        /** F-234 SF-234-01 : copie un record existant pour ajuster un sous-ensemble de champs. */
        public Builder toBuilder() {
            return new Builder()
                    .dateExpirationTitre(dateExpirationTitre)
                    .typeTitreSejour(typeTitreSejour)
                    .typeProcedureDetectee(typeProcedureDetectee)
                    .dateDepotProcedure(dateDepotProcedure)
                    .typeTitreSejourCode(typeTitreSejourCode)
                    .nationaliteUe(nationaliteUe)
                    .typeRecoursCode(typeRecoursCode)
                    .dateNotificationDecisionContestee(dateNotificationDecisionContestee)
                    .inferredChecklistType(inferredChecklistType)
                    .dateNotificationOqtf(dateNotificationOqtf)
                    .motifOqtfCode(motifOqtfCode)
                    .recoursFormeDetected(recoursFormeDetected)
                    .dateHeureNotificationOqtfSansDelai(dateHeureNotificationOqtfSansDelai)
                    .placementCraDetected(placementCraDetected)
                    .dateNotificationAnnexe13(dateNotificationAnnexe13)
                    .delaiDepartImposeJours(delaiDepartImposeJours)
                    .motifOqtCodeBe(motifOqtCodeBe)
                    .transfertImminentDetected(transfertImminentDetected)
                    .aesMetiersTensionEligibleDetecte(aesMetiersTensionEligibleDetecte)
                    .aesFamilialEligibleDetecte(aesFamilialEligibleDetecte)
                    .aesHumanitaireEligibleDetecte(aesHumanitaireEligibleDetecte)
                    .aesEtudiantEligibleDetecte(aesEtudiantEligibleDetecte)
                    .changementStatutEnvisageDetecte(changementStatutEnvisageDetecte)
                    .procedureAsileDetectee(procedureAsileDetectee)
                    .naturalisationEnvisageeDetectee(naturalisationEnvisageeDetectee)
                    .clientMineurDetecte(clientMineurDetecte)
                    .mesureEloignementDetectee(mesureEloignementDetectee)
                    .procedure9bisEnvisagee(procedure9bisEnvisagee)
                    .procedure9terMedicaleDetectee(procedure9terMedicaleDetectee)
                    .regroupement40bisDetecte(regroupement40bisDetecte)
                    .regroupement40terDetecte(regroupement40terDetecte)
                    .oqtAnnexe13Detectee(oqtAnnexe13Detectee)
                    .nationalite(nationalite)
                    .dateOrdonnanceProtectionJaf(dateOrdonnanceProtectionJaf)
                    .nomRequerant(nomRequerant)
                    .prenomRequerant(prenomRequerant)
                    .dateDecisionContestee(dateDecisionContestee)
                    .referenceDecision(referenceDecision)
                    // SF-246-17
                    .dublinEtatMembreResponsable(dublinEtatMembreResponsable)
                    .dublinMotifTransfert(dublinMotifTransfert)
                    .crrvTypeVisa(crrvTypeVisa)
                    .crrvMotifRefus(crrvMotifRefus)
                    // SF-246-18 : champs AES Immigration FR
                    .aesDateEntreeFrance(aesDateEntreeFrance)
                    .aesDureePresenceMois(aesDureePresenceMois)
                    .aesAnneesScolariteConsecutives(aesAnneesScolariteConsecutives)
                    .aesNiveauEtudes(aesNiveauEtudes)
                    .aesDureeScolaritePlusAncienEnfantAnnees(aesDureeScolaritePlusAncienEnfantAnnees)
                    .aesMotifHumanitaire(aesMotifHumanitaire)
                    .aesMoisActiviteSalariee(aesMoisActiviteSalariee)
                    .aesCodeMetier(aesCodeMetier)
                    // SF-246-19 : pré-fill statut & dispositifs Immigration FR
                    .changementTitreEnvisage(changementTitreEnvisage)
                    .changementRemunerationEur(changementRemunerationEur)
                    .natDureeResidenceReguliereAnnees(natDureeResidenceReguliereAnnees)
                    .natDureeMariageAnnees(natDureeMariageAnnees)
                    .natAgeDemandeur(natAgeDemandeur)
                    .mineursDateNaissance(mineursDateNaissance)
                    .algerienPresenceReguliereMois(algerienPresenceReguliereMois)
                    .asileDateDecisionAnterieure(asileDateDecisionAnterieure)
                    .eloiDureePresenceIrreguliereMois(eloiDureePresenceIrreguliereMois)
                    .eloiMotifMenace(eloiMotifMenace);
        }

        public static final class Builder {
            private String dateExpirationTitre;
            private String typeTitreSejour;
            private String typeProcedureDetectee;
            private String dateDepotProcedure;
            private String typeTitreSejourCode;
            private Boolean nationaliteUe;
            private String typeRecoursCode;
            private String dateNotificationDecisionContestee;
            private String inferredChecklistType;
            private String dateNotificationOqtf;
            private String motifOqtfCode;
            private DetectedAnswer recoursFormeDetected;
            private String dateHeureNotificationOqtfSansDelai;
            private Boolean placementCraDetected;
            private String dateNotificationAnnexe13;
            private Integer delaiDepartImposeJours;
            private String motifOqtCodeBe;
            private Boolean transfertImminentDetected;
            private boolean aesMetiersTensionEligibleDetecte;
            private boolean aesFamilialEligibleDetecte;
            private boolean aesHumanitaireEligibleDetecte;
            private boolean aesEtudiantEligibleDetecte;
            private boolean changementStatutEnvisageDetecte;
            private boolean procedureAsileDetectee;
            private boolean naturalisationEnvisageeDetectee;
            private boolean clientMineurDetecte;
            private boolean mesureEloignementDetectee;
            private boolean procedure9bisEnvisagee;
            private boolean procedure9terMedicaleDetectee;
            private boolean regroupement40bisDetecte;
            private boolean regroupement40terDetecte;
            private boolean oqtAnnexe13Detectee;
            private String nationalite;
            private String dateOrdonnanceProtectionJaf;
            // SF-246-16 : identité requérant + référence décision contestée
            private String nomRequerant;
            private String prenomRequerant;
            private String dateDecisionContestee;
            private String referenceDecision;
            // SF-246-17 : pré-fill dublin-recours + crrv-refus-visa
            private String dublinEtatMembreResponsable;
            private String dublinMotifTransfert;
            private String crrvTypeVisa;
            private String crrvMotifRefus;
            // SF-246-18 : pré-fill outils AES Immigration FR
            private String aesDateEntreeFrance;
            private Integer aesDureePresenceMois;
            private Integer aesAnneesScolariteConsecutives;
            private String aesNiveauEtudes;
            private Integer aesDureeScolaritePlusAncienEnfantAnnees;
            private String aesMotifHumanitaire;
            private Integer aesMoisActiviteSalariee;
            private String aesCodeMetier;
            // SF-246-19 : pré-fill statut & dispositifs Immigration FR
            private String changementTitreEnvisage;
            private Integer changementRemunerationEur;
            private Integer natDureeResidenceReguliereAnnees;
            private Integer natDureeMariageAnnees;
            private Integer natAgeDemandeur;
            private String mineursDateNaissance;
            private Integer algerienPresenceReguliereMois;
            private String asileDateDecisionAnterieure;
            private Integer eloiDureePresenceIrreguliereMois;
            private String eloiMotifMenace;

            private Builder() {}

            public Builder dateExpirationTitre(String v) { this.dateExpirationTitre = v; return this; }
            public Builder typeTitreSejour(String v) { this.typeTitreSejour = v; return this; }
            public Builder typeProcedureDetectee(String v) { this.typeProcedureDetectee = v; return this; }
            public Builder dateDepotProcedure(String v) { this.dateDepotProcedure = v; return this; }
            public Builder typeTitreSejourCode(String v) { this.typeTitreSejourCode = v; return this; }
            public Builder nationaliteUe(Boolean v) { this.nationaliteUe = v; return this; }
            public Builder typeRecoursCode(String v) { this.typeRecoursCode = v; return this; }
            public Builder dateNotificationDecisionContestee(String v) { this.dateNotificationDecisionContestee = v; return this; }
            public Builder inferredChecklistType(String v) { this.inferredChecklistType = v; return this; }
            public Builder dateNotificationOqtf(String v) { this.dateNotificationOqtf = v; return this; }
            public Builder motifOqtfCode(String v) { this.motifOqtfCode = v; return this; }
            public Builder recoursFormeDetected(DetectedAnswer v) { this.recoursFormeDetected = v; return this; }
            public Builder dateHeureNotificationOqtfSansDelai(String v) { this.dateHeureNotificationOqtfSansDelai = v; return this; }
            public Builder placementCraDetected(Boolean v) { this.placementCraDetected = v; return this; }
            public Builder dateNotificationAnnexe13(String v) { this.dateNotificationAnnexe13 = v; return this; }
            public Builder delaiDepartImposeJours(Integer v) { this.delaiDepartImposeJours = v; return this; }
            public Builder motifOqtCodeBe(String v) { this.motifOqtCodeBe = v; return this; }
            public Builder transfertImminentDetected(Boolean v) { this.transfertImminentDetected = v; return this; }
            public Builder aesMetiersTensionEligibleDetecte(boolean v) { this.aesMetiersTensionEligibleDetecte = v; return this; }
            public Builder aesFamilialEligibleDetecte(boolean v) { this.aesFamilialEligibleDetecte = v; return this; }
            public Builder aesHumanitaireEligibleDetecte(boolean v) { this.aesHumanitaireEligibleDetecte = v; return this; }
            public Builder aesEtudiantEligibleDetecte(boolean v) { this.aesEtudiantEligibleDetecte = v; return this; }
            public Builder changementStatutEnvisageDetecte(boolean v) { this.changementStatutEnvisageDetecte = v; return this; }
            public Builder procedureAsileDetectee(boolean v) { this.procedureAsileDetectee = v; return this; }
            public Builder naturalisationEnvisageeDetectee(boolean v) { this.naturalisationEnvisageeDetectee = v; return this; }
            public Builder clientMineurDetecte(boolean v) { this.clientMineurDetecte = v; return this; }
            public Builder mesureEloignementDetectee(boolean v) { this.mesureEloignementDetectee = v; return this; }
            public Builder procedure9bisEnvisagee(boolean v) { this.procedure9bisEnvisagee = v; return this; }
            public Builder procedure9terMedicaleDetectee(boolean v) { this.procedure9terMedicaleDetectee = v; return this; }
            public Builder regroupement40bisDetecte(boolean v) { this.regroupement40bisDetecte = v; return this; }
            public Builder regroupement40terDetecte(boolean v) { this.regroupement40terDetecte = v; return this; }
            public Builder oqtAnnexe13Detectee(boolean v) { this.oqtAnnexe13Detectee = v; return this; }
            public Builder nationalite(String v) { this.nationalite = v; return this; }
            public Builder dateOrdonnanceProtectionJaf(String v) { this.dateOrdonnanceProtectionJaf = v; return this; }
            public Builder nomRequerant(String v) { this.nomRequerant = v; return this; }
            public Builder prenomRequerant(String v) { this.prenomRequerant = v; return this; }
            public Builder dateDecisionContestee(String v) { this.dateDecisionContestee = v; return this; }
            public Builder referenceDecision(String v) { this.referenceDecision = v; return this; }
            // SF-246-17
            public Builder dublinEtatMembreResponsable(String v) { this.dublinEtatMembreResponsable = v; return this; }
            public Builder dublinMotifTransfert(String v) { this.dublinMotifTransfert = v; return this; }
            public Builder crrvTypeVisa(String v) { this.crrvTypeVisa = v; return this; }
            public Builder crrvMotifRefus(String v) { this.crrvMotifRefus = v; return this; }
            // SF-246-18 : champs AES Immigration FR
            public Builder aesDateEntreeFrance(String v) { this.aesDateEntreeFrance = v; return this; }
            public Builder aesDureePresenceMois(Integer v) { this.aesDureePresenceMois = v; return this; }
            public Builder aesAnneesScolariteConsecutives(Integer v) { this.aesAnneesScolariteConsecutives = v; return this; }
            public Builder aesNiveauEtudes(String v) { this.aesNiveauEtudes = v; return this; }
            public Builder aesDureeScolaritePlusAncienEnfantAnnees(Integer v) { this.aesDureeScolaritePlusAncienEnfantAnnees = v; return this; }
            public Builder aesMotifHumanitaire(String v) { this.aesMotifHumanitaire = v; return this; }
            public Builder aesMoisActiviteSalariee(Integer v) { this.aesMoisActiviteSalariee = v; return this; }
            public Builder aesCodeMetier(String v) { this.aesCodeMetier = v; return this; }
            // SF-246-19 : pré-fill statut & dispositifs Immigration FR
            public Builder changementTitreEnvisage(String v) { this.changementTitreEnvisage = v; return this; }
            public Builder changementRemunerationEur(Integer v) { this.changementRemunerationEur = v; return this; }
            public Builder natDureeResidenceReguliereAnnees(Integer v) { this.natDureeResidenceReguliereAnnees = v; return this; }
            public Builder natDureeMariageAnnees(Integer v) { this.natDureeMariageAnnees = v; return this; }
            public Builder natAgeDemandeur(Integer v) { this.natAgeDemandeur = v; return this; }
            public Builder mineursDateNaissance(String v) { this.mineursDateNaissance = v; return this; }
            public Builder algerienPresenceReguliereMois(Integer v) { this.algerienPresenceReguliereMois = v; return this; }
            public Builder asileDateDecisionAnterieure(String v) { this.asileDateDecisionAnterieure = v; return this; }
            public Builder eloiDureePresenceIrreguliereMois(Integer v) { this.eloiDureePresenceIrreguliereMois = v; return this; }
            public Builder eloiMotifMenace(String v) { this.eloiMotifMenace = v; return this; }

            public ImmigrationExtractedData build() {
                return new ImmigrationExtractedData(
                        dateExpirationTitre, typeTitreSejour, typeProcedureDetectee, dateDepotProcedure,
                        typeTitreSejourCode, nationaliteUe, typeRecoursCode, dateNotificationDecisionContestee,
                        inferredChecklistType,
                        dateNotificationOqtf, motifOqtfCode, recoursFormeDetected,
                        dateHeureNotificationOqtfSansDelai, placementCraDetected,
                        dateNotificationAnnexe13, delaiDepartImposeJours, motifOqtCodeBe, transfertImminentDetected,
                        aesMetiersTensionEligibleDetecte, aesFamilialEligibleDetecte,
                        aesHumanitaireEligibleDetecte, aesEtudiantEligibleDetecte,
                        changementStatutEnvisageDetecte, procedureAsileDetectee,
                        naturalisationEnvisageeDetectee, clientMineurDetecte, mesureEloignementDetectee,
                        procedure9bisEnvisagee, procedure9terMedicaleDetectee,
                        regroupement40bisDetecte, regroupement40terDetecte, oqtAnnexe13Detectee,
                        nationalite, dateOrdonnanceProtectionJaf,
                        nomRequerant, prenomRequerant, dateDecisionContestee, referenceDecision,
                        dublinEtatMembreResponsable, dublinMotifTransfert, crrvTypeVisa, crrvMotifRefus,
                        aesDateEntreeFrance, aesDureePresenceMois, aesAnneesScolariteConsecutives,
                        aesNiveauEtudes, aesDureeScolaritePlusAncienEnfantAnnees,
                        aesMotifHumanitaire, aesMoisActiviteSalariee, aesCodeMetier,
                        changementTitreEnvisage, changementRemunerationEur,
                        natDureeResidenceReguliereAnnees, natDureeMariageAnnees, natAgeDemandeur,
                        mineursDateNaissance, algerienPresenceReguliereMois,
                        asileDateDecisionAnterieure, eloiDureePresenceIrreguliereMois,
                        eloiMotifMenace);
            }
        }
    }

    static final Set<String> IMMIGRATION_TITLE_CODES = Set.of(
            "VLS_TS_ETUDIANT", "VLS_TS_SALARIE", "CST_SALARIE", "CARTE_PLURIANNUELLE",
            // SF-IM-07-04 : sous-types explicites de la carte pluriannuelle (droit
            // au travail différent selon le motif).
            "CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE", "CARTE_PLURIANNUELLE_SALARIE",
            "CARTE_PLURIANNUELLE_PASSEPORT_TALENT", "CARTE_PLURIANNUELLE_VPF",
            "CARTE_RESIDENT", "APS", "CST_VPF",
            // SF-IM-07-04 : code parallèle à CST_VPF pour conjoint de Français (L.423-1).
            "CST_VPF_CONJOINT_FR",
            "RECEPISSE_ASILE",
            "CARTE_A_TRAVAIL", "CARTE_A_ETUDES", "CARTE_A_FAMILLE",
            "CARTE_B", "CARTE_C", "PERMIS_UNIQUE", "ANNEXE_15", "ATTESTATION_IMMATRICULATION"
    );

    static final Set<String> IMMIGRATION_RECOURS_CODES = Set.of(
            "RECOURS_GRACIEUX_PREFET", "RECOURS_CONTENTIEUX_TA", "RECOURS_CNDA",
            "RECOURS_CGRA", "RECOURS_CCE", "RECOURS_CE_BELGIQUE"
    );

    /**
     * SF-155-04-00-BE-immig-BE : codes de motif OQT belges pour pré-fill F-IM-08 Annexe 13.
     * Exactement alignés sur {@code Annexe13BeCalculator.MOTIFS_VALIDES} (art. 7 Loi 15/12/1980)
     * — toute divergence casserait le pré-fill en silence côté frontend.
     */
    static final Set<String> MOTIFS_OQT_BE_CODES = Set.of(
            "SEJOUR_IRREGULIER_ART_7", "REFUS_SEJOUR_APRES_DEMANDE",
            "FIN_SEJOUR_REGULIER", "AUTRE"
    );

    /**
     * SF-246-17 : codes de motif de transfert Dublin III (Règl. UE 604/2013) pour pré-fill
     * F-IM-22 (dublin-recours). Alignés sur l'enum {@code MotifTransfertDublin} du frontend
     * (dublin-recours.model.ts) — toute divergence casserait le pré-fill en silence.
     */
    static final Set<String> MOTIFS_TRANSFERT_DUBLIN_CODES = Set.of(
            "DEMANDE_ASILE_AUTRE_ETAT", "VISA_DELIVRE_AUTRE_ETAT",
            "ENTREE_IRREGULIERE_AUTRE_ETAT", "MEMBRE_FAMILLE_AUTRE_ETAT", "AUTRE"
    );

    /**
     * SF-246-17 : codes de type de visa CRRV pour pré-fill F-IM-23 (crrv-refus-visa).
     * Alignés sur l'enum {@code TypeVisaCrrv} du frontend (crrv-refus-visa.model.ts).
     */
    static final Set<String> TYPES_VISA_CRRV_CODES = Set.of(
            "COURT_SEJOUR", "LONG_SEJOUR", "REGROUPEMENT_FAMILIAL", "ETUDIANT", "AUTRE"
    );

    /** SF-246-17 : longueur max du motif de refus CRRV (texte libre). */
    static final int MAX_CRRV_MOTIF_REFUS_LENGTH = 500;

    /** SF-246-17 : longueur max de l'état membre Dublin (texte libre). */
    static final int MAX_DUBLIN_ETAT_MEMBRE_LENGTH = 200;

    /**
     * SF-246-18 : niveaux d'études AES étudiant (whitelist 4 codes).
     * Alignés sur le type {@code AesEtudiantNiveauEtudes} frontend.
     */
    static final Set<String> AES_NIVEAU_ETUDES_CODES = Set.of(
            "LYCEE", "BAC_PLUS_1_2", "BAC_PLUS_3_4", "BAC_PLUS_5_PLUS"
    );

    /**
     * SF-246-18 : motifs humanitaires AES (whitelist 6 codes).
     * Alignés sur le type {@code MotifHumanitaire} frontend.
     */
    static final Set<String> AES_MOTIFS_HUMANITAIRES_CODES = Set.of(
            "RISQUES_AU_RETOUR", "ISOLEMENT_TOTAL", "VICTIME_VIOLENCES",
            "VICTIME_TRAITE", "SITUATION_MEDICALE_PRECAIRE_HORS_L425_9", "AUTRE_HUMANITAIRE"
    );

    /**
     * SF-246-19 : motifs de menace pour mesures d'éloignement (whitelist 5 codes).
     * Alignés sur {@code MotifMenaceCode} frontend (mesures-eloignement.model.ts).
     */
    static final Set<String> ELOI_MOTIFS_MENACE_CODES = Set.of(
            "ORDRE_PUBLIC", "SECURITE_ETAT", "TERRORISME", "RECIDIVE_GRAVE", "AUTRE"
    );

    /** SF-246-19 : borne max rémunération contrat (plausibilité). */
    static final int MAX_CHANGEMENT_REMUNERATION_EUR = 500_000;

    /** SF-246-19 : borne max durées en années (naturalisation, âge). */
    static final int MAX_NAT_DUREE_ANNEES = 70;
    static final int MAX_NAT_AGE = 120;

    /** SF-246-19 : borne max durée présence en mois (régime algérien / éloignement). */
    static final int MAX_PRESENCE_MOIS = 600;

    /**
     * Famille — agrégat des flags décisionnels niveau 3 (FR + BE) extraits depuis la clé
     * {@code famille_extracted_data} du JSON IA, pour permettre à F-IA-04 de basculer les
     * outils Famille ALWAYS_ON → CONTEXTUAL (cf. F-166 pattern).
     *
     * <p>F-200 (FR) livre 30 flags décisionnels niveau 3 en tête. F-202 (BE) livre 5 flags
     * décisionnels niveau 3 en queue — un dossier FR a tous les flags BE à false et
     * inversement, conformément aux instructions du prompt {@code FAMILLE_INSTRUCTION}.
     */
    public record FamilleExtractedData(
            // === Flags FR (F-200) — 30 flags ===
            // Permettent à F-IA-04 de basculer 30 outils Famille FR ALWAYS_ON → CONTEXTUAL
            // (migration 216). Dossiers BE : tous false (les régimes BE équivalents
            // sont gérés par les flags BE F-202 en queue de ce record).
            // 4 cas de divorce (F-FA-07/08/09/10)
            boolean divorceConsentementMutuelEnvisage,
            boolean divorceAlterationLienEnvisage,
            boolean divorceFauteEnvisage,
            boolean divorceAccepteEnvisage,
            // Révision post-divorce (F-FA-13)
            boolean revisionPostDivorceEnvisagee,
            // Ordonnance de protection (F-FA-14)
            boolean ordonnanceProtectionEnvisagee,
            // Régimes matrimoniaux (F-FA-15/16/17)
            boolean recompensesEnvisagees,
            boolean regimeCommunauteUniverselleDetecte,
            boolean partageJudiciaireEnvisage,
            // Adoption + filiation (F-FA-18 + sous-types)
            boolean adoptionEnvisagee,
            boolean reconnaissancePaternelleEnvisagee,
            boolean contestationPaterniteEnvisagee,
            boolean recherchePaterniteEnvisagee,
            boolean possessionEtatEnvisagee,
            // Autorité parentale conflictuelle (F-FA-19-changement-residence + desaccords-parentaux)
            boolean changementResidenceEnvisage,
            boolean desaccordParentalDetecte,
            // PACS / séparation / indivision / ordonnance requête (F-FA-20/21/22/23)
            boolean pacsDissolutionEnvisagee,
            boolean separationCorpsEnvisagee,
            boolean indivisionEnvisagee,
            boolean ordonnanceRequeteEnvisagee,
            // Successions / libéralités (F-FA-24-* — 7 sous-outils)
            boolean successionEnvisagee,
            boolean testamentEnvisage,
            boolean donationEnvisagee,
            boolean reserveHereditaireEnvisagee,
            boolean partageSuccessoralEnvisage,
            boolean indivisionSuccessoraleEnvisagee,
            boolean rapportSuccessionEnvisage,
            // Protection des majeurs (F-FA-25)
            boolean protectionMajeurEnvisagee,
            // État civil (F-FA-26)
            boolean changementEtatCivilEnvisage,
            // PMA / GPA (F-FA-27)
            boolean pmaGpaEnvisagee,
            // F-210 SF-210-01 : médiation familiale obligatoire pré-saisine JAF
            boolean mediationFamilialePreSaisinePertinente,
            // === Flags BE (F-202) — 5 flags ===
            // F-202 : 5 flags décisionnels niveau 3 — Famille BELGIQUE uniquement, default false.
            // Permettent à F-IA-04 de basculer les outils Famille BE ALWAYS_ON → CONTEXTUAL
            // (migration 217). Dossiers FR : tous false.
            boolean divorceDcEnvisage,
            boolean divorceDdiEnvisage,
            boolean cohabitationLegaleBeDetectee,
            boolean pacteSuccessoralEnvisage,
            boolean kafalaRecueilDetecte,
            // F-239 : champs string extraits par l'IA pour pré-fill outils décisionnels Famille
            // Date de signature du PV d'acceptation (FR — divorce accepté art. 233-234 Cciv)
            // OU de la convention préalable (BE — DC art. 1287+ CJ). Format ISO YYYY-MM-DD.
            // Utilisé par F-FA-07 checklist divorce pour pré-cocher les étapes "Signature
            // convention FR" + "Rédaction convention BE" (helper divorce-checklist-section-prefill-rules).
            String dateAcceptationPV,
            // SF-246-06 : 16 champs IA successions/libéralités pour pré-fill des
            // 8 outils décisionnels F-FA-24 (partage-successoral, reserve-heriditaire,
            // rapport-succession, acceptation-renonciation, indivision-successorale,
            // devolution-legale, donation, testament-validite). Famille FR uniquement,
            // tous nullables — le prompt impose null hors FR / hors certitude.
            // Sous-objet IA source : `famille_extracted_data.succession_detection`.
            // `dateDecesDetectee` ≠ `dateOuvertureSuccessionDetectee` : concepts
            // juridiquement distincts (cadrage §5.1.1), souvent identiques en pratique.
            String dateDecesDetectee,
            String dateOuvertureSuccessionDetectee,
            String modePartageDemandeDetecte,
            Integer nombreCoheritiersDetecte,
            Double montantSuccessionEurDetecte,
            Double montantLibsTotalEurDetecte,
            Integer nombreEnfantsSuccessionDetecte,
            String dateDonationDetectee,
            Double montantDonationsRecuesEurDetecte,
            Double valeurDonationAuJourPartageEurDetectee,
            Double actifBrutSuccessionEurDetecte,
            Double passifSuccessionEurDetecte,
            String typeIndivisionSuccessoraleDetecte,
            Integer nbDescendantsDetecte,
            Integer nbFreresSoeursDetecte,
            String dateRedactionTestamentDetectee,
            // SF-246-07 : 4 champs IA régimes matrimoniaux / liquidation pour pré-fill
            // des 3 outils décisionnels F-FA-15/16/17 (recompenses, communaute-universelle,
            // partage-judiciaire). Famille FR uniquement, tous nullables — le prompt impose
            // null hors FR / hors certitude.
            // Sous-objet IA source : `famille_extracted_data.regime_matrimonial_detection`.
            Double valeurCommunauteEurDetectee,
            String regimeMatrimonialDetecte,
            Double valeurBiensIndivisionEur,
            Integer nombreCoindivisairesDetecte,
            // SF-246-08 : 7 champs IA vie commune & protection pour pré-fill des
            // 6 outils décisionnels F-FA-12/13/14/20/21/22 (pacs-dissolution,
            // separation-corps, indivision, ordonnance-protection, mesures-provisoires,
            // revisions-post-divorce). Famille FR uniquement, tous nullables.
            // Sous-objet IA source : `famille_extracted_data.vie_commune_detection`.
            // `dateSeparation` (FR) ≠ `dateSeparation` BE de SF-246-12 (champ séparé).
            // `dateRequeteOP` (date dépôt requête) ≠ `dateOrdonnanceProtectionJaf`
            // (date ordonnance rendue — Immigration SF-246-04).
            // `patrimoineCommunEur` (montant €) ≠ `patrimoineCommun` boolean existant.
            String dateSeparation,
            Double patrimoineCommunEur,
            String dateConclusionPacs,
            String dateRequeteOP,
            String dateAudienceAOMP,
            Integer nbEnfantsACharge,
            Double revenusAnnuelsEpoux,
            // SF-246-09 : 7 champs IA filiation / adoption pour pré-fill des
            // 4 outils décisionnels F-FA-18 (contestation-paternite,
            // recherche-paternite, reconnaissance-paternelle, adoption).
            // Famille FR uniquement, tous nullables — le prompt impose null hors FR
            // / hors certitude. Sous-objet IA source :
            // `famille_extracted_data.filiation_detection`.
            // `dateNaissanceEnfantDetectee` (reconnaissance) ≠
            // `dateNaissanceEnfantRechercheDetectee` (recherche de paternité) :
            // deux contextes juridiques distincts (art. 316 vs 327 Cciv).
            String dateEtablissementFiliationDetectee,
            String dateConnaissanceVeriteDetectee,
            String dateMajoriteEnfantDetectee,
            String dateNaissanceEnfantRechercheDetectee,
            String dateNaissanceEnfantDetectee,
            Integer ageAdoptantDetecte,
            Integer ageAdopteDetecte,
            // SF-246-10 : 3 champs IA autorité parentale pour pré-fill des
            // 4 outils décisionnels F-FA-19 (autorite-parentale,
            // changement-residence, desaccords-parentaux, calendrier-garde).
            // Famille FR uniquement, tous nullables — le prompt impose null hors FR
            // / hors certitude. Sous-objet IA source :
            // `famille_extracted_data.autorite_parentale_detection`.
            // `agesEnfantsDetectes` : liste d'entiers [0, 25] ou null (jamais []).
            // Remplace le champ aspirationnel `ageEnfants` du DTO frontend.
            java.util.List<Integer> agesEnfantsDetectes,
            String dateDebutCalendrierDetectee,
            String dateFinCalendrierDetectee,
            // SF-246-03 : codes de faute détectés pour pré-fill F-FA-09 divorce pour faute
            // (Famille FR uniquement, nullable). Codes alignés sur FauteCode frontend :
            // {ADULTERE, VIOLENCES, ABANDON, OUTRAGES, DEVOIR_ASSISTANCE,
            //  DEVOIR_FIDELITE, DEVOIR_COMMUNAUTE_VIE, AUTRE}.
            // Codes hors whitelist exclus par extractFamilleData(). Liste vide → null
            // (jamais [] — invariant cadrage §5.1.2). Source :
            // `famille_extracted_data.divorce_faute_detection.fautes_detectees`.
            java.util.List<String> fautesDetectees) {

        /**
         * F-234 SF-234-01 : Builder pattern pour {@link FamilleExtractedData}.
         */
        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            // FR (F-200) — 30 flags
            private boolean divorceConsentementMutuelEnvisage;
            private boolean divorceAlterationLienEnvisage;
            private boolean divorceFauteEnvisage;
            private boolean divorceAccepteEnvisage;
            private boolean revisionPostDivorceEnvisagee;
            private boolean ordonnanceProtectionEnvisagee;
            private boolean recompensesEnvisagees;
            private boolean regimeCommunauteUniverselleDetecte;
            private boolean partageJudiciaireEnvisage;
            private boolean adoptionEnvisagee;
            private boolean reconnaissancePaternelleEnvisagee;
            private boolean contestationPaterniteEnvisagee;
            private boolean recherchePaterniteEnvisagee;
            private boolean possessionEtatEnvisagee;
            private boolean changementResidenceEnvisage;
            private boolean desaccordParentalDetecte;
            private boolean pacsDissolutionEnvisagee;
            private boolean separationCorpsEnvisagee;
            private boolean indivisionEnvisagee;
            private boolean ordonnanceRequeteEnvisagee;
            private boolean successionEnvisagee;
            private boolean testamentEnvisage;
            private boolean donationEnvisagee;
            private boolean reserveHereditaireEnvisagee;
            private boolean partageSuccessoralEnvisage;
            private boolean indivisionSuccessoraleEnvisagee;
            private boolean rapportSuccessionEnvisage;
            private boolean protectionMajeurEnvisagee;
            private boolean changementEtatCivilEnvisage;
            private boolean pmaGpaEnvisagee;
            // F-210
            private boolean mediationFamilialePreSaisinePertinente;
            // BE (F-202) — 5 flags
            private boolean divorceDcEnvisage;
            private boolean divorceDdiEnvisage;
            private boolean cohabitationLegaleBeDetectee;
            private boolean pacteSuccessoralEnvisage;
            private boolean kafalaRecueilDetecte;
            // F-239 : string fields
            private String dateAcceptationPV;
            // SF-246-06 : 16 champs IA successions/libéralités (F-FA-24), nullables.
            private String dateDecesDetectee;
            private String dateOuvertureSuccessionDetectee;
            private String modePartageDemandeDetecte;
            private Integer nombreCoheritiersDetecte;
            private Double montantSuccessionEurDetecte;
            private Double montantLibsTotalEurDetecte;
            private Integer nombreEnfantsSuccessionDetecte;
            private String dateDonationDetectee;
            private Double montantDonationsRecuesEurDetecte;
            private Double valeurDonationAuJourPartageEurDetectee;
            private Double actifBrutSuccessionEurDetecte;
            private Double passifSuccessionEurDetecte;
            private String typeIndivisionSuccessoraleDetecte;
            private Integer nbDescendantsDetecte;
            private Integer nbFreresSoeursDetecte;
            private String dateRedactionTestamentDetectee;
            // SF-246-07 : 4 champs IA régimes matrimoniaux / liquidation (F-FA-15/16/17), nullables.
            private Double valeurCommunauteEurDetectee;
            private String regimeMatrimonialDetecte;
            private Double valeurBiensIndivisionEur;
            private Integer nombreCoindivisairesDetecte;
            // SF-246-08 : 7 champs IA vie commune & protection (F-FA-12/13/14/20/21/22), nullables.
            private String dateSeparation;
            private Double patrimoineCommunEur;
            private String dateConclusionPacs;
            private String dateRequeteOP;
            private String dateAudienceAOMP;
            private Integer nbEnfantsACharge;
            private Double revenusAnnuelsEpoux;
            // SF-246-09 : 7 champs IA filiation / adoption (F-FA-18), nullables.
            private String dateEtablissementFiliationDetectee;
            private String dateConnaissanceVeriteDetectee;
            private String dateMajoriteEnfantDetectee;
            private String dateNaissanceEnfantRechercheDetectee;
            private String dateNaissanceEnfantDetectee;
            private Integer ageAdoptantDetecte;
            private Integer ageAdopteDetecte;
            // SF-246-10 : 3 champs IA autorité parentale (F-FA-19), nullables.
            private java.util.List<Integer> agesEnfantsDetectes;
            private String dateDebutCalendrierDetectee;
            private String dateFinCalendrierDetectee;
            // SF-246-03 : codes de faute détectés pour pré-fill F-FA-09 (Famille FR).
            private java.util.List<String> fautesDetectees;

            private Builder() {}

            public Builder divorceConsentementMutuelEnvisage(boolean v) { this.divorceConsentementMutuelEnvisage = v; return this; }
            public Builder divorceAlterationLienEnvisage(boolean v) { this.divorceAlterationLienEnvisage = v; return this; }
            public Builder divorceFauteEnvisage(boolean v) { this.divorceFauteEnvisage = v; return this; }
            public Builder divorceAccepteEnvisage(boolean v) { this.divorceAccepteEnvisage = v; return this; }
            public Builder revisionPostDivorceEnvisagee(boolean v) { this.revisionPostDivorceEnvisagee = v; return this; }
            public Builder ordonnanceProtectionEnvisagee(boolean v) { this.ordonnanceProtectionEnvisagee = v; return this; }
            public Builder recompensesEnvisagees(boolean v) { this.recompensesEnvisagees = v; return this; }
            public Builder regimeCommunauteUniverselleDetecte(boolean v) { this.regimeCommunauteUniverselleDetecte = v; return this; }
            public Builder partageJudiciaireEnvisage(boolean v) { this.partageJudiciaireEnvisage = v; return this; }
            public Builder adoptionEnvisagee(boolean v) { this.adoptionEnvisagee = v; return this; }
            public Builder reconnaissancePaternelleEnvisagee(boolean v) { this.reconnaissancePaternelleEnvisagee = v; return this; }
            public Builder contestationPaterniteEnvisagee(boolean v) { this.contestationPaterniteEnvisagee = v; return this; }
            public Builder recherchePaterniteEnvisagee(boolean v) { this.recherchePaterniteEnvisagee = v; return this; }
            public Builder possessionEtatEnvisagee(boolean v) { this.possessionEtatEnvisagee = v; return this; }
            public Builder changementResidenceEnvisage(boolean v) { this.changementResidenceEnvisage = v; return this; }
            public Builder desaccordParentalDetecte(boolean v) { this.desaccordParentalDetecte = v; return this; }
            public Builder pacsDissolutionEnvisagee(boolean v) { this.pacsDissolutionEnvisagee = v; return this; }
            public Builder separationCorpsEnvisagee(boolean v) { this.separationCorpsEnvisagee = v; return this; }
            public Builder indivisionEnvisagee(boolean v) { this.indivisionEnvisagee = v; return this; }
            public Builder ordonnanceRequeteEnvisagee(boolean v) { this.ordonnanceRequeteEnvisagee = v; return this; }
            public Builder successionEnvisagee(boolean v) { this.successionEnvisagee = v; return this; }
            public Builder testamentEnvisage(boolean v) { this.testamentEnvisage = v; return this; }
            public Builder donationEnvisagee(boolean v) { this.donationEnvisagee = v; return this; }
            public Builder reserveHereditaireEnvisagee(boolean v) { this.reserveHereditaireEnvisagee = v; return this; }
            public Builder partageSuccessoralEnvisage(boolean v) { this.partageSuccessoralEnvisage = v; return this; }
            public Builder indivisionSuccessoraleEnvisagee(boolean v) { this.indivisionSuccessoraleEnvisagee = v; return this; }
            public Builder rapportSuccessionEnvisage(boolean v) { this.rapportSuccessionEnvisage = v; return this; }
            public Builder protectionMajeurEnvisagee(boolean v) { this.protectionMajeurEnvisagee = v; return this; }
            public Builder changementEtatCivilEnvisage(boolean v) { this.changementEtatCivilEnvisage = v; return this; }
            public Builder pmaGpaEnvisagee(boolean v) { this.pmaGpaEnvisagee = v; return this; }
            public Builder mediationFamilialePreSaisinePertinente(boolean v) { this.mediationFamilialePreSaisinePertinente = v; return this; }
            public Builder divorceDcEnvisage(boolean v) { this.divorceDcEnvisage = v; return this; }
            public Builder divorceDdiEnvisage(boolean v) { this.divorceDdiEnvisage = v; return this; }
            public Builder cohabitationLegaleBeDetectee(boolean v) { this.cohabitationLegaleBeDetectee = v; return this; }
            public Builder pacteSuccessoralEnvisage(boolean v) { this.pacteSuccessoralEnvisage = v; return this; }
            public Builder kafalaRecueilDetecte(boolean v) { this.kafalaRecueilDetecte = v; return this; }
            public Builder dateAcceptationPV(String v) { this.dateAcceptationPV = v; return this; }
            // SF-246-06 : setters des 16 champs IA successions/libéralités.
            public Builder dateDecesDetectee(String v) { this.dateDecesDetectee = v; return this; }
            public Builder dateOuvertureSuccessionDetectee(String v) { this.dateOuvertureSuccessionDetectee = v; return this; }
            public Builder modePartageDemandeDetecte(String v) { this.modePartageDemandeDetecte = v; return this; }
            public Builder nombreCoheritiersDetecte(Integer v) { this.nombreCoheritiersDetecte = v; return this; }
            public Builder montantSuccessionEurDetecte(Double v) { this.montantSuccessionEurDetecte = v; return this; }
            public Builder montantLibsTotalEurDetecte(Double v) { this.montantLibsTotalEurDetecte = v; return this; }
            public Builder nombreEnfantsSuccessionDetecte(Integer v) { this.nombreEnfantsSuccessionDetecte = v; return this; }
            public Builder dateDonationDetectee(String v) { this.dateDonationDetectee = v; return this; }
            public Builder montantDonationsRecuesEurDetecte(Double v) { this.montantDonationsRecuesEurDetecte = v; return this; }
            public Builder valeurDonationAuJourPartageEurDetectee(Double v) { this.valeurDonationAuJourPartageEurDetectee = v; return this; }
            public Builder actifBrutSuccessionEurDetecte(Double v) { this.actifBrutSuccessionEurDetecte = v; return this; }
            public Builder passifSuccessionEurDetecte(Double v) { this.passifSuccessionEurDetecte = v; return this; }
            public Builder typeIndivisionSuccessoraleDetecte(String v) { this.typeIndivisionSuccessoraleDetecte = v; return this; }
            public Builder nbDescendantsDetecte(Integer v) { this.nbDescendantsDetecte = v; return this; }
            public Builder nbFreresSoeursDetecte(Integer v) { this.nbFreresSoeursDetecte = v; return this; }
            public Builder dateRedactionTestamentDetectee(String v) { this.dateRedactionTestamentDetectee = v; return this; }
            // SF-246-07 : setters des 4 champs IA régimes matrimoniaux / liquidation.
            public Builder valeurCommunauteEurDetectee(Double v) { this.valeurCommunauteEurDetectee = v; return this; }
            public Builder regimeMatrimonialDetecte(String v) { this.regimeMatrimonialDetecte = v; return this; }
            public Builder valeurBiensIndivisionEur(Double v) { this.valeurBiensIndivisionEur = v; return this; }
            public Builder nombreCoindivisairesDetecte(Integer v) { this.nombreCoindivisairesDetecte = v; return this; }
            // SF-246-08 : setters des 7 champs IA vie commune & protection.
            public Builder dateSeparation(String v) { this.dateSeparation = v; return this; }
            public Builder patrimoineCommunEur(Double v) { this.patrimoineCommunEur = v; return this; }
            public Builder dateConclusionPacs(String v) { this.dateConclusionPacs = v; return this; }
            public Builder dateRequeteOP(String v) { this.dateRequeteOP = v; return this; }
            public Builder dateAudienceAOMP(String v) { this.dateAudienceAOMP = v; return this; }
            public Builder nbEnfantsACharge(Integer v) { this.nbEnfantsACharge = v; return this; }
            public Builder revenusAnnuelsEpoux(Double v) { this.revenusAnnuelsEpoux = v; return this; }
            // SF-246-09 : setters des 7 champs IA filiation / adoption (F-FA-18).
            public Builder dateEtablissementFiliationDetectee(String v) { this.dateEtablissementFiliationDetectee = v; return this; }
            public Builder dateConnaissanceVeriteDetectee(String v) { this.dateConnaissanceVeriteDetectee = v; return this; }
            public Builder dateMajoriteEnfantDetectee(String v) { this.dateMajoriteEnfantDetectee = v; return this; }
            public Builder dateNaissanceEnfantRechercheDetectee(String v) { this.dateNaissanceEnfantRechercheDetectee = v; return this; }
            public Builder dateNaissanceEnfantDetectee(String v) { this.dateNaissanceEnfantDetectee = v; return this; }
            public Builder ageAdoptantDetecte(Integer v) { this.ageAdoptantDetecte = v; return this; }
            public Builder ageAdopteDetecte(Integer v) { this.ageAdopteDetecte = v; return this; }
            // SF-246-10 : setters des 3 champs IA autorité parentale (F-FA-19).
            public Builder agesEnfantsDetectes(java.util.List<Integer> v) { this.agesEnfantsDetectes = v; return this; }
            public Builder dateDebutCalendrierDetectee(String v) { this.dateDebutCalendrierDetectee = v; return this; }
            public Builder dateFinCalendrierDetectee(String v) { this.dateFinCalendrierDetectee = v; return this; }
            // SF-246-03 : setter codes de faute détectés (F-FA-09).
            public Builder fautesDetectees(java.util.List<String> v) { this.fautesDetectees = v; return this; }

            public FamilleExtractedData build() {
                return new FamilleExtractedData(
                        divorceConsentementMutuelEnvisage, divorceAlterationLienEnvisage,
                        divorceFauteEnvisage, divorceAccepteEnvisage,
                        revisionPostDivorceEnvisagee,
                        ordonnanceProtectionEnvisagee,
                        recompensesEnvisagees, regimeCommunauteUniverselleDetecte, partageJudiciaireEnvisage,
                        adoptionEnvisagee, reconnaissancePaternelleEnvisagee,
                        contestationPaterniteEnvisagee, recherchePaterniteEnvisagee, possessionEtatEnvisagee,
                        changementResidenceEnvisage, desaccordParentalDetecte,
                        pacsDissolutionEnvisagee, separationCorpsEnvisagee,
                        indivisionEnvisagee, ordonnanceRequeteEnvisagee,
                        successionEnvisagee, testamentEnvisage, donationEnvisagee, reserveHereditaireEnvisagee,
                        partageSuccessoralEnvisage, indivisionSuccessoraleEnvisagee, rapportSuccessionEnvisage,
                        protectionMajeurEnvisagee, changementEtatCivilEnvisage, pmaGpaEnvisagee,
                        mediationFamilialePreSaisinePertinente,
                        divorceDcEnvisage, divorceDdiEnvisage, cohabitationLegaleBeDetectee,
                        pacteSuccessoralEnvisage, kafalaRecueilDetecte,
                        dateAcceptationPV,
                        // SF-246-06 : 16 champs IA successions/libéralités.
                        dateDecesDetectee, dateOuvertureSuccessionDetectee,
                        modePartageDemandeDetecte, nombreCoheritiersDetecte,
                        montantSuccessionEurDetecte, montantLibsTotalEurDetecte,
                        nombreEnfantsSuccessionDetecte, dateDonationDetectee,
                        montantDonationsRecuesEurDetecte, valeurDonationAuJourPartageEurDetectee,
                        actifBrutSuccessionEurDetecte, passifSuccessionEurDetecte,
                        typeIndivisionSuccessoraleDetecte, nbDescendantsDetecte,
                        nbFreresSoeursDetecte, dateRedactionTestamentDetectee,
                        // SF-246-07 : 4 champs IA régimes matrimoniaux / liquidation.
                        valeurCommunauteEurDetectee, regimeMatrimonialDetecte,
                        valeurBiensIndivisionEur, nombreCoindivisairesDetecte,
                        // SF-246-08 : 7 champs IA vie commune & protection.
                        dateSeparation, patrimoineCommunEur,
                        dateConclusionPacs, dateRequeteOP, dateAudienceAOMP,
                        nbEnfantsACharge, revenusAnnuelsEpoux,
                        // SF-246-09 : 7 champs IA filiation / adoption.
                        dateEtablissementFiliationDetectee, dateConnaissanceVeriteDetectee,
                        dateMajoriteEnfantDetectee, dateNaissanceEnfantRechercheDetectee,
                        dateNaissanceEnfantDetectee, ageAdoptantDetecte, ageAdopteDetecte,
                        // SF-246-10 : 3 champs IA autorité parentale.
                        agesEnfantsDetectes, dateDebutCalendrierDetectee, dateFinCalendrierDetectee,
                        // SF-246-03 : codes de faute détectés (F-FA-09).
                        fautesDetectees);
            }
        }
    }

    public record TimelineEntry(String date, String evenement) {}

    public record AnalysisDocumentEntry(int index, String name) {}

    public record VersionSummary(
            UUID id,
            int version,
            String analysisType,
            Instant updatedAt,
            Integer faitsCount,
            Integer pointsJuridiquesCount,
            Integer risquesCount,
            Integer questionsOuvertesCount,
            Integer timelineCount
    ) {}

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void populateCounts(CaseAnalysis analysis, String rawResult) {
        if (rawResult == null || rawResult.isBlank()) return;
        try {
            JsonNode root = MAPPER.readTree(stripMarkdownCodeBlock(rawResult));
            analysis.setFaitsCount(sizeOf(root, "faits"));
            analysis.setPointsJuridiquesCount(sizeOf(root, "points_juridiques"));
            analysis.setRisquesCount(sizeOf(root, "risques"));
            analysis.setQuestionsOuvertesCount(sizeOf(root, "questions_ouvertes"));
            analysis.setTimelineCount(sizeOf(root, "timeline"));
        } catch (Exception ignored) {
            // JSON malformé — compteurs restent null (fail-open)
        }
    }

    public static void populateRiskScore(CaseAnalysis analysis, String rawResult) {
        if (rawResult == null || rawResult.isBlank()) return;
        try {
            JsonNode root = MAPPER.readTree(stripMarkdownCodeBlock(rawResult));
            JsonNode scoreNode = root.get("score_risque");
            if (scoreNode == null || !scoreNode.isObject()) return;
            JsonNode niveauNode = scoreNode.get("niveau");
            JsonNode valeurNode = scoreNode.get("valeur");
            if (niveauNode != null && niveauNode.isTextual()) {
                String niveau = niveauNode.asText().toUpperCase();
                if (niveau.equals("FAIBLE") || niveau.equals("MOYEN") || niveau.equals("ELEVE")) {
                    analysis.setRiskLevel(niveau);
                }
            }
            if (valeurNode != null && valeurNode.isNumber()) {
                int valeur = valeurNode.asInt();
                if (valeur >= 0 && valeur <= 100) {
                    analysis.setRiskScore(valeur);
                }
            }
        } catch (Exception ignored) {
            // JSON malformé — risk score reste null (fail-open)
        }
    }

    private static int sizeOf(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return (node != null && node.isArray()) ? node.size() : 0;
    }

    public static CaseAnalysisResponse from(CaseAnalysis analysis) {
        return from(analysis, List.of());
    }

    public static CaseAnalysisResponse from(CaseAnalysis analysis, List<AnalysisDocument> documents) {
        List<TimelineEntry> timeline = List.of();
        List<AnalysisItem> faits = List.of();
        List<AnalysisItem> pointsJuridiques = List.of();
        List<AnalysisItem> risques = List.of();
        List<String> questionsOuvertes = List.of();
        List<String> piecesManquantes = List.of();
        List<String> pointsProcedure = List.of();
        CompensationCalculator.CompensationEstimate compensationEstimate = null;
        BelgianCompensationCalculator.BelgianCompensationEstimate belgianCompensationEstimate = null;
        PensionAlimentaireCalculator.PensionAlimentaireEstimate pensionAlimentaireEstimate = null;
        PrestationCompensatoireCalculator.PrestationCompensatoireEstimate prestationCompensatoireEstimate = null;
        LiquidationCommunauteResult liquidationCommunaute = null;
        TravailExtractedData travailExtractedData = null;
        ImmigrationExtractedData immigrationExtractedData = null;
        LicenciementValidityDetection licenciementValidityDetection = null;
        RuptureConvValidityDetection ruptureConvValidityDetection = null;
        List<PieceManquanteEntry> piecesManquantesDetails = List.of();
        List<fr.ailegalcase.immigration.ImmigrationTriggerEvent> immigrationTriggerEvents = List.of();
        List<fr.ailegalcase.immigration.ImmigrationStrategyScenario> immigrationStrategyScenarios = List.of();
        DivorceConsentementValidityDetection divorceConsentementValidityDetection = null;
        DivorceConsentementScoring divorceConsentementScoring = null;
        // F-202 : flags Famille BE (F-200 ajoutera les flags FR ultérieurement).
        FamilleExtractedData familleExtractedData = null;

        String raw = stripMarkdownCodeBlock(analysis.getAnalysisResult());
        if (raw != null && !raw.isBlank()) {
            JsonNode root = tryParseJson(raw);
            if (root != null) try {
                timeline = extractTimeline(root);
                faits = extractItemList(root, "faits");
                pointsJuridiques = extractItemList(root, "points_juridiques");
                risques = extractItemList(root, "risques");
                questionsOuvertes = extractStringList(root, "questions_ouvertes");
                piecesManquantesDetails = extractPiecesManquantesDetails(root);
                piecesManquantes = piecesManquantesDetails.stream().map(PieceManquanteEntry::texte).toList();
                pointsProcedure = extractPointsProcedureTexts(root);
                compensationEstimate = extractCompensationEstimate(root);
                pensionAlimentaireEstimate = extractPensionAlimentaireEstimate(root);
                prestationCompensatoireEstimate = extractPrestationCompensatoireEstimate(root);
                liquidationCommunaute = extractLiquidationCommunaute(root);
                travailExtractedData = extractTravailData(root);
                immigrationExtractedData = extractImmigrationData(root);
                licenciementValidityDetection = extractLicenciementValidityDetection(root);
                ruptureConvValidityDetection = extractRuptureConvValidityDetection(root);
                immigrationTriggerEvents = extractImmigrationTriggerEvents(root);
                immigrationStrategyScenarios = extractImmigrationStrategyScenarios(root);
                divorceConsentementValidityDetection = extractDivorceConsentementValidityDetection(root);
                divorceConsentementScoring = computeDivorceConsentementScoring(divorceConsentementValidityDetection);
                // F-202 SF-202-01 : 5 flags Famille BE (la fonction retourne null si tous false ou clé absente).
                familleExtractedData = extractFamilleData(root);

                // SF-IM-01-04 : enrichit ImmigrationExtractedData avec le type de
                // checklist inféré (combine titre actuel + titre cible suggéré par
                // le 1er événement déclencheur F-150 le cas échéant).
                // SF-IM-01-06 : passe aussi l'event_code du 1er trigger à l'inférence
                // — ImmigrationPieceReferentiel priorise l'événement (étape 1 du
                // switch) sur le titre cible. Sans ça, MARIAGE_RESSORTISSANT_FR
                // était détecté mais silencieusement ignoré → checklist retombait
                // sur VISA_ETUDIANT au lieu de CST_VPF_CONJOINT_FR.
                if (immigrationExtractedData != null) {
                    String firstEventCode = immigrationTriggerEvents.isEmpty()
                            ? null
                            : immigrationTriggerEvents.get(0).eventCode();
                    String targetTitleCode = immigrationTriggerEvents.isEmpty()
                            ? null
                            : immigrationTriggerEvents.get(0).suggestedTitleCode();
                    String inferred = fr.ailegalcase.casefile.ImmigrationPieceReferentiel
                            .inferChecklistType(immigrationExtractedData.typeTitreSejourCode(), targetTitleCode, firstEventCode);
                    if (inferred != null) {
                        // F-234 SF-234-01 : reconstruction via toBuilder() — propage automatiquement
                        // tous les champs du record (y compris la nationalite F-235) et n'ajuste
                        // que inferredChecklistType.
                        immigrationExtractedData = immigrationExtractedData.toBuilder()
                                .inferredChecklistType(inferred)
                                .build();
                    }
                }
            } catch (Exception ignored) {
                // JSON malformé — on retourne les listes vides
            }
        }

        List<AnalysisDocumentEntry> analysisDocuments = buildAnalysisDocuments(documents);

        return new CaseAnalysisResponse(
                analysis.getId(),
                analysis.getVersion(),
                analysis.getAnalysisType().name(),
                analysis.getAnalysisStatus().name(),
                timeline,
                faits,
                pointsJuridiques,
                risques,
                questionsOuvertes,
                piecesManquantes,
                pointsProcedure,
                analysis.getRiskLevel(),
                analysis.getRiskScore(),
                analysis.getModelUsed(),
                analysis.getUpdatedAt(),
                analysisDocuments,
                compensationEstimate,
                belgianCompensationEstimate,
                pensionAlimentaireEstimate,
                prestationCompensatoireEstimate,
                liquidationCommunaute,
                travailExtractedData,
                immigrationExtractedData,
                licenciementValidityDetection,
                ruptureConvValidityDetection,
                piecesManquantesDetails,
                immigrationTriggerEvents,
                immigrationStrategyScenarios,
                divorceConsentementValidityDetection,
                divorceConsentementScoring,
                familleExtractedData
        );
    }

    public static CaseAnalysisResponse from(CaseAnalysis analysis, List<AnalysisDocument> documents, String country) {
        CaseAnalysisResponse base = from(analysis, documents);
        if ("BELGIQUE".equals(country) && base.compensationEstimate() != null) {
            var ce = base.compensationEstimate();
            var belgian = BelgianCompensationCalculator.calculate(
                    ce.ancienneteAnnees(), ce.ancienneteMois(), ce.salaireReference()).orElse(null);
            // Conserver compensationEstimate non-null pour que le frontend F-DT-09
            // (indemnite-comparatif-section) puisse lire typeRupture / ancienneté /
            // salaire détectés par l'IA et déclencher les alertes F-IA-03 aussi
            // côté BE. Le panneau d'affichage Macron FR est masqué côté frontend
            // via un guard sur belgianCompensationEstimate.
            return new CaseAnalysisResponse(
                    base.id(), base.version(), base.analysisType(), base.status(),
                    base.timeline(), base.faits(), base.pointsJuridiques(), base.risques(),
                    base.questionsOuvertes(), base.piecesManquantes(), base.pointsProcedure(),
                    base.riskLevel(), base.riskScore(), base.modelUsed(), base.updatedAt(),
                    base.analysisDocuments(),
                    ce, belgian,
                    base.pensionAlimentaireEstimate(), base.prestationCompensatoireEstimate(),
                    base.liquidationCommunaute(),
                    base.travailExtractedData(), base.immigrationExtractedData(),
                    base.licenciementValidityDetection(),
                    base.ruptureConvValidityDetection(),
                    base.piecesManquantesDetails(),
                    base.immigrationTriggerEvents(), base.immigrationStrategyScenarios(),
                    base.divorceConsentementValidityDetection(), base.divorceConsentementScoring(),
                    base.familleExtractedData());
        }
        return base;
    }

    static CompensationCalculator.CompensationEstimate extractCompensationEstimate(JsonNode root) {
        JsonNode compNode = root.get("compensation_data");
        try {
            String typeRupture = null;
            Integer annees = null;
            Integer mois = null;
            Double salaire = null;

            if (compNode != null && compNode.isObject()) {
                String rawType = compNode.has("type_rupture") && !compNode.get("type_rupture").isNull()
                        ? compNode.get("type_rupture").asText() : null;
                typeRupture = TypeRuptureFallback.normalize(rawType);
                annees  = compNode.has("anciennete_annees")  && !compNode.get("anciennete_annees").isNull()
                        ? compNode.get("anciennete_annees").intValue() : null;
                mois    = compNode.has("anciennete_mois")    && !compNode.get("anciennete_mois").isNull()
                        ? compNode.get("anciennete_mois").intValue() : null;
                salaire = compNode.has("salaire_reference_mensuel") && !compNode.get("salaire_reference_mensuel").isNull()
                        ? compNode.get("salaire_reference_mensuel").doubleValue() : null;
            }

            // Fallback : si l'IA n'a pas peuplé type_rupture (ou pas compensation_data du tout)
            // mais qu'elle a détecté un licenciement ailleurs, on dérive un type par défaut
            // et on remonte au minimum un estimate partiel porteur de ce type_rupture
            // (pour le pré-remplissage F-DT-09 et les alertes de cohérence F-IA-03).
            if (typeRupture == null) {
                typeRupture = TypeRuptureFallback.derive(root);
                if (typeRupture == null) return null;
                var calculated = CompensationCalculator.calculate(typeRupture, annees, mois, salaire);
                if (calculated.isPresent()) return calculated.get();
                int safeAnnees = annees != null ? annees : 0;
                int safeMois   = mois != null ? mois : 0;
                double safeSalaire = (salaire != null && salaire > 0) ? salaire : 0;
                return new CompensationCalculator.CompensationEstimate(
                        0.0, safeSalaire, safeAnnees, safeMois,
                        typeRupture, 0, 0.0, true);
            }

            return CompensationCalculator.calculate(typeRupture, annees, mois, salaire).orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    static final Set<String> MODE_GARDE_DETAILLE_VALUES = Set.of(
            "ALTERNEE_FR", "DVH_CLASSIQUE_FR", "DVH_ELARGI_FR",
            "ALTERNEE_BE", "SECONDAIRE_BE", "SECONDAIRE_ELARGI_BE"
    );

    static PensionAlimentaireCalculator.PensionAlimentaireEstimate extractPensionAlimentaireEstimate(JsonNode root) {
        JsonNode node = root.get("pension_alimentaire_data");
        if (node == null || !node.isObject()) return null;
        try {
            Double revenus = node.has("revenus_net_mensuel_debiteur") && !node.get("revenus_net_mensuel_debiteur").isNull()
                    ? node.get("revenus_net_mensuel_debiteur").doubleValue() : null;
            Integer nbEnfants = node.has("nb_enfants") && !node.get("nb_enfants").isNull()
                    ? node.get("nb_enfants").intValue() : null;
            String modeGarde = node.has("mode_garde") && !node.get("mode_garde").isNull()
                    ? node.get("mode_garde").asText() : null;
            String pays = node.has("pays_applicable") && !node.get("pays_applicable").isNull()
                    ? node.get("pays_applicable").asText() : null;
            String modeGardeDetaille = null;
            if (node.has("mode_garde_detaille") && !node.get("mode_garde_detaille").isNull()) {
                String raw = node.get("mode_garde_detaille").asText();
                if (raw != null && !raw.isBlank()) {
                    String normalized = raw.trim().toUpperCase();
                    if (MODE_GARDE_DETAILLE_VALUES.contains(normalized)) {
                        modeGardeDetaille = normalized;
                    }
                }
            }
            var estimate = PensionAlimentaireCalculator.calculate(revenus, nbEnfants, modeGarde, pays).orElse(null);
            if (estimate == null) return null;
            if (modeGardeDetaille == null) return estimate;
            return new PensionAlimentaireCalculator.PensionAlimentaireEstimate(
                    estimate.montantMin(), estimate.montantMax(), estimate.revenus(),
                    estimate.nbEnfants(), estimate.modeGarde(), estimate.pays(),
                    estimate.donneesPartielles(), modeGardeDetaille);
        } catch (Exception ignored) {
            return null;
        }
    }

    static PrestationCompensatoireCalculator.PrestationCompensatoireEstimate extractPrestationCompensatoireEstimate(JsonNode root) {
        JsonNode node = root.get("prestation_compensatoire_data");
        if (node == null || !node.isObject()) return null;
        try {
            Double revenusA    = node.has("revenus_net_mensuel_epoux_a") && !node.get("revenus_net_mensuel_epoux_a").isNull()
                    ? node.get("revenus_net_mensuel_epoux_a").doubleValue() : null;
            Double revenusB    = node.has("revenus_net_mensuel_epoux_b") && !node.get("revenus_net_mensuel_epoux_b").isNull()
                    ? node.get("revenus_net_mensuel_epoux_b").doubleValue() : null;
            Integer duree      = node.has("duree_mariage_annees") && !node.get("duree_mariage_annees").isNull()
                    ? node.get("duree_mariage_annees").intValue() : null;
            String pays        = node.has("pays_applicable") && !node.get("pays_applicable").isNull()
                    ? node.get("pays_applicable").asText() : null;
            return PrestationCompensatoireCalculator.calculate(revenusA, revenusB, duree, pays).orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    static LiquidationCommunauteResult extractLiquidationCommunaute(JsonNode root) {
        JsonNode node = root.get("liquidation_communaute_data");
        if (node == null || !node.isObject()) return null;
        try {
            String regime = node.has("regime_matrimonial") && !node.get("regime_matrimonial").isNull()
                    ? node.get("regime_matrimonial").asText() : null;
            List<LiquidationCommunauteResult.BienItem> actifCommun       = extractBienItems(node, "actif_commun", "valeur_estimee");
            List<LiquidationCommunauteResult.BienItem> biensPropresA     = extractBienItems(node, "biens_propres_epoux_a", "valeur_estimee");
            List<LiquidationCommunauteResult.BienItem> biensPropresB     = extractBienItems(node, "biens_propres_epoux_b", "valeur_estimee");
            List<LiquidationCommunauteResult.BienItem> passifCommun      = extractBienItems(node, "passif_commun", "montant");
            return new LiquidationCommunauteResult(regime, actifCommun, biensPropresA, biensPropresB, passifCommun);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<LiquidationCommunauteResult.BienItem> extractBienItems(JsonNode parent, String field, String valeurKey) {
        JsonNode array = parent.get(field);
        if (array == null || !array.isArray()) return List.of();
        java.util.List<LiquidationCommunauteResult.BienItem> result = new java.util.ArrayList<>();
        for (JsonNode item : array) {
            if (!item.isObject() || !item.has("libelle")) continue;
            String libelle = item.get("libelle").asText();
            Double valeur  = item.has(valeurKey) && !item.get(valeurKey).isNull()
                    ? item.get(valeurKey).doubleValue() : null;
            result.add(new LiquidationCommunauteResult.BienItem(libelle, valeur));
        }
        return List.copyOf(result);
    }

    private static List<AnalysisDocumentEntry> buildAnalysisDocuments(List<AnalysisDocument> documents) {
        if (documents == null || documents.isEmpty()) return List.of();
        List<AnalysisDocumentEntry> result = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            result.add(new AnalysisDocumentEntry(i, documents.get(i).getDocumentName()));
        }
        return List.copyOf(result);
    }

    /**
     * Parse un array JSON en List<AnalysisItem>. Fail-open :
     * - item string → AnalysisItem(texte, null, null, null)
     * - item objet {texte, source?, extrait?, sourceRef?} → AnalysisItem complet
     * - item malformé → ignoré
     *
     * <p>F-146 SF-146-01 : support du champ {@code sourceRef} produit par le
     * pipeline IA enrichi. Rétrocompat totale avec items pré-F-146.
     */
    static List<AnalysisItem> extractItemList(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isArray()) return List.of();
        List<AnalysisItem> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual()) {
                result.add(AnalysisItem.ofText(item.asText()));
            } else if (item.isObject()) {
                String texte = item.has("texte") ? item.get("texte").asText() : item.toString();
                String source = item.has("source") && !item.get("source").isNull()
                        ? item.get("source").asText() : null;
                String extrait = item.has("extrait") && !item.get("extrait").isNull()
                        ? item.get("extrait").asText() : null;
                SourceRef sourceRef = extractSourceRef(item.get("sourceRef"));
                result.add(new AnalysisItem(texte, source, extrait, sourceRef));
            }
        }
        return List.copyOf(result);
    }

    /**
     * F-146 SF-146-01 : parse le sous-objet {@code sourceRef}. Fail-open sur
     * champs manquants ou types erronés (retourne null pour le field concerné).
     */
    static SourceRef extractSourceRef(JsonNode ref) {
        if (ref == null || !ref.isObject()) return null;
        String documentName = textOrNullSr(ref.get("documentName"));
        String pieceType = textOrNullSr(ref.get("pieceType"));
        String pieceLabel = textOrNullSr(ref.get("pieceLabel"));
        Integer pageStart = intOrNullSr(ref.get("pageStart"));
        Integer pageEnd = intOrNullSr(ref.get("pageEnd"));
        if (documentName == null && pieceType == null && pieceLabel == null
                && pageStart == null && pageEnd == null) {
            return null;
        }
        return new SourceRef(documentName, pieceType, pieceLabel, pageStart, pageEnd);
    }

    private static String textOrNullSr(JsonNode n) {
        return n != null && !n.isNull() && n.isTextual() ? n.asText() : null;
    }

    private static Integer intOrNullSr(JsonNode n) {
        return n != null && !n.isNull() && n.isNumber() ? n.asInt() : null;
    }

    private static List<String> extractStringList(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isArray()) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual()) result.add(item.asText());
        }
        return List.copyOf(result);
    }

    /**
     * Extrait pieces_manquantes en tolérant les deux formats :
     * - legacy : array de strings
     * - nouveau : array d'objets {texte, critere_code?}
     */
    /**
     * F-150 SF-150-01 : parse le tableau {@code trigger_events} produit par l'IA
     * pour les dossiers immigration. Fail-open : codes inconnus / dates invalides
     * skippés silencieusement.
     */
    static List<fr.ailegalcase.immigration.ImmigrationTriggerEvent> extractImmigrationTriggerEvents(JsonNode root) {
        JsonNode node = root.get("trigger_events");
        if (node == null || !node.isArray() || node.isEmpty()) return List.of();

        List<fr.ailegalcase.immigration.ImmigrationTriggerEvent> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isObject()) continue;

            JsonNode codeNode = item.get("event_code");
            if (codeNode == null || !codeNode.isTextual()) continue;
            var codeOpt = fr.ailegalcase.immigration.ImmigrationTriggerEventReferential
                    .parseCode(codeNode.asText());
            if (codeOpt.isEmpty()) continue; // code inconnu

            var defOpt = fr.ailegalcase.immigration.ImmigrationTriggerEventReferential.resolve(codeOpt.get());
            if (defOpt.isEmpty()) continue;
            var def = defOpt.get();

            String eventDate = textOrNull(item.get("event_date"));
            String sourceDocument = textOrNull(item.get("source_document"));
            String justification = textOrNull(item.get("justification"));

            result.add(new fr.ailegalcase.immigration.ImmigrationTriggerEvent(
                    codeOpt.get().name(),
                    def.eventLabel(),
                    eventDate,
                    sourceDocument,
                    justification,
                    def.baseLegale(),
                    def.suggestedTitleCode(),
                    def.suggestedTitleLabel()
            ));
        }
        return List.copyOf(result);
    }

    private static String textOrNull(JsonNode n) {
        if (n == null || n.isNull() || !n.isTextual()) return null;
        String v = n.asText().trim();
        return v.isEmpty() ? null : v;
    }

    /** F-151 SF-151-01 : parse les scenarii stratégiques immigration. Fail-open. */
    static List<fr.ailegalcase.immigration.ImmigrationStrategyScenario> extractImmigrationStrategyScenarios(JsonNode root) {
        JsonNode node = root.get("strategy_scenarios");
        if (node == null || !node.isArray() || node.isEmpty()) return List.of();

        Set<String> VALID_RISK = Set.of("FAIBLE", "MOYEN", "ELEVE");
        List<fr.ailegalcase.immigration.ImmigrationStrategyScenario> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isObject()) continue;

            String label = textOrNull(item.get("scenario_label"));
            String description = textOrNull(item.get("scenario_description"));
            if (label == null || description == null) continue;

            String riskLevel = textOrNull(item.get("risk_level"));
            if (riskLevel != null) {
                String upper = riskLevel.toUpperCase();
                riskLevel = VALID_RISK.contains(upper) ? upper : null;
            }

            result.add(new fr.ailegalcase.immigration.ImmigrationStrategyScenario(
                    label,
                    description,
                    textOrNull(item.get("base_legale")),
                    textOrNull(item.get("target_title_code")),
                    textOrNull(item.get("target_title_label")),
                    textOrNull(item.get("delay_days_estimate")),
                    riskLevel,
                    textOrNull(item.get("risk_justification")),
                    extractStringArray(item.get("required_additional_pieces")),
                    extractStringArray(item.get("advantages")),
                    extractStringArray(item.get("drawbacks"))
            ));
        }
        return List.copyOf(result);
    }

    private static List<String> extractStringArray(JsonNode n) {
        if (n == null || !n.isArray()) return List.of();
        List<String> out = new ArrayList<>();
        for (JsonNode item : n) {
            if (item.isTextual()) {
                String v = item.asText().trim();
                if (!v.isEmpty()) out.add(v);
            }
        }
        return out;
    }

    static List<PieceManquanteEntry> extractPiecesManquantesDetails(JsonNode root) {
        JsonNode node = root.get("pieces_manquantes");
        if (node == null || !node.isArray()) return List.of();
        List<PieceManquanteEntry> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual()) {
                String txt = item.asText();
                if (txt != null && !txt.isBlank()) result.add(new PieceManquanteEntry(txt, null));
            } else if (item.isObject()) {
                JsonNode texteNode = item.get("texte");
                if (texteNode == null || !texteNode.isTextual()) continue;
                String texte = texteNode.asText();
                if (texte.isBlank()) continue;
                String code = null;
                JsonNode codeNode = item.get("critere_code");
                if (codeNode != null && codeNode.isTextual()) {
                    String raw = codeNode.asText().trim();
                    if (!raw.isEmpty()) code = raw.toUpperCase();
                }
                result.add(new PieceManquanteEntry(texte, code));
            }
        }
        return List.copyOf(result);
    }

    /**
     * Extrait les descriptions de points_procedure en tolérant les deux formats :
     * - legacy : array de strings
     * - nouveau : array d'objets {texte, critere_code?}
     */
    static List<String> extractPointsProcedureTexts(JsonNode root) {
        JsonNode node = root.get("points_procedure");
        if (node == null || !node.isArray()) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual()) {
                String txt = item.asText();
                if (txt != null && !txt.isBlank()) result.add(txt);
            } else if (item.isObject()) {
                JsonNode texte = item.get("texte");
                if (texte != null && texte.isTextual()) {
                    String t = texte.asText();
                    if (!t.isBlank()) result.add(t);
                }
            }
        }
        return List.copyOf(result);
    }

    public static String stripMarkdownCodeBlock(String raw) {
        if (raw == null) return null;
        String s = raw.strip();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline != -1) s = s.substring(firstNewline + 1);
            if (s.endsWith("```")) s = s.substring(0, s.lastIndexOf("```")).strip();
        }
        return s;
    }

    /**
     * Parse avec fallback récupération : tente d'abord parse strict, puis si échec
     * essaie de tronquer au dernier '}' valide. Retourne null si tout échoue.
     */
    static JsonNode tryParseJson(String raw) {
        try {
            return MAPPER.readTree(raw);
        } catch (Exception strictFail) {
            String recovered = recoverTruncatedJson(raw);
            if (recovered != null) {
                try {
                    return MAPPER.readTree(recovered);
                } catch (Exception recoveryFail) {
                    return null;
                }
            }
            return null;
        }
    }

    /**
     * Essaie de récupérer un JSON valide depuis une chaîne potentiellement tronquée
     * (cas stop_reason=max_tokens). Coupe au dernier '}' trouvé et ferme les accolades
     * manquantes. Retourne null si aucune récupération possible.
     */
    static String recoverTruncatedJson(String raw) {
        if (raw == null || raw.isBlank() || !raw.startsWith("{")) return null;
        // Cherche le dernier '}' matching qui pourrait fermer l'objet racine
        int depth = 0;
        int lastValidObjectClose = -1;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (escaped) { escaped = false; continue; }
            if (c == '\\' && inString) { escaped = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 1) lastValidObjectClose = i;  // dernier child-object fermé
            }
        }
        if (lastValidObjectClose == -1) return null;
        // Tronque à ce point et ferme l'objet racine
        return raw.substring(0, lastValidObjectClose + 1) + "}";
    }

    private static List<TimelineEntry> extractTimeline(JsonNode root) {
        JsonNode node = root.get("timeline");
        if (node == null || !node.isArray()) return List.of();
        List<TimelineEntry> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isObject()) {
                String date = item.has("date") ? item.get("date").asText() : "";
                String evenement = item.has("evenement") ? item.get("evenement").asText() : "";
                result.add(new TimelineEntry(date, evenement));
            }
        }
        return List.copyOf(result);
    }

    static TravailExtractedData extractTravailData(JsonNode root) {
        JsonNode node = root.get("travail_extracted_data");
        if (node == null || !node.isObject()) return null;
        try {
            // SF-246-01 : sous-objet procédural pour pré-fill F-DT-36. Peut être absent
            // (dossier sans pièces de licenciement, dossier BE) → tous les champs null.
            JsonNode procedure = node.get("procedure_licenciement_detection");
            boolean hasProcedure = procedure != null && procedure.isObject();
            // SF-246-02 : sous-objet détail de la clause de non-concurrence pour pré-fill
            // F-DT-24. Peut être absent (clause non détectée, dossier BE) → 3 champs null.
            JsonNode clauseNc = node.get("clause_non_concurrence_detail");
            boolean hasClauseNc = clauseNc != null && clauseNc.isObject();
            // F-234 SF-234-01 : construction via Builder — propage automatiquement null/false
            // sur les champs absents au lieu de propager des arguments positionnels.
            return TravailExtractedData.builder()
                    // SF-129-01 : normaliser le code convention pour matcher le référentiel
                    .conventionCollective(fr.ailegalcase.casefile.ConventionCodeNormalizer.normalize(textOrNull(node, "convention_collective")))
                    .dateEntree(textOrNull(node, "date_entree"))
                    .salaireBrutMensuel(doubleOrNull(node, "salaire_brut_mensuel"))
                    .typeContrat(textOrNull(node, "type_contrat"))
                    .poste(textOrNull(node, "poste"))
                    .motifLicenciement(textOrNull(node, "motif_licenciement"))
                    .dateLicenciement(textOrNull(node, "date_licenciement"))
                    .congesContractuels(intOrNull(node, "conges_contractuels"))
                    .primeAncienneteContractuelle(doubleOrNull(node, "prime_anciennete_contractuelle"))
                    .nomSalarie(textOrNull(node, "nom_salarie"))
                    .prenomSalarie(textOrNull(node, "prenom_salarie"))
                    .adresseSalarie(textOrNull(node, "adresse_salarie"))
                    .nomEmployeur(textOrNull(node, "nom_employeur"))
                    .adresseEmployeur(textOrNull(node, "adresse_employeur"))
                    .siretEmployeur(normalizeFrIdentifier(textOrNull(node, "siret_employeur")))
                    .bceEmployeur(normalizeBeBceIdentifier(textOrNull(node, "bce_employeur")))
                    .representantEmployeur(textOrNull(node, "representant_employeur"))
                    // SF-130-01 : flag IA "salaire déduit d'un net"
                    .salaireEstDeduit(booleanOrNull(node, "salaire_est_deduit"))
                    // SF-155-04-00-BE-travail : 5 champs IA pour F-DT-11 / F-DT-15 / F-DT-19
                    .motifNullitePressenti(normalizeEnumCode(textOrNull(node, "motif_nullite_pressenti"), MOTIFS_NULLITE_CODES))
                    .origineInaptitudePressentie(normalizeEnumCode(textOrNull(node, "origine_inaptitude_pressentie"), ORIGINE_INAPTITUDE_CODES))
                    .avisMedecinTravailDate(textOrNull(node, "avis_medecin_travail_date"))
                    .reclassementRespecteDetected(extractDetectedAnswer(node.get("reclassement_respecte_detected")))
                    .heuresSupMentionneesDansDossier(extractHeuresSupMentionnees(node.get("heures_sup_mentionnees")))
                    // SF-166-01 : 8 flags décisionnels niveau 3 — fail-safe à false
                    .rappelSalaireDetecte(booleanOrFalse(node, "rappel_salaire_detecte"))
                    .travailDissimuleDetecte(booleanOrFalse(node, "travail_dissimule_detecte"))
                    .clauseNonConcurrenceDetectee(booleanOrFalse(node, "clause_non_concurrence_detectee"))
                    .statutProtegeDetecte(booleanOrFalse(node, "statut_protege_detecte"))
                    .transactionEnvisagee(booleanOrFalse(node, "transaction_envisagee"))
                    .atMpDetecte(booleanOrFalse(node, "at_mp_detecte"))
                    .urgenceProcedurale(booleanOrFalse(node, "urgence_procedurale"))
                    .contestationAreEnvisagee(booleanOrFalse(node, "contestation_are_envisagee"))
                    // F-204 : 5 flags décisionnels niveau 3 — Travail BE uniquement, fail-safe à false
                    .harcelementBeDetecte(booleanOrFalse(node, "harcelement_be_detecte"))
                    .discriminationBeDetectee(booleanOrFalse(node, "discrimination_be_detectee"))
                    .inaptitudeMedicaleBeDetectee(booleanOrFalse(node, "inaptitude_medicale_be_detectee"))
                    .heuresSupMentionneesBe(booleanOrFalse(node, "heures_sup_mentionnees_be"))
                    .motifGraveBeEnvisage(booleanOrFalse(node, "motif_grave_be_envisage"))
                    // F-205 : 23 flags décisionnels niveau 3 additionnels — Travail FR uniquement, fail-safe à false
                    .abandonPosteDetecte(booleanOrFalse(node, "abandon_poste_detecte"))
                    .arretMaladieLongDetecte(booleanOrFalse(node, "arret_maladie_long_detecte"))
                    .priseActeEnvisagee(booleanOrFalse(node, "prise_acte_envisagee"))
                    .resiliationJudiciaireEnvisagee(booleanOrFalse(node, "resiliation_judiciaire_envisagee"))
                    .forfaitJoursDetecte(booleanOrFalse(node, "forfait_jours_detecte"))
                    .transfertEntrepriseDetecte(booleanOrFalse(node, "transfert_entreprise_detecte"))
                    .fauteInexcusableEnvisagee(booleanOrFalse(node, "faute_inexcusable_envisagee"))
                    .csCrpEnvisage(booleanOrFalse(node, "cs_crp_envisage"))
                    .cspPropose(booleanOrFalse(node, "csp_propose"))
                    .mutationRefusee(booleanOrFalse(node, "mutation_refusee"))
                    .modificationContratRefusee(booleanOrFalse(node, "modification_contrat_refusee"))
                    .fauteGraveEnvisagee(booleanOrFalse(node, "faute_grave_envisagee"))
                    .fauteLourdeEnvisagee(booleanOrFalse(node, "faute_lourde_envisagee"))
                    .cddRequalificationEnvisagee(booleanOrFalse(node, "cdd_requalification_envisagee"))
                    .interimRequalificationEnvisagee(booleanOrFalse(node, "interim_requalification_envisagee"))
                    .forfaitJoursValiditeContestee(booleanOrFalse(node, "forfait_jours_validite_contestee"))
                    .prescriptionProcheDetectee(booleanOrFalse(node, "prescription_proche_detectee"))
                    .ruptureAmiableNegociee(booleanOrFalse(node, "rupture_amiable_negociee"))
                    .entretienPreavisObtenu(booleanOrFalse(node, "entretien_preavis_obtenu"))
                    .cseConsultationDemandee(booleanOrFalse(node, "cse_consultation_demandee"))
                    .irpElectionDemandee(booleanOrFalse(node, "irp_election_demandee"))
                    .inspectionTravailSaisie(booleanOrFalse(node, "inspection_travail_saisie"))
                    .mediationJudiciaireEnvisagee(booleanOrFalse(node, "mediation_judiciaire_envisagee"))
                    // SF-246-01 : 6 champs procéduraux pour pré-fill F-DT-36 — booléens via
                    // booleanOrNull(), dates ISO validées (fail-open → null si non ISO),
                    // appréciations via extractDetectedAnswer(). Tous null si sous-objet absent.
                    .convocationEntretienDetectee(hasProcedure ? booleanOrNull(procedure, "convocation_entretien_detectee") : null)
                    .dateConvocationEntretienDetectee(hasProcedure ? isoDateOrNull(procedure, "date_convocation_entretien") : null)
                    .dateEntretienPrealableDetectee(hasProcedure ? isoDateOrNull(procedure, "date_entretien_prealable") : null)
                    .entretienPrealableTenuDetected(hasProcedure ? extractDetectedAnswer(procedure.get("entretien_prealable_tenu")) : null)
                    .lettreLicenciementEcriteDetectee(hasProcedure ? booleanOrNull(procedure, "lettre_licenciement_ecrite") : null)
                    .lettreLicenciementMotiveeDetected(hasProcedure ? extractDetectedAnswer(procedure.get("lettre_licenciement_motivee")) : null)
                    .motivationLettreSuffisanteDetected(hasProcedure ? extractDetectedAnswer(procedure.get("motivation_lettre_suffisante")) : null)
                    // SF-246-02 : 3 champs IA pour pré-fill F-DT-24 — durée bornée [0, 600]
                    // mois, zone tronquée à 500 car., contrepartie strictement positive.
                    // Tous null si sous-objet clause_non_concurrence_detail absent.
                    .nonConcurrenceDureeMois(hasClauseNc ? boundedIntOrNull(clauseNc, "duree_mois", 0, MAX_NON_CONCURRENCE_DUREE_MOIS) : null)
                    .nonConcurrenceZoneGeographique(hasClauseNc ? truncatedTextOrNull(clauseNc, "zone_geographique", MAX_NON_CONCURRENCE_ZONE_LENGTH) : null)
                    .nonConcurrenceContrepartieMontantEur(hasClauseNc ? positiveDoubleOrNull(clauseNc, "contrepartie_montant_mensuel_eur") : null)
                    // SF-246-13 : 2 champs IA complétant le pré-fill F-DT-24 — date de
                    // prise d'effet validée ISO YYYY-MM-DD (fail-open → null si non ISO),
                    // secteur d'activité normalisé sur l'enum SecteurActivite (code hors
                    // liste → null). Tous null si sous-objet clause_non_concurrence_detail
                    // absent.
                    .nonConcurrenceDatePriseEffet(hasClauseNc ? isoDateOrNull(clauseNc, "date_prise_effet") : null)
                    .nonConcurrenceSecteurActivite(hasClauseNc ? normalizeEnumCode(textOrNull(clauseNc, "secteur_activite"), SECTEUR_ACTIVITE_CODES) : null)
                    // SF-246-05 : âge du demandeur pour pré-fill F-DT-29 crédit-temps fin
                    // de carrière — entier borné [0, 100], null hors plage / absent / BE
                    // non concerné. Le prompt impose null hors Belgique.
                    .ageDemandeurAnnees(boundedIntOrNull(node, "age_demandeur_annees", 0, MAX_AGE_DEMANDEUR_ANNEES))
                    // SF-207-01 : 2 champs IA Travail BE pour pré-fill F-207-01
                    // prescription Travail BE. dateRuptureContrat validée ISO
                    // YYYY-MM-DD (fail-open → null) ; motifRupture en texte libre.
                    // Le prompt impose null pour un dossier travail FR.
                    .dateRuptureContrat(isoDateOrNull(node, "date_rupture_contrat"))
                    .motifRupture(textOrNull(node, "motif_rupture"))
                    // SF-207-02 : 6 champs IA Travail BE pour pré-fill F-207-02
                    // C4 ONEM checklist. Le prompt impose null hors Belgique.
                    // numeroBce normalisé via le helper BE existant (suppression
                    // préfixe BE/points). dernierSalaireMensuelBrut décodé en
                    // BigDecimal pour préserver la précision.
                    .raisonSocialeEmployeur(textOrNull(node, "raison_sociale_employeur"))
                    .numeroBce(normalizeBeBceIdentifier(textOrNull(node, "numero_bce")))
                    .categorieOnem(textOrNull(node, "categorie_onem"))
                    .motifExplicite(textOrNull(node, "motif_explicite"))
                    .preavisPresteJours(nonNegativeIntOrNull(node, "preavis_preste_jours"))
                    .dernierSalaireMensuelBrut(bigDecimalOrNull(node, "dernier_salaire_mensuel_brut"))
                    // SF-246-22 : type de procédure travail + date déclencheur pour pré-fill
                    // F-136 travail-procedure (FR+BE). Sous-objet procedure_travail_detection.
                    .procedureTravailDetectee(extractProcedureTravailCode(node))
                    .dateDeclencheurProcedure(extractProcedureTravailDate(node))
                    .build();
        } catch (Exception ignored) { return null; }
    }

    /**
     * SF-246-22 : extrait et whitelizte le code de procédure travail depuis
     * {@code travail_extracted_data.procedure_travail_detection.procedure_detectee}.
     * Codes admis (6 exacts — 3 FR + 3 BE) :
     * {@code PRUDHOMMES_FR, APPEL_CA_SOCIALE_FR, CASSATION_SOCIALE_FR,
     * TRIBUNAL_TRAVAIL_BE, COUR_TRAVAIL_BE, CASSATION_BE}.
     * Tout code hors whitelist → null (fail-open).
     */
    private static final java.util.Set<String> PROCEDURE_TRAVAIL_WHITELIST = java.util.Set.of(
            "PRUDHOMMES_FR", "APPEL_CA_SOCIALE_FR", "CASSATION_SOCIALE_FR",
            "TRIBUNAL_TRAVAIL_BE", "COUR_TRAVAIL_BE", "CASSATION_BE");

    private static String extractProcedureTravailCode(JsonNode travailNode) {
        JsonNode ptd = travailNode.get("procedure_travail_detection");
        if (ptd == null || !ptd.isObject()) return null;
        JsonNode codeNode = ptd.get("procedure_detectee");
        if (codeNode == null || codeNode.isNull() || !codeNode.isTextual()) return null;
        String code = codeNode.asText().trim().toUpperCase();
        return PROCEDURE_TRAVAIL_WHITELIST.contains(code) ? code : null;
    }

    /** SF-246-22 : extrait la date déclencheur (ISO YYYY-MM-DD) depuis le sous-objet. */
    private static String extractProcedureTravailDate(JsonNode travailNode) {
        JsonNode ptd = travailNode.get("procedure_travail_detection");
        if (ptd == null || !ptd.isObject()) return null;
        return isoDateOrNull(ptd, "date_declencheur");
    }

    /** SF-155-04-00-BE-travail : upper-case puis check whitelist, null sinon (fail-open). */
    private static String normalizeEnumCode(String raw, Set<String> allowed) {
        if (raw == null) return null;
        String up = raw.trim().toUpperCase();
        return allowed.contains(up) ? up : null;
    }

    /** SF-155-04-00-BE-travail : parse un objet {reponse, justification} avec troncature 500 car. */
    private static DetectedAnswer extractDetectedAnswer(JsonNode node) {
        if (node == null || !node.isObject()) return null;
        String reponseRaw = node.has("reponse") && !node.get("reponse").isNull() ? node.get("reponse").asText() : null;
        String reponse = normalizeReponse(reponseRaw);
        String justification = node.has("justification") && !node.get("justification").isNull()
                ? node.get("justification").asText() : null;
        if (justification != null && justification.length() > MAX_JUSTIFICATION_LENGTH) {
            justification = justification.substring(0, MAX_JUSTIFICATION_LENGTH);
        }
        return new DetectedAnswer(reponse, justification);
    }

    /** SF-155-04-00-BE-travail : parse objet heures sup agrégé (3 entiers nullable). */
    private static HeuresSupMentionnees extractHeuresSupMentionnees(JsonNode node) {
        if (node == null || !node.isObject()) return null;
        Integer t25 = nonNegativeIntOrNull(node, "total_declarees_25pct");
        Integer t50 = nonNegativeIntOrNull(node, "total_declarees_50pct");
        Integer hc = nonNegativeIntOrNull(node, "hors_contingent");
        if (t25 == null && t50 == null && hc == null) return null;
        return new HeuresSupMentionnees(t25, t50, hc);
    }

    /**
     * SF-155-04-00-BE-travail : entier ≥ 0, null si absent, négatif, non numérique,
     * ou texte (ex. "trente" — Jackson coerce les textes en 0 via intValue(),
     * on exige donc explicitement un node numérique).
     */
    private static Integer nonNegativeIntOrNull(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull() || !node.get(field).isNumber()) return null;
        int v = node.get(field).intValue();
        return v >= 0 ? v : null;
    }

    /** Normalise un SIREN/SIRET : garde uniquement les chiffres, null si 0 chiffre, sinon renvoie la chaîne. */
    static String normalizeFrIdentifier(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : digits;
    }

    /** Normalise un BCE belge : retire le préfixe `BE`, les espaces/points, garde les chiffres. */
    static String normalizeBeBceIdentifier(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : digits;
    }

    static LicenciementValidityDetection extractLicenciementValidityDetection(JsonNode root) {
        JsonNode node = root.get("licenciement_validity_detection");
        if (node == null || !node.isObject() || node.size() == 0) return null;
        Map<String, DetectedAnswer> detections = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> {
            String code = entry.getKey();
            if (!LICENCIEMENT_CRITERE_CODES.contains(code)) return;
            JsonNode value = entry.getValue();
            if (value == null || !value.isObject()) return;
            String reponse = normalizeReponse(textOrNull(value, "reponse"));
            String justification = textOrNull(value, "justification");
            if (justification != null && justification.length() > MAX_JUSTIFICATION_LENGTH) {
                justification = justification.substring(0, MAX_JUSTIFICATION_LENGTH);
            }
            detections.put(code, new DetectedAnswer(reponse, justification));
        });
        return detections.isEmpty() ? null : new LicenciementValidityDetection(detections);
    }

    /** F-152 SF-152-01 : parseur détection validité divorce consentement mutuel. */
    static DivorceConsentementValidityDetection extractDivorceConsentementValidityDetection(JsonNode root) {
        JsonNode node = root.get("divorce_consentement_validity_detection");
        if (node == null || !node.isObject() || node.size() == 0) return null;
        Map<String, DetectedAnswer> detections = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> {
            String code = entry.getKey() == null ? null : entry.getKey().toUpperCase();
            if (code == null || !DIVORCE_CONSENTEMENT_CRITERE_CODES.contains(code)) return;
            JsonNode value = entry.getValue();
            if (value == null || !value.isObject()) return;
            String reponse = normalizeReponse(textOrNull(value, "reponse"));
            String justification = textOrNull(value, "justification");
            if (justification != null && justification.length() > MAX_JUSTIFICATION_LENGTH) {
                justification = justification.substring(0, MAX_JUSTIFICATION_LENGTH);
            }
            detections.put(code, new DetectedAnswer(reponse, justification));
        });
        return detections.isEmpty() ? null : new DivorceConsentementValidityDetection(detections);
    }

    /**
     * F-152 SF-152-01 : calcul du scoring à partir de la détection.
     * Score = (nombre de OUI / 7) × 100, arrondi. INCONNU compte comme manquant.
     * Verdict : VALIDE (≥ 85), RISQUE_MOYEN (50-84), RISQUE_ELEVE_NULLITE (< 50).
     */
    static DivorceConsentementScoring computeDivorceConsentementScoring(
            DivorceConsentementValidityDetection detection) {
        if (detection == null || detection.detections().isEmpty()) return null;

        List<String> oui = new ArrayList<>();
        List<String> non = new ArrayList<>();
        List<String> inconnu = new ArrayList<>();

        for (String code : DIVORCE_CONSENTEMENT_CRITERE_CODES) {
            DetectedAnswer answer = detection.detections().get(code);
            if (answer == null) {
                inconnu.add(code);
            } else if ("OUI".equals(answer.reponse())) {
                oui.add(code);
            } else if ("NON".equals(answer.reponse())) {
                non.add(code);
            } else {
                inconnu.add(code);
            }
        }

        int total = DIVORCE_CONSENTEMENT_CRITERE_CODES.size();
        int score = (int) Math.round(oui.size() * 100.0 / total);
        String verdict = score >= 85 ? "VALIDE"
                : score >= 50 ? "RISQUE_MOYEN"
                : "RISQUE_ELEVE_NULLITE";
        return new DivorceConsentementScoring(score, verdict, oui, non, inconnu);
    }

    static RuptureConvValidityDetection extractRuptureConvValidityDetection(JsonNode root) {
        JsonNode node = root.get("rupture_conv_validity_detection");
        if (node == null || !node.isObject() || node.size() == 0) return null;
        Map<String, DetectedAnswer> detections = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> {
            String code = entry.getKey() == null ? null : entry.getKey().toUpperCase();
            if (code == null || !RUPTURE_CONV_CRITERE_CODES.contains(code)) return;
            JsonNode value = entry.getValue();
            if (value == null || !value.isObject()) return;
            String reponse = normalizeReponse(textOrNull(value, "reponse"));
            String justification = textOrNull(value, "justification");
            if (justification != null && justification.length() > MAX_JUSTIFICATION_LENGTH) {
                justification = justification.substring(0, MAX_JUSTIFICATION_LENGTH);
            }
            detections.put(code, new DetectedAnswer(reponse, justification));
        });
        return detections.isEmpty() ? null : new RuptureConvValidityDetection(detections);
    }

    private static String normalizeReponse(String raw) {
        if (raw == null) return "INCONNU";
        String up = raw.trim().toUpperCase();
        return (up.equals("OUI") || up.equals("NON")) ? up : "INCONNU";
    }

    static ImmigrationExtractedData extractImmigrationData(JsonNode root) {
        String dateExpiration = textOrNull(root, "date_expiration_titre");
        String typeTitre = textOrNull(root, "type_titre_sejour");
        String typeProcedure = textOrNull(root, "type_procedure_detectee");
        String dateDepot = textOrNull(root, "date_depot_procedure");
        String typeCode = normalizeTitleCode(textOrNull(root, "type_titre_sejour_code"));
        Boolean nationaliteUe = normalizeNationaliteUe(root.get("nationalite_ue"));
        String recoursCode = normalizeRecoursCode(textOrNull(root, "type_recours_code"));
        String dateNotif = textOrNull(root, "date_notification_decision_contestee");
        // SF-155-04-00-BE-immig-FR : 5 champs IA pour F-IM-08-02 / F-IM-08-04.
        String dateNotifOqtf = textOrNull(root, "date_notification_oqtf");
        String motifOqtfCode = normalizeEnumCode(textOrNull(root, "motif_oqtf_code"), MOTIFS_OQTF_FR_CODES);
        DetectedAnswer recoursFormeDetected = extractDetectedAnswer(root.get("recours_forme_detected"));
        String dateHeureNotifOqtfSansDelai = validateOqtfSansDelaiDateTime(textOrNull(root, "date_heure_notification_oqtf_sans_delai"));
        Boolean placementCraDetected = booleanOrNull(root, "placement_cra_detected");
        // SF-155-04-00-BE-immig-BE : 4 champs IA pour F-IM-08 Annexe 13 BE (dossiers BE uniquement,
        // null pour dossiers FR — cf. section IMMIGRATION_INSTRUCTION du prompt).
        String dateAnnexe13 = textOrNull(root, "date_notification_annexe13");
        Integer delaiDepart = nonNegativeIntOrNull(root, "delai_depart_impose_jours");
        String motifOqtBe = normalizeEnumCode(textOrNull(root, "motif_oqt_code_be"), MOTIFS_OQT_BE_CODES);
        Boolean transfertImminent = booleanOrNull(root, "transfert_imminent_detected");
        // F-201 : 9 flags décisionnels niveau 3 — Immigration FR uniquement, fail-safe à false.
        boolean aesMetiersTension = booleanOrFalse(root, "aes_metiers_tension_eligible_detecte");
        boolean aesFamilial = booleanOrFalse(root, "aes_familial_eligible_detecte");
        boolean aesHumanitaire = booleanOrFalse(root, "aes_humanitaire_eligible_detecte");
        boolean aesEtudiant = booleanOrFalse(root, "aes_etudiant_eligible_detecte");
        boolean changementStatut = booleanOrFalse(root, "changement_statut_envisage_detecte");
        boolean procedureAsile = booleanOrFalse(root, "procedure_asile_detectee");
        boolean naturalisationEnvisagee = booleanOrFalse(root, "naturalisation_envisagee_detectee");
        boolean clientMineur = booleanOrFalse(root, "client_mineur_detecte");
        boolean mesureEloignement = booleanOrFalse(root, "mesure_eloignement_detectee");
        // F-203 : 5 flags décisionnels niveau 3 — Immigration BE uniquement, fail-safe à false.
        boolean procedure9bis = booleanOrFalse(root, "procedure_9bis_envisagee");
        boolean procedure9ter = booleanOrFalse(root, "procedure_9ter_medicale_detectee");
        boolean regroupement40bis = booleanOrFalse(root, "regroupement_40bis_detecte");
        boolean regroupement40ter = booleanOrFalse(root, "regroupement_40ter_detecte");
        boolean oqtAnnexe13 = booleanOrFalse(root, "oqt_annexe13_detectee");
        // F-235 : nationalite (texte libre, ex. "Algérienne") — consommée par
        // DecisionToolVisibilityService pour activer F-IM-17 et autres outils
        // décisionnels conditionnés à un régime national bilatéral.
        String nationalite = textOrNull(root, "nationalite");
        // SF-246-04 : date de l'ordonnance de protection JAF (Cciv 515-9) — Immigration FR uniquement,
        // pré-fill F-IM-24 victime de violences L.425-6. Texte brut conservé : le pré-fill front
        // rejette tout format non ISO via ISO_DATE_RE.
        String dateOrdonnanceProtectionJaf = textOrNull(root, "date_ordonnance_protection_jaf");
        // SF-246-16 : identité requérant + référence décision contestée pour pré-fill F-IM-06.
        String nomRequerant = textOrNull(root, "nom_requerant");
        String prenomRequerant = textOrNull(root, "prenom_requerant");
        String dateDecisionContestee = textOrNull(root, "date_decision_contestee");
        String referenceDecision = textOrNull(root, "reference_decision");
        // SF-246-17 : pré-fill dublin-recours (F-IM-22) + crrv-refus-visa (F-IM-23).
        // FR uniquement — le prompt impose null pour dossiers BE.
        String dublinEtatMembre = truncatedTextOrNull(root, "dublin_etat_membre_responsable", MAX_DUBLIN_ETAT_MEMBRE_LENGTH);
        String dublinMotifTransfert = normalizeEnumCode(textOrNull(root, "dublin_motif_transfert"), MOTIFS_TRANSFERT_DUBLIN_CODES);
        String crrvTypeVisa = normalizeEnumCode(textOrNull(root, "crrv_type_visa"), TYPES_VISA_CRRV_CODES);
        String crrvMotifRefus = truncatedTextOrNull(root, "crrv_motif_refus", MAX_CRRV_MOTIF_REFUS_LENGTH);
        // SF-246-18 : pré-fill outils AES Immigration FR (aes-etudiant / aes-famille /
        // aes-humanitaire / aes-metiers-tension). FR uniquement — null pour dossiers BE.
        String aesDateEntreeFranceRaw = textOrNull(root, "aes_date_entree_france");
        // Guard : ISO YYYY-MM-DD strict, date non future.
        final String ISO_DATE_PATTERN_STR = "\\d{4}-\\d{2}-\\d{2}";
        String aesDateEntreeFrance = null;
        Integer aesDureePresenceMois = null;
        if (aesDateEntreeFranceRaw != null && aesDateEntreeFranceRaw.matches(ISO_DATE_PATTERN_STR)) {
            String todayStr = java.time.LocalDate.now().toString();
            if (aesDateEntreeFranceRaw.compareTo(todayStr) <= 0) {
                aesDateEntreeFrance = aesDateEntreeFranceRaw;
                java.time.LocalDate entree = java.time.LocalDate.parse(aesDateEntreeFranceRaw);
                java.time.LocalDate today = java.time.LocalDate.now();
                aesDureePresenceMois = (int) java.time.temporal.ChronoUnit.MONTHS.between(entree, today);
            }
        }
        Integer aesAnneesScolariteConsecutives = nonNegativeIntOrNull(root, "aes_annees_scolarite_consecutives");
        String aesNiveauEtudes = normalizeEnumCode(textOrNull(root, "aes_niveau_etudes"), AES_NIVEAU_ETUDES_CODES);
        Integer aesDureeScolaritePlusAncienEnfantAnnees = nonNegativeIntOrNull(root, "aes_duree_scolarite_plus_ancien_enfant_annees");
        String aesMotifHumanitaire = normalizeEnumCode(textOrNull(root, "aes_motif_humanitaire"), AES_MOTIFS_HUMANITAIRES_CODES);
        // aesMoisActiviteSalariee : entier [0–24] ; au-delà → null.
        Integer aesMoisActiviteSalarieeRaw = nonNegativeIntOrNull(root, "aes_mois_activite_salariee");
        Integer aesMoisActiviteSalariee = (aesMoisActiviteSalarieeRaw != null && aesMoisActiviteSalarieeRaw <= 24)
                ? aesMoisActiviteSalarieeRaw : null;
        String aesCodeMetier = textOrNull(root, "aes_code_metier");
        if (aesCodeMetier != null && aesCodeMetier.isBlank()) aesCodeMetier = null;
        // SF-246-19 : pré-fill statut & dispositifs Immigration FR
        // Changement de statut : titre envisagé (réutilise IMMIGRATION_TITLE_CODES) + rémunération
        String changementTitreEnvisage = normalizeEnumCode(
                textOrNull(root, "changement_titre_envisage"), IMMIGRATION_TITLE_CODES);
        Integer changementRemunerationEurRaw = nonNegativeIntOrNull(root, "changement_remuneration_eur");
        Integer changementRemunerationEur = (changementRemunerationEurRaw != null
                && changementRemunerationEurRaw > 0
                && changementRemunerationEurRaw <= MAX_CHANGEMENT_REMUNERATION_EUR)
                ? changementRemunerationEurRaw : null;
        // Naturalisation : durée résidence + durée mariage + âge demandeur
        Integer natDureeResidenceReguliereAnnees = boundedIntOrNull(
                root, "nat_duree_residence_reguliere_annees", 0, MAX_NAT_DUREE_ANNEES);
        Integer natDureeMariageAnnees = boundedIntOrNull(
                root, "nat_duree_mariage_annees", 0, MAX_NAT_DUREE_ANNEES);
        Integer natAgeDemandeur = boundedIntOrNull(root, "nat_age_demandeur", 0, MAX_NAT_AGE);
        // Mineurs : date de naissance (ISO non-future)
        String mineursDateNaissanceRaw = textOrNull(root, "mineurs_date_naissance");
        String mineursDateNaissance = null;
        if (mineursDateNaissanceRaw != null && mineursDateNaissanceRaw.matches(ISO_DATE_PATTERN_STR)) {
            String todayStr2 = java.time.LocalDate.now().toString();
            if (mineursDateNaissanceRaw.compareTo(todayStr2) <= 0) {
                mineursDateNaissance = mineursDateNaissanceRaw;
            }
        }
        // Régime algérien : durée présence régulière en mois
        Integer algerienPresenceReguliereMois = boundedIntOrNull(
                root, "algerien_presence_reguliere_mois", 0, MAX_PRESENCE_MOIS);
        // Asile avancé : date décision antérieure (ISO non-future)
        String asileDateDecisionAnterieureRaw = textOrNull(root, "asile_date_decision_anterieure");
        String asileDateDecisionAnterieure = null;
        if (asileDateDecisionAnterieureRaw != null
                && asileDateDecisionAnterieureRaw.matches(ISO_DATE_PATTERN_STR)) {
            String todayStr3 = java.time.LocalDate.now().toString();
            if (asileDateDecisionAnterieureRaw.compareTo(todayStr3) <= 0) {
                asileDateDecisionAnterieure = asileDateDecisionAnterieureRaw;
            }
        }
        // Mesures d'éloignement : durée présence irrégulière + motif menace
        Integer eloiDureePresenceIrreguliereMois = boundedIntOrNull(
                root, "eloi_duree_presence_irreguliere_mois", 0, MAX_PRESENCE_MOIS);
        String eloiMotifMenace = normalizeEnumCode(
                textOrNull(root, "eloi_motif_menace"), ELOI_MOTIFS_MENACE_CODES);
        if (dateExpiration == null && typeTitre == null && typeProcedure == null
                && dateDepot == null && typeCode == null && nationaliteUe == null
                && recoursCode == null && dateNotif == null
                && dateNotifOqtf == null && motifOqtfCode == null && recoursFormeDetected == null
                && dateHeureNotifOqtfSansDelai == null && placementCraDetected == null
                && dateAnnexe13 == null && delaiDepart == null
                && motifOqtBe == null && transfertImminent == null
                && !aesMetiersTension && !aesFamilial && !aesHumanitaire && !aesEtudiant
                && !changementStatut && !procedureAsile && !naturalisationEnvisagee
                && !clientMineur && !mesureEloignement
                && !procedure9bis && !procedure9ter && !regroupement40bis
                && !regroupement40ter && !oqtAnnexe13
                && nationalite == null
                && dateOrdonnanceProtectionJaf == null
                && nomRequerant == null && prenomRequerant == null
                && dateDecisionContestee == null && referenceDecision == null
                && dublinEtatMembre == null && dublinMotifTransfert == null
                && crrvTypeVisa == null && crrvMotifRefus == null
                && aesDateEntreeFrance == null && aesAnneesScolariteConsecutives == null
                && aesNiveauEtudes == null && aesDureeScolaritePlusAncienEnfantAnnees == null
                && aesMotifHumanitaire == null && aesMoisActiviteSalariee == null
                && aesCodeMetier == null
                && changementTitreEnvisage == null && changementRemunerationEur == null
                && natDureeResidenceReguliereAnnees == null && natDureeMariageAnnees == null
                && natAgeDemandeur == null && mineursDateNaissance == null
                && algerienPresenceReguliereMois == null && asileDateDecisionAnterieure == null
                && eloiDureePresenceIrreguliereMois == null && eloiMotifMenace == null) return null;
        // F-234 SF-234-01 : construction via Builder.
        return ImmigrationExtractedData.builder()
                .dateExpirationTitre(dateExpiration)
                .typeTitreSejour(typeTitre)
                .typeProcedureDetectee(typeProcedure)
                .dateDepotProcedure(dateDepot)
                .typeTitreSejourCode(typeCode)
                .nationaliteUe(nationaliteUe)
                .typeRecoursCode(recoursCode)
                .dateNotificationDecisionContestee(dateNotif)
                // inferredChecklistType : null à ce stade — peuplé en aval dans from() après inférence.
                // SF-155-04-00-BE-immig-FR : 5 champs pour F-IM-08-02 / F-IM-08-04
                .dateNotificationOqtf(dateNotifOqtf)
                .motifOqtfCode(motifOqtfCode)
                .recoursFormeDetected(recoursFormeDetected)
                .dateHeureNotificationOqtfSansDelai(dateHeureNotifOqtfSansDelai)
                .placementCraDetected(placementCraDetected)
                // SF-155-04-00-BE-immig-BE : 4 champs pour F-IM-08 Annexe 13 BE
                .dateNotificationAnnexe13(dateAnnexe13)
                .delaiDepartImposeJours(delaiDepart)
                .motifOqtCodeBe(motifOqtBe)
                .transfertImminentDetected(transfertImminent)
                // F-201 : 9 flags Immigration FR
                .aesMetiersTensionEligibleDetecte(aesMetiersTension)
                .aesFamilialEligibleDetecte(aesFamilial)
                .aesHumanitaireEligibleDetecte(aesHumanitaire)
                .aesEtudiantEligibleDetecte(aesEtudiant)
                .changementStatutEnvisageDetecte(changementStatut)
                .procedureAsileDetectee(procedureAsile)
                .naturalisationEnvisageeDetectee(naturalisationEnvisagee)
                .clientMineurDetecte(clientMineur)
                .mesureEloignementDetectee(mesureEloignement)
                // F-203 : 5 flags Immigration BE
                .procedure9bisEnvisagee(procedure9bis)
                .procedure9terMedicaleDetectee(procedure9ter)
                .regroupement40bisDetecte(regroupement40bis)
                .regroupement40terDetecte(regroupement40ter)
                .oqtAnnexe13Detectee(oqtAnnexe13)
                // F-235 : nationalite (texte libre)
                .nationalite(nationalite)
                // SF-246-04 : date ordonnance de protection JAF (pré-fill F-IM-24)
                .dateOrdonnanceProtectionJaf(dateOrdonnanceProtectionJaf)
                // SF-246-16 : identité requérant + référence décision contestée (pré-fill F-IM-06)
                .nomRequerant(nomRequerant)
                .prenomRequerant(prenomRequerant)
                .dateDecisionContestee(dateDecisionContestee)
                .referenceDecision(referenceDecision)
                // SF-246-17 : pré-fill dublin-recours + crrv-refus-visa
                .dublinEtatMembreResponsable(dublinEtatMembre)
                .dublinMotifTransfert(dublinMotifTransfert)
                .crrvTypeVisa(crrvTypeVisa)
                .crrvMotifRefus(crrvMotifRefus)
                // SF-246-18 : pré-fill outils AES Immigration FR
                .aesDateEntreeFrance(aesDateEntreeFrance)
                .aesDureePresenceMois(aesDureePresenceMois)
                .aesAnneesScolariteConsecutives(aesAnneesScolariteConsecutives)
                .aesNiveauEtudes(aesNiveauEtudes)
                .aesDureeScolaritePlusAncienEnfantAnnees(aesDureeScolaritePlusAncienEnfantAnnees)
                .aesMotifHumanitaire(aesMotifHumanitaire)
                .aesMoisActiviteSalariee(aesMoisActiviteSalariee)
                .aesCodeMetier(aesCodeMetier)
                // SF-246-19 : pré-fill statut & dispositifs Immigration FR
                .changementTitreEnvisage(changementTitreEnvisage)
                .changementRemunerationEur(changementRemunerationEur)
                .natDureeResidenceReguliereAnnees(natDureeResidenceReguliereAnnees)
                .natDureeMariageAnnees(natDureeMariageAnnees)
                .natAgeDemandeur(natAgeDemandeur)
                .mineursDateNaissance(mineursDateNaissance)
                .algerienPresenceReguliereMois(algerienPresenceReguliereMois)
                .asileDateDecisionAnterieure(asileDateDecisionAnterieure)
                .eloiDureePresenceIrreguliereMois(eloiDureePresenceIrreguliereMois)
                .eloiMotifMenace(eloiMotifMenace)
                .build();
    }

    /**
     * SF-155-04-00-BE-immig-FR : valide l'horodatage OQTF sans délai via regex permissive.
     * Accepte {@code YYYY-MM-DDTHH:mm} et {@code YYYY-MM-DDTHH:mm:ss}. Retourne {@code null}
     * pour tout autre format (fail-open).
     */
    private static String validateOqtfSansDelaiDateTime(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        return OQTF_SANS_DELAI_DATETIME_PATTERN.matcher(trimmed).matches() ? trimmed : null;
    }

    private static String normalizeRecoursCode(String raw) {
        if (raw == null) return null;
        String up = raw.trim().toUpperCase();
        return IMMIGRATION_RECOURS_CODES.contains(up) ? up : null;
    }

    private static String normalizeTitleCode(String raw) {
        if (raw == null) return null;
        String up = raw.trim().toUpperCase();
        return IMMIGRATION_TITLE_CODES.contains(up) ? up : null;
    }

    private static Boolean normalizeNationaliteUe(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isBoolean()) return node.booleanValue();
        if (node.isTextual()) {
            String s = node.asText().trim().toLowerCase();
            if ("true".equals(s)) return Boolean.TRUE;
            if ("false".equals(s)) return Boolean.FALSE;
        }
        return null;
    }

    private static String textOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private static Double doubleOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).doubleValue() : null;
    }

    /**
     * SF-207-02 : décodage d'un montant en {@link java.math.BigDecimal} pour
     * préserver la précision décimale (ex. dernier salaire mensuel brut du
     * C4 ONEM). Retourne {@code null} si le champ est absent, null, ou si la
     * valeur ne peut pas être décodée en BigDecimal.
     */
    private static java.math.BigDecimal bigDecimalOrNull(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) return null;
        try {
            return node.get(field).decimalValue();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Integer intOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).intValue() : null;
    }

    /**
     * SF-246-02 : entier borné à {@code [min, max]} inclus. Retourne {@code null} si
     * le champ est absent, null, non numérique, ou hors plage (garde anti-valeur
     * aberrante — ex. durée de clause de non-concurrence négative ou {@literal >} 600 mois).
     */
    private static Integer boundedIntOrNull(JsonNode node, String field, int min, int max) {
        if (!node.has(field) || node.get(field).isNull() || !node.get(field).isNumber()) return null;
        int v = node.get(field).intValue();
        return v >= min && v <= max ? v : null;
    }

    /**
     * SF-246-02 : double strictement positif. Retourne {@code null} si le champ est
     * absent, null, non numérique, ou {@literal <=} 0 (invariant cadrage §5.2 — un
     * montant non identifié de façon fiable reste null, jamais 0).
     */
    private static Double positiveDoubleOrNull(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull() || !node.get(field).isNumber()) return null;
        double v = node.get(field).doubleValue();
        return v > 0 ? v : null;
    }

    /**
     * SF-246-02 : texte trimmé et tronqué à {@code maxLength} caractères. Retourne
     * {@code null} si le champ est absent, null, non textuel ou vide après trim.
     */
    private static String truncatedTextOrNull(JsonNode node, String field, int maxLength) {
        if (!node.has(field) || node.get(field).isNull() || !node.get(field).isTextual()) return null;
        String s = node.get(field).asText().trim();
        if (s.isEmpty()) return null;
        return s.length() > maxLength ? s.substring(0, maxLength) : s;
    }

    private static Boolean booleanOrNull(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) return null;
        JsonNode v = node.get(field);
        if (v.isBoolean()) return v.asBoolean();
        if (v.isTextual()) {
            String s = v.asText().toLowerCase();
            if ("true".equals(s)) return true;
            if ("false".equals(s)) return false;
        }
        return null;
    }

    /**
     * SF-166-01 : variante fail-safe de {@link #booleanOrNull} pour les 8 flags décisionnels niveau 3.
     * Retourne {@code true} uniquement si le JSON contient explicitement {@code true} ou la chaîne
     * {@code "true"}. Tout autre cas (champ absent, null, valeur non-boolean) → {@code false}.
     */
    private static boolean booleanOrFalse(JsonNode node, String field) {
        Boolean v = booleanOrNull(node, field);
        return v != null && v;
    }

    /**
     * F-239 : extrait une chaîne d'un nœud JSON, ou null si absent / vide /
     * non textuel. Trim le résultat. Utilisé pour les champs string optionnels
     * extraits par l'IA (ex: `date_accord_initial_divorce` au format YYYY-MM-DD).
     */
    private static String stringOrNull(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        if (v == null || v.isNull() || !v.isTextual()) return null;
        String s = v.asText().trim();
        return s.isEmpty() ? null : s;
    }

    private static final java.util.regex.Pattern ISO_DATE_PATTERN =
            java.util.regex.Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final java.util.regex.Pattern ISO_DATE_ANYWHERE_PATTERN =
            java.util.regex.Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");

    /**
     * SF-246-01 : extrait une date au format ISO strict {@code YYYY-MM-DD}, ou
     * {@code null} si absente, non textuelle ou non conforme (fail-open). Garantit
     * qu'une date renvoyée par le LLM dans un format inattendu ne pré-remplit pas
     * un champ date du formulaire F-DT-36.
     */
    private static String isoDateOrNull(JsonNode node, String field) {
        String raw = textOrNull(node, field);
        if (raw == null) return null;
        String trimmed = raw.trim();
        return ISO_DATE_PATTERN.matcher(trimmed).matches() ? trimmed : null;
    }

    /**
     * F-241 : fallback déterministe pour extraire la date de signature de la
     * convention/PV/accord initial de divorce depuis la `timeline` IA quand le
     * LLM a omis de la peupler dans `famille_extracted_data.date_accord_initial_divorce`.
     *
     * <p>Heuristique : on cherche un événement timeline dont le libellé contient
     * "signature" ou "signé" ET au moins un des mots-clés
     * "convention" / "PV" / "procès-verbal" / "accord". Premier match → date.</p>
     *
     * <p>Filet de sécurité contre la résistance LLM constatée le 2026-05-11
     * (cas Vermeersch BE — date 12/12/2025 présente dans timeline/faits/scoring
     * mais `date_acceptation_pv` retourné null malgré prompt renforcé).</p>
     */
    static String extractDateAccordInitialDivorceFromTimeline(JsonNode root) {
        JsonNode node = root.get("timeline");
        if (node == null || !node.isArray()) return null;
        for (JsonNode item : node) {
            if (!item.isObject()) continue;
            String date = item.has("date") ? item.get("date").asText("") : "";
            String evenement = item.has("evenement") ? item.get("evenement").asText("") : "";
            if (date.isEmpty() || evenement.isEmpty()) continue;
            String norm = evenement.toLowerCase(java.util.Locale.ROOT);
            // Exclusion explicite des marqueurs négatifs avant tout autre check.
            if (norm.contains("non sign") || norm.contains("pas sign") || norm.contains("non-sign")) continue;
            boolean hasSignatureMarker = norm.contains("signature") || norm.contains("signé")
                    || norm.contains("signee");
            if (!hasSignatureMarker) continue;
            boolean hasDocMarker = norm.contains("convention") || norm.contains("pv")
                    || norm.contains("procès-verbal") || norm.contains("proces-verbal")
                    || norm.contains("accord");
            if (!hasDocMarker) continue;
            String trimmed = date.trim();
            if (ISO_DATE_PATTERN.matcher(trimmed).matches()) {
                return trimmed;
            }
            java.util.regex.Matcher m = ISO_DATE_ANYWHERE_PATTERN.matcher(trimmed);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }

    /**
     * F-200 + F-202 : parseur des 30 flags Famille FR (F-200) + 5 flags Famille BE (F-202)
     * depuis la clé {@code famille_extracted_data} du JSON IA. Retourne {@code null} si la
     * clé est absente / non-objet (cas dossiers Travail / Immigration où l'IA n'émet pas
     * ce nœud) ou si tous les flags sont à false (économie mémoire — la map de visibilité
     * ne lit que les flags à true via {@code addBooleanFlagIfTrue}).
     */
    static FamilleExtractedData extractFamilleData(JsonNode root) {
        JsonNode node = root.get("famille_extracted_data");
        if (node == null || !node.isObject()) return null;
        // === Flags FR (F-200) === — 30 flags fail-safe à false
        boolean dcm = booleanOrFalse(node, "divorce_consentement_mutuel_envisage");
        boolean dal = booleanOrFalse(node, "divorce_alteration_lien_envisage");
        boolean dfa = booleanOrFalse(node, "divorce_faute_envisage");
        boolean dac = booleanOrFalse(node, "divorce_accepte_envisage");
        boolean rev = booleanOrFalse(node, "revision_post_divorce_envisagee");
        boolean op = booleanOrFalse(node, "ordonnance_protection_envisagee");
        boolean rec = booleanOrFalse(node, "recompenses_envisagees");
        boolean rcu = booleanOrFalse(node, "regime_communaute_universelle_detecte");
        boolean pj = booleanOrFalse(node, "partage_judiciaire_envisage");
        boolean ado = booleanOrFalse(node, "adoption_envisagee");
        boolean rp = booleanOrFalse(node, "reconnaissance_paternelle_envisagee");
        boolean cp = booleanOrFalse(node, "contestation_paternite_envisagee");
        boolean rche = booleanOrFalse(node, "recherche_paternite_envisagee");
        boolean pe = booleanOrFalse(node, "possession_etat_envisagee");
        boolean cr = booleanOrFalse(node, "changement_residence_envisage");
        boolean dp = booleanOrFalse(node, "desaccord_parental_detecte");
        boolean pd = booleanOrFalse(node, "pacs_dissolution_envisagee");
        boolean sc = booleanOrFalse(node, "separation_corps_envisagee");
        boolean ind = booleanOrFalse(node, "indivision_envisagee");
        boolean or = booleanOrFalse(node, "ordonnance_requete_envisagee");
        boolean su = booleanOrFalse(node, "succession_envisagee");
        boolean te = booleanOrFalse(node, "testament_envisage");
        boolean don = booleanOrFalse(node, "donation_envisagee");
        boolean rh = booleanOrFalse(node, "reserve_hereditaire_envisagee");
        boolean ps = booleanOrFalse(node, "partage_successoral_envisage");
        boolean iss = booleanOrFalse(node, "indivision_successorale_envisagee");
        boolean rs = booleanOrFalse(node, "rapport_succession_envisage");
        boolean pm = booleanOrFalse(node, "protection_majeur_envisagee");
        boolean cec = booleanOrFalse(node, "changement_etat_civil_envisage");
        boolean pmg = booleanOrFalse(node, "pma_gpa_envisagee");
        // F-210 SF-210-01 : flag médiation pré-saisine JAF
        boolean mfp = booleanOrFalse(node, "mediation_familiale_pre_saisine_pertinente");
        // === Flags BE (F-202) === — 5 flags fail-safe à false
        boolean divorceDc = booleanOrFalse(node, "divorce_dc_envisage");
        boolean divorceDdi = booleanOrFalse(node, "divorce_ddi_envisage");
        boolean cohabitationLegale = booleanOrFalse(node, "cohabitation_legale_be_detectee");
        boolean pacteSuccessoral = booleanOrFalse(node, "pacte_successoral_envisage");
        boolean kafalaRecueil = booleanOrFalse(node, "kafala_recueil_detecte");
        // F-239 + F-241 : extraction des champs string Famille pour pré-fill F-IA-04.
        // F-241 — priorité au nouveau nom neutre FR+BE `date_accord_initial_divorce`,
        // rétro-compat sur l'ancienne clé `date_acceptation_pv` (biais FR-PV),
        // et fallback déterministe sur la timeline IA si le LLM a omis le champ
        // (cas Vermeersch BE 2026-05-11 : LLM résistant malgré prompt renforcé).
        String dateAcceptationPV = stringOrNull(node, "date_accord_initial_divorce");
        if (dateAcceptationPV == null) {
            dateAcceptationPV = stringOrNull(node, "date_acceptation_pv");
        }
        if (dateAcceptationPV == null) {
            dateAcceptationPV = extractDateAccordInitialDivorceFromTimeline(root);
        }
        // SF-246-06 : sous-objet `succession_detection` — 16 champs IA successions/
        // libéralités pour pré-fill des 8 outils F-FA-24. Absent → tous null
        // (no-op gracieux des prefillFromAi() frontend). Dates via isoDateOrNull(),
        // montants via positiveDoubleOrNull() (jamais 0 — invariant §5.2),
        // dénombrements via boundedIntOrNull(_, _, 0, 50), énumérations via
        // stringOrNull() + whitelist (fail-open hors énumération).
        JsonNode sd = node.get("succession_detection");
        boolean sdObject = sd != null && sd.isObject();
        String dateDecesDetectee = sdObject ? isoDateOrNull(sd, "date_deces") : null;
        String dateOuvertureSuccessionDetectee = sdObject ? isoDateOrNull(sd, "date_ouverture_succession") : null;
        String modePartageDemandeDetecte = sdObject
                ? whitelistedOrNull(stringOrNull(sd, "mode_partage_demande"), "AMIABLE", "JUDICIAIRE")
                : null;
        Integer nombreCoheritiersDetecte = sdObject ? boundedIntOrNull(sd, "nombre_coheritiers", 0, 50) : null;
        Double montantSuccessionEurDetecte = sdObject ? positiveDoubleOrNull(sd, "montant_succession_eur") : null;
        Double montantLibsTotalEurDetecte = sdObject ? positiveDoubleOrNull(sd, "montant_liberalites_total_eur") : null;
        Integer nombreEnfantsSuccessionDetecte = sdObject ? boundedIntOrNull(sd, "nombre_enfants_succession", 0, 50) : null;
        String dateDonationDetectee = sdObject ? isoDateOrNull(sd, "date_donation") : null;
        Double montantDonationsRecuesEurDetecte = sdObject ? positiveDoubleOrNull(sd, "montant_donations_recues_eur") : null;
        Double valeurDonationAuJourPartageEurDetectee = sdObject ? positiveDoubleOrNull(sd, "valeur_donation_au_jour_partage_eur") : null;
        Double actifBrutSuccessionEurDetecte = sdObject ? positiveDoubleOrNull(sd, "actif_brut_succession_eur") : null;
        Double passifSuccessionEurDetecte = sdObject ? positiveDoubleOrNull(sd, "passif_succession_eur") : null;
        String typeIndivisionSuccessoraleDetecte = sdObject
                ? whitelistedOrNull(stringOrNull(sd, "type_indivision_successorale"), "LEGALE", "CONVENTIONNELLE")
                : null;
        Integer nbDescendantsDetecte = sdObject ? boundedIntOrNull(sd, "nb_descendants", 0, 50) : null;
        Integer nbFreresSoeursDetecte = sdObject ? boundedIntOrNull(sd, "nb_freres_soeurs", 0, 50) : null;
        String dateRedactionTestamentDetectee = sdObject ? isoDateOrNull(sd, "date_redaction_testament") : null;
        boolean successionDetectionPresent = dateDecesDetectee != null || dateOuvertureSuccessionDetectee != null
                || modePartageDemandeDetecte != null || nombreCoheritiersDetecte != null
                || montantSuccessionEurDetecte != null || montantLibsTotalEurDetecte != null
                || nombreEnfantsSuccessionDetecte != null || dateDonationDetectee != null
                || montantDonationsRecuesEurDetecte != null || valeurDonationAuJourPartageEurDetectee != null
                || actifBrutSuccessionEurDetecte != null || passifSuccessionEurDetecte != null
                || typeIndivisionSuccessoraleDetecte != null || nbDescendantsDetecte != null
                || nbFreresSoeursDetecte != null || dateRedactionTestamentDetectee != null;
        // SF-246-07 : sous-objet `regime_matrimonial_detection` — 4 champs IA régimes
        // matrimoniaux / liquidation pour pré-fill des 3 outils F-FA-15/16/17.
        // Absent → tous null (no-op gracieux des prefillFromAi() frontend).
        // Montants via positiveDoubleOrNull() (jamais 0 — invariant §5.2),
        // dénombrements via boundedIntOrNull(_, _, 0, 50),
        // régime via stringOrNull() + whitelist 4 valeurs.
        JsonNode rmd = node.get("regime_matrimonial_detection");
        boolean rmdObject = rmd != null && rmd.isObject();
        String regimeMatrimonialDetecte = rmdObject
                ? whitelistedOrNull(stringOrNull(rmd, "regime_matrimonial"),
                        "COMMUNAUTE_LEGALE", "COMMUNAUTE_UNIVERSELLE",
                        "SEPARATION_BIENS", "PARTICIPATION_ACQUETS")
                : null;
        Double valeurCommunauteEurDetectee = rmdObject ? positiveDoubleOrNull(rmd, "valeur_communaute_eur") : null;
        Double valeurBiensIndivisionEur = rmdObject ? positiveDoubleOrNull(rmd, "valeur_biens_indivision_eur") : null;
        Integer nombreCoindivisairesDetecte = rmdObject ? boundedIntOrNull(rmd, "nombre_coindivisaires", 0, 50) : null;
        boolean regimeMatrimonialDetectionPresent = regimeMatrimonialDetecte != null
                || valeurCommunauteEurDetectee != null
                || valeurBiensIndivisionEur != null
                || nombreCoindivisairesDetecte != null;
        // SF-246-08 : sous-objet `vie_commune_detection` — 7 champs IA vie commune &
        // protection pour pré-fill des 6 outils F-FA-12/13/14/20/21/22.
        // Absent → tous null (no-op gracieux des prefillFromAi() frontend).
        // Dates via isoDateOrNull() (§5.1 — YYYY-MM-DD strict),
        // montants via positiveDoubleOrNull() (jamais 0 — invariant §5.2),
        // entiers via boundedIntOrNull(_, _, 0, 30).
        // `patrimoineCommunEur` (montant €) ≠ `patrimoineCommun` boolean existant.
        // `dateRequeteOP` ≠ `dateOrdonnanceProtectionJaf` (SF-246-04 immigration).
        JsonNode vcd = node.get("vie_commune_detection");
        boolean vcdObject = vcd != null && vcd.isObject();
        String dateSeparation = vcdObject ? isoDateOrNull(vcd, "date_separation") : null;
        Double patrimoineCommunEur = vcdObject ? positiveDoubleOrNull(vcd, "patrimoine_commun_eur") : null;
        String dateConclusionPacs = vcdObject ? isoDateOrNull(vcd, "date_conclusion_pacs") : null;
        String dateRequeteOP = vcdObject ? isoDateOrNull(vcd, "date_requete_op") : null;
        String dateAudienceAOMP = vcdObject ? isoDateOrNull(vcd, "date_audience_aomp") : null;
        Integer nbEnfantsACharge = vcdObject ? boundedIntOrNull(vcd, "nb_enfants_a_charge", 0, 30) : null;
        Double revenusAnnuelsEpoux = vcdObject ? positiveDoubleOrNull(vcd, "revenus_annuels_epoux_eur") : null;
        boolean vieCommuneDetectionPresent = dateSeparation != null
                || patrimoineCommunEur != null
                || dateConclusionPacs != null
                || dateRequeteOP != null
                || dateAudienceAOMP != null
                || nbEnfantsACharge != null
                || revenusAnnuelsEpoux != null;
        // SF-246-09 : sous-objet `filiation_detection` — 7 champs IA filiation /
        // adoption pour pré-fill des 4 outils F-FA-18. Absent → tous null
        // (no-op gracieux des prefillFromAi() frontend). Dates via isoDateOrNull()
        // (§5.1 — YYYY-MM-DD strict), âges via boundedIntOrNull(_, _, 0, 120).
        // `dateNaissanceEnfantDetectee` (reconnaissance art. 316) ≠
        // `dateNaissanceEnfantRechercheDetectee` (recherche de paternité art. 327) :
        // deux contextes juridiques distincts — le prompt nomme chaque concept sans
        // ambiguïté (invariant cadrage §5.1.1).
        // `ageAdoptantDetecte` / `ageAdopteDetecte` : extraits directement en âge
        // (pas calculés à partir d'une date) — l'âge à la date de la requête est
        // l'unité pertinente du formulaire adoption (art. 343+ Cciv).
        JsonNode fd = node.get("filiation_detection");
        boolean fdObject = fd != null && fd.isObject();
        String dateEtablissementFiliationDetectee = fdObject ? isoDateOrNull(fd, "date_etablissement_filiation") : null;
        String dateConnaissanceVeriteDetectee = fdObject ? isoDateOrNull(fd, "date_connaissance_verite") : null;
        String dateMajoriteEnfantDetectee = fdObject ? isoDateOrNull(fd, "date_majorite_enfant") : null;
        String dateNaissanceEnfantRechercheDetectee = fdObject ? isoDateOrNull(fd, "date_naissance_enfant_recherche") : null;
        String dateNaissanceEnfantDetectee = fdObject ? isoDateOrNull(fd, "date_naissance_enfant") : null;
        Integer ageAdoptantDetecte = fdObject ? boundedIntOrNull(fd, "age_adoptant", 0, 120) : null;
        Integer ageAdopteDetecte = fdObject ? boundedIntOrNull(fd, "age_adopte", 0, 120) : null;
        boolean filiationDetectionPresent = dateEtablissementFiliationDetectee != null
                || dateConnaissanceVeriteDetectee != null
                || dateMajoriteEnfantDetectee != null
                || dateNaissanceEnfantRechercheDetectee != null
                || dateNaissanceEnfantDetectee != null
                || ageAdoptantDetecte != null
                || ageAdopteDetecte != null;
        // SF-246-10 : sous-objet `autorite_parentale_detection` — 3 champs IA
        // autorité parentale pour pré-fill des 4 outils F-FA-19. Absent → tous
        // null (no-op gracieux des prefillFromAi() frontend). Ages via boucle
        // boundedIntOrNull(_, _, 0, 25) — chaque élément invalide exclu ; liste
        // vide → null (jamais [] — invariant cadrage §5.1.2 transposé aux listes).
        // Dates via isoDateOrNull() (§5.1 — YYYY-MM-DD strict).
        JsonNode apd = node.get("autorite_parentale_detection");
        boolean apdObject = apd != null && apd.isObject();
        java.util.List<Integer> agesEnfantsDetectes = null;
        if (apdObject) {
            com.fasterxml.jackson.databind.JsonNode agesNode = apd.get("ages_enfants");
            if (agesNode != null && agesNode.isArray() && !agesNode.isEmpty()) {
                java.util.List<Integer> raw = new java.util.ArrayList<>();
                for (com.fasterxml.jackson.databind.JsonNode item : agesNode) {
                    if (item.isInt()) {
                        int v = item.asInt();
                        if (v >= 0 && v <= 25) raw.add(v);
                    }
                }
                agesEnfantsDetectes = raw.isEmpty() ? null : java.util.Collections.unmodifiableList(raw);
            }
        }
        String dateDebutCalendrierDetectee = apdObject ? isoDateOrNull(apd, "date_debut_calendrier") : null;
        String dateFinCalendrierDetectee = apdObject ? isoDateOrNull(apd, "date_fin_calendrier") : null;
        boolean autoriteParentaleDetectionPresent = agesEnfantsDetectes != null
                || dateDebutCalendrierDetectee != null
                || dateFinCalendrierDetectee != null;
        // SF-246-03 : sous-objet `divorce_faute_detection` — liste de codes de faute
        // pour pré-fill de l'outil F-FA-09 divorce pour faute. Absent → null (no-op).
        // Codes whitelistés (énumération fermée, alignés sur FauteCode frontend F-FA-09) ;
        // codes hors whitelist exclus. Liste vide après filtrage → null (jamais []).
        // Famille FR uniquement — le prompt impose null pour les dossiers BE.
        java.util.Set<String> FAUTE_WHITELIST = java.util.Set.of(
                "ADULTERE", "VIOLENCES", "ABANDON",
                "OUTRAGES", "DEVOIR_ASSISTANCE",
                "DEVOIR_FIDELITE", "DEVOIR_COMMUNAUTE_VIE", "AUTRE");
        JsonNode dfd = node.get("divorce_faute_detection");
        java.util.List<String> fautesDetectees = null;
        if (dfd != null && dfd.isObject()) {
            JsonNode fautesNode = dfd.get("fautes_detectees");
            if (fautesNode != null && fautesNode.isArray() && !fautesNode.isEmpty()) {
                java.util.List<String> raw = new java.util.ArrayList<>();
                for (JsonNode item : fautesNode) {
                    if (item.isTextual()) {
                        String code = item.asText().trim().toUpperCase();
                        if (FAUTE_WHITELIST.contains(code) && !raw.contains(code)) {
                            raw.add(code);
                        }
                    }
                }
                fautesDetectees = raw.isEmpty() ? null : java.util.Collections.unmodifiableList(raw);
            }
        }
        boolean divorceFauteDetectionPresent = fautesDetectees != null;

        if (!dcm && !dal && !dfa && !dac && !rev && !op && !rec && !rcu && !pj
                && !ado && !rp && !cp && !rche && !pe && !cr && !dp
                && !pd && !sc && !ind && !or
                && !su && !te && !don && !rh && !ps && !iss && !rs
                && !pm && !cec && !pmg && !mfp
                && !divorceDc && !divorceDdi && !cohabitationLegale && !pacteSuccessoral && !kafalaRecueil
                && dateAcceptationPV == null
                && !successionDetectionPresent
                && !regimeMatrimonialDetectionPresent
                && !vieCommuneDetectionPresent
                && !filiationDetectionPresent
                && !autoriteParentaleDetectionPresent
                && !divorceFauteDetectionPresent) {
            return null;
        }
        // F-234 SF-234-01 : construction via Builder.
        return FamilleExtractedData.builder()
                // FR (30) — F-200
                .divorceConsentementMutuelEnvisage(dcm)
                .divorceAlterationLienEnvisage(dal)
                .divorceFauteEnvisage(dfa)
                .divorceAccepteEnvisage(dac)
                .revisionPostDivorceEnvisagee(rev)
                .ordonnanceProtectionEnvisagee(op)
                .recompensesEnvisagees(rec)
                .regimeCommunauteUniverselleDetecte(rcu)
                .partageJudiciaireEnvisage(pj)
                .adoptionEnvisagee(ado)
                .reconnaissancePaternelleEnvisagee(rp)
                .contestationPaterniteEnvisagee(cp)
                .recherchePaterniteEnvisagee(rche)
                .possessionEtatEnvisagee(pe)
                .changementResidenceEnvisage(cr)
                .desaccordParentalDetecte(dp)
                .pacsDissolutionEnvisagee(pd)
                .separationCorpsEnvisagee(sc)
                .indivisionEnvisagee(ind)
                .ordonnanceRequeteEnvisagee(or)
                .successionEnvisagee(su)
                .testamentEnvisage(te)
                .donationEnvisagee(don)
                .reserveHereditaireEnvisagee(rh)
                .partageSuccessoralEnvisage(ps)
                .indivisionSuccessoraleEnvisagee(iss)
                .rapportSuccessionEnvisage(rs)
                .protectionMajeurEnvisagee(pm)
                .changementEtatCivilEnvisage(cec)
                .pmaGpaEnvisagee(pmg)
                // F-210 (1)
                .mediationFamilialePreSaisinePertinente(mfp)
                // BE (5) — F-202
                .divorceDcEnvisage(divorceDc)
                .divorceDdiEnvisage(divorceDdi)
                .cohabitationLegaleBeDetectee(cohabitationLegale)
                .pacteSuccessoralEnvisage(pacteSuccessoral)
                .kafalaRecueilDetecte(kafalaRecueil)
                // F-239 : string fields
                .dateAcceptationPV(dateAcceptationPV)
                // SF-246-06 : 16 champs IA successions/libéralités (F-FA-24).
                .dateDecesDetectee(dateDecesDetectee)
                .dateOuvertureSuccessionDetectee(dateOuvertureSuccessionDetectee)
                .modePartageDemandeDetecte(modePartageDemandeDetecte)
                .nombreCoheritiersDetecte(nombreCoheritiersDetecte)
                .montantSuccessionEurDetecte(montantSuccessionEurDetecte)
                .montantLibsTotalEurDetecte(montantLibsTotalEurDetecte)
                .nombreEnfantsSuccessionDetecte(nombreEnfantsSuccessionDetecte)
                .dateDonationDetectee(dateDonationDetectee)
                .montantDonationsRecuesEurDetecte(montantDonationsRecuesEurDetecte)
                .valeurDonationAuJourPartageEurDetectee(valeurDonationAuJourPartageEurDetectee)
                .actifBrutSuccessionEurDetecte(actifBrutSuccessionEurDetecte)
                .passifSuccessionEurDetecte(passifSuccessionEurDetecte)
                .typeIndivisionSuccessoraleDetecte(typeIndivisionSuccessoraleDetecte)
                .nbDescendantsDetecte(nbDescendantsDetecte)
                .nbFreresSoeursDetecte(nbFreresSoeursDetecte)
                .dateRedactionTestamentDetectee(dateRedactionTestamentDetectee)
                // SF-246-07 : 4 champs IA régimes matrimoniaux / liquidation (F-FA-15/16/17).
                .valeurCommunauteEurDetectee(valeurCommunauteEurDetectee)
                .regimeMatrimonialDetecte(regimeMatrimonialDetecte)
                .valeurBiensIndivisionEur(valeurBiensIndivisionEur)
                .nombreCoindivisairesDetecte(nombreCoindivisairesDetecte)
                // SF-246-08 : 7 champs IA vie commune & protection (F-FA-12/13/14/20/21/22).
                .dateSeparation(dateSeparation)
                .patrimoineCommunEur(patrimoineCommunEur)
                .dateConclusionPacs(dateConclusionPacs)
                .dateRequeteOP(dateRequeteOP)
                .dateAudienceAOMP(dateAudienceAOMP)
                .nbEnfantsACharge(nbEnfantsACharge)
                .revenusAnnuelsEpoux(revenusAnnuelsEpoux)
                // SF-246-09 : 7 champs IA filiation / adoption (F-FA-18).
                .dateEtablissementFiliationDetectee(dateEtablissementFiliationDetectee)
                .dateConnaissanceVeriteDetectee(dateConnaissanceVeriteDetectee)
                .dateMajoriteEnfantDetectee(dateMajoriteEnfantDetectee)
                .dateNaissanceEnfantRechercheDetectee(dateNaissanceEnfantRechercheDetectee)
                .dateNaissanceEnfantDetectee(dateNaissanceEnfantDetectee)
                .ageAdoptantDetecte(ageAdoptantDetecte)
                .ageAdopteDetecte(ageAdopteDetecte)
                // SF-246-10 : 3 champs IA autorité parentale (F-FA-19).
                .agesEnfantsDetectes(agesEnfantsDetectes)
                .dateDebutCalendrierDetectee(dateDebutCalendrierDetectee)
                .dateFinCalendrierDetectee(dateFinCalendrierDetectee)
                // SF-246-03 : codes de faute détectés pour pré-fill F-FA-09.
                .fautesDetectees(fautesDetectees)
                .build();
    }

    /**
     * SF-246-06 : retourne {@code value} (normalisé en MAJUSCULES, trimmé) s'il
     * appartient à la whitelist {@code allowed}, sinon {@code null} (fail-open).
     * Utilisé pour les énumérations IA {@code mode_partage_demande}
     * ({@code AMIABLE}/{@code JUDICIAIRE}) et {@code type_indivision_successorale}
     * ({@code LEGALE}/{@code CONVENTIONNELLE}) du sous-objet {@code succession_detection}.
     */
    private static String whitelistedOrNull(String value, String... allowed) {
        if (value == null) return null;
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT);
        if (normalized.isEmpty()) return null;
        for (String candidate : allowed) {
            if (candidate.equals(normalized)) return normalized;
        }
        return null;
    }
}
