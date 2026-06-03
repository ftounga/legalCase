/**
 * SF-FA-10-02 : modèles TypeScript pour l'outil "Divorce accepté"
 * (art. 233-234 Cciv + 1123 CPC). FRANCE uniquement.
 *
 * Backend : `DivorceAccepteRequest` / `DivorceAccepteResponse` (PR #514).
 */

export type DivorceAccepteVerdict = 'ELEVEE' | 'FAIBLE';

export interface DivorceAccepteRequest {
  acceptationPrincipeSignee: boolean;
  /** ISO date YYYY-MM-DD (optionnel — si null le PV n'a pas encore été signé). */
  dateAcceptationPV?: string | null;
  dureeMariageAnnees: number;
  revenusAnnuelsEpoux1Eur: number;
  revenusAnnuelsEpoux2Eur: number;
  patrimoineCommun: boolean;
  /** ISO date YYYY-MM-DD (optionnel — date d'assignation devant le JAF). */
  dateAssignation?: string | null;
}

export interface DivorceAccepteResponse {
  caseFileId: string;
  acceptationPrincipeSignee: boolean;
  dateAcceptationPV: string | null;
  dureeMariageAnnees: number;
  revenusAnnuelsEpoux1Eur: number;
  revenusAnnuelsEpoux2Eur: number;
  patrimoineCommun: boolean;
  dateAssignation: string | null;
  country: 'FRANCE';
  acceptationValide: boolean;
  ordrePublic: boolean;
  eligibilite: boolean;
  scoreGlobal: number;
  verdictEligibilite: DivorceAccepteVerdict;
  delaiProcedureMoisPrevisionnel: number;
  prestationCompensatoireFourchetteMin: number;
  prestationCompensatoireFourchetteMax: number;
  criteresNonRemplis: string[];
  formule: string;
  baseJuridique: string;
  messages: string[];
}

/**
 * SF-FA-10-02 : structure minimale frontend pour le pré-fill IA des outils
 * famille (divorce accepté, et plus tard altération / faute / etc.).
 *
 * Tous les champs sont optionnels et peuvent être absents — les composants
 * doivent être no-op gracieux. Cette interface est volontairement gardée
 * frontend-only tant que le backend n'expose pas un type équivalent
 * `FamilleExtractedData` dans `CaseAnalysisResult`.
 */
