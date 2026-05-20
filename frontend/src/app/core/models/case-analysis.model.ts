import { DocumentPieceType } from './document.model';
import { FamilleExtractedData } from './divorce-accepte.model';

export interface TimelineEntry {
  date: string;
  evenement: string;
}

/**
 * F-146 SF-146-01 : référence précise d'une citation — document + pièce + pages.
 * Tous les champs sauf `documentName` sont nullable (pièce non identifiable ou
 * analyse pré-F-146).
 */
export interface SourceRef {
  documentName: string | null;
  pieceType: DocumentPieceType | null;
  pieceLabel: string | null;
  pageStart: number | null;
  pageEnd: number | null;
}

export interface AnalysisItem {
  texte: string;
  source: string | null;
  extrait: string | null;
  /** F-146 SF-146-01 : référence précise (null pour les analyses legacy). */
  sourceRef?: SourceRef | null;
}

export interface CompensationEstimate {
  indemnite: number;
  salaireReference: number;
  ancienneteAnnees: number;
  ancienneteMois: number;
  typeRupture: string;
  plafondMinMois: number;
  plafondMaxMois: number;
  donneesPartielles: boolean;
}

export type ModeGardeDetaille =
  | 'ALTERNEE_FR' | 'DVH_CLASSIQUE_FR' | 'DVH_ELARGI_FR'
  | 'ALTERNEE_BE' | 'SECONDAIRE_BE' | 'SECONDAIRE_ELARGI_BE';

export interface PensionAlimentaireEstimate {
  montantMin: number;
  montantMax: number;
  revenus: number;
  nbEnfants: number;
  modeGarde: 'EXCLUSIVE' | 'ALTERNEE';
  pays: 'FRANCE' | 'BELGIQUE';
  donneesPartielles: boolean;
  modeGardeDetaille?: ModeGardeDetaille | null;
  /** F-153 SF-153-01 : fourchette jurisprudentielle JAF (null si données partielles). */
  jurisprudenceRange?: JurisprudenceRange | null;
}

export interface PrestationCompensatoireEstimate {
  montantMin: number;
  montantMax: number;
  ecartRevenus: number;
  dureeMarriage: number;
  pays: 'FRANCE' | 'BELGIQUE';
  donneesPartielles: boolean;
  /** F-153 SF-153-01 : fourchette jurisprudentielle JAF (null si données partielles). */
  jurisprudenceRange?: JurisprudenceRange | null;
}

/** F-153 SF-153-01 : fourchette p25/p50/p75 observée en jurisprudence. */
export interface JurisprudenceRange {
  p25: number;
  p50: number;
  p75: number;
  label: string;
  sourceRef: string;
}

export interface BienItem {
  libelle: string;
  valeur: number | null;
}

export interface LiquidationCommunaute {
  regimeMatrimonial: string | null;
  actifCommun: BienItem[];
  biensPropresEpouxA: BienItem[];
  biensPropresEpouxB: BienItem[];
  passifCommun: BienItem[];
}

export interface BelgianCompensationEstimate {
  preavisSemaines: number;
  indemniteCompensatoire: number;
  salaireHebdomadaire: number;
  salaireReference: number;
  ancienneteAnnees: number;
  ancienneteMois: number;
  cct109MinSemaines: number;
  cct109MaxSemaines: number;
  cct109MinEuros: number;
  cct109MaxEuros: number;
  donneesPartielles: boolean;
}

export interface CaseAnalysisResult {
  id: string;
  version: number;
  analysisType: 'STANDARD' | 'ENRICHED';
  status: string;
  timeline: TimelineEntry[];
  faits: AnalysisItem[];
  pointsJuridiques: AnalysisItem[];
  risques: AnalysisItem[];
  questionsOuvertes: string[];
  piecesManquantes: string[];
  riskLevel: string | null;
  riskScore: number | null;
  modelUsed: string | null;
  updatedAt: string | null;
  analysisDocuments?: { index: number; name: string }[];
  compensationEstimate?: CompensationEstimate | null;
  belgianCompensationEstimate?: BelgianCompensationEstimate | null;
  pensionAlimentaireEstimate?: PensionAlimentaireEstimate | null;
  prestationCompensatoireEstimate?: PrestationCompensatoireEstimate | null;
  liquidationCommunaute?: LiquidationCommunaute | null;
  travailExtractedData?: TravailExtractedData | null;
  immigrationExtractedData?: ImmigrationExtractedData | null;
  /**
   * F-IA-04 : extraction Famille agrégée (ne contient que les champs
   * effectivement utilisés par les outils décisionnels Famille — voir
   * `FamilleExtractedData` pour la liste complète). Renseigné pour les
   * dossiers `DROIT_FAMILLE` (FR + BE), null sinon.
   *
   * Branchement TypeScript ajouté pour exposer au panel décisionnel
   * `ctx.synthesis?.familleExtractedData` (cf. decisional-tools-panel
   * lignes 710-1450). Backend produit ce champ via `CaseAnalysisResponse`
   * Spring depuis avril 2026 (SF-FA-10-02 et suivantes).
   */
  familleExtractedData?: FamilleExtractedData | null;
  licenciementValidityDetection?: LicenciementValidityDetection | null;
  ruptureConvValidityDetection?: RuptureConvValidityDetection | null;
  piecesManquantesDetails?: PieceManquanteEntry[] | null;
  /** F-150 : événements factuels détectés qui ouvrent un nouveau droit de séjour (liste vide hors immigration). */
  immigrationTriggerEvents?: ImmigrationTriggerEvent[] | null;
  /** F-151 : scenarii stratégiques immigration comparés (liste vide si aucun choix stratégique ouvert). */
  immigrationStrategyScenarios?: ImmigrationStrategyScenario[] | null;
  /** F-152 : détection validité divorce par consentement mutuel (famille, null hors domaine famille). */
  divorceConsentementValidityDetection?: DivorceConsentementValidityDetection | null;
  /** F-152 : scoring calculé (null si détection absente). */
  divorceConsentementScoring?: DivorceConsentementScoring | null;
  /**
   * F-197 SF-197-02 : type de litige (Travail FR) ou type de procédure
   * (Immigration) tel que détecté par l'IA, projeté top-level pour permettre
   * l'affichage badge "Type litige" dans la grille F-162 et le rappel dans
   * le dialog override. Renseigné par le backend depuis le JSON brut de
   * l'analyse (`type_litige_detecte` Travail FR / `type_procedure_detectee`
   * Immigration). Null hors Travail FR / Immigration ou si non détecté.
   */
  typeLitigeDetecte?: string | null;
}

