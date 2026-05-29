/**
 * SF-214-36 : modèles miroirs du contrat API (backend SF-214-35) pour l'outil
 * décisionnel "Assignation à résidence" (F-IM-42-assignation-residence-fr).
 * FRANCE uniquement — assignation à résidence prononcée en vue de l'exécution
 * d'une mesure d'éloignement (OQTF) ou comme mesure de surveillance (CESEDA).
 *
 * Analyseur de validité / délais : statut de l'assignation (en cours / proche de
 * l'expiration / expirée) + date d'échéance + durée totale autorisée +
 * possibilité de renouvellement + motifs de contestation + recours possible +
 * délai du recours devant le TA (48 h).
 */

export type StatutAssignationResidence =
  | 'EN_COURS'
  | 'EXPIRATION_PROCHE'
  | 'EXPIRE';

export type MotifAssignation =
  | 'EXECUTION_OQTF'
  | 'SURVEILLANCE_MESURE_ELOIGNEMENT'
  | 'AUTRE';

export interface AssignationResidenceRequest {
  dateNotificationAssignation: string; // YYYY-MM-DD — requis
  dureeAssignationJours: number; // durée de l'assignation (jours)
  motifAssignation: MotifAssignation; // EXECUTION_OQTF | SURVEILLANCE_MESURE_ELOIGNEMENT | AUTRE
  obligationsPresentation?: string | null; // obligations de présentation (optionnel)
}

export interface AssignationResidenceResponse {
  caseFileId: string;
  dateNotificationAssignation: string;
  dureeAssignationJours: number;
  motifAssignation: MotifAssignation;
  obligationsPresentation?: string | null;
  country: string;
  statut: StatutAssignationResidence;
  dateEcheanceAssignation: string; // YYYY-MM-DD
  dureeTotaleAutoriseeJours: number;
  renouvellementPossible: boolean;
  motifsContestation: string[];
  recoursPossible: boolean;
  delaiRecoursTaHeures: number; // délai du recours devant le TA (48 h)
}
