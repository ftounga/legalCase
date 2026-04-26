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
  /** Régime matrimonial : true si communauté ou participation aux acquêts. */
  patrimoineCommun?: boolean | null;
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
   * SF-FA-11-02 : date de séparation effective (ISO YYYY-MM-DD) — pré-fill
   * pour l'outil F-FA-11 désunion irrémédiable BE (art. 229 CC).
   */
  dateSeparation?: string | null;
  /**
   * SF-FA-11-02 : séparation consentue par les 2 époux — pré-fill pour
   * l'outil F-FA-11 désunion irrémédiable BE.
   */
  separationConsentue?: boolean | null;
  /** SF-FA-15-02 : régime matrimonial détecté par l'IA. */
  regimeMatrimonialDetecte?: string | null;
  /** SF-FA-19-02 : autorité parentale détectée. */
  regimeExerciceActuel?: string | null;
  dangerCaracterise?: boolean | null;
  consentementAutreParent?: boolean | null;
  interferenceVieEnfant?: boolean | null;
  ageEnfants?: number[] | null;
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
  violencesAllegueesDetectees?: string[] | null;
  preuvesViolencesDetectees?: string[] | null;
  dangerImmediatDetected?: boolean | null;
  presenceEnfantsDetected?: boolean | null;
  logementCommunDetected?: boolean | null;
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
  modeDissolutionPacsDetecte?: string | null;
  regimeBiensPacsDetecte?: string | null;
  creancesAllegueesDetectees?: string[] | null;
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
  /** SF-FA-26-02 : date de naissance du demandeur (ISO YYYY-MM-DD). */
  dateNaissanceDemandeurDetectee?: string | null;
  /** SF-FA-26-02 : demandeur majeur détecté par l'IA. */
  majeurDemandeurDetected?: boolean | null;
  /** SF-FA-26-02 : consentement parental détecté (mineur). */
  consentementParentalDetected?: boolean | null;
  /**
   * SF-FA-17-02 : pré-fill outil "Partage judiciaire" (art. 840+ Cciv +
   * 1364+ CPC). Toutes valeurs optionnelles — composant no-op gracieux.
   *
   * `pvDifficultesEtablisDetected` : PV de difficultés (art. 1366 CPC)
   * dressé par le notaire — préalable obligatoire à la saisine.
   */
  pvDifficultesEtablisDetected?: boolean | null;
  /**
   * SF-FA-17-02 : tentative amiable épuisée (échec voie amiable —
   * sinon refus pour défaut d'intérêt à agir).
   */
  tentativeAmiableEpuiseueeDetected?: boolean | null;
  /** SF-FA-17-02 : nombre de co-indivisaires détecté par l'IA. */
  nombreCoindivisairesDetecte?: number | null;
  /** SF-FA-17-02 : valeur estimée des biens en indivision (€). */
  valeurBiensIndivisionEur?: number | null;
  /**
   * SF-FA-24-02 : pré-fill outil "Dévolution légale successorale"
   * (F-FA-24, art. 731 et s. Cciv). Tous les champs sont optionnels —
   * le composant est no-op gracieux si l'IA ne les a pas détectés.
   *
   * `conjointSurvivantDetected` : présence d'un conjoint survivant.
   */
  conjointSurvivantDetected?: boolean | null;
  /** SF-FA-24-02 : nombre de descendants détecté par l'IA. */
  nbDescendantsDetecte?: number | null;
  /**
   * SF-FA-24-02 : descendants tous communs au défunt + au conjoint
   * (vs famille recomposée). Détermine si l'option ¼/usufruit est ouverte.
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
  dateNaissanceEnfantDetectee?: string | null;
  /**
   * SF-FA-16-02 : pré-fill communauté universelle (art. 1526 + 1527 al. 2 Cciv).
   */
  contratNotarieDetected?: boolean | null;
  enfantsNonCommunsDetected?: boolean | null;
  clauseAttributionIntegraleDetected?: boolean | null;
  valeurCommunauteEurDetectee?: number | null;
  /**
   * SF-FA-24-04 : pré-fill validité testament (art. 967-1035 Cciv).
   */
  formeTestamentDetectee?: string | null;
  dateRedactionTestamentDetectee?: string | null;
  saineDEspritTestateurDetected?: boolean | null;
  legsExcedeQuotiteDisponibleDetected?: boolean | null;
  /**
   * SF-FA-18-04 : pré-fill contestation de paternité (art. 332-335 Cciv).
   */
  qualiteAagirContestationDetected?:
    | 'PERE_DECLARE'
    | 'PERE_BIOLOGIQUE_PRESUME'
    | 'MERE'
    | 'ENFANT_MAJEUR'
    | null;
  dateEtablissementFiliationDetectee?: string | null;
  dateConnaissanceVeriteDetectee?: string | null;
  dateMajoriteEnfantDetectee?: string | null;
  possessionEtatConforme5AnsDetected?: boolean | null;
  expertiseAdnDemandeeDetected?: boolean | null;
  motifsSerieuxDetected?: boolean | null;
  /**
   * SF-FA-27-02 : pré-fill PMA / GPA / bioéthique.
   */
  dispositifBioethiqueDetecte?: string | null;
  /**
   * SF-FA-18-06 : pré-fill action en recherche de paternité (art. 327 + 340 Cciv).
   */
  qualiteDuDemandeurRechercheDetected?:
    | 'ENFANT_MAJEUR'
    | 'REPRESENTANT_LEGAL_MINEUR'
    | 'MERE'
    | null;
  dateNaissanceEnfantRechercheDetectee?: string | null;
  presomptionPossessionEtatRechercheDetected?: boolean | null;
  expertiseAdnDemandeeRechercheDetected?: boolean | null;
  pereDesigneRefuseADNDetected?: boolean | null;
  motifsSerieuxRechercheDetected?: boolean | null;
  /**
   * SF-FA-24-08 : pré-fill outil "Réserve héréditaire et action en
   * réduction" (F-FA-24, art. 913 + 914-1 + 920-928 Cciv). Tous champs
   * optionnels — composant no-op gracieux si l'IA ne les a pas détectés.
   */
  nombreEnfantsSuccessionDetecte?: number | null;
  montantSuccessionEurDetecte?: number | null;
  montantLibsTotalEurDetecte?: number | null;
  /** Format ISO `YYYY-MM-DD`. */
  dateOuvertureSuccessionDetectee?: string | null;
  qualiteDuDemandeurReserveDetecte?:
    | 'HERITIER_RESERVATAIRE_DESCENDANT'
    | 'CONJOINT_SURVIVANT'
    | null;
}