/**
 * F-185 SF-185-01 — état partiel d'une synthèse en cours de streaming Sonnet.
 * `sections` est l'objet JSON top-level reconstruit à partir des sections déjà closes
 * (clés = `timeline`, `faits`, `pointsJuridiques`, etc. — sous-ensemble de
 * {@link CaseAnalysisResult}). Null si l'analyse vient juste de démarrer.
 *
 * Note : les clés sont en `snake_case` côté backend (telles que produites par Sonnet :
 * `points_juridiques`, `questions_ouvertes`, `pieces_manquantes`). Le frontend doit
 * mapper vers son camelCase au moment d'afficher.
 */
export interface CaseAnalysisPartialResponse {
  analysisId: string;
  version: number;
  /** F-190 SF-190-02 — type de l'analyse en cours de streaming. STANDARD = première synthèse, ENRICHED = re-analyse. */
  analysisType?: 'STANDARD' | 'ENRICHED';
  status: 'PROCESSING' | 'PARTIAL';
  sections: Partial<CaseAnalysisPartialSections> | null;
  updatedAt: string;
}

/**
 * F-185 SF-185-01 — clés JSON exactement telles que produites par Sonnet (snake_case).
 * Toutes optionnelles : seules celles déjà arrivées dans le stream sont présentes.
 */
export interface CaseAnalysisPartialSections {
  timeline: TimelineEntry[];
  faits: AnalysisItem[];
  points_juridiques: AnalysisItem[];
  risques: AnalysisItem[];
  questions_ouvertes: string[];
  pieces_manquantes: string[];
  pieces_manquantes_details: PieceManquanteEntry[];
  risk_level: string;
  risk_score: number;
  travail_extracted_data: unknown;
  immigration_extracted_data: unknown;
  famille_extracted_data: unknown;
}

/** F-152 SF-152-01 : détection validité divorce consentement mutuel. */
export interface DivorceConsentementValidityDetection {
  detections: { [critereCode: string]: DetectedAnswer };
}

/** F-152 SF-152-01 : scoring 0-100 + verdict. */
export type DivorceConsentementVerdict = 'VALIDE' | 'RISQUE_MOYEN' | 'RISQUE_ELEVE_NULLITE';

export interface DivorceConsentementScoring {
  score: number;
  verdict: DivorceConsentementVerdict;
  criteresValides: string[];
  criteresNonValides: string[];
  criteresInconnus: string[];
}

/** F-150 SF-150-01 : événement déclencheur immigration détecté dans le dossier. */
export interface ImmigrationTriggerEvent {
  eventCode: string;
  eventLabel: string;
  eventDate: string | null;
  sourceDocument: string | null;
  justification: string | null;
  baseLegale: string;
  suggestedTitleCode: string;
  suggestedTitleLabel: string;
}

/** F-151 SF-151-01 : scénario stratégique immigration. */
export type StrategyRiskLevel = 'FAIBLE' | 'MOYEN' | 'ELEVE';

export interface ImmigrationStrategyScenario {
  scenarioLabel: string;
  scenarioDescription: string;
  baseLegale: string | null;
  targetTitleCode: string | null;
  targetTitleLabel: string | null;
  delayDaysEstimate: string | null;
  riskLevel: StrategyRiskLevel | null;
  riskJustification: string | null;
  requiredAdditionalPieces: string[];
  advantages: string[];
  drawbacks: string[];
}

export interface PieceManquanteEntry {
  texte: string;
  critereCode?: string | null;
}

export interface DetectedAnswer {
  reponse: 'OUI' | 'NON' | 'INCONNU';
  justification?: string | null;
}

export interface LicenciementValidityDetection {
  detections: { [critereCode: string]: DetectedAnswer };
}

export interface RuptureConvValidityDetection {
  detections: { [critereCode: string]: DetectedAnswer };
}

