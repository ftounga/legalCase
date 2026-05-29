/**
 * SF-214-32 : modèles miroirs du contrat API (backend SF-214-31) pour l'outil
 * décisionnel "Recours TA Nantes naturalisation" (F-IM-40-naturalisation-recours-ta-fr).
 * FRANCE uniquement — recours pour excès de pouvoir contre un refus de décret
 * de naturalisation devant le tribunal administratif de Nantes (délai 2 mois).
 *
 * Calculateur de délai : statut du recours + date d'échéance du recours TA +
 * jours restants + tribunal compétent (TA de Nantes) + motifs de recours +
 * bases juridiques.
 */

export type StatutNaturalisationRecoursTa =
  | 'RECOURS_POSSIBLE'
  | 'URGENT'
  | 'PRESCRIT';

export interface NaturalisationRecoursTaRequest {
  dateRefusDecret: string; // YYYY-MM-DD — requis
  motivationRefus?: string | null; // optionnel — texte libre
  recoursPrerequis: boolean; // recours administratif préalable effectué ?
}

export interface NaturalisationRecoursTaResponse {
  caseFileId: string;
  dateRefusDecret: string;
  motivationRefus?: string | null;
  recoursPrerequis: boolean;
  country: string;
  statut: StatutNaturalisationRecoursTa;
  dateEcheanceRecoursTa: string; // YYYY-MM-DD
  joursRestants: number;
  tribunalCompetent: string; // TA de Nantes
  basesJuridiques: string[];
  motifsRecoursDisponibles: string[];
}
