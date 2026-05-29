/**
 * SF-214-28 : modèles miroirs du contrat API (backend SF-214-27) pour l'outil
 * décisionnel "MNA — évaluation de l'âge / recours JE" (F-IM-38-mna-evaluation-age-fr).
 * FRANCE UNIQUEMENT — l'évaluation de la minorité par l'ASE, la contestation des
 * examens osseux et le recours devant le juge des enfants (JE) sont spécifiques au
 * droit français (CASF / code civil / décret 2019-57).
 *
 * Le backend détermine le statut de la situation (en attente, recours JE urgent,
 * examen osseux contesté, pris en charge), calcule l'échéance de saisine du JE et
 * fournit les arguments de contestation de l'examen osseux, la procédure ASE et la
 * liste des droits attachés à la qualité de mineur isolé.
 */

export type StatutMnaEvaluationAge =
  | 'EN_ATTENTE_EVALUATION'
  | 'RECOURS_JE_URGENT'
  | 'EXAMEN_OSSEUX_CONTESTE'
  | 'PRIS_EN_CHARGE';

export interface MnaEvaluationAgeRequest {
  dateNaissanceDeclaree: string; // YYYY-MM-DD, requis
  evaluationASERefusee: boolean;
  dateRefusASE?: string | null; // YYYY-MM-DD, optionnel
  examenOsseuxOrdonne: boolean;
  resultatExamenOsseux?: string | null; // optionnel
}

export interface MnaEvaluationAgeResponse {
  caseFileId: string;
  dateNaissanceDeclaree: string;
  evaluationASERefusee: boolean;
  dateRefusASE?: string | null;
  examenOsseuxOrdonne: boolean;
  resultatExamenOsseux?: string | null;
  country: string;
  statut: StatutMnaEvaluationAge;
  dateEcheanceSaisineJE: string | null; // YYYY-MM-DD ou null
  contestationExamenOsseux: string[];
  procedureASE: string[];
  droitsAttaches: string[];
}