export interface FamilleExtractedData {
  /** Durée du mariage en années entières (depuis la date du mariage). */
  dureeMariageAnnees?: number | null;
  /** Revenus annuels bruts époux 1 (€). */
  revenusAnnuelsEpoux1Eur?: number | null;
  /** Revenus annuels bruts époux 2 (€). */
  revenusAnnuelsEpoux2Eur?: number | null;
  /**
   * Régime matrimonial : true si communauté ou participation aux acquêts.
   * SF-246-25 : désormais réel — source backend : `communaute_partage_protection_detection_v2.patrimoine_commun_bool`.
   * Distinct de `patrimoineCommunEur` (montant €) et `patrimoineCommunSignificatifDetecte` (PACS).
   */
  patrimoineCommun?: boolean | null;
  /**
   * SF-246-25 : flag global indiquant que des violences ont été alléguées dans le dossier.
   * Source backend réelle : `communaute_partage_protection_detection_v2.violences_alleguees_bool`.
   * Distinct de `violencesAllegueesDetectees` (liste typée des codes de violence).
   */
  violencesAlleguees?: boolean | null;
  /** Date de signature du PV d'acceptation (ISO YYYY-MM-DD). */
  dateAcceptationPV?: string | null;
  /**
   * SF-155-20 : valeur vénale du bien immobilier principal (€) — utilisée
   * par l'outil F-FA-05 (partage immobilier) pour pré-remplir le champ
   * "Valeur vénale". Optionnel ; absent si le pipeline IA ne l'a pas extrait.
   */
  valeurImmeuble?: number | null;
  /**
   * SF-155-20 : capital restant dû du prêt hypothécaire associé (€).
   * Pré-remplit le champ "Capital restant dû" de l'outil F-FA-05.
   */
  capitalRestantDu?: number | null;
  /**
   * SF-FA-11-02 : date de séparation effective FR (ISO YYYY-MM-DD) — pré-fill
   * pour les outils Famille FR (séparation de corps, indivision, SF-246-08).
   * Source backend réelle : `vie_commune_detection.date_separation` (FR uniquement).
   * DISTINCT de `dateSeparationBe` (SF-246-12, divorce DDI belge).
   */
  dateSeparation?: string | null;
  /**
   * SF-246-12 : date de la cessation effective de la vie commune — pré-fill
   * pour l'outil F-FA-11 divorce-desunion-be (BELGIQUE UNIQUEMENT).
   * Source backend réelle : `divorce_ddi_be_detection.date_separation_be`.
   * CJ art. 1255 § 1 — point de départ du délai DDI.
   * DISTINCT de `dateSeparation` (FR — `vie_commune_detection` SF-246-08).
   */
  dateSeparationBe?: string | null;
  /**
   * SF-FA-11-02 : séparation consentue par les 2 époux — pré-fill pour
   * l'outil F-FA-11 désunion irrémédiable BE.
   * Note : ce champ reste aspirationnel — aucune source backend ne l'extrait.
   */
  separationConsentue?: boolean | null;
  /** SF-FA-15-02 : régime matrimonial détecté par l'IA. */
  regimeMatrimonialDetecte?: string | null;
  /** SF-FA-19-02 : autorité parentale détectée. */
  regimeExerciceActuel?: string | null;
  dangerCaracterise?: boolean | null;
  consentementAutreParent?: boolean | null;
  interferenceVieEnfant?: boolean | null;
  /**
   * SF-246-10 : âges en années entières des enfants concernés (liste, ordre de
   * naissance). Remplace le champ aspirationnel `ageEnfants` (source backend
   * absente). Source backend réelle : `autorite_parentale_detection.ages_enfants`.
   * Entiers [0, 25] ou null (jamais []).
   */
  agesEnfantsDetectes?: number[] | null;
  /**
   * SF-FA-19-06 : pré-fill outil "Désaccords parentaux art. 373-2-10".
   * Valeurs string brutes — converties en enum côté composant.
   */
  domaineDesaccordDetecte?: string | null;
  intensiteDesaccordDetecte?: string | null;
  tentativesMediationDetectees?: string[] | null;
  /** SF-FA-19-06 : urgence détectée par l'IA → délai 30j au lieu de 90j. */
  urgenceDetectee?: boolean | null;
  /** SF-FA-14-02 : pré-fill ordonnance de protection. */
  dateRequeteOP?: string | null;
  /**
   * SF-246-25 : types de violences alléguées détectés par l'IA.
   * Source backend réelle : `communaute_partage_protection_detection_v2.violences_alleguees_detectees`.
   * Codes ∈ {PHYSIQUES, PSYCHOLOGIQUES, SEXUELLES, ECONOMIQUES, MENACES_MORT}.
   * Null si aucun code reconnu (jamais []).
   */
  violencesAllegueesDetectees?: string[] | null;
  /**
   * SF-246-25 : types de preuves de violences détectées par l'IA.
   * Source backend réelle : `communaute_partage_protection_detection_v2.preuves_violences`.
   * Codes ∈ {CONSTAT_HUISSIER, MAIN_COURANTE, CERTIFICAT_MEDICAL, TEMOIGNAGES,
   * PHOTOS, PLAINTE_DEPOSEE, JUGEMENT_CORRECTIONNEL, AUTRE}.
   * Null si aucun code reconnu (jamais []).
   */
  preuvesViolencesDetectees?: string[] | null;
  /**
   * SF-246-25 : danger immédiat pour la victime détecté par l'IA.
   * Source backend réelle : `communaute_partage_protection_detection_v2.danger_immediat_detected`.
   */
  dangerImmediatDetected?: boolean | null;
  /**
   * SF-246-25 : présence d'enfants dans le foyer détectée par l'IA.
   * Source backend réelle : `communaute_partage_protection_detection_v2.presence_enfants_detected`.
   */
  presenceEnfantsDetected?: boolean | null;
  /**
   * SF-246-25 : logement commun (victime et auteur partagent le domicile) détecté.
   * Source backend réelle : `communaute_partage_protection_detection_v2.logement_commun_detected`.
   */
  logementCommunDetected?: boolean | null;
  /**
   * SF-246-25 : victime financièrement dépendante de l'auteur des violences détectée.
   * Source backend réelle : `communaute_partage_protection_detection_v2.victime_financierement_dependante_detected`.
   */
  victimeFinanciairementDependanteDetected?: boolean | null;
  demandeurDejaProtegeDetected?: boolean | null;
  /**
   * SF-FA-19-04 : pré-fill outil "Changement de résidence" (art. 373-2 Cciv).
   * Raison détectée du changement (mutation / regroupement / logement / etc.).
   * Valeur attendue parmi `RaisonChangement` (TRAVAIL / FAMILLE / LOGEMENT /
   * RAPPROCHEMENT_FAMILIAL / AUTRE) — toute autre valeur est ignorée.
   */
  raisonChangementDetectee?: string | null;
  /**
   * SF-FA-19-04 : l'autre parent a-t-il été informé préalablement
   * (heuristique IA — emails / SMS / courriers détectés).
   */
  informePrealablement?: boolean | null;
  /**
   * SF-FA-19-04 : mode de résidence actuel des enfants. Valeur attendue
   * parmi `ModeResidenceCh` (`ALTERNEE` / `EXCLUSIVE_DEMANDEUR` /
   * `EXCLUSIVE_DEFENDEUR`) — toute autre valeur est ignorée.
   *
   * NB : distinct de `regimeExerciceActuel` (SF-FA-19-02), qui décrit
   * l'*exercice* parental (qui décide), pas la *résidence* (où vit l'enfant).
   */
  modeResidenceActuel?: string | null;
  /** SF-FA-20-02 : dissolution PACS pré-fill. */
  dateConclusionPacs?: string | null;
  /**
   * SF-246-25 : mode de dissolution du PACS détecté par l'IA.
   * Source backend réelle : `communaute_partage_protection_detection_v2.mode_dissolution_pacs`.
   * Codes ∈ {DECLARATION_UNILATERALE, DECLARATION_CONJOINTE, MARIAGE_PARTENAIRES, MARIAGE_TIERS, DECES}.
   * Null si absent ou code inconnu.
   */
  modeDissolutionPacsDetecte?: string | null;
  /**
   * SF-246-25 : régime des biens du PACS détecté par l'IA.
   * Source backend réelle : `communaute_partage_protection_detection_v2.regime_biens_pacs`.
   * Codes ∈ {SEPARATION_BIENS, INDIVISION_AMENAGEE, INDIVISION_PAR_DEFAUT}.
   * Null si absent ou code inconnu.
   */
  regimeBiensPacsDetecte?: string | null;
  /**
   * SF-246-25 : créances alléguées entre partenaires PACS ou co-indivisaires.
   * Source backend réelle : `communaute_partage_protection_detection_v2.creances_alleguees`.
   * Codes ∈ {CONTRIBUTION_DESEQUILIBRE, INVESTISSEMENT_BIEN_PROPRE,
   * ENRICHISSEMENT_INJUSTE, PRESTATION_TRAVAIL_NON_REMUNEREE, AUCUNE}.
   * Null si aucun code reconnu (jamais []).
   */
  creancesAllegueesDetectees?: string[] | null;
  /**
   * SF-246-25 : patrimoine commun du PACS significatif détecté (impact sur partage).
   * Source backend réelle : `communaute_partage_protection_detection_v2.patrimoine_commun_significatif_detecte`.
   */
  patrimoineCommunSignificatifDetecte?: boolean | null;
  /** SF-FA-25-02 : majeurs protégés pré-fill. */
  regimeProtectionDemande?: string | null;
  altertationFacultesMentales?: boolean | null;
  altertationFacultesPhysiques?: boolean | null;
  certificatMedicalCirconstancieDetected?: boolean | null;
  dateCertificatMedicalDetected?: string | null;
  consentementPersonneAProtegerDetected?: boolean | null;
  demandeurFamilialDetected?: string | null;
  actesEnvisagesDetected?: string[] | null;
  /**
   * SF-FA-25-06 : pré-fill des 4 nouveaux champs spécifiques aux régimes
   * curatelle renforcée (art. 472), tutelle (art. 440 al. 3) et mandat de
   * protection future (art. 477+). Tous optionnels — le composant est no-op
   * gracieux si l'IA n'a rien détecté.
   */
  incapaciteGestionQuotidienneDetected?: boolean | null;
  altertationGraveDetected?: boolean | null;
  mandatPrealableSigneDetected?: boolean | null;
  /** Valeur attendue : `'NOTARIE'` ou `'SOUS_SEING_PRIVE'`. */
  formeMandatProtectionDetected?: string | null;
  /**
   * SF-FA-26-02 : pré-fill outil "Changement état civil" (art. 60 / 61-1 / 61-5
   * Cciv ; loi 2016-1547 / 2022-301). Toutes les valeurs sont optionnelles —
   * le composant est no-op gracieux si absent.
   *
   * Type changement : valeur attendue parmi `TypeChangement`
   * (`NOM` / `PRENOM` / `SEXE` / `NOM_ET_PRENOM`) — toute autre valeur ignorée.
   */
  typeChangementDetecte?: string | null;
  /**
   * SF-FA-26-02 : motif invoqué détecté. Valeur attendue parmi `MotifInvoque`
   * (`INTERET_LEGITIME` / `MARIAGE` / `RECTIFICATION_ERREUR` /
   * `IDENTIFICATION_GENRE` / `AUTRE`) — toute autre valeur ignorée.
   */
  motifChangementDetecte?: string | null;
  /**
   * SF-FA-26-02 / SF-246-11 : date de naissance du demandeur du changement
   * d'état civil (ISO YYYY-MM-DD). Source backend réelle (SF-246-11) :
   * `changement_etat_civil_detection.date_naissance_demandeur`.
   */
  dateNaissanceDemandeurDetectee?: string | null;
  /** SF-FA-26-02 : demandeur majeur détecté par l'IA. */
  majeurDemandeurDetected?: boolean | null;
  /** SF-FA-26-02 : consentement parental détecté (mineur). */
  consentementParentalDetected?: boolean | null;
  /**
   * SF-FA-17-02 / SF-246-25 : pré-fill outil "Partage judiciaire" (art. 840+ Cciv + 1364+ CPC).
   * Toutes valeurs optionnelles — composant no-op gracieux.
   *
   * `pvDifficultesEtablisDetected` : PV de difficultés (art. 1366 CPC) dressé par le notaire —
   * préalable obligatoire à la saisine.
   * Source backend réelle : `communaute_partage_protection_detection_v2.pv_difficultes_etablis_detected`.
   */
  pvDifficultesEtablisDetected?: boolean | null;
  /**
   * SF-246-25 : tentative amiable épuisée (échec voie amiable — sinon refus pour défaut d'intérêt à agir).
   * Source backend réelle : `communaute_partage_protection_detection_v2.tentative_amiable_epuiseuee_detected`.
   */
  tentativeAmiableEpuiseueeDetected?: boolean | null;
  /** SF-FA-17-02 : nombre de co-indivisaires détecté par l'IA. */
  nombreCoindivisairesDetecte?: number | null;
  /** SF-FA-17-02 : valeur estimée des biens en indivision (€). */
  valeurBiensIndivisionEur?: number | null;
  /**
   * SF-246-24 : présence d'un conjoint survivant dans la succession (art. 756-757-3 Cciv).
   * Champ mutualisé entre les outils reserve-heriditaire et devolution-legale.
   * Source backend réelle : `succession_detection_v2.conjoint_survivant`.
   */
  conjointSurvivantDetected?: boolean | null;
  /** SF-FA-24-02 : nombre de descendants détecté par l'IA. */
  nbDescendantsDetecte?: number | null;
  /**
   * SF-246-24 : tous les descendants du défunt sont-ils issus de l'union avec le conjoint
   * survivant ? (art. 757 Cciv — détermine si l'option ¼ PJ vs usufruit total est ouverte).
   * Source backend réelle : `succession_detection_v2.tous_descendants_communs_avec_conjoint`.
   */
  tousDescendantsCommunsAvecConjointDetected?: boolean | null;
  /** SF-FA-24-02 : nombre de frères/sœurs détecté par l'IA. */
  nbFreresSoeursDetecte?: number | null;
  /**
   * SF-FA-18-02 : pré-fill reconnaissance paternelle (art. 316 Cciv).
   */
  consentementLibreDuPereDetected?: boolean | null;
  paterniteVraisemblableDetected?: boolean | null;
  enfantNonReconnuParAutrePereDetected?: boolean | null;
  procedureRespecteeReconnaissanceDetected?: boolean | null;
  /**
   * SF-246-09 : date de naissance de l'enfant faisant l'objet d'une
   * reconnaissance paternelle (art. 316 Cciv).
   * Source backend réelle : `filiation_detection.date_naissance_enfant`.
   * DISTINCT de `dateNaissanceEnfantRechercheDetectee` (action en recherche
   * de paternité art. 327 Cciv).
   */
  dateNaissanceEnfantDetectee?: string | null;
  /**
   * SF-FA-16-02 / SF-246-25 : pré-fill communauté universelle (art. 1526 + 1527 al. 2 Cciv).
   * Source backend réelle : `communaute_partage_protection_detection_v2.contrat_notarie_detected`.
   */
  contratNotarieDetected?: boolean | null;
  /**
   * SF-246-25 : présence d'enfants non communs (art. 1527 al. 2 Cciv — action en retranchement possible).
   * Source backend réelle : `communaute_partage_protection_detection_v2.enfants_non_communs_detected`.
   */
  enfantsNonCommunsDetected?: boolean | null;
  /**
   * SF-246-25 : clause d'attribution intégrale de la communauté universelle (art. 1527 al. 1 Cciv).
   * Source backend réelle : `communaute_partage_protection_detection_v2.clause_attribution_integrale_detected`.
   */
  clauseAttributionIntegraleDetected?: boolean | null;
  valeurCommunauteEurDetectee?: number | null;
  /**
   * SF-246-24 : forme juridique du testament (art. 967-1035 Cciv) — OLOGRAPHE, AUTHENTIQUE ou
   * MYSTIQUE. Source backend réelle : `succession_detection_v2.forme_testament`.
   */
  formeTestamentDetectee?: string | null;
  /**
   * SF-246-06 : date de rédaction du testament (ISO YYYY-MM-DD). Source backend réelle :
   * `succession_detection.date_redaction_testament`.
   */
  dateRedactionTestamentDetectee?: string | null;
  /**
   * SF-246-24 : le testateur était-il sain d'esprit lors de la rédaction du testament ?
   * (art. 901 Cciv — capacité mentale). Source backend réelle :
   * `succession_detection_v2.saine_esprit_testateur`.
   */
  saineDEspritTestateurDetected?: boolean | null;
  /**
   * SF-246-24 : le legs excède-t-il la quotité disponible, portant atteinte à la réserve ?
   * (art. 912-920 Cciv — legs réductible). Source backend réelle :
   * `succession_detection_v2.legs_excede_quotite_disponible`.
   */
  legsExcedeQuotiteDisponibleDetected?: boolean | null;
  /**
   * SF-FA-18-04 / SF-246-26 : pré-fill contestation de paternité (art. 332-335 Cciv).
   * Qualité à agir du demandeur dans l'action en contestation.
   * Source backend réelle : `filiation_detection_v2.qualite_aagir_contestation`.
   */
  qualiteAagirContestationDetected?:
    | 'PERE_DECLARE'
    | 'PERE_BIOLOGIQUE_PRESUME'
    | 'MERE'
    | 'ENFANT_MAJEUR'
    | null;
  /**
   * SF-246-09 : date d'établissement de la filiation contestée (source backend
   * réelle : `filiation_detection.date_etablissement_filiation`).
   */
  dateEtablissementFiliationDetectee?: string | null;
  /**
   * SF-246-09 : date de connaissance de la vérité sur la filiation (point de
   * départ du délai de prescription art. 321 Cciv). Source backend réelle :
   * `filiation_detection.date_connaissance_verite`.
   */
  dateConnaissanceVeriteDetectee?: string | null;
  /**
   * SF-246-09 : date de majorité de l'enfant concerné (18 ans — départ du
   * délai 10 ans pour l'enfant art. 321 Cciv). Source backend réelle :
   * `filiation_detection.date_majorite_enfant`.
   */
  dateMajoriteEnfantDetectee?: string | null;
  /**
   * SF-246-26 : possession d'état conforme depuis 5 ans (art. 333 Cciv — fin de non-recevoir).
   * Source backend réelle : `filiation_detection_v2.possession_etat_conforme_5ans`.
   */
  possessionEtatConforme5AnsDetected?: boolean | null;
  /**
   * SF-246-26 : expertise ADN demandée dans l'action en contestation de paternité.
   * Source backend réelle : `filiation_detection_v2.expertise_adn_demandee_contestation`.
   * DISTINCT de `expertiseAdnDemandeeRechercheDetected` (action en recherche art. 327).
   */
  expertiseAdnDemandeeDetected?: boolean | null;
  /**
   * SF-246-26 : motifs sérieux justifiant la contestation de paternité (art. 332 al. 2 Cciv).
   * Source backend réelle : `filiation_detection_v2.motifs_serieux_contestation`.
   * DISTINCT de `motifsSerieuxRechercheDetected` (recherche de paternité art. 340 Cciv).
   */
  motifsSerieuxDetected?: boolean | null;
  /**
   * SF-FA-27-02 : pré-fill PMA / GPA / bioéthique.
   */
  dispositifBioethiqueDetecte?: string | null;
  /**
   * SF-FA-18-06 / SF-246-26 : pré-fill action en recherche de paternité (art. 327 + 340 Cciv).
   * Qualité du demandeur dans l'action en recherche.
   * Source backend réelle : `filiation_detection_v2.qualite_demandeur_recherche`.
   * DISTINCT de `qualiteAagirContestationDetected` (contestation art. 332).
   */
  qualiteDuDemandeurRechercheDetected?:
    | 'ENFANT_MAJEUR'
    | 'REPRESENTANT_LEGAL_MINEUR'
    | 'MERE'
    | null;
  /**
   * SF-246-09 : date de naissance de l'enfant dont la paternité est recherchée
   * (action art. 327 Cciv). Source backend réelle :
   * `filiation_detection.date_naissance_enfant_recherche`.
   * DISTINCT de `dateNaissanceEnfantDetectee` (reconnaissance art. 316 Cciv).
   */
  dateNaissanceEnfantRechercheDetectee?: string | null;
  /**
   * SF-246-26 : présomption de possession d'état invoquée dans la recherche de paternité (art. 311-1 Cciv).
   * Source backend réelle : `filiation_detection_v2.presomption_possession_etat_recherche`.
   */
  presomptionPossessionEtatRechercheDetected?: boolean | null;
  /**
   * SF-246-26 : expertise ADN demandée dans l'action en recherche de paternité (art. 16-11 Cciv).
   * Source backend réelle : `filiation_detection_v2.expertise_adn_demandee_recherche`.
   * DISTINCT de `expertiseAdnDemandeeDetected` (contestation art. 332-335).
   */
  expertiseAdnDemandeeRechercheDetected?: boolean | null;
  /**
   * SF-246-26 : le père désigné refuse l'expertise ADN ordonnée (art. 16-11 al. 3 Cciv).
   * Source backend réelle : `filiation_detection_v2.pere_designe_refuse_adn`.
   */
  pereDesigneRefuseADNDetected?: boolean | null;
  /**
   * SF-246-26 : motifs sérieux justifiant la présomption de paternité (art. 342 Cciv).
   * Source backend réelle : `filiation_detection_v2.motifs_serieux_recherche`.
   * DISTINCT de `motifsSerieuxDetected` (contestation art. 332 al. 2).
   */
  motifsSerieuxRechercheDetected?: boolean | null;
  /**
   * SF-246-24 : forme juridique de la donation (art. 931-939 Cciv) — NOTARIEE, MANUELLE,
   * INDIRECTE ou DEGUISEE. Source backend réelle : `succession_detection_v2.forme_donation`.
   */
  formeDonationDetectee?: string | null;
  /**
   * SF-246-06 : date de l'acte de donation (ISO YYYY-MM-DD). Source backend réelle :
   * `succession_detection.date_donation`.
   */
  dateDonationDetectee?: string | null;
  /**
   * SF-246-24 : le donateur était-il sain d'esprit au moment de la donation ?
   * (art. 901 Cciv — capacité mentale). Source backend réelle :
   * `succession_detection_v2.saine_esprit_donateur`.
   */
  saineDEspritDonateurDetected?: boolean | null;
  /**
   * SF-246-24 : la donation respecte-t-elle la quotité disponible (art. 912-919-2 Cciv) ?
   * Source backend réelle : `succession_detection_v2.respect_quotite_disponible`.
   */
  respectQuotiteDisponibleDetected?: boolean | null;
  /**
   * SF-FA-24-08 : pré-fill réserve héréditaire (art. 913 + 914-1 + 920-928 Cciv).
   */
  nombreEnfantsSuccessionDetecte?: number | null;
  montantSuccessionEurDetecte?: number | null;
  montantLibsTotalEurDetecte?: number | null;
  dateOuvertureSuccessionDetectee?: string | null;
  /**
   * SF-246-24 : qualité du demandeur de la réserve héréditaire (art. 913 Cciv).
   * Source backend réelle : `succession_detection_v2.qualite_du_demandeur_reserve`.
   */
  qualiteDuDemandeurReserveDetecte?:
    | 'HERITIER_RESERVATAIRE_DESCENDANT'
    | 'CONJOINT_SURVIVANT'
    | null;
  /**
   * SF-FA-18-10 / SF-246-26 : pré-fill adoption (art. 343-370-2 Cciv).
   * Forme d'adoption demandée (plénière vs simple).
   * Source backend réelle : `filiation_detection_v2.forme_adoption_demandee`.
   */
  formeAdoptionDemandeeDetected?: 'PLENIERE' | 'SIMPLE' | null;
  /**
   * SF-246-26 : l'adopté est un pupille de l'État (art. L224-4 CASF).
   * Source backend réelle : `filiation_detection_v2.pupille_etat`.
   */
  pupilleEtatDetected?: boolean | null;
  /**
   * SF-246-26 : l'adoptant est marié (art. 343 Cciv — condition forme du demandeur).
   * Source backend réelle : `filiation_detection_v2.adoptant_marie`.
   */
  adoptantMarieDetected?: boolean | null;
  /**
   * SF-246-09 : âge de l'adoptant en années à la date de la requête d'adoption
   * (condition art. 343 al. 2 Cciv : 28 ans minimum). Source backend réelle :
   * `filiation_detection.age_adoptant`. Entier [0, 120] ou null.
   */
  ageAdoptantDetecte?: number | null;
  /**
   * SF-246-09 : âge de l'adopté en années à la date de la requête d'adoption
   * (adoption plénière : limite 15 ans art. 345 Cciv). Source backend réelle :
   * `filiation_detection.age_adopte`. Entier [0, 120] ou null.
   */
  ageAdopteDetecte?: number | null;
  /**
   * SF-FA-24-10 : pré-fill partage successoral (art. 815-840 Cciv).
   */
  modePartageDemandeDetecte?: string | null;
  nombreCoheritiersDetecte?: number | null;
  dateDecesDetectee?: string | null;
  /**
   * SF-FA-24-12 : pré-fill type d'indivision successorale (art. 815 / 1873-1+).
   */
  typeIndivisionSuccessoraleDetecte?: string | null;
  /**
   * SF-246-06 : montant des donations reçues par l'héritier pour rapport à succession
   * (art. 843 Cciv). Source backend réelle : `succession_detection.montant_donations_recues_eur`.
   */
  montantDonationsRecuesEurDetecte?: number | null;
  /**
   * SF-246-06 : valeur de la donation réévaluée au jour du partage (art. 860 Cciv).
   * Source backend réelle : `succession_detection.valeur_donation_au_jour_partage_eur`.
   */
  valeurDonationAuJourPartageEurDetectee?: number | null;
  /**
   * SF-246-24 : qualité de l'héritier tenu au rapport à succession (art. 843 Cciv).
   * DESCENDANT ou CONJOINT_SURVIVANT. Source backend réelle :
   * `succession_detection_v2.qualite_heritier_rapport`.
   * Distinct de `qualiteHeritierDetectee` (acceptation-renonciation, art. 734-754 Cciv).
   */
  qualiteHeritierRapportDetectee?:
    | 'DESCENDANT'
    | 'CONJOINT_SURVIVANT'
    | null;
  /**
   * SF-246-24 : la donation comporte-t-elle une clause de dispense de rapport (art. 843 al.1 Cciv) ?
   * Source backend réelle : `succession_detection_v2.donation_dispense_de_rapport`.
   */
  donationDispenseDeRapportDetected?: boolean | null;
  /**
   * SF-246-24 : la nature présumée de la libéralité est-elle non rapportable (legs, art. 843 al.2 Cciv) ?
   * Source backend réelle : `succession_detection_v2.nature_presumee_non_rapportable`.
   */
  naturePresumeeNonRapportableDetected?: boolean | null;
  /**
   * F-210 SF-210-02 : flag pivot "médiation familiale obligatoire pré-saisine
   * JAF pertinente" (Cciv art. 373-2-10 al. 3). True si l'IA détecte une
   * saisine in-scope (autorité parentale / contribution / résidence enfant /
   * DVH) sans tentative documentée ni exception applicable.
   */
  mediationFamilialePreSaisinePertinente?: boolean | null;
  /**
   * F-210 SF-210-02 : motif détecté pour l'outil médiation familiale
   * pré-saisine (best-effort).
   */
  motifSaisineMediationDetecte?:
    | 'AUTORITE_PARENTALE'
    | 'CONTRIBUTION_ENTRETIEN'
    | 'RESIDENCE_ENFANT'
    | 'DROIT_VISITE'
    | 'DIVORCE_CONTENTIEUX'
    | 'SUCCESSION'
    | 'AUTRE'
    | null;
  /** F-210 SF-210-04 : actif brut succession détecté (€). */
  actifBrutSuccessionEurDetecte?: number | null;
  /** F-210 SF-210-04 : passif succession détecté (€). */
  passifSuccessionEurDetecte?: number | null;
  /**
   * SF-246-24 : qualité héritier détectée pour l'outil acceptation-renonciation
   * (art. 734-754 Cciv). PREMIER_RANG = descendants ; SECOND_RANG = ascendants et
   * collatéraux privilégiés. Source backend réelle : `succession_detection_v2.qualite_heritier`.
   * Distinct de `qualiteHeritierRapportDetectee` (rapport à succession, art. 843 Cciv).
   */
  qualiteHeritierDetectee?: 'PREMIER_RANG' | 'SECOND_RANG' | null;
  /**
   * SF-246-24 : des actes équivalant à une acceptation tacite (art. 783 Cciv) ont-ils
   * déjà été posés ? Source backend réelle : `succession_detection_v2.actes_equivalent_acceptation_dejas_poses`.
   */
  actesEquivalentAcceptationDejaPosesDetected?: boolean | null;
  /**
   * SF-246-24 : la succession comporte-t-elle des dettes incertaines ou à risque élevé ?
   * (motif d'acceptation à concurrence de l'actif net art. 787+ ou de renonciation)
   * Source backend réelle : `succession_detection_v2.dettes_incertaines`.
   */
  dettesIncertainesDetected?: boolean | null;
  /**
   * SF-246-03 : codes de fautes matrimoniales détectées par le pipeline IA pour
   * pré-remplissage de l'outil F-FA-09 (divorce pour faute, art. 242 Cciv, FR uniquement).
   * Codes ∈ {VIOLENCES, ADULTERE, ABANDON_DOMICILE, MANQUEMENT_DEVOIR_ASSISTANCE,
   * INJURES_GRAVES, MANQUEMENT_DEVOIR_FIDELITE, MANQUEMENT_DEVOIR_RESPECT}.
   * Source backend réelle : `divorce_faute_detection.fautes_detectees`.
   * Null si aucune faute documentée ou dossier belge (jamais []).
   * Remplace le stub aspirationnel de l'ancien cast permissif.
   */
  fautesDetectees?: string[] | null;
  /**
   * SF-246-08 : pré-fill lot séparation / indivision / PACS / protection
   * (F-FA-12/13/14/20/21/22). Sous-objet backend `vie_commune_detection`.
   *
   * `patrimoineCommunEur` : valeur du patrimoine commun (€) — invariant §5.2
   * (> 0 ou null, jamais 0). Distinct de `patrimoineCommun` (boolean existant).
   *
   * `revenusAnnuelsEpoux` : revenus annuels débiteur/demandeur (€) — pré-fill
   * outil mesures provisoires + révisions post-divorce.
   *
   * Dates — invariant §5.1 (ISO YYYY-MM-DD strict) :
   * - `dateSeparation` : date séparation effective FR (déjà existant, désormais réel).
   * - `dateConclusionPacs` : date de conclusion du PACS (déjà existant, désormais réel).
   * - `dateRequeteOP` : date dépôt requête OP (déjà existant, désormais réel).
   * - `dateAudienceAOMP` : date audience audience ordonnance de mesures provisoires.
   *
   * `nbEnfantsACharge` : nombre d'enfants à charge [0, 30].
   */
  /** SF-246-08 : montant du patrimoine commun (€) — distinct du boolean `patrimoineCommun`. */
  patrimoineCommunEur?: number | null;
  /** SF-246-08 : date de l'audience AOMP (ISO YYYY-MM-DD). */
  dateAudienceAOMP?: string | null;
  /** SF-246-08 : nombre d'enfants à charge [0, 30]. */
  nbEnfantsACharge?: number | null;
  /** SF-246-08 : revenus annuels du débiteur/demandeur (€) — pré-fill mesures provisoires & révisions. */
  revenusAnnuelsEpoux?: number | null;
  /**
   * SF-246-10 : date de début de la période pour laquelle un calendrier de garde
   * est demandé ou en vigueur (ISO YYYY-MM-DD). Source backend réelle :
   * `autorite_parentale_detection.date_debut_calendrier`.
   */
  dateDebutCalendrierDetectee?: string | null;
  /**
   * SF-246-10 : date de fin de la période du calendrier de garde demandé ou en
   * vigueur (ISO YYYY-MM-DD). Source backend réelle :
   * `autorite_parentale_detection.date_fin_calendrier`.
   */
  dateFinCalendrierDetectee?: string | null;
  /**
   * SF-246-27 : régime de protection juridique d'un majeur demandé ou en vigueur
   * (art. 425+ Cciv). Source backend réelle :
   * `protection_divorce_detection_v2.regime_protection_majeurs`.
   * Valeurs : SAUVEGARDE_JUSTICE | HABILITATION_FAMILIALE | CURATELLE_SIMPLE |
   * CURATELLE_RENFORCEE | TUTELLE | MANDAT_PROTECTION_FUTURE.
   * Remplace le champ aspirationnel `regimeProtectionDemande`.
   */
  regimeProtectionMajeursDetected?:
    | 'SAUVEGARDE_JUSTICE'
    | 'HABILITATION_FAMILIALE'
    | 'CURATELLE_SIMPLE'
    | 'CURATELLE_RENFORCEE'
    | 'TUTELLE'
    | 'MANDAT_PROTECTION_FUTURE'
    | null;
  /**
   * SF-246-27 : date du certificat médical circonstancié établissant l'altération
   * des facultés du majeur à protéger (art. 431 Cciv), ISO YYYY-MM-DD.
   * Source backend réelle : `protection_divorce_detection_v2.date_certificat_medical_majeurs`.
   * Remplace le champ aspirationnel `dateCertificatMedicalDetected`.
   */
  dateCertificatMedicalMajeursDetected?: string | null;
  /**
   * SF-246-27 : date de début du protocole PMA (assistance médicale à la procréation,
   * art. L2141-2 CSP), ISO YYYY-MM-DD.
   * Source backend réelle : `protection_divorce_detection_v2.date_pma`.
   * DISTINCT de `dateReconnaissanceAnterieurePmaDetected` et `dateDonGametesDetected`.
   */
  datePmaDetected?: string | null;
  /**
   * SF-246-27 : date de la reconnaissance anticipée effectuée devant notaire avant
   * le début de la PMA (art. L2141-5 al. 2 CSP), ISO YYYY-MM-DD.
   * Source backend réelle : `protection_divorce_detection_v2.date_reconnaissance_anterieure_pma`.
   * DISTINCT de `datePmaDetected` et `dateDonGametesDetected`.
   */
  dateReconnaissanceAnterieurePmaDetected?: string | null;
  /**
   * SF-246-27 : date du don de gamètes (art. L2143-3 CSP — droit d'accès aux
   * origines personnelles), ISO YYYY-MM-DD.
   * Source backend réelle : `protection_divorce_detection_v2.date_don_gametes`.
   * DISTINCT de `datePmaDetected` et `dateReconnaissanceAnterieurePmaDetected`.
   */
  dateDonGametesDetected?: string | null;
  /**
   * SF-246-27 : motif principal de saisine du médiateur familial (art. 373-2-10 Cciv),
   * source réelle.
   * Source backend réelle : `protection_divorce_detection_v2.motif_saisine_mediation`.
   * Valeurs : AUTORITE_PARENTALE | CONTRIBUTION_ENTRETIEN | DROIT_VISITE | RESIDENCE | AUTRE.
   * Remplace le champ aspirationnel `motifSaisineMediationDetecte`.
   */
  motifSaisineMediationDetected?:
    | 'AUTORITE_PARENTALE'
    | 'CONTRIBUTION_ENTRETIEN'
    | 'DROIT_VISITE'
    | 'RESIDENCE'
    | 'AUTRE'
    | null;
  /**
   * SF-246-27 : date de l'assignation en divorce (FR uniquement — divorce-accepte +
   * divorce-alteration, art. 56 CPC), ISO YYYY-MM-DD.
   * Source backend réelle : `protection_divorce_detection_v2.date_assignation_divorce`.
   * DISTINCT de la date de la requête en divorce et de la date de la convention CMU.
   */
  dateAssignationDivorce?: string | null;
  /**
   * SF-246-27 : BELGIQUE UNIQUEMENT — date de l'audience d'homologation de la convention
   * de divorce par consentement commun (DC) devant le tribunal de la famille belge
   * (art. 1288bis C.jud.BE), ISO YYYY-MM-DD.
   * Source backend réelle : `protection_divorce_detection_v2.date_audience_homologation_dc_be`.
   * Pour un dossier France, ce champ est null.
   */
  dateAudienceHomologationDcBe?: string | null;

