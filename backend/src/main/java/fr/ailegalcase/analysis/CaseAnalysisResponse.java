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
        DivorceConsentementScoring divorceConsentementScoring
) {

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
            boolean motifGraveBeEnvisage) {

        /** Constructeur rétrocompat 9 champs (avant SF-DT-04-04). */
        public TravailExtractedData(String conventionCollective, String dateEntree, Double salaireBrutMensuel,
                                     String typeContrat, String poste, String motifLicenciement, String dateLicenciement,
                                     Integer congesContractuels, Double primeAncienneteContractuelle) {
            this(conventionCollective, dateEntree, salaireBrutMensuel,
                    typeContrat, poste, motifLicenciement, dateLicenciement,
                    congesContractuels, primeAncienneteContractuelle,
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null,
                    false, false, false, false, false, false, false, false, false, false, false, false, false);
        }

        /** Constructeur rétrocompat 17 champs (avant SF-130-01). */
        public TravailExtractedData(String conventionCollective, String dateEntree, Double salaireBrutMensuel,
                                     String typeContrat, String poste, String motifLicenciement, String dateLicenciement,
                                     Integer congesContractuels, Double primeAncienneteContractuelle,
                                     String nomSalarie, String prenomSalarie, String adresseSalarie,
                                     String nomEmployeur, String adresseEmployeur,
                                     String siretEmployeur, String bceEmployeur,
                                     String representantEmployeur) {
            this(conventionCollective, dateEntree, salaireBrutMensuel,
                    typeContrat, poste, motifLicenciement, dateLicenciement,
                    congesContractuels, primeAncienneteContractuelle,
                    nomSalarie, prenomSalarie, adresseSalarie,
                    nomEmployeur, adresseEmployeur,
                    siretEmployeur, bceEmployeur,
                    representantEmployeur, null,
                    null, null, null, null, null,
                    false, false, false, false, false, false, false, false, false, false, false, false, false);
        }

        /** Constructeur rétrocompat 18 champs (avant SF-155-04-00-BE-travail). */
        public TravailExtractedData(String conventionCollective, String dateEntree, Double salaireBrutMensuel,
                                     String typeContrat, String poste, String motifLicenciement, String dateLicenciement,
                                     Integer congesContractuels, Double primeAncienneteContractuelle,
                                     String nomSalarie, String prenomSalarie, String adresseSalarie,
                                     String nomEmployeur, String adresseEmployeur,
                                     String siretEmployeur, String bceEmployeur,
                                     String representantEmployeur, Boolean salaireEstDeduit) {
            this(conventionCollective, dateEntree, salaireBrutMensuel,
                    typeContrat, poste, motifLicenciement, dateLicenciement,
                    congesContractuels, primeAncienneteContractuelle,
                    nomSalarie, prenomSalarie, adresseSalarie,
                    nomEmployeur, adresseEmployeur,
                    siretEmployeur, bceEmployeur,
                    representantEmployeur, salaireEstDeduit,
                    null, null, null, null, null,
                    false, false, false, false, false, false, false, false, false, false, false, false, false);
        }

        /** Constructeur rétrocompat 23 champs (avant SF-166-01). */
        public TravailExtractedData(String conventionCollective, String dateEntree, Double salaireBrutMensuel,
                                     String typeContrat, String poste, String motifLicenciement, String dateLicenciement,
                                     Integer congesContractuels, Double primeAncienneteContractuelle,
                                     String nomSalarie, String prenomSalarie, String adresseSalarie,
                                     String nomEmployeur, String adresseEmployeur,
                                     String siretEmployeur, String bceEmployeur,
                                     String representantEmployeur, Boolean salaireEstDeduit,
                                     String motifNullitePressenti, String origineInaptitudePressentie,
                                     String avisMedecinTravailDate,
                                     DetectedAnswer reclassementRespecteDetected,
                                     HeuresSupMentionnees heuresSupMentionneesDansDossier) {
            this(conventionCollective, dateEntree, salaireBrutMensuel,
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
                    false, false, false, false, false, false, false, false, false, false, false, false, false);
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
            boolean oqtAnnexe13Detectee) {
        public ImmigrationExtractedData(String dateExpirationTitre, String typeTitreSejour,
                                         String typeProcedureDetectee, String dateDepotProcedure) {
            this(dateExpirationTitre, typeTitreSejour, typeProcedureDetectee, dateDepotProcedure,
                    null, null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null,
                    false, false, false, false, false, false, false, false, false,
                    false, false, false, false, false);
        }
        public ImmigrationExtractedData(String dateExpirationTitre, String typeTitreSejour,
                                         String typeProcedureDetectee, String dateDepotProcedure,
                                         String typeTitreSejourCode, Boolean nationaliteUe) {
            this(dateExpirationTitre, typeTitreSejour, typeProcedureDetectee, dateDepotProcedure,
                    typeTitreSejourCode, nationaliteUe, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null,
                    false, false, false, false, false, false, false, false, false,
                    false, false, false, false, false);
        }
        /** Rétrocompat 8-args pré-SF-IM-01-04. */
        public ImmigrationExtractedData(String dateExpirationTitre, String typeTitreSejour,
                                         String typeProcedureDetectee, String dateDepotProcedure,
                                         String typeTitreSejourCode, Boolean nationaliteUe,
                                         String typeRecoursCode, String dateNotificationDecisionContestee) {
            this(dateExpirationTitre, typeTitreSejour, typeProcedureDetectee, dateDepotProcedure,
                    typeTitreSejourCode, nationaliteUe, typeRecoursCode, dateNotificationDecisionContestee, null,
                    null, null, null, null, null,
                    null, null, null, null,
                    false, false, false, false, false, false, false, false, false,
                    false, false, false, false, false);
        }
        /** Rétrocompat 9-args pré-SF-155-04-00-BE-immig-FR/BE (signature SF-IM-01-04). */
        public ImmigrationExtractedData(String dateExpirationTitre, String typeTitreSejour,
                                         String typeProcedureDetectee, String dateDepotProcedure,
                                         String typeTitreSejourCode, Boolean nationaliteUe,
                                         String typeRecoursCode, String dateNotificationDecisionContestee,
                                         String inferredChecklistType) {
            this(dateExpirationTitre, typeTitreSejour, typeProcedureDetectee, dateDepotProcedure,
                    typeTitreSejourCode, nationaliteUe, typeRecoursCode, dateNotificationDecisionContestee,
                    inferredChecklistType,
                    null, null, null, null, null,
                    null, null, null, null,
                    false, false, false, false, false, false, false, false, false,
                    false, false, false, false, false);
        }
        /** Rétrocompat 14-args pré-SF-155-04-00-BE-immig-BE (signature post-SF-155-04-00-BE-immig-FR). */
        public ImmigrationExtractedData(String dateExpirationTitre, String typeTitreSejour,
                                         String typeProcedureDetectee, String dateDepotProcedure,
                                         String typeTitreSejourCode, Boolean nationaliteUe,
                                         String typeRecoursCode, String dateNotificationDecisionContestee,
                                         String inferredChecklistType,
                                         String dateNotificationOqtf, String motifOqtfCode,
                                         DetectedAnswer recoursFormeDetected,
                                         String dateHeureNotificationOqtfSansDelai,
                                         Boolean placementCraDetected) {
            this(dateExpirationTitre, typeTitreSejour, typeProcedureDetectee, dateDepotProcedure,
                    typeTitreSejourCode, nationaliteUe, typeRecoursCode, dateNotificationDecisionContestee,
                    inferredChecklistType,
                    dateNotificationOqtf, motifOqtfCode, recoursFormeDetected,
                    dateHeureNotificationOqtfSansDelai, placementCraDetected,
                    null, null, null, null,
                    false, false, false, false, false, false, false, false, false,
                    false, false, false, false, false);
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
                        immigrationExtractedData = new ImmigrationExtractedData(
                                immigrationExtractedData.dateExpirationTitre(),
                                immigrationExtractedData.typeTitreSejour(),
                                immigrationExtractedData.typeProcedureDetectee(),
                                immigrationExtractedData.dateDepotProcedure(),
                                immigrationExtractedData.typeTitreSejourCode(),
                                immigrationExtractedData.nationaliteUe(),
                                immigrationExtractedData.typeRecoursCode(),
                                immigrationExtractedData.dateNotificationDecisionContestee(),
                                inferred,
                                // SF-155-04-00-BE-immig-FR : préserver les 5 champs FR lors de la reconstruction
                                immigrationExtractedData.dateNotificationOqtf(),
                                immigrationExtractedData.motifOqtfCode(),
                                immigrationExtractedData.recoursFormeDetected(),
                                immigrationExtractedData.dateHeureNotificationOqtfSansDelai(),
                                immigrationExtractedData.placementCraDetected(),
                                // SF-155-04-00-BE-immig-BE : préserver les 4 champs BE lors de la reconstruction
                                immigrationExtractedData.dateNotificationAnnexe13(),
                                immigrationExtractedData.delaiDepartImposeJours(),
                                immigrationExtractedData.motifOqtCodeBe(),
                                immigrationExtractedData.transfertImminentDetected(),
                                // F-201 : préserver les 9 flags Immigration FR
                                immigrationExtractedData.aesMetiersTensionEligibleDetecte(),
                                immigrationExtractedData.aesFamilialEligibleDetecte(),
                                immigrationExtractedData.aesHumanitaireEligibleDetecte(),
                                immigrationExtractedData.aesEtudiantEligibleDetecte(),
                                immigrationExtractedData.changementStatutEnvisageDetecte(),
                                immigrationExtractedData.procedureAsileDetectee(),
                                immigrationExtractedData.naturalisationEnvisageeDetectee(),
                                immigrationExtractedData.clientMineurDetecte(),
                                immigrationExtractedData.mesureEloignementDetectee(),
                                // F-203 : préserver les 5 flags Immigration BE
                                immigrationExtractedData.procedure9bisEnvisagee(),
                                immigrationExtractedData.procedure9terMedicaleDetectee(),
                                immigrationExtractedData.regroupement40bisDetecte(),
                                immigrationExtractedData.regroupement40terDetecte(),
                                immigrationExtractedData.oqtAnnexe13Detectee()
                        );
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
                divorceConsentementScoring
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
                    base.divorceConsentementValidityDetection(), base.divorceConsentementScoring());
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
            return new TravailExtractedData(
                    // SF-129-01 : normaliser le code convention pour matcher le référentiel
                    fr.ailegalcase.casefile.ConventionCodeNormalizer.normalize(textOrNull(node, "convention_collective")),
                    textOrNull(node, "date_entree"),
                    doubleOrNull(node, "salaire_brut_mensuel"),
                    textOrNull(node, "type_contrat"),
                    textOrNull(node, "poste"),
                    textOrNull(node, "motif_licenciement"),
                    textOrNull(node, "date_licenciement"),
                    intOrNull(node, "conges_contractuels"),
                    doubleOrNull(node, "prime_anciennete_contractuelle"),
                    textOrNull(node, "nom_salarie"),
                    textOrNull(node, "prenom_salarie"),
                    textOrNull(node, "adresse_salarie"),
                    textOrNull(node, "nom_employeur"),
                    textOrNull(node, "adresse_employeur"),
                    normalizeFrIdentifier(textOrNull(node, "siret_employeur")),
                    normalizeBeBceIdentifier(textOrNull(node, "bce_employeur")),
                    textOrNull(node, "representant_employeur"),
                    // SF-130-01 : flag IA "salaire déduit d'un net"
                    booleanOrNull(node, "salaire_est_deduit"),
                    // SF-155-04-00-BE-travail : 5 champs IA pour F-DT-11 / F-DT-15 / F-DT-19
                    normalizeEnumCode(textOrNull(node, "motif_nullite_pressenti"), MOTIFS_NULLITE_CODES),
                    normalizeEnumCode(textOrNull(node, "origine_inaptitude_pressentie"), ORIGINE_INAPTITUDE_CODES),
                    textOrNull(node, "avis_medecin_travail_date"),
                    extractDetectedAnswer(node.get("reclassement_respecte_detected")),
                    extractHeuresSupMentionnees(node.get("heures_sup_mentionnees")),
                    // SF-166-01 : 8 flags décisionnels niveau 3 — fail-safe à false
                    booleanOrFalse(node, "rappel_salaire_detecte"),
                    booleanOrFalse(node, "travail_dissimule_detecte"),
                    booleanOrFalse(node, "clause_non_concurrence_detectee"),
                    booleanOrFalse(node, "statut_protege_detecte"),
                    booleanOrFalse(node, "transaction_envisagee"),
                    booleanOrFalse(node, "at_mp_detecte"),
                    booleanOrFalse(node, "urgence_procedurale"),
                    booleanOrFalse(node, "contestation_are_envisagee"),
                    // F-204 : 5 flags décisionnels niveau 3 — Travail BE uniquement, fail-safe à false
                    booleanOrFalse(node, "harcelement_be_detecte"),
                    booleanOrFalse(node, "discrimination_be_detectee"),
                    booleanOrFalse(node, "inaptitude_medicale_be_detectee"),
                    booleanOrFalse(node, "heures_sup_mentionnees_be"),
                    booleanOrFalse(node, "motif_grave_be_envisage")
            );
        } catch (Exception ignored) { return null; }
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
                && !regroupement40ter && !oqtAnnexe13) return null;
        return new ImmigrationExtractedData(dateExpiration, typeTitre, typeProcedure, dateDepot,
                typeCode, nationaliteUe, recoursCode, dateNotif, null,
                // SF-155-04-00-BE-immig-FR : 5 champs pour F-IM-08-02 / F-IM-08-04
                dateNotifOqtf, motifOqtfCode, recoursFormeDetected,
                dateHeureNotifOqtfSansDelai, placementCraDetected,
                // SF-155-04-00-BE-immig-BE : 4 champs pour F-IM-08 Annexe 13 BE
                dateAnnexe13, delaiDepart, motifOqtBe, transfertImminent,
                // F-201 : 9 flags Immigration FR
                aesMetiersTension, aesFamilial, aesHumanitaire, aesEtudiant,
                changementStatut, procedureAsile, naturalisationEnvisagee,
                clientMineur, mesureEloignement,
                // F-203 : 5 flags Immigration BE
                procedure9bis, procedure9ter, regroupement40bis, regroupement40ter, oqtAnnexe13);
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

    private static Integer intOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).intValue() : null;
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
}
