/**
 * SF-212-28 : modèles TypeScript de l'outil "Burn-out — reconnaissance MP"
 * (F-DT-64-burnout-reconnaissance-mp, FRANCE uniquement — L. 461-1 al. 4
 * et 5 CSS ; tableau 57 maladies professionnelles ; comité régional de
 * reconnaissance des maladies professionnelles CRRMP ; circulaire DGT
 * 2016/01).
 *
 * Contrat API importé de SF-212-27 (backend, endpoints POST/GET figés
 * sur `/api/v1/case-files/{caseFileId}/burnout-reconnaissance-mp`).
 */

/** Verdict d'évaluation des chances de reconnaissance CRRMP — 3 niveaux. */
export type BurnoutAnalyseChances =
  | 'CHANCES_IMPORTANTES'
  | 'CHANCES_MODEREES'
  | 'CHANCES_FAIBLES';

/** Code structuré d'un facteur d'analyse F-IA-03. */
export type BurnoutCodeCritere =
  | 'DT64_DIAGNOSTIC_POSE'
  | 'DT64_TAUX_IPP'
  | 'DT64_LIEN_CAUSAL'
  | 'DT64_SURCHARGE_DOCUMENTEE';

/** Facteur du dossier exposé dans la réponse API. */
export interface BurnoutFacteurDossier {
  code: BurnoutCodeCritere;
  libelle: string;
  fondement: string;
  poids: number;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/burnout-reconnaissance-mp`.
 *
 * 8 champs : diagnostic posé, taux IPP estimé, années d'exposition,
 * surcharge documentée, manquements sécurité L. 4121-1, harcèlement
 * concomitant L. 1152-1, arrêts maladie multiples, lien causal direct
 * établi (L. 461-1 al. 4 CSS).
 */
export interface BurnoutReconnaissanceMpRequest {
  diagnosticBurnoutPose: boolean | null;
  tauxIPPEstime: number | null;
  anneesExpositionProfessionnelle: number | null;
  surchargeChargeDocumentee: boolean | null;
  manquementsSecuriteDocumentes: boolean | null;
  harcelementConcomitant: boolean | null;
  arretsMaladieMultiples: boolean | null;
  lienCausalDirectEtabli: boolean | null;
}

/**
 * Réponse de l'endpoint POST / GET — inclut le snapshot des inputs
 * (pour ré-édition du formulaire UI) ET les sorties calculées (verdict
 * 3 niveaux, score, conditions légales, délai d'instruction CRRMP,
 * facteurs F-IA-03, bases juridiques, messages).
 */
export interface BurnoutReconnaissanceMpResponse extends BurnoutReconnaissanceMpRequest {
  caseFileId: string;
  analyseChancesCRRMP: BurnoutAnalyseChances;
  scoreChances: number;
  /** True si taux IPP ≥ 25 % (L. 461-1 al. 4 CSS). */
  conditionIPPRemplie: boolean;
  /** True si taux IPP < 25 % — procédure CRRMP en l'état irrecevable. */
  alerteIPPInsuffisante: boolean;
  /** Délai d'instruction CRRMP en mois (4-6 — circulaire DGT 2016/01). */
  delaiInstructionMois: number;
  facteursDossier: BurnoutFacteurDossier[];
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
