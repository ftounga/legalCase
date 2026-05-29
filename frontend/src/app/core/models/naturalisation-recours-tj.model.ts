/**
 * SF-214-30 : modèles miroirs du contrat API (backend SF-214-29) pour l'outil
 * décisionnel "Recours TJ naturalisation" (F-IM-39-naturalisation-recours-tj-fr).
 * FRANCE uniquement — recours juridictionnel contre un refus d'enregistrement /
 * une contestation de nationalité, délai de 6 mois devant le tribunal judiciaire.
 *
 * Calculateur de délai : statut du recours + date d'échéance du recours
 * judiciaire + jours restants + tribunal compétent + motifs de recours +
 * bases juridiques.
 */

export type VoieNaturalisation =
  | 'MARIAGE'
  | 'ASCENDANT'
  | 'MINEUR_22_1';

export type TypeRefus =
  | 'REFUS_ENREGISTREMENT'
  | 'CONTESTATION_NATIONALITE';

export type StatutNaturalisationRecoursTj =
  | 'RECOURS_POSSIBLE'
  | 'URGENT'
  | 'PRESCRIT';

export interface NaturalisationRecoursTjRequest {
  voieNaturalisation: VoieNaturalisation;
  dateRefusDeclaration: string; // YYYY-MM-DD
  typeRefus: TypeRefus;
}

export interface NaturalisationRecoursTjResponse {
  caseFileId: string;
  voieNaturalisation: VoieNaturalisation;
  dateRefusDeclaration: string;
  typeRefus: TypeRefus;
  country: string;
  statut: StatutNaturalisationRecoursTj;
  dateEcheanceRecoursJudicaire: string; // YYYY-MM-DD
  joursRestants: number;
  tribunalCompetent: string;
  basesJuridiques: string[];
  motifsRecoursDisponibles: string[];
}