  // ── SF-246-28 : 16 champs IA Famille BE (levée PREFILL_COUNT_ALWAYS_ZERO) ─────────
  // Source backend réelle : `famille_be_detection_v2`. BELGIQUE UNIQUEMENT.

  /**
   * SF-246-28 : BELGIQUE UNIQUEMENT — mode d'hébergement principal des enfants détecté
   * par l'IA (convention de garde ou jugement BE art. 374 CC BE).
   * Source backend réelle : `famille_be_detection_v2.mode_hebergement_principal_be`.
   * Codes ∈ {HEBERGEMENT_EGALITAIRE, HEBERGEMENT_PRINCIPAL_UN_PARENT, HEBERGEMENT_NON_FIXE}.
   * Null si absent ou code inconnu.
   */
  modeHebergementPrincipalBeDetecte?: string | null;
  /**
   * SF-246-28 : BELGIQUE UNIQUEMENT — nombre d'enfants communs [1, 12] détecté
   * par l'IA (jugement / convention de garde BE).
   * Source backend réelle : `famille_be_detection_v2.nombre_enfants_be`.
   * Null si absent ou hors plage.
   */
  nombreEnfantsBeDetecte?: number | null;
  /**
   * SF-246-28 : BELGIQUE UNIQUEMENT — revenu mensuel net du parent 1 (€/mois, ≥ 0)
   * détecté par l'IA (fiches de paie BE). 0 est valide (parent sans revenu).
   * Source backend réelle : `famille_be_detection_v2.revenu_mensuel_parent1_be`.
   * Null si absent ou négatif.
   */
  revenuMensuelParent1BeDetecte?: number | null;
  /**
   * SF-246-28 : BELGIQUE UNIQUEMENT — revenu mensuel net du parent 2 (€/mois, ≥ 0)
   * détecté par l'IA (fiches de paie BE). 0 est valide.
   * Source backend réelle : `famille_be_detection_v2.revenu_mensuel_parent2_be`.
   * Null si absent ou négatif.
   */
  revenuMensuelParent2BeDetecte?: number | null;
  /**
   * SF-246-28 : BELGIQUE UNIQUEMENT — allocations familiales mensuelles (€/mois, ≥ 0)
   * détectées par l'IA (paiement FAMIFED BE).
   * Source backend réelle : `famille_be_detection_v2.allocations_familiales_be`.
   * Null si absent ou négatif.
   * Note : champ backend s'appelle `allocationsFamilialesMensuellesBeDetectees`
   * (espace dans la mini-spec était une coquille).
   */
  allocationsFamilialesMensuellesBeDetectees?: number | null;
  /**
   * SF-246-28 : BELGIQUE UNIQUEMENT — nuits d'hébergement mensuelles pour le parent 1
   * [0, 30] détectées par l'IA (convention de garde / jugement BE).
   * Source backend réelle : `famille_be_detection_v2.nuits_hebergement_parent1_be`.
   * Null si absent ou hors plage.
   */
  nuitsHebergementParent1BeDetectees?: number | null;
  /**
   * SF-246-28 : BELGIQUE UNIQUEMENT — nuits d'hébergement mensuelles pour le parent 2
   * [0, 30] détectées par l'IA.
   * Source backend réelle : `famille_be_detection_v2.nuits_hebergement_parent2_be`.
   * Null si absent ou hors plage.
   */
  nuitsHebergementParent2BeDetectees?: number | null;
  /**
   * SF-246-28 : BELGIQUE UNIQUEMENT — durée du mariage en années entières [0, 80]
   * détectée par l'IA (acte de mariage BE / jugement — CC BE art. 301 § 3).
   * Source backend réelle : `famille_be_detection_v2.duree_mariage_annees_be`.
   * Null si absent ou hors plage.
   */
  dureeMariageAnneesBeDetectee?: number | null;
  /**
   * SF-246-28 : BELGIQUE UNIQUEMENT — revenu mensuel net du créancier de pension
   * alimentaire (€/mois, ≥ 0) détecté par l'IA (fiches de paie BE, art. 301 § 3 CC BE).
   * Source backend réelle : `famille_be_detection_v2.revenu_mensuel_creancier_be`.
   * Null si absent ou négatif.
   */
  revenuMensuelCreancierBeDetecte?: number | null;
  /**
   * SF-246-28 : BELGIQUE UNIQUEMENT — revenu mensuel net du débiteur de pension
   * alimentaire (€/mois, ≥ 0) détecté par l'IA.
   * Source backend réelle : `famille_be_detection_v2.revenu_mensuel_debiteur_be`.
   * Null si absent ou négatif.
   */
  revenuMensuelDebiteurBeDetecte?: number | null;
  /**
   * SF-246-28 : BELGIQUE UNIQUEMENT — date de désignation du notaire commis à la
   * liquidation-partage (ISO YYYY-MM-DD — CJ art. 1207 BE).
   * Source backend réelle : `famille_be_detection_v2.date_designation_notaire_be`.
   * Null si absent ou format non ISO.
   */
  dateDesignationNotaireBeDetectee?: string | null;
  /**
   * SF-246-28 : BELGIQUE UNIQUEMENT — date d'ouverture des opérations de liquidation-partage
   * (ISO YYYY-MM-DD — CJ BE).
   * Source backend réelle : `famille_be_detection_v2.date_ouverture_operations_be`.
   * Null si absent ou format non ISO.
   */
  dateOuvertureOperationsBeDetectee?: string | null;
  /**
   * SF-246-28 : BELGIQUE UNIQUEMENT — date de notification du projet de liquidation-partage
   * (ISO YYYY-MM-DD — CJ art. 1218 BE — point de départ du délai de contredits).
   * Source backend réelle : `famille_be_detection_v2.date_notification_projet_be`.
   * Null si absent ou format non ISO.
   */
  dateNotificationProjetBeDetectee?: string | null;
  /**
   * SF-246-28 : BELGIQUE UNIQUEMENT — date d'homologation par le tribunal de la famille
   * (ISO YYYY-MM-DD — CJ BE).
   * Source backend réelle : `famille_be_detection_v2.date_homologation_be`.
   * Null si absent ou format non ISO.
   */
  dateHomologationBeDetectee?: string | null;
  /**
   * SF-246-28 : BELGIQUE UNIQUEMENT — date du mariage (ISO YYYY-MM-DD — acte de mariage
   * BE, CC art. 3.18+ BE).
   * Source backend réelle : `famille_be_detection_v2.date_mariage_be`.
   * Null si absent ou format non ISO.
   */
  dateMariageBeDetectee?: string | null;
  /**
   * SF-246-28 : BELGIQUE UNIQUEMENT — contrat de mariage signé devant notaire
   * (CC art. 1.2.59+ BE — détermine si le régime est légal ou conventionnel).
   * Source backend réelle : `famille_be_detection_v2.contrat_mariage_signe_be`.
   * Null si absent.
   */
  contratMariageSigneBeDetecte?: boolean | null;

