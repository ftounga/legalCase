import { DocumentPieceType } from './document.model';

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