export interface TravailExtractedData {
  conventionCollective?: string | null;
  dateEntree?: string | null;
  salaireBrutMensuel?: number | null;
  typeContrat?: string | null;
  poste?: string | null;
  motifLicenciement?: string | null;
  dateLicenciement?: string | null;
  congesContractuels?: number | null;
  primeAncienneteContractuelle?: number | null;
  /** SF-130-01 : true si salaireBrutMensuel a été déduit d'un net via × 1,30 */
  salaireEstDeduit?: boolean | null;
  /** SF-155-04 : motif de nullité pressenti pour pré-fill F-DT-11 harcèlement / discrimination (FR uniquement). */
  motifNullitePressenti?: 'DISCRIMINATION' | 'HARCELEMENT_MORAL' | 'HARCELEMENT_SEXUEL'
    | 'RETORSION' | 'SYNDICAL' | 'MATERNITE_PATERNITE' | 'ACCIDENT_MP' | null;
  /** SF-155-04 : origine d'inaptitude pressentie pour pré-fill F-DT-15 (FR uniquement). */
  origineInaptitudePressentie?: 'ACCIDENT_TRAVAIL' | 'MALADIE_PROFESSIONNELLE' | 'MALADIE_ORDINAIRE' | null;
  /** SF-155-04 : date de l'avis d'inaptitude (YYYY-MM-DD) pour pré-fill F-DT-15. */
  avisMedecinTravailDate?: string | null;
  /** SF-155-04 : détection recherche de reclassement documentée pour pré-fill F-DT-15. */
  reclassementRespecteDetected?: DetectedAnswer | null;
  /** SF-155-04 : heures sup mentionnées dans le dossier pour pré-fill F-DT-19 (FR uniquement). */
  heuresSupMentionneesDansDossier?: HeuresSupMentionnees | null;
  // SF-246-22 : suppression du vestige `fautesDetectees` — ce champ a été
  // déplacé vers `FamilleExtractedData` par SF-246-03. Il n'a jamais été
  // alimenté par le pipeline IA côté Travail (stub aspirationnel).
  // Le composant `divorce-faute-section` utilise `FamilleExtractedData` — aucun impact.
  /**
   * SF-DT-29-02 : âge du demandeur en années entières pour pré-fill F-DT-29
   * (crédit-temps BE, AR 29/10/1997 fin de carrière nécessite âge ≥ 55/60).
   * SF-246-05 : champ branché sur le pipeline IA — extrait par
   * `extractTravailData()` depuis la clé `age_demandeur_annees` (Travail BE
   * uniquement, borné [0, 100]). `null` si non déterminable ou dossier FR.
   */
  ageDemandeurAnnees?: number | null;
  /**
   * F-197 SF-197-02 : type de litige tel qu'il a été détecté par le pipeline
   * IA (Travail FR uniquement). Présent pour permettre aux outils
   * décisionnels de raisonner sur le type IA brut, indépendamment de
   * l'override avocat ({@link #typeLitigeAvocatOverride}). Renseigné
   * post-pipeline IA via projection JSON `type_litige_detecte`.
   */
  typeLitigeDetecte?: string | null;
  /**
   * F-197 SF-197-02 : override avocat single-value du type de litige (Travail
   * FR). Si présent, prend précédence sur {@link #typeLitigeDetecte} pour le
   * pré-remplissage des outils décisionnels au prochain run de Synthèse
   * enrichie (F-DT-08/09/10/11/12/13). Null tant qu'aucun override n'a été
   * posé. Persisté côté backend dans la table `case_file_type_litige_override`
   * (cf. SF-197-01).
   */
  typeLitigeAvocatOverride?: string | null;
  /**
   * SF-246-01 : flags procéduraux du licenciement pour pré-fill F-DT-36
   * (nullité de procédure, FR uniquement). Tous nullables — restent `null` pour
   * un dossier travail belge (concept procédural FR sans équivalent BE direct).
   */
  convocationEntretienDetectee?: boolean | null;
  dateConvocationEntretienDetectee?: string | null;
  dateEntretienPrealableDetectee?: string | null;
  entretienPrealableTenuDetected?: DetectedAnswer | null;
  lettreLicenciementEcriteDetectee?: boolean | null;
  lettreLicenciementMotiveeDetected?: DetectedAnswer | null;
  motivationLettreSuffisanteDetected?: DetectedAnswer | null;
  /**
   * SF-246-02 : flag de visibilité F-166 — `true` si une clause de non-concurrence
   * est textuellement présente au contrat de travail produit aux pièces. Réutilisé
   * par F-DT-24 comme pré-fill du booléen `clausePresenteContrat`. Travail FR.
   */
  clauseNonConcurrenceDetectee?: boolean | null;
  /**
   * SF-246-02 : détail de la clause de non-concurrence pour pré-fill F-DT-24
   * (FR uniquement). Durée en mois (`[0, 600]`), zone géographique en texte libre
   * (≤ 500 car.), contrepartie en euros bruts mensuels (`> 0`). Tous nullables —
   * restent `null` pour un dossier travail belge (régime CCT 1bis distinct).
   */
  nonConcurrenceDureeMois?: number | null;
  nonConcurrenceZoneGeographique?: string | null;
  nonConcurrenceContrepartieMontantEur?: number | null;
  /**
   * SF-246-13 : date de prise d'effet + secteur d'activité de la clause de
   * non-concurrence pour pré-fill F-DT-24 (FR uniquement). Tous deux nullables.
   * `nonConcurrenceDatePriseEffet` : date ISO YYYY-MM-DD (= date de fin/rupture contrat).
   * `nonConcurrenceSecteurActivite` : code parmi INFORMATIQUE / COMMERCE / INDUSTRIE / SERVICES / AUTRE.
   */
  nonConcurrenceDatePriseEffet?: string | null;
  nonConcurrenceSecteurActivite?: string | null;
  /**
   * SF-246-15 : identités salarié/employeur pour pré-fill des fiches de procédure
   * `prudhome-fiche` (FR) et `tribunal-travail-fiche` (BE). Champs présents dans le
   * record backend depuis F-DT-04/F-DT-06 — dette DTO frontend réglée ici.
   * `siretEmployeur` : FR uniquement (14 chiffres) ; `bceEmployeur` : BE uniquement.
   */
  nomSalarie?: string | null;
  prenomSalarie?: string | null;
  adresseSalarie?: string | null;
  nomEmployeur?: string | null;
  adresseEmployeur?: string | null;
  siretEmployeur?: string | null;
  bceEmployeur?: string | null;
  /**
   * SF-207-01 : 2 champs IA Travail BE pour pré-fill F-207
   * (prescription Travail BE). Tous deux nullables.
   * `dateRuptureContrat` : date de rupture du contrat au format ISO YYYY-MM-DD
   * (point de départ du délai de 1 an post-rupture, Loi 03/07/1978 art. 15
   * al. 1 + CCT 109 art. 11). Distincte de `dateLicenciement` (date de
   * notification) — la date de rupture intègre le préavis presté éventuel.
   * `motifRupture` : motif de rupture détecté en texte libre court
   * (`licenciement`, `démission`, `faute grave`, `RCC`, `rupture amiable`,
   * `fin de CDD`...). Utilisé pour pré-fill `typeCreance`.
   * Restent `null` pour un dossier Travail FR.
   */
  dateRuptureContrat?: string | null;
  motifRupture?: string | null;
  /**
   * SF-246-22 : type de procédure travail identifié par le pipeline IA pour pré-fill
   * F-136 `travail-procedure` (calendrier procédural FR + BE).
   * Codes admis (6 exacts — 3 FR + 3 BE) :
   * `PRUDHOMMES_FR`, `APPEL_CA_SOCIALE_FR`, `CASSATION_SOCIALE_FR` (France),
   * `TRIBUNAL_TRAVAIL_BE`, `COUR_TRAVAIL_BE`, `CASSATION_BE` (Belgique).
   * Code hors whitelist → null. Gating pays appliqué par `TravailProcedurePrefillRules`
   * (`_FR` pour workspace France, `_BE` pour workspace Belgique).
   * Source backend réelle : `procedure_travail_detection.procedure_detectee`.
   * Remplace le stub aspirationnel de l'ancien type d'intersection `TravailProcedureAiData`.
   */
  procedureTravailDetectee?: string | null;
  /**
   * SF-246-22 : date déclencheur de la procédure travail (date de saisine prud'homale,
   * citation à comparaître, dépôt de requête) au format ISO YYYY-MM-DD.
   * Null si non détectable ou si le sous-objet `procedure_travail_detection` est absent.
   * Source backend réelle : `procedure_travail_detection.date_declencheur`.
   * Remplace le stub aspirationnel de l'ancien type d'intersection `TravailProcedureAiData`.
   */
  dateDeclencheurProcedure?: string | null;
  /**
   * SF-207-02b : 6 champs IA Travail BE pour pré-fill F-207 (checklist C4 ONEM).
   * Tous nullables — restent `null` pour un dossier Travail FR (régime distinct
   * de l'attestation France Travail R.1234-9).
   * `raisonSocialeEmployeur` : raison sociale (dénomination juridique) de l'employeur.
   * `numeroBce` : numéro BCE (Banque-Carrefour des Entreprises) — 10 chiffres.
   * `categorieOnem` : code catégorie ONEM (ex. "9" pour faute grave).
   * `motifExplicite` : motif explicite de la fin de contrat (texte libre du C4).
   * `preavisPresteJours` : durée de préavis presté en jours (Integer côté backend).
   * `dernierSalaireMensuelBrut` : dernier salaire mensuel brut (BigDecimal backend
   * → number frontend).
   */
  raisonSocialeEmployeur?: string | null;
  numeroBce?: string | null;
  categorieOnem?: string | null;
  motifExplicite?: string | null;
  preavisPresteJours?: number | null;
  dernierSalaireMensuelBrut?: number | null;
  /**
   * SF-207-03 / SF-207-03b : 3 champs IA Travail BE pour pré-fill F-207
   * (contestation décision C4 ONEM). BE-only — restent null pour FR.
   * `dateNotificationDecisionOnem` : date de notification de la décision
   * ONEM contestée (ISO YYYY-MM-DD).
   * `dateDecisionDirecteur` : date de notification de la décision du
   * Directeur du Bureau du chômage sur le recours administratif (ISO).
   * `recoursAdminDejaForme` : true si le recours administratif au
   * Directeur a déjà été formé (Boolean nullable côté backend).
   */
  dateNotificationDecisionOnem?: string | null;
  dateDecisionDirecteur?: string | null;
  recoursAdminDejaForme?: boolean | null;
  /**
   * SF-207-04 / SF-207-04b : 2 champs IA Travail BE pour pré-fill F-207
   * (déclaration AT Fedris). BE-only — restent null pour FR.
   * `dateAccident` : date de survenance de l'accident du travail (ISO YYYY-MM-DD).
   * `dateConnaissanceAccidentEmployeur` : date à laquelle l'employeur a eu
   * connaissance de l'accident (point de départ du délai 8 j Fedris, ISO).
   */
  dateAccident?: string | null;
  dateConnaissanceAccidentEmployeur?: string | null;
  /**
   * SF-207-05 / SF-207-05b : 3 champs IA Travail BE pour pré-fill F-207
   * (référé tribunal du travail BE — CJ art. 584). BE-only — restent null
   * pour FR (régime distinct du référé prud'homal R.1454-1 CT).
   * `motifUrgenceDetecte` : code parmi la whitelist
   *   {HARCELEMENT, SALAIRE_IMPAYE, MODIFICATION_UNILATERALE, AUTRE} détecté
   *   par le pipeline IA sur le motif d'urgence (texte libre du dossier).
   *   Toute valeur hors whitelist → null.
   * `dateFaitGenerateurUrgence` : date du fait générateur de l'urgence (ISO
   *   YYYY-MM-DD) — point de départ du raisonnement « urgence qualifiable ».
   * `perilImmediatPresume` : flag présumant un péril en demeure caractérisé
   *   (préjudice imminent / irréversible) — pré-fill du booléen `perilEnDemeure`.
   */
  motifUrgenceDetecte?: string | null;
  dateFaitGenerateurUrgence?: string | null;
  perilImmediatPresume?: boolean | null;
  /**
   * SF-207-06 / SF-207-06b : 4 champs IA Travail BE pour pré-fill F-207
   * (RCC BE — conditions d'éligibilité, ex-prépension). BE-only — restent
   * null pour FR (régime distinct, sans équivalent direct).
   * `dateNaissanceSalarie` : date de naissance du salarié (ISO YYYY-MM-DD).
   *   Sert au calcul de l'âge à la date de licenciement envisagée
   *   (seuils CCT 17 / CCT 17/13 / AR 03/05/2007).
   * `anneesCarriereSalarie` : nombre d'années de carrière professionnelle
   *   salariée cumulées (entier borné [0, 60]).
   * `metierLourdDetecte` : flag détecté de reconnaissance de métier lourd
   *   (CCT 17/13). True uniquement → pré-fill ; false / autre → laissé décoché.
   * `entrepriseEnDifficulteDetectee` : flag détecté de reconnaissance
   *   d'entreprise en difficulté par arrêté ministériel (AR 03/05/2007 art. 8).
   *   True uniquement → pré-fill ; false / autre → laissé décoché.
   */
  dateNaissanceSalarie?: string | null;
  anneesCarriereSalarie?: number | null;
  metierLourdDetecte?: boolean | null;
  entrepriseEnDifficulteDetectee?: boolean | null;
  /**
   * SF-207-07 / SF-207-07b : 3 champs IA Travail BE pour pré-fill F-207
   * (RCC BE — indemnité complémentaire, calculateur CCT 17 art. 5). BE-only —
   * restent null pour FR (pas d'équivalent direct du RCC en droit français).
   * `remunerationNetteReferenceRccDetectee` : rémunération nette mensuelle de
   *   référence à la rupture (€). Base de calcul de la formule CCT 17 art. 5
   *   (indemnité = (remunNette − allocOnem) / 2, plancher 0).
   * `allocationOnemMensuelleEstimee` : allocation ONEM mensuelle estimée (€).
   *   Fournie par l'avocat (formule complexe ONEM hors scope IA — estimation
   *   conservative seulement).
   * `dateDebutRccEnvisagee` : date de début effective du RCC (ISO YYYY-MM-DD).
   *   Base de comptage du nombre de mensualités jusqu'à l'âge légal de la pension.
   */
  remunerationNetteReferenceRccDetectee?: number | null;
  allocationOnemMensuelleEstimee?: number | null;
  dateDebutRccEnvisagee?: string | null;
  // -------------------------------------------------------------------------
  // SF-246-21 — sous-objet `requalification_detection` (CDD + intérim)
  // FR uniquement — null pour dossier Travail BE.
  // -------------------------------------------------------------------------
  /** Durée du dernier CDD en mois [0, 120]. Source : contrat CDD. */
  cddDureeMois?: number | null;
  /** Date de fin du dernier CDD (ISO YYYY-MM-DD). */
  cddDateFinDernierContrat?: string | null;
  /** Date de début du CDD suivant — succession de CDD (ISO). */
  cddNouveauDateDebut?: string | null;
  /** Date de fin du CDD suivant (ISO). */
  cddNouveauDateFin?: string | null;
  /** Total des salaires bruts sur la durée du CDD (€ > 0). */
  cddTotalSalairesBruts?: number | null;
  /** Durée totale cumulée des missions d'intérim en mois [0, 120]. */
  interimDureeTotaleMois?: number | null;
  /** Date de fin de la dernière mission d'intérim (ISO YYYY-MM-DD). */
  interimDateFinDerniereMission?: string | null;
  /** Date de début d'une nouvelle mission d'intérim (ISO). */
  interimNouvellesMissionDateDebut?: string | null;
  /** Date de fin d'une nouvelle mission d'intérim (ISO). */
  interimNouvellesMissionDateFin?: string | null;
  /** Nom ou SIRET de l'entreprise utilisatrice (≤ 200 car.). */
  interimEntrepriseUtilisatrice?: string | null;
  /** Total des rémunérations brutes sur toutes missions (€ > 0). */
  interimTotalRemunerationsBrutes?: number | null;
  /** Durée de la mission d'intérim en jours calendaires [0, 3650]. */
  interimDureeMissionJours?: number | null;
  // -------------------------------------------------------------------------
  // SF-246-21 — sous-objet `paie_detection`
  // FR uniquement — null pour dossier Travail BE.
  // -------------------------------------------------------------------------
  /** Jours de congés payés acquis [0, 50]. Source : bulletins / STC. */
  congesJoursAcquis?: number | null;
  /** Jours de congés payés pris [0, 50]. */
  congesJoursPris?: number | null;
  /** Salaire effectivement versé par mois (€ > 0). Distinct du montant dû. */
  rappelSalaireMontantPerverseMensuel?: number | null;
  /** Date de début de la période de rappel — premier mois impayé (ISO). */
  rappelSalairePeriodeDebut?: string | null;
  /** Date de fin de la période de rappel — dernier mois impayé (ISO). */
  rappelSalairePeriodeFin?: string | null;
  // -------------------------------------------------------------------------
  // SF-246-21 — sous-objet `rupture_collective_detection`
  // FR uniquement — null pour dossier Travail BE.
  // -------------------------------------------------------------------------
  /** Âge du salarié en années [16, 80] — extractible si pièce d'identité aux pièces. */
  salarieAgeAnnees?: number | null;
  /** Effectif de l'entreprise (PSE) en nb salariés [0, 100000]. */
  pseNombreSalaries?: number | null;
  /** Nombre de licenciements envisagés dans le PSE [0, 100000]. */
  pseNombreLicenciements?: number | null;
  /** Date de signature du protocole transactionnel (ISO YYYY-MM-DD). */
  transactionDateSignature?: string | null;
  /** Montant de l'indemnité transactionnelle (€ > 0). */
  transactionIndemniteMontantEur?: number | null;
  // -------------------------------------------------------------------------
  // SF-246-21 — sous-objet `sante_discrimination_detection`
  // FR uniquement — null pour dossier Travail BE.
  // -------------------------------------------------------------------------
  /** Date de l'accident du travail (ISO YYYY-MM-DD) — distincte de dateLicenciement. */
  atDateAccident?: string | null;
  /** Date de première exposition au risque MP (ISO). Distincte de atDateAccident. */
  atDateExposition?: string | null;
  /**
   * Type de décision France Travail contestée — whitelist :
   * REFUS_INSCRIPTION | RADIATION | SUPPRESSION_ARE | REDUCTION_ARE | EXCLUSION_TEMPORAIRE | AUTRE
   */
  areTypeDecision?: string | null;
  /** Montant contesté dans la décision France Travail (€ > 0). */
  areMontantConteste?: number | null;
  /**
   * Motif de discrimination — whitelist :
   * SEXE | AGE | ORIGINE | HANDICAP | RELIGION | ORIENTATION_SEXUELLE | GROSSESSE | ACTIVITES_SYNDICALES | AUTRE
   */
  discriminationMotif?: string | null;
  /**
   * Contexte de l'acte discriminatoire — whitelist :
   * REFUS_EMBAUCHE | LICENCIEMENT | MUTATION | SANCTION_DISCIPLINAIRE | PROMOTION_REFUSEE | REMUNERATION_INFERIEURE | HARCELEMENT | AUTRE
   */
  discriminationContexte?: string | null;
  // -------------------------------------------------------------------------
  // SF-246-21 — sous-objet `procedure_details_detection`
  // FR uniquement — null pour dossier Travail BE.
  // -------------------------------------------------------------------------
  /** Montant de la provision demandée en référé prud'homal (€ > 0). */
  refereMontantProvision?: number | null;
  /** Date du certificat de travail (ISO YYYY-MM-DD). */
  documentsDateCertificatTravail?: string | null;
  /** Date de l'attestation France Travail (ISO YYYY-MM-DD). */
  documentsDateAttestationFranceTravail?: string | null;
  /** Date du solde de tout compte signé (ISO YYYY-MM-DD). */
  documentsDateSoldeToutCompte?: string | null;
  // -------------------------------------------------------------------------
  // SF-246-23 — sous-objet `travail_be_detection` (BELGIQUE uniquement)
  // null pour tout dossier Travail FRANCE.
  // -------------------------------------------------------------------------
  /**
   * BELGIQUE — Date à laquelle l'employeur a eu connaissance du fait constituant
   * le motif grave (ISO YYYY-MM-DD). Point de départ du délai de 3 j ouvrables
   * art. 35 Loi 03/07/1978. Strictement antérieure à dateLicenciement.
   * Pré-fill F-DT-27 motif-grave-be.
   */
  dateConnaissanceFait?: string | null;
  /**
   * BELGIQUE — Date à laquelle l'employeur a notifié les motifs de la rupture
   * au travailleur par lettre recommandée (ISO YYYY-MM-DD). Point d'arrivée du
   * 2e délai de 3 j ouvrables. Strictement postérieure à dateLicenciement.
   * Pré-fill F-DT-27 motif-grave-be.
   */
  dateNotificationMotifs?: string | null;
  /**
   * BELGIQUE — Numéro ou libellé de la commission paritaire applicable (≤ 20 car.,
   * ex. "CP 200", "SCP 200.01"). Concept distinct de conventionCollective (IDCC FR).
   * Pré-fill F-DT-28 avantages-conventionnels-be.
   */
  commissionParitaireBe?: string | null;
  /**
   * BELGIQUE — Jours de travail effectif (ou assimilés) au cours de l'année
   * précédente [0, 365]. Base pécule de vacances simple (Loi 28/06/1971).
   * Pré-fill F-DT-28 avantages-conventionnels-be.
   */
  joursTravaillesAnneePrecedenteBe?: number | null;
  /**
   * BELGIQUE — Jours effectivement prestés depuis le 1er avril de l'exercice
   * courant [0, 365]. Distinct de joursTravaillesAnneePrecedenteBe (année précédente).
   * Pré-fill F-DT-28 avantages-conventionnels-be.
   */
  joursPrestesBe?: number | null;
  /**
   * BELGIQUE — Date à laquelle le travailleur a formellement introduit sa demande
   * de crédit-temps auprès de l'employeur (ISO YYYY-MM-DD). Distincte de dateEntree
   * et de la date d'entrée en vigueur du crédit-temps.
   * Pré-fill F-DT-29 credit-temps-be.
   */
  dateDemandeCreditTemps?: string | null;
  /**
   * F-DT-38 (rupture période d'essai) — flag accident du travail / maladie pro
   * détecté dans le dossier (déclencheur protection rupture pendant arrêt AT/MP).
   * Référencé par `RupturePeriodeEssaiSectionPrefillRules.computeArretAccidentTravail`.
   * Dette frontend résorbée ici (le helper consommait un champ encore non typé).
   */
  atMpDetecte?: boolean | null;
  // -------------------------------------------------------------------------
  // SF-206-01 — sous-objet `abandon_poste_detail` (FRANCE uniquement)
  // Pré-fill F-DT-42 (abandon de poste / présomption de démission).
  // Tous nullables — restent `null` pour un dossier Travail BE (mécanisme
  // franco-français de la loi 21/12/2022).
  // -------------------------------------------------------------------------
  /** Date de présentation de la mise en demeure de reprendre le poste (ISO YYYY-MM-DD). */
  abandonPosteDateMiseEnDemeure?: string | null;
  /** Mode de notification de la MED (LRAR | REMISE_MAIN_PROPRE | AUTRE). */
  abandonPosteModeNotification?: string | null;
  /** Délai accordé par l'employeur au salarié pour reprendre / justifier (en jours). */
  abandonPosteDelaiAccordeJours?: number | null;
  /** Motif d'absence invoqué (AUCUN | MEDICAL | DROIT_RETRAIT | DROIT_GREVE | MODIFICATION_CONTRAT_REFUSEE | DEFAUT_PAIEMENT_SALAIRE | AUTRE). */
  abandonPosteMotifAbsence?: string | null;
  /** Date de reprise du poste ou de justification de l'absence (ISO YYYY-MM-DD). */
  abandonPosteDateReprise?: string | null;
  /** True si la MED mentionne le délai imparti (D.1237-2-1). */
  abandonPosteMedMentionneDelai?: boolean | null;
  /** True si la MED mentionne les conséquences (présomption de démission). */
  abandonPosteMedMentionneConsequences?: boolean | null;
  /** True si reprise ou justification effectivement intervenue dans le délai. */
  abandonPosteRepriseDansDelai?: boolean | null;
}