  // ── SF-216-01 : 5 champs IA prestation compensatoire + vie commune FR ─────────
  // Source backend réelle : `famille_extracted_data.vie_commune_detection`
  //                       + `famille_extracted_data.prestation_compensatoire_detection`.
  // FRANCE UNIQUEMENT — prompt impose null hors FR / hors certitude.

  /**
   * SF-216-01 : FRANCE — revenus annuels bruts de l'époux 1 (€/an, ≥ 0) détectés
   * par l'IA (fiches de paie / avis d'imposition). Distinct de `revenusAnnuelsEpoux1Eur`
   * (legacy DivorceAccepte, lié au calculateur SF-198 par convention historique).
   * Source backend réelle : `vie_commune_detection.revenus_annuels_epoux1_eur`.
   * Le record Java `FamilleExtractedData` expose ce champ via Jackson sous le nom
   * `revenusAnnuelsEpoux1` (sans suffixe `Eur` — conserver l'alignement Jackson).
   * Null si absent ou hors plage.
   */
  revenusAnnuelsEpoux1?: number | null;
  /**
   * SF-216-01 : FRANCE — revenus annuels bruts de l'époux 2 (€/an, ≥ 0) détectés
   * par l'IA. Source backend : `vie_commune_detection.revenus_annuels_epoux2_eur`
   * (Jackson : `revenusAnnuelsEpoux2`).
   * Null si absent ou hors plage.
   */
  revenusAnnuelsEpoux2?: number | null;
  /**
   * SF-216-01 : FRANCE — âge en années entières [0, 120] de l'époux 1 détecté
   * par l'IA (pièce d'identité). Source backend : `vie_commune_detection.age_epoux1_annees`.
   * Null si absent ou hors plage.
   */
  ageEpoux1Annees?: number | null;
  /**
   * SF-216-01 : FRANCE — âge en années entières [0, 120] de l'époux 2 détecté
   * par l'IA. Source backend : `vie_commune_detection.age_epoux2_annees`.
   * Null si absent ou hors plage.
   */
  ageEpoux2Annees?: number | null;
  /**
   * SF-216-01 : FRANCE — flag CONTEXTUAL indiquant que la prestation compensatoire
   * est envisagée dans le dossier (mention art. 270 / disparité niveaux de vie).
   * Source backend : `prestation_compensatoire_detection.envisagee`.
   */
  prestationCompensatoireEnvisagee?: boolean | null;

