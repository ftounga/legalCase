package fr.ailegalcase.analysis;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
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
            boolean teletravailLitigeDetecte,
            // SF-212-19 : nouveau flag F-205 — déclenche F-DT-48 mise à pied disciplinaire (FR).
            boolean miseAPiedDisciplinaireDetectee,
            // SF-212-23 : nouveau flag F-205 — déclenche F-DT-56 égalité salariale femmes/hommes (FR).
            boolean egaliteSalarialePressentie,
            // SF-212-17 : nouveau flag F-205 — déclenche F-DT-43 rupture anticipée CDD (FR).
            boolean ruptureAnticipeeCddDetectee,
            // SF-212-21 / F-256 : flag F-205 `demission_equivoque_pressentie` (FR — déclenche
            // F-DT-41 démission validité équivoque). Projeté sur le record depuis F-256
            // (slot libéré par refactor sous-records). DecisionToolVisibilityService continue
            // de le lire depuis le JsonNode brut — projection redondante OK.
            boolean demissionEquivoquePressentie,
            // SF-212-35 / F-256 : flag F-205 `pdv_rcc_envisage` (FR — déclenche F-DT-46
            // PDV / RCC conformité). Projeté sur le record depuis F-256 (slot libéré par
            // refactor sous-records).
            boolean pdvRccEnvisage,
            // SF-212-29 : nouveau flag F-205 `conge_maternite_paternite_detecte` (FR —
            // déclenche F-DT-77 congé maternité / paternité). True si le pipeline IA
            // détecte une situation de congé maternité ou paternité à analyser
            // (certificat médical de grossesse, déclaration de paternité, congé
            // notifié, retour de congé, indices L. 1225-1 à L. 1225-40 CT).
            boolean congeMaternitePaterniteDetecte,
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
            // SF-207-03 : 3 champs IA Travail BE pour pré-fill F-207-03
            // contestation C4 ONEM (double délai recours admin Directeur +
            // recours tribunal du travail). Tous nullables — Travail BE
            // uniquement, restent null pour un dossier FR (le régime FR
            // contestation France Travail F-DT-35 est juridiquement distinct).
            // dateNotificationDecisionOnem : date de notification de la
            // décision ONEM contestée (format ISO YYYY-MM-DD). Point de
            // départ du délai de 1 mois (AR 25/11/1991 art. 144).
            // dateDecisionDirecteur : date de notification de la décision
            // du Directeur sur le recours admin déjà formé (cas B —
            // format ISO YYYY-MM-DD). Point de départ du délai de 3 mois
            // (CJ art. 580, 2°). Reste null si recours admin non encore formé.
            // recoursAdminDejaForme : booléen — true si le recours admin
            // Directeur a déjà été formé (oriente vers cas B). Null si
            // non détectable.
            String dateNotificationDecisionOnem,
            String dateDecisionDirecteur,
            Boolean recoursAdminDejaForme,
            // SF-207-04 : 2 champs IA Travail BE pour pré-fill F-207-04 outil
            // déclaration AT Fedris (Loi 10/04/1971 art. 62 — délai 8 jours
            // de l'employeur à compter de la connaissance de l'accident).
            // Tous nullables — Travail BE uniquement, restent null pour un
            // dossier FR (le régime FR AT employeur 48h L.441-1 CSS est
            // juridiquement distinct — géré par AtMpCalculator / F-DT-33).
            // dateAccident : date de survenance de l'accident du travail
            // (format ISO YYYY-MM-DD). Point de départ par défaut si la
            // date de connaissance employeur n'est pas documentée.
            // dateConnaissanceAccidentEmployeur : date à laquelle l'employeur
            // a eu connaissance de l'accident (ISO YYYY-MM-DD) — point de
            // départ effectif du délai de 8 jours (art. 62 al. 1). Reste
            // null si non distincte de la date d'accident ou non détectable.
            String dateAccident,
            String dateConnaissanceAccidentEmployeur,
            // SF-207-05 : 3 champs IA Travail BE pour pré-fill F-207-05 outil
            // référé tribunal du travail BE (CJ art. 584). Tous nullables —
            // Travail BE uniquement, restent null pour un dossier FR (le
            // référé prud'homal FR R.1454-1 CT est un régime juridiquement
            // distinct géré par ReferePrudhomalCalculator / F-DT-34).
            // Note : `urgenceProcedurale` (boolean, F-166 SF-166-01) est
            // FRANCE-only et ne couvre PAS le référé BE — ces 3 champs sont
            // disjoints et autonomes côté BE.
            // motifUrgenceDetecte : code motif d'urgence détecté
            // (HARCELEMENT / SALAIRE_IMPAYE / MODIFICATION_UNILATERALE /
            // AUTRE), null hors plage ou si non détectable.
            // dateFaitGenerateurUrgence : date du fait générateur de
            // l'urgence (ISO YYYY-MM-DD), null si non détectable.
            // perilImmediatPresume : booléen — true uniquement si les pièces
            // évoquent un péril en demeure caractérisé (préjudice imminent
            // ou irréversible). Null si non factualisable.
            String motifUrgenceDetecte,
            String dateFaitGenerateurUrgence,
            Boolean perilImmediatPresume,
            // SF-246-22 : type de procédure travail et date déclencheur pour pré-fill
            // F-136 travail-procedure (FR+BE). 6 codes whitelistés (3 FR + 3 BE) :
            // PRUDHOMMES_FR, APPEL_CA_SOCIALE_FR, CASSATION_SOCIALE_FR,
            // TRIBUNAL_TRAVAIL_BE, COUR_TRAVAIL_BE, CASSATION_BE.
            // Codes hors whitelist exclus par extractTravailData().
            // dateDeclencheurProcedure : format ISO YYYY-MM-DD ou null.
            String procedureTravailDetectee,
            String dateDeclencheurProcedure,
            // SF-246-21 — sous-objet `requalification_detection` :
            // CDD : durée [0,120] mois, dates fin/début/fin du contrat suivant (ISO).
            // Intérim : durée totale [0,120] mois, dates fin/début/fin mission suivante (ISO),
            // entreprise utilisatrice (≤ 200 car.), total rémunérations brutes (€ > 0),
            // durée mission en jours [0,3650]. Tous FR uniquement, null pour dossier BE.
            Integer cddDureeMois,
            String cddDateFinDernierContrat,
            String cddNouveauDateDebut,
            String cddNouveauDateFin,
            Double cddTotalSalairesBruts,
            Integer interimDureeTotaleMois,
            String interimDateFinDerniereMission,
            String interimNouvellesMissionDateDebut,
            String interimNouvellesMissionDateFin,
            String interimEntrepriseUtilisatrice,
            Double interimTotalRemunerationsBrutes,
            Integer interimDureeMissionJours,
            // SF-246-21 — sous-objet `paie_detection` :
            // CP : jours acquis [0,50] et jours pris [0,50] (bulletins/solde TC).
            // Rappel salaire : montant versé (€ > 0), période début/fin (ISO).
            // Tous FR uniquement, null pour dossier BE.
            Integer congesJoursAcquis,
            Integer congesJoursPris,
            Double rappelSalaireMontantPerverseMensuel,
            String rappelSalairePeriodeDebut,
            String rappelSalairePeriodeFin,
            // SF-246-21 — sous-objet `rupture_collective_detection` :
            // Lic éco : âge salarié [16,80] ans (si pièce d'identité aux pièces).
            // PSE : nb salariés [0,100000], nb licenciements [0,100000].
            // Transaction : date de signature (ISO), indemnité (€ > 0).
            // Tous FR uniquement, null pour dossier BE.
            Integer salarieAgeAnnees,
            Integer pseNombreSalaries,
            Integer pseNombreLicenciements,
            String transactionDateSignature,
            Double transactionIndemniteMontantEur,
            // SF-246-21 — sous-objet `sante_discrimination_detection` :
            // AT/MP : date accident AT (ISO, distincte de dateLicenciement),
            //   date exposition MP (ISO).
            // ARE : type décision contestée (whitelist), montant contesté (€ > 0).
            // Discrimination : motif (whitelist), contexte (whitelist).
            // Tous FR uniquement, null pour dossier BE.
            String atDateAccident,
            String atDateExposition,
            String areTypeDecision,
            Double areMontantConteste,
            String discriminationMotif,
            String discriminationContexte,
            // SF-246-21 — sous-objet `procedure_details_detection` :
            // Référé prud'homal : montant de la provision demandée (€ > 0).
            // Documents fin de contrat : dates certificat travail / attestation
            //   France Travail / solde de tout compte (toutes ISO YYYY-MM-DD).
            // Tous FR uniquement, null pour dossier BE.
            Double refereMontantProvision,
            String documentsDateCertificatTravail,
            String documentsDateAttestationFranceTravail,
            String documentsDateSoldeToutCompte,
            // SF-246-23 — sous-objet `travail_be_detection` : 6 champs BELGIQUE
            // UNIQUEMENT pour pré-fill des 3 outils F-DT-27/F-DT-28/F-DT-29.
            // Tous nullables — restent null pour un dossier Travail FRANCE.
            // motif-grave-be (F-DT-27) :
            //   dateConnaissanceFait : date à laquelle l'employeur a eu connaissance
            //     du fait constituant le motif grave (ISO YYYY-MM-DD). Point de départ
            //     du délai de 3 j ouvrables art. 35 Loi 03/07/1978. Distincte de
            //     dateLicenciement (date notification rupture).
            //   dateNotificationMotifs : date à laquelle l'employeur a notifié les
            //     motifs au travailleur par lettre recommandée (ISO YYYY-MM-DD). Point
            //     d'arrivée du 2e délai de 3 j ouvrables. Strictement postérieure à
            //     dateConnaissanceFait.
            // avantages-conventionnels-be (F-DT-28) :
            //   commissionParitaireBe : numéro/libellé de la commission paritaire BE
            //     (ex. "CP 200", "SCP 200.01") — concept distinct de conventionCollective
            //     (domaine FR). Borné ≤ 20 car.
            //   joursTravaillesAnneePrecedenteBe : jours de travail effectif (ou
            //     assimilés) au cours de l'année précédente [0, 365].
            //   joursPrestesBe : jours effectivement prestés depuis le 1er avril de
            //     l'exercice courant [0, 365].
            // credit-temps-be (F-DT-29) :
            //   dateDemandeCreditTemps : date à laquelle le travailleur a formellement
            //     introduit sa demande de crédit-temps (ISO YYYY-MM-DD). NE PAS
            //     confondre avec la date d'entrée en vigueur du crédit-temps.
            String dateConnaissanceFait,       // BELGIQUE — motif-grave-be
            String dateNotificationMotifs,     // BELGIQUE — motif-grave-be
            String commissionParitaireBe,      // BELGIQUE — avantages-conventionnels-be
            Integer joursTravaillesAnneePrecedenteBe, // BELGIQUE — avantages-conventionnels-be
            Integer joursPrestesBe,            // BELGIQUE — avantages-conventionnels-be
            String dateDemandeCreditTemps,     // BELGIQUE — credit-temps-be
            // SF-206-01 : 8 champs IA procéduraux pour pré-fill F-DT-42 (abandon
            // de poste / présomption de démission, Travail FR uniquement,
            // nullables). Sous-objet `abandon_poste_detail`. La présomption de
            // démission par abandon de poste (loi 21/12/2022, art. L.1237-1-1 CT
            // et D.1237-2-1 s. CT) est un mécanisme franco-français : ces champs
            // restent null pour la BE.
            String abandonPosteDateMiseEnDemeure,
            String abandonPosteModeNotification,
            Integer abandonPosteDelaiAccordeJours,
            String abandonPosteMotifAbsence,
            String abandonPosteDateReprise,
            Boolean abandonPosteMedMentionneDelai,
            Boolean abandonPosteMedMentionneConsequences,
            Boolean abandonPosteRepriseDansDelai,
            // SF-206-05 : 11 champs IA pour pré-fill F-DT-39 (prise d'acte de la
            // rupture aux torts de l'employeur, Travail FR uniquement, nullables).
            // Sous-objet `prise_acte_detail`. La prise d'acte CPH avec effets
            // licenciement / démission (Cass. soc. 25/06/2003 n°01-42.679) est un
            // mécanisme franco-français — ces champs restent null pour la BE.
            Boolean priseActeDefautPaiementSalaire,
            java.math.BigDecimal priseActeMontantImpayes,
            Boolean priseActeHarcelement,
            Boolean priseActeManquementSecurite,
            Boolean priseActeModificationContrat,
            Boolean priseActeDeclassement,
            Boolean priseActeDiscrimination,
            Boolean priseActeHeuresSupNonPayees,
            Boolean priseActeNonRespectRepos,
            Boolean priseActeGriefsPersistants,
            Boolean priseActeGriefImpossiblePoursuite,
            // SF-206-07 : 12 champs IA pour pré-fill F-DT-40 (résiliation
            // judiciaire du contrat de travail aux torts de l'employeur,
            // Travail FR uniquement, nullables). Sous-objet
            // `resiliation_judiciaire_detail`. La résiliation judiciaire CPH
            // avec maintien en poste (Cass. soc. 16/03/1989 ; Cass. soc.
            // 20/01/1998 ; art. L.1411-1 CT) est un mécanisme franco-français
            // — ces champs restent null pour la BE.
            Boolean resiliationJudDefautPaiementSalaire,
            java.math.BigDecimal resiliationJudMontantImpayes,
            Boolean resiliationJudHarcelement,
            Boolean resiliationJudManquementSecurite,
            Boolean resiliationJudModificationContrat,
            Boolean resiliationJudDeclassement,
            Boolean resiliationJudDiscrimination,
            Boolean resiliationJudHeuresSupNonPayees,
            Boolean resiliationJudNonRespectRepos,
            Boolean resiliationJudManquementsPersistants,
            Boolean resiliationJudSalarieEnPoste,
            Boolean resiliationJudLicenciementEnCours,
            // SF-207-06 : 4 champs IA Travail BE pour pré-fill F-207-06 outil
            // RCC BE conditions d'éligibilité (CCT 17 ; CCT 17/13 ; AR 03/05/2007
            // art. 3 et 8). Tous nullables — Travail BE uniquement, restent null
            // pour un dossier FR (aucun équivalent direct du RCC en droit français).
            String dateNaissanceSalarie,
            Integer anneesCarriereSalarie,
            Boolean metierLourdDetecte,
            Boolean entrepriseEnDifficulteDetectee,
            // SF-207-07 : 3 champs IA Travail BE pour pré-fill F-207-07 outil
            // RCC BE indemnité complémentaire (CCT 17 art. 5 ; loi 03/07/1978 ;
            // AR 03/05/2007). Tous nullables — Travail BE uniquement, restent
            // null pour un dossier FR. dateNaissanceSalarie (SF-207-06) est
            // réutilisé en amont du calcul de l'âge légal de pension.
            // - remunerationNetteReferenceRccDetectee : rémunération NETTE
            //   mensuelle de référence (€), base du / 2 de l'art. 5 CCT 17.
            // - allocationOnemMensuelleEstimee : allocation ONEM mensuelle (€)
            //   estimée — l'avocat la fournit (formule ONEM hors scope IA).
            // - dateDebutRccEnvisagee : date début effective RCC (ISO),
            //   base du comptage des mensualités jusqu'à pension.
            Double remunerationNetteReferenceRccDetectee,
            Double allocationOnemMensuelleEstimee,
            String dateDebutRccEnvisagee,
            // SF-206-03 : 5 champs IA pour pré-fill F-DT-75 (congés payés
            // acquis pendant arrêt maladie, Travail FR uniquement, nullables).
            String cpArretMaladieType,
            Integer cpArretMaladieNombreMois,
            Boolean cpArretMaladieSalarieEnPoste,
            String cpArretMaladieDateRupture,
            java.math.BigDecimal cpArretMaladieJoursDejaAccordes,
            // SF-207-08 : 3 champs IA Travail BE pour pré-fill F-207-08 outil
            // outplacement BE obligatoire 45+ (CCT n°82 ; CCT n°82 bis ;
            // Loi 05/09/2001 art. 13 ; AR 30/05/2018).
            Double ancienneteSalarie,
            String motifLicenciementDetecte,
            Boolean offreOutplacementMentionnee,
            // SF-246-29 : 14 champs IA pour pré-fill exhaustif F-DT-38 (rupture
            // de période d'essai, Travail FR uniquement, nullables). Sous-objet
            // `rupture_periode_essai_detail`. La période d'essai (L.1221-19 à
            // L.1221-25 CT) est un mécanisme franco-français — ces champs
            // restent null pour la BE (statut unique 2014, Loi 26/12/2013
            // abolissant la clause d'essai BE). Préfixe `rpe…` pour éviter
            // toute collision avec les champs existants
            // (grossesseAuMomentRupture, atMpDetecte, etc.).
            String rpeCategorieSocioProfessionnelle,
            Integer rpeDureeCddMois,
            Integer rpeDureePeriodeEssaiMois,
            Boolean rpeRenouvellementInvoque,
            Boolean rpeAccordBrancheRenouvellement,
            Boolean rpeAccordEcritSalarieRenouvellement,
            String rpeAuteurRupture,
            Integer rpeDelaiPrevenanceJours,
            Boolean rpeMotifLieCompetences,
            Boolean rpeMotifEconomique,
            String rpeAtteinteLiberteFondamentale,
            Boolean rpeLettreRuptureMotivee,
            Boolean rpeMotifsAveresParPieces,
            Boolean rpeCcnPlusFavorableRespectee,
            // SF-252-01 : 7 champs IA pour pré-fill F-DT-38 — 5 protections nullité
            // additionnelles + grossesse post-rupture (audit 2026-05-20). Sous-objet
            // `rupture_periode_essai_detail` enrichi. FR uniquement, nullables.
            Boolean rpeSalarieProtege,
            Boolean rpeAutorisationInspectionTravail,
            Boolean rpeLanceurAlerte,
            Boolean rpeTemoinHarcelement,
            Boolean rpeDroitRetraitExerce,
            Boolean rpeGrossesseDeclareePostRupture,
            String rpeDateNotificationGrossesse,
            // SF-212-01 : 6 champs IA pour pré-fill F-DT-36-licenciement-faute-grave-lourde
            // (Travail FR uniquement, nullables). Sous-objet `faute_grave_detail`.
            // La distinction faute grave / faute lourde (L. 1234-1 CT ; L. 1234-9 CT)
            // est un mécanisme franco-français — ces champs restent null pour la BE
            // (le motif grave belge relève d'un outil distinct F-DT-27).
            String fauteGraveFaitsReproches,
            List<String> fauteGraveDatesFaits,
            String fauteGraveQualificationEmployeur,
            Boolean fauteGraveIntentionNuireAlleeguee,
            Integer fauteGraveAncienneteMois,
            Double fauteGraveSalaireMensuelBrut,
            // SF-212-03 : 5 champs IA pour pré-fill F-DT-50-forfait-jours-validite
            // (Travail FR uniquement, nullables). Sous-objet `forfait_jours_detail`.
            // Le régime forfait jours L.3121-58+ CT (avec exigence post-Cass. soc.
            // 29/06/2011 sur l'effectivité du suivi de la charge) est un mécanisme
            // franco-français — ces champs restent null pour la BE.
            Boolean forfaitJoursAccordCollectifExiste,
            Boolean forfaitJoursEntretienAnnuelRealise,
            Boolean forfaitJoursDocumentControle,
            Boolean forfaitJoursCategorieAutonome,
            Integer forfaitJoursNbJours,
            // SF-212-05 : 5 champs IA pour pré-fill F-DT-72-transfert-entreprise-l1224-1
            // (Travail FR uniquement, nullables). Sous-objet `transfert_entreprise_detail`.
            String transfertTypeTransfert,
            Boolean transfertEeaIdentifiee,
            Boolean transfertActivitePreservee,
            Boolean transfertLicenciementsPreTransfert,
            String transfertDateTransfert,
            // SF-212-07 : 6 champs IA pour pré-fill F-DT-44-csp-crp-conformite (FR).
            // F-256 : regroupés en sous-record @JsonUnwrapped.
            @JsonUnwrapped CspDetail cspDetail,
            // SF-212-09 : 4 champs IA pour pré-fill F-DT-91-faute-inexcusable-employeur (FR).
            // F-256 : regroupés en sous-record @JsonUnwrapped.
            @JsonUnwrapped FauteInexcusableDetail fauteInexcusableDetail,
            // SF-212-25 : 4 champs IA pour pré-fill F-DT-61-lanceur-alerte-protection (FR).
            // F-256 : regroupés en sous-record @JsonUnwrapped.
            @JsonUnwrapped LanceurAlerteDetail lanceurAlerteDetail,
            // SF-212-11 : 4 champs IA pour pré-fill F-DT-70-modification-contrat-refus (FR).
            // F-256 : regroupés en sous-record @JsonUnwrapped.
            @JsonUnwrapped ModifContratDetail modifContratDetail,
            // SF-212-13 : 6 champs IA pour pré-fill F-DT-71-mutation-clause-mobilite (FR).
            // F-256 : regroupés en sous-record @JsonUnwrapped.
            @JsonUnwrapped MutationMobiliteDetail mutationMobiliteDetail,
            // SF-212-15 : 7 champs IA pour pré-fill F-DT-82-teletravail-accord (FR).
            // F-256 : regroupés en sous-record @JsonUnwrapped.
            @JsonUnwrapped TeletravailDetail teletravailDetail,
            // SF-212-19 : 7 champs IA pour pré-fill F-DT-48-mise-a-pied-disciplinaire (FR).
            // F-256 : regroupés en sous-record @JsonUnwrapped — JSON HTTP plat préservé,
            // 6 slots libérés sur le constructeur canonical.
            @JsonUnwrapped MiseAPiedDetail miseAPiedDetail,
            // SF-212-23 : sous-objet IA pour pré-fill F-DT-56-egalite-salariale-femmes-hommes (FR).
            // Sub-flag de egaliteSalarialePressentie : null si non documenté ou hors FRANCE.
            EgaliteSalarialeDetail egaliteSalarialeDetail,
            // F-256 SF-212-17 : sous-objet IA pour pré-fill F-DT-43 rupture anticipée CDD (FR).
            // 3 champs (auteur, motif, dateTerme). Sub-flag de ruptureAnticipeeCddDetectee.
            RuptureAnticipeeCddDetail ruptureAnticipeeCddDetail,
            // F-256 SF-212-21 : sous-objet IA pour pré-fill F-DT-41 démission équivoque (FR).
            // 5 champs (modeExpression, contexteAltercation, pression, retractation,
            // manquementsEmployeur). Sub-flag de demissionEquivoquePressentie.
            DemissionEquivoqueDetail demissionEquivoqueDetail,
            // F-256 SF-212-35 : sous-objet IA pour pré-fill F-DT-46 PDV / RCC conformité (FR).
            // 4 champs (typeDispositif, accordMajoritaire, validationDREETS, indemnitesLegales).
            // Sub-flag de pdvRccEnvisage.
            PdvRccDetail pdvRccDetail,
            // SF-212-29 : sous-objet IA pour pré-fill F-DT-77 congé maternité / paternité (FR).
            // 5 champs (typeConge, rangEnfant, naissanceMultiple, dateDebut, salaire).
            // Sub-flag de congeMaternitePaterniteDetecte.
            CongeMaternitePaterniteDetail congeMaternitePaterniteDetail,
            // SF-212-27 : flag F-205 — déclenche F-DT-64 burn-out reconnaissance MP (FR).
            // Pertinent quand un dossier évoque une demande de reconnaissance du burn-out
            // comme maladie professionnelle hors tableau via procédure CRRMP
            // (L. 461-1 al. 4 et 5 CSS, comité régional de reconnaissance des maladies
            // professionnelles, circulaire DGT 2016/01). Mécanisme franco-français.
            boolean burnoutDetecte,
            // SF-212-27 : sous-objet IA pour pré-fill F-DT-64 burn-out reconnaissance MP (FR).
            // 4 champs (diagnostic, taux IPP, surcharge documentée, arrêts maladie).
            // Sub-flag de burnoutDetecte. @JsonUnwrapped — JSON HTTP plat préservé.
            @JsonUnwrapped BurnoutDetail burnoutDetail,
            // SF-212-31 : flag F-205 — déclenche F-DT-65 élections CSE conformité (FR).
            // Pertinent quand un dossier évoque une procédure électorale CSE en cours
            // ou contestée (organisation, contestation, PAP, PV de carence, invitation
            // OS, vote électronique). L. 2314-1 à L. 2314-37 CT ; ordonnance n°2017-1386
            // du 22/09/2017 instituant le CSE. Mécanisme franco-français.
            boolean electionCseDetectee,
            // SF-212-31 : sous-objet IA pour pré-fill F-DT-65 élections CSE conformité (FR).
            // 4 champs (date élection, PAP négocié, collèges conformes, résultats contestés).
            // Sub-flag de electionCseDetectee. @JsonUnwrapped — JSON HTTP plat préservé.
            @JsonUnwrapped ElectionsCseDetail electionsCseDetail,
            // SF-212-33 : flag F-205 — déclenche F-DT-49 temps partiel — requalification en
            // temps plein (FR). Pertinent quand un dossier évoque une situation de temps
            // partiel susceptible d'être requalifié en temps complet : absence des mentions
            // obligatoires L. 3123-6 (durée, répartition), heures complémentaires
            // structurellement > 1/3 (L. 3123-9), modification unilatérale de la
            // répartition (L. 3123-12). Mécanisme strictement français.
            boolean tempsPartielRequalificationEnvisagee,
            // SF-212-33 : sous-objet IA pour pré-fill F-DT-49 temps partiel —
            // requalification en temps plein (FR). 4 champs (durée contractuelle,
            // mentions durée, mentions répartition, HC moyenne). Sub-flag de
            // tempsPartielRequalificationEnvisagee. @JsonUnwrapped — JSON HTTP plat préservé.
            @JsonUnwrapped TempsPartielRequalificationDetail tempsPartielRequalificationDetail,
            // SF-212-37 : flag F-205 — déclenche F-DT-84 conciliation CPH BCA (FR).
            // Pertinent quand un dossier évoque une procédure prud'homale dont la
            // phase de conciliation au Bureau de Conciliation et d'Orientation (BCO)
            // est envisagée : saisine CPH, convocation BCO, mention "conciliation",
            // "transaction prud'homale", "barème BCA". R. 1454-7 à R. 1454-12 CT ;
            // L. 1235-1 al. 3 CT — barème transactions BCA. Mécanisme franco-français.
            boolean conciliationCphEnvisagee,
            // SF-212-37 : sous-objet IA pour pré-fill F-DT-84 conciliation CPH BCA (FR).
            // 3 champs (ancienneté mois, salaire mensuel brut, montant demandes).
            // Sub-flag de conciliationCphEnvisagee. @JsonUnwrapped — JSON HTTP plat préservé.
            @JsonUnwrapped ConciliationCphDetail conciliationCphDetail,
            // SF-218-01 : date de notification du jugement CPH (ISO YYYY-MM-DD, nullable).
            // Pré-fill de l'outil F-DT-86 (appel CPH cour d'appel, FR) : fait courir le
            // délai d'appel d'un mois (art. 538 CPC ; R. 1461-1 CPC). null si non détectable.
            String dateNotificationJugement,
            // SF-218-01 : flag F-205 — déclenche F-DT-86 appel CPH cour d'appel (FR).
            // true uniquement si les pièces évoquent un jugement prud'homal rendu + une
            // intention d'interjeter appel. FR-only, default false. Régime BE distinct.
            boolean appelCphEnvisage) {

        /**
         * SF-212-23 — sous-objet pré-fill IA pour l'outil F-DT-56 (égalité
         * salariale femmes/hommes, FRANCE — L. 1142-7 à L. 1142-10 CT ;
         * L. 1144-1 CT ; loi 05/09/2018). Tous champs nullables ; null
         * implique pas de données IA pour la projection sur le formulaire UI.
         */
        public record EgaliteSalarialeDetail(
                String sexeSalarie,
                Double salaireBrut,
                Integer anciennete,
                Double ecartPourcentage
        ) {}

        /**
         * F-256 SF-212-19 — sous-record consolidant les 7 champs IA de F-DT-48
         * (mise à pied disciplinaire, FRANCE only). @JsonUnwrapped côté record
         * parent : les champs apparaissent à plat dans le JSON HTTP (parité
         * stricte du contrat externe).
         */
        public record MiseAPiedDetail(
                String mapDisciplinaireNature,
                Boolean mapDisciplinaireProcedureSuivie,
                Boolean mapDisciplinairePrescriptionFaute,
                Boolean mapDisciplinaireDureeRi,
                Integer mapDisciplinaireDureeJours,
                Boolean mapDisciplinaireSalaireSuspendu,
                Boolean mapDisciplinaireSanctionsAnterieures
        ) {}

        /**
         * F-256 SF-212-15 — sous-record consolidant les 7 champs IA de F-DT-82
         * (télétravail accord, FRANCE only). @JsonUnwrapped — JSON HTTP plat.
         */
        public record TeletravailDetail(
                String teletravailCadre,
                Boolean teletravailDoubleVolontariat,
                Boolean teletravailIndemniteVersee,
                Double teletravailMontantIndemniteJournalier,
                Boolean teletravailAccidentDomicile,
                Boolean teletravailRetourBureauImpose,
                Boolean teletravailRefusCauseIncrimination
        ) {}

        /**
         * F-256 SF-212-13 — sous-record consolidant les 6 champs IA de F-DT-71
         * (mutation / clause de mobilité, FRANCE only). @JsonUnwrapped — JSON HTTP plat.
         */
        public record MutationMobiliteDetail(
                Boolean mutationClausePresente,
                Boolean mutationZoneGeographiquePrecise,
                Boolean mutationInteretLegitimeEmployeur,
                Integer mutationDelaiPrevenanceSemaines,
                Boolean mutationSituationFamilialeContraingnante,
                Boolean mutationMotifProfessionnel
        ) {}

        /**
         * F-256 SF-212-11 — sous-record consolidant les 4 champs IA de F-DT-70
         * (modification du contrat — refus, FRANCE only). @JsonUnwrapped — JSON HTTP plat.
         */
        public record ModifContratDetail(
                String modifContratElementModifie,
                Boolean modifContratContractualise,
                Boolean modifContratMotifEco,
                Boolean modifContratNotifEcrite
        ) {}

        /**
         * F-256 SF-212-25 — sous-record consolidant les 4 champs IA de F-DT-61
         * (protection lanceur d'alerte, FRANCE only). @JsonUnwrapped — JSON HTTP plat.
         */
        public record LanceurAlerteDetail(
                String lanceurAlerteNatureSignalement,
                String lanceurAlerteProcedure,
                Boolean lanceurAlerteMesureRepresaille,
                String lanceurAlerteNatureMesure
        ) {}

        /**
         * F-256 SF-212-09 — sous-record consolidant les 4 champs IA de F-DT-91
         * (faute inexcusable employeur, FRANCE only). @JsonUnwrapped — JSON HTTP plat.
         */
        public record FauteInexcusableDetail(
                Boolean fauteInexcusableConscienceDanger,
                Boolean fauteInexcusableSignalementPrior,
                Boolean fauteInexcusableMesuresPrevention,
                Integer fauteInexcusableTauxIpp
        ) {}

        /**
         * F-256 SF-212-07 — sous-record consolidant les 6 champs IA de F-DT-44
         * (CSP / CRP conformité, FRANCE only). @JsonUnwrapped — JSON HTTP plat.
         */
        public record CspDetail(
                Integer cspEffectifEntreprise,
                Boolean cspProposeDetail,
                Boolean cspDocumentRemis,
                String cspDateRemise,
                Boolean cspAdhesion,
                Double cspSalaireMensuelBrut
        ) {}

        /**
         * F-256 SF-212-17 — sous-record IA pour F-DT-43 rupture anticipée du CDD
         * (FRANCE only). 3 champs alignés sur le prompt PART9 :
         * {@code rupture_anticipee_cdd_auteur} (EMPLOYEUR / SALARIE),
         * {@code rupture_anticipee_cdd_motif} (ACCORD_PARTIES / FAUTE_GRAVE /
         * FORCE_MAJEURE / INAPTITUDE / CDI_EMBAUCHE / AUTRE),
         * {@code rupture_anticipee_cdd_date_terme} (ISO YYYY-MM-DD).
         * Tous nullables — null hors FRANCE ou si non documenté.
         */
        public record RuptureAnticipeeCddDetail(
                String ruptureAnticipeeCddAuteur,
                String ruptureAnticipeeCddMotif,
                String ruptureAnticipeeCddDateTerme
        ) {}

        /**
         * F-256 SF-212-21 — sous-record IA pour F-DT-41 démission validité
         * équivoque (FRANCE only). 5 champs alignés sur la mini-spec SF-212-21 :
         * mode d'expression de la démission (texte libre / enum à raffiner),
         * contexte d'altercation, pression sur le salarié, rétractation rapide,
         * manquements employeur contemporains. Tous nullables — null hors FRANCE.
         */
        public record DemissionEquivoqueDetail(
                String demissionModeExpression,
                Boolean demissionContexteAltercation,
                Boolean demissionPression,
                Boolean demissionRetractation,
                Boolean demissionManquementsEmployeur
        ) {}

        /**
         * F-256 SF-212-35 — sous-record IA pour F-DT-46 PDV / RCC conformité
         * (FRANCE only). 4 champs alignés sur le prompt PART12 :
         * {@code pdv_rcc_type_dispositif} (RCC / PDV),
         * {@code pdv_rcc_accord_majoritaire},
         * {@code pdv_rcc_validation_dreets},
         * {@code pdv_rcc_indemnites_legales}.
         * Tous nullables — null hors FRANCE ou si non documenté.
         */
        public record PdvRccDetail(
                String pdvRccTypeDispositif,
                Boolean pdvRccAccordMajoritaire,
                Boolean pdvRccValidationDREETS,
                Boolean pdvRccIndemnitesLegales
        ) {}

        /**
         * SF-212-29 — sous-record IA pour F-DT-77 congé maternité / paternité
         * (FRANCE only). 5 champs alignés sur le prompt PART14 :
         * {@code conge_maternite_paternite_type} (MATERNITE / PATERNITE),
         * {@code conge_maternite_rang_enfant} (entier ≥ 1),
         * {@code conge_maternite_naissance_multiple},
         * {@code conge_maternite_date_debut} (ISO YYYY-MM-DD),
         * {@code conge_maternite_salaire_mensuel_brut} (€).
         * Tous nullables — null hors FRANCE ou si non documenté.
         */
        public record CongeMaternitePaterniteDetail(
                String congeMaternitePaterniteType,
                Integer congeMaterniteRangEnfant,
                Boolean congeMaterniteNaissanceMultiple,
                String congeMaterniteDateDebut,
                Double congeMaterniteSalaireMensuelBrut
        ) {}

        /**
         * SF-212-27 — sous-record IA pour F-DT-64 burn-out reconnaissance maladie
         * professionnelle hors tableau (FRANCE only — L. 461-1 al. 4 et 5 CSS ;
         * comité régional de reconnaissance des maladies professionnelles CRRMP ;
         * circulaire DGT 2016/01). 4 champs alignés sur le prompt PART13 :
         * {@code burnout_diagnostic} (diagnostic médical posé),
         * {@code burnout_taux_ipp} (taux d'incapacité permanente partielle estimé
         * en %, doit être ≥ 25 % pour ouvrir la procédure hors tableau),
         * {@code burnout_surcharge_documentee} (surcharge / manquements employeur
         * obligation sécurité L. 4121-1 documentés), {@code burnout_arrets_maladie}
         * (arrêts maladie cumulés documentés).
         * Tous nullables — null hors FRANCE ou si non documenté.
         * @JsonUnwrapped — JSON HTTP plat préservé (parité contrat externe stricte).
         */
        public record BurnoutDetail(
                Boolean burnoutDiagnostic,
                Integer burnoutTauxIpp,
                Boolean burnoutSurchargeDocumentee,
                Boolean burnoutArretsMaladie
        ) {}

        /**
         * SF-212-31 — sous-record IA pour F-DT-65 élections CSE conformité
         * (FRANCE only — L. 2314-1 à L. 2314-37 CT ; R. 2314-1+ CT ;
         * ordonnance n°2017-1386 du 22/09/2017 instituant le CSE). 4 champs
         * alignés sur le prompt PART15 :
         * {@code election_cse_date_election} (ISO YYYY-MM-DD — date du 1er
         * tour, base du calcul du délai de contestation de 15 jours
         * L. 2314-32 CT),
         * {@code election_cse_pap_negocie} (PAP négocié avec OS L. 2314-6 CT),
         * {@code election_cse_colleges_conformes} (au moins 2 collèges
         * L. 2314-11 CT),
         * {@code election_cse_resultats_contestes} (résultats contestés).
         * Tous nullables — null hors FRANCE ou si non documenté.
         * @JsonUnwrapped — JSON HTTP plat préservé (parité contrat externe stricte).
         */
        public record ElectionsCseDetail(
                String electionCseDateElection,
                Boolean electionCsePapNegocie,
                Boolean electionCseCollegesConformes,
                Boolean electionCseResultatsContestes
        ) {}

        /**
         * SF-212-33 — sous-record IA pour F-DT-49 temps partiel —
         * requalification en temps plein (FRANCE only — L. 3123-1 à
         * L. 3123-20 CT ; L. 3123-6 mentions obligatoires ; L. 3123-9
         * plafond heures complémentaires 1/10 ou 1/3 ; L. 3245-1
         * prescription triennale rappel salaire ; Cass. soc. 22/01/1992
         * présomption de temps complet réfragable). 4 champs alignés
         * sur le prompt PART16 :
         * {@code temps_partiel_duree_contractuelle} (durée hebdomadaire
         * contractuelle en heures — sert de base au calcul du ratio HC),
         * {@code temps_partiel_mentions_duree} (true si le contrat
         * mentionne la durée — L. 3123-6),
         * {@code temps_partiel_mentions_repartition} (true si le contrat
         * mentionne la répartition jours/semaines — L. 3123-6),
         * {@code temps_partiel_hc_moyenne} (heures complémentaires
         * réellement effectuées en moyenne par semaine).
         * Tous nullables — null hors FRANCE ou si non documenté.
         * @JsonUnwrapped — JSON HTTP plat préservé (parité contrat externe stricte).
         */
        public record TempsPartielRequalificationDetail(
                Double tempsPartielDureeContractuelle,
                Boolean tempsPartielMentionsDuree,
                Boolean tempsPartielMentionsRepartition,
                Double tempsPartielHCMoyenne
        ) {}

        /**
         * SF-212-37 — sous-record IA pour F-DT-84 conciliation CPH BCA
         * (FRANCE only — R. 1454-7 à R. 1454-12 CT ; L. 1235-1 al. 3 CT
         * — barème transactions BCA). 3 champs alignés sur le prompt PART17 :
         * {@code conciliation_cph_anciennete_mois} (ancienneté en mois —
         * base du palier BCA),
         * {@code conciliation_cph_salaire} (salaire mensuel brut en € —
         * base de calcul du montant minimum BCA),
         * {@code conciliation_cph_montant_demandes} (montant total des
         * demandes du salarié en € — base de comparaison BCA vs Macron).
         * Tous nullables — null hors FRANCE ou si non documenté.
         * @JsonUnwrapped — JSON HTTP plat préservé (parité contrat externe stricte).
         */
        public record ConciliationCphDetail(
                Integer conciliationCphAncienneteMois,
                Double conciliationCphSalaire,
                Double conciliationCphMontantDemandes
        ) {}


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
                    .teletravailLitigeDetecte(teletravailLitigeDetecte)
                    .miseAPiedDisciplinaireDetectee(miseAPiedDisciplinaireDetectee)
                    .egaliteSalarialePressentie(egaliteSalarialePressentie)
                    .ruptureAnticipeeCddDetectee(ruptureAnticipeeCddDetectee)
                    .demissionEquivoquePressentie(demissionEquivoquePressentie)
                    .pdvRccEnvisage(pdvRccEnvisage)
                    // SF-212-29 — F-205 flag F-DT-77 congé maternité / paternité.
                    .congeMaternitePaterniteDetecte(congeMaternitePaterniteDetecte)
                    .burnoutDetecte(burnoutDetecte)
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
                    .dateNotificationDecisionOnem(dateNotificationDecisionOnem)
                    .dateDecisionDirecteur(dateDecisionDirecteur)
                    .recoursAdminDejaForme(recoursAdminDejaForme)
                    .dateAccident(dateAccident)
                    .dateConnaissanceAccidentEmployeur(dateConnaissanceAccidentEmployeur)
                    .motifUrgenceDetecte(motifUrgenceDetecte)
                    .dateFaitGenerateurUrgence(dateFaitGenerateurUrgence)
                    .perilImmediatPresume(perilImmediatPresume)
                    .procedureTravailDetectee(procedureTravailDetectee)
                    .dateDeclencheurProcedure(dateDeclencheurProcedure)
                    // SF-246-21 — requalification_detection
                    .cddDureeMois(cddDureeMois)
                    .cddDateFinDernierContrat(cddDateFinDernierContrat)
                    .cddNouveauDateDebut(cddNouveauDateDebut)
                    .cddNouveauDateFin(cddNouveauDateFin)
                    .cddTotalSalairesBruts(cddTotalSalairesBruts)
                    .interimDureeTotaleMois(interimDureeTotaleMois)
                    .interimDateFinDerniereMission(interimDateFinDerniereMission)
                    .interimNouvellesMissionDateDebut(interimNouvellesMissionDateDebut)
                    .interimNouvellesMissionDateFin(interimNouvellesMissionDateFin)
                    .interimEntrepriseUtilisatrice(interimEntrepriseUtilisatrice)
                    .interimTotalRemunerationsBrutes(interimTotalRemunerationsBrutes)
                    .interimDureeMissionJours(interimDureeMissionJours)
                    // SF-246-21 — paie_detection
                    .congesJoursAcquis(congesJoursAcquis)
                    .congesJoursPris(congesJoursPris)
                    .rappelSalaireMontantPerverseMensuel(rappelSalaireMontantPerverseMensuel)
                    .rappelSalairePeriodeDebut(rappelSalairePeriodeDebut)
                    .rappelSalairePeriodeFin(rappelSalairePeriodeFin)
                    // SF-246-21 — rupture_collective_detection
                    .salarieAgeAnnees(salarieAgeAnnees)
                    .pseNombreSalaries(pseNombreSalaries)
                    .pseNombreLicenciements(pseNombreLicenciements)
                    .transactionDateSignature(transactionDateSignature)
                    .transactionIndemniteMontantEur(transactionIndemniteMontantEur)
                    // SF-246-21 — sante_discrimination_detection
                    .atDateAccident(atDateAccident)
                    .atDateExposition(atDateExposition)
                    .areTypeDecision(areTypeDecision)
                    .areMontantConteste(areMontantConteste)
                    .discriminationMotif(discriminationMotif)
                    .discriminationContexte(discriminationContexte)
                    // SF-246-21 — procedure_details_detection
                    .refereMontantProvision(refereMontantProvision)
                    .documentsDateCertificatTravail(documentsDateCertificatTravail)
                    .documentsDateAttestationFranceTravail(documentsDateAttestationFranceTravail)
                    .documentsDateSoldeToutCompte(documentsDateSoldeToutCompte)
                    // SF-246-23 — travail_be_detection (BELGIQUE uniquement)
                    .dateConnaissanceFait(dateConnaissanceFait)
                    .dateNotificationMotifs(dateNotificationMotifs)
                    .commissionParitaireBe(commissionParitaireBe)
                    .joursTravaillesAnneePrecedenteBe(joursTravaillesAnneePrecedenteBe)
                    .joursPrestesBe(joursPrestesBe)
                    .dateDemandeCreditTemps(dateDemandeCreditTemps)
                    // SF-206-01 — abandon de poste / présomption de démission
                    .abandonPosteDateMiseEnDemeure(abandonPosteDateMiseEnDemeure)
                    .abandonPosteModeNotification(abandonPosteModeNotification)
                    .abandonPosteDelaiAccordeJours(abandonPosteDelaiAccordeJours)
                    .abandonPosteMotifAbsence(abandonPosteMotifAbsence)
                    .abandonPosteDateReprise(abandonPosteDateReprise)
                    .abandonPosteMedMentionneDelai(abandonPosteMedMentionneDelai)
                    .abandonPosteMedMentionneConsequences(abandonPosteMedMentionneConsequences)
                    .abandonPosteRepriseDansDelai(abandonPosteRepriseDansDelai)
                    // SF-206-05 — prise_acte_detail (FRANCE uniquement)
                    .priseActeDefautPaiementSalaire(priseActeDefautPaiementSalaire)
                    .priseActeMontantImpayes(priseActeMontantImpayes)
                    .priseActeHarcelement(priseActeHarcelement)
                    .priseActeManquementSecurite(priseActeManquementSecurite)
                    .priseActeModificationContrat(priseActeModificationContrat)
                    .priseActeDeclassement(priseActeDeclassement)
                    .priseActeDiscrimination(priseActeDiscrimination)
                    .priseActeHeuresSupNonPayees(priseActeHeuresSupNonPayees)
                    .priseActeNonRespectRepos(priseActeNonRespectRepos)
                    .priseActeGriefsPersistants(priseActeGriefsPersistants)
                    .priseActeGriefImpossiblePoursuite(priseActeGriefImpossiblePoursuite)
                    // SF-206-07 — resiliation_judiciaire_detail (FRANCE uniquement)
                    .resiliationJudDefautPaiementSalaire(resiliationJudDefautPaiementSalaire)
                    .resiliationJudMontantImpayes(resiliationJudMontantImpayes)
                    .resiliationJudHarcelement(resiliationJudHarcelement)
                    .resiliationJudManquementSecurite(resiliationJudManquementSecurite)
                    .resiliationJudModificationContrat(resiliationJudModificationContrat)
                    .resiliationJudDeclassement(resiliationJudDeclassement)
                    .resiliationJudDiscrimination(resiliationJudDiscrimination)
                    .resiliationJudHeuresSupNonPayees(resiliationJudHeuresSupNonPayees)
                    .resiliationJudNonRespectRepos(resiliationJudNonRespectRepos)
                    .resiliationJudManquementsPersistants(resiliationJudManquementsPersistants)
                    .resiliationJudSalarieEnPoste(resiliationJudSalarieEnPoste)
                    .resiliationJudLicenciementEnCours(resiliationJudLicenciementEnCours)
                    // SF-207-06 — rcc_be_conditions_detection (BELGIQUE uniquement)
                    .dateNaissanceSalarie(dateNaissanceSalarie)
                    .anneesCarriereSalarie(anneesCarriereSalarie)
                    .metierLourdDetecte(metierLourdDetecte)
                    .entrepriseEnDifficulteDetectee(entrepriseEnDifficulteDetectee)
                    // SF-207-07 — rcc_be_indemnite_detection (BELGIQUE uniquement)
                    .remunerationNetteReferenceRccDetectee(remunerationNetteReferenceRccDetectee)
                    .allocationOnemMensuelleEstimee(allocationOnemMensuelleEstimee)
                    .dateDebutRccEnvisagee(dateDebutRccEnvisagee)
                    // SF-206-03 — conges_payes_arret_maladie_detail (FRANCE uniquement)
                    .cpArretMaladieType(cpArretMaladieType)
                    .cpArretMaladieNombreMois(cpArretMaladieNombreMois)
                    .cpArretMaladieSalarieEnPoste(cpArretMaladieSalarieEnPoste)
                    .cpArretMaladieDateRupture(cpArretMaladieDateRupture)
                    .cpArretMaladieJoursDejaAccordes(cpArretMaladieJoursDejaAccordes)
                    // SF-207-08 — outplacement_be_detection (BELGIQUE uniquement)
                    .ancienneteSalarie(ancienneteSalarie)
                    .motifLicenciementDetecte(motifLicenciementDetecte)
                    .offreOutplacementMentionnee(offreOutplacementMentionnee)
                    // SF-246-29 — rupture_periode_essai_detail (FRANCE uniquement)
                    .rpeCategorieSocioProfessionnelle(rpeCategorieSocioProfessionnelle)
                    .rpeDureeCddMois(rpeDureeCddMois)
                    .rpeDureePeriodeEssaiMois(rpeDureePeriodeEssaiMois)
                    .rpeRenouvellementInvoque(rpeRenouvellementInvoque)
                    .rpeAccordBrancheRenouvellement(rpeAccordBrancheRenouvellement)
                    .rpeAccordEcritSalarieRenouvellement(rpeAccordEcritSalarieRenouvellement)
                    .rpeAuteurRupture(rpeAuteurRupture)
                    .rpeDelaiPrevenanceJours(rpeDelaiPrevenanceJours)
                    .rpeMotifLieCompetences(rpeMotifLieCompetences)
                    .rpeMotifEconomique(rpeMotifEconomique)
                    .rpeAtteinteLiberteFondamentale(rpeAtteinteLiberteFondamentale)
                    .rpeLettreRuptureMotivee(rpeLettreRuptureMotivee)
                    .rpeMotifsAveresParPieces(rpeMotifsAveresParPieces)
                    .rpeCcnPlusFavorableRespectee(rpeCcnPlusFavorableRespectee)
                    // SF-252-01 — 7 protections nullité additionnelles (FR uniquement)
                    .rpeSalarieProtege(rpeSalarieProtege)
                    .rpeAutorisationInspectionTravail(rpeAutorisationInspectionTravail)
                    .rpeLanceurAlerte(rpeLanceurAlerte)
                    .rpeTemoinHarcelement(rpeTemoinHarcelement)
                    .rpeDroitRetraitExerce(rpeDroitRetraitExerce)
                    .rpeGrossesseDeclareePostRupture(rpeGrossesseDeclareePostRupture)
                    .rpeDateNotificationGrossesse(rpeDateNotificationGrossesse)
                    // SF-212-01 — faute_grave_detail (FRANCE uniquement)
                    .fauteGraveFaitsReproches(fauteGraveFaitsReproches)
                    .fauteGraveDatesFaits(fauteGraveDatesFaits)
                    .fauteGraveQualificationEmployeur(fauteGraveQualificationEmployeur)
                    .fauteGraveIntentionNuireAlleeguee(fauteGraveIntentionNuireAlleeguee)
                    .fauteGraveAncienneteMois(fauteGraveAncienneteMois)
                    .fauteGraveSalaireMensuelBrut(fauteGraveSalaireMensuelBrut)
                    // SF-212-03 — forfait_jours_detail (FRANCE uniquement)
                    .forfaitJoursAccordCollectifExiste(forfaitJoursAccordCollectifExiste)
                    .forfaitJoursEntretienAnnuelRealise(forfaitJoursEntretienAnnuelRealise)
                    .forfaitJoursDocumentControle(forfaitJoursDocumentControle)
                    .forfaitJoursCategorieAutonome(forfaitJoursCategorieAutonome)
                    .forfaitJoursNbJours(forfaitJoursNbJours)
                    // SF-212-05 — transfert_entreprise_detail (FRANCE uniquement)
                    .transfertTypeTransfert(transfertTypeTransfert)
                    .transfertEeaIdentifiee(transfertEeaIdentifiee)
                    .transfertActivitePreservee(transfertActivitePreservee)
                    .transfertLicenciementsPreTransfert(transfertLicenciementsPreTransfert)
                    .transfertDateTransfert(transfertDateTransfert)
                    // F-256 SF-212-07 — csp_detail consolidé
                    .cspDetail(cspDetail)
                    // F-256 SF-212-09 — faute_inexcusable_detail consolidé
                    .fauteInexcusableDetail(fauteInexcusableDetail)
                    // F-256 SF-212-25 — lanceur_alerte_detail consolidé
                    .lanceurAlerteDetail(lanceurAlerteDetail)
                    // F-256 SF-212-11 — modification_contrat_detail consolidé
                    .modifContratDetail(modifContratDetail)
                    // F-256 SF-212-13 — mutation_mobilite_detail consolidé
                    .mutationMobiliteDetail(mutationMobiliteDetail)
                    // F-256 SF-212-15 — teletravail_detail consolidé
                    .teletravailDetail(teletravailDetail)
                    // F-256 SF-212-19 — mise_a_pied_detail consolidé
                    .miseAPiedDetail(miseAPiedDetail)
                    // SF-212-23 — egalite_salariale_detail
                    .egaliteSalarialeDetail(egaliteSalarialeDetail)
                    // F-256 SF-212-17 — rupture_anticipee_cdd_detail
                    .ruptureAnticipeeCddDetail(ruptureAnticipeeCddDetail)
                    // F-256 SF-212-21 — demission_equivoque_detail
                    .demissionEquivoqueDetail(demissionEquivoqueDetail)
                    // F-256 SF-212-35 — pdv_rcc_detail
                    .pdvRccDetail(pdvRccDetail)
                    // SF-212-29 — conge_maternite_paternite_detail
                    .congeMaternitePaterniteDetail(congeMaternitePaterniteDetail)
                    // SF-212-27 — burn-out reconnaissance MP (FR)
                    .burnoutDetecte(burnoutDetecte)
                    .burnoutDetail(burnoutDetail)
                    // SF-212-31 — élections CSE conformité (FR)
                    .electionCseDetectee(electionCseDetectee)
                    .electionsCseDetail(electionsCseDetail)
                    // SF-212-33 — temps partiel — requalification (FR)
                    .tempsPartielRequalificationEnvisagee(tempsPartielRequalificationEnvisagee)
                    .tempsPartielRequalificationDetail(tempsPartielRequalificationDetail)
                    // SF-212-37 — conciliation CPH BCA (FR)
                    .conciliationCphEnvisagee(conciliationCphEnvisagee)
                    .conciliationCphDetail(conciliationCphDetail)
                    // SF-218-01 — appel CPH cour d'appel (FR)
                    .dateNotificationJugement(dateNotificationJugement)
                    .appelCphEnvisage(appelCphEnvisage);
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
            private boolean teletravailLitigeDetecte;
            // SF-212-19 — F-205 flag (FRANCE only) — déclenche F-DT-48 mise à pied disciplinaire.
            private boolean miseAPiedDisciplinaireDetectee;
            // SF-212-23 — F-205 flag (FRANCE only) — déclenche F-DT-56 égalité salariale femmes/hommes.
            private boolean egaliteSalarialePressentie;
            // SF-212-17 — F-205 flag (FRANCE only) — déclenche F-DT-43 rupture anticipée CDD.
            private boolean ruptureAnticipeeCddDetectee;
            // F-256 SF-212-21 — F-205 flag (FRANCE only) — déclenche F-DT-41 démission équivoque.
            private boolean demissionEquivoquePressentie;
            // F-256 SF-212-35 — F-205 flag (FRANCE only) — déclenche F-DT-46 PDV/RCC conformité.
            private boolean pdvRccEnvisage;
            // SF-212-29 — F-205 flag (FRANCE only) — déclenche F-DT-77 congé maternité / paternité.
            private boolean congeMaternitePaterniteDetecte;
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
            // SF-207-03 — 3 champs IA Travail BE pour pré-fill F-207-03 contestation C4 ONEM.
            private String dateNotificationDecisionOnem;
            private String dateDecisionDirecteur;
            private Boolean recoursAdminDejaForme;
            // SF-207-04 — 2 champs IA Travail BE pour pré-fill F-207-04 déclaration AT Fedris.
            private String dateAccident;
            private String dateConnaissanceAccidentEmployeur;
            // SF-207-05 — 3 champs IA Travail BE pour pré-fill F-207-05 référé tribunal du travail BE.
            private String motifUrgenceDetecte;
            private String dateFaitGenerateurUrgence;
            private Boolean perilImmediatPresume;
            // SF-246-22 — type de procédure travail + date déclencheur pour pré-fill F-136.
            private String procedureTravailDetectee;
            private String dateDeclencheurProcedure;
            // SF-246-21 — requalification_detection (CDD + intérim)
            private Integer cddDureeMois;
            private String cddDateFinDernierContrat;
            private String cddNouveauDateDebut;
            private String cddNouveauDateFin;
            private Double cddTotalSalairesBruts;
            private Integer interimDureeTotaleMois;
            private String interimDateFinDerniereMission;
            private String interimNouvellesMissionDateDebut;
            private String interimNouvellesMissionDateFin;
            private String interimEntrepriseUtilisatrice;
            private Double interimTotalRemunerationsBrutes;
            private Integer interimDureeMissionJours;
            // SF-246-21 — paie_detection (CP + rappel salaire)
            private Integer congesJoursAcquis;
            private Integer congesJoursPris;
            private Double rappelSalaireMontantPerverseMensuel;
            private String rappelSalairePeriodeDebut;
            private String rappelSalairePeriodeFin;
            // SF-246-21 — rupture_collective_detection (lic-éco + PSE + transaction)
            private Integer salarieAgeAnnees;
            private Integer pseNombreSalaries;
            private Integer pseNombreLicenciements;
            private String transactionDateSignature;
            private Double transactionIndemniteMontantEur;
            // SF-246-21 — sante_discrimination_detection (AT/MP + ARE + discrimination)
            private String atDateAccident;
            private String atDateExposition;
            private String areTypeDecision;
            private Double areMontantConteste;
            private String discriminationMotif;
            private String discriminationContexte;
            // SF-246-21 — procedure_details_detection (référé + documents fin contrat)
            private Double refereMontantProvision;
            private String documentsDateCertificatTravail;
            private String documentsDateAttestationFranceTravail;
            private String documentsDateSoldeToutCompte;
            // SF-246-23 — travail_be_detection (BELGIQUE uniquement)
            private String dateConnaissanceFait;
            private String dateNotificationMotifs;
            private String commissionParitaireBe;
            private Integer joursTravaillesAnneePrecedenteBe;
            private Integer joursPrestesBe;
            private String dateDemandeCreditTemps;
            // SF-206-01
            private String abandonPosteDateMiseEnDemeure;
            private String abandonPosteModeNotification;
            private Integer abandonPosteDelaiAccordeJours;
            private String abandonPosteMotifAbsence;
            private String abandonPosteDateReprise;
            private Boolean abandonPosteMedMentionneDelai;
            private Boolean abandonPosteMedMentionneConsequences;
            private Boolean abandonPosteRepriseDansDelai;
            // SF-206-05 — prise_acte_detail (FRANCE uniquement)
            private Boolean priseActeDefautPaiementSalaire;
            private java.math.BigDecimal priseActeMontantImpayes;
            private Boolean priseActeHarcelement;
            private Boolean priseActeManquementSecurite;
            private Boolean priseActeModificationContrat;
            private Boolean priseActeDeclassement;
            private Boolean priseActeDiscrimination;
            private Boolean priseActeHeuresSupNonPayees;
            private Boolean priseActeNonRespectRepos;
            private Boolean priseActeGriefsPersistants;
            private Boolean priseActeGriefImpossiblePoursuite;
            // SF-206-07 — resiliation_judiciaire_detail (FRANCE uniquement)
            private Boolean resiliationJudDefautPaiementSalaire;
            private java.math.BigDecimal resiliationJudMontantImpayes;
            private Boolean resiliationJudHarcelement;
            private Boolean resiliationJudManquementSecurite;
            private Boolean resiliationJudModificationContrat;
            private Boolean resiliationJudDeclassement;
            private Boolean resiliationJudDiscrimination;
            private Boolean resiliationJudHeuresSupNonPayees;
            private Boolean resiliationJudNonRespectRepos;
            private Boolean resiliationJudManquementsPersistants;
            private Boolean resiliationJudSalarieEnPoste;
            private Boolean resiliationJudLicenciementEnCours;
            // SF-207-06 — rcc_be_conditions_detection (BELGIQUE uniquement)
            private String dateNaissanceSalarie;
            private Integer anneesCarriereSalarie;
            private Boolean metierLourdDetecte;
            private Boolean entrepriseEnDifficulteDetectee;
            // SF-207-07 — rcc_be_indemnite_detection (BELGIQUE uniquement)
            private Double remunerationNetteReferenceRccDetectee;
            private Double allocationOnemMensuelleEstimee;
            private String dateDebutRccEnvisagee;
            // SF-206-03 — conges_payes_arret_maladie_detail (FRANCE uniquement)
            private String cpArretMaladieType;
            private Integer cpArretMaladieNombreMois;
            private Boolean cpArretMaladieSalarieEnPoste;
            private String cpArretMaladieDateRupture;
            private java.math.BigDecimal cpArretMaladieJoursDejaAccordes;
            // SF-207-08 — outplacement_be_detection (BELGIQUE uniquement)
            private Double ancienneteSalarie;
            private String motifLicenciementDetecte;
            private Boolean offreOutplacementMentionnee;
            // SF-246-29 — rupture_periode_essai_detail (FRANCE uniquement)
            private String rpeCategorieSocioProfessionnelle;
            private Integer rpeDureeCddMois;
            private Integer rpeDureePeriodeEssaiMois;
            private Boolean rpeRenouvellementInvoque;
            private Boolean rpeAccordBrancheRenouvellement;
            private Boolean rpeAccordEcritSalarieRenouvellement;
            private String rpeAuteurRupture;
            private Integer rpeDelaiPrevenanceJours;
            private Boolean rpeMotifLieCompetences;
            private Boolean rpeMotifEconomique;
            private String rpeAtteinteLiberteFondamentale;
            private Boolean rpeLettreRuptureMotivee;
            private Boolean rpeMotifsAveresParPieces;
            private Boolean rpeCcnPlusFavorableRespectee;
            // SF-252-01 — 7 protections nullité additionnelles (FR uniquement)
            private Boolean rpeSalarieProtege;
            private Boolean rpeAutorisationInspectionTravail;
            private Boolean rpeLanceurAlerte;
            private Boolean rpeTemoinHarcelement;
            private Boolean rpeDroitRetraitExerce;
            private Boolean rpeGrossesseDeclareePostRupture;
            private String rpeDateNotificationGrossesse;
            // SF-212-01 — faute_grave_detail (FRANCE uniquement)
            private String fauteGraveFaitsReproches;
            private List<String> fauteGraveDatesFaits;
            private String fauteGraveQualificationEmployeur;
            private Boolean fauteGraveIntentionNuireAlleeguee;
            private Integer fauteGraveAncienneteMois;
            private Double fauteGraveSalaireMensuelBrut;
            // SF-212-03 — forfait_jours_detail (FRANCE uniquement)
            private Boolean forfaitJoursAccordCollectifExiste;
            private Boolean forfaitJoursEntretienAnnuelRealise;
            private Boolean forfaitJoursDocumentControle;
            private Boolean forfaitJoursCategorieAutonome;
            private Integer forfaitJoursNbJours;
            // SF-212-05 — transfert_entreprise_detail (FRANCE uniquement)
            private String transfertTypeTransfert;
            private Boolean transfertEeaIdentifiee;
            private Boolean transfertActivitePreservee;
            private Boolean transfertLicenciementsPreTransfert;
            private String transfertDateTransfert;
            // F-256 — sous-records consolidés (slots libérés)
            private CspDetail cspDetail;
            private FauteInexcusableDetail fauteInexcusableDetail;
            private LanceurAlerteDetail lanceurAlerteDetail;
            private ModifContratDetail modifContratDetail;
            private MutationMobiliteDetail mutationMobiliteDetail;
            private TeletravailDetail teletravailDetail;
            private MiseAPiedDetail miseAPiedDetail;
            // SF-212-23 — egalite_salariale_detail (FRANCE uniquement) — sous-record regroupé.
            private EgaliteSalarialeDetail egaliteSalarialeDetail;
            // F-256 — 3 sous-records pour résorber les dettes pré-fill
            private RuptureAnticipeeCddDetail ruptureAnticipeeCddDetail;
            private DemissionEquivoqueDetail demissionEquivoqueDetail;
            private PdvRccDetail pdvRccDetail;
            // SF-212-29 — conge_maternite_paternite_detail (FRANCE uniquement)
            private CongeMaternitePaterniteDetail congeMaternitePaterniteDetail;
            // SF-212-27 — flag F-205 + sous-record burn-out reconnaissance MP (FR uniquement)
            private boolean burnoutDetecte;
            private BurnoutDetail burnoutDetail;
            // SF-212-31 — flag F-205 + sous-record élections CSE conformité (FR uniquement)
            private boolean electionCseDetectee;
            private ElectionsCseDetail electionsCseDetail;
            // SF-212-33 — flag F-205 + sous-record temps partiel — requalification (FR uniquement)
            private boolean tempsPartielRequalificationEnvisagee;
            private TempsPartielRequalificationDetail tempsPartielRequalificationDetail;
            // SF-212-37 — flag F-205 + sous-record conciliation CPH BCA (FR uniquement)
            private boolean conciliationCphEnvisagee;
            private ConciliationCphDetail conciliationCphDetail;
            // SF-218-01 — appel CPH cour d'appel (FR)
            private String dateNotificationJugement;
            private boolean appelCphEnvisage;

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
            public Builder teletravailLitigeDetecte(boolean v) { this.teletravailLitigeDetecte = v; return this; }
            // SF-212-19 — F-205 flag (FRANCE only) — déclenche F-DT-48 mise à pied disciplinaire.
            public Builder miseAPiedDisciplinaireDetectee(boolean v) { this.miseAPiedDisciplinaireDetectee = v; return this; }
            // SF-212-23 — F-205 flag (FRANCE only) — déclenche F-DT-56 égalité salariale femmes/hommes.
            public Builder egaliteSalarialePressentie(boolean v) { this.egaliteSalarialePressentie = v; return this; }
            // SF-212-17 — F-205 flag (FRANCE only) — déclenche F-DT-43 rupture anticipée CDD.
            public Builder ruptureAnticipeeCddDetectee(boolean v) { this.ruptureAnticipeeCddDetectee = v; return this; }
            // F-256 SF-212-21 — F-205 flag (FRANCE only) — déclenche F-DT-41 démission équivoque.
            public Builder demissionEquivoquePressentie(boolean v) { this.demissionEquivoquePressentie = v; return this; }
            // F-256 SF-212-35 — F-205 flag (FRANCE only) — déclenche F-DT-46 PDV/RCC conformité.
            public Builder pdvRccEnvisage(boolean v) { this.pdvRccEnvisage = v; return this; }
            // SF-212-29 — F-205 flag (FRANCE only) — déclenche F-DT-77 congé maternité / paternité.
            public Builder congeMaternitePaterniteDetecte(boolean v) { this.congeMaternitePaterniteDetecte = v; return this; }
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
            public Builder dateNotificationDecisionOnem(String v) { this.dateNotificationDecisionOnem = v; return this; }
            public Builder dateDecisionDirecteur(String v) { this.dateDecisionDirecteur = v; return this; }
            public Builder recoursAdminDejaForme(Boolean v) { this.recoursAdminDejaForme = v; return this; }
            public Builder dateAccident(String v) { this.dateAccident = v; return this; }
            public Builder dateConnaissanceAccidentEmployeur(String v) { this.dateConnaissanceAccidentEmployeur = v; return this; }
            public Builder motifUrgenceDetecte(String v) { this.motifUrgenceDetecte = v; return this; }
            public Builder dateFaitGenerateurUrgence(String v) { this.dateFaitGenerateurUrgence = v; return this; }
            public Builder perilImmediatPresume(Boolean v) { this.perilImmediatPresume = v; return this; }
            public Builder procedureTravailDetectee(String v) { this.procedureTravailDetectee = v; return this; }
            public Builder dateDeclencheurProcedure(String v) { this.dateDeclencheurProcedure = v; return this; }
            // SF-246-21 — requalification_detection
            public Builder cddDureeMois(Integer v) { this.cddDureeMois = v; return this; }
            public Builder cddDateFinDernierContrat(String v) { this.cddDateFinDernierContrat = v; return this; }
            public Builder cddNouveauDateDebut(String v) { this.cddNouveauDateDebut = v; return this; }
            public Builder cddNouveauDateFin(String v) { this.cddNouveauDateFin = v; return this; }
            public Builder cddTotalSalairesBruts(Double v) { this.cddTotalSalairesBruts = v; return this; }
            public Builder interimDureeTotaleMois(Integer v) { this.interimDureeTotaleMois = v; return this; }
            public Builder interimDateFinDerniereMission(String v) { this.interimDateFinDerniereMission = v; return this; }
            public Builder interimNouvellesMissionDateDebut(String v) { this.interimNouvellesMissionDateDebut = v; return this; }
            public Builder interimNouvellesMissionDateFin(String v) { this.interimNouvellesMissionDateFin = v; return this; }
            public Builder interimEntrepriseUtilisatrice(String v) { this.interimEntrepriseUtilisatrice = v; return this; }
            public Builder interimTotalRemunerationsBrutes(Double v) { this.interimTotalRemunerationsBrutes = v; return this; }
            public Builder interimDureeMissionJours(Integer v) { this.interimDureeMissionJours = v; return this; }
            // SF-246-21 — paie_detection
            public Builder congesJoursAcquis(Integer v) { this.congesJoursAcquis = v; return this; }
            public Builder congesJoursPris(Integer v) { this.congesJoursPris = v; return this; }
            public Builder rappelSalaireMontantPerverseMensuel(Double v) { this.rappelSalaireMontantPerverseMensuel = v; return this; }
            public Builder rappelSalairePeriodeDebut(String v) { this.rappelSalairePeriodeDebut = v; return this; }
            public Builder rappelSalairePeriodeFin(String v) { this.rappelSalairePeriodeFin = v; return this; }
            // SF-246-21 — rupture_collective_detection
            public Builder salarieAgeAnnees(Integer v) { this.salarieAgeAnnees = v; return this; }
            public Builder pseNombreSalaries(Integer v) { this.pseNombreSalaries = v; return this; }
            public Builder pseNombreLicenciements(Integer v) { this.pseNombreLicenciements = v; return this; }
            public Builder transactionDateSignature(String v) { this.transactionDateSignature = v; return this; }
            public Builder transactionIndemniteMontantEur(Double v) { this.transactionIndemniteMontantEur = v; return this; }
            // SF-246-21 — sante_discrimination_detection
            public Builder atDateAccident(String v) { this.atDateAccident = v; return this; }
            public Builder atDateExposition(String v) { this.atDateExposition = v; return this; }
            public Builder areTypeDecision(String v) { this.areTypeDecision = v; return this; }
            public Builder areMontantConteste(Double v) { this.areMontantConteste = v; return this; }
            public Builder discriminationMotif(String v) { this.discriminationMotif = v; return this; }
            public Builder discriminationContexte(String v) { this.discriminationContexte = v; return this; }
            // SF-246-21 — procedure_details_detection
            public Builder refereMontantProvision(Double v) { this.refereMontantProvision = v; return this; }
            public Builder documentsDateCertificatTravail(String v) { this.documentsDateCertificatTravail = v; return this; }
            public Builder documentsDateAttestationFranceTravail(String v) { this.documentsDateAttestationFranceTravail = v; return this; }
            public Builder documentsDateSoldeToutCompte(String v) { this.documentsDateSoldeToutCompte = v; return this; }
            // SF-246-23 — travail_be_detection (BELGIQUE uniquement)
            public Builder dateConnaissanceFait(String v) { this.dateConnaissanceFait = v; return this; }
            public Builder dateNotificationMotifs(String v) { this.dateNotificationMotifs = v; return this; }
            public Builder commissionParitaireBe(String v) { this.commissionParitaireBe = v; return this; }
            public Builder joursTravaillesAnneePrecedenteBe(Integer v) { this.joursTravaillesAnneePrecedenteBe = v; return this; }
            public Builder joursPrestesBe(Integer v) { this.joursPrestesBe = v; return this; }
            public Builder dateDemandeCreditTemps(String v) { this.dateDemandeCreditTemps = v; return this; }
            // SF-206-01
            public Builder abandonPosteDateMiseEnDemeure(String v) { this.abandonPosteDateMiseEnDemeure = v; return this; }
            public Builder abandonPosteModeNotification(String v) { this.abandonPosteModeNotification = v; return this; }
            public Builder abandonPosteDelaiAccordeJours(Integer v) { this.abandonPosteDelaiAccordeJours = v; return this; }
            public Builder abandonPosteMotifAbsence(String v) { this.abandonPosteMotifAbsence = v; return this; }
            public Builder abandonPosteDateReprise(String v) { this.abandonPosteDateReprise = v; return this; }
            public Builder abandonPosteMedMentionneDelai(Boolean v) { this.abandonPosteMedMentionneDelai = v; return this; }
            public Builder abandonPosteMedMentionneConsequences(Boolean v) { this.abandonPosteMedMentionneConsequences = v; return this; }
            public Builder abandonPosteRepriseDansDelai(Boolean v) { this.abandonPosteRepriseDansDelai = v; return this; }
            // SF-206-05 — prise_acte_detail (FRANCE uniquement)
            public Builder priseActeDefautPaiementSalaire(Boolean v) { this.priseActeDefautPaiementSalaire = v; return this; }
            public Builder priseActeMontantImpayes(java.math.BigDecimal v) { this.priseActeMontantImpayes = v; return this; }
            public Builder priseActeHarcelement(Boolean v) { this.priseActeHarcelement = v; return this; }
            public Builder priseActeManquementSecurite(Boolean v) { this.priseActeManquementSecurite = v; return this; }
            public Builder priseActeModificationContrat(Boolean v) { this.priseActeModificationContrat = v; return this; }
            public Builder priseActeDeclassement(Boolean v) { this.priseActeDeclassement = v; return this; }
            public Builder priseActeDiscrimination(Boolean v) { this.priseActeDiscrimination = v; return this; }
            public Builder priseActeHeuresSupNonPayees(Boolean v) { this.priseActeHeuresSupNonPayees = v; return this; }
            public Builder priseActeNonRespectRepos(Boolean v) { this.priseActeNonRespectRepos = v; return this; }
            public Builder priseActeGriefsPersistants(Boolean v) { this.priseActeGriefsPersistants = v; return this; }
            public Builder priseActeGriefImpossiblePoursuite(Boolean v) { this.priseActeGriefImpossiblePoursuite = v; return this; }
            // SF-206-07 — resiliation_judiciaire_detail (FRANCE uniquement)
            public Builder resiliationJudDefautPaiementSalaire(Boolean v) { this.resiliationJudDefautPaiementSalaire = v; return this; }
            public Builder resiliationJudMontantImpayes(java.math.BigDecimal v) { this.resiliationJudMontantImpayes = v; return this; }
            public Builder resiliationJudHarcelement(Boolean v) { this.resiliationJudHarcelement = v; return this; }
            public Builder resiliationJudManquementSecurite(Boolean v) { this.resiliationJudManquementSecurite = v; return this; }
            public Builder resiliationJudModificationContrat(Boolean v) { this.resiliationJudModificationContrat = v; return this; }
            public Builder resiliationJudDeclassement(Boolean v) { this.resiliationJudDeclassement = v; return this; }
            public Builder resiliationJudDiscrimination(Boolean v) { this.resiliationJudDiscrimination = v; return this; }
            public Builder resiliationJudHeuresSupNonPayees(Boolean v) { this.resiliationJudHeuresSupNonPayees = v; return this; }
            public Builder resiliationJudNonRespectRepos(Boolean v) { this.resiliationJudNonRespectRepos = v; return this; }
            public Builder resiliationJudManquementsPersistants(Boolean v) { this.resiliationJudManquementsPersistants = v; return this; }
            public Builder resiliationJudSalarieEnPoste(Boolean v) { this.resiliationJudSalarieEnPoste = v; return this; }
            public Builder resiliationJudLicenciementEnCours(Boolean v) { this.resiliationJudLicenciementEnCours = v; return this; }
            // SF-207-06 — rcc_be_conditions_detection (BELGIQUE uniquement)
            public Builder dateNaissanceSalarie(String v) { this.dateNaissanceSalarie = v; return this; }
            public Builder anneesCarriereSalarie(Integer v) { this.anneesCarriereSalarie = v; return this; }
            public Builder metierLourdDetecte(Boolean v) { this.metierLourdDetecte = v; return this; }
            public Builder entrepriseEnDifficulteDetectee(Boolean v) { this.entrepriseEnDifficulteDetectee = v; return this; }
            // SF-207-07 — rcc_be_indemnite_detection (BELGIQUE uniquement)
            public Builder remunerationNetteReferenceRccDetectee(Double v) { this.remunerationNetteReferenceRccDetectee = v; return this; }
            public Builder allocationOnemMensuelleEstimee(Double v) { this.allocationOnemMensuelleEstimee = v; return this; }
            public Builder dateDebutRccEnvisagee(String v) { this.dateDebutRccEnvisagee = v; return this; }
            // SF-206-03 — conges_payes_arret_maladie_detail (FRANCE uniquement)
            public Builder cpArretMaladieType(String v) { this.cpArretMaladieType = v; return this; }
            public Builder cpArretMaladieNombreMois(Integer v) { this.cpArretMaladieNombreMois = v; return this; }
            public Builder cpArretMaladieSalarieEnPoste(Boolean v) { this.cpArretMaladieSalarieEnPoste = v; return this; }
            public Builder cpArretMaladieDateRupture(String v) { this.cpArretMaladieDateRupture = v; return this; }
            public Builder cpArretMaladieJoursDejaAccordes(java.math.BigDecimal v) { this.cpArretMaladieJoursDejaAccordes = v; return this; }
            // SF-207-08 — outplacement_be_detection (BELGIQUE uniquement)
            public Builder ancienneteSalarie(Double v) { this.ancienneteSalarie = v; return this; }
            public Builder motifLicenciementDetecte(String v) { this.motifLicenciementDetecte = v; return this; }
            public Builder offreOutplacementMentionnee(Boolean v) { this.offreOutplacementMentionnee = v; return this; }
            // SF-246-29 — rupture_periode_essai_detail (FRANCE uniquement)
            public Builder rpeCategorieSocioProfessionnelle(String v) { this.rpeCategorieSocioProfessionnelle = v; return this; }
            public Builder rpeDureeCddMois(Integer v) { this.rpeDureeCddMois = v; return this; }
            public Builder rpeDureePeriodeEssaiMois(Integer v) { this.rpeDureePeriodeEssaiMois = v; return this; }
            public Builder rpeRenouvellementInvoque(Boolean v) { this.rpeRenouvellementInvoque = v; return this; }
            public Builder rpeAccordBrancheRenouvellement(Boolean v) { this.rpeAccordBrancheRenouvellement = v; return this; }
            public Builder rpeAccordEcritSalarieRenouvellement(Boolean v) { this.rpeAccordEcritSalarieRenouvellement = v; return this; }
            public Builder rpeAuteurRupture(String v) { this.rpeAuteurRupture = v; return this; }
            public Builder rpeDelaiPrevenanceJours(Integer v) { this.rpeDelaiPrevenanceJours = v; return this; }
            public Builder rpeMotifLieCompetences(Boolean v) { this.rpeMotifLieCompetences = v; return this; }
            public Builder rpeMotifEconomique(Boolean v) { this.rpeMotifEconomique = v; return this; }
            public Builder rpeAtteinteLiberteFondamentale(String v) { this.rpeAtteinteLiberteFondamentale = v; return this; }
            public Builder rpeLettreRuptureMotivee(Boolean v) { this.rpeLettreRuptureMotivee = v; return this; }
            public Builder rpeMotifsAveresParPieces(Boolean v) { this.rpeMotifsAveresParPieces = v; return this; }
            public Builder rpeCcnPlusFavorableRespectee(Boolean v) { this.rpeCcnPlusFavorableRespectee = v; return this; }
            // SF-252-01 — 7 protections nullité additionnelles (FR uniquement)
            public Builder rpeSalarieProtege(Boolean v) { this.rpeSalarieProtege = v; return this; }
            public Builder rpeAutorisationInspectionTravail(Boolean v) { this.rpeAutorisationInspectionTravail = v; return this; }
            public Builder rpeLanceurAlerte(Boolean v) { this.rpeLanceurAlerte = v; return this; }
            public Builder rpeTemoinHarcelement(Boolean v) { this.rpeTemoinHarcelement = v; return this; }
            public Builder rpeDroitRetraitExerce(Boolean v) { this.rpeDroitRetraitExerce = v; return this; }
            public Builder rpeGrossesseDeclareePostRupture(Boolean v) { this.rpeGrossesseDeclareePostRupture = v; return this; }
            public Builder rpeDateNotificationGrossesse(String v) { this.rpeDateNotificationGrossesse = v; return this; }
            // SF-212-01 — faute_grave_detail (FRANCE uniquement)
            public Builder fauteGraveFaitsReproches(String v) { this.fauteGraveFaitsReproches = v; return this; }
            public Builder fauteGraveDatesFaits(List<String> v) { this.fauteGraveDatesFaits = v; return this; }
            public Builder fauteGraveQualificationEmployeur(String v) { this.fauteGraveQualificationEmployeur = v; return this; }
            public Builder fauteGraveIntentionNuireAlleeguee(Boolean v) { this.fauteGraveIntentionNuireAlleeguee = v; return this; }
            public Builder fauteGraveAncienneteMois(Integer v) { this.fauteGraveAncienneteMois = v; return this; }
            public Builder fauteGraveSalaireMensuelBrut(Double v) { this.fauteGraveSalaireMensuelBrut = v; return this; }
            // SF-212-03 — forfait_jours_detail (FRANCE uniquement)
            public Builder forfaitJoursAccordCollectifExiste(Boolean v) { this.forfaitJoursAccordCollectifExiste = v; return this; }
            public Builder forfaitJoursEntretienAnnuelRealise(Boolean v) { this.forfaitJoursEntretienAnnuelRealise = v; return this; }
            public Builder forfaitJoursDocumentControle(Boolean v) { this.forfaitJoursDocumentControle = v; return this; }
            public Builder forfaitJoursCategorieAutonome(Boolean v) { this.forfaitJoursCategorieAutonome = v; return this; }
            public Builder forfaitJoursNbJours(Integer v) { this.forfaitJoursNbJours = v; return this; }
            // SF-212-05 — transfert_entreprise_detail (FRANCE uniquement)
            public Builder transfertTypeTransfert(String v) { this.transfertTypeTransfert = v; return this; }
            public Builder transfertEeaIdentifiee(Boolean v) { this.transfertEeaIdentifiee = v; return this; }
            public Builder transfertActivitePreservee(Boolean v) { this.transfertActivitePreservee = v; return this; }
            public Builder transfertLicenciementsPreTransfert(Boolean v) { this.transfertLicenciementsPreTransfert = v; return this; }
            public Builder transfertDateTransfert(String v) { this.transfertDateTransfert = v; return this; }
            // F-256 — setters sous-records consolidés (SF-212-07/09/11/13/15/19/25)
            public Builder cspDetail(CspDetail v) { this.cspDetail = v; return this; }
            public Builder fauteInexcusableDetail(FauteInexcusableDetail v) { this.fauteInexcusableDetail = v; return this; }
            public Builder lanceurAlerteDetail(LanceurAlerteDetail v) { this.lanceurAlerteDetail = v; return this; }
            public Builder modifContratDetail(ModifContratDetail v) { this.modifContratDetail = v; return this; }
            public Builder mutationMobiliteDetail(MutationMobiliteDetail v) { this.mutationMobiliteDetail = v; return this; }
            public Builder teletravailDetail(TeletravailDetail v) { this.teletravailDetail = v; return this; }
            public Builder miseAPiedDetail(MiseAPiedDetail v) { this.miseAPiedDetail = v; return this; }
            // SF-212-23 — egalite_salariale_detail (FRANCE uniquement) — sous-record regroupé.
            public Builder egaliteSalarialeDetail(EgaliteSalarialeDetail v) { this.egaliteSalarialeDetail = v; return this; }
            // F-256 — 3 sous-records pour les dettes pré-fill IA
            public Builder ruptureAnticipeeCddDetail(RuptureAnticipeeCddDetail v) { this.ruptureAnticipeeCddDetail = v; return this; }
            public Builder demissionEquivoqueDetail(DemissionEquivoqueDetail v) { this.demissionEquivoqueDetail = v; return this; }
            public Builder pdvRccDetail(PdvRccDetail v) { this.pdvRccDetail = v; return this; }
            // SF-212-29 — sous-record pré-fill IA pour F-DT-77 congé maternité / paternité.
            public Builder congeMaternitePaterniteDetail(CongeMaternitePaterniteDetail v) { this.congeMaternitePaterniteDetail = v; return this; }
            // SF-212-27 — F-205 flag (FRANCE only) — déclenche F-DT-64 burn-out reconnaissance MP.
            public Builder burnoutDetecte(boolean v) { this.burnoutDetecte = v; return this; }
            // SF-212-27 — burnout_detail (FRANCE uniquement) — sous-record IA.
            public Builder burnoutDetail(BurnoutDetail v) { this.burnoutDetail = v; return this; }
            // SF-212-31 — F-205 flag (FRANCE only) — déclenche F-DT-65 élections CSE conformité.
            public Builder electionCseDetectee(boolean v) { this.electionCseDetectee = v; return this; }
            // SF-212-31 — elections_cse_detail (FRANCE uniquement) — sous-record IA.
            public Builder electionsCseDetail(ElectionsCseDetail v) { this.electionsCseDetail = v; return this; }
            // SF-212-33 — F-205 flag (FRANCE only) — déclenche F-DT-49 temps partiel — requalification.
            public Builder tempsPartielRequalificationEnvisagee(boolean v) { this.tempsPartielRequalificationEnvisagee = v; return this; }
            // SF-212-33 — temps_partiel_requalification_detail (FRANCE uniquement) — sous-record IA.
            public Builder tempsPartielRequalificationDetail(TempsPartielRequalificationDetail v) { this.tempsPartielRequalificationDetail = v; return this; }
            // SF-212-37 — F-205 flag (FRANCE only) — déclenche F-DT-84 conciliation CPH BCA.
            public Builder conciliationCphEnvisagee(boolean v) { this.conciliationCphEnvisagee = v; return this; }
            // SF-212-37 — conciliation_cph_detail (FRANCE uniquement) — sous-record IA.
            public Builder conciliationCphDetail(ConciliationCphDetail v) { this.conciliationCphDetail = v; return this; }
            // SF-218-01 — appel CPH cour d'appel (FRANCE uniquement).
            public Builder dateNotificationJugement(String v) { this.dateNotificationJugement = v; return this; }
            public Builder appelCphEnvisage(boolean v) { this.appelCphEnvisage = v; return this; }

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
                        mutationRefusee, modificationContratRefusee,
                        teletravailLitigeDetecte,
                        // SF-212-19 — F-205 flag F-DT-48 mise à pied disciplinaire.
                        miseAPiedDisciplinaireDetectee,
                        // SF-212-23 — F-205 flag F-DT-56 égalité salariale femmes/hommes.
                        egaliteSalarialePressentie,
                        // SF-212-17 — F-205 flag F-DT-43 rupture anticipée CDD.
                        ruptureAnticipeeCddDetectee,
                        // F-256 SF-212-21 — F-205 flag F-DT-41 démission équivoque.
                        demissionEquivoquePressentie,
                        // F-256 SF-212-35 — F-205 flag F-DT-46 PDV/RCC conformité.
                        pdvRccEnvisage,
                        // SF-212-29 — F-205 flag F-DT-77 congé maternité / paternité.
                        congeMaternitePaterniteDetecte,
                        fauteGraveEnvisagee,
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
                        // SF-207-03 — contestation_c4_onem_detection
                        dateNotificationDecisionOnem, dateDecisionDirecteur, recoursAdminDejaForme,
                        // SF-207-04 — at_fedris_declaration_detection
                        dateAccident, dateConnaissanceAccidentEmployeur,
                        // SF-207-05 — refere_tribunal_travail_be_detection
                        motifUrgenceDetecte, dateFaitGenerateurUrgence, perilImmediatPresume,
                        procedureTravailDetectee, dateDeclencheurProcedure,
                        // SF-246-21 — requalification_detection
                        cddDureeMois, cddDateFinDernierContrat, cddNouveauDateDebut,
                        cddNouveauDateFin, cddTotalSalairesBruts,
                        interimDureeTotaleMois, interimDateFinDerniereMission,
                        interimNouvellesMissionDateDebut, interimNouvellesMissionDateFin,
                        interimEntrepriseUtilisatrice, interimTotalRemunerationsBrutes,
                        interimDureeMissionJours,
                        // SF-246-21 — paie_detection
                        congesJoursAcquis, congesJoursPris,
                        rappelSalaireMontantPerverseMensuel, rappelSalairePeriodeDebut,
                        rappelSalairePeriodeFin,
                        // SF-246-21 — rupture_collective_detection
                        salarieAgeAnnees, pseNombreSalaries, pseNombreLicenciements,
                        transactionDateSignature, transactionIndemniteMontantEur,
                        // SF-246-21 — sante_discrimination_detection
                        atDateAccident, atDateExposition,
                        areTypeDecision, areMontantConteste,
                        discriminationMotif, discriminationContexte,
                        // SF-246-21 — procedure_details_detection
                        refereMontantProvision,
                        documentsDateCertificatTravail, documentsDateAttestationFranceTravail,
                        documentsDateSoldeToutCompte,
                        // SF-246-23 — travail_be_detection (BELGIQUE uniquement)
                        dateConnaissanceFait, dateNotificationMotifs,
                        commissionParitaireBe,
                        joursTravaillesAnneePrecedenteBe, joursPrestesBe,
                        dateDemandeCreditTemps,
                        // SF-206-01
                        abandonPosteDateMiseEnDemeure, abandonPosteModeNotification,
                        abandonPosteDelaiAccordeJours, abandonPosteMotifAbsence,
                        abandonPosteDateReprise, abandonPosteMedMentionneDelai,
                        abandonPosteMedMentionneConsequences, abandonPosteRepriseDansDelai,
                        // SF-206-05 — prise_acte_detail (FRANCE uniquement)
                        priseActeDefautPaiementSalaire, priseActeMontantImpayes,
                        priseActeHarcelement, priseActeManquementSecurite,
                        priseActeModificationContrat, priseActeDeclassement,
                        priseActeDiscrimination, priseActeHeuresSupNonPayees,
                        priseActeNonRespectRepos, priseActeGriefsPersistants,
                        priseActeGriefImpossiblePoursuite,
                        // SF-206-07 — resiliation_judiciaire_detail (FRANCE uniquement)
                        resiliationJudDefautPaiementSalaire, resiliationJudMontantImpayes,
                        resiliationJudHarcelement, resiliationJudManquementSecurite,
                        resiliationJudModificationContrat, resiliationJudDeclassement,
                        resiliationJudDiscrimination, resiliationJudHeuresSupNonPayees,
                        resiliationJudNonRespectRepos, resiliationJudManquementsPersistants,
                        resiliationJudSalarieEnPoste, resiliationJudLicenciementEnCours,
                        // SF-207-06 — rcc_be_conditions_detection (BELGIQUE uniquement)
                        dateNaissanceSalarie, anneesCarriereSalarie,
                        metierLourdDetecte, entrepriseEnDifficulteDetectee,
                        // SF-207-07 — rcc_be_indemnite_detection (BELGIQUE uniquement)
                        remunerationNetteReferenceRccDetectee,
                        allocationOnemMensuelleEstimee,
                        dateDebutRccEnvisagee,
                        // SF-206-03 — conges_payes_arret_maladie_detail (FRANCE uniquement)
                        cpArretMaladieType, cpArretMaladieNombreMois,
                        cpArretMaladieSalarieEnPoste, cpArretMaladieDateRupture,
                        cpArretMaladieJoursDejaAccordes,
                        // SF-207-08 — outplacement_be_detection (BELGIQUE uniquement)
                        ancienneteSalarie,
                        motifLicenciementDetecte,
                        offreOutplacementMentionnee,
                        // SF-246-29 — rupture_periode_essai_detail (FRANCE uniquement)
                        rpeCategorieSocioProfessionnelle, rpeDureeCddMois,
                        rpeDureePeriodeEssaiMois, rpeRenouvellementInvoque,
                        rpeAccordBrancheRenouvellement, rpeAccordEcritSalarieRenouvellement,
                        rpeAuteurRupture, rpeDelaiPrevenanceJours,
                        rpeMotifLieCompetences, rpeMotifEconomique,
                        rpeAtteinteLiberteFondamentale, rpeLettreRuptureMotivee,
                        rpeMotifsAveresParPieces, rpeCcnPlusFavorableRespectee,
                        // SF-252-01 — 7 protections nullité additionnelles (FR uniquement)
                        rpeSalarieProtege, rpeAutorisationInspectionTravail,
                        rpeLanceurAlerte, rpeTemoinHarcelement, rpeDroitRetraitExerce,
                        rpeGrossesseDeclareePostRupture, rpeDateNotificationGrossesse,
                        // SF-212-01 — faute_grave_detail (FRANCE uniquement)
                        fauteGraveFaitsReproches, fauteGraveDatesFaits,
                        fauteGraveQualificationEmployeur, fauteGraveIntentionNuireAlleeguee,
                        fauteGraveAncienneteMois, fauteGraveSalaireMensuelBrut,
                        // SF-212-03 — forfait_jours_detail (FRANCE uniquement)
                        forfaitJoursAccordCollectifExiste, forfaitJoursEntretienAnnuelRealise,
                        forfaitJoursDocumentControle, forfaitJoursCategorieAutonome,
                        forfaitJoursNbJours,
                        // SF-212-05 — transfert_entreprise_detail (FRANCE uniquement)
                        transfertTypeTransfert, transfertEeaIdentifiee,
                        transfertActivitePreservee, transfertLicenciementsPreTransfert,
                        transfertDateTransfert,
                        // F-256 — sous-records consolidés
                        cspDetail,
                        fauteInexcusableDetail,
                        lanceurAlerteDetail,
                        modifContratDetail,
                        mutationMobiliteDetail,
                        teletravailDetail,
                        miseAPiedDetail,
                        // SF-212-23 — egalite_salariale_detail
                        egaliteSalarialeDetail,
                        // F-256 — sous-records pour les 3 dettes pré-fill
                        ruptureAnticipeeCddDetail,
                        demissionEquivoqueDetail,
                        pdvRccDetail,
                        // SF-212-29 — conge_maternite_paternite_detail
                        congeMaternitePaterniteDetail,
                        // SF-212-27 — burn-out reconnaissance MP (FR)
                        burnoutDetecte,
                        burnoutDetail,
                        // SF-212-31 — élections CSE conformité (FR)
                        electionCseDetectee,
                        electionsCseDetail,
                        // SF-212-33 — temps partiel — requalification en temps plein (FR)
                        tempsPartielRequalificationEnvisagee,
                        tempsPartielRequalificationDetail,
                        // SF-212-37 — conciliation CPH BCA (FR)
                        conciliationCphEnvisagee,
                        conciliationCphDetail,
                        dateNotificationJugement,
                        appelCphEnvisage);
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
     * SF-207-05 : codes whitelistés pour {@code motif_urgence_detecte} (BE).
     * Liste alignée sur l'enum {@link fr.ailegalcase.casefile.RefereTribunalTravailBeMotifUrgence}
     * pour pré-fill direct de l'outil F-207-05 référé tribunal du travail BE.
     * Un code hors whitelist renvoyé par le LLM est ramené à {@code null} par
     * {@code normalizeEnumCode} (fail-open).
     */
    static final Set<String> REFERE_BE_MOTIF_URGENCE_CODES = Set.of(
            "HARCELEMENT", "SALAIRE_IMPAYE", "MODIFICATION_UNILATERALE", "AUTRE"
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
     * SF-206-01 : codes de mode de notification de la mise en demeure d'abandon
     * de poste — alignés sur l'enum {@code AbandonPostePresomptionDemissionCalculator.ModeNotification}
     * (FR uniquement).
     */
    static final Set<String> ABANDON_POSTE_MODE_NOTIFICATION_CODES = Set.of(
            "LRAR", "REMISE_MAIN_PROPRE", "AUTRE"
    );

    /**
     * SF-246-29 : codes de catégorie socio-professionnelle pour pré-fill F-DT-38
     * (rupture de période d'essai). Alignés sur l'enum {@code CategorieSocioProfessionnelle}
     * du front (rupture-periode-essai.model.ts) et sur l'enum interne du
     * Calculator. Détermine la durée légale max L.1221-19 CT (2 / 3 / 4 mois).
     * Un code hors liste renvoyé par le LLM est ramené à {@code null} par
     * {@code normalizeEnumCode}. FR uniquement.
     */
    static final Set<String> CATEGORIE_SOCIO_PROFESSIONNELLE_CODES = Set.of(
            "OUVRIER_EMPLOYE", "AGENT_MAITRISE_TECHNICIEN", "CADRE"
    );

    /**
     * SF-246-29 : codes d'auteur de la rupture de période d'essai pour pré-fill
     * F-DT-38. Alignés sur l'enum {@code AuteurRupture} du front. FR uniquement.
     */
    static final Set<String> AUTEUR_RUPTURE_CODES = Set.of(
            "EMPLOYEUR", "SALARIE"
    );

    /** SF-246-29 : borne haute durée période d'essai contractuelle en mois. */
    private static final int MAX_RPE_DUREE_ESSAI_MOIS = 24;

    /** SF-246-29 : borne haute durée CDD en mois. */
    private static final int MAX_RPE_DUREE_CDD_MOIS = 36;

    /** SF-246-29 : borne haute délai de prévenance en jours. */
    private static final int MAX_RPE_DELAI_PREVENANCE_JOURS = 30;

    /** SF-246-29 : longueur max du texte d'atteinte à liberté fondamentale. */
    private static final int MAX_RPE_ATTEINTE_LIBERTE_LENGTH = 500;

    /** SF-212-02 : longueur max du résumé des faits reprochés (F-DT-36). */
    static final int MAX_FAUTE_GRAVE_FAITS_REPROCHES_LENGTH = 500;

    /**
     * SF-212-02 : borne haute ancienneté en mois pour le pré-fill F-DT-36
     * (50 ans = 600 mois). Au-delà, la valeur est jugée aberrante par
     * {@code boundedIntOrNull} et ramenée à {@code null}.
     */
    static final int MAX_FAUTE_GRAVE_ANCIENNETE_MOIS = 600;

    /**
     * SF-212-02 : codes de qualification de la faute disciplinaire (F-DT-36)
     * — alignés sur l'enum
     * {@code LicenciementFauteGraveLourdCalculator.QualificationFaute} (FR
     * uniquement, L.1234-1 s. CT).
     */
    static final Set<String> QUALIFICATION_FAUTE_CODES = Set.of(
            "FAUTE_SIMPLE", "FAUTE_GRAVE", "FAUTE_LOURDE"
    );

    /**
     * SF-212-03 : borne haute du nombre de jours du forfait jours (F-DT-50).
     * 235 = plafond admis même en cas d'accord collectif majoré (L. 3121-64 CT).
     * Au-delà, la valeur est jugée aberrante par {@code boundedIntOrNull} et
     * ramenée à {@code null}.
     */
    static final int MAX_FORFAIT_JOURS_NB_JOURS = 235;

    /**
     * SF-212-05 : codes de type de transfert d'entreprise (F-DT-72) — alignés
     * sur l'enum {@code TransfertEntrepriseL12241Calculator.TypeTransfert}
     * (FR uniquement, L. 1224-1 CT ; Directive 2001/23/CE).
     */
    static final Set<String> TYPE_TRANSFERT_CODES = Set.of(
            "CESSION", "FUSION", "APPORT_PARTIEL_ACTIF",
            "EXTERNALISATION", "REPRISE_ACTIVITE", "AUTRE"
    );

    /**
     * SF-212-07 : borne haute de l'effectif d'entreprise (F-DT-44 CSP/CRP).
     */
    static final int MAX_CSP_EFFECTIF_ENTREPRISE = 100_000;

    /**
     * SF-212-09 : borne haute du taux d'IPP (F-DT-91 faute inexcusable).
     */
    static final int MAX_IPP_TAUX = 100;

    /**
     * F-256 SF-212-17 : codes d'auteur de rupture anticipée du CDD (F-DT-43)
     * — alignés sur l'enum {@code RacAuteurRupture} du frontend.
     */
    static final Set<String> RAC_AUTEUR_CODES = Set.of("EMPLOYEUR", "SALARIE");

    /**
     * F-256 SF-212-17 : codes de motif de rupture anticipée du CDD (F-DT-43)
     * — alignés sur l'enum {@code RacMotifRupture} du frontend et le prompt PART9.
     */
    static final Set<String> RAC_MOTIF_CODES = Set.of(
            "ACCORD_PARTIES", "FAUTE_GRAVE", "FORCE_MAJEURE",
            "INAPTITUDE", "CDI_EMBAUCHE", "AUTRE");

    /**
     * F-256 SF-212-35 : codes de type de dispositif PDV / RCC (F-DT-46)
     * — alignés sur l'enum {@code PdvRccTypeDispositif} du frontend et le prompt PART12.
     */
    static final Set<String> PDV_RCC_TYPE_DISPOSITIF_CODES = Set.of("RCC", "PDV");

    /**
     * SF-214-41 : codes de motif de retrait de titre pour fraude (F-IM-45)
     * — alignés sur l'enum {@code RetraitTitreFraudeMotifEnum} et le prompt
     * IMMIGRATION PART3. Un code hors whitelist renvoyé par le LLM est ramené
     * à {@code null} par {@code normalizeEnumCode()}.
     */
    static final Set<String> RETRAIT_TITRE_FRAUDE_MOTIF_CODES =
            Set.of("MARIAGE_GRIS", "FAUSSES_DECLARATIONS", "FRAUDE_DOCUMENTAIRE", "PERTE_CONDITIONS");

    /**
     * SF-212-29 : codes de type de congé maternité / paternité (F-DT-77)
     * — alignés sur l'enum {@code CongeMaternitePaterniteInput.TypeConge} et
     * le prompt PART14. Un code hors whitelist renvoyé par le LLM est ramené
     * à {@code null} par {@code normalizeEnumCode}.
     */
    static final Set<String> CONGE_MAT_PAT_TYPE_CODES = Set.of("MATERNITE", "PATERNITE");

    /**
     * SF-206-01 : codes de motif d'absence invoqué par le salarié en cas
     * d'abandon de poste — alignés sur l'enum
     * {@code AbandonPostePresomptionDemissionCalculator.MotifAbsence} (FR uniquement).
     */
    static final Set<String> ABANDON_POSTE_MOTIF_ABSENCE_CODES = Set.of(
            "AUCUN", "MEDICAL", "DROIT_RETRAIT", "DROIT_GREVE",
            "MODIFICATION_CONTRAT_REFUSEE", "DEFAUT_PAIEMENT_SALAIRE", "AUTRE"
    );

    /**
     * SF-206-03 : codes de type d'arrêt maladie pour F-DT-75 (congés payés
     * acquis pendant arrêt maladie) — alignés sur l'enum
     * {@code CongesPayesArretMaladieCalculator.TypeArret} (FR uniquement).
     */
    static final Set<String> CP_ARRET_MALADIE_TYPE_CODES = Set.of(
            "MALADIE_NON_PROFESSIONNELLE", "ACCIDENT_TRAVAIL_MALADIE_PRO"
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

    // SF-246-21 : bornes et whitelists pour les 5 sous-objets thématiques.

    /** Durée CDD/intérim maximale plausible en mois (10 ans). */
    static final int MAX_DUREE_CDD_INTERIM_MOIS = 120;

    /** Durée de mission intérim maximale en jours (~10 ans). */
    static final int MAX_DUREE_MISSION_JOURS = 3650;

    /** Longueur max entreprise utilisatrice intérim (≤ 200 car.). */
    static final int MAX_INTERIM_ENTREPRISE_LENGTH = 200;

    /** Jours de congés payés — borne haute plausible (hors forfait). */
    static final int MAX_CONGES_JOURS = 50;

    // SF-246-23 — bornes pour les champs travail_be_detection.
    /** Jours de travail/prestés BE — borne haute plausible (≤ 365 j/an). */
    static final int MAX_JOURS_TRAVAIL_BE = 365;
    /** Longueur max commission paritaire BE (code court ex. "CP 200.01"). */
    static final int MAX_COMMISSION_PARITAIRE_BE_LENGTH = 20;

    /**
     * SF-207-06 : nombre d'années de carrière professionnelle salariée
     * maximal plausible (BE — RCC). Toute valeur hors [0, 60] est jugée
     * aberrante par {@code boundedIntOrNull} et ramenée à {@code null}.
     */
    static final int MAX_CARRIERE_RCC_BE = 60;

    /** Âge maximal plausible du salarié (lic-éco). */
    static final int MAX_SALARIE_AGE_ANNEES = 80;

    /** Âge minimal légal du salarié. */
    static final int MIN_SALARIE_AGE_ANNEES = 16;

    /** Effectifs PSE maximal plausible. */
    static final int MAX_PSE_NOMBRE = 100_000;

    /**
     * SF-246-21 : codes whitelistés pour `are_type_decision`
     * (enum frontend {@code TypeDecisionContestee}).
     */
    static final Set<String> ARE_TYPE_DECISION_CODES = Set.of(
            "REFUS_INSCRIPTION", "RADIATION", "SUPPRESSION_ARE",
            "REDUCTION_ARE", "EXCLUSION_TEMPORAIRE", "AUTRE");

    /**
     * SF-246-21 : codes whitelistés pour `discrimination_motif`
     * (enum frontend {@code MotifDiscrimination}).
     */
    static final Set<String> DISCRIMINATION_MOTIF_CODES = Set.of(
            "SEXE", "AGE", "ORIGINE", "HANDICAP", "RELIGION",
            "ORIENTATION_SEXUELLE", "GROSSESSE", "ACTIVITES_SYNDICALES", "AUTRE");

    /**
     * SF-246-21 : codes whitelistés pour `discrimination_contexte`
     * (enum frontend {@code ContexteActe}).
     */
    static final Set<String> DISCRIMINATION_CONTEXTE_CODES = Set.of(
            "REFUS_EMBAUCHE", "LICENCIEMENT", "MUTATION", "SANCTION_DISCIPLINAIRE",
            "PROMOTION_REFUSEE", "REMUNERATION_INFERIEURE", "HARCELEMENT", "AUTRE");

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
            // SF-214-11 : flag pivot DÉRIVÉ pour F-IM-30 AES calcul présence prouvée (FR).
            // NON extrait par le pipeline IA : dérivé par OR des 4 flags AES ci-dessus
            // (aesMetiersTension / aesFamilial / aesHumanitaire / aesEtudiant) dans le
            // builder. Exposé pour cohérence API ; la visibilité de l'outil est calculée
            // par DecisionToolVisibilityService (même OR sur le JSON immigration_extracted_data).
            boolean aesCalculPresenceDeclenche,
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
            /** SF-214-19 : true si la demande d'asile a été examinée en procédure accélérée
             *  (pays sûr, L. 531-24 — délai de recours CNDA réduit à 15 j). Pré-fill F-IM-34. */
            boolean asileProcedureeAccelereee,
            /** Durée de présence irrégulière en France en mois entiers (IRTF art. L.612-6+). */
            Integer eloiDureePresenceIrreguliereMois,
            /**
             * Motif de menace pour l'ordre public / sécurité.
             * Whitelist 5 codes : ORDRE_PUBLIC / SECURITE_ETAT / TERRORISME / RECIDIVE_GRAVE / AUTRE.
             */
            String eloiMotifMenace,
            // SF-246-20 : pré-fill lot Immigration BE (9bis / 9ter / 40bis / 40ter)
            /** Date d'entrée en Belgique ISO YYYY-MM-DD non-future (Annexe 26 / passeport) — art. 9bis. */
            String be9bisDateEntreeBelgique,
            /** Durée de présence en Belgique en mois entiers calculée depuis be9bisDateEntreeBelgique. */
            Integer be9bisDureePresenceMois,
            /** Date de début des symptômes ISO YYYY-MM-DD non-future (certificat médical) — art. 9ter. */
            String be9terDateDebutSymptomes,
            /** Lien familial art. 40bis — whitelist 5 codes LIENS_FAMILIAUX_40BIS_CODES. */
            String be40bisLienFamilial,
            /** Lien familial art. 40ter — whitelist 5 codes LIENS_FAMILIAUX_40TER_CODES (distinct de 40bis). */
            String be40terLienFamilial,
            /** Revenus mensuels nets du regroupant belge (> 0, ≤ MAX_BE_REVENUS_MENSUELS_NETS). */
            Integer be40terRevenusMensuelsNets,
            // === SF-214-01 — F-IM-25 Étranger malade L. 425-9 CESEDA (FRANCE UNIQUEMENT, null pour BE) ===
            /** true si mentions "maladie grave", "traitement indisponible", "OFII médical", "L.425-9" dans les pièces. */
            boolean etrangerMaladeDetecte,
            /** Pathologie principale extraite des certificats médicaux ou pièces médicales (texte libre ≤ 500 car.). */
            String etrangerMaladePathologie,
            /** true si les pièces indiquent que le traitement est disponible dans le pays d'origine. */
            Boolean etrangerMaladeTraitementDisponible,
            /** Avis du collège médical OFII : FAVORABLE | DEFAVORABLE | EN_ATTENTE (null si non rendu). */
            String etrangerMaladeAvisOFII,
            /** Date de l'avis OFII au format YYYY-MM-DD (non future). Null si non rendu ou non lisible. */
            String etrangerMalaDateAvisOFII,
            // === SF-215-05 — F-IM-27 Regroupement 10bis BE (BELGIQUE UNIQUEMENT, null pour FR) ===
            /**
             * SF-215-05 : true si les pièces évoquent un regroupement familial 10bis
             * (regroupant tiers en séjour LIMITÉ — carte A), false sinon. Pivot pour la
             * visibility rule CONTEXTUAL (trigger {@code regroupement_10bis_detecte}).
             * Dossiers FR : toujours false.
             */
            boolean regroupementTiersLimiteDetecte,
            /** SF-215-05 : lien familial 10bis — whitelist {@link #LIENS_FAMILIAUX_10BIS_CODES} (5 codes). */
            String be10bisLienFamilial,
            /** SF-215-05 : revenus mensuels nets du regroupant 10bis (> 0, ≤ MAX_BE_REVENUS_MENSUELS_NETS). */
            Integer be10bisRevenusMensuels,
            /** SF-215-05 : durée de séjour ininterrompu en mois (≥ 0, ≤ 600). */
            Integer be10bisDureeSejour,
            /** SF-215-05 : date d'expiration de la carte A au format YYYY-MM-DD (la date peut être future ou passée). */
            String be10bisDateFinCarteA,
            // === SF-215-07 — F-IM-28 Naturalisation 12bis BE (BELGIQUE UNIQUEMENT, null/false pour FR) ===
            /**
             * SF-215-07 : true si les pièces évoquent une démarche de déclaration de
             * nationalité belge art. 12bis CNB (loi 28/06/1984), false sinon. Pivot
             * pour la visibility rule CONTEXTUAL (trigger {@code naturalisation_be_envisagee}).
             * Partageable avec SF-215-09 (naturalisation conjoint Belge art. 16 CNB).
             * Dossiers FR : toujours false.
             */
            boolean naturalisationBeEnvisagee,
            /** SF-215-07 : durée de séjour ininterrompu en mois (≥ 0, ≤ 600). Null si non extractible ou dossier FR. */
            Integer naturalisationBeDureeSejour,
            /** SF-215-07 : type de séjour — whitelist LIMITE/ILLIMITE. Null si non extractible ou dossier FR. */
            String naturalisationBeTypeSejour,
            /** SF-215-07 : niveau de langue — whitelist INFERIEUR_A2/A2/SUPERIEUR_A2. Null si non extractible ou dossier FR. */
            String naturalisationBeNiveauLangue,
            // === SF-215-09 — F-IM-29 Naturalisation conjoint Belge BE art. 16 CNB (BELGIQUE UNIQUEMENT, null pour FR) ===
            // Pas de nouveau flag pivot — `naturalisationBeEnvisagee` (SF-215-07) est partagé entre les deux
            // voies de naturalisation BE (12bis et conjoint Belge art. 16). Le panel F-IA-04 propose les deux
            // outils dès que le flag est levé, l'avocat choisit la voie pertinente selon le profil.
            /** SF-215-09 : date du mariage avec un(e) Belge au format YYYY-MM-DD (non future) — art. 16 §1 CNB. Null si non extractible ou dossier FR. */
            String naturalisationBeArt16DateMarriage,
            /** SF-215-09 : durée de cohabitation ininterrompue en mois (≥ 0, ≤ 600) — art. 16 §1 2° CNB. Null si non extractible ou dossier FR. */
            Integer naturalisationBeArt16DureeCohabitation,
            /** SF-215-09 : niveau de langue — whitelist INFERIEUR_A2/A2/SUPERIEUR_A2. Null si non extractible ou dossier FR. */
            String naturalisationBeArt16NiveauLangue,
            // === SF-215-11 — F-IM-30 AESM + tutelle MENA BE (BELGIQUE UNIQUEMENT, null/false pour FR) ===
            /**
             * SF-215-11 : true si les pièces évoquent un mineur étranger non accompagné
             * sur le territoire belge (MENA — mention « mineur non accompagné »,
             * « MENA », « tuteur DGDE », « Service des Tutelles SPF Justice », « AESM »,
             * « certificat de scolarité MENA », passeport sans accompagnant, etc.).
             * Pivot pour la visibility rule CONTEXTUAL de F-IM-30 (trigger
             * {@code mineur_non_accompagne_be_detecte}). Distinct de
             * {@code clientMineurDetecte} (F-201 — mineurs FR L.435-3 CESEDA).
             * Dossiers FR : toujours false.
             */
            boolean mineurNonAccompagneBeDetecte,
            /** SF-215-11 : âge du mineur en années entières (0–17, gate strict mineur). Null si non extractible ou dossier FR. */
            Integer menaAge,
            /** SF-215-11 : date d'arrivée en Belgique du MENA au format YYYY-MM-DD (non future). Null si non extractible ou dossier FR. */
            String menaDateArrivee,
            /** SF-215-11 : durée de scolarité continue en mois (0–120). Null si non extractible ou dossier FR. */
            Integer menaDureeScolaire,
            // === SF-215-13 — F-IM-31 Recours CCE annulation 30j BE (BELGIQUE UNIQUEMENT, null/false pour FR) ===
            /**
             * SF-215-13 : true si les pièces évoquent un recours en annulation envisagé
             * devant le Conseil du Contentieux des Étrangers (CCE) — décision OE/CGRA
             * notifiée, recours art. 39/82, etc. Pivot pour la visibility rule CONTEXTUAL
             * de F-IM-31 (trigger {@code recours_cce_envisage}). Dossiers FR : toujours false.
             */
            boolean recoursCceEnvisage,
            /** SF-215-13 : date de notification de la décision OE/CGRA YYYY-MM-DD (non future). Null si non extractible ou dossier FR. */
            String recoursCceDateNotification,
            /** SF-215-13 : type de décision contestée — whitelist {@link #CCE_TYPE_DECISION_CODES} (7 codes). Null si non extractible ou dossier FR. */
            String recoursCceTypeDecision,
            // === SF-215-15 — F-IM-32 Recours CCE extrême urgence 5j ouvrables BE (BELGIQUE UNIQUEMENT, null pour FR) ===
            /** SF-215-15 : date de l'acte exécutoire imminent (OQT exécutoire, transfert Dublin) YYYY-MM-DD. Null si non extractible ou dossier FR. */
            String recoursExtremeUrgenceDateActe,
            /** SF-215-15 : type d'acte exécutoire — whitelist {@link #CCE_EXTREME_URGENCE_TYPE_ACTE_CODES} (5 codes). Null si non extractible ou dossier FR. */
            String recoursExtremeUrgenceTypeActe,
            // === SF-215-17 — F-IM-33 Annexe 13quinquies OQT + interdiction d'entrée art. 74/11 BE (BELGIQUE UNIQUEMENT, null pour FR) ===
            /** SF-215-17 : date de notification de l'Annexe 13quinquies (OQT + interdiction d'entrée) YYYY-MM-DD (non future). Null si non extractible ou dossier FR. */
            String interdictionEntreeDateNotification,
            /** SF-215-17 : motif de l'interdiction d'entrée — whitelist {@link #INTERDICTION_ENTREE_MOTIF_CODES} (5 codes). Null si non extractible ou dossier FR. */
            String interdictionEntreeMotif,
            // === SF-215-19 — F-IM-34 Protection temporaire Ukraine BE (BELGIQUE UNIQUEMENT, null pour FR) ===
            /** SF-215-19 : date d'arrivée en Belgique / première demande de protection temporaire YYYY-MM-DD (non future). Null si non extractible ou dossier FR. */
            String ptUkraineDateArrivee,
            /** SF-215-19 : true si le bénéficiaire est de nationalité ukrainienne. Null si non extractible ou dossier FR. */
            Boolean ptUkraineNationalite,
            // === SF-214-03 — F-IM-26 Regroupement familial L. 434-1+ CESEDA (FRANCE UNIQUEMENT, null/false pour BE) ===
            /**
             * SF-214-03 : true si les pièces évoquent un regroupement familial envisagé
             * (mentions « regroupement familial », « OFII », « membre de famille »,
             * « rejoindre en France », « visa long séjour famille »). Pivot pour la
             * visibility rule CONTEXTUAL de F-IM-26 (trigger {@code regroupement_familial_envisage}).
             * Dossiers BE : toujours false.
             */
            boolean regroupementFamilialEnvisage,
            /** SF-214-03 : ressources mensuelles nettes du regroupant en € (> 0, hors APL/RSA/alloc). Null si non extractible ou dossier BE. */
            Double regroupementRessourcesMensuelles,
            /** SF-214-03 : type de regroupement — whitelist CONJOINT/ENFANT_MINEUR/AUTRE. Null si non extractible ou dossier BE. */
            String regroupementType,
            // === SF-214-05 — F-IM-27 VPF liens personnels et familiaux L. 423-23 CESEDA (FRANCE UNIQUEMENT, null/false pour BE) ===
            /**
             * SF-214-05 : true si les pièces évoquent une demande de titre « vie privée
             * et familiale » L. 423-23 CESEDA (mentions « vie privée et familiale »,
             * « L.423-23 », « 7° », « liens personnels et familiaux en France »,
             * « atteinte disproportionnée », « article 8 CEDH », « ancienneté de résidence »).
             * Pivot pour la visibility rule CONTEXTUAL de F-IM-27 (trigger
             * {@code vie_privee_familiale_detectee}). Dossiers BE : toujours false.
             */
            boolean viePriveeFamilialeDetectee,
            /** SF-214-05 : niveau d'intégration du requérant — whitelist FORT/MOYEN/FAIBLE. Null si non extractible ou dossier BE. */
            String vpfNiveauIntegration,
            // === SF-214-07 — F-IM-28 Validation VLS-TS OFII 3 mois R. 311-3 CESEDA (FRANCE UNIQUEMENT, null pour BE) ===
            /**
             * SF-214-07 : true si les pièces indiquent que la validation du VLS-TS auprès
             * de l'OFII a été effectuée (mentions « VLS-TS validé », « validation OFII »,
             * « vignette OFII », « taxe OFII acquittée », confirmation ANEF de validation),
             * false sinon. La date d'entrée est réutilisée via {@code aesDateEntreeFrance}
             * et le type de VLS-TS via {@code typeTitreSejourCode} (proxy). Null si non
             * extractible ou dossier BE.
             */
            Boolean vlsTsValidationOFIIEffectuee,
            // === SF-214-15 — F-IM-32 Récépissé vs attestation de prolongation R. 311-4 / R. 311-6 CESEDA (FRANCE UNIQUEMENT, null/false pour BE) ===
            /**
             * SF-214-15 : flag pivot — true si les pièces évoquent un titre de séjour
             * en cours de recouvrement / renouvellement avec document transitoire
             * (mentions « récépissé », « attestation de prolongation », « en cours de
             * renouvellement », « en attente de décision »). Pivot pour la visibility
             * rule CONTEXTUAL de F-IM-32 (trigger {@code recouvrement_titre_en_cours}).
             * Dossiers BE : toujours false.
             */
            boolean recouvrementTitreEnCours,
            /**
             * SF-214-15 : type du document transitoire — whitelist
             * RECEPISSE/ATTESTATION_PROLONGATION/INCONNU. Null si non extractible ou
             * dossier BE. La date d'expiration est réutilisée via {@code dateExpirationTitre}.
             */
            String recepisseOuAttestationType,
            // === SF-214-17 — F-IM-33 Demande OFPRA introduction GUDA/ADA R. 521-1+ CESEDA (FRANCE UNIQUEMENT, false pour BE) ===
            /**
             * SF-214-17 : true si les pièces indiquent que le passage au GUDA (guichet
             * unique demande d'asile) a déjà été effectué (mentions « GUDA », « guichet
             * unique », « passage préfecture asile », « enregistrement de la demande
             * d'asile », attestation de demande d'asile), false sinon. La date d'arrivée
             * en France est réutilisée via {@code aesDateEntreeFrance}. L'outil reste
             * visible via le trigger {@code procedure_asile_detectee} (F-201). Dossiers
             * BE : toujours false.
             */
            boolean gudaPassageEffectue,
            // === SF-214-21 — F-IM-35 Victime de la traite des êtres humains L. 425-1 CESEDA (FRANCE UNIQUEMENT, false/null pour BE) ===
            /**
             * SF-214-21 : flag pivot — true si les pièces évoquent une situation de
             * traite des êtres humains (mentions « traite des êtres humains », « TEH »,
             * « prostitution forcée », « exploitation », « OCRTEH », « victime de
             * traite », « servitude »). Pivot pour la visibility rule CONTEXTUAL de
             * F-IM-35 (trigger {@code victime_traite_detectee}). Dossiers BE : toujours false.
             */
            boolean victimeTraiteDetectee,
            /**
             * SF-214-21 : true si une plainte (ou un signalement par une association
             * agréée) a été déposée contre l'auteur des faits de traite, false sinon,
             * null si non extractible ou dossier BE.
             */
            Boolean tehPlainteDeposee,
            /**
             * SF-214-21 : date de dépôt de plainte au format YYYY-MM-DD. Null si non
             * extractible ou dossier BE.
             */
            String tehDatePlainte,
            // === SF-214-23 — F-IM-36 Carte de résident 10 ans L. 426-1 CESEDA (FRANCE UNIQUEMENT, null/false pour BE) ===
            /**
             * SF-214-23 : flag pivot — true si les pièces évoquent une carte de
             * résident 10 ans envisagée (mentions « carte de résident », « L.426-1 »,
             * « séjour de 10 ans », « titre 10 ans », « résidence permanente », durée
             * de présence ≥ 5 ans avec titre VPF en cours). Pivot pour la visibility
             * rule CONTEXTUAL de F-IM-36 (trigger {@code carte_resident_envisagee}).
             * Dossiers BE : toujours false.
             */
            boolean carteResidentEnvisagee,
            /**
             * SF-214-23 : ressources mensuelles nettes du demandeur en € (> 0). Null
             * si non extractible ou dossier BE. La durée de séjour est dérivée de
             * {@code aesDureePresenceMois} déjà extrait — non redemandée ici.
             */
            Double carteResidentRessources,
            // === SF-214-25 — F-IM-37 ANEF procédure / pannes / recours (FRANCE UNIQUEMENT, false pour BE) ===
            /**
             * SF-214-25 : flag pivot — true si les pièces évoquent une panne ou une
             * impossibilité de dépôt sur la plateforme ANEF (mentions « ANEF en
             * panne », « site indisponible », « connexion impossible », « erreur
             * ANEF », « plateforme étrangers »). Pivot pour la visibility rule
             * CONTEXTUAL de F-IM-37 (trigger {@code anef_panne_detectee}). Le type
             * de titre et la date d'expiration sont dérivés de {@code typeTitreSejour}
             * et {@code dateExpirationTitre} déjà extraits. Dossiers BE : toujours false.
             */
            boolean anefPanneDetectee,
            /**
             * SF-214-27 : flag de pré-fill — true si les pièces évoquent un refus
             * d'évaluation / de prise en charge par l'ASE d'un mineur non accompagné
             * (mentions « évaluation ASE », « refus de prise en charge », « juge des
             * enfants », « MNA », « mineur non accompagné »). Sert au pré-remplissage
             * de l'outil F-IM-38 (MNA évaluation d'âge) ; la visibilité reste pilotée
             * par le flag existant {@code clientMineurDetecte} (F-201). Dossiers BE :
             * toujours false.
             */
            boolean mnaEvaluationRefusee,
            /**
             * SF-214-27 : flag de pré-fill — true si les pièces évoquent qu'un examen
             * osseux a été ordonné dans le cadre de l'évaluation d'âge d'un mineur non
             * accompagné (mentions « examen osseux », « test osseux », « radiographie
             * du poignet », « détermination de l'âge »). Sert au pré-remplissage de
             * l'outil F-IM-38. Dossiers BE : toujours false.
             */
            boolean mnaExamenOsseuxOrdonne,
            // === SF-214-29 — F-IM-39 Recours TJ refus déclaration de nationalité Cciv 26-3 (FRANCE UNIQUEMENT, null pour BE) ===
            /**
             * SF-214-29 : voie de déclaration de nationalité française visée par le refus —
             * whitelist {@code MARIAGE / ASCENDANT / MINEUR_22_1}. Sert au pré-remplissage de
             * l'outil F-IM-39 (recours TJ) ; la visibilité reste pilotée par le flag existant
             * {@code naturalisationEnvisageeDetectee} (F-201). Null si non extractible ou dossier BE.
             */
            String naturalisationVoie,
            /**
             * SF-214-29 : date du refus de déclaration de nationalité au format YYYY-MM-DD
             * (non future) — point de départ du délai de recours de 6 mois (Cciv 26-3). Null
             * si non extractible ou dossier BE.
             */
            String naturalisationDateRefus,
            // === SF-214-33 — F-IM-41 Appel CAA / cassation CE délais (FRANCE UNIQUEMENT, false/null pour BE) ===
            /**
             * SF-214-33 : flag pivot — true si les pièces évoquent qu'un recours en
             * appel devant la CAA ou un pourvoi en cassation devant le CE est envisagé
             * après un jugement du tribunal administratif en contentieux des étrangers
             * (mentions « jugement », « décision TA », « appel », « CAA », « pourvoi en
             * cassation », « CE »). Pivot pour la visibility rule CONTEXTUAL de F-IM-41
             * (trigger {@code recours_envisage_detecte}). Dossiers BE : toujours false.
             */
            boolean recoursEnvisageDetecte,
            /**
             * SF-214-33 : date du jugement du tribunal administratif au format
             * YYYY-MM-DD (non future) — point de départ du délai d'appel devant la CAA.
             * Sert au pré-remplissage de l'outil F-IM-41. Null si non extractible ou dossier BE.
             */
            String recoursDateJugementTA,
            // === SF-214-35 — F-IM-42 Assignation à résidence L. 731-1 (FRANCE UNIQUEMENT, false/null pour BE) ===
            /**
             * SF-214-35 : flag pivot — true si les pièces évoquent une mesure
             * d'assignation à résidence (alternative à la rétention administrative)
             * notifiée à l'étranger (mentions « assignation à résidence », « L.731-1 »,
             * « pointage gendarmerie », « obligation présentation », « assigné à
             * résidence »). Pivot pour la visibility rule CONTEXTUAL de F-IM-42
             * (trigger {@code assignation_residence_detectee}). Dossiers BE : toujours false.
             */
            boolean assignationResidenceDetectee,
            /**
             * SF-214-35 : date de notification de l'assignation à résidence au format
             * YYYY-MM-DD (non future) — point de départ de la durée d'assignation.
             * Sert au pré-remplissage de l'outil F-IM-42. Null si non extractible ou dossier BE.
             */
            String assignationDateNotification,
            // === SF-214-37 — F-IM-43 ITF judiciaire (peine complémentaire C. pén. 131-30, FRANCE UNIQUEMENT, null pour BE) ===
            /**
             * SF-214-37 : date de la condamnation pénale ayant prononcé l'interdiction
             * du territoire français (ITF) au format YYYY-MM-DD (non future) — point de
             * départ du délai de relèvement (5 ans, C. pén. 131-30-1). Sert au
             * pré-remplissage de l'outil F-IM-43. Null si non extractible ou dossier BE.
             * La visibilité de l'outil réutilise le flag pivot {@code mesureEloignementDetectee}
             * (l'ITF est une mesure d'éloignement d'origine pénale, trigger partagé avec F-IM-20).
             */
            String itfJudiciaireDateCondamnation,
            /**
             * SF-214-37 : durée de l'ITF prononcée, en années entières (> 0). Sert au
             * pré-remplissage de l'outil F-IM-43. Null si non extractible ou dossier BE.
             */
            Integer itfJudiciaireDureeAnnees,
            // === SF-214-41 — F-IM-45 Retrait de titre pour fraude L. 412-7 (FRANCE UNIQUEMENT, false/null pour BE) ===
            /**
             * SF-214-41 : flag pivot — true si les pièces évoquent une décision de
             * retrait de titre de séjour pour fraude (mentions « retrait de titre »,
             * « fraude », « mariage blanc », « mariage gris », « fausses déclarations »,
             * « L.412-7 »). Pivot pour la visibility rule CONTEXTUAL de F-IM-45
             * (trigger {@code retrait_titre_fraude_detecte}). Dossiers BE : toujours false.
             */
            boolean retraitTitreFraudeDetecte,
            /**
             * SF-214-41 : date de la décision de retrait au format YYYY-MM-DD (non
             * future) — point de départ du délai de recours de 2 mois devant le TA.
             * Sert au pré-remplissage de l'outil F-IM-45. Null si non extractible ou dossier BE.
             */
            String retraitTitreDateRetrait,
            /**
             * SF-214-41 : motif du retrait — whitelist {@link #RETRAIT_TITRE_FRAUDE_MOTIF_CODES}
             * (MARIAGE_GRIS / FAUSSES_DECLARATIONS / FRAUDE_DOCUMENTAIRE / PERTE_CONDITIONS).
             * Sert au pré-remplissage de l'outil F-IM-45. Null si non extractible ou dossier BE.
             */
            String retraitTitreMotif) {

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
                    // SF-214-11 : aesCalculPresenceDeclenche est dérivé dans build() — pas recopié ici.
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
                    .asileProcedureeAccelereee(asileProcedureeAccelereee)
                    .eloiDureePresenceIrreguliereMois(eloiDureePresenceIrreguliereMois)
                    .eloiMotifMenace(eloiMotifMenace)
                    // SF-246-20 : lot Immigration BE
                    .be9bisDateEntreeBelgique(be9bisDateEntreeBelgique)
                    .be9bisDureePresenceMois(be9bisDureePresenceMois)
                    .be9terDateDebutSymptomes(be9terDateDebutSymptomes)
                    .be40bisLienFamilial(be40bisLienFamilial)
                    .be40terLienFamilial(be40terLienFamilial)
                    .be40terRevenusMensuelsNets(be40terRevenusMensuelsNets)
                    // SF-214-01 : F-IM-25 Étranger malade L.425-9
                    .etrangerMaladeDetecte(etrangerMaladeDetecte)
                    .etrangerMaladePathologie(etrangerMaladePathologie)
                    .etrangerMaladeTraitementDisponible(etrangerMaladeTraitementDisponible)
                    .etrangerMaladeAvisOFII(etrangerMaladeAvisOFII)
                    .etrangerMalaDateAvisOFII(etrangerMalaDateAvisOFII)
                    // SF-215-05 : F-IM-27 Regroupement 10bis BE
                    .regroupementTiersLimiteDetecte(regroupementTiersLimiteDetecte)
                    .be10bisLienFamilial(be10bisLienFamilial)
                    .be10bisRevenusMensuels(be10bisRevenusMensuels)
                    .be10bisDureeSejour(be10bisDureeSejour)
                    .be10bisDateFinCarteA(be10bisDateFinCarteA)
                    // SF-215-07 : F-IM-28 Naturalisation 12bis BE
                    .naturalisationBeEnvisagee(naturalisationBeEnvisagee)
                    .naturalisationBeDureeSejour(naturalisationBeDureeSejour)
                    .naturalisationBeTypeSejour(naturalisationBeTypeSejour)
                    .naturalisationBeNiveauLangue(naturalisationBeNiveauLangue)
                    // SF-215-09 : F-IM-29 Naturalisation conjoint Belge BE (art. 16 CNB)
                    .naturalisationBeArt16DateMarriage(naturalisationBeArt16DateMarriage)
                    .naturalisationBeArt16DureeCohabitation(naturalisationBeArt16DureeCohabitation)
                    .naturalisationBeArt16NiveauLangue(naturalisationBeArt16NiveauLangue)
                    // SF-215-11 : F-IM-30 AESM + tutelle MENA BE
                    .mineurNonAccompagneBeDetecte(mineurNonAccompagneBeDetecte)
                    .menaAge(menaAge)
                    .menaDateArrivee(menaDateArrivee)
                    .menaDureeScolaire(menaDureeScolaire)
                    // SF-215-13 : F-IM-31 Recours CCE annulation 30j BE
                    .recoursCceEnvisage(recoursCceEnvisage)
                    .recoursCceDateNotification(recoursCceDateNotification)
                    .recoursCceTypeDecision(recoursCceTypeDecision)
                    // SF-215-15 : F-IM-32 Recours CCE extrême urgence 5j ouvrables BE
                    .recoursExtremeUrgenceDateActe(recoursExtremeUrgenceDateActe)
                    .recoursExtremeUrgenceTypeActe(recoursExtremeUrgenceTypeActe)
                    .interdictionEntreeDateNotification(interdictionEntreeDateNotification)
                    .interdictionEntreeMotif(interdictionEntreeMotif)
                    // SF-215-19 : F-IM-34 Protection temporaire Ukraine BE
                    .ptUkraineDateArrivee(ptUkraineDateArrivee)
                    .ptUkraineNationalite(ptUkraineNationalite)
                    .regroupementFamilialEnvisage(regroupementFamilialEnvisage)
                    .regroupementRessourcesMensuelles(regroupementRessourcesMensuelles)
                    .regroupementType(regroupementType)
                    // SF-214-35 : F-IM-42 Assignation à résidence L. 731-1 FR
                    .assignationResidenceDetectee(assignationResidenceDetectee)
                    .assignationDateNotification(assignationDateNotification)
                    // SF-214-37 : F-IM-43 ITF judiciaire (peine complémentaire C. pén. 131-30) FR
                    .itfJudiciaireDateCondamnation(itfJudiciaireDateCondamnation)
                    .itfJudiciaireDureeAnnees(itfJudiciaireDureeAnnees)
                    // SF-214-41 : F-IM-45 Retrait de titre pour fraude L. 412-7 FR
                    .retraitTitreFraudeDetecte(retraitTitreFraudeDetecte)
                    .retraitTitreDateRetrait(retraitTitreDateRetrait)
                    .retraitTitreMotif(retraitTitreMotif);
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
            private boolean asileProcedureeAccelereee;
            private Integer eloiDureePresenceIrreguliereMois;
            private String eloiMotifMenace;
            // SF-246-20 : lot Immigration BE
            private String be9bisDateEntreeBelgique;
            private Integer be9bisDureePresenceMois;
            private String be9terDateDebutSymptomes;
            private String be40bisLienFamilial;
            private String be40terLienFamilial;
            private Integer be40terRevenusMensuelsNets;
            // SF-214-01 : F-IM-25 Étranger malade L.425-9
            private boolean etrangerMaladeDetecte;
            private String etrangerMaladePathologie;
            private Boolean etrangerMaladeTraitementDisponible;
            private String etrangerMaladeAvisOFII;
            private String etrangerMalaDateAvisOFII;
            // SF-215-05 : F-IM-27 Regroupement 10bis BE
            private boolean regroupementTiersLimiteDetecte;
            private String be10bisLienFamilial;
            private Integer be10bisRevenusMensuels;
            private Integer be10bisDureeSejour;
            private String be10bisDateFinCarteA;
            // SF-215-07 : F-IM-28 Naturalisation 12bis BE
            private boolean naturalisationBeEnvisagee;
            private Integer naturalisationBeDureeSejour;
            private String naturalisationBeTypeSejour;
            private String naturalisationBeNiveauLangue;
            // SF-215-09 : F-IM-29 Naturalisation conjoint Belge BE (art. 16 CNB)
            private String naturalisationBeArt16DateMarriage;
            private Integer naturalisationBeArt16DureeCohabitation;
            private String naturalisationBeArt16NiveauLangue;
            // SF-215-11 : F-IM-30 AESM + tutelle MENA BE
            private boolean mineurNonAccompagneBeDetecte;
            private Integer menaAge;
            private String menaDateArrivee;
            private Integer menaDureeScolaire;
            // SF-215-13 : F-IM-31 Recours CCE annulation 30j BE
            private boolean recoursCceEnvisage;
            private String recoursCceDateNotification;
            private String recoursCceTypeDecision;
            // SF-215-15 : F-IM-32 Recours CCE extrême urgence 5j ouvrables BE
            private String recoursExtremeUrgenceDateActe;
            private String recoursExtremeUrgenceTypeActe;
            private String interdictionEntreeDateNotification;
            private String interdictionEntreeMotif;
            // SF-215-19 : F-IM-34 Protection temporaire Ukraine BE
            private String ptUkraineDateArrivee;
            private Boolean ptUkraineNationalite;
            private boolean regroupementFamilialEnvisage;
            private Double regroupementRessourcesMensuelles;
            private String regroupementType;
            // SF-214-05 : F-IM-27 VPF liens personnels L. 423-23 FR
            private boolean viePriveeFamilialeDetectee;
            private String vpfNiveauIntegration;
            // SF-214-07 : F-IM-28 validation VLS-TS OFII 3 mois R. 311-3 FR
            private Boolean vlsTsValidationOFIIEffectuee;
            // SF-214-15 : F-IM-32 récépissé vs attestation de prolongation R. 311-4/R. 311-6 FR
            private boolean recouvrementTitreEnCours;
            private String recepisseOuAttestationType;
            // SF-214-17 : F-IM-33 demande OFPRA introduction GUDA/ADA R. 521-1+ FR
            private boolean gudaPassageEffectue;
            // SF-214-21 : F-IM-35 victime de la traite des êtres humains L. 425-1 FR
            private boolean victimeTraiteDetectee;
            private Boolean tehPlainteDeposee;
            private String tehDatePlainte;
            // SF-214-23 : F-IM-36 carte de résident 10 ans L. 426-1 FR
            private boolean carteResidentEnvisagee;
            private Double carteResidentRessources;
            // SF-214-25 : F-IM-37 ANEF procédure / pannes / recours FR
            private boolean anefPanneDetectee;
            // SF-214-27 : F-IM-38 MNA évaluation d'âge FR
            private boolean mnaEvaluationRefusee;
            private boolean mnaExamenOsseuxOrdonne;
            // SF-214-29 : F-IM-39 recours TJ refus déclaration de nationalité FR
            private String naturalisationVoie;
            private String naturalisationDateRefus;
            private boolean recoursEnvisageDetecte;
            private String recoursDateJugementTA;
            // SF-214-35 : F-IM-42 Assignation à résidence L. 731-1 FR
            private boolean assignationResidenceDetectee;
            private String assignationDateNotification;
            // SF-214-37 : F-IM-43 ITF judiciaire FR
            private String itfJudiciaireDateCondamnation;
            private Integer itfJudiciaireDureeAnnees;
            // SF-214-41 : F-IM-45 Retrait de titre pour fraude L. 412-7 FR
            private boolean retraitTitreFraudeDetecte;
            private String retraitTitreDateRetrait;
            private String retraitTitreMotif;

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
            public Builder asileProcedureeAccelereee(boolean v) { this.asileProcedureeAccelereee = v; return this; }
            public Builder eloiDureePresenceIrreguliereMois(Integer v) { this.eloiDureePresenceIrreguliereMois = v; return this; }
            public Builder eloiMotifMenace(String v) { this.eloiMotifMenace = v; return this; }
            // SF-246-20 : lot Immigration BE
            public Builder be9bisDateEntreeBelgique(String v) { this.be9bisDateEntreeBelgique = v; return this; }
            public Builder be9bisDureePresenceMois(Integer v) { this.be9bisDureePresenceMois = v; return this; }
            public Builder be9terDateDebutSymptomes(String v) { this.be9terDateDebutSymptomes = v; return this; }
            public Builder be40bisLienFamilial(String v) { this.be40bisLienFamilial = v; return this; }
            public Builder be40terLienFamilial(String v) { this.be40terLienFamilial = v; return this; }
            public Builder be40terRevenusMensuelsNets(Integer v) { this.be40terRevenusMensuelsNets = v; return this; }
            // SF-214-01 : F-IM-25 Étranger malade L.425-9
            public Builder etrangerMaladeDetecte(boolean v) { this.etrangerMaladeDetecte = v; return this; }
            public Builder etrangerMaladePathologie(String v) { this.etrangerMaladePathologie = v; return this; }
            public Builder etrangerMaladeTraitementDisponible(Boolean v) { this.etrangerMaladeTraitementDisponible = v; return this; }
            public Builder etrangerMaladeAvisOFII(String v) { this.etrangerMaladeAvisOFII = v; return this; }
            public Builder etrangerMalaDateAvisOFII(String v) { this.etrangerMalaDateAvisOFII = v; return this; }
            // SF-215-05 : F-IM-27 Regroupement 10bis BE
            public Builder regroupementTiersLimiteDetecte(boolean v) { this.regroupementTiersLimiteDetecte = v; return this; }
            public Builder be10bisLienFamilial(String v) { this.be10bisLienFamilial = v; return this; }
            public Builder be10bisRevenusMensuels(Integer v) { this.be10bisRevenusMensuels = v; return this; }
            public Builder be10bisDureeSejour(Integer v) { this.be10bisDureeSejour = v; return this; }
            public Builder be10bisDateFinCarteA(String v) { this.be10bisDateFinCarteA = v; return this; }
            // SF-215-07 : F-IM-28 Naturalisation 12bis BE
            public Builder naturalisationBeEnvisagee(boolean v) { this.naturalisationBeEnvisagee = v; return this; }
            public Builder naturalisationBeDureeSejour(Integer v) { this.naturalisationBeDureeSejour = v; return this; }
            public Builder naturalisationBeTypeSejour(String v) { this.naturalisationBeTypeSejour = v; return this; }
            public Builder naturalisationBeNiveauLangue(String v) { this.naturalisationBeNiveauLangue = v; return this; }
            // SF-215-09 : F-IM-29 Naturalisation conjoint Belge BE (art. 16 CNB)
            public Builder naturalisationBeArt16DateMarriage(String v) { this.naturalisationBeArt16DateMarriage = v; return this; }
            public Builder naturalisationBeArt16DureeCohabitation(Integer v) { this.naturalisationBeArt16DureeCohabitation = v; return this; }
            public Builder naturalisationBeArt16NiveauLangue(String v) { this.naturalisationBeArt16NiveauLangue = v; return this; }
            // SF-215-11 : F-IM-30 AESM + tutelle MENA BE
            public Builder mineurNonAccompagneBeDetecte(boolean v) { this.mineurNonAccompagneBeDetecte = v; return this; }
            public Builder menaAge(Integer v) { this.menaAge = v; return this; }
            public Builder menaDateArrivee(String v) { this.menaDateArrivee = v; return this; }
            public Builder menaDureeScolaire(Integer v) { this.menaDureeScolaire = v; return this; }
            // SF-215-13 : F-IM-31 Recours CCE annulation 30j BE
            public Builder recoursCceEnvisage(boolean v) { this.recoursCceEnvisage = v; return this; }
            public Builder recoursCceDateNotification(String v) { this.recoursCceDateNotification = v; return this; }
            public Builder recoursCceTypeDecision(String v) { this.recoursCceTypeDecision = v; return this; }
            // SF-215-15 : F-IM-32 Recours CCE extrême urgence 5j ouvrables BE
            public Builder recoursExtremeUrgenceDateActe(String v) { this.recoursExtremeUrgenceDateActe = v; return this; }
            public Builder recoursExtremeUrgenceTypeActe(String v) { this.recoursExtremeUrgenceTypeActe = v; return this; }
            public Builder interdictionEntreeDateNotification(String v) { this.interdictionEntreeDateNotification = v; return this; }
            public Builder interdictionEntreeMotif(String v) { this.interdictionEntreeMotif = v; return this; }
            // SF-215-19 : F-IM-34 Protection temporaire Ukraine BE
            public Builder ptUkraineDateArrivee(String v) { this.ptUkraineDateArrivee = v; return this; }
            public Builder ptUkraineNationalite(Boolean v) { this.ptUkraineNationalite = v; return this; }
            public Builder regroupementFamilialEnvisage(boolean v) { this.regroupementFamilialEnvisage = v; return this; }
            public Builder regroupementRessourcesMensuelles(Double v) { this.regroupementRessourcesMensuelles = v; return this; }
            public Builder regroupementType(String v) { this.regroupementType = v; return this; }
            public Builder viePriveeFamilialeDetectee(boolean v) { this.viePriveeFamilialeDetectee = v; return this; }
            public Builder vpfNiveauIntegration(String v) { this.vpfNiveauIntegration = v; return this; }
            public Builder vlsTsValidationOFIIEffectuee(Boolean v) { this.vlsTsValidationOFIIEffectuee = v; return this; }
            public Builder recouvrementTitreEnCours(boolean v) { this.recouvrementTitreEnCours = v; return this; }
            public Builder recepisseOuAttestationType(String v) { this.recepisseOuAttestationType = v; return this; }
            public Builder gudaPassageEffectue(boolean v) { this.gudaPassageEffectue = v; return this; }
            public Builder victimeTraiteDetectee(boolean v) { this.victimeTraiteDetectee = v; return this; }
            public Builder tehPlainteDeposee(Boolean v) { this.tehPlainteDeposee = v; return this; }
            public Builder tehDatePlainte(String v) { this.tehDatePlainte = v; return this; }
            public Builder carteResidentEnvisagee(boolean v) { this.carteResidentEnvisagee = v; return this; }
            public Builder carteResidentRessources(Double v) { this.carteResidentRessources = v; return this; }
            public Builder anefPanneDetectee(boolean v) { this.anefPanneDetectee = v; return this; }
            public Builder mnaEvaluationRefusee(boolean v) { this.mnaEvaluationRefusee = v; return this; }
            public Builder mnaExamenOsseuxOrdonne(boolean v) { this.mnaExamenOsseuxOrdonne = v; return this; }
            public Builder naturalisationVoie(String v) { this.naturalisationVoie = v; return this; }
            public Builder naturalisationDateRefus(String v) { this.naturalisationDateRefus = v; return this; }
            public Builder recoursEnvisageDetecte(boolean v) { this.recoursEnvisageDetecte = v; return this; }
            public Builder recoursDateJugementTA(String v) { this.recoursDateJugementTA = v; return this; }
            public Builder assignationResidenceDetectee(boolean v) { this.assignationResidenceDetectee = v; return this; }
            public Builder assignationDateNotification(String v) { this.assignationDateNotification = v; return this; }
            public Builder itfJudiciaireDateCondamnation(String v) { this.itfJudiciaireDateCondamnation = v; return this; }
            public Builder itfJudiciaireDureeAnnees(Integer v) { this.itfJudiciaireDureeAnnees = v; return this; }
            public Builder retraitTitreFraudeDetecte(boolean v) { this.retraitTitreFraudeDetecte = v; return this; }
            public Builder retraitTitreDateRetrait(String v) { this.retraitTitreDateRetrait = v; return this; }
            public Builder retraitTitreMotif(String v) { this.retraitTitreMotif = v; return this; }

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
                        // SF-214-11 : flag pivot dérivé — OR des 4 flags AES.
                        (aesMetiersTensionEligibleDetecte || aesFamilialEligibleDetecte
                                || aesHumanitaireEligibleDetecte || aesEtudiantEligibleDetecte),
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
                        asileDateDecisionAnterieure, asileProcedureeAccelereee,
                        eloiDureePresenceIrreguliereMois,
                        eloiMotifMenace,
                        be9bisDateEntreeBelgique, be9bisDureePresenceMois,
                        be9terDateDebutSymptomes,
                        be40bisLienFamilial, be40terLienFamilial,
                        be40terRevenusMensuelsNets,
                        // SF-214-01 : F-IM-25 Étranger malade L.425-9
                        etrangerMaladeDetecte, etrangerMaladePathologie,
                        etrangerMaladeTraitementDisponible, etrangerMaladeAvisOFII,
                        etrangerMalaDateAvisOFII,
                        // SF-215-05 : F-IM-27 Regroupement 10bis BE
                        regroupementTiersLimiteDetecte, be10bisLienFamilial,
                        be10bisRevenusMensuels, be10bisDureeSejour,
                        be10bisDateFinCarteA,
                        // SF-215-07 : F-IM-28 Naturalisation 12bis BE
                        naturalisationBeEnvisagee, naturalisationBeDureeSejour,
                        naturalisationBeTypeSejour, naturalisationBeNiveauLangue,
                        // SF-215-09 : F-IM-29 Naturalisation conjoint Belge BE (art. 16 CNB)
                        naturalisationBeArt16DateMarriage,
                        naturalisationBeArt16DureeCohabitation,
                        naturalisationBeArt16NiveauLangue,
                        // SF-215-11 : F-IM-30 AESM + tutelle MENA BE
                        mineurNonAccompagneBeDetecte,
                        menaAge,
                        menaDateArrivee,
                        menaDureeScolaire,
                        // SF-215-13 : F-IM-31 Recours CCE annulation 30j BE
                        recoursCceEnvisage,
                        recoursCceDateNotification,
                        recoursCceTypeDecision,
                        // SF-215-15 : F-IM-32 Recours CCE extrême urgence 5j ouvrables BE
                        recoursExtremeUrgenceDateActe,
                        recoursExtremeUrgenceTypeActe,
                        interdictionEntreeDateNotification,
                        interdictionEntreeMotif,
                        // SF-215-19 : F-IM-34 Protection temporaire Ukraine BE
                        ptUkraineDateArrivee,
                        ptUkraineNationalite,
                        // SF-214-03 : F-IM-26 Regroupement familial FR
                        regroupementFamilialEnvisage,
                        regroupementRessourcesMensuelles,
                        regroupementType,
                        // SF-214-05 : F-IM-27 VPF liens personnels L. 423-23 FR
                        viePriveeFamilialeDetectee,
                        vpfNiveauIntegration,
                        vlsTsValidationOFIIEffectuee,
                        // SF-214-15 : F-IM-32 récépissé vs attestation de prolongation FR
                        recouvrementTitreEnCours,
                        recepisseOuAttestationType,
                        // SF-214-17 : F-IM-33 demande OFPRA introduction GUDA/ADA FR
                        gudaPassageEffectue,
                        // SF-214-21 : F-IM-35 victime de la traite des êtres humains L. 425-1 FR
                        victimeTraiteDetectee,
                        tehPlainteDeposee,
                        tehDatePlainte,
                        // SF-214-23 : F-IM-36 carte de résident 10 ans L. 426-1 FR
                        carteResidentEnvisagee,
                        carteResidentRessources,
                        // SF-214-25 : F-IM-37 ANEF procédure / pannes / recours FR
                        anefPanneDetectee,
                        // SF-214-27 : F-IM-38 MNA évaluation d'âge FR
                        mnaEvaluationRefusee,
                        mnaExamenOsseuxOrdonne,
                        // SF-214-29 : F-IM-39 recours TJ refus déclaration de nationalité FR
                        naturalisationVoie,
                        naturalisationDateRefus,
                        // SF-214-33 : F-IM-41 appel CAA / cassation CE délais FR
                        recoursEnvisageDetecte,
                        recoursDateJugementTA,
                        assignationResidenceDetectee,
                        assignationDateNotification,
                        // SF-214-37 : F-IM-43 ITF judiciaire FR
                        itfJudiciaireDateCondamnation,
                        itfJudiciaireDureeAnnees,
                        // SF-214-41 : F-IM-45 Retrait de titre pour fraude L. 412-7 FR
                        retraitTitreFraudeDetecte,
                        retraitTitreDateRetrait,
                        retraitTitreMotif);
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

    /**
     * SF-214-29 : voies de déclaration de nationalité française pour le pré-fill de
     * l'outil F-IM-39 (recours TJ Cciv 26-3). Aligné sur l'enum
     * {@code NaturalisationRecoursTjVoieEnum} backend.
     */
    static final Set<String> NATURALISATION_VOIE_CODES = Set.of(
            "MARIAGE", "ASCENDANT", "MINEUR_22_1"
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

    /**
     * SF-246-20 : lien familial art. 40bis — 5 codes distincts du 40ter.
     * Alignés sur l'enum {@code LienFamilial} du frontend (belgian-40bis.model.ts).
     */
    static final Set<String> LIENS_FAMILIAUX_40BIS_CODES = Set.of(
            "CONJOINT", "PARTENAIRE_ENREGISTRE",
            "DESCENDANT_MINEUR", "DESCENDANT_MAJEUR_CHARGE", "ASCENDANT_CHARGE"
    );

    /**
     * SF-246-20 : lien familial art. 40ter — 5 codes distincts du 40bis.
     * Alignés sur l'enum {@code LienFamilial} du frontend (belgian-40ter.model.ts).
     * Codes différents : PARTENAIRE_LEGAL_ENREGISTRE (pas PARTENAIRE_ENREGISTRE) +
     * ASCENDANT_CHARGE_HANDICAP (pas ASCENDANT_CHARGE).
     */
    static final Set<String> LIENS_FAMILIAUX_40TER_CODES = Set.of(
            "CONJOINT", "PARTENAIRE_LEGAL_ENREGISTRE",
            "DESCENDANT_MINEUR", "DESCENDANT_MAJEUR_CHARGE", "ASCENDANT_CHARGE_HANDICAP"
    );

    /**
     * SF-215-05 : lien familial art. 10bis — 5 codes alignés sur l'enum Java
     * {@code Regroupement10bisBeLienFamilialEnum}. Identiques au 10ter Java mais
     * distincts des whitelists 40bis/40ter ci-dessus (qui utilisent
     * DESCENDANT_MINEUR / DESCENDANT_MAJEUR_CHARGE).
     */
    static final Set<String> LIENS_FAMILIAUX_10BIS_CODES = Set.of(
            "CONJOINT", "PARTENAIRE_ENREGISTRE",
            "ENFANT_MOINS_21", "ENFANT_21_PLUS_CHARGE", "ASCENDANT_CHARGE"
    );

    /**
     * SF-215-07 : type de séjour du demandeur naturalisation 12bis BE — 2 codes alignés
     * sur l'enum Java {@code NaturalisationBeTypeSejourEnum}.
     */
    static final Set<String> NATURALISATION_BE_TYPE_SEJOUR_CODES = Set.of(
            "LIMITE", "ILLIMITE"
    );

    /**
     * SF-215-07 : niveau de langue du demandeur naturalisation 12bis BE — 3 codes alignés
     * sur l'enum Java {@code NaturalisationBeNiveauLangueEnum}.
     */
    static final Set<String> NATURALISATION_BE_NIVEAU_LANGUE_CODES = Set.of(
            "INFERIEUR_A2", "A2", "SUPERIEUR_A2"
    );

    /**
     * SF-215-13 : type de décision OE/CGRA contestée par le recours en annulation CCE —
     * 7 codes alignés sur l'enum Java {@code CceAnnulationBeTypeDecisionEnum}.
     */
    static final Set<String> CCE_TYPE_DECISION_CODES = Set.of(
            "REFUS_TITRE", "REFUS_REGROUPEMENT", "REFUS_9BIS", "REFUS_9TER",
            "OQT_ANNEXE13", "DECISION_CGRA", "AUTRE"
    );

    /** SF-215-15 : whitelist (5 valeurs) du type d'acte exécutoire pour F-IM-32 (recours CCE extrême urgence). */
    static final Set<String> CCE_EXTREME_URGENCE_TYPE_ACTE_CODES = Set.of(
            "OQT_EXECUTE", "TRANSFERT_DUBLIN", "REFUS_ACCES_TERRITOIRE",
            "EXPULSION_IMMEDIATE", "AUTRE"
    );

    /**
     * SF-215-17 : whitelist (5 valeurs) du motif de l'interdiction d'entrée pour
     * F-IM-33 (Annexe 13quinquies, art. 74/11) — alignée sur l'enum Java
     * {@code Annexe13quinquiesBeMotifEnum}.
     */
    static final Set<String> INTERDICTION_ENTREE_MOTIF_CODES = Set.of(
            "SEJOUR_IRREGULIER", "MENACE_ORDRE_PUBLIC", "RAISONS_SECURITE_NATIONALE",
            "ATTEINTE_INTERET_UE", "DECISION_JUDICIAIRE"
    );

    /** SF-246-20 : borne max revenus mensuels nets regroupant belge (plausibilité). */
    static final int MAX_BE_REVENUS_MENSUELS_NETS = 30_000;

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
            java.util.List<String> fautesDetectees,
            // SF-246-11 : date de naissance du demandeur pour pré-fill F-FA-26
            // (changement d'état civil — art. 60 / 61-1 / 61-5 Cciv ; loi 2022-301).
            // Famille FR uniquement, nullable — le prompt impose null hors FR / hors certitude.
            // Sous-objet IA source : `famille_extracted_data.changement_etat_civil_detection`.
            // Distinct des dates de naissance d'enfant (SF-246-09 — acteurs différents) et de
            // la date de la requête (fait juridique différent).
            String dateNaissanceDemandeurDetectee,
            // SF-246-24 : 15 champs booléens/énumérés `*Detected` pour résorber la dette D2
            // du lot Famille FR successions/libéralités (7 outils F-FA-24).
            // Source : `famille_extracted_data.succession_detection_v2`.
            // Famille FR uniquement, tous nullables — le prompt impose null hors FR / hors certitude.
            // --- acceptation-renonciation (F-FA-24) ---
            String qualiteHeritierDetectee,           // 'PREMIER_RANG' | 'SECOND_RANG'
            Boolean actesEquivalentAcceptationDejaPosesDetected,
            Boolean dettesIncertainesDetected,
            // --- reserve-heriditaire (F-FA-24) ---
            Boolean conjointSurvivantDetected,
            String qualiteDuDemandeurReserveDetecte,  // 'HERITIER_RESERVATAIRE_DESCENDANT' | 'CONJOINT_SURVIVANT'
            // --- rapport-succession (F-FA-24) ---
            String qualiteHeritierRapportDetectee,    // 'DESCENDANT' | 'CONJOINT_SURVIVANT'
            Boolean donationDispenseDeRapportDetected,
            Boolean naturePresumeeNonRapportableDetected,
            // --- devolution-legale (F-FA-24) —— conjointSurvivantDetected mutualisé ci-dessus ---
            Boolean tousDescendantsCommunsAvecConjointDetected,
            // --- donation (F-FA-24) ---
            String formeDonationDetectee,             // 'NOTARIEE' | 'MANUELLE' | 'INDIRECTE' | 'DEGUISEE'
            Boolean saineDEspritDonateurDetected,
            Boolean respectQuotiteDisponibleDetected,
            // --- testament-validite (F-FA-24) ---
            String formeTestamentDetectee,            // 'OLOGRAPHE' | 'AUTHENTIQUE' | 'MYSTIQUE'
            Boolean saineDEspritTestateurDetected,
            Boolean legsExcedeQuotiteDisponibleDetected,
            // SF-246-25 : 17 champs booléens/énumérés `*Detected` pour résorber la dette D2
            // du lot Famille FR régimes matrimoniaux, vie commune & protection
            // (8 outils : communaute-universelle, partage-judiciaire, ordonnance-protection,
            //  mesures-provisoires, revisions-post-divorce, pacs-dissolution, separation-corps,
            //  indivision).
            // Source : `famille_extracted_data.communaute_partage_protection_detection_v2`.
            // Famille FR uniquement, tous nullables — le prompt impose null hors FR / hors certitude.
            // --- communaute-universelle (F-FA-16) ---
            Boolean contratNotarieDetected,           // art. 1526+ Cciv
            Boolean enfantsNonCommunsDetected,        // art. 1527 al. 2 Cciv
            Boolean clauseAttributionIntegraleDetected,  // art. 1527 Cciv
            // --- partage-judiciaire (F-FA-17) ---
            Boolean pvDifficultesEtablisDetected,     // art. 1366 CPC
            Boolean tentativeAmiableEpuiseueeDetected,  // défaut d'intérêt à agir si non épuisé
            // --- ordonnance-protection / indivision (F-FA-14 + F-FA-22) ---
            java.util.List<String> violencesAllegueesDetectees,  // whitelist PHYSIQUES|PSYCHOLOGIQUES|SEXUELLES|ECONOMIQUES|MENACES_MORT
            java.util.List<String> preuvesViolencesDetectees,    // whitelist CONSTAT_HUISSIER|MAIN_COURANTE|CERTIFICAT_MEDICAL|TEMOIGNAGES|PHOTOS|PLAINTE_DEPOSEE|JUGEMENT_CORRECTIONNEL|AUTRE
            Boolean dangerImmediatDetected,           // péril immédiat (art. 515-10 Cciv)
            Boolean presenceEnfantsDetected,          // mineurs sous la garde commune
            Boolean logementCommunDetected,           // cohabitation effective au moment de la requête
            Boolean victimeFinanciairementDependanteDetected,  // dépendance économique (art. 515-9 Cciv)
            // --- pacs-dissolution (F-FA-20) ---
            String modeDissolutionPacsDetecte,        // 'DECLARATION_UNILATERALE'|'DECLARATION_CONJOINTE'|'MARIAGE_PARTENAIRES'|'MARIAGE_TIERS'|'DECES'
            String regimeBiensPacsDetecte,            // 'SEPARATION_BIENS'|'INDIVISION_AMENAGEE'|'INDIVISION_PAR_DEFAUT'
            java.util.List<String> creancesAllegueesDetectees,  // whitelist CONTRIBUTION_DESEQUILIBRE|INVESTISSEMENT_BIEN_PROPRE|ENRICHISSEMENT_INJUSTE|PRESTATION_TRAVAIL_NON_REMUNEREE|AUCUNE
            Boolean patrimoineCommunSignificatifDetecte,  // patrimoine commun existant et significatif
            // --- separation-corps (F-FA-21) ---
            Boolean patrimoineCommun,                 // régime communautaire (bool) — art. 1400+ Cciv
            // --- mesures-provisoires (F-FA-12) ---
            Boolean violencesAlleguees,               // violences alléguées (flag global bool)
            // SF-246-26 : sous-objet `filiation_detection_v2` — 12 champs D2 filiation / adoption.
            // --- contestation-paternite (F-FA-18-04) ---
            String qualiteAagirContestationDetected,  // whitelist PERE_DECLARE|PERE_BIOLOGIQUE_PRESUME|MERE|ENFANT_MAJEUR
            Boolean possessionEtatConforme5AnsDetected,
            Boolean expertiseAdnDemandeeDetected,
            Boolean motifsSerieuxDetected,
            // --- recherche-paternite (F-FA-18-06) ---
            String qualiteDuDemandeurRechercheDetected, // whitelist ENFANT_MAJEUR|REPRESENTANT_LEGAL_MINEUR|MERE
            Boolean presomptionPossessionEtatRechercheDetected,
            Boolean expertiseAdnDemandeeRechercheDetected,
            Boolean pereDesigneRefuseADNDetected,
            Boolean motifsSerieuxRechercheDetected,
            // --- adoption (F-FA-18-10) ---
            String formeAdoptionDemandeeDetected,     // whitelist PLENIERE|SIMPLE
            Boolean pupilleEtatDetected,
            Boolean adoptantMarieDetected,              // adoptant marié (art. 343 Cciv)
            // SF-246-27 : 8 champs IA protection majeurs / PMA / médiation / divorce (F-FA protection & divorce).
            String regimeProtectionMajeursDetected,   // whitelist SAUVEGARDE_JUSTICE|HABILITATION_FAMILIALE|CURATELLE_SIMPLE|CURATELLE_RENFORCEE|TUTELLE|MANDAT_PROTECTION_FUTURE
            String dateCertificatMedicalMajeursDetected, // ISO YYYY-MM-DD
            String datePmaDetected,                   // ISO YYYY-MM-DD (PMA — art. L2141-2 CSP)
            String dateReconnaissanceAnterieurePmaDetected, // ISO YYYY-MM-DD (reconnaissance antérieure PMA)
            String dateDonGametesDetected,            // ISO YYYY-MM-DD (don de gamètes — art. L2143-3 CSP)
            String motifSaisineMediationDetected,     // whitelist AUTORITE_PARENTALE|CONTRIBUTION_ENTRETIEN|DROIT_VISITE|RESIDENCE|AUTRE
            String dateAssignationDivorce,            // ISO YYYY-MM-DD — partagé divorce-accepte + divorce-alteration (FR)
            String dateAudienceHomologationDcBe,      // ISO YYYY-MM-DD — BELGIQUE UNIQUEMENT divorce-dc-be
            // SF-246-12 : date de séparation effective pour pré-fill divorce-desunion-be
            // (Famille BELGIQUE UNIQUEMENT, nullable). Distincte de dateSeparation FR (SF-246-08).
            // Source : `famille_extracted_data.divorce_ddi_be_detection.date_separation_be`.
            // CJ art. 1255 — date de la cessation effective de la vie commune entre les époux.
            String dateSeparationBe,                  // ISO YYYY-MM-DD — BELGIQUE UNIQUEMENT
            // SF-246-28 : 16 champs IA Famille BE — levée PREFILL_COUNT_ALWAYS_ZERO.
            // Sous-objet IA source : `famille_extracted_data.famille_be_detection_v2`.
            // BELGIQUE UNIQUEMENT — tous nullables, prompt impose null hors BE / hors certitude.
            // --- autorite-parentale-be (F-217-SF-217-05) ---
            String modeHebergementPrincipalBeDetecte,  // BELGIQUE UNIQUEMENT — whitelist HEBERGEMENT_EGALITAIRE|HEBERGEMENT_PRINCIPAL_UN_PARENT|HEBERGEMENT_NON_FIXE
            // --- contribution-alimentaire-enfants-be (F-217-SF-217-07) ---
            Integer nombreEnfantsBeDetecte,            // BELGIQUE UNIQUEMENT — [1, 12]
            Double revenuMensuelParent1BeDetecte,      // BELGIQUE UNIQUEMENT — ≥ 0 €/mois (fiche de paie)
            Double revenuMensuelParent2BeDetecte,      // BELGIQUE UNIQUEMENT — ≥ 0 €/mois (fiche de paie)
            Double allocationsFamilialesMensuellesBeDetectees,  // BELGIQUE UNIQUEMENT — ≥ 0 €/mois (FAMIFED)
            Integer nuitsHebergementParent1BeDetectees, // BELGIQUE UNIQUEMENT — [0, 30] nuits/mois
            Integer nuitsHebergementParent2BeDetectees, // BELGIQUE UNIQUEMENT — [0, 30] nuits/mois
            // --- contribution-conjoint-be (F-217-SF-217-09) ---
            Integer dureeMariageAnneesBeDetectee,      // BELGIQUE UNIQUEMENT — [0, 80] ans (acte mariage)
            Double revenuMensuelCreancierBeDetecte,    // BELGIQUE UNIQUEMENT — ≥ 0 €/mois (art. 301 § 3 CC BE)
            Double revenuMensuelDebiteurBeDetecte,     // BELGIQUE UNIQUEMENT — ≥ 0 €/mois (art. 301 § 3 CC BE)
            // --- liquidation-partage-be (F-217-SF-217-03) ---
            String dateDesignationNotaireBeDetectee,   // BELGIQUE UNIQUEMENT — ISO YYYY-MM-DD (CJ art. 1207)
            String dateOuvertureOperationsBeDetectee,  // BELGIQUE UNIQUEMENT — ISO YYYY-MM-DD
            String dateNotificationProjetBeDetectee,   // BELGIQUE UNIQUEMENT — ISO YYYY-MM-DD (CJ art. 1218 — délai contredits)
            String dateHomologationBeDetectee,         // BELGIQUE UNIQUEMENT — ISO YYYY-MM-DD
            // --- regime-communaute-legale-be (F-217-SF-217-03) ---
            String dateMariageBeDetectee,              // BELGIQUE UNIQUEMENT — ISO YYYY-MM-DD (acte mariage BE)
            Boolean contratMariageSigneBeDetecte,      // BELGIQUE UNIQUEMENT — contrat notarié (CC art. 1.2.59+ BE)
            // SF-216-01 : 6 champs IA prestation compensatoire + vie commune FR.
            // Source : `famille_extracted_data.vie_commune_detection` (4 champs FR)
            //        + `famille_extracted_data.prestation_compensatoire_detection` (1 flag).
            // FRANCE UNIQUEMENT — prompt impose null hors FR / hors certitude.
            // `dureeMariageAnnees` (FR) ≠ `dureeMariageAnneesBeDetectee` (BE).
            // `revenusAnnuelsEpoux1/2` ≠ `revenusAnnuelsEpoux` (SF-246-08 générique).
            Integer dureeMariageAnnees,                // FR — [0, 80] ans (acte mariage)
            Double revenusAnnuelsEpoux1,               // FR — >= 0 EUR/an (epoux 1, fiche de paie)
            Double revenusAnnuelsEpoux2,               // FR — >= 0 EUR/an (epoux 2, fiche de paie)
            Integer ageEpoux1Annees,                   // FR — [0, 120] ans (piece d'identite)
            Integer ageEpoux2Annees,                   // FR — [0, 120] ans (piece d'identite)
            Boolean prestationCompensatoireEnvisagee,  // FR — CONTEXTUAL : mention art. 270 / disparite niveaux de vie
            // SF-216-05 : 3 champs IA liquidation communaute legale FR.
            // Source : `famille_extracted_data.liquidation_communaute_detection`.
            // FRANCE UNIQUEMENT — prompt impose null hors FR / hors certitude / hors regime COMMUNAUTE_LEGALE.
            Boolean liquidationCommunauteEnvisagee,    // FR — CONTEXTUAL : mention art. 1467 / partage actif commun
            Integer recompensesEpoux1Eur,              // FR — >= 0 EUR (recompenses dues par epoux 1 a la communaute, art. 1433 Cciv)
            Integer recompensesEpoux2Eur,              // FR — >= 0 EUR (recompenses dues par epoux 2 a la communaute, art. 1433 Cciv)
            // SF-216-07 : 3 champs IA ARIPA recouvrement pension alimentaire impayee FR.
            // Source : `famille_extracted_data.aripa_recouvrement_detection`.
            // FRANCE UNIQUEMENT — prompt impose null hors FR / hors certitude.
            Boolean aripaRecouvrementEnvisage,         // FR — CONTEXTUAL : mention ARIPA / SDR / pension impayee / L. 581 CSS
            Integer montantPensionMensuelleDueEur,     // FR — >= 0 EUR/mois (montant pension fixe par titre executoire)
            Boolean titreExecutoireDetecte,            // FR — true si titre executoire (jugement / convention CM / acte notarie) detecte
            // SF-216-03 : 2 champs IA pension alimentaire enfant FR.
            // Source : `famille_extracted_data.pension_alimentaire_detection`.
            // FRANCE UNIQUEMENT — prompt impose null hors FR / hors certitude.
            Boolean pensionAlimentaireEnvisagee,       // FR — CONTEXTUAL : mention art. 371-2 / contribution entretien
            String modeResidenceEnfantsDetecte,        // FR — ALTERNEE | PRINCIPALE_PARENT1 | PRINCIPALE_PARENT2 | null
            // SF-216-09 : 3 champs IA délégation autorité parentale FR.
            // Source : `famille_extracted_data.delegation_ap_detection`.
            // FRANCE UNIQUEMENT — prompt impose null hors FR / hors certitude.
            Boolean delegationApEnvisagee,             // FR — CONTEXTUAL : mention art. 376-1 / délégation AP / tiers détenteur
            String tiersLienFamilialDetecte,           // FR — GRANDS_PARENTS | ONCLE_TANTE | FAMILLE_ELARGIE | ASSOCIATION_HABILITEE | AUTRE | null
            Boolean accordParentsDetecte,              // FR — true si accord des deux parents documenté pour la délégation
            // SF-216-11 : 3 champs IA retrait autorité parentale FR.
            // Source : `famille_extracted_data.retrait_ap_detection`.
            // FRANCE UNIQUEMENT — prompt impose null hors FR / hors certitude.
            Boolean retraitApEnvisage,                 // FR — CONTEXTUAL : mention art. 378 / retrait AP / déchéance / loi 2022
            Boolean condamnationPenaleDetectee,        // FR — true si condamnation pénale documentée (art. 378 al. 1)
            Boolean violencesLmvss2022Detectees,       // FR — true si violences conjugales en présence enfant (loi 2022-140)
            // SF-216-15 : 1 champ IA adoption intra-familiale FR.
            // Source : `famille_extracted_data.adoption_intra_detection`.
            // FRANCE UNIQUEMENT — prompt impose null hors FR / hors certitude.
            // Flag CONTEXTUAL non pré-remplissable côté UI (V1 — PREFILL_COUNT_ALWAYS_ZERO).
            Boolean adoptionIntraEnvisagee,            // FR — CONTEXTUAL : mention art. 345-1 / adoption enfant du conjoint
            // SF-216-17 : 4 champs IA adoption internationale FR.
            // Source : `famille_extracted_data.adoption_internationale_detection`.
            // FRANCE UNIQUEMENT — prompt impose null hors FR / hors certitude.
            // adoptionInternationaleEnvisagee active la visibilité CONTEXTUAL F-IA-04.
            Boolean adoptionInternationaleEnvisagee,   // FR — CONTEXTUAL : mention art. 370-3 / Convention La Haye / OAA / agrément
            String paysOrigineAdopteDetecte,           // FR — pays d'origine de l'enfant extrait des pièces
            Boolean agrement2025DetecteValide,         // FR — true si agrément valide (≤ 5 ans) documenté
            Boolean exequaturRequisDetecte,            // FR — true si décision étrangère mentionnée nécessitant exequatur TJ
            // SF-216-13 : 2 champs IA audition du mineur par le JAF FR.
            // Source : `famille_extracted_data.audition_mineur_detection`.
            // FRANCE UNIQUEMENT — prompt impose null hors FR / hors certitude.
            // auditionMineurEnvisagee active la visibilité CONTEXTUAL F-IA-04.
            Boolean auditionMineurEnvisagee,           // FR — CONTEXTUAL : mention art. 388-1 / audition de l'enfant / entendre l'enfant
            Boolean demandeAuditionFormaliseeDetectee, // FR — true si demande d'audition déjà formalisée (pré-fill UI)
            // SF-216-19 : 3 champs IA indignité successorale FR.
            // Source : `famille_extracted_data.indignite_successorale_detection`.
            // FRANCE UNIQUEMENT — prompt impose null hors FR / hors certitude.
            // indigniteSuccessoraleEnvisagee active la visibilité CONTEXTUAL F-IA-04.
            Boolean indigniteSuccessoraleEnvisagee,    // FR — CONTEXTUAL : mention art. 726 / condamnation meurtre + succession
            Boolean condamnationPenaleSuccessionDetectee, // FR — true si condamnation pénale en lien avec le défunt documentée
            Boolean pardonTestamentaireDetecte,        // FR — true si pardon explicite dans le testament détecté (art. 728 Cciv)
            // SF-216-21 : 3 champs IA recel successoral FR.
            // Source : `famille_extracted_data.recel_succession_detection`.
            // FRANCE UNIQUEMENT — prompt impose null hors FR / hors certitude.
            // recelSuccessoralEnvisage active la visibilité CONTEXTUAL F-IA-04.
            Boolean recelSuccessoralEnvisage,          // FR — CONTEXTUAL : mention art. 778 / recel succession / bien dissimulé
            String typeRecelDetecte,                   // FR — type de recel qualifié dans les pièces (DISSIMULATION_BIEN, DESTRUCTION_TESTAMENT, etc.)
            String preuveRecelDetectee,                // FR — nature de la preuve mentionnée (AVEUX, DOCUMENT, TEMOIGNAGE, FAISCEAU_INDICES, etc.)
            // SF-216-23 : 3 champs IA donation entre époux FR.
            // Source : `famille_extracted_data.donation_entre_epoux_detection`.
            // FRANCE UNIQUEMENT — prompt impose null hors FR / hors certitude.
            // donationEntreEpouxEnvisagee active la visibilité CONTEXTUAL F-IA-04.
            Boolean donationEntreEpouxEnvisagee,       // FR — CONTEXTUAL : mention art. 1096 / donation au dernier vivant / avantage matrimonial
            Boolean revocabiliteDetectee,              // FR — true si révocation expresse / tacite détectée (art. 1096 al. 2)
            String bienDonnePrincipalType,             // FR — type de bien donné identifié (IMMOBILIER, MOBILIER, PORTEFEUILLE, NUMERAIRE, AUTRE)
            // SF-216-27 : 3 champs IA partage successoral notarié FR.
            // Source : `famille_extracted_data.partage_notarial_detection`.
            // FRANCE UNIQUEMENT — prompt impose null hors FR / hors certitude.
            // partageNotarialEnvisage active la visibilité CONTEXTUAL F-IA-04.
            Boolean partageNotarialEnvisage,           // FR — CONTEXTUAL : mention art. 816 / partage amiable / notaire désigné succession
            Boolean presenceImmeubleSuccessionDetecte, // FR — true si la succession comprend un immeuble (art. 1592 CGI — notaire obligatoire)
            String declarationSuccessionEcheancDetectee, // FR — échéance fiscale explicitement détectée (ISO date), null si à calculer depuis dateOuverture
            // SF-216-25 : 4 champs IA présomption de paternité du mari + désaveu FR.
            // Source : `famille_extracted_data.presomption_paternite_detection`.
            // FRANCE UNIQUEMENT — prompt impose null hors FR / hors certitude.
            // presomptionPaterniteEnvisagee active la visibilité CONTEXTUAL F-IA-04.
            Boolean presomptionPaterniteEnvisagee,     // FR — CONTEXTUAL : mention art. 312-316 / désaveu paternité / présomption mari
            Boolean desaveuEnvisage,                   // FR — true si action en désaveu (art. 316 al. 2 Cciv) documentée
            String dateConclusionMariageDetectee,      // FR — date conclusion du mariage extraite (ISO YYYY-MM-DD)
            String dateDissolutionMariageDetectee,     // FR — date dissolution du mariage extraite (ISO YYYY-MM-DD)
            // SF-216-29 : 3 champs IA donation-partage FR.
            // Source : `famille_extracted_data.donation_partage_detection`.
            // FRANCE UNIQUEMENT — prompt impose null hors FR / hors certitude.
            // donationPartageEnvisagee active la visibilité CONTEXTUAL F-IA-04.
            Boolean donationPartageEnvisagee,          // FR — CONTEXTUAL : mention art. 1075 / donation-partage / répartir patrimoine aux enfants
            Boolean presencePetitsEnfantsSubstitutionDetectee, // FR — true si petits-enfants bénéficiaires par substitution (art. 1075-1) documenté
            Boolean donationPartageConjonctiveDetectee) { // FR — true si donation conjointe des deux parents (art. 1075-2) documentée

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
            // SF-246-11 : date de naissance du demandeur pour pré-fill F-FA-26 (Famille FR).
            private String dateNaissanceDemandeurDetectee;
            // SF-246-24 : 15 champs booléens/énumérés D2 successions/libéralités (F-FA-24).
            private String qualiteHeritierDetectee;
            private Boolean actesEquivalentAcceptationDejaPosesDetected;
            private Boolean dettesIncertainesDetected;
            private Boolean conjointSurvivantDetected;
            private String qualiteDuDemandeurReserveDetecte;
            private String qualiteHeritierRapportDetectee;
            private Boolean donationDispenseDeRapportDetected;
            private Boolean naturePresumeeNonRapportableDetected;
            private Boolean tousDescendantsCommunsAvecConjointDetected;
            private String formeDonationDetectee;
            private Boolean saineDEspritDonateurDetected;
            private Boolean respectQuotiteDisponibleDetected;
            private String formeTestamentDetectee;
            private Boolean saineDEspritTestateurDetected;
            private Boolean legsExcedeQuotiteDisponibleDetected;
            // SF-246-25 : 17 champs booléens/énumérés D2 régimes & vie commune
            private Boolean contratNotarieDetected;
            private Boolean enfantsNonCommunsDetected;
            private Boolean clauseAttributionIntegraleDetected;
            private Boolean pvDifficultesEtablisDetected;
            private Boolean tentativeAmiableEpuiseueeDetected;
            private java.util.List<String> violencesAllegueesDetectees;
            private java.util.List<String> preuvesViolencesDetectees;
            private Boolean dangerImmediatDetected;
            private Boolean presenceEnfantsDetected;
            private Boolean logementCommunDetected;
            private Boolean victimeFinanciairementDependanteDetected;
            private String modeDissolutionPacsDetecte;
            private String regimeBiensPacsDetecte;
            private java.util.List<String> creancesAllegueesDetectees;
            private Boolean patrimoineCommunSignificatifDetecte;
            private Boolean patrimoineCommun;
            private Boolean violencesAlleguees;
            // SF-246-26 : 12 champs D2 filiation / adoption.
            private String qualiteAagirContestationDetected;
            private Boolean possessionEtatConforme5AnsDetected;
            private Boolean expertiseAdnDemandeeDetected;
            private Boolean motifsSerieuxDetected;
            private String qualiteDuDemandeurRechercheDetected;
            private Boolean presomptionPossessionEtatRechercheDetected;
            private Boolean expertiseAdnDemandeeRechercheDetected;
            private Boolean pereDesigneRefuseADNDetected;
            private Boolean motifsSerieuxRechercheDetected;
            private String formeAdoptionDemandeeDetected;
            private Boolean pupilleEtatDetected;
            private Boolean adoptantMarieDetected;
            // SF-246-27 : 8 champs IA protection majeurs / PMA / médiation / divorce.
            private String regimeProtectionMajeursDetected;
            private String dateCertificatMedicalMajeursDetected;
            private String datePmaDetected;
            private String dateReconnaissanceAnterieurePmaDetected;
            private String dateDonGametesDetected;
            private String motifSaisineMediationDetected;
            private String dateAssignationDivorce;
            private String dateAudienceHomologationDcBe;

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
            // SF-246-11 : setter date de naissance demandeur F-FA-26.
            public Builder dateNaissanceDemandeurDetectee(String v) { this.dateNaissanceDemandeurDetectee = v; return this; }
            // SF-246-24 : setters des 15 champs booléens/énumérés D2 successions/libéralités.
            public Builder qualiteHeritierDetectee(String v) { this.qualiteHeritierDetectee = v; return this; }
            public Builder actesEquivalentAcceptationDejaPosesDetected(Boolean v) { this.actesEquivalentAcceptationDejaPosesDetected = v; return this; }
            public Builder dettesIncertainesDetected(Boolean v) { this.dettesIncertainesDetected = v; return this; }
            public Builder conjointSurvivantDetected(Boolean v) { this.conjointSurvivantDetected = v; return this; }
            public Builder qualiteDuDemandeurReserveDetecte(String v) { this.qualiteDuDemandeurReserveDetecte = v; return this; }
            public Builder qualiteHeritierRapportDetectee(String v) { this.qualiteHeritierRapportDetectee = v; return this; }
            public Builder donationDispenseDeRapportDetected(Boolean v) { this.donationDispenseDeRapportDetected = v; return this; }
            public Builder naturePresumeeNonRapportableDetected(Boolean v) { this.naturePresumeeNonRapportableDetected = v; return this; }
            public Builder tousDescendantsCommunsAvecConjointDetected(Boolean v) { this.tousDescendantsCommunsAvecConjointDetected = v; return this; }
            public Builder formeDonationDetectee(String v) { this.formeDonationDetectee = v; return this; }
            public Builder saineDEspritDonateurDetected(Boolean v) { this.saineDEspritDonateurDetected = v; return this; }
            public Builder respectQuotiteDisponibleDetected(Boolean v) { this.respectQuotiteDisponibleDetected = v; return this; }
            public Builder formeTestamentDetectee(String v) { this.formeTestamentDetectee = v; return this; }
            public Builder saineDEspritTestateurDetected(Boolean v) { this.saineDEspritTestateurDetected = v; return this; }
            public Builder legsExcedeQuotiteDisponibleDetected(Boolean v) { this.legsExcedeQuotiteDisponibleDetected = v; return this; }
            // SF-246-25 : setters des 17 champs booléens/énumérés D2 régimes & vie commune.
            public Builder contratNotarieDetected(Boolean v) { this.contratNotarieDetected = v; return this; }
            public Builder enfantsNonCommunsDetected(Boolean v) { this.enfantsNonCommunsDetected = v; return this; }
            public Builder clauseAttributionIntegraleDetected(Boolean v) { this.clauseAttributionIntegraleDetected = v; return this; }
            public Builder pvDifficultesEtablisDetected(Boolean v) { this.pvDifficultesEtablisDetected = v; return this; }
            public Builder tentativeAmiableEpuiseueeDetected(Boolean v) { this.tentativeAmiableEpuiseueeDetected = v; return this; }
            public Builder violencesAllegueesDetectees(java.util.List<String> v) { this.violencesAllegueesDetectees = v; return this; }
            public Builder preuvesViolencesDetectees(java.util.List<String> v) { this.preuvesViolencesDetectees = v; return this; }
            public Builder dangerImmediatDetected(Boolean v) { this.dangerImmediatDetected = v; return this; }
            public Builder presenceEnfantsDetected(Boolean v) { this.presenceEnfantsDetected = v; return this; }
            public Builder logementCommunDetected(Boolean v) { this.logementCommunDetected = v; return this; }
            public Builder victimeFinanciairementDependanteDetected(Boolean v) { this.victimeFinanciairementDependanteDetected = v; return this; }
            public Builder modeDissolutionPacsDetecte(String v) { this.modeDissolutionPacsDetecte = v; return this; }
            public Builder regimeBiensPacsDetecte(String v) { this.regimeBiensPacsDetecte = v; return this; }
            public Builder creancesAllegueesDetectees(java.util.List<String> v) { this.creancesAllegueesDetectees = v; return this; }
            public Builder patrimoineCommunSignificatifDetecte(Boolean v) { this.patrimoineCommunSignificatifDetecte = v; return this; }
            public Builder patrimoineCommun(Boolean v) { this.patrimoineCommun = v; return this; }
            public Builder violencesAlleguees(Boolean v) { this.violencesAlleguees = v; return this; }
            // SF-246-26 setters
            public Builder qualiteAagirContestationDetected(String v) { this.qualiteAagirContestationDetected = v; return this; }
            public Builder possessionEtatConforme5AnsDetected(Boolean v) { this.possessionEtatConforme5AnsDetected = v; return this; }
            public Builder expertiseAdnDemandeeDetected(Boolean v) { this.expertiseAdnDemandeeDetected = v; return this; }
            public Builder motifsSerieuxDetected(Boolean v) { this.motifsSerieuxDetected = v; return this; }
            public Builder qualiteDuDemandeurRechercheDetected(String v) { this.qualiteDuDemandeurRechercheDetected = v; return this; }
            public Builder presomptionPossessionEtatRechercheDetected(Boolean v) { this.presomptionPossessionEtatRechercheDetected = v; return this; }
            public Builder expertiseAdnDemandeeRechercheDetected(Boolean v) { this.expertiseAdnDemandeeRechercheDetected = v; return this; }
            public Builder pereDesigneRefuseADNDetected(Boolean v) { this.pereDesigneRefuseADNDetected = v; return this; }
            public Builder motifsSerieuxRechercheDetected(Boolean v) { this.motifsSerieuxRechercheDetected = v; return this; }
            public Builder formeAdoptionDemandeeDetected(String v) { this.formeAdoptionDemandeeDetected = v; return this; }
            public Builder pupilleEtatDetected(Boolean v) { this.pupilleEtatDetected = v; return this; }
            public Builder adoptantMarieDetected(Boolean v) { this.adoptantMarieDetected = v; return this; }
            // SF-246-12 : champ BE divorce-desunion-be
            private String dateSeparationBe;
            // SF-246-27 setters
            public Builder regimeProtectionMajeursDetected(String v) { this.regimeProtectionMajeursDetected = v; return this; }
            public Builder dateCertificatMedicalMajeursDetected(String v) { this.dateCertificatMedicalMajeursDetected = v; return this; }
            public Builder datePmaDetected(String v) { this.datePmaDetected = v; return this; }
            public Builder dateReconnaissanceAnterieurePmaDetected(String v) { this.dateReconnaissanceAnterieurePmaDetected = v; return this; }
            public Builder dateDonGametesDetected(String v) { this.dateDonGametesDetected = v; return this; }
            public Builder motifSaisineMediationDetected(String v) { this.motifSaisineMediationDetected = v; return this; }
            public Builder dateAssignationDivorce(String v) { this.dateAssignationDivorce = v; return this; }
            public Builder dateAudienceHomologationDcBe(String v) { this.dateAudienceHomologationDcBe = v; return this; }
            // SF-246-12 setter
            public Builder dateSeparationBe(String v) { this.dateSeparationBe = v; return this; }

            // SF-246-28 : champs privés Famille BE (16 champs BELGIQUE UNIQUEMENT).
            private String modeHebergementPrincipalBeDetecte;
            private Integer nombreEnfantsBeDetecte;
            private Double revenuMensuelParent1BeDetecte;
            private Double revenuMensuelParent2BeDetecte;
            private Double allocationsFamilialesMensuellesBeDetectees;
            private Integer nuitsHebergementParent1BeDetectees;
            private Integer nuitsHebergementParent2BeDetectees;
            private Integer dureeMariageAnneesBeDetectee;
            private Double revenuMensuelCreancierBeDetecte;
            private Double revenuMensuelDebiteurBeDetecte;
            private String dateDesignationNotaireBeDetectee;
            private String dateOuvertureOperationsBeDetectee;
            private String dateNotificationProjetBeDetectee;
            private String dateHomologationBeDetectee;
            private String dateMariageBeDetectee;
            private Boolean contratMariageSigneBeDetecte;

            // SF-246-28 setters Famille BE (BELGIQUE UNIQUEMENT).
            public Builder modeHebergementPrincipalBeDetecte(String v) { this.modeHebergementPrincipalBeDetecte = v; return this; }
            public Builder nombreEnfantsBeDetecte(Integer v) { this.nombreEnfantsBeDetecte = v; return this; }
            public Builder revenuMensuelParent1BeDetecte(Double v) { this.revenuMensuelParent1BeDetecte = v; return this; }
            public Builder revenuMensuelParent2BeDetecte(Double v) { this.revenuMensuelParent2BeDetecte = v; return this; }
            public Builder allocationsFamilialesMensuellesBeDetectees(Double v) { this.allocationsFamilialesMensuellesBeDetectees = v; return this; }
            public Builder nuitsHebergementParent1BeDetectees(Integer v) { this.nuitsHebergementParent1BeDetectees = v; return this; }
            public Builder nuitsHebergementParent2BeDetectees(Integer v) { this.nuitsHebergementParent2BeDetectees = v; return this; }
            public Builder dureeMariageAnneesBeDetectee(Integer v) { this.dureeMariageAnneesBeDetectee = v; return this; }
            public Builder revenuMensuelCreancierBeDetecte(Double v) { this.revenuMensuelCreancierBeDetecte = v; return this; }
            public Builder revenuMensuelDebiteurBeDetecte(Double v) { this.revenuMensuelDebiteurBeDetecte = v; return this; }
            public Builder dateDesignationNotaireBeDetectee(String v) { this.dateDesignationNotaireBeDetectee = v; return this; }
            public Builder dateOuvertureOperationsBeDetectee(String v) { this.dateOuvertureOperationsBeDetectee = v; return this; }
            public Builder dateNotificationProjetBeDetectee(String v) { this.dateNotificationProjetBeDetectee = v; return this; }
            public Builder dateHomologationBeDetectee(String v) { this.dateHomologationBeDetectee = v; return this; }
            public Builder dateMariageBeDetectee(String v) { this.dateMariageBeDetectee = v; return this; }
            public Builder contratMariageSigneBeDetecte(Boolean v) { this.contratMariageSigneBeDetecte = v; return this; }
            // SF-216-01 : setters des 6 champs IA prestation compensatoire + vie commune FR.
            private Integer dureeMariageAnnees;
            private Double revenusAnnuelsEpoux1;
            private Double revenusAnnuelsEpoux2;
            private Integer ageEpoux1Annees;
            private Integer ageEpoux2Annees;
            private Boolean prestationCompensatoireEnvisagee;
            public Builder dureeMariageAnnees(Integer v) { this.dureeMariageAnnees = v; return this; }
            public Builder revenusAnnuelsEpoux1(Double v) { this.revenusAnnuelsEpoux1 = v; return this; }
            public Builder revenusAnnuelsEpoux2(Double v) { this.revenusAnnuelsEpoux2 = v; return this; }
            public Builder ageEpoux1Annees(Integer v) { this.ageEpoux1Annees = v; return this; }
            public Builder ageEpoux2Annees(Integer v) { this.ageEpoux2Annees = v; return this; }
            public Builder prestationCompensatoireEnvisagee(Boolean v) { this.prestationCompensatoireEnvisagee = v; return this; }
            // SF-216-05 : setters des 3 champs IA liquidation communaute legale FR.
            private Boolean liquidationCommunauteEnvisagee;
            private Integer recompensesEpoux1Eur;
            private Integer recompensesEpoux2Eur;
            public Builder liquidationCommunauteEnvisagee(Boolean v) { this.liquidationCommunauteEnvisagee = v; return this; }
            public Builder recompensesEpoux1Eur(Integer v) { this.recompensesEpoux1Eur = v; return this; }
            public Builder recompensesEpoux2Eur(Integer v) { this.recompensesEpoux2Eur = v; return this; }
            // SF-216-07 : setters des 3 champs IA ARIPA recouvrement FR.
            private Boolean aripaRecouvrementEnvisage;
            private Integer montantPensionMensuelleDueEur;
            private Boolean titreExecutoireDetecte;
            public Builder aripaRecouvrementEnvisage(Boolean v) { this.aripaRecouvrementEnvisage = v; return this; }
            public Builder montantPensionMensuelleDueEur(Integer v) { this.montantPensionMensuelleDueEur = v; return this; }
            public Builder titreExecutoireDetecte(Boolean v) { this.titreExecutoireDetecte = v; return this; }
            // SF-216-03 : setters des 2 champs IA pension alimentaire enfant FR.
            private Boolean pensionAlimentaireEnvisagee;
            private String modeResidenceEnfantsDetecte;
            public Builder pensionAlimentaireEnvisagee(Boolean v) { this.pensionAlimentaireEnvisagee = v; return this; }
            public Builder modeResidenceEnfantsDetecte(String v) { this.modeResidenceEnfantsDetecte = v; return this; }
            // SF-216-09 : setters des 3 champs IA délégation autorité parentale FR.
            private Boolean delegationApEnvisagee;
            private String tiersLienFamilialDetecte;
            private Boolean accordParentsDetecte;
            public Builder delegationApEnvisagee(Boolean v) { this.delegationApEnvisagee = v; return this; }
            public Builder tiersLienFamilialDetecte(String v) { this.tiersLienFamilialDetecte = v; return this; }
            public Builder accordParentsDetecte(Boolean v) { this.accordParentsDetecte = v; return this; }
            // SF-216-11 : setters des 3 champs IA retrait autorité parentale FR.
            private Boolean retraitApEnvisage;
            private Boolean condamnationPenaleDetectee;
            private Boolean violencesLmvss2022Detectees;
            public Builder retraitApEnvisage(Boolean v) { this.retraitApEnvisage = v; return this; }
            public Builder condamnationPenaleDetectee(Boolean v) { this.condamnationPenaleDetectee = v; return this; }
            public Builder violencesLmvss2022Detectees(Boolean v) { this.violencesLmvss2022Detectees = v; return this; }
            // SF-216-15 : setter du flag IA adoption intra-familiale FR.
            private Boolean adoptionIntraEnvisagee;
            public Builder adoptionIntraEnvisagee(Boolean v) { this.adoptionIntraEnvisagee = v; return this; }
            // SF-216-17 : setters des 4 champs IA adoption internationale FR.
            private Boolean adoptionInternationaleEnvisagee;
            private String paysOrigineAdopteDetecte;
            private Boolean agrement2025DetecteValide;
            private Boolean exequaturRequisDetecte;
            public Builder adoptionInternationaleEnvisagee(Boolean v) { this.adoptionInternationaleEnvisagee = v; return this; }
            public Builder paysOrigineAdopteDetecte(String v) { this.paysOrigineAdopteDetecte = v; return this; }
            public Builder agrement2025DetecteValide(Boolean v) { this.agrement2025DetecteValide = v; return this; }
            public Builder exequaturRequisDetecte(Boolean v) { this.exequaturRequisDetecte = v; return this; }
            // SF-216-13 : setters des 2 champs IA audition du mineur par le JAF FR.
            private Boolean auditionMineurEnvisagee;
            private Boolean demandeAuditionFormaliseeDetectee;
            public Builder auditionMineurEnvisagee(Boolean v) { this.auditionMineurEnvisagee = v; return this; }
            public Builder demandeAuditionFormaliseeDetectee(Boolean v) { this.demandeAuditionFormaliseeDetectee = v; return this; }
            // SF-216-19 : setters des 3 champs IA indignité successorale FR.
            private Boolean indigniteSuccessoraleEnvisagee;
            private Boolean condamnationPenaleSuccessionDetectee;
            private Boolean pardonTestamentaireDetecte;
            public Builder indigniteSuccessoraleEnvisagee(Boolean v) { this.indigniteSuccessoraleEnvisagee = v; return this; }
            public Builder condamnationPenaleSuccessionDetectee(Boolean v) { this.condamnationPenaleSuccessionDetectee = v; return this; }
            public Builder pardonTestamentaireDetecte(Boolean v) { this.pardonTestamentaireDetecte = v; return this; }
            // SF-216-21 : setters des 3 champs IA recel successoral FR.
            private Boolean recelSuccessoralEnvisage;
            private String typeRecelDetecte;
            private String preuveRecelDetectee;
            public Builder recelSuccessoralEnvisage(Boolean v) { this.recelSuccessoralEnvisage = v; return this; }
            public Builder typeRecelDetecte(String v) { this.typeRecelDetecte = v; return this; }
            public Builder preuveRecelDetectee(String v) { this.preuveRecelDetectee = v; return this; }
            // SF-216-23 : setters des 3 champs IA donation entre époux FR.
            private Boolean donationEntreEpouxEnvisagee;
            private Boolean revocabiliteDetectee;
            private String bienDonnePrincipalType;
            public Builder donationEntreEpouxEnvisagee(Boolean v) { this.donationEntreEpouxEnvisagee = v; return this; }
            public Builder revocabiliteDetectee(Boolean v) { this.revocabiliteDetectee = v; return this; }
            public Builder bienDonnePrincipalType(String v) { this.bienDonnePrincipalType = v; return this; }
            // SF-216-27 : setters des 3 champs IA partage successoral notarié FR.
            private Boolean partageNotarialEnvisage;
            private Boolean presenceImmeubleSuccessionDetecte;
            private String declarationSuccessionEcheancDetectee;
            public Builder partageNotarialEnvisage(Boolean v) { this.partageNotarialEnvisage = v; return this; }
            public Builder presenceImmeubleSuccessionDetecte(Boolean v) { this.presenceImmeubleSuccessionDetecte = v; return this; }
            public Builder declarationSuccessionEcheancDetectee(String v) { this.declarationSuccessionEcheancDetectee = v; return this; }
            // SF-216-25 : setters des 4 champs IA présomption de paternité FR.
            private Boolean presomptionPaterniteEnvisagee;
            private Boolean desaveuEnvisage;
            private String dateConclusionMariageDetectee;
            private String dateDissolutionMariageDetectee;
            public Builder presomptionPaterniteEnvisagee(Boolean v) { this.presomptionPaterniteEnvisagee = v; return this; }
            public Builder desaveuEnvisage(Boolean v) { this.desaveuEnvisage = v; return this; }
            public Builder dateConclusionMariageDetectee(String v) { this.dateConclusionMariageDetectee = v; return this; }
            public Builder dateDissolutionMariageDetectee(String v) { this.dateDissolutionMariageDetectee = v; return this; }
            // SF-216-29 : setters des 3 champs IA donation-partage FR.
            private Boolean donationPartageEnvisagee;
            private Boolean presencePetitsEnfantsSubstitutionDetectee;
            private Boolean donationPartageConjonctiveDetectee;
            public Builder donationPartageEnvisagee(Boolean v) { this.donationPartageEnvisagee = v; return this; }
            public Builder presencePetitsEnfantsSubstitutionDetectee(Boolean v) { this.presencePetitsEnfantsSubstitutionDetectee = v; return this; }
            public Builder donationPartageConjonctiveDetectee(Boolean v) { this.donationPartageConjonctiveDetectee = v; return this; }

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
                        fautesDetectees,
                        // SF-246-11 : date de naissance demandeur F-FA-26.
                        dateNaissanceDemandeurDetectee,
                        // SF-246-24 : 15 champs booléens/énumérés D2 successions/libéralités (F-FA-24).
                        qualiteHeritierDetectee,
                        actesEquivalentAcceptationDejaPosesDetected,
                        dettesIncertainesDetected,
                        conjointSurvivantDetected,
                        qualiteDuDemandeurReserveDetecte,
                        qualiteHeritierRapportDetectee,
                        donationDispenseDeRapportDetected,
                        naturePresumeeNonRapportableDetected,
                        tousDescendantsCommunsAvecConjointDetected,
                        formeDonationDetectee,
                        saineDEspritDonateurDetected,
                        respectQuotiteDisponibleDetected,
                        formeTestamentDetectee,
                        saineDEspritTestateurDetected,
                        legsExcedeQuotiteDisponibleDetected,
                        // SF-246-25 : 17 champs booléens/énumérés D2 régimes & vie commune.
                        contratNotarieDetected,
                        enfantsNonCommunsDetected,
                        clauseAttributionIntegraleDetected,
                        pvDifficultesEtablisDetected,
                        tentativeAmiableEpuiseueeDetected,
                        violencesAllegueesDetectees,
                        preuvesViolencesDetectees,
                        dangerImmediatDetected,
                        presenceEnfantsDetected,
                        logementCommunDetected,
                        victimeFinanciairementDependanteDetected,
                        modeDissolutionPacsDetecte,
                        regimeBiensPacsDetecte,
                        creancesAllegueesDetectees,
                        patrimoineCommunSignificatifDetecte,
                        patrimoineCommun,
                        violencesAlleguees,
                        // SF-246-26 : 12 champs filiation_detection_v2
                        qualiteAagirContestationDetected,
                        possessionEtatConforme5AnsDetected,
                        expertiseAdnDemandeeDetected,
                        motifsSerieuxDetected,
                        qualiteDuDemandeurRechercheDetected,
                        presomptionPossessionEtatRechercheDetected,
                        expertiseAdnDemandeeRechercheDetected,
                        pereDesigneRefuseADNDetected,
                        motifsSerieuxRechercheDetected,
                        formeAdoptionDemandeeDetected,
                        pupilleEtatDetected,
                        adoptantMarieDetected,
                        // SF-246-27 : 8 champs IA protection majeurs / PMA / médiation / divorce.
                        regimeProtectionMajeursDetected,
                        dateCertificatMedicalMajeursDetected,
                        datePmaDetected,
                        dateReconnaissanceAnterieurePmaDetected,
                        dateDonGametesDetected,
                        motifSaisineMediationDetected,
                        dateAssignationDivorce,
                        dateAudienceHomologationDcBe,
                        // SF-246-12 : date de séparation effective BE (divorce-desunion-be).
                        dateSeparationBe,
                        // SF-246-28 : 16 champs IA Famille BE — levée PREFILL_COUNT_ALWAYS_ZERO.
                        modeHebergementPrincipalBeDetecte,
                        nombreEnfantsBeDetecte,
                        revenuMensuelParent1BeDetecte,
                        revenuMensuelParent2BeDetecte,
                        allocationsFamilialesMensuellesBeDetectees,
                        nuitsHebergementParent1BeDetectees,
                        nuitsHebergementParent2BeDetectees,
                        dureeMariageAnneesBeDetectee,
                        revenuMensuelCreancierBeDetecte,
                        revenuMensuelDebiteurBeDetecte,
                        dateDesignationNotaireBeDetectee,
                        dateOuvertureOperationsBeDetectee,
                        dateNotificationProjetBeDetectee,
                        dateHomologationBeDetectee,
                        dateMariageBeDetectee,
                        contratMariageSigneBeDetecte,
                        // SF-216-01 : 6 champs IA prestation compensatoire + vie commune FR.
                        dureeMariageAnnees,
                        revenusAnnuelsEpoux1,
                        revenusAnnuelsEpoux2,
                        ageEpoux1Annees,
                        ageEpoux2Annees,
                        prestationCompensatoireEnvisagee,
                        // SF-216-05 : 3 champs IA liquidation communaute legale FR.
                        liquidationCommunauteEnvisagee,
                        recompensesEpoux1Eur,
                        recompensesEpoux2Eur,
                        // SF-216-07 : 3 champs IA ARIPA recouvrement FR.
                        aripaRecouvrementEnvisage,
                        montantPensionMensuelleDueEur,
                        titreExecutoireDetecte,
                        // SF-216-03 : 2 champs IA pension alimentaire enfant FR.
                        pensionAlimentaireEnvisagee,
                        modeResidenceEnfantsDetecte,
                        // SF-216-09 : 3 champs IA délégation autorité parentale FR.
                        delegationApEnvisagee,
                        tiersLienFamilialDetecte,
                        accordParentsDetecte,
                        // SF-216-11 : 3 champs IA retrait autorité parentale FR.
                        retraitApEnvisage,
                        condamnationPenaleDetectee,
                        violencesLmvss2022Detectees,
                        // SF-216-15 : 1 champ IA adoption intra-familiale FR.
                        adoptionIntraEnvisagee,
                        // SF-216-17 : 4 champs IA adoption internationale FR.
                        adoptionInternationaleEnvisagee,
                        paysOrigineAdopteDetecte,
                        agrement2025DetecteValide,
                        exequaturRequisDetecte,
                        // SF-216-13 : 2 champs IA audition du mineur par le JAF FR.
                        auditionMineurEnvisagee,
                        demandeAuditionFormaliseeDetectee,
                        // SF-216-19 : 3 champs IA indignité successorale FR.
                        indigniteSuccessoraleEnvisagee,
                        condamnationPenaleSuccessionDetectee,
                        pardonTestamentaireDetecte,
                        // SF-216-21 : 3 champs IA recel successoral FR.
                        recelSuccessoralEnvisage,
                        typeRecelDetecte,
                        preuveRecelDetectee,
                        // SF-216-23 : 3 champs IA donation entre époux FR.
                        donationEntreEpouxEnvisagee,
                        revocabiliteDetectee,
                        bienDonnePrincipalType,
                        // SF-216-27 : 3 champs IA partage successoral notarié FR.
                        partageNotarialEnvisage,
                        presenceImmeubleSuccessionDetecte,
                        declarationSuccessionEcheancDetectee,
                        // SF-216-25 : 4 champs IA présomption de paternité FR.
                        presomptionPaterniteEnvisagee,
                        desaveuEnvisage,
                        dateConclusionMariageDetectee,
                        dateDissolutionMariageDetectee,
                        // SF-216-29 : 3 champs IA donation-partage FR.
                        donationPartageEnvisagee,
                        presencePetitsEnfantsSubstitutionDetectee,
                        donationPartageConjonctiveDetectee);
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
            // SF-246-23 : sous-objet travail_be_detection (BELGIQUE UNIQUEMENT).
            // Peut être absent (dossier FR, pièces insuffisantes) → 6 champs null.
            JsonNode travailBe = node.get("travail_be_detection");
            boolean hasTravailBe = travailBe != null && travailBe.isObject();
            // SF-206-01 : sous-objet pour pré-fill F-DT-42 (abandon de poste
            // présomption de démission). Peut être absent (dossier sans MED ou
            // dossier BE) → tous les champs null.
            JsonNode abandonPoste = node.get("abandon_poste_detail");
            boolean hasAbandonPoste = abandonPoste != null && abandonPoste.isObject();
            // SF-206-05 : sous-objet pour pré-fill F-DT-39 (prise d'acte de la
            // rupture aux torts de l'employeur). Peut être absent (dossier sans
            // prise d'acte envisagée ou dossier BE) → tous les 11 champs null.
            JsonNode priseActe = node.get("prise_acte_detail");
            boolean hasPriseActe = priseActe != null && priseActe.isObject();
            // SF-206-03 : sous-objet pour pré-fill F-DT-75 (congés payés acquis
            // pendant arrêt maladie). Peut être absent (dossier sans arrêt
            // maladie long ou dossier BE) → tous les champs null.
            JsonNode cpArretMaladie = node.get("conges_payes_arret_maladie_detail");
            boolean hasCpArretMaladie = cpArretMaladie != null && cpArretMaladie.isObject();
            // SF-246-29 : sous-objet pour pré-fill exhaustif F-DT-38 (rupture
            // de période d'essai). Peut être absent (dossier sans contrat ni
            // lettre, dossier BE) → tous les 14 champs null. Le prompt impose
            // null pour la BE.
            JsonNode ruptureEssai = node.get("rupture_periode_essai_detail");
            boolean hasRuptureEssai = ruptureEssai != null && ruptureEssai.isObject();
            // SF-212-02 : sous-objet pour pré-fill F-DT-36 (licenciement pour
            // faute grave / faute lourde). Peut être absent (dossier sans
            // document disciplinaire ou dossier BE) → tous les 6 champs null.
            // Le prompt impose null pour la BE (distinction faute grave/lourde
            // strictement française — L.1234-1 s. CT).
            JsonNode fauteGrave = node.get("faute_grave_detail");
            boolean hasFauteGrave = fauteGrave != null && fauteGrave.isObject();
            // SF-212-03 : sous-objet pour pré-fill F-DT-50-forfait-jours-validite
            // (validité de la convention de forfait jours sur l'année). Peut
            // être absent (dossier sans clause de forfait ou dossier BE) →
            // tous les 5 champs null. Le prompt impose null pour la BE
            // (régime forfait jours L.3121-58+ CT strictement français).
            JsonNode forfaitJours = node.get("forfait_jours_detail");
            boolean hasForfaitJours = forfaitJours != null && forfaitJours.isObject();
            // SF-212-05 : sous-objet pour pré-fill F-DT-72 (transfert d'entreprise
            // L. 1224-1, FRANCE uniquement). Peut être absent (dossier sans
            // document de transfert, dossier BE) → tous les 5 champs null.
            // Le prompt impose null pour la BE (maintien des contrats lors
            // d'un transfert relève en BE de la CCT 32bis distincte).
            JsonNode transfertEntreprise = node.get("transfert_entreprise_detail");
            boolean hasTransfertEntreprise = transfertEntreprise != null && transfertEntreprise.isObject();
            // F-256 : sous-records consolidés (SF-212-07/09/11/13/15/19/25 + 17/21/35).
            // Les 7 premiers étaient à plat — désormais regroupés pour libérer des slots
            // sur le constructeur canonical de TravailExtractedData. JSON HTTP plat préservé
            // grâce à @JsonUnwrapped (parité contrat externe stricte).
            // SF-212-07 : sous-objet pour pré-fill F-DT-44 (CSP/CRP conformité FR).
            JsonNode cspNode = node.get("csp_detail");
            boolean hasCspNode = cspNode != null && cspNode.isObject();
            TravailExtractedData.CspDetail cspDetail = hasCspNode
                    ? new TravailExtractedData.CspDetail(
                            boundedIntOrNull(cspNode, "csp_effectif_entreprise", 0, MAX_CSP_EFFECTIF_ENTREPRISE),
                            booleanOrNull(cspNode, "csp_propose"),
                            booleanOrNull(cspNode, "csp_document_remis"),
                            isoDateOrNull(cspNode, "csp_date_remise"),
                            booleanOrNull(cspNode, "csp_adhesion"),
                            positiveDoubleOrNull(cspNode, "csp_salaire_mensuel_brut"))
                    : null;
            // SF-212-09 : sous-objet pour pré-fill F-DT-91 (faute inexcusable employeur FR).
            JsonNode fauteInexcusableNode = node.get("faute_inexcusable_detail");
            boolean hasFauteInexcusableNode = fauteInexcusableNode != null && fauteInexcusableNode.isObject();
            TravailExtractedData.FauteInexcusableDetail fauteInexcusableDetail = hasFauteInexcusableNode
                    ? new TravailExtractedData.FauteInexcusableDetail(
                            booleanOrNull(fauteInexcusableNode, "faute_inexcusable_conscience_danger"),
                            booleanOrNull(fauteInexcusableNode, "faute_inexcusable_signalement_prior"),
                            booleanOrNull(fauteInexcusableNode, "faute_inexcusable_mesures_prevention"),
                            boundedIntOrNull(fauteInexcusableNode, "faute_inexcusable_taux_ipp", 0, MAX_IPP_TAUX))
                    : null;
            // SF-212-25 : sous-objet pour pré-fill F-DT-61 (protection lanceur d'alerte FR).
            JsonNode lanceurAlerteNode = node.get("lanceur_alerte_detail");
            boolean hasLanceurAlerteNode = lanceurAlerteNode != null && lanceurAlerteNode.isObject();
            TravailExtractedData.LanceurAlerteDetail lanceurAlerteDetail = hasLanceurAlerteNode
                    ? new TravailExtractedData.LanceurAlerteDetail(
                            textOrNull(lanceurAlerteNode, "lanceur_alerte_nature_signalement"),
                            textOrNull(lanceurAlerteNode, "lanceur_alerte_procedure"),
                            booleanOrNull(lanceurAlerteNode, "lanceur_alerte_mesure_represaille"),
                            textOrNull(lanceurAlerteNode, "lanceur_alerte_nature_mesure"))
                    : null;
            // SF-212-11 : sous-objet pour pré-fill F-DT-70 (modification contrat refus FR).
            JsonNode modifContratNode = node.get("modification_contrat_detail");
            boolean hasModifContratNode = modifContratNode != null && modifContratNode.isObject();
            TravailExtractedData.ModifContratDetail modifContratDetail = hasModifContratNode
                    ? new TravailExtractedData.ModifContratDetail(
                            textOrNull(modifContratNode, "modif_contrat_element_modifie"),
                            booleanOrNull(modifContratNode, "modif_contrat_contractualise"),
                            booleanOrNull(modifContratNode, "modif_contrat_motif_eco"),
                            booleanOrNull(modifContratNode, "modif_contrat_notif_ecrite"))
                    : null;
            // SF-212-13 : sous-objet pour pré-fill F-DT-71 (mutation clause de mobilité FR).
            JsonNode mutationNode = node.get("mutation_mobilite_detail");
            boolean hasMutationNode = mutationNode != null && mutationNode.isObject();
            TravailExtractedData.MutationMobiliteDetail mutationMobiliteDetail = hasMutationNode
                    ? new TravailExtractedData.MutationMobiliteDetail(
                            booleanOrNull(mutationNode, "mutation_clause_presente"),
                            booleanOrNull(mutationNode, "mutation_zone_geographique_precise"),
                            booleanOrNull(mutationNode, "mutation_interet_legitime_employeur"),
                            intOrNull(mutationNode, "mutation_delai_prevenance_semaines"),
                            booleanOrNull(mutationNode, "mutation_situation_familiale_contraingnante"),
                            booleanOrNull(mutationNode, "mutation_motif_professionnel"))
                    : null;
            // SF-212-15 : sous-objet pour pré-fill F-DT-82 (télétravail — conformité et litige FR).
            JsonNode teletravailNode = node.get("teletravail_detail");
            boolean hasTeletravailNode = teletravailNode != null && teletravailNode.isObject();
            TravailExtractedData.TeletravailDetail teletravailDetail = hasTeletravailNode
                    ? new TravailExtractedData.TeletravailDetail(
                            textOrNull(teletravailNode, "teletravail_cadre"),
                            booleanOrNull(teletravailNode, "teletravail_double_volontariat"),
                            booleanOrNull(teletravailNode, "teletravail_indemnite_versee"),
                            doubleOrNull(teletravailNode, "teletravail_montant_indemnite_journalier"),
                            booleanOrNull(teletravailNode, "teletravail_accident_domicile"),
                            booleanOrNull(teletravailNode, "teletravail_retour_bureau_impose"),
                            booleanOrNull(teletravailNode, "teletravail_refus_cause_incrimination"))
                    : null;
            // SF-212-19 : sous-objet pour pré-fill F-DT-48 (mise à pied disciplinaire FR).
            JsonNode miseAPiedNode = node.get("mise_a_pied_detail");
            boolean hasMiseAPiedNode = miseAPiedNode != null && miseAPiedNode.isObject();
            TravailExtractedData.MiseAPiedDetail miseAPiedDetail = hasMiseAPiedNode
                    ? new TravailExtractedData.MiseAPiedDetail(
                            textOrNull(miseAPiedNode, "map_disciplinaire_nature"),
                            booleanOrNull(miseAPiedNode, "map_disciplinaire_procedure_suivie"),
                            booleanOrNull(miseAPiedNode, "map_disciplinaire_prescription_faute"),
                            booleanOrNull(miseAPiedNode, "map_disciplinaire_duree_ri"),
                            intOrNull(miseAPiedNode, "map_disciplinaire_duree_jours"),
                            booleanOrNull(miseAPiedNode, "map_disciplinaire_salaire_suspendu"),
                            booleanOrNull(miseAPiedNode, "map_disciplinaire_sanctions_anterieures"))
                    : null;
            // SF-212-23 : sous-objet pour pré-fill F-DT-56 (égalité salariale femmes/hommes FR).
            JsonNode egaliteSalarialeDetailNode = node.get("egalite_salariale_detail");
            boolean hasEgaliteSalariale = egaliteSalarialeDetailNode != null && egaliteSalarialeDetailNode.isObject();
            TravailExtractedData.EgaliteSalarialeDetail egaliteSalarialeDetail = hasEgaliteSalariale
                    ? new TravailExtractedData.EgaliteSalarialeDetail(
                            textOrNull(egaliteSalarialeDetailNode, "egalite_salariale_sexe_salarie"),
                            doubleOrNull(egaliteSalarialeDetailNode, "egalite_salariale_salaire_brut"),
                            intOrNull(egaliteSalarialeDetailNode, "egalite_salariale_anciennete"),
                            doubleOrNull(egaliteSalarialeDetailNode, "egalite_salariale_ecart_pourcentage"))
                    : null;
            // F-256 SF-212-17 : sous-objet pour pré-fill F-DT-43 (rupture anticipée du CDD FR).
            JsonNode racNode = node.get("rupture_anticipee_cdd_detail");
            boolean hasRac = racNode != null && racNode.isObject();
            TravailExtractedData.RuptureAnticipeeCddDetail ruptureAnticipeeCddDetail = hasRac
                    ? new TravailExtractedData.RuptureAnticipeeCddDetail(
                            normalizeEnumCode(textOrNull(racNode, "rupture_anticipee_cdd_auteur"), RAC_AUTEUR_CODES),
                            normalizeEnumCode(textOrNull(racNode, "rupture_anticipee_cdd_motif"), RAC_MOTIF_CODES),
                            isoDateOrNull(racNode, "rupture_anticipee_cdd_date_terme"))
                    : null;
            // F-256 SF-212-21 : sous-objet pour pré-fill F-DT-41 (démission équivoque FR).
            JsonNode demNode = node.get("demission_equivoque_detail");
            boolean hasDem = demNode != null && demNode.isObject();
            TravailExtractedData.DemissionEquivoqueDetail demissionEquivoqueDetail = hasDem
                    ? new TravailExtractedData.DemissionEquivoqueDetail(
                            textOrNull(demNode, "demission_mode_expression"),
                            booleanOrNull(demNode, "demission_contexte_altercation"),
                            booleanOrNull(demNode, "demission_pression"),
                            booleanOrNull(demNode, "demission_retractation"),
                            booleanOrNull(demNode, "demission_manquements_employeur"))
                    : null;
            // F-256 SF-212-35 : sous-objet pour pré-fill F-DT-46 (PDV / RCC conformité FR).
            JsonNode pdvNode = node.get("pdv_rcc_detail");
            boolean hasPdv = pdvNode != null && pdvNode.isObject();
            TravailExtractedData.PdvRccDetail pdvRccDetail = hasPdv
                    ? new TravailExtractedData.PdvRccDetail(
                            normalizeEnumCode(textOrNull(pdvNode, "pdv_rcc_type_dispositif"), PDV_RCC_TYPE_DISPOSITIF_CODES),
                            booleanOrNull(pdvNode, "pdv_rcc_accord_majoritaire"),
                            booleanOrNull(pdvNode, "pdv_rcc_validation_dreets"),
                            booleanOrNull(pdvNode, "pdv_rcc_indemnites_legales"))
                    : null;
            // SF-212-29 : sous-objet pour pré-fill F-DT-77 (congé maternité / paternité FR).
            // 5 champs : type (MATERNITE/PATERNITE), rang enfant (≥1), naissance multiple,
            // date début (ISO YYYY-MM-DD), salaire mensuel brut (€).
            JsonNode congeMpNode = node.get("conge_maternite_paternite_detail");
            boolean hasCongeMp = congeMpNode != null && congeMpNode.isObject();
            TravailExtractedData.CongeMaternitePaterniteDetail congeMaternitePaterniteDetail = hasCongeMp
                    ? new TravailExtractedData.CongeMaternitePaterniteDetail(
                            normalizeEnumCode(textOrNull(congeMpNode, "conge_maternite_paternite_type"), CONGE_MAT_PAT_TYPE_CODES),
                            boundedIntOrNull(congeMpNode, "conge_maternite_rang_enfant", 1, 20),
                            booleanOrNull(congeMpNode, "conge_maternite_naissance_multiple"),
                            isoDateOrNull(congeMpNode, "conge_maternite_date_debut"),
                            positiveDoubleOrNull(congeMpNode, "conge_maternite_salaire_mensuel_brut"))
                    : null;
            // SF-212-27 : sous-objet pour pré-fill F-DT-64 (burn-out reconnaissance MP FR).
            JsonNode burnoutNode = node.get("burnout_detail");
            boolean hasBurnout = burnoutNode != null && burnoutNode.isObject();
            TravailExtractedData.BurnoutDetail burnoutDetail = hasBurnout
                    ? new TravailExtractedData.BurnoutDetail(
                            booleanOrNull(burnoutNode, "burnout_diagnostic"),
                            boundedIntOrNull(burnoutNode, "burnout_taux_ipp", 0, MAX_IPP_TAUX),
                            booleanOrNull(burnoutNode, "burnout_surcharge_documentee"),
                            booleanOrNull(burnoutNode, "burnout_arrets_maladie"))
                    : null;
            // SF-212-31 : sous-objet pour pré-fill F-DT-65 (élections CSE conformité FR).
            // 4 champs : date élection (ISO YYYY-MM-DD), PAP négocié, collèges conformes,
            // résultats contestés.
            JsonNode electionsCseNode = node.get("elections_cse_detail");
            boolean hasElectionsCse = electionsCseNode != null && electionsCseNode.isObject();
            TravailExtractedData.ElectionsCseDetail electionsCseDetail = hasElectionsCse
                    ? new TravailExtractedData.ElectionsCseDetail(
                            isoDateOrNull(electionsCseNode, "election_cse_date_election"),
                            booleanOrNull(electionsCseNode, "election_cse_pap_negocie"),
                            booleanOrNull(electionsCseNode, "election_cse_colleges_conformes"),
                            booleanOrNull(electionsCseNode, "election_cse_resultats_contestes"))
                    : null;
            // SF-212-33 : sous-objet pour pré-fill F-DT-49 (temps partiel — requalification FR).
            // 4 champs : durée contractuelle (h/sem), mentions durée, mentions répartition,
            // HC moyenne (h/sem). Tous nullables ; FRANCE uniquement (régime BE distinct).
            JsonNode tempsPartielNode = node.get("temps_partiel_requalification_detail");
            boolean hasTempsPartiel = tempsPartielNode != null && tempsPartielNode.isObject();
            TravailExtractedData.TempsPartielRequalificationDetail tempsPartielRequalificationDetail = hasTempsPartiel
                    ? new TravailExtractedData.TempsPartielRequalificationDetail(
                            positiveDoubleOrNull(tempsPartielNode, "temps_partiel_duree_contractuelle"),
                            booleanOrNull(tempsPartielNode, "temps_partiel_mentions_duree"),
                            booleanOrNull(tempsPartielNode, "temps_partiel_mentions_repartition"),
                            positiveDoubleOrNull(tempsPartielNode, "temps_partiel_hc_moyenne"))
                    : null;
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
                    .teletravailLitigeDetecte(booleanOrFalse(node, "teletravail_litige_detecte"))
                    // SF-212-19 : flag F-205 — déclenche F-DT-48 mise à pied disciplinaire.
                    .miseAPiedDisciplinaireDetectee(booleanOrFalse(node, "mise_a_pied_disciplinaire_detectee"))
                    // SF-212-23 : flag F-205 — déclenche F-DT-56 égalité salariale femmes/hommes.
                    .egaliteSalarialePressentie(booleanOrFalse(node, "egalite_salariale_pressentie"))
                    // SF-212-17 : flag F-205 — déclenche F-DT-43 rupture anticipée CDD.
                    .ruptureAnticipeeCddDetectee(booleanOrFalse(node, "rupture_anticipee_cdd_detectee"))
                    // F-256 SF-212-21 : flag F-205 — déclenche F-DT-41 démission équivoque.
                    .demissionEquivoquePressentie(booleanOrFalse(node, "demission_equivoque_pressentie"))
                    // F-256 SF-212-35 : flag F-205 — déclenche F-DT-46 PDV/RCC conformité.
                    .pdvRccEnvisage(booleanOrFalse(node, "pdv_rcc_envisage"))
                    // SF-212-29 : flag F-205 — déclenche F-DT-77 congé maternité / paternité.
                    .congeMaternitePaterniteDetecte(booleanOrFalse(node, "conge_maternite_paternite_detecte"))
                    // SF-212-27 : flag F-205 — déclenche F-DT-64 burn-out reconnaissance MP.
                    .burnoutDetecte(booleanOrFalse(node, "burnout_detecte"))
                    // SF-212-31 : flag F-205 — déclenche F-DT-65 élections CSE conformité.
                    .electionCseDetectee(booleanOrFalse(node, "election_cse_detectee"))
                    // SF-212-33 : flag F-205 — déclenche F-DT-49 temps partiel — requalification.
                    .tempsPartielRequalificationEnvisagee(booleanOrFalse(node, "temps_partiel_requalification_envisagee"))
                    // SF-218-01 : flag F-205 — déclenche F-DT-86 appel CPH cour d'appel (FR).
                    .appelCphEnvisage(booleanOrFalse(node, "appel_cph_envisage"))
                    .dateNotificationJugement(isoDateOrNull(node, "date_notification_jugement"))
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
                    // SF-207-03 : 3 champs IA Travail BE pour pré-fill F-207-03
                    // contestation C4 ONEM. Le prompt impose null hors Belgique.
                    // Dates validées ISO YYYY-MM-DD (fail-open → null si non ISO) ;
                    // booléen extrait via booleanOrNull (peut rester null si IA
                    // n'a pas pu trancher).
                    .dateNotificationDecisionOnem(isoDateOrNull(node, "date_notification_decision_onem"))
                    .dateDecisionDirecteur(isoDateOrNull(node, "date_decision_directeur"))
                    .recoursAdminDejaForme(booleanOrNull(node, "recours_admin_deja_forme"))
                    // SF-207-04 : 2 champs IA Travail BE pour pré-fill F-207-04
                    // déclaration AT Fedris. Le prompt impose null hors Belgique.
                    // Dates validées ISO YYYY-MM-DD (fail-open → null si non ISO).
                    .dateAccident(isoDateOrNull(node, "date_accident"))
                    .dateConnaissanceAccidentEmployeur(isoDateOrNull(node, "date_connaissance_accident_employeur"))
                    // SF-207-05 : 3 champs IA Travail BE pour pré-fill F-207-05
                    // référé tribunal du travail BE. Le prompt impose null hors
                    // Belgique. motifUrgenceDetecte normalisé sur l'enum BE
                    // (code hors liste → null). dateFaitGenerateurUrgence
                    // validée ISO YYYY-MM-DD (fail-open → null). perilImmediatPresume
                    // booléen extrait via booleanOrNull (peut rester null si
                    // l'IA n'a pas pu trancher).
                    .motifUrgenceDetecte(normalizeEnumCode(textOrNull(node, "motif_urgence_detecte"), REFERE_BE_MOTIF_URGENCE_CODES))
                    .dateFaitGenerateurUrgence(isoDateOrNull(node, "date_fait_generateur_urgence"))
                    .perilImmediatPresume(booleanOrNull(node, "peril_immediat_presume"))
                    // SF-246-22 : type de procédure travail + date déclencheur pour pré-fill
                    // F-136 travail-procedure (FR+BE). Sous-objet procedure_travail_detection.
                    .procedureTravailDetectee(extractProcedureTravailCode(node))
                    .dateDeclencheurProcedure(extractProcedureTravailDate(node))
                    // SF-246-23 : sous-objet travail_be_detection (BELGIQUE uniquement).
                    // 2 dates motif grave, CP + jours avantages conventionnels, date demande crédit-temps.
                    // Toutes null si sous-objet absent (dossier FR ou pièces insuffisantes).
                    .dateConnaissanceFait(hasTravailBe ? isoDateOrNull(travailBe, "date_connaissance_fait") : null)
                    .dateNotificationMotifs(hasTravailBe ? isoDateOrNull(travailBe, "date_notification_motifs") : null)
                    .commissionParitaireBe(hasTravailBe ? truncatedTextOrNull(travailBe, "commission_paritaire_be", MAX_COMMISSION_PARITAIRE_BE_LENGTH) : null)
                    .joursTravaillesAnneePrecedenteBe(hasTravailBe ? boundedIntOrNull(travailBe, "jours_travailles_annee_precedente_be", 0, MAX_JOURS_TRAVAIL_BE) : null)
                    .joursPrestesBe(hasTravailBe ? boundedIntOrNull(travailBe, "jours_prestes_be", 0, MAX_JOURS_TRAVAIL_BE) : null)
                    .dateDemandeCreditTemps(hasTravailBe ? isoDateOrNull(travailBe, "date_demande_credit_temps") : null)
                    // SF-207-06 : 4 champs IA Travail BE pour pré-fill F-207-06
                    // RCC BE conditions d'éligibilité. Le prompt impose null
                    // hors Belgique. dateNaissanceSalarie validée ISO YYYY-MM-DD
                    // (fail-open → null si non ISO). anneesCarriereSalarie borné
                    // [0, MAX_CARRIERE_RCC_BE] (≤ 60 ans, valeur plausible).
                    // Les 2 booléens (metierLourdDetecte, entrepriseEnDifficulteDetectee)
                    // peuvent rester null si l'IA n'a pas pu trancher.
                    .dateNaissanceSalarie(isoDateOrNull(node, "date_naissance_salarie"))
                    .anneesCarriereSalarie(boundedIntOrNull(node, "annees_carriere_salarie", 0, MAX_CARRIERE_RCC_BE))
                    .metierLourdDetecte(booleanOrNull(node, "metier_lourd_detecte"))
                    .entrepriseEnDifficulteDetectee(booleanOrNull(node, "entreprise_en_difficulte_detectee"))
                    // SF-207-07 : 3 champs IA Travail BE pour pré-fill F-207-07
                    // RCC BE indemnité complémentaire. Le prompt impose null hors
                    // Belgique. Les 2 montants sont extraits via doubleOrNull
                    // (pas de borne haute — peuvent être de l'ordre de plusieurs
                    // milliers € / mois). dateDebutRccEnvisagee validée ISO
                    // YYYY-MM-DD (fail-open → null si non ISO).
                    .remunerationNetteReferenceRccDetectee(doubleOrNull(node, "remuneration_nette_reference_rcc_detectee"))
                    .allocationOnemMensuelleEstimee(doubleOrNull(node, "allocation_onem_mensuelle_estimee"))
                    .dateDebutRccEnvisagee(isoDateOrNull(node, "date_debut_rcc_envisagee"))
                    // SF-207-08 : 3 champs IA Travail BE pour pré-fill F-207-08
                    // outplacement BE obligatoire 45+ (CCT 82 ; CCT 82 bis ;
                    // Loi 05/09/2001 art. 13 ; AR 30/05/2018). Le prompt impose
                    // null hors Belgique. ancienneteSalarie : Double années
                    // avec décimales (pas de borne haute applicative — on borne
                    // soft à [0, 60] côté prompt). motifLicenciementDetecte :
                    // whitelist 4 valeurs. offreOutplacementMentionnee : booléen
                    // tri-état (null = pas tranchable).
                    .ancienneteSalarie(doubleOrNull(node, "anciennete_salarie"))
                    .motifLicenciementDetecte(whitelistedOrNull(
                            textOrNull(node, "motif_licenciement_detecte"),
                            "LICENCIEMENT_ECONOMIQUE",
                            "LICENCIEMENT_AUTRE",
                            "FAUTE_GRAVE",
                            "DEMISSION"))
                    .offreOutplacementMentionnee(booleanOrNull(node, "offre_outplacement_mentionnee"))
                    // SF-246-21 : 5 sous-objets thématiques Travail FR (FR uniquement, null dossier BE).
                    .cddDureeMois(extract246_21CddDureeMois(node))
                    .cddDateFinDernierContrat(extract246_21CddDateFin(node))
                    .cddNouveauDateDebut(extract246_21CddNouveauDateDebut(node))
                    .cddNouveauDateFin(extract246_21CddNouveauDateFin(node))
                    .cddTotalSalairesBruts(extract246_21CddTotalSalaires(node))
                    .interimDureeTotaleMois(extract246_21InterimDureeTotale(node))
                    .interimDateFinDerniereMission(extract246_21InterimDateFin(node))
                    .interimNouvellesMissionDateDebut(extract246_21InterimNouvellesMissionDateDebut(node))
                    .interimNouvellesMissionDateFin(extract246_21InterimNouvellesMissionDateFin(node))
                    .interimEntrepriseUtilisatrice(extract246_21InterimEntreprise(node))
                    .interimTotalRemunerationsBrutes(extract246_21InterimTotalRemunerations(node))
                    .interimDureeMissionJours(extract246_21InterimDureeMissionJours(node))
                    .congesJoursAcquis(extract246_21CongesJoursAcquis(node))
                    .congesJoursPris(extract246_21CongesJoursPris(node))
                    .rappelSalaireMontantPerverseMensuel(extract246_21RappelMontantPerverse(node))
                    .rappelSalairePeriodeDebut(extract246_21RappelPeriodeDebut(node))
                    .rappelSalairePeriodeFin(extract246_21RappelPeriodeFin(node))
                    .salarieAgeAnnees(extract246_21SalarieAge(node))
                    .pseNombreSalaries(extract246_21PseNombreSalaries(node))
                    .pseNombreLicenciements(extract246_21PseNombreLicenciements(node))
                    .transactionDateSignature(extract246_21TransactionDateSignature(node))
                    .transactionIndemniteMontantEur(extract246_21TransactionIndemnite(node))
                    .atDateAccident(extract246_21AtDateAccident(node))
                    .atDateExposition(extract246_21AtDateExposition(node))
                    .areTypeDecision(extract246_21AreTypeDecision(node))
                    .areMontantConteste(extract246_21AreMontantConteste(node))
                    .discriminationMotif(extract246_21DiscriminationMotif(node))
                    .discriminationContexte(extract246_21DiscriminationContexte(node))
                    .refereMontantProvision(extract246_21RefereMontantProvision(node))
                    .documentsDateCertificatTravail(extract246_21DocumentsDateCertificat(node))
                    .documentsDateAttestationFranceTravail(extract246_21DocumentsDateAttestation(node))
                    .documentsDateSoldeToutCompte(extract246_21DocumentsDateSolde(node))
                    // SF-206-01 : 8 champs IA pour pré-fill F-DT-42 — date ISO
                    // YYYY-MM-DD validée, délai borné [0, 365] jours, mode et
                    // motif normalisés sur enum (code hors liste → null).
                    // Booléens via booleanOrNull(). Tous null si sous-objet
                    // abandon_poste_detail absent.
                    .abandonPosteDateMiseEnDemeure(hasAbandonPoste ? isoDateOrNull(abandonPoste, "date_mise_en_demeure") : null)
                    .abandonPosteModeNotification(hasAbandonPoste ? normalizeEnumCode(textOrNull(abandonPoste, "mode_notification"), ABANDON_POSTE_MODE_NOTIFICATION_CODES) : null)
                    .abandonPosteDelaiAccordeJours(hasAbandonPoste ? boundedIntOrNull(abandonPoste, "delai_accorde_jours", 0, 365) : null)
                    .abandonPosteMotifAbsence(hasAbandonPoste ? normalizeEnumCode(textOrNull(abandonPoste, "motif_absence"), ABANDON_POSTE_MOTIF_ABSENCE_CODES) : null)
                    .abandonPosteDateReprise(hasAbandonPoste ? isoDateOrNull(abandonPoste, "date_reprise") : null)
                    .abandonPosteMedMentionneDelai(hasAbandonPoste ? booleanOrNull(abandonPoste, "med_mentionne_delai") : null)
                    .abandonPosteMedMentionneConsequences(hasAbandonPoste ? booleanOrNull(abandonPoste, "med_mentionne_consequences") : null)
                    .abandonPosteRepriseDansDelai(hasAbandonPoste ? booleanOrNull(abandonPoste, "reprise_dans_delai") : null)
                    // SF-206-05 : 11 champs IA pour pré-fill F-DT-39 (prise d'acte
                    // de la rupture aux torts de l'employeur, FR uniquement).
                    // Booléens via booleanOrNull(), montant en BigDecimal pour préserver
                    // la précision et accepter une borne stricte >= 0 (signum() côté
                    // calculator). Tous null si sous-objet prise_acte_detail absent.
                    .priseActeDefautPaiementSalaire(hasPriseActe ? booleanOrNull(priseActe, "defaut_paiement_salaire") : null)
                    .priseActeMontantImpayes(hasPriseActe ? bigDecimalOrNull(priseActe, "montant_impayes_eur") : null)
                    .priseActeHarcelement(hasPriseActe ? booleanOrNull(priseActe, "harcelement") : null)
                    .priseActeManquementSecurite(hasPriseActe ? booleanOrNull(priseActe, "manquement_securite") : null)
                    .priseActeModificationContrat(hasPriseActe ? booleanOrNull(priseActe, "modification_unilaterale_contrat") : null)
                    .priseActeDeclassement(hasPriseActe ? booleanOrNull(priseActe, "declassement") : null)
                    .priseActeDiscrimination(hasPriseActe ? booleanOrNull(priseActe, "discrimination") : null)
                    .priseActeHeuresSupNonPayees(hasPriseActe ? booleanOrNull(priseActe, "heures_sup_non_payees") : null)
                    .priseActeNonRespectRepos(hasPriseActe ? booleanOrNull(priseActe, "non_respect_durees_repos") : null)
                    .priseActeGriefsPersistants(hasPriseActe ? booleanOrNull(priseActe, "griefs_actuels_et_persistants") : null)
                    .priseActeGriefImpossiblePoursuite(hasPriseActe ? booleanOrNull(priseActe, "grief_rend_impossible_poursuite") : null)
                    // SF-206-03 : 5 champs IA pour pré-fill F-DT-75 — typeArret
                    // normalisé sur l'enum (code hors liste → null) ; nombreMois
                    // borné [1, 1200] (limite plausible — 100 ans) ; salarieEnPoste
                    // booléen ; dateRupture ISO YYYY-MM-DD validée (fail-open →
                    // null si non ISO) ; joursDejaAccordes BigDecimal (préserve la
                    // précision). Tous null si sous-objet absent.
                    .cpArretMaladieType(hasCpArretMaladie ? normalizeEnumCode(textOrNull(cpArretMaladie, "type_arret"), CP_ARRET_MALADIE_TYPE_CODES) : null)
                    .cpArretMaladieNombreMois(hasCpArretMaladie ? boundedIntOrNull(cpArretMaladie, "nombre_mois_arret", 1, 1200) : null)
                    .cpArretMaladieSalarieEnPoste(hasCpArretMaladie ? booleanOrNull(cpArretMaladie, "salarie_encore_en_poste") : null)
                    .cpArretMaladieDateRupture(hasCpArretMaladie ? isoDateOrNull(cpArretMaladie, "date_rupture_contrat") : null)
                    .cpArretMaladieJoursDejaAccordes(hasCpArretMaladie ? bigDecimalOrNull(cpArretMaladie, "jours_cp_deja_accordes") : null)
                    // SF-246-29 : 14 champs IA pour pré-fill exhaustif F-DT-38 — 2
                    // enums normalisés via normalizeEnumCode (codes hors whitelist
                    // → null), 3 entiers bornés via boundedIntOrNull (hors plage →
                    // null), 8 booléens via booleanOrNull (tri-état), 1 texte
                    // tronqué via truncatedTextOrNull (≤ 500 car.). Tous null si
                    // sous-objet rupture_periode_essai_detail absent (dossier BE).
                    .rpeCategorieSocioProfessionnelle(hasRuptureEssai ? normalizeEnumCode(textOrNull(ruptureEssai, "categorie_socio_professionnelle"), CATEGORIE_SOCIO_PROFESSIONNELLE_CODES) : null)
                    .rpeDureeCddMois(hasRuptureEssai ? boundedIntOrNull(ruptureEssai, "duree_cdd_mois", 0, MAX_RPE_DUREE_CDD_MOIS) : null)
                    .rpeDureePeriodeEssaiMois(hasRuptureEssai ? boundedIntOrNull(ruptureEssai, "duree_periode_essai_mois", 0, MAX_RPE_DUREE_ESSAI_MOIS) : null)
                    .rpeRenouvellementInvoque(hasRuptureEssai ? booleanOrNull(ruptureEssai, "renouvellement_invoque") : null)
                    .rpeAccordBrancheRenouvellement(hasRuptureEssai ? booleanOrNull(ruptureEssai, "accord_branche_renouvellement") : null)
                    .rpeAccordEcritSalarieRenouvellement(hasRuptureEssai ? booleanOrNull(ruptureEssai, "accord_ecrit_salarie_renouvellement") : null)
                    .rpeAuteurRupture(hasRuptureEssai ? normalizeEnumCode(textOrNull(ruptureEssai, "auteur_rupture"), AUTEUR_RUPTURE_CODES) : null)
                    .rpeDelaiPrevenanceJours(hasRuptureEssai ? boundedIntOrNull(ruptureEssai, "delai_prevenance_jours_appliques", 0, MAX_RPE_DELAI_PREVENANCE_JOURS) : null)
                    .rpeMotifLieCompetences(hasRuptureEssai ? booleanOrNull(ruptureEssai, "motif_lie_competences_professionnelles") : null)
                    .rpeMotifEconomique(hasRuptureEssai ? booleanOrNull(ruptureEssai, "motif_economique_ou_organisationnel") : null)
                    .rpeAtteinteLiberteFondamentale(hasRuptureEssai ? truncatedTextOrNull(ruptureEssai, "atteinte_liberte_fondamentale", MAX_RPE_ATTEINTE_LIBERTE_LENGTH) : null)
                    .rpeLettreRuptureMotivee(hasRuptureEssai ? booleanOrNull(ruptureEssai, "lettre_rupture_motivee") : null)
                    .rpeMotifsAveresParPieces(hasRuptureEssai ? booleanOrNull(ruptureEssai, "motifs_averes_par_pieces") : null)
                    .rpeCcnPlusFavorableRespectee(hasRuptureEssai ? booleanOrNull(ruptureEssai, "ccn_plus_favorable_respectee") : null)
                    // SF-252-01 — 7 protections nullité additionnelles (FR uniquement,
                    // null hors FR). Sous-objet `rupture_periode_essai_detail` enrichi
                    // par le prompt avec les définitions juridiques.
                    .rpeSalarieProtege(hasRuptureEssai ? booleanOrNull(ruptureEssai, "salarie_protege") : null)
                    .rpeAutorisationInspectionTravail(hasRuptureEssai ? booleanOrNull(ruptureEssai, "autorisation_inspection_travail_obtenue") : null)
                    .rpeLanceurAlerte(hasRuptureEssai ? booleanOrNull(ruptureEssai, "lanceur_alerte") : null)
                    .rpeTemoinHarcelement(hasRuptureEssai ? booleanOrNull(ruptureEssai, "temoin_ou_victime_harcelement") : null)
                    .rpeDroitRetraitExerce(hasRuptureEssai ? booleanOrNull(ruptureEssai, "droit_de_retrait_exerce") : null)
                    .rpeGrossesseDeclareePostRupture(hasRuptureEssai ? booleanOrNull(ruptureEssai, "grossesse_declaree_post_rupture") : null)
                    .rpeDateNotificationGrossesse(hasRuptureEssai ? isoDateOrNull(ruptureEssai, "date_notification_grossesse") : null)
                    // SF-212-02 : 6 champs IA pour pré-fill F-DT-36 (faute grave /
                    // faute lourde, FRANCE uniquement). Texte tronqué à 500 car.
                    // pour les faits reprochés, liste de dates ISO (peut être vide),
                    // qualification employeur normalisée sur enum 3 valeurs (code
                    // hors whitelist → null), booléen tri-état pour l'intention de
                    // nuire (null si non déterminable), ancienneté entière bornée
                    // [0, 600] mois, salaire mensuel brut strictement positif. Tous
                    // null si sous-objet `faute_grave_detail` absent (dossier sans
                    // document disciplinaire ou dossier BE).
                    .fauteGraveFaitsReproches(hasFauteGrave ? truncatedTextOrNull(fauteGrave, "faute_grave_faits_reproches", MAX_FAUTE_GRAVE_FAITS_REPROCHES_LENGTH) : null)
                    .fauteGraveDatesFaits(hasFauteGrave ? extractStringList(fauteGrave, "faute_grave_dates_faits") : null)
                    .fauteGraveQualificationEmployeur(hasFauteGrave ? normalizeEnumCode(textOrNull(fauteGrave, "faute_grave_qualification_employeur"), QUALIFICATION_FAUTE_CODES) : null)
                    .fauteGraveIntentionNuireAlleeguee(hasFauteGrave ? booleanOrNull(fauteGrave, "faute_grave_intention_nuire_alleeguee") : null)
                    .fauteGraveAncienneteMois(hasFauteGrave ? boundedIntOrNull(fauteGrave, "faute_grave_anciennete_mois", 0, MAX_FAUTE_GRAVE_ANCIENNETE_MOIS) : null)
                    .fauteGraveSalaireMensuelBrut(hasFauteGrave ? positiveDoubleOrNull(fauteGrave, "faute_grave_salaire_mensuel_brut") : null)
                    // SF-212-03 : 5 champs IA pour pré-fill F-DT-50 (forfait jours
                    // validité, FRANCE uniquement). Booléens tri-état (null si non
                    // déterminable), nombre de jours entier borné [0, 235] (au-delà
                    // = ramené à null). Tous null si sous-objet `forfait_jours_detail`
                    // absent (dossier sans clause de forfait ou dossier BE).
                    .forfaitJoursAccordCollectifExiste(hasForfaitJours ? booleanOrNull(forfaitJours, "forfait_jours_accord_collectif_existe") : null)
                    .forfaitJoursEntretienAnnuelRealise(hasForfaitJours ? booleanOrNull(forfaitJours, "forfait_jours_entretien_annuel_realise") : null)
                    .forfaitJoursDocumentControle(hasForfaitJours ? booleanOrNull(forfaitJours, "forfait_jours_document_controle") : null)
                    .forfaitJoursCategorieAutonome(hasForfaitJours ? booleanOrNull(forfaitJours, "forfait_jours_categorie_autonome") : null)
                    .forfaitJoursNbJours(hasForfaitJours ? boundedIntOrNull(forfaitJours, "forfait_jours_nb_jours", 0, MAX_FORFAIT_JOURS_NB_JOURS) : null)
                    // SF-212-05 : 5 champs IA pour pré-fill F-DT-72 (transfert
                    // d'entreprise L. 1224-1, FRANCE uniquement). Code type transfert
                    // normalisé sur enum 6 valeurs (code hors whitelist → null),
                    // 3 booléens tri-état (null si non déterminable), date ISO
                    // YYYY-MM-DD. Tous null si sous-objet `transfert_entreprise_detail`
                    // absent (dossier sans document de transfert ou dossier BE).
                    .transfertTypeTransfert(hasTransfertEntreprise ? normalizeEnumCode(textOrNull(transfertEntreprise, "transfert_type_transfert"), TYPE_TRANSFERT_CODES) : null)
                    .transfertEeaIdentifiee(hasTransfertEntreprise ? booleanOrNull(transfertEntreprise, "transfert_eea_identifiee") : null)
                    .transfertActivitePreservee(hasTransfertEntreprise ? booleanOrNull(transfertEntreprise, "transfert_activite_preservee") : null)
                    .transfertLicenciementsPreTransfert(hasTransfertEntreprise ? booleanOrNull(transfertEntreprise, "transfert_licenciements_pre_transfert") : null)
                    .transfertDateTransfert(hasTransfertEntreprise ? isoDateOrNull(transfertEntreprise, "transfert_date_transfert") : null)
                    // F-256 — sous-records consolidés (SF-212-07/09/11/13/15/19/25)
                    .cspDetail(cspDetail)
                    .fauteInexcusableDetail(fauteInexcusableDetail)
                    .lanceurAlerteDetail(lanceurAlerteDetail)
                    .modifContratDetail(modifContratDetail)
                    .mutationMobiliteDetail(mutationMobiliteDetail)
                    .teletravailDetail(teletravailDetail)
                    .miseAPiedDetail(miseAPiedDetail)
                    // SF-212-23 : sous-objet IA pour pré-fill F-DT-56 (égalité salariale FR).
                    .egaliteSalarialeDetail(egaliteSalarialeDetail)
                    // F-256 — 3 sous-records pour les dettes pré-fill IA (SF-212-17/21/35)
                    .ruptureAnticipeeCddDetail(ruptureAnticipeeCddDetail)
                    .demissionEquivoqueDetail(demissionEquivoqueDetail)
                    .pdvRccDetail(pdvRccDetail)
                    // SF-212-29 — sous-record pré-fill IA F-DT-77 congé maternité / paternité.
                    .congeMaternitePaterniteDetail(congeMaternitePaterniteDetail)
                    // SF-212-27 — burn-out reconnaissance MP (FR)
                    .burnoutDetail(burnoutDetail)
                    // SF-212-31 — élections CSE conformité (FR)
                    .electionsCseDetail(electionsCseDetail)
                    // SF-212-33 — temps partiel — requalification en temps plein (FR)
                    .tempsPartielRequalificationDetail(tempsPartielRequalificationDetail)
                    .build();
        } catch (Exception ignored) { return null; }
    }

    // -----------------------------------------------------------------------
    // SF-246-21 : méthodes d'extraction des 5 sous-objets thématiques
    // -----------------------------------------------------------------------

    /** Retourne le nœud `requalification_detection` ou null si absent/non-objet. */
    private static JsonNode getRequalificationNode(JsonNode travailNode) {
        JsonNode n = travailNode.get("requalification_detection");
        return (n != null && n.isObject()) ? n : null;
    }

    /** Retourne le nœud `paie_detection` ou null. */
    private static JsonNode getPaieNode(JsonNode travailNode) {
        JsonNode n = travailNode.get("paie_detection");
        return (n != null && n.isObject()) ? n : null;
    }

    /** Retourne le nœud `rupture_collective_detection` ou null. */
    private static JsonNode getRuptureCollectiveNode(JsonNode travailNode) {
        JsonNode n = travailNode.get("rupture_collective_detection");
        return (n != null && n.isObject()) ? n : null;
    }

    /** Retourne le nœud `sante_discrimination_detection` ou null. */
    private static JsonNode getSanteDiscriminationNode(JsonNode travailNode) {
        JsonNode n = travailNode.get("sante_discrimination_detection");
        return (n != null && n.isObject()) ? n : null;
    }

    /** Retourne le nœud `procedure_details_detection` ou null. */
    private static JsonNode getProcedureDetailsNode(JsonNode travailNode) {
        JsonNode n = travailNode.get("procedure_details_detection");
        return (n != null && n.isObject()) ? n : null;
    }

    // --- requalification_detection — CDD ---

    private static Integer extract246_21CddDureeMois(JsonNode travailNode) {
        JsonNode n = getRequalificationNode(travailNode);
        return n != null ? boundedIntOrNull(n, "cdd_duree_mois", 0, MAX_DUREE_CDD_INTERIM_MOIS) : null;
    }

    private static String extract246_21CddDateFin(JsonNode travailNode) {
        JsonNode n = getRequalificationNode(travailNode);
        return n != null ? isoDateOrNull(n, "cdd_date_fin_dernier_contrat") : null;
    }

    private static String extract246_21CddNouveauDateDebut(JsonNode travailNode) {
        JsonNode n = getRequalificationNode(travailNode);
        return n != null ? isoDateOrNull(n, "cdd_nouveau_date_debut") : null;
    }

    private static String extract246_21CddNouveauDateFin(JsonNode travailNode) {
        JsonNode n = getRequalificationNode(travailNode);
        return n != null ? isoDateOrNull(n, "cdd_nouveau_date_fin") : null;
    }

    private static Double extract246_21CddTotalSalaires(JsonNode travailNode) {
        JsonNode n = getRequalificationNode(travailNode);
        return n != null ? positiveDoubleOrNull(n, "cdd_total_salaires_bruts") : null;
    }

    // --- requalification_detection — intérim ---

    private static Integer extract246_21InterimDureeTotale(JsonNode travailNode) {
        JsonNode n = getRequalificationNode(travailNode);
        return n != null ? boundedIntOrNull(n, "interim_duree_totale_mois", 0, MAX_DUREE_CDD_INTERIM_MOIS) : null;
    }

    private static String extract246_21InterimDateFin(JsonNode travailNode) {
        JsonNode n = getRequalificationNode(travailNode);
        return n != null ? isoDateOrNull(n, "interim_date_fin_derniere_mission") : null;
    }

    private static String extract246_21InterimNouvellesMissionDateDebut(JsonNode travailNode) {
        JsonNode n = getRequalificationNode(travailNode);
        return n != null ? isoDateOrNull(n, "interim_nouvelle_mission_date_debut") : null;
    }

    private static String extract246_21InterimNouvellesMissionDateFin(JsonNode travailNode) {
        JsonNode n = getRequalificationNode(travailNode);
        return n != null ? isoDateOrNull(n, "interim_nouvelle_mission_date_fin") : null;
    }

    private static String extract246_21InterimEntreprise(JsonNode travailNode) {
        JsonNode n = getRequalificationNode(travailNode);
        return n != null ? truncatedTextOrNull(n, "interim_entreprise_utilisatrice", MAX_INTERIM_ENTREPRISE_LENGTH) : null;
    }

    private static Double extract246_21InterimTotalRemunerations(JsonNode travailNode) {
        JsonNode n = getRequalificationNode(travailNode);
        return n != null ? positiveDoubleOrNull(n, "interim_total_remunerations_brutes") : null;
    }

    private static Integer extract246_21InterimDureeMissionJours(JsonNode travailNode) {
        JsonNode n = getRequalificationNode(travailNode);
        return n != null ? boundedIntOrNull(n, "interim_duree_mission_jours", 0, MAX_DUREE_MISSION_JOURS) : null;
    }

    // --- paie_detection ---

    private static Integer extract246_21CongesJoursAcquis(JsonNode travailNode) {
        JsonNode n = getPaieNode(travailNode);
        return n != null ? boundedIntOrNull(n, "conges_jours_acquis", 0, MAX_CONGES_JOURS) : null;
    }

    private static Integer extract246_21CongesJoursPris(JsonNode travailNode) {
        JsonNode n = getPaieNode(travailNode);
        return n != null ? boundedIntOrNull(n, "conges_jours_pris", 0, MAX_CONGES_JOURS) : null;
    }

    private static Double extract246_21RappelMontantPerverse(JsonNode travailNode) {
        JsonNode n = getPaieNode(travailNode);
        return n != null ? positiveDoubleOrNull(n, "rappel_salaire_montant_perverse_mensuel") : null;
    }

    private static String extract246_21RappelPeriodeDebut(JsonNode travailNode) {
        JsonNode n = getPaieNode(travailNode);
        return n != null ? isoDateOrNull(n, "rappel_salaire_periode_debut") : null;
    }

    private static String extract246_21RappelPeriodeFin(JsonNode travailNode) {
        JsonNode n = getPaieNode(travailNode);
        return n != null ? isoDateOrNull(n, "rappel_salaire_periode_fin") : null;
    }

    // --- rupture_collective_detection ---

    private static Integer extract246_21SalarieAge(JsonNode travailNode) {
        JsonNode n = getRuptureCollectiveNode(travailNode);
        return n != null ? boundedIntOrNull(n, "salarie_age_annees", MIN_SALARIE_AGE_ANNEES, MAX_SALARIE_AGE_ANNEES) : null;
    }

    private static Integer extract246_21PseNombreSalaries(JsonNode travailNode) {
        JsonNode n = getRuptureCollectiveNode(travailNode);
        return n != null ? boundedIntOrNull(n, "pse_nombre_salaries", 0, MAX_PSE_NOMBRE) : null;
    }

    private static Integer extract246_21PseNombreLicenciements(JsonNode travailNode) {
        JsonNode n = getRuptureCollectiveNode(travailNode);
        return n != null ? boundedIntOrNull(n, "pse_nombre_licenciements", 0, MAX_PSE_NOMBRE) : null;
    }

    private static String extract246_21TransactionDateSignature(JsonNode travailNode) {
        JsonNode n = getRuptureCollectiveNode(travailNode);
        return n != null ? isoDateOrNull(n, "transaction_date_signature") : null;
    }

    private static Double extract246_21TransactionIndemnite(JsonNode travailNode) {
        JsonNode n = getRuptureCollectiveNode(travailNode);
        return n != null ? positiveDoubleOrNull(n, "transaction_indemnite_montant_eur") : null;
    }

    // --- sante_discrimination_detection ---

    private static String extract246_21AtDateAccident(JsonNode travailNode) {
        JsonNode n = getSanteDiscriminationNode(travailNode);
        return n != null ? isoDateOrNull(n, "at_date_accident") : null;
    }

    private static String extract246_21AtDateExposition(JsonNode travailNode) {
        JsonNode n = getSanteDiscriminationNode(travailNode);
        return n != null ? isoDateOrNull(n, "at_date_exposition") : null;
    }

    private static String extract246_21AreTypeDecision(JsonNode travailNode) {
        JsonNode n = getSanteDiscriminationNode(travailNode);
        return n != null ? normalizeEnumCode(textOrNull(n, "are_type_decision"), ARE_TYPE_DECISION_CODES) : null;
    }

    private static Double extract246_21AreMontantConteste(JsonNode travailNode) {
        JsonNode n = getSanteDiscriminationNode(travailNode);
        return n != null ? positiveDoubleOrNull(n, "are_montant_conteste") : null;
    }

    private static String extract246_21DiscriminationMotif(JsonNode travailNode) {
        JsonNode n = getSanteDiscriminationNode(travailNode);
        return n != null ? normalizeEnumCode(textOrNull(n, "discrimination_motif"), DISCRIMINATION_MOTIF_CODES) : null;
    }

    private static String extract246_21DiscriminationContexte(JsonNode travailNode) {
        JsonNode n = getSanteDiscriminationNode(travailNode);
        return n != null ? normalizeEnumCode(textOrNull(n, "discrimination_contexte"), DISCRIMINATION_CONTEXTE_CODES) : null;
    }

    // --- procedure_details_detection ---

    private static Double extract246_21RefereMontantProvision(JsonNode travailNode) {
        JsonNode n = getProcedureDetailsNode(travailNode);
        return n != null ? positiveDoubleOrNull(n, "refere_montant_provision") : null;
    }

    private static String extract246_21DocumentsDateCertificat(JsonNode travailNode) {
        JsonNode n = getProcedureDetailsNode(travailNode);
        return n != null ? isoDateOrNull(n, "documents_date_certificat_travail") : null;
    }

    private static String extract246_21DocumentsDateAttestation(JsonNode travailNode) {
        JsonNode n = getProcedureDetailsNode(travailNode);
        return n != null ? isoDateOrNull(n, "documents_date_attestation_france_travail") : null;
    }

    private static String extract246_21DocumentsDateSolde(JsonNode travailNode) {
        JsonNode n = getProcedureDetailsNode(travailNode);
        return n != null ? isoDateOrNull(n, "documents_date_solde_tout_compte") : null;
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

    /** SF-216-03 : whitelist mode de résidence enfant FR (pension alimentaire). */
    private static final java.util.Set<String> MODE_RESIDENCE_ENFANT_WHITELIST = java.util.Set.of(
            "ALTERNEE", "PRINCIPALE_PARENT1", "PRINCIPALE_PARENT2");

    /** SF-216-09 : whitelist lien familial tiers délégation AP FR (art. 376-1 Cciv). */
    private static final java.util.Set<String> TIERS_LIEN_FAMILIAL_WHITELIST = java.util.Set.of(
            "GRANDS_PARENTS", "ONCLE_TANTE", "FAMILLE_ELARGIE", "ASSOCIATION_HABILITEE", "AUTRE");

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
        // SF-214-19 : asile examiné en procédure accélérée (pré-fill F-IM-34 AJ CNDA)
        boolean asileProcedureeAccelereee = booleanOrFalse(root, "asile_procedure_acceleree_detectee");
        // Mesures d'éloignement : durée présence irrégulière + motif menace
        Integer eloiDureePresenceIrreguliereMois = boundedIntOrNull(
                root, "eloi_duree_presence_irreguliere_mois", 0, MAX_PRESENCE_MOIS);
        String eloiMotifMenace = normalizeEnumCode(
                textOrNull(root, "eloi_motif_menace"), ELOI_MOTIFS_MENACE_CODES);
        // SF-246-20 : lot Immigration BE — sous-objet immigration_be_detection_v2
        JsonNode beDet = root.get("immigration_be_detection_v2");
        // 9bis : date d'entrée en Belgique + durée calculée
        String be9bisDateEntreeBelgique = null;
        Integer be9bisDureePresenceMois = null;
        if (beDet != null && beDet.isObject()) {
            String be9bisRaw = textOrNull(beDet, "be_9bis_date_entree_belgique");
            if (be9bisRaw != null && be9bisRaw.matches(ISO_DATE_PATTERN_STR)) {
                String todayStr4 = java.time.LocalDate.now().toString();
                if (be9bisRaw.compareTo(todayStr4) <= 0) {
                    be9bisDateEntreeBelgique = be9bisRaw;
                    java.time.LocalDate entree = java.time.LocalDate.parse(be9bisRaw);
                    java.time.LocalDate today = java.time.LocalDate.now();
                    be9bisDureePresenceMois = (int) java.time.temporal.ChronoUnit.MONTHS.between(entree, today);
                }
            }
        }
        // 9ter : date de début des symptômes
        String be9terDateDebutSymptomes = null;
        if (beDet != null && beDet.isObject()) {
            String be9terRaw = textOrNull(beDet, "be_9ter_date_debut_symptomes");
            if (be9terRaw != null && be9terRaw.matches(ISO_DATE_PATTERN_STR)) {
                String todayStr5 = java.time.LocalDate.now().toString();
                if (be9terRaw.compareTo(todayStr5) <= 0) {
                    be9terDateDebutSymptomes = be9terRaw;
                }
            }
        }
        // 40bis : lien familial (whitelist 40bis — distinct de 40ter)
        String be40bisLienFamilial = null;
        if (beDet != null && beDet.isObject()) {
            be40bisLienFamilial = normalizeEnumCode(
                    textOrNull(beDet, "be_40bis_lien_familial"), LIENS_FAMILIAUX_40BIS_CODES);
        }
        // 40ter : lien familial (whitelist 40ter — distinct de 40bis) + revenus
        String be40terLienFamilial = null;
        Integer be40terRevenusMensuelsNets = null;
        if (beDet != null && beDet.isObject()) {
            be40terLienFamilial = normalizeEnumCode(
                    textOrNull(beDet, "be_40ter_lien_familial"), LIENS_FAMILIAUX_40TER_CODES);
            JsonNode revenusNode = beDet.get("be_40ter_revenus_mensuels_nets");
            if (revenusNode != null && !revenusNode.isNull() && revenusNode.isInt()) {
                int rev = revenusNode.asInt();
                if (rev > 0 && rev <= MAX_BE_REVENUS_MENSUELS_NETS) {
                    be40terRevenusMensuelsNets = rev;
                }
            }
        }
        // SF-214-01 : F-IM-25 Étranger malade L.425-9 CESEDA (FRANCE UNIQUEMENT).
        boolean etrangerMaladeDetecte = booleanOrFalse(root, "etranger_malade_detecte");
        String etrangerMaladePathologie = textOrNull(root, "etranger_malade_pathologie");
        Boolean etrangerMaladeTraitementDisponible = booleanOrNull(root, "etranger_malade_traitement_disponible");
        String etrangerMaladeAvisOFII = normalizeEnumCode(textOrNull(root, "etranger_malade_avis_ofii"),
                java.util.Set.of("FAVORABLE", "DEFAVORABLE", "EN_ATTENTE"));
        String etrangerMalaDateAvisOFIIRaw = textOrNull(root, "etranger_mala_date_avis_ofii");
        final String ISO_DATE_SF214 = "\\d{4}-\\d{2}-\\d{2}";
        String etrangerMalaDateAvisOFII = (etrangerMalaDateAvisOFIIRaw != null
                && etrangerMalaDateAvisOFIIRaw.matches(ISO_DATE_SF214)
                && etrangerMalaDateAvisOFIIRaw.compareTo(java.time.LocalDate.now().toString()) <= 0)
                ? etrangerMalaDateAvisOFIIRaw : null;
        // SF-215-05 : F-IM-27 Regroupement 10bis BE — 1 flag + 4 champs pré-fill (BE uniquement).
        boolean regroupementTiersLimiteDetecte = booleanOrFalse(root, "regroupement_10bis_detecte");
        String be10bisLienFamilial = normalizeEnumCode(
                textOrNull(root, "be_10bis_lien_familial"), LIENS_FAMILIAUX_10BIS_CODES);
        Integer be10bisRevenusMensuelsRaw = nonNegativeIntOrNull(root, "be_10bis_revenus_mensuels");
        Integer be10bisRevenusMensuels = (be10bisRevenusMensuelsRaw != null
                && be10bisRevenusMensuelsRaw > 0
                && be10bisRevenusMensuelsRaw <= MAX_BE_REVENUS_MENSUELS_NETS)
                ? be10bisRevenusMensuelsRaw : null;
        Integer be10bisDureeSejour = boundedIntOrNull(root, "be_10bis_duree_sejour", 0, 600);
        String be10bisDateFinCarteARaw = textOrNull(root, "be_10bis_date_fin_carte_a");
        // Date d'expiration : peut être passée OU future — on garde le format ISO uniquement.
        String be10bisDateFinCarteA = (be10bisDateFinCarteARaw != null
                && be10bisDateFinCarteARaw.matches(ISO_DATE_SF214))
                ? be10bisDateFinCarteARaw : null;
        // SF-215-07 : F-IM-28 Naturalisation 12bis BE — flag pivot + 3 champs IA réels.
        // 2 champs aspirationnels (preuveIntegration / preuveEmploi) NON extraits : ce
        // sont des évaluations juridiques que l'avocat saisit manuellement (PREFILL_COUNT
        // toujours zéro pour ces deux booléens — documenté F-246).
        boolean naturalisationBeEnvisagee = booleanOrFalse(root, "naturalisation_be_envisagee");
        Integer naturalisationBeDureeSejour = boundedIntOrNull(
                root, "naturalisation_be_duree_sejour", 0, 600);
        String naturalisationBeTypeSejour = normalizeEnumCode(
                textOrNull(root, "naturalisation_be_type_sejour"),
                NATURALISATION_BE_TYPE_SEJOUR_CODES);
        String naturalisationBeNiveauLangue = normalizeEnumCode(
                textOrNull(root, "naturalisation_be_niveau_langue"),
                NATURALISATION_BE_NIVEAU_LANGUE_CODES);
        // SF-215-09 : F-IM-29 Naturalisation conjoint Belge BE art. 16 CNB —
        // 3 champs IA réels (date mariage + durée cohabitation + niveau langue).
        // Le flag pivot `naturalisationBeEnvisagee` (SF-215-07) est partagé entre les deux
        // voies de naturalisation BE (12bis + conjoint Belge art. 16) — pas de nouveau flag.
        // 2 champs aspirationnels (cohabitationLegale / preuveIntegration) NON extraits :
        // évaluations juridiques saisies manuellement par l'avocat (PREFILL_COUNT_ALWAYS_ZERO).
        String naturalisationBeArt16DateMarriageRaw = textOrNull(root, "naturalisation_be_art16_date_marriage");
        String naturalisationBeArt16DateMarriage = (naturalisationBeArt16DateMarriageRaw != null
                && naturalisationBeArt16DateMarriageRaw.matches(ISO_DATE_SF214))
                ? naturalisationBeArt16DateMarriageRaw : null;
        Integer naturalisationBeArt16DureeCohabitation = boundedIntOrNull(
                root, "naturalisation_be_art16_duree_cohabitation", 0, 600);
        String naturalisationBeArt16NiveauLangue = normalizeEnumCode(
                textOrNull(root, "naturalisation_be_art16_niveau_langue"),
                NATURALISATION_BE_NIVEAU_LANGUE_CODES);
        // SF-215-11 : F-IM-30 AESM + tutelle MENA BE — flag pivot + 3 champs IA réels.
        // 5 champs aspirationnels NON extraits (tuteurDesigne / integrationScolaire /
        // projetVieElabore / perspectiveAutonomie / menaceOrdrePublic) = évaluations
        // juridiques saisies manuellement par l'avocat (PREFILL_COUNT_ALWAYS_ZERO).
        boolean mineurNonAccompagneBeDetecte = booleanOrFalse(
                root, "mineur_non_accompagne_be_detecte");
        Integer menaAge = boundedIntOrNull(root, "mena_age", 0, 17);
        String menaDateArriveeRaw = textOrNull(root, "mena_date_arrivee");
        String menaDateArrivee = (menaDateArriveeRaw != null
                && menaDateArriveeRaw.matches(ISO_DATE_SF214)
                && menaDateArriveeRaw.compareTo(java.time.LocalDate.now().toString()) <= 0)
                ? menaDateArriveeRaw : null;
        Integer menaDureeScolaire = boundedIntOrNull(root, "mena_duree_scolaire", 0, 120);
        // SF-215-13 : F-IM-31 Recours CCE annulation 30j BE — flag pivot + 2 champs IA réels.
        // recoursForme / dateRecours sont aspirationnels (actions procédurales non extractibles).
        boolean recoursCceEnvisage = booleanOrFalse(root, "recours_cce_envisage");
        String recoursCceDateNotificationRaw = textOrNull(root, "recours_cce_date_notification");
        String recoursCceDateNotification = (recoursCceDateNotificationRaw != null
                && recoursCceDateNotificationRaw.matches(ISO_DATE_SF214)
                && recoursCceDateNotificationRaw.compareTo(java.time.LocalDate.now().toString()) <= 0)
                ? recoursCceDateNotificationRaw : null;
        String recoursCceTypeDecision = normalizeEnumCode(
                textOrNull(root, "recours_cce_type_decision"), CCE_TYPE_DECISION_CODES);
        // SF-215-15 : F-IM-32 Recours CCE extrême urgence 5j ouvrables BE — 2 champs IA réels.
        // recoursForme / dateRecours sont aspirationnels (actions procédurales non extractibles).
        // L'acte exécutoire peut être daté dans le futur (rapatriement programmé) : pas de borne haute « non future » ici.
        String recoursExtremeUrgenceDateActeRaw = textOrNull(root, "recours_extreme_urgence_date_acte");
        String recoursExtremeUrgenceDateActe = (recoursExtremeUrgenceDateActeRaw != null
                && recoursExtremeUrgenceDateActeRaw.matches(ISO_DATE_SF214))
                ? recoursExtremeUrgenceDateActeRaw : null;
        String recoursExtremeUrgenceTypeActe = normalizeEnumCode(
                textOrNull(root, "recours_extreme_urgence_type_acte"), CCE_EXTREME_URGENCE_TYPE_ACTE_CODES);
        // SF-215-17 : F-IM-33 Annexe 13quinquies OQT + interdiction d'entrée art. 74/11 BE — 2 champs IA réels.
        // precedentSejour / recoursForme / dateRecours sont aspirationnels (non extractibles de façon fiable).
        String interdictionEntreeDateNotificationRaw = textOrNull(root, "interdiction_entree_date_notification");
        String interdictionEntreeDateNotification = (interdictionEntreeDateNotificationRaw != null
                && interdictionEntreeDateNotificationRaw.matches(ISO_DATE_SF214)
                && interdictionEntreeDateNotificationRaw.compareTo(java.time.LocalDate.now().toString()) <= 0)
                ? interdictionEntreeDateNotificationRaw : null;
        String interdictionEntreeMotif = normalizeEnumCode(
                textOrNull(root, "interdiction_entree_motif"), INTERDICTION_ENTREE_MOTIF_CODES);
        // SF-215-19 : F-IM-34 Protection temporaire Ukraine BE — 2 champs IA réels.
        // residence/apatride/membreFamille/titreSejour sont aspirationnels (appréciation juridique).
        String ptUkraineDateArriveeRaw = textOrNull(root, "pt_ukraine_date_arrivee");
        String ptUkraineDateArrivee = (ptUkraineDateArriveeRaw != null
                && ptUkraineDateArriveeRaw.matches(ISO_DATE_SF214)
                && ptUkraineDateArriveeRaw.compareTo(java.time.LocalDate.now().toString()) <= 0)
                ? ptUkraineDateArriveeRaw : null;
        Boolean ptUkraineNationalite = booleanOrNull(root, "pt_ukraine_nationalite");
        // SF-214-03 : F-IM-26 Regroupement familial FR — 1 flag pivot + 2 champs pré-fill (FR uniquement).
        boolean regroupementFamilialEnvisage = booleanOrFalse(root, "regroupement_familial_envisage");
        Double regroupementRessourcesMensuellesRaw = doubleOrNull(root, "regroupement_ressources_mensuelles");
        Double regroupementRessourcesMensuelles = (regroupementRessourcesMensuellesRaw != null
                && regroupementRessourcesMensuellesRaw > 0)
                ? regroupementRessourcesMensuellesRaw : null;
        String regroupementType = normalizeEnumCode(textOrNull(root, "regroupement_type"),
                java.util.Set.of("CONJOINT", "ENFANT_MINEUR", "AUTRE"));
        // SF-214-05 : F-IM-27 VPF liens personnels L. 423-23 FR — 1 flag pivot + 1 champ pré-fill (FR uniquement).
        boolean viePriveeFamilialeDetectee = booleanOrFalse(root, "vie_privee_familiale_detectee");
        String vpfNiveauIntegration = normalizeEnumCode(textOrNull(root, "vpf_niveau_integration"),
                java.util.Set.of("FORT", "MOYEN", "FAIBLE"));
        // SF-214-07 : F-IM-28 validation VLS-TS OFII 3 mois R. 311-3 FR — 1 booléen tri-état (FR uniquement).
        // Date d'entrée réutilisée via aesDateEntreeFrance ; type via typeTitreSejourCode (proxy).
        Boolean vlsTsValidationOFIIEffectuee = booleanOrNull(root, "vls_ts_validation_ofii_effectuee");
        // SF-214-15 : F-IM-32 récépissé vs attestation de prolongation R. 311-4/R. 311-6 FR —
        // 1 flag pivot + 1 champ pré-fill (FR uniquement). Date d'expiration réutilisée via dateExpirationTitre.
        boolean recouvrementTitreEnCours = booleanOrFalse(root, "recouvrement_titre_en_cours");
        String recepisseOuAttestationType = normalizeEnumCode(textOrNull(root, "recepisse_ou_attestation_type"),
                java.util.Set.of("RECEPISSE", "ATTESTATION_PROLONGATION", "INCONNU"));
        // SF-214-17 : F-IM-33 demande OFPRA introduction GUDA/ADA R. 521-1+ FR — 1 flag pré-fill (FR uniquement).
        // Date d'arrivée réutilisée via aesDateEntreeFrance ; visibilité via procedure_asile_detectee (F-201).
        boolean gudaPassageEffectue = booleanOrFalse(root, "guda_passage_effectue");
        // SF-214-21 : F-IM-35 victime de la traite des êtres humains L. 425-1 FR —
        // 1 flag pivot + 2 champs pré-fill (FR uniquement).
        boolean victimeTraiteDetectee = booleanOrFalse(root, "victime_traite_detectee");
        Boolean tehPlainteDeposee = booleanOrNull(root, "teh_plainte_deposee");
        String tehDatePlainte = textOrNull(root, "teh_date_plainte");
        // SF-214-23 : F-IM-36 carte de résident 10 ans L. 426-1 FR —
        // 1 flag pivot + 1 champ pré-fill (FR uniquement). Durée de séjour dérivée
        // de aesDureePresenceMois déjà extrait.
        boolean carteResidentEnvisagee = booleanOrFalse(root, "carte_resident_envisagee");
        Double carteResidentRessources = doubleOrNull(root, "carte_resident_ressources");
        // SF-214-25 : F-IM-37 ANEF procédure / pannes / recours FR — 1 flag pivot
        // (FR uniquement). Type de titre et date d'expiration dérivés des champs
        // typeTitreSejour / dateExpirationTitre déjà extraits.
        boolean anefPanneDetectee = booleanOrFalse(root, "anef_panne_detectee");
        // SF-214-27 : F-IM-38 MNA évaluation d'âge FR — 2 flags de pré-fill
        // (FR uniquement). La date de naissance est dérivée de mineurs_date_naissance
        // et la visibilité du flag existant client_mineur_detecte (F-201).
        boolean mnaEvaluationRefusee = booleanOrFalse(root, "mna_evaluation_refusee");
        boolean mnaExamenOsseuxOrdonne = booleanOrFalse(root, "mna_examen_osseux_ordonne");
        // SF-214-29 : F-IM-39 recours TJ refus déclaration de nationalité Cciv 26-3 FR —
        // 2 champs de pré-fill (FR uniquement). Voie whitelistée + date de refus ISO non future.
        // La visibilité reste pilotée par le flag existant naturalisation_envisagee_detectee (F-201).
        String naturalisationVoie = normalizeEnumCode(
                textOrNull(root, "naturalisation_voie"), NATURALISATION_VOIE_CODES);
        String naturalisationDateRefusRaw = textOrNull(root, "naturalisation_date_refus");
        String naturalisationDateRefus = null;
        if (naturalisationDateRefusRaw != null
                && naturalisationDateRefusRaw.matches("\\d{4}-\\d{2}-\\d{2}")
                && naturalisationDateRefusRaw.compareTo(java.time.LocalDate.now().toString()) <= 0) {
            naturalisationDateRefus = naturalisationDateRefusRaw;
        }
        // SF-214-33 : F-IM-41 appel CAA / cassation CE délais FR — 1 flag pivot
        // + 1 champ de pré-fill (FR uniquement). Date du jugement TA ISO non future.
        boolean recoursEnvisageDetecte = booleanOrFalse(root, "recours_envisage_detecte");
        String recoursDateJugementTARaw = textOrNull(root, "recours_date_jugement_ta");
        String recoursDateJugementTA = null;
        if (recoursDateJugementTARaw != null
                && recoursDateJugementTARaw.matches("\\d{4}-\\d{2}-\\d{2}")
                && recoursDateJugementTARaw.compareTo(java.time.LocalDate.now().toString()) <= 0) {
            recoursDateJugementTA = recoursDateJugementTARaw;
        }
        // SF-214-35 : F-IM-42 assignation à résidence L. 731-1 FR — 1 flag pivot
        // + 1 champ de pré-fill (FR uniquement). Date de notification ISO non future.
        boolean assignationResidenceDetectee = booleanOrFalse(root, "assignation_residence_detectee");
        String assignationDateNotificationRaw = textOrNull(root, "assignation_date_notification");
        String assignationDateNotification = null;
        if (assignationDateNotificationRaw != null
                && assignationDateNotificationRaw.matches("\\d{4}-\\d{2}-\\d{2}")
                && assignationDateNotificationRaw.compareTo(java.time.LocalDate.now().toString()) <= 0) {
            assignationDateNotification = assignationDateNotificationRaw;
        }
        // SF-214-41 : F-IM-45 retrait de titre pour fraude L. 412-7 FR — 1 flag pivot
        // + 2 champs de pré-fill (FR uniquement). Date de retrait ISO non future ;
        // motif whitelisté (4 codes), ramené à null hors whitelist.
        boolean retraitTitreFraudeDetecte = booleanOrFalse(root, "retrait_titre_fraude_detecte");
        String retraitTitreDateRetraitRaw = textOrNull(root, "retrait_titre_date_retrait");
        String retraitTitreDateRetrait = null;
        if (retraitTitreDateRetraitRaw != null
                && retraitTitreDateRetraitRaw.matches("\\d{4}-\\d{2}-\\d{2}")
                && retraitTitreDateRetraitRaw.compareTo(java.time.LocalDate.now().toString()) <= 0) {
            retraitTitreDateRetrait = retraitTitreDateRetraitRaw;
        }
        String retraitTitreMotif = normalizeEnumCode(
                textOrNull(root, "retrait_titre_motif"), RETRAIT_TITRE_FRAUDE_MOTIF_CODES);
        // SF-214-37 : F-IM-43 ITF judiciaire (peine complémentaire C. pén. 131-30) FR —
        // 2 champs de pré-fill (FR uniquement). Réutilise le flag pivot
        // mesure_eloignement_detectee (déjà extrait) pour la visibilité de l'outil.
        // Date de condamnation ISO non future + durée d'ITF en années (> 0).
        String itfJudiciaireDateCondamnationRaw = textOrNull(root, "itf_judiciaire_date_condamnation");
        String itfJudiciaireDateCondamnation = null;
        if (itfJudiciaireDateCondamnationRaw != null
                && itfJudiciaireDateCondamnationRaw.matches("\\d{4}-\\d{2}-\\d{2}")
                && itfJudiciaireDateCondamnationRaw.compareTo(java.time.LocalDate.now().toString()) <= 0) {
            itfJudiciaireDateCondamnation = itfJudiciaireDateCondamnationRaw;
        }
        Integer itfJudiciaireDureeAnneesRaw = nonNegativeIntOrNull(root, "itf_judiciaire_duree_annees");
        Integer itfJudiciaireDureeAnnees = (itfJudiciaireDureeAnneesRaw != null
                && itfJudiciaireDureeAnneesRaw > 0)
                ? itfJudiciaireDureeAnneesRaw : null;
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
                && eloiDureePresenceIrreguliereMois == null && eloiMotifMenace == null
                && be9bisDateEntreeBelgique == null && be9terDateDebutSymptomes == null
                && be40bisLienFamilial == null && be40terLienFamilial == null
                && be40terRevenusMensuelsNets == null
                // SF-214-01 : champs nullables seulement (le boolean primitif n'est jamais null)
                && etrangerMaladePathologie == null && etrangerMaladeTraitementDisponible == null
                && etrangerMaladeAvisOFII == null && etrangerMalaDateAvisOFII == null
                // SF-215-05 : champs nullables seulement (le boolean primitif n'est jamais null)
                && !regroupementTiersLimiteDetecte
                && be10bisLienFamilial == null && be10bisRevenusMensuels == null
                && be10bisDureeSejour == null && be10bisDateFinCarteA == null
                // SF-215-07 : champs nullables seulement (le boolean primitif n'est jamais null)
                && !naturalisationBeEnvisagee
                && naturalisationBeDureeSejour == null
                && naturalisationBeTypeSejour == null
                && naturalisationBeNiveauLangue == null
                // SF-215-09 : 3 champs IA art. 16 CNB (nullables)
                && naturalisationBeArt16DateMarriage == null
                && naturalisationBeArt16DureeCohabitation == null
                && naturalisationBeArt16NiveauLangue == null
                // SF-215-11 : F-IM-30 AESM + tutelle MENA BE (flag + 3 champs IA, nullables)
                && !mineurNonAccompagneBeDetecte
                && menaAge == null
                && menaDateArrivee == null
                && menaDureeScolaire == null
                // SF-215-13 : F-IM-31 Recours CCE annulation 30j BE (flag + 2 champs IA, nullables)
                && !recoursCceEnvisage
                && recoursCceDateNotification == null
                && recoursCceTypeDecision == null
                // SF-215-15 : F-IM-32 Recours CCE extrême urgence 5j ouvrables BE (2 champs IA, nullables)
                && recoursExtremeUrgenceDateActe == null
                && recoursExtremeUrgenceTypeActe == null
                // SF-215-17 : F-IM-33 Annexe 13quinquies OQT + interdiction d'entrée BE (2 champs IA, nullables)
                && interdictionEntreeDateNotification == null
                && interdictionEntreeMotif == null
                // SF-215-19 : F-IM-34 Protection temporaire Ukraine BE (2 champs IA, nullables)
                && ptUkraineDateArrivee == null
                && ptUkraineNationalite == null
                // SF-214-03 : F-IM-26 Regroupement familial FR (1 flag + 2 champs IA)
                && !regroupementFamilialEnvisage
                && regroupementRessourcesMensuelles == null
                && regroupementType == null
                // SF-214-05 : F-IM-27 VPF liens personnels L. 423-23 FR (1 flag + 1 champ IA)
                && !viePriveeFamilialeDetectee
                && vpfNiveauIntegration == null
                // SF-214-07 : F-IM-28 validation VLS-TS OFII 3 mois R. 311-3 FR (1 booléen tri-état)
                && vlsTsValidationOFIIEffectuee == null
                // SF-214-15 : F-IM-32 récépissé vs attestation de prolongation FR (1 flag + 1 champ IA)
                && !recouvrementTitreEnCours
                && recepisseOuAttestationType == null
                // SF-214-17 : F-IM-33 demande OFPRA introduction GUDA/ADA FR (1 flag pré-fill)
                && !gudaPassageEffectue
                // SF-214-21 : F-IM-35 victime de la traite des êtres humains L. 425-1 FR (1 flag + 2 champs IA)
                && !victimeTraiteDetectee
                && tehPlainteDeposee == null
                && tehDatePlainte == null
                // SF-214-23 : F-IM-36 carte de résident 10 ans L. 426-1 FR (1 flag + 1 champ IA)
                && !carteResidentEnvisagee
                && carteResidentRessources == null
                // SF-214-25 : F-IM-37 ANEF procédure / pannes / recours FR (1 flag pivot)
                && !anefPanneDetectee
                // SF-214-27 : F-IM-38 MNA évaluation d'âge FR (2 flags de pré-fill)
                && !mnaEvaluationRefusee
                && !mnaExamenOsseuxOrdonne
                // SF-214-29 : F-IM-39 recours TJ refus déclaration de nationalité FR (2 champs IA, nullables)
                && naturalisationVoie == null
                && naturalisationDateRefus == null
                // SF-214-33 : F-IM-41 appel CAA / cassation CE délais FR (1 flag pivot + 1 champ IA)
                && !recoursEnvisageDetecte
                && recoursDateJugementTA == null
                // SF-214-35 : F-IM-42 assignation à résidence L. 731-1 FR (1 flag pivot + 1 champ IA)
                && !assignationResidenceDetectee
                && assignationDateNotification == null
                // SF-214-37 : F-IM-43 ITF judiciaire C. pén. 131-30 FR (2 champs IA, flag pivot réutilisé)
                && itfJudiciaireDateCondamnation == null
                && itfJudiciaireDureeAnnees == null
                // SF-214-41 : F-IM-45 retrait de titre pour fraude L. 412-7 FR (1 flag pivot + 2 champs IA)
                && !retraitTitreFraudeDetecte
                && retraitTitreDateRetrait == null
                && retraitTitreMotif == null) return null;
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
                .asileProcedureeAccelereee(asileProcedureeAccelereee)
                .eloiDureePresenceIrreguliereMois(eloiDureePresenceIrreguliereMois)
                .eloiMotifMenace(eloiMotifMenace)
                // SF-246-20 : lot Immigration BE
                .be9bisDateEntreeBelgique(be9bisDateEntreeBelgique)
                .be9bisDureePresenceMois(be9bisDureePresenceMois)
                .be9terDateDebutSymptomes(be9terDateDebutSymptomes)
                .be40bisLienFamilial(be40bisLienFamilial)
                .be40terLienFamilial(be40terLienFamilial)
                .be40terRevenusMensuelsNets(be40terRevenusMensuelsNets)
                // SF-214-01 : F-IM-25 Étranger malade L.425-9
                .etrangerMaladeDetecte(etrangerMaladeDetecte)
                .etrangerMaladePathologie(etrangerMaladePathologie)
                .etrangerMaladeTraitementDisponible(etrangerMaladeTraitementDisponible)
                .etrangerMaladeAvisOFII(etrangerMaladeAvisOFII)
                .etrangerMalaDateAvisOFII(etrangerMalaDateAvisOFII)
                // SF-215-05 : F-IM-27 Regroupement 10bis BE — flag + 4 champs pré-fill
                .regroupementTiersLimiteDetecte(regroupementTiersLimiteDetecte)
                .be10bisLienFamilial(be10bisLienFamilial)
                .be10bisRevenusMensuels(be10bisRevenusMensuels)
                .be10bisDureeSejour(be10bisDureeSejour)
                .be10bisDateFinCarteA(be10bisDateFinCarteA)
                // SF-215-07 : F-IM-28 Naturalisation 12bis BE — flag + 3 champs pré-fill réels
                .naturalisationBeEnvisagee(naturalisationBeEnvisagee)
                .naturalisationBeDureeSejour(naturalisationBeDureeSejour)
                .naturalisationBeTypeSejour(naturalisationBeTypeSejour)
                .naturalisationBeNiveauLangue(naturalisationBeNiveauLangue)
                // SF-215-09 : F-IM-29 Naturalisation conjoint Belge BE — 3 champs pré-fill réels (art. 16 CNB)
                .naturalisationBeArt16DateMarriage(naturalisationBeArt16DateMarriage)
                .naturalisationBeArt16DureeCohabitation(naturalisationBeArt16DureeCohabitation)
                .naturalisationBeArt16NiveauLangue(naturalisationBeArt16NiveauLangue)
                // SF-215-11 : F-IM-30 AESM + tutelle MENA BE — flag pivot + 3 champs pré-fill réels
                .mineurNonAccompagneBeDetecte(mineurNonAccompagneBeDetecte)
                .menaAge(menaAge)
                .menaDateArrivee(menaDateArrivee)
                .menaDureeScolaire(menaDureeScolaire)
                // SF-215-13 : F-IM-31 Recours CCE annulation 30j BE — flag pivot + 2 champs pré-fill réels
                .recoursCceEnvisage(recoursCceEnvisage)
                .recoursCceDateNotification(recoursCceDateNotification)
                .recoursCceTypeDecision(recoursCceTypeDecision)
                // SF-215-15 : F-IM-32 Recours CCE extrême urgence 5j ouvrables BE — 2 champs pré-fill réels
                .recoursExtremeUrgenceDateActe(recoursExtremeUrgenceDateActe)
                .recoursExtremeUrgenceTypeActe(recoursExtremeUrgenceTypeActe)
                .interdictionEntreeDateNotification(interdictionEntreeDateNotification)
                .interdictionEntreeMotif(interdictionEntreeMotif)
                // SF-215-19 : F-IM-34 Protection temporaire Ukraine BE — 2 champs pré-fill réels
                .ptUkraineDateArrivee(ptUkraineDateArrivee)
                .ptUkraineNationalite(ptUkraineNationalite)
                .regroupementFamilialEnvisage(regroupementFamilialEnvisage)
                .regroupementRessourcesMensuelles(regroupementRessourcesMensuelles)
                .regroupementType(regroupementType)
                .viePriveeFamilialeDetectee(viePriveeFamilialeDetectee)
                .vpfNiveauIntegration(vpfNiveauIntegration)
                .vlsTsValidationOFIIEffectuee(vlsTsValidationOFIIEffectuee)
                .recouvrementTitreEnCours(recouvrementTitreEnCours)
                .recepisseOuAttestationType(recepisseOuAttestationType)
                .gudaPassageEffectue(gudaPassageEffectue)
                .victimeTraiteDetectee(victimeTraiteDetectee)
                .tehPlainteDeposee(tehPlainteDeposee)
                .tehDatePlainte(tehDatePlainte)
                .carteResidentEnvisagee(carteResidentEnvisagee)
                .carteResidentRessources(carteResidentRessources)
                .anefPanneDetectee(anefPanneDetectee)
                .mnaEvaluationRefusee(mnaEvaluationRefusee)
                .mnaExamenOsseuxOrdonne(mnaExamenOsseuxOrdonne)
                .naturalisationVoie(naturalisationVoie)
                .naturalisationDateRefus(naturalisationDateRefus)
                .recoursEnvisageDetecte(recoursEnvisageDetecte)
                .recoursDateJugementTA(recoursDateJugementTA)
                .assignationResidenceDetectee(assignationResidenceDetectee)
                .assignationDateNotification(assignationDateNotification)
                // SF-214-37 : F-IM-43 ITF judiciaire C. pén. 131-30 FR
                .itfJudiciaireDateCondamnation(itfJudiciaireDateCondamnation)
                .itfJudiciaireDureeAnnees(itfJudiciaireDureeAnnees)
                // SF-214-41 : F-IM-45 retrait de titre pour fraude L. 412-7 FR
                .retraitTitreFraudeDetecte(retraitTitreFraudeDetecte)
                .retraitTitreDateRetrait(retraitTitreDateRetrait)
                .retraitTitreMotif(retraitTitreMotif)
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
     * SF-246-28 : montant ≥ 0 (revenus, allocations — 0 est une valeur valide
     * pour un parent sans revenu). Retourne {@code null} si le champ est absent,
     * null, non numérique ou strictement négatif (valeur aberrante).
     */
    private static Double nonNegativeDoubleOrNull(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull() || !node.get(field).isNumber()) return null;
        double v = node.get(field).doubleValue();
        return v >= 0 ? v : null;
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
        // SF-246-11 : sous-objet `changement_etat_civil_detection` — date de naissance du
        // demandeur pour pré-fill F-FA-26 (changement d'état civil). Famille FR uniquement.
        // null hors FR / hors certitude (prompt impose null pour les dossiers BE).
        JsonNode cecd = node.get("changement_etat_civil_detection");
        String dateNaissanceDemandeurDetectee = (cecd != null && cecd.isObject())
                ? isoDateOrNull(cecd, "date_naissance_demandeur")
                : null;
        boolean cecDetectionPresent = dateNaissanceDemandeurDetectee != null;
        // SF-246-24 : sous-objet `succession_detection_v2` — 15 champs booléens/énumérés de
        // qualification juridique successorale pour pré-fill des 7 outils F-FA-24.
        // Complémentaire à `succession_detection` (SF-246-06) ; les deux coexistent.
        // Famille FR uniquement — prompt impose null pour dossiers BE.
        JsonNode sdv2 = node.get("succession_detection_v2");
        boolean sdv2Object = sdv2 != null && sdv2.isObject();
        String qualiteHeritierDetectee = sdv2Object
                ? whitelistedOrNull(stringOrNull(sdv2, "qualite_heritier"), "PREMIER_RANG", "SECOND_RANG")
                : null;
        Boolean actesEquivalentAcceptationDejaPosesDetected = sdv2Object
                ? booleanOrNull(sdv2, "actes_equivalent_acceptation_dejas_poses")
                : null;
        Boolean dettesIncertainesDetected = sdv2Object
                ? booleanOrNull(sdv2, "dettes_incertaines")
                : null;
        Boolean conjointSurvivantDetected = sdv2Object
                ? booleanOrNull(sdv2, "conjoint_survivant")
                : null;
        String qualiteDuDemandeurReserveDetecte = sdv2Object
                ? whitelistedOrNull(stringOrNull(sdv2, "qualite_du_demandeur_reserve"),
                        "HERITIER_RESERVATAIRE_DESCENDANT", "CONJOINT_SURVIVANT")
                : null;
        String qualiteHeritierRapportDetectee = sdv2Object
                ? whitelistedOrNull(stringOrNull(sdv2, "qualite_heritier_rapport"),
                        "DESCENDANT", "CONJOINT_SURVIVANT")
                : null;
        Boolean donationDispenseDeRapportDetected = sdv2Object
                ? booleanOrNull(sdv2, "donation_dispense_de_rapport")
                : null;
        Boolean naturePresumeeNonRapportableDetected = sdv2Object
                ? booleanOrNull(sdv2, "nature_presumee_non_rapportable")
                : null;
        Boolean tousDescendantsCommunsAvecConjointDetected = sdv2Object
                ? booleanOrNull(sdv2, "tous_descendants_communs_avec_conjoint")
                : null;
        String formeDonationDetectee = sdv2Object
                ? whitelistedOrNull(stringOrNull(sdv2, "forme_donation"),
                        "NOTARIEE", "MANUELLE", "INDIRECTE", "DEGUISEE")
                : null;
        Boolean saineDEspritDonateurDetected = sdv2Object
                ? booleanOrNull(sdv2, "saine_esprit_donateur")
                : null;
        Boolean respectQuotiteDisponibleDetected = sdv2Object
                ? booleanOrNull(sdv2, "respect_quotite_disponible")
                : null;
        String formeTestamentDetectee = sdv2Object
                ? whitelistedOrNull(stringOrNull(sdv2, "forme_testament"),
                        "OLOGRAPHE", "AUTHENTIQUE", "MYSTIQUE")
                : null;
        Boolean saineDEspritTestateurDetected = sdv2Object
                ? booleanOrNull(sdv2, "saine_esprit_testateur")
                : null;
        Boolean legsExcedeQuotiteDisponibleDetected = sdv2Object
                ? booleanOrNull(sdv2, "legs_excede_quotite_disponible")
                : null;
        boolean successionV2DetectionPresent = qualiteHeritierDetectee != null
                || actesEquivalentAcceptationDejaPosesDetected != null
                || dettesIncertainesDetected != null
                || conjointSurvivantDetected != null
                || qualiteDuDemandeurReserveDetecte != null
                || qualiteHeritierRapportDetectee != null
                || donationDispenseDeRapportDetected != null
                || naturePresumeeNonRapportableDetected != null
                || tousDescendantsCommunsAvecConjointDetected != null
                || formeDonationDetectee != null
                || saineDEspritDonateurDetected != null
                || respectQuotiteDisponibleDetected != null
                || formeTestamentDetectee != null
                || saineDEspritTestateurDetected != null
                || legsExcedeQuotiteDisponibleDetected != null;
        // SF-246-25 : sous-objet `communaute_partage_protection_detection_v2` — 17 champs
        // booléens/énumérés pour les 8 outils régimes & vie commune. Absent → tous null.
        // Listes via listOrNullWhitelisted() (jamais [] — invariant §5.1.2).
        java.util.Set<String> VIOLENCE_WHITELIST = java.util.Set.of(
                "PHYSIQUES", "PSYCHOLOGIQUES", "SEXUELLES", "ECONOMIQUES", "MENACES_MORT");
        java.util.Set<String> PREUVE_WHITELIST = java.util.Set.of(
                "CONSTAT_HUISSIER", "MAIN_COURANTE", "CERTIFICAT_MEDICAL",
                "TEMOIGNAGES", "PHOTOS", "PLAINTE_DEPOSEE", "JUGEMENT_CORRECTIONNEL", "AUTRE");
        java.util.Set<String> CREANCE_WHITELIST = java.util.Set.of(
                "CONTRIBUTION_DESEQUILIBRE", "INVESTISSEMENT_BIEN_PROPRE",
                "ENRICHISSEMENT_INJUSTE", "PRESTATION_TRAVAIL_NON_REMUNEREE", "AUCUNE");
        java.util.Set<String> MODE_PACS_WHITELIST = java.util.Set.of(
                "DECLARATION_UNILATERALE", "DECLARATION_CONJOINTE",
                "MARIAGE_PARTENAIRES", "MARIAGE_TIERS", "DECES");
        java.util.Set<String> REGIME_PACS_WHITELIST = java.util.Set.of(
                "SEPARATION_BIENS", "INDIVISION_AMENAGEE", "INDIVISION_PAR_DEFAUT");
        JsonNode cppd = node.get("communaute_partage_protection_detection_v2");
        boolean cppdObject = cppd != null && cppd.isObject();
        Boolean contratNotarieDetected = cppdObject ? booleanOrNull(cppd, "contrat_notarie") : null;
        Boolean enfantsNonCommunsDetected = cppdObject ? booleanOrNull(cppd, "enfants_non_communs") : null;
        Boolean clauseAttributionIntegraleDetected = cppdObject ? booleanOrNull(cppd, "clause_attribution_integrale") : null;
        Boolean pvDifficultesEtablisDetected = cppdObject ? booleanOrNull(cppd, "pv_difficultes_etablis") : null;
        Boolean tentativeAmiableEpuiseueeDetected = cppdObject ? booleanOrNull(cppd, "tentative_amiable_epuisee") : null;
        java.util.List<String> violencesAllegueesDetectees = cppdObject
                ? listOrNullWhitelisted(cppd, "violences_alleguees", VIOLENCE_WHITELIST) : null;
        java.util.List<String> preuvesViolencesDetectees = cppdObject
                ? listOrNullWhitelisted(cppd, "preuves_violences", PREUVE_WHITELIST) : null;
        Boolean dangerImmediatDetected = cppdObject ? booleanOrNull(cppd, "danger_immediat") : null;
        Boolean presenceEnfantsDetected = cppdObject ? booleanOrNull(cppd, "presence_enfants") : null;
        Boolean logementCommunDetected = cppdObject ? booleanOrNull(cppd, "logement_commun") : null;
        Boolean victimeFinanciairementDependanteDetected = cppdObject ? booleanOrNull(cppd, "victime_financierement_dependante") : null;
        String modeDissolutionPacsDetecte = cppdObject
                ? whitelistedOrNull(stringOrNull(cppd, "mode_dissolution_pacs"), MODE_PACS_WHITELIST.toArray(String[]::new)) : null;
        String regimeBiensPacsDetecte = cppdObject
                ? whitelistedOrNull(stringOrNull(cppd, "regime_biens_pacs"), REGIME_PACS_WHITELIST.toArray(String[]::new)) : null;
        java.util.List<String> creancesAllegueesDetectees = cppdObject
                ? listOrNullWhitelisted(cppd, "creances_alleguees", CREANCE_WHITELIST) : null;
        Boolean patrimoineCommunSignificatifDetecte = cppdObject ? booleanOrNull(cppd, "patrimoine_commun_significatif") : null;
        Boolean patrimoineCommun = cppdObject ? booleanOrNull(cppd, "patrimoine_commun_bool") : null;
        Boolean violencesAlleguees = cppdObject ? booleanOrNull(cppd, "violences_alleguees_bool") : null;
        // SF-246-26 : sous-objet `filiation_detection_v2` — 12 champs D2 filiation / adoption.
        // Complément du sous-objet `filiation_detection` (SF-246-09) qui porte les dates/âges ;
        // ce sous-objet porte les QUALIFICATIONS JURIDIQUES booléennes et énumérées.
        // France uniquement : sous-objet null si dossier belge (prompt le garantit).
        java.util.Set<String> QUALITE_AAGIR_WHITELIST = java.util.Set.of(
                "PERE_DECLARE", "PERE_BIOLOGIQUE_PRESUME", "MERE", "ENFANT_MAJEUR");
        java.util.Set<String> QUALITE_DEMANDEUR_RECHERCHE_WHITELIST = java.util.Set.of(
                "ENFANT_MAJEUR", "REPRESENTANT_LEGAL_MINEUR", "MERE");
        java.util.Set<String> FORME_ADOPTION_WHITELIST = java.util.Set.of("PLENIERE", "SIMPLE");
        JsonNode fdv2 = node.get("filiation_detection_v2");
        boolean fdv2Object = fdv2 != null && fdv2.isObject();
        String qualiteAagirContestationDetected = fdv2Object
                ? whitelistedOrNull(stringOrNull(fdv2, "qualite_aagir_contestation"),
                        QUALITE_AAGIR_WHITELIST.toArray(String[]::new)) : null;
        Boolean possessionEtatConforme5AnsDetected = fdv2Object ? booleanOrNull(fdv2, "possession_etat_conforme_5ans") : null;
        Boolean expertiseAdnDemandeeDetected = fdv2Object ? booleanOrNull(fdv2, "expertise_adn_demandee_contestation") : null;
        Boolean motifsSerieuxDetected = fdv2Object ? booleanOrNull(fdv2, "motifs_serieux_contestation") : null;
        String qualiteDuDemandeurRechercheDetected = fdv2Object
                ? whitelistedOrNull(stringOrNull(fdv2, "qualite_demandeur_recherche"),
                        QUALITE_DEMANDEUR_RECHERCHE_WHITELIST.toArray(String[]::new)) : null;
        Boolean presomptionPossessionEtatRechercheDetected = fdv2Object ? booleanOrNull(fdv2, "presomption_possession_etat_recherche") : null;
        Boolean expertiseAdnDemandeeRechercheDetected = fdv2Object ? booleanOrNull(fdv2, "expertise_adn_demandee_recherche") : null;
        Boolean pereDesigneRefuseADNDetected = fdv2Object ? booleanOrNull(fdv2, "pere_designe_refuse_adn") : null;
        Boolean motifsSerieuxRechercheDetected = fdv2Object ? booleanOrNull(fdv2, "motifs_serieux_recherche") : null;
        String formeAdoptionDemandeeDetected = fdv2Object
                ? whitelistedOrNull(stringOrNull(fdv2, "forme_adoption_demandee"),
                        FORME_ADOPTION_WHITELIST.toArray(String[]::new)) : null;
        Boolean pupilleEtatDetected = fdv2Object ? booleanOrNull(fdv2, "pupille_etat") : null;
        Boolean adoptantMarieDetected = fdv2Object ? booleanOrNull(fdv2, "adoptant_marie") : null;
        boolean filiationV2DetectionPresent = qualiteAagirContestationDetected != null
                || possessionEtatConforme5AnsDetected != null
                || expertiseAdnDemandeeDetected != null
                || motifsSerieuxDetected != null
                || qualiteDuDemandeurRechercheDetected != null
                || presomptionPossessionEtatRechercheDetected != null
                || expertiseAdnDemandeeRechercheDetected != null
                || pereDesigneRefuseADNDetected != null
                || motifsSerieuxRechercheDetected != null
                || formeAdoptionDemandeeDetected != null
                || pupilleEtatDetected != null
                || adoptantMarieDetected != null;
        // SF-246-27 : sous-objet `protection_divorce_detection_v2` — 8 champs D2/D3.
        // majeurs-proteges (2), pma-gpa-bioethique (3), mediation-familiale (1),
        // divorce-accepte + divorce-alteration (1 partagé), divorce-dc-be (1, BE uniquement).
        java.util.Set<String> REGIME_PROTECTION_WHITELIST = java.util.Set.of(
                "SAUVEGARDE_JUSTICE", "HABILITATION_FAMILIALE",
                "CURATELLE_SIMPLE", "CURATELLE_RENFORCEE",
                "TUTELLE", "MANDAT_PROTECTION_FUTURE");
        java.util.Set<String> MOTIF_MEDIATION_WHITELIST = java.util.Set.of(
                "AUTORITE_PARENTALE", "CONTRIBUTION_ENTRETIEN",
                "DROIT_VISITE", "RESIDENCE", "AUTRE");
        JsonNode pdv2 = node.get("protection_divorce_detection_v2");
        boolean pdv2Object = pdv2 != null && pdv2.isObject();
        String regimeProtectionMajeursDetected = pdv2Object
                ? whitelistedOrNull(stringOrNull(pdv2, "regime_protection_majeurs"),
                        REGIME_PROTECTION_WHITELIST.toArray(String[]::new)) : null;
        String dateCertificatMedicalMajeursDetected = pdv2Object ? isoDateOrNull(pdv2, "date_certificat_medical_majeurs") : null;
        String datePmaDetected = pdv2Object ? isoDateOrNull(pdv2, "date_pma") : null;
        String dateReconnaissanceAnterieurePmaDetected = pdv2Object ? isoDateOrNull(pdv2, "date_reconnaissance_anterieure_pma") : null;
        String dateDonGametesDetected = pdv2Object ? isoDateOrNull(pdv2, "date_don_gametes") : null;
        String motifSaisineMediationDetected = pdv2Object
                ? whitelistedOrNull(stringOrNull(pdv2, "motif_saisine_mediation"),
                        MOTIF_MEDIATION_WHITELIST.toArray(String[]::new)) : null;
        String dateAssignationDivorce = pdv2Object ? isoDateOrNull(pdv2, "date_assignation_divorce") : null;
        String dateAudienceHomologationDcBe = pdv2Object ? isoDateOrNull(pdv2, "date_audience_homologation_dc_be") : null;
        boolean protectionDivorceV2Present = regimeProtectionMajeursDetected != null
                || dateCertificatMedicalMajeursDetected != null
                || datePmaDetected != null
                || dateReconnaissanceAnterieurePmaDetected != null
                || dateDonGametesDetected != null
                || motifSaisineMediationDetected != null
                || dateAssignationDivorce != null
                || dateAudienceHomologationDcBe != null;
        // SF-246-12 : sous-objet `divorce_ddi_be_detection` — date de séparation effective
        // pour pré-fill F-FA-11 (divorce-desunion-be). BELGIQUE UNIQUEMENT, nullable.
        // Distincte de `dateSeparation` FR (SF-246-08) — invariant cadrage §5.1.1.
        // CJ art. 1255 — point de départ du délai de 6 mois (voie consensuelle)
        // ou 1 an (voie unilatérale). Dossier FR → null (prompt impose null hors BE).
        JsonNode ddiBe = node.get("divorce_ddi_be_detection");
        String dateSeparationBe = (ddiBe != null && ddiBe.isObject())
                ? isoDateOrNull(ddiBe, "date_separation_be")
                : null;
        // SF-246-28 : sous-objet `famille_be_detection_v2` — 16 champs D3 Famille BE.
        // Lève le PREFILL_COUNT_ALWAYS_ZERO sur 5 outils : autorite-parentale-be,
        // contribution-alimentaire-enfants-be, contribution-conjoint-be,
        // liquidation-partage-be, regime-communaute-legale-be. BELGIQUE UNIQUEMENT,
        // tous nullables — le prompt impose null hors BE / hors certitude.
        // Montants/revenus via nonNegativeDoubleOrNull() (0 est une valeur valide pour
        // un revenu nul), nuits via boundedIntOrNull(0,30), durée mariage via
        // boundedIntOrNull(0,80), nombre enfants via boundedIntOrNull(1,12).
        java.util.Set<String> MODE_HEBERGEMENT_BE_WHITELIST = java.util.Set.of(
                "HEBERGEMENT_EGALITAIRE",
                "HEBERGEMENT_PRINCIPAL_UN_PARENT",
                "HEBERGEMENT_NON_FIXE");
        JsonNode fbv2 = node.get("famille_be_detection_v2");
        boolean fbv2Object = fbv2 != null && fbv2.isObject();
        // --- autorite-parentale-be ---
        String modeHebergementPrincipalBeDetecte = fbv2Object
                ? whitelistedOrNull(stringOrNull(fbv2, "mode_hebergement_principal_be"),
                        MODE_HEBERGEMENT_BE_WHITELIST.toArray(String[]::new))
                : null;
        // --- contribution-alimentaire-enfants-be ---
        Integer nombreEnfantsBeDetecte = fbv2Object ? boundedIntOrNull(fbv2, "nombre_enfants_be", 1, 12) : null;
        Double revenuMensuelParent1BeDetecte = fbv2Object ? nonNegativeDoubleOrNull(fbv2, "revenu_mensuel_parent1_be") : null;
        Double revenuMensuelParent2BeDetecte = fbv2Object ? nonNegativeDoubleOrNull(fbv2, "revenu_mensuel_parent2_be") : null;
        Double allocationsFamilialesMensuellesBeDetectees = fbv2Object ? nonNegativeDoubleOrNull(fbv2, "allocations_familiales_be") : null;
        Integer nuitsHebergementParent1BeDetectees = fbv2Object ? boundedIntOrNull(fbv2, "nuits_hebergement_parent1_be", 0, 30) : null;
        Integer nuitsHebergementParent2BeDetectees = fbv2Object ? boundedIntOrNull(fbv2, "nuits_hebergement_parent2_be", 0, 30) : null;
        // --- contribution-conjoint-be ---
        Integer dureeMariageAnneesBeDetectee = fbv2Object ? boundedIntOrNull(fbv2, "duree_mariage_annees_be", 0, 80) : null;
        Double revenuMensuelCreancierBeDetecte = fbv2Object ? nonNegativeDoubleOrNull(fbv2, "revenu_mensuel_creancier_be") : null;
        Double revenuMensuelDebiteurBeDetecte = fbv2Object ? nonNegativeDoubleOrNull(fbv2, "revenu_mensuel_debiteur_be") : null;
        // --- liquidation-partage-be ---
        String dateDesignationNotaireBeDetectee = fbv2Object ? isoDateOrNull(fbv2, "date_designation_notaire_be") : null;
        String dateOuvertureOperationsBeDetectee = fbv2Object ? isoDateOrNull(fbv2, "date_ouverture_operations_be") : null;
        String dateNotificationProjetBeDetectee = fbv2Object ? isoDateOrNull(fbv2, "date_notification_projet_be") : null;
        String dateHomologationBeDetectee = fbv2Object ? isoDateOrNull(fbv2, "date_homologation_be") : null;
        // --- regime-communaute-legale-be ---
        String dateMariageBeDetectee = fbv2Object ? isoDateOrNull(fbv2, "date_mariage_be") : null;
        Boolean contratMariageSigneBeDetecte = fbv2Object ? booleanOrNull(fbv2, "contrat_mariage_signe_be") : null;
        boolean familleBev2Present = modeHebergementPrincipalBeDetecte != null
                || nombreEnfantsBeDetecte != null
                || revenuMensuelParent1BeDetecte != null
                || revenuMensuelParent2BeDetecte != null
                || allocationsFamilialesMensuellesBeDetectees != null
                || nuitsHebergementParent1BeDetectees != null
                || nuitsHebergementParent2BeDetectees != null
                || dureeMariageAnneesBeDetectee != null
                || revenuMensuelCreancierBeDetecte != null
                || revenuMensuelDebiteurBeDetecte != null
                || dateDesignationNotaireBeDetectee != null
                || dateOuvertureOperationsBeDetectee != null
                || dateNotificationProjetBeDetectee != null
                || dateHomologationBeDetectee != null
                || dateMariageBeDetectee != null
                || contratMariageSigneBeDetecte != null;
        // SF-216-01 : 6 champs IA prestation compensatoire + vie commune FR.
        // `duree_mariage_annees_fr` et `revenus_annuels_epoux1/2_eur` étendent le
        // sous-objet `vie_commune_detection` déjà présent (SF-246-08).
        // `prestation_compensatoire_detection.envisagee` = flag CONTEXTUAL.
        // FRANCE UNIQUEMENT — null si dossier BE.
        Integer dureeMariageAnnesFr = vcdObject ? boundedIntOrNull(vcd, "duree_mariage_annees_fr", 0, 80) : null;
        Double revenusAnnuelsEpoux1Fr = vcdObject ? nonNegativeDoubleOrNull(vcd, "revenus_annuels_epoux1_eur") : null;
        Double revenusAnnuelsEpoux2Fr = vcdObject ? nonNegativeDoubleOrNull(vcd, "revenus_annuels_epoux2_eur") : null;
        Integer ageEpoux1AnneesFr = vcdObject ? boundedIntOrNull(vcd, "age_epoux1_annees", 0, 120) : null;
        Integer ageEpoux2AnneesFr = vcdObject ? boundedIntOrNull(vcd, "age_epoux2_annees", 0, 120) : null;
        JsonNode pcd = node.get("prestation_compensatoire_detection");
        Boolean prestationCompensatoireEnvisagee = (pcd != null && pcd.isObject())
                ? booleanOrNull(pcd, "envisagee") : null;
        boolean sf216_01Present = dureeMariageAnnesFr != null
                || revenusAnnuelsEpoux1Fr != null
                || revenusAnnuelsEpoux2Fr != null
                || ageEpoux1AnneesFr != null
                || ageEpoux2AnneesFr != null
                || prestationCompensatoireEnvisagee != null;
        // SF-216-05 : sous-objet `liquidation_communaute_detection` — 3 champs IA
        // pour la liquidation de communauté légale FR (art. 1467 + 1433 Cciv).
        // FRANCE UNIQUEMENT — null si dossier BE.
        JsonNode lcd = node.get("liquidation_communaute_detection");
        boolean lcdObject = lcd != null && lcd.isObject();
        Boolean liquidationCommunauteEnvisagee = lcdObject ? booleanOrNull(lcd, "envisagee") : null;
        Integer recompensesEpoux1Eur = lcdObject ? nonNegativeIntOrNull(lcd, "recompenses_epoux1_eur") : null;
        Integer recompensesEpoux2Eur = lcdObject ? nonNegativeIntOrNull(lcd, "recompenses_epoux2_eur") : null;
        boolean sf216_05Present = liquidationCommunauteEnvisagee != null
                || recompensesEpoux1Eur != null
                || recompensesEpoux2Eur != null;
        // SF-216-07 : sous-objet `aripa_recouvrement_detection` — 3 champs IA
        // pour l'outil ARIPA recouvrement pension alimentaire impayee FR (art. L. 581 CSS).
        // FRANCE UNIQUEMENT — null si dossier BE.
        JsonNode ard = node.get("aripa_recouvrement_detection");
        boolean ardObject = ard != null && ard.isObject();
        Boolean aripaRecouvrementEnvisage = ardObject ? booleanOrNull(ard, "envisage") : null;
        Integer montantPensionMensuelleDueEur = ardObject
                ? nonNegativeIntOrNull(ard, "montant_pension_mensuelle_due_eur") : null;
        Boolean titreExecutoireDetecte = ardObject ? booleanOrNull(ard, "titre_executoire_detecte") : null;
        boolean sf216_07Present = aripaRecouvrementEnvisage != null
                || montantPensionMensuelleDueEur != null
                || titreExecutoireDetecte != null;
        // SF-216-03 : sous-objet `pension_alimentaire_detection` — 2 champs IA
        // pour la pension alimentaire enfant FR (art. 371-2 Cciv + barème Cass.).
        // FRANCE UNIQUEMENT — null si dossier BE.
        JsonNode pad = node.get("pension_alimentaire_detection");
        boolean padObject = pad != null && pad.isObject();
        Boolean pensionAlimentaireEnvisagee = padObject ? booleanOrNull(pad, "envisagee") : null;
        String modeResidenceEnfantsDetecte = padObject
                ? normalizeEnumCode(textOrNull(pad, "mode_residence"), MODE_RESIDENCE_ENFANT_WHITELIST)
                : null;
        boolean sf216_03Present = pensionAlimentaireEnvisagee != null
                || modeResidenceEnfantsDetecte != null;
        // SF-216-09 : sous-objet `delegation_ap_detection` — 3 champs IA pour la
        // délégation autorité parentale FR (art. 376-1 Cciv).
        // FRANCE UNIQUEMENT — null si dossier BE.
        JsonNode dad = node.get("delegation_ap_detection");
        boolean dadObject = dad != null && dad.isObject();
        Boolean delegationApEnvisagee = dadObject ? booleanOrNull(dad, "envisagee") : null;
        String tiersLienFamilialDetecte = dadObject
                ? normalizeEnumCode(textOrNull(dad, "tiers_lien_familial"), TIERS_LIEN_FAMILIAL_WHITELIST)
                : null;
        Boolean accordParentsDetecte = dadObject ? booleanOrNull(dad, "accord_parents") : null;
        boolean sf216_09Present = delegationApEnvisagee != null
                || tiersLienFamilialDetecte != null
                || accordParentsDetecte != null;
        // SF-216-11 : sous-objet `retrait_ap_detection` — 3 champs IA pour le
        // retrait autorité parentale FR (art. 378-381 Cciv + loi 2022-140 LMVSS).
        // FRANCE UNIQUEMENT — null si dossier BE.
        JsonNode rad = node.get("retrait_ap_detection");
        boolean radObject = rad != null && rad.isObject();
        Boolean retraitApEnvisage = radObject ? booleanOrNull(rad, "envisage") : null;
        Boolean condamnationPenaleDetectee = radObject
                ? booleanOrNull(rad, "condamnation_penale_detectee") : null;
        Boolean violencesLmvss2022Detectees = radObject
                ? booleanOrNull(rad, "violences_lmvss_2022_detectees") : null;
        boolean sf216_11Present = retraitApEnvisage != null
                || condamnationPenaleDetectee != null
                || violencesLmvss2022Detectees != null;
        // SF-216-15 : sous-objet `adoption_intra_detection` — 1 champ IA pour
        // l'adoption de l'enfant du conjoint (adoption intra-familiale, art. 345-1 Cciv).
        // FRANCE UNIQUEMENT — null si dossier BE.
        JsonNode aid = node.get("adoption_intra_detection");
        boolean aidObject = aid != null && aid.isObject();
        Boolean adoptionIntraEnvisagee = aidObject ? booleanOrNull(aid, "envisagee") : null;
        boolean sf216_15Present = adoptionIntraEnvisagee != null;
        // SF-216-17 : sous-objet `adoption_internationale_detection` — 4 champs IA
        // pour l'adoption internationale FR (art. 370-3 à 370-5 Cciv + Convention
        // La Haye 1993). FRANCE UNIQUEMENT — null si dossier BE.
        JsonNode aint = node.get("adoption_internationale_detection");
        boolean aintObject = aint != null && aint.isObject();
        Boolean adoptionInternationaleEnvisagee = aintObject ? booleanOrNull(aint, "envisagee") : null;
        String paysOrigineAdopteDetecte = aintObject ? textOrNull(aint, "pays_origine") : null;
        Boolean agrement2025DetecteValide = aintObject ? booleanOrNull(aint, "agrement_valide") : null;
        Boolean exequaturRequisDetecte = aintObject ? booleanOrNull(aint, "exequatur_requis") : null;
        boolean sf216_17Present = adoptionInternationaleEnvisagee != null
                || paysOrigineAdopteDetecte != null
                || agrement2025DetecteValide != null
                || exequaturRequisDetecte != null;
        // SF-216-13 : sous-objet `audition_mineur_detection` — 2 champs IA pour
        // l'audition du mineur par le JAF FR (art. 388-1 Cciv + art. 1074-1
        // à 1074-3 CPC + CIDE art. 12). FRANCE UNIQUEMENT — null si dossier BE.
        JsonNode amd = node.get("audition_mineur_detection");
        boolean amdObject = amd != null && amd.isObject();
        Boolean auditionMineurEnvisagee = amdObject ? booleanOrNull(amd, "envisagee") : null;
        Boolean demandeAuditionFormaliseeDetectee = amdObject
                ? booleanOrNull(amd, "demande_formalisee_detectee") : null;
        boolean sf216_13Present = auditionMineurEnvisagee != null
                || demandeAuditionFormaliseeDetectee != null;
        // SF-216-19 : sous-objet `indignite_successorale_detection` — 3 champs IA
        // pour l'indignité successorale FR (art. 726-729-1 Cciv + Loi 2022-1617).
        // FRANCE UNIQUEMENT — null si dossier BE.
        JsonNode indSucc = node.get("indignite_successorale_detection");
        boolean indSuccObject = indSucc != null && indSucc.isObject();
        Boolean indigniteSuccessoraleEnvisagee = indSuccObject ? booleanOrNull(indSucc, "envisagee") : null;
        Boolean condamnationPenaleSuccessionDetectee = indSuccObject
                ? booleanOrNull(indSucc, "condamnation_penale_detectee") : null;
        Boolean pardonTestamentaireDetecte = indSuccObject
                ? booleanOrNull(indSucc, "pardon_testamentaire") : null;
        boolean sf216_19Present = indigniteSuccessoraleEnvisagee != null
                || condamnationPenaleSuccessionDetectee != null
                || pardonTestamentaireDetecte != null;
        // SF-216-21 : sous-objet `recel_succession_detection` — 3 champs IA
        // pour le recel successoral FR (art. 778 Cciv + Cass. 1ère civ.,
        // 14/11/2012). FRANCE UNIQUEMENT — null si dossier BE.
        JsonNode recelSuccNode = node.get("recel_succession_detection");
        boolean recelSuccObject = recelSuccNode != null && recelSuccNode.isObject();
        Boolean recelSuccessoralEnvisage = recelSuccObject ? booleanOrNull(recelSuccNode, "envisage") : null;
        String typeRecelDetecte = recelSuccObject ? textOrNull(recelSuccNode, "type_recel") : null;
        String preuveRecelDetectee = recelSuccObject ? textOrNull(recelSuccNode, "preuve_recel") : null;
        boolean sf216_21Present = recelSuccessoralEnvisage != null
                || typeRecelDetecte != null
                || preuveRecelDetectee != null;
        // SF-216-23 : sous-objet `donation_entre_epoux_detection` — 3 champs IA
        // pour la donation entre époux FR (art. 1091-1100 Cciv + art. 265 al. 2
        // + art. 1527 al. 2 + art. 912-928).
        // FRANCE UNIQUEMENT — null si dossier BE.
        JsonNode donEp = node.get("donation_entre_epoux_detection");
        boolean donEpObject = donEp != null && donEp.isObject();
        Boolean donationEntreEpouxEnvisagee = donEpObject ? booleanOrNull(donEp, "envisagee") : null;
        Boolean revocabiliteDetectee = donEpObject
                ? booleanOrNull(donEp, "revocabilite_detectee") : null;
        String bienDonnePrincipalType = donEpObject
                ? textOrNull(donEp, "bien_donne_principal_type") : null;
        boolean sf216_23Present = donationEntreEpouxEnvisagee != null
                || revocabiliteDetectee != null
                || bienDonnePrincipalType != null;
        // SF-216-27 : sous-objet `partage_notarial_detection` — 3 champs IA
        // pour le partage successoral notarié FR (art. 816 et s. Cciv +
        // art. 870 Cciv + art. 1592 CGI + art. 641 CGI + art. 840 Cciv).
        // FRANCE UNIQUEMENT — null si dossier BE.
        JsonNode partageNotNode = node.get("partage_notarial_detection");
        boolean partageNotObject = partageNotNode != null && partageNotNode.isObject();
        Boolean partageNotarialEnvisage = partageNotObject
                ? booleanOrNull(partageNotNode, "envisage") : null;
        Boolean presenceImmeubleSuccessionDetecte = partageNotObject
                ? booleanOrNull(partageNotNode, "presence_immeuble") : null;
        String declarationSuccessionEcheancDetectee = partageNotObject
                ? isoDateOrNull(partageNotNode, "declaration_succession_echeance") : null;
        boolean sf216_27Present = partageNotarialEnvisage != null
                || presenceImmeubleSuccessionDetecte != null
                || declarationSuccessionEcheancDetectee != null;
        // SF-216-25 : sous-objet `presomption_paternite_detection` — 4 champs IA
        // pour la présomption de paternité du mari FR (art. 312-316 Cciv +
        // art. 333 al. 1 + Cass. 1ère civ., 19/2/2014).
        // FRANCE UNIQUEMENT — null si dossier BE.
        JsonNode presPat = node.get("presomption_paternite_detection");
        boolean presPatObject = presPat != null && presPat.isObject();
        Boolean presomptionPaterniteEnvisagee = presPatObject
                ? booleanOrNull(presPat, "envisagee") : null;
        Boolean desaveuEnvisage = presPatObject
                ? booleanOrNull(presPat, "desaveu_envisage") : null;
        String dateConclusionMariageDetectee = presPatObject
                ? textOrNull(presPat, "date_conclusion_mariage") : null;
        String dateDissolutionMariageDetectee = presPatObject
                ? textOrNull(presPat, "date_dissolution_mariage") : null;
        boolean sf216_25Present = presomptionPaterniteEnvisagee != null
                || desaveuEnvisage != null
                || dateConclusionMariageDetectee != null
                || dateDissolutionMariageDetectee != null;
        // SF-216-29 : sous-objet `donation_partage_detection` — 3 champs IA
        // pour la donation-partage FR (art. 1075 à 1075-5 Cciv + art. 1078,
        // 1078-1, 1080 + art. 912-928).
        // FRANCE UNIQUEMENT — null si dossier BE.
        JsonNode donPartage = node.get("donation_partage_detection");
        boolean donPartageObject = donPartage != null && donPartage.isObject();
        Boolean donationPartageEnvisagee = donPartageObject
                ? booleanOrNull(donPartage, "envisagee") : null;
        Boolean presencePetitsEnfantsSubstitutionDetectee = donPartageObject
                ? booleanOrNull(donPartage, "petits_enfants_substitution") : null;
        Boolean donationPartageConjonctiveDetectee = donPartageObject
                ? booleanOrNull(donPartage, "conjonctive") : null;
        boolean sf216_29Present = donationPartageEnvisagee != null
                || presencePetitsEnfantsSubstitutionDetectee != null
                || donationPartageConjonctiveDetectee != null;

        boolean communautePartageProtectionV2Present = contratNotarieDetected != null
                || enfantsNonCommunsDetected != null
                || clauseAttributionIntegraleDetected != null
                || pvDifficultesEtablisDetected != null
                || tentativeAmiableEpuiseueeDetected != null
                || violencesAllegueesDetectees != null
                || preuvesViolencesDetectees != null
                || dangerImmediatDetected != null
                || presenceEnfantsDetected != null
                || logementCommunDetected != null
                || victimeFinanciairementDependanteDetected != null
                || modeDissolutionPacsDetecte != null
                || regimeBiensPacsDetecte != null
                || creancesAllegueesDetectees != null
                || patrimoineCommunSignificatifDetecte != null
                || patrimoineCommun != null
                || violencesAlleguees != null;

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
                && !divorceFauteDetectionPresent
                && !cecDetectionPresent
                && !successionV2DetectionPresent
                && !communautePartageProtectionV2Present
                && !filiationV2DetectionPresent
                && !protectionDivorceV2Present
                && dateSeparationBe == null
                && !familleBev2Present
                && !sf216_01Present
                && !sf216_05Present
                && !sf216_07Present
                && !sf216_03Present
                && !sf216_09Present
                && !sf216_11Present
                && !sf216_15Present
                && !sf216_17Present
                && !sf216_13Present
                && !sf216_19Present
                && !sf216_21Present
                && !sf216_23Present
                && !sf216_27Present
                && !sf216_25Present
                && !sf216_29Present) {
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
                // SF-246-11 : date de naissance demandeur pour pré-fill F-FA-26.
                .dateNaissanceDemandeurDetectee(dateNaissanceDemandeurDetectee)
                // SF-246-24 : 15 champs booléens/énumérés de qualification juridique
                // successorale — sous-objet `succession_detection_v2`.
                .qualiteHeritierDetectee(qualiteHeritierDetectee)
                .actesEquivalentAcceptationDejaPosesDetected(actesEquivalentAcceptationDejaPosesDetected)
                .dettesIncertainesDetected(dettesIncertainesDetected)
                .conjointSurvivantDetected(conjointSurvivantDetected)
                .qualiteDuDemandeurReserveDetecte(qualiteDuDemandeurReserveDetecte)
                .qualiteHeritierRapportDetectee(qualiteHeritierRapportDetectee)
                .donationDispenseDeRapportDetected(donationDispenseDeRapportDetected)
                .naturePresumeeNonRapportableDetected(naturePresumeeNonRapportableDetected)
                .tousDescendantsCommunsAvecConjointDetected(tousDescendantsCommunsAvecConjointDetected)
                .formeDonationDetectee(formeDonationDetectee)
                .saineDEspritDonateurDetected(saineDEspritDonateurDetected)
                .respectQuotiteDisponibleDetected(respectQuotiteDisponibleDetected)
                .formeTestamentDetectee(formeTestamentDetectee)
                .saineDEspritTestateurDetected(saineDEspritTestateurDetected)
                .legsExcedeQuotiteDisponibleDetected(legsExcedeQuotiteDisponibleDetected)
                // SF-246-25 : 17 champs booléens/énumérés D2 régimes & vie commune.
                .contratNotarieDetected(contratNotarieDetected)
                .enfantsNonCommunsDetected(enfantsNonCommunsDetected)
                .clauseAttributionIntegraleDetected(clauseAttributionIntegraleDetected)
                .pvDifficultesEtablisDetected(pvDifficultesEtablisDetected)
                .tentativeAmiableEpuiseueeDetected(tentativeAmiableEpuiseueeDetected)
                .violencesAllegueesDetectees(violencesAllegueesDetectees)
                .preuvesViolencesDetectees(preuvesViolencesDetectees)
                .dangerImmediatDetected(dangerImmediatDetected)
                .presenceEnfantsDetected(presenceEnfantsDetected)
                .logementCommunDetected(logementCommunDetected)
                .victimeFinanciairementDependanteDetected(victimeFinanciairementDependanteDetected)
                .modeDissolutionPacsDetecte(modeDissolutionPacsDetecte)
                .regimeBiensPacsDetecte(regimeBiensPacsDetecte)
                .creancesAllegueesDetectees(creancesAllegueesDetectees)
                .patrimoineCommunSignificatifDetecte(patrimoineCommunSignificatifDetecte)
                .patrimoineCommun(patrimoineCommun)
                .violencesAlleguees(violencesAlleguees)
                // SF-246-26 : 12 champs D2 filiation / adoption.
                .qualiteAagirContestationDetected(qualiteAagirContestationDetected)
                .possessionEtatConforme5AnsDetected(possessionEtatConforme5AnsDetected)
                .expertiseAdnDemandeeDetected(expertiseAdnDemandeeDetected)
                .motifsSerieuxDetected(motifsSerieuxDetected)
                .qualiteDuDemandeurRechercheDetected(qualiteDuDemandeurRechercheDetected)
                .presomptionPossessionEtatRechercheDetected(presomptionPossessionEtatRechercheDetected)
                .expertiseAdnDemandeeRechercheDetected(expertiseAdnDemandeeRechercheDetected)
                .pereDesigneRefuseADNDetected(pereDesigneRefuseADNDetected)
                .motifsSerieuxRechercheDetected(motifsSerieuxRechercheDetected)
                .formeAdoptionDemandeeDetected(formeAdoptionDemandeeDetected)
                .pupilleEtatDetected(pupilleEtatDetected)
                .adoptantMarieDetected(adoptantMarieDetected)
                // SF-246-27 : 8 champs IA protection majeurs / PMA / médiation / divorce.
                .regimeProtectionMajeursDetected(regimeProtectionMajeursDetected)
                .dateCertificatMedicalMajeursDetected(dateCertificatMedicalMajeursDetected)
                .datePmaDetected(datePmaDetected)
                .dateReconnaissanceAnterieurePmaDetected(dateReconnaissanceAnterieurePmaDetected)
                .dateDonGametesDetected(dateDonGametesDetected)
                .motifSaisineMediationDetected(motifSaisineMediationDetected)
                .dateAssignationDivorce(dateAssignationDivorce)
                .dateAudienceHomologationDcBe(dateAudienceHomologationDcBe)
                // SF-246-12 : date de séparation effective BE (divorce-desunion-be).
                .dateSeparationBe(dateSeparationBe)
                // SF-246-28 : 16 champs IA Famille BE — levée PREFILL_COUNT_ALWAYS_ZERO (BELGIQUE UNIQUEMENT).
                .modeHebergementPrincipalBeDetecte(modeHebergementPrincipalBeDetecte)
                .nombreEnfantsBeDetecte(nombreEnfantsBeDetecte)
                .revenuMensuelParent1BeDetecte(revenuMensuelParent1BeDetecte)
                .revenuMensuelParent2BeDetecte(revenuMensuelParent2BeDetecte)
                .allocationsFamilialesMensuellesBeDetectees(allocationsFamilialesMensuellesBeDetectees)
                .nuitsHebergementParent1BeDetectees(nuitsHebergementParent1BeDetectees)
                .nuitsHebergementParent2BeDetectees(nuitsHebergementParent2BeDetectees)
                .dureeMariageAnneesBeDetectee(dureeMariageAnneesBeDetectee)
                .revenuMensuelCreancierBeDetecte(revenuMensuelCreancierBeDetecte)
                .revenuMensuelDebiteurBeDetecte(revenuMensuelDebiteurBeDetecte)
                .dateDesignationNotaireBeDetectee(dateDesignationNotaireBeDetectee)
                .dateOuvertureOperationsBeDetectee(dateOuvertureOperationsBeDetectee)
                .dateNotificationProjetBeDetectee(dateNotificationProjetBeDetectee)
                .dateHomologationBeDetectee(dateHomologationBeDetectee)
                .dateMariageBeDetectee(dateMariageBeDetectee)
                .contratMariageSigneBeDetecte(contratMariageSigneBeDetecte)
                // SF-216-01 : 6 champs IA prestation compensatoire + vie commune FR.
                .dureeMariageAnnees(dureeMariageAnnesFr)
                .revenusAnnuelsEpoux1(revenusAnnuelsEpoux1Fr)
                .revenusAnnuelsEpoux2(revenusAnnuelsEpoux2Fr)
                .ageEpoux1Annees(ageEpoux1AnneesFr)
                .ageEpoux2Annees(ageEpoux2AnneesFr)
                .prestationCompensatoireEnvisagee(prestationCompensatoireEnvisagee)
                // SF-216-05 : 3 champs IA liquidation communaute legale FR.
                .liquidationCommunauteEnvisagee(liquidationCommunauteEnvisagee)
                .recompensesEpoux1Eur(recompensesEpoux1Eur)
                .recompensesEpoux2Eur(recompensesEpoux2Eur)
                // SF-216-07 : 3 champs IA ARIPA recouvrement FR.
                .aripaRecouvrementEnvisage(aripaRecouvrementEnvisage)
                .montantPensionMensuelleDueEur(montantPensionMensuelleDueEur)
                .titreExecutoireDetecte(titreExecutoireDetecte)
                // SF-216-03 : 2 champs IA pension alimentaire enfant FR.
                .pensionAlimentaireEnvisagee(pensionAlimentaireEnvisagee)
                .modeResidenceEnfantsDetecte(modeResidenceEnfantsDetecte)
                // SF-216-09 : 3 champs IA délégation autorité parentale FR.
                .delegationApEnvisagee(delegationApEnvisagee)
                .tiersLienFamilialDetecte(tiersLienFamilialDetecte)
                .accordParentsDetecte(accordParentsDetecte)
                // SF-216-11 : 3 champs IA retrait autorité parentale FR.
                .retraitApEnvisage(retraitApEnvisage)
                .condamnationPenaleDetectee(condamnationPenaleDetectee)
                .violencesLmvss2022Detectees(violencesLmvss2022Detectees)
                // SF-216-15 : 1 champ IA adoption intra-familiale FR.
                .adoptionIntraEnvisagee(adoptionIntraEnvisagee)
                // SF-216-17 : 4 champs IA adoption internationale FR.
                .adoptionInternationaleEnvisagee(adoptionInternationaleEnvisagee)
                .paysOrigineAdopteDetecte(paysOrigineAdopteDetecte)
                .agrement2025DetecteValide(agrement2025DetecteValide)
                .exequaturRequisDetecte(exequaturRequisDetecte)
                // SF-216-13 : 2 champs IA audition du mineur par le JAF FR.
                .auditionMineurEnvisagee(auditionMineurEnvisagee)
                .demandeAuditionFormaliseeDetectee(demandeAuditionFormaliseeDetectee)
                // SF-216-19 : 3 champs IA indignité successorale FR.
                .indigniteSuccessoraleEnvisagee(indigniteSuccessoraleEnvisagee)
                .condamnationPenaleSuccessionDetectee(condamnationPenaleSuccessionDetectee)
                .pardonTestamentaireDetecte(pardonTestamentaireDetecte)
                // SF-216-21 : 3 champs IA recel successoral FR.
                .recelSuccessoralEnvisage(recelSuccessoralEnvisage)
                .typeRecelDetecte(typeRecelDetecte)
                .preuveRecelDetectee(preuveRecelDetectee)
                // SF-216-23 : 3 champs IA donation entre époux FR.
                .donationEntreEpouxEnvisagee(donationEntreEpouxEnvisagee)
                .revocabiliteDetectee(revocabiliteDetectee)
                .bienDonnePrincipalType(bienDonnePrincipalType)
                // SF-216-27 : 3 champs IA partage successoral notarié FR.
                .partageNotarialEnvisage(partageNotarialEnvisage)
                .presenceImmeubleSuccessionDetecte(presenceImmeubleSuccessionDetecte)
                .declarationSuccessionEcheancDetectee(declarationSuccessionEcheancDetectee)
                // SF-216-25 : 4 champs IA présomption de paternité FR.
                .presomptionPaterniteEnvisagee(presomptionPaterniteEnvisagee)
                .desaveuEnvisage(desaveuEnvisage)
                .dateConclusionMariageDetectee(dateConclusionMariageDetectee)
                .dateDissolutionMariageDetectee(dateDissolutionMariageDetectee)
                // SF-216-29 : 3 champs IA donation-partage FR.
                .donationPartageEnvisagee(donationPartageEnvisagee)
                .presencePetitsEnfantsSubstitutionDetectee(presencePetitsEnfantsSubstitutionDetectee)
                .donationPartageConjonctiveDetectee(donationPartageConjonctiveDetectee)
                .build();
    }

    /**
     * SF-246-25 : extraction d'une liste de codes whitelistés depuis un nœud JSON.
     * Retourne {@code null} si la liste serait vide après filtrage (jamais [] —
     * invariant cadrage §5.1.2). Codes normalisés en MAJUSCULES avant filtrage.
     */
    private static java.util.List<String> listOrNullWhitelisted(
            JsonNode parent, String field, java.util.Set<String> whitelist) {
        JsonNode arrNode = parent.get(field);
        if (arrNode == null || !arrNode.isArray() || arrNode.isEmpty()) return null;
        java.util.List<String> result = new java.util.ArrayList<>();
        for (JsonNode item : arrNode) {
            if (item.isTextual()) {
                String code = item.asText().trim().toUpperCase(java.util.Locale.ROOT);
                if (whitelist.contains(code) && !result.contains(code)) {
                    result.add(code);
                }
            }
        }
        return result.isEmpty() ? null : java.util.Collections.unmodifiableList(result);
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
