/**
 * SF-212-22 : modèles TypeScript de l'outil « Démission — validité et
 * caractère équivoque » (F-DT-41-demission-validite-equivoque, FRANCE
 * uniquement — L. 1237-1 CT ; Cass. soc. 09/05/2007 — la démission doit
 * résulter d'une volonté claire et non équivoque ; Cass. soc. constante —
 * requalification possible en prise d'acte de la rupture aux torts de
 * l'employeur ou en licenciement sans cause réelle et sérieuse).
 *
 * Contrat API importé de SF-212-21 (backend, endpoints POST/GET figés sur
 * `/api/v1/case-files/{caseFileId}/demission-validite-equivoque`).
 */

/** Mode d'expression de la démission. */
export type DemModeExpression = 'ECRIT_FORMEL' | 'EMAIL' | 'SMS' | 'ORAL' | 'AUTRE';

/** Verdict d'analyse de la démission — 3 niveaux. */
export type DemAnalyse = 'VOLONTE_CLAIRE' | 'DEMISSION_EQUIVOQUE' | 'RETRACTATION_POSSIBLE';

/** Code structuré F-IA-03 d'un facteur d'équivocité. */
export type DemCodeCritere =
  | 'DT41_CONTEXTE_ALTERCATION'
  | 'DT41_PRESSION'
  | 'DT41_RETRACTATION'
  | 'DT41_MANQUEMENTS_EMPLOYEUR';

/** Facteur d'équivocité exposé dans la réponse API. */
export interface DemFacteurEquivocite {
  code: DemCodeCritere;
  libelle: string;
  fondement: string;
  poids: number;
  explication: string;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/demission-validite-equivoque`.
 *
 * 7 champs : mode d'expression (select), contexte d'altercation (toggle),
 * pression / menace (toggle), rétractation dans délai (toggle), délai de
 * rétractation en jours (nombre), manquements employeur contemporains
 * (toggle), état émotionnel perturbé (toggle).
 */
export interface DemissionEquivoqueRequest {
  modeExpressionDemission: DemModeExpression;
  contexteAltercation: boolean;
  pressionOuMenace: boolean;
  retractationDansDelai: boolean;
  delaiRetractationJours: number | null;
  manquementsEmployeurContemporains: boolean;
  etatEmotionnelPerturbe: boolean;
}

/**
 * Réponse de l'endpoint POST / GET — inclut le snapshot des inputs (pour
 * ré-édition du formulaire UI) ET les sorties calculées (verdict 3 niveaux,
 * score équivocité, facteurs d'équivocité F-IA-03, indicateur de
 * requalification possible, bases juridiques, messages).
 */
export interface DemissionEquivoqueResponse extends DemissionEquivoqueRequest {
  caseFileId: string;
  analyseDemission: DemAnalyse;
  scoreEquivocite: number;
  facteursEquivocite: DemFacteurEquivocite[];
  requalificationPossible: boolean;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