  // ── SF-216-05 : 3 champs IA liquidation communauté légale FR (rétro-compat) ──
  /**
   * SF-216-05 : FRANCE — flag CONTEXTUAL indiquant que la liquidation de la
   * communauté légale est envisagée (mention art. 1467 Cciv / partage actif commun).
   * Source backend : `liquidation_communaute_detection.envisagee`.
   */
  liquidationCommunauteEnvisagee?: boolean | null;
  /**
   * SF-216-05 : FRANCE — récompenses dues par l'époux 1 à la communauté
   * (€, ≥ 0). Source backend : `liquidation_communaute_detection.recompenses_epoux1_eur`.
   */
  recompensesEpoux1Eur?: number | null;
  /**
   * SF-216-05 : FRANCE — récompenses dues par l'époux 2 à la communauté.
   * Source backend : `liquidation_communaute_detection.recompenses_epoux2_eur`.
   */
  recompensesEpoux2Eur?: number | null;

  // ── SF-216-07 : 3 champs IA ARIPA recouvrement pension alimentaire impayée FR ──
  /**
   * SF-216-07 : FRANCE — flag CONTEXTUAL indiquant qu'une démarche ARIPA de
   * recouvrement de pension alimentaire impayée est envisagée (mention ARIPA /
   * SDR / pension impayée / art. L. 581 CSS).
   * Source backend : `aripa_recouvrement_detection.envisage`.
   */
  aripaRecouvrementEnvisage?: boolean | null;
  /**
   * SF-216-07 : FRANCE — montant en euros entiers (≥ 0) de la pension
   * alimentaire mensuelle fixée par titre exécutoire (jugement / convention
   * CM notariée / acte notarié). Source backend :
   * `aripa_recouvrement_detection.montant_pension_mensuelle_due_eur`.
   */
  montantPensionMensuelleDueEur?: number | null;
  /**
   * SF-216-07 : FRANCE — true si un titre exécutoire fixant la pension
   * alimentaire est documenté dans le dossier (jugement définitif / convention
   * CM notariée / acte notarié / ordonnance).
   * Source backend : `aripa_recouvrement_detection.titre_executoire_detecte`.
   */
  titreExecutoireDetecte?: boolean | null;

