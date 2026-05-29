/**
 * SF-214-14 : modèles miroirs du contrat API (backend SF-214-13) pour l'outil
 * décisionnel "Renouvellement délai dépôt" (F-IM-31-renouvellement-delai-depot-fr).
 * FR uniquement — calculateur du délai optimal/impératif de dépôt d'une demande
 * de renouvellement de titre de séjour avant son expiration.
 *
 * Calculateur de délai : statut de dépôt + date optimale de dépôt + date impérative
 * + jours restants + signaux risque d'irrégularité / alerte de retard.
 */

export type StatutRenouvellementDelai =
  | 'EN_AVANCE'
  | 'A_DEPOSER'
  | 'A_DEPOSER_URGENT'
  | 'EXPIRE'
  | 'DEPOSE';

export interface RenouvellementDelaiRequest {
  dateExpirationTitre: string; // YYYY-MM-DD, requis
  dateDepotDossier?: string | null; // YYYY-MM-DD, optionnel
  typeTitre?: string | null; // optionnel
}

export interface RenouvellementDelaiResponse {
  caseFileId: string;
  dateExpirationTitre: string;
  dateDepotDossier?: string | null;
  typeTitre?: string | null;
  country: string;
  statut: StatutRenouvellementDelai;
  dateOptimalDepot: string; // YYYY-MM-DD
  dateDepotImperatif: string; // YYYY-MM-DD
  joursRestantsAvantOptimal: number;
  joursRestantsAvantImperatif: number;
  risqueIrruption: boolean;
  alerteRetard: boolean;
}
