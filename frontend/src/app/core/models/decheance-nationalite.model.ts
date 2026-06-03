/**
 * SF-220-05 : modèles miroirs du contrat API (backend SF-220-05) pour l'outil
 * décisionnel "validité d'une mesure de déchéance de nationalité"
 * (F-IM-51-decheance-nationalite-fr, Cciv 25 / 25-1). FR uniquement.
 *
 * Apprécie la régularité d'une mesure (envisagée ou prononcée) de déchéance de
 * la nationalité française : interdiction d'apatridie (binational requis),
 * délai Cciv 25-1, et calcul du délai de recours (REP Conseil d'État, 2 mois).
 * Distinct de F-IM-13 (acquisition de la nationalité) et de F-IM-39/40 (recours
 * refus naturalisation TJ / TA Nantes).
 */

export type MotifDecheance =
  | 'TERRORISME'
  | 'ATTEINTE_INTERETS_NATION'
  | 'FRAUDE_ACQUISITION'
  | 'AUTRE';

export type ValiditeDecheance =
  | 'CONDITIONS_REUNIES'
  | 'MESURE_CONTESTABLE'
  | 'MESURE_IRREGULIERE'
  | 'INDETERMINE';

export interface DecheanceNationaliteRequest {
  motif: MotifDecheance | null;
  binational: boolean | null;
  dateAcquisitionNationalite: string | null;
  dateFaits: string | null;
  mesurePrononcee: boolean;
  dateDecret: string | null;
}

export interface DecheanceNationaliteResponse {
  caseFileId: string;
  motif: MotifDecheance | null;
  binational: boolean | null;
  dateAcquisitionNationalite: string | null;
  dateFaits: string | null;
  mesurePrononcee: boolean;
  dateDecret: string | null;
  country: string;
  validite: ValiditeDecheance;
  conditionsManquantes: string[];
  voiesRecours: string[];
  delaiRecoursJours: number | null;
  basesJuridiques: string[];
  messages: string[];
}