  // ── F-246 — nombre d'enfants détectés (sous-objet filiation v2) ──
  /**
   * F-246 SF-246-26 : nombre d'enfants détectés par l'IA (sous-objet
   * `filiation_detection_v2.nombre_enfants_detecte`). Utilisé notamment par
   * SF-216-07 ARIPA pour pré-remplir le nombre d'enfants à charge.
   */
  nombreEnfantsDetecte?: number | null;

  /**
   * SF-216-03 : FRANCE — flag CONTEXTUAL indiquant qu'une pension alimentaire enfant
   * est envisagée (mention art. 371-2 Cciv, contribution entretien éducation,
   * barème indicatif Cass.). Source backend : `pension_alimentaire_detection.envisagee`.
   */
  pensionAlimentaireEnvisagee?: boolean | null;
  /**
   * SF-216-03 : FRANCE — mode de résidence des enfants détecté
   * (ALTERNEE / PRINCIPALE_PARENT1 / PRINCIPALE_PARENT2 / null).
   * Source backend : `pension_alimentaire_detection.mode_residence`.
   */
  modeResidenceEnfantsDetecte?: 'ALTERNEE' | 'PRINCIPALE_PARENT1' | 'PRINCIPALE_PARENT2' | string | null;

  // ── SF-222-01 : 6 champs IA ASF allocation de soutien familial FR (F-FA-ASF-CAF) ──
  /**
   * SF-222-01 : FRANCE — flag CONTEXTUAL indiquant qu'une situation ASF est
   * détectée (parent isolé + autre parent défaillant / décédé / inconnu —
   * art. L. 523-1 CSS). Source backend : `asf_caf_detection.detecte`.
   */
  asfCafDetecte?: boolean | null;
  /** SF-222-01 : FRANCE — parent isolé. Source : `asf_caf_detection.parent_isole`. */
  asfParentIsole?: boolean | null;
  /** SF-222-01 : FRANCE — pension fixée par titre exécutoire. Source : `asf_caf_detection.pension_fixee`. */
  asfPensionFixee?: boolean | null;
  /** SF-222-01 : FRANCE — montant pension mensuelle (€, ≥ 0). Source : `asf_caf_detection.montant_pension_mensuel`. */
  asfMontantPension?: number | null;
  /** SF-222-01 : FRANCE — état de paiement (NON_PAYEE / PARTIELLE / PAYEE). Source : `asf_caf_detection.pension_payee`. */
  asfPensionPayee?: 'NON_PAYEE' | 'PARTIELLE' | 'PAYEE' | string | null;
  /** SF-222-01 : FRANCE — nombre d'enfants à charge (≥ 1). Source : `asf_caf_detection.nb_enfants`. */
  asfNbEnfants?: number | null;