/** SF-155-04 : agrégat heures sup (totaux déclarés 25 % / 50 % / hors contingent). */
export interface HeuresSupMentionnees {
  totalDeclarees25pct?: number | null;
  totalDeclarees50pct?: number | null;
  horsContingent?: number | null;
}

export interface ImmigrationExtractedData {
  dateExpirationTitre?: string | null;
  typeTitreSejour?: string | null;
  typeProcedureDetectee?: string | null;
  dateDepotProcedure?: string | null;
  typeTitreSejourCode?: string | null;
  nationaliteUe?: boolean | null;
  typeRecoursCode?: string | null;
  dateNotificationDecisionContestee?: string | null;
  /** SF-IM-01-04 : type de checklist inféré pour F-IM-01 (13 régimes V1). */
  inferredChecklistType?: string | null;
  /** SF-155-04-00-BE-immig-FR : date notification OQTF avec délai (F-IM-08-02). FR uniquement. */
  dateNotificationOqtf?: string | null;
  /** SF-155-04-00-BE-immig-FR : code motif OQTF avec délai (F-IM-08-02), aligné enum MotifOqtf front. FR uniquement. */
  motifOqtfCode?: 'REFUS_TITRE' | 'EXPIRATION_TITRE' | 'SEJOUR_IRREGULIER' | 'RETRAIT_TITRE' | 'AUTRE' | null;
  /** SF-155-04-00-BE-immig-FR : indicateur détection recours OQTF déjà formé (F-IM-08-02 / F-IM-08-04). FR uniquement. */
  recoursFormeDetected?: DetectedAnswer | null;
  /** SF-155-04-00-BE-immig-FR : horodatage notification OQTF sans délai ISO partiel (F-IM-08-04, urgence 48h). FR uniquement. */
  dateHeureNotificationOqtfSansDelai?: string | null;
  /** SF-155-04-00-BE-immig-FR : placement en CRA concomitant à l'OQTF sans délai (F-IM-08-04). FR uniquement. */
  placementCraDetected?: boolean | null;
  // SF-155-04-00-BE-immig-BE : 4 champs Annexe 13 BE pour pré-fill F-IM-08-06
  // (BELGIQUE uniquement — dossiers FR : null).
  /** Date de notification de l'Annexe 13 / OQT belge (YYYY-MM-DD). */
  dateNotificationAnnexe13?: string | null;
  /** Délai de départ volontaire imposé par l'OE (entier ≥ 0, typiquement 0/7/30). */
  delaiDepartImposeJours?: number | null;
  /**
   * Code motif OQT belge (Annexe13BeCalculator.MOTIFS_VALIDES) :
   * SEJOUR_IRREGULIER_ART_7 | REFUS_SEJOUR_APRES_DEMANDE | FIN_SEJOUR_REGULIER | AUTRE.
   */
  motifOqtCodeBe?: string | null;
  /** Indices factuels d'un transfert imminent vers CRA ou frontière (signal critique). */
  transfertImminentDetected?: boolean | null;
  /**
   * F-197 SF-197-02 : override avocat single-value du type de procédure
   * (Immigration). Si présent, prend précédence sur {@link #typeProcedureDetectee}
   * pour le pré-remplissage des outils décisionnels au prochain run de
   * Synthèse enrichie (F-IM-08/20). Null tant qu'aucun override n'a été
   * posé. Persisté côté backend dans la table `case_file_type_litige_override`
   * (cf. SF-197-01).
   */
  typeProcedureAvocatOverride?: string | null;
  /**
   * SF-246-04 : date de l'ordonnance de protection JAF (Cciv 515-9) pour
   * pré-fill de l'outil F-IM-24 (victime de violences L.425-6). FR uniquement
   * — dossier BE : null.
   */
  dateOrdonnanceProtectionJaf?: string | null;
  /** F-235 : nationalité du requérant en texte libre (ex. "Algérienne"). */
  nationalite?: string | null;
  /** SF-246-16 : identité requérant + référence décision contestée pour pré-fill F-IM-06. */
  nomRequerant?: string | null;
  prenomRequerant?: string | null;
  dateDecisionContestee?: string | null;
  referenceDecision?: string | null;
  /**
   * SF-246-17 : pré-fill outils dublin-recours (F-IM-22) et crrv-refus-visa (F-IM-23).
   * FRANCE uniquement — dossier BE : null. Tous nullables.
   * `dublinEtatMembreResponsable` : texte libre (≤ 200 car.).
   * `dublinMotifTransfert` : code parmi DEMANDE_ASILE_AUTRE_ETAT / VISA_DELIVRE_AUTRE_ETAT /
   *   ENTREE_IRREGULIERE_AUTRE_ETAT / MEMBRE_FAMILLE_AUTRE_ETAT / AUTRE.
   * `crrvTypeVisa` : code parmi COURT_SEJOUR / LONG_SEJOUR / REGROUPEMENT_FAMILIAL / ETUDIANT / AUTRE.
   * `crrvMotifRefus` : texte libre (≤ 500 car.).
   */
  dublinEtatMembreResponsable?: string | null;
  dublinMotifTransfert?: string | null;
  crrvTypeVisa?: string | null;
  crrvMotifRefus?: string | null;
  /**
   * SF-246-18 : pré-fill outils AES Immigration FR.
   * FRANCE uniquement — dossier BE : null. Tous nullables.
   * `aesDateEntreeFrance` : date d'entrée en France ISO YYYY-MM-DD.
   * `aesDureePresenceMois` : mois entiers depuis aesDateEntreeFrance (calculé backend).
   * `aesAnneesScolariteConsecutives` : années d'études consécutives en France.
   * `aesNiveauEtudes` : LYCEE / BAC_PLUS_1_2 / BAC_PLUS_3_4 / BAC_PLUS_5_PLUS.
   * `aesDureeScolaritePlusAncienEnfantAnnees` : années scolarité enfant le plus ancien.
   * `aesMotifHumanitaire` : code motif humanitaire (6 valeurs).
   * `aesMoisActiviteSalariee` : mois salariat dans les 24 derniers mois (0–24).
   * `aesCodeMetier` : code ROME ou libellé métier en tension.
   */
  aesDateEntreeFrance?: string | null;
  aesDureePresenceMois?: number | null;
  aesAnneesScolariteConsecutives?: number | null;
  aesNiveauEtudes?: string | null;
  aesDureeScolaritePlusAncienEnfantAnnees?: number | null;
  aesMotifHumanitaire?: string | null;
  aesMoisActiviteSalariee?: number | null;
  aesCodeMetier?: string | null;
  /**
   * SF-246-19 : pré-fill statut & dispositifs Immigration FR.
   * FRANCE uniquement — dossier BE : null. Tous nullables.
   * `changementTitreEnvisage` : code titre envisagé (même whitelist que typeTitreSejourCode).
   * `changementRemunerationEur` : rémunération brute annuelle en euros (> 0, ≤ 500 000).
   * `natDureeResidenceReguliereAnnees` : durée résidence régulière en années (0–70, voies DECRET/ASCENDANT).
   * `natDureeMariageAnnees` : durée mariage avec Français(e) en années (0–70, voie MARIAGE).
   * `natAgeDemandeur` : âge du demandeur en années (0–120, voie ASCENDANT).
   * `mineursDateNaissance` : date de naissance du mineur YYYY-MM-DD (non future).
   * `algerienPresenceReguliereMois` : durée présence régulière en mois (0–600, régime algérien).
   * `asileDateDecisionAnterieure` : date décision antérieure asile YYYY-MM-DD (non future).
   * `eloiDureePresenceIrreguliereMois` : durée présence irrégulière en mois (0–600, IRTF).
   * `eloiMotifMenace` : motif menace (ORDRE_PUBLIC / SECURITE_ETAT / TERRORISME / RECIDIVE_GRAVE / AUTRE).
   */
  changementTitreEnvisage?: string | null;
  changementRemunerationEur?: number | null;
  natDureeResidenceReguliereAnnees?: number | null;
  natDureeMariageAnnees?: number | null;
  natAgeDemandeur?: number | null;
  mineursDateNaissance?: string | null;
  algerienPresenceReguliereMois?: number | null;
  asileDateDecisionAnterieure?: string | null;
  eloiDureePresenceIrreguliereMois?: number | null;
  eloiMotifMenace?: string | null;
  /**
   * SF-246-20 : pré-fill lot Immigration BE — 4 outils belgian-9bis / 9ter / 40bis / 40ter.
   * BELGIQUE UNIQUEMENT — null pour dossiers FRANCE. Tous nullables.
   * `be9bisDateEntreeBelgique` : date d'entrée en Belgique YYYY-MM-DD (art. 9bis, Annexe 26 / passeport), non future.
   * `be9bisDureePresenceMois` : mois entiers depuis be9bisDateEntreeBelgique jusqu'à aujourd'hui (calculé backend).
   * `be9terDateDebutSymptomes` : date du début des symptômes médicaux YYYY-MM-DD (art. 9ter, certificat médical), non future.
   * `be40bisLienFamilial` : lien familial 40bis — whitelist CONJOINT / ENFANT / ASCENDANT / PARTENAIRE_ENREGISTRE.
   * `be40terLienFamilial` : lien familial 40ter — même whitelist.
   * `be40terRevenusMensuelsNets` : revenus mensuels nets du regroupant belge (€, > 0, ≤ 30 000).
   */
  be9bisDateEntreeBelgique?: string | null;
  be9bisDureePresenceMois?: number | null;
  be9terDateDebutSymptomes?: string | null;
  be40bisLienFamilial?: string | null;
  be40terLienFamilial?: string | null;
  be40terRevenusMensuelsNets?: number | null;
}

export interface CaseAnalysisVersionSummary {
  id: string;
  version: number;
  analysisType: 'STANDARD' | 'ENRICHED';
  updatedAt: string;
  faitsCount: number | null;
  pointsJuridiquesCount: number | null;
  risquesCount: number | null;
  questionsOuvertesCount: number | null;
  timelineCount: number | null;
}

export interface DiffItem {
  text: string;
  reason: string | null;
}

export interface SectionDiff {
  added: DiffItem[];
  removed: DiffItem[];
  unchanged: DiffItem[];
  enriched: DiffItem[];
}

export interface TimelineDiffItem {
  date: string;
  evenement: string;
  reason: string | null;
}

export interface TimelineSectionDiff {
  added: TimelineDiffItem[];
  removed: TimelineDiffItem[];
  unchanged: TimelineDiffItem[];
  enriched: TimelineDiffItem[];
}

export interface AnalysisDiff {
  from: { id: string; version: number; analysisType: string; updatedAt: string };
  to:   { id: string; version: number; analysisType: string; updatedAt: string };
  faits: SectionDiff;
  pointsJuridiques: SectionDiff;
  risques: SectionDiff;
  questionsOuvertes: SectionDiff;
  timeline: TimelineSectionDiff;
}