  /**
   * SF-216-09 : FRANCE — flag CONTEXTUAL délégation autorité parentale envisagée
   * (art. 376-1 Cciv). Source backend : `delegation_ap_detection.envisagee`.
   */
  delegationApEnvisagee?: boolean | null;
  /**
   * SF-216-09 : FRANCE — lien entre l'enfant et le tiers candidat à recevoir la
   * délégation (whitelist 5 valeurs). Source backend :
   * `delegation_ap_detection.tiers_lien_familial`.
   */
  tiersLienFamilialDetecte?:
    | 'GRANDS_PARENTS'
    | 'ONCLE_TANTE'
    | 'FAMILLE_ELARGIE'
    | 'ASSOCIATION_HABILITEE'
    | 'AUTRE'
    | string
    | null;
  /**
   * SF-216-09 : FRANCE — true si accord des deux parents documenté pour la
   * délégation. Source backend : `delegation_ap_detection.accord_parents`.
   */
  accordParentsDetecte?: boolean | null;

  /**
   * SF-216-11 : FRANCE — flag CONTEXTUAL retrait autorité parentale envisagé
   * (mention art. 378 Cciv, déchéance parentale, loi 2022 LMVSS, condamnation
   * pénale sur l'enfant). Source backend : `retrait_ap_detection.envisage`.
   */
  retraitApEnvisage?: boolean | null;
  /**
   * SF-216-11 : FRANCE — true si une condamnation pénale (jugement / peine)
   * visant le parent dont le retrait est envisagé est documentée dans les
   * pièces. Source backend : `retrait_ap_detection.condamnation_penale_detectee`.
   */
  condamnationPenaleDetectee?: boolean | null;
  /**
   * SF-216-11 : FRANCE — true si des violences conjugales graves en présence
   * de l'enfant au sens de la loi n°2022-140 du 7 février 2022 (LMVSS) sont
   * documentées. Source backend : `retrait_ap_detection.violences_lmvss_2022_detectees`.
   */
  violencesLmvss2022Detectees?: boolean | null;

  /**
   * SF-216-15 : FRANCE — flag CONTEXTUAL indiquant qu'une adoption de l'enfant
   * du conjoint (adoption intra-familiale) est envisagée (mention art. 345-1
   * Cciv, couple recomposé + adoption, consentement adoption).
   * Source backend : `adoption_intra_detection.envisagee`.
   * V1 — utilisé uniquement pour activer la visibilité du panel F-IA-04
   * (CONTEXTUAL). Aucun pré-fill IA dérivé en V1 (PREFILL_COUNT_ALWAYS_ZERO).
   */
  adoptionIntraEnvisagee?: boolean | null;

  /**
   * SF-216-17 : FRANCE — flag CONTEXTUAL indiquant qu'une adoption
   * internationale est envisagée (mention art. 370-3 Cciv, Convention La
   * Haye, OAA, agrément Conseil départemental, MAI).
   * Source backend : `adoption_internationale_detection.envisagee`.
   */
  adoptionInternationaleEnvisagee?: boolean | null;

  /**
   * SF-216-17 : FRANCE — pays d'origine de l'enfant pour adoption
   * internationale (texte libre en MAJUSCULES sans accent, ex. "VIETNAM",
   * "COLOMBIE", "MAROC", "ETHIOPIE").
   * Source backend : `adoption_internationale_detection.pays_origine`.
   */
  paysOrigineAdopteDetecte?: string | null;

  /**
   * SF-216-17 : FRANCE — true si un agrément du Conseil départemental est
   * documenté et valide (≤ 5 ans, loi n°2001-111 du 6/2/2001).
   * Source backend : `adoption_internationale_detection.agrement_valide`.
   */
  agrement2025DetecteValide?: boolean | null;

  /**
   * SF-216-17 : FRANCE — true si une décision d'adoption rendue à l'étranger
   * est documentée et nécessite un exequatur TJ (art. 370-5 Cciv).
   * Source backend : `adoption_internationale_detection.exequatur_requis`.
   */
  exequaturRequisDetecte?: boolean | null;

  /**
   * SF-216-13 : FRANCE — flag CONTEXTUAL indiquant qu'une audition du mineur
   * par le JAF est envisagée (mention art. 388-1 Cciv, demande d'audition,
   * entendre l'enfant, avocat de l'enfant).
   * Source backend : `audition_mineur_detection.envisagee`.
   * V1 — utilisé pour activer la visibilité du panel F-IA-04 (CONTEXTUAL).
   */
  auditionMineurEnvisagee?: boolean | null;

  /**
   * SF-216-13 : FRANCE — true si une demande d'audition du mineur a déjà
   * été formellement présentée au juge (requête, conclusions, ordonnance).
   * Pré-fill UI du champ `demandeFormalisee`.
   * Source backend : `audition_mineur_detection.demande_formalisee_detectee`.
   */
  demandeAuditionFormaliseeDetectee?: boolean | null;

  /**
   * SF-216-19 : FRANCE — flag CONTEXTUAL indiquant qu'une indignité
   * successorale est envisagée (mention art. 726 Cciv, condamnation pour
   * meurtre / tentative / violences graves contre le défunt, témoignage
   * faux, obstruction au testament).
   * Source backend : `indignite_successorale_detection.envisagee`.
   */
  indigniteSuccessoraleEnvisagee?: boolean | null;

  /**
   * SF-216-19 : FRANCE — true si une condamnation pénale définitive en
   * lien avec le défunt (meurtre / tentative / violences graves / actes
   * de barbarie / témoignage faux) est documentée dans les pièces.
   * Source backend : `indignite_successorale_detection.condamnation_penale_detectee`.
   */
  condamnationPenaleSuccessionDetectee?: boolean | null;

  /**
   * SF-216-19 : FRANCE — true si le défunt a explicitement pardonné
   * l'héritier indigne dans son testament (art. 728 Cciv — pardon
   * testamentaire neutralisant l'indignité).
   * Source backend : `indignite_successorale_detection.pardon_testamentaire`.
   */
  pardonTestamentaireDetecte?: boolean | null;

  /**
   * SF-216-21 : FRANCE — flag CONTEXTUAL indiquant qu'un recel successoral
   * est envisagé (mention art. 778 Cciv, dissimulation de bien / donation,
   * destruction de testament, recel de créance).
   * Source backend : `recel_succession_detection.envisage`.
   */
  recelSuccessoralEnvisage?: boolean | null;

  /**
   * SF-216-21 : FRANCE — type de recel détecté dans les pièces
   * (DISSIMULATION_BIEN, DISSIMULATION_DONATION, DESTRUCTION_TESTAMENT,
   * RECEL_CREANCE, AUTRE). Pré-fill UI du champ `typeRecel`.
   * Source backend : `recel_succession_detection.type_recel`.
   */
  typeRecelDetecte?: string | null;

  /**
   * SF-216-21 : FRANCE — nature de la preuve mentionnée dans les pièces
   * (AVEUX, DOCUMENT, TEMOIGNAGE, EXPERTISE, FAISCEAU_INDICES, AUCUNE).
   * Pré-fill UI du champ `preuveRecel`.
   * Source backend : `recel_succession_detection.preuve_recel`.
   */
  preuveRecelDetectee?: string | null;

  /**
   * SF-216-23 : FRANCE — flag CONTEXTUAL indiquant qu'une donation entre
   * époux ou un avantage matrimonial est envisagé (mention art. 1096
   * Cciv, donation au dernier vivant, clause d'attribution intégrale,
   * etc.).
   * Source backend : `donation_entre_epoux_detection.envisagee`.
   */
  donationEntreEpouxEnvisagee?: boolean | null;

  /**
   * SF-216-23 : FRANCE — true si une révocation expresse (testament
   * postérieur, acte notarié) ou tacite (faits incompatibles) de la
   * donation entre époux est documentée dans les pièces (art. 1096
   * al. 2 Cciv).
   * Source backend : `donation_entre_epoux_detection.revocabilite_detectee`.
   */
  revocabiliteDetectee?: boolean | null;

  /**
   * SF-216-23 : FRANCE — type principal du bien donné identifié dans
   * les pièces (IMMOBILIER, MOBILIER, PORTEFEUILLE, NUMERAIRE, AUTRE).
   * Source backend : `donation_entre_epoux_detection.bien_donne_principal_type`.
   */
  bienDonnePrincipalType?: string | null;

  /**
   * SF-216-27 : FRANCE — flag CONTEXTUAL indiquant qu'un partage
   * successoral amiable devant notaire est envisagé (mention
   * art. 816 Cciv, notaire désigné pour partage, projet de partage,
   * acte de partage).
   * Source backend : `partage_notarial_detection.envisage`.
   */
  partageNotarialEnvisage?: boolean | null;

  /**
   * SF-216-27 : FRANCE — true si la succession comprend un immeuble
   * (déclenche l'obligation notariale art. 1592 CGI — règles de
   * publicité foncière).
   * Source backend : `partage_notarial_detection.presence_immeuble`.
   */
  presenceImmeubleSuccessionDetecte?: boolean | null;

  /**
   * SF-216-27 : FRANCE — échéance fiscale explicitement détectée dans
   * les pièces pour la déclaration de succession (art. 641 CGI —
   * 6 mois du décès). Date ISO YYYY-MM-DD. Null si à calculer depuis
   * `dateOuvertureSuccessionDetectee`.
   * Source backend : `partage_notarial_detection.declaration_succession_echeance`.
   */
  declarationSuccessionEcheancDetectee?: string | null;

  /**
   * SF-216-29 : FRANCE — flag CONTEXTUAL indiquant qu'une donation-partage
   * (art. 1075 à 1075-5 Cciv) est envisagée (mention « donation-partage »,
   * « partage anticipé », « art. 1075 Cciv », acte notarié de répartition
   * du patrimoine entre descendants).
   * Source backend : `donation_partage_detection.envisagee`.
   */
  donationPartageEnvisagee?: boolean | null;

  /**
   * SF-216-29 : FRANCE — true si la donation-partage vise les petits-
   * enfants par substitution du descendant intermédiaire (art. 1075-1
   * Cciv — donation-partage transgénérationnelle).
   * Source backend : `donation_partage_detection.petits_enfants_substitution`.
   */
  presencePetitsEnfantsSubstitutionDetectee?: boolean | null;

  /**
   * SF-216-29 : FRANCE — true si la donation-partage est conjonctive (les
   * deux parents font une donation commune — art. 1075-2 Cciv).
   * Source backend : `donation_partage_detection.conjonctive`.
   */
  donationPartageConjonctiveDetectee?: boolean | null;

  /**
   * SF-216-25 : FRANCE — flag CONTEXTUAL indiquant qu'une situation de
   * présomption de paternité du mari (application, renversement, désaveu)
   * est envisagée (mention art. 312-316 Cciv, désaveu, présomption mari).
   * Source backend : `presomption_paternite_detection.envisagee`.
   */
  presomptionPaterniteEnvisagee?: boolean | null;

  /**
   * SF-216-25 : FRANCE — true si une action en désaveu de paternité
   * (art. 316 al. 2 Cciv) est envisagée, introduite ou documentée
   * dans les pièces.
   * Source backend : `presomption_paternite_detection.desaveu_envisage`.
   */
  desaveuEnvisage?: boolean | null;

  /**
   * SF-216-25 : FRANCE — date de conclusion du mariage extraite des
   * pièces (acte de mariage, livret de famille). Format ISO YYYY-MM-DD.
   * Source backend : `presomption_paternite_detection.date_conclusion_mariage`.
   */
  dateConclusionMariageDetectee?: string | null;

  /**
   * SF-216-25 : FRANCE — date de dissolution du mariage extraite des
   * pièces (jugement de divorce, acte de décès). Format ISO YYYY-MM-DD.
   * Source backend : `presomption_paternite_detection.date_dissolution_mariage`.
   */
  dateDissolutionMariageDetectee?: string | null;
}
