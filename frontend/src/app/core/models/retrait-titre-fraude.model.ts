/**
 * SF-214-42 : modèles miroirs du contrat API (backend SF-214-41) pour l'outil
 * décisionnel "Retrait titre pour fraude" (F-IM-45-retrait-titre-fraude-fr).
 * FRANCE uniquement — retrait d'un titre de séjour obtenu par fraude (mariage
 * gris, fausses déclarations, fraude documentaire) ou consécutif à la perte des
 * conditions de délivrance (CESEDA).
 *
 * Analyseur de validité : statut du recours (recours possible / urgent /
 * prescrit) + vices de procédure éventuels + motifs de contestation + délai du
 * recours devant le TA + base juridique applicable.
 */

export type StatutRetraitTitreFraude =
  | 'RECOURS_POSSIBLE'
  | 'URGENT'
  | 'PRESCRIT';

export type MotifRetraitTitreFraude =
  | 'MARIAGE_GRIS'
  | 'FAUSSES_DECLARATIONS'
  | 'FRAUDE_DOCUMENTAIRE'
  | 'PERTE_CONDITIONS';

export interface RetraitTitreFraudeRequest {
  dateRetrait: string; // YYYY-MM-DD — requis
  motifRetrait: MotifRetraitTitreFraude; // MARIAGE_GRIS | FAUSSES_DECLARATIONS | FRAUDE_DOCUMENTAIRE | PERTE_CONDITIONS
  miseEnDemeurePrealable: boolean; // une mise en demeure préalable a-t-elle été notifiée ?
  dateMiseEnDemeure?: string | null; // YYYY-MM-DD — optionnel
}

export interface RetraitTitreFraudeResponse {
  caseFileId: string;
  dateRetrait: string;
  motifRetrait: MotifRetraitTitreFraude;
  miseEnDemeurePrealable: boolean;
  dateMiseEnDemeure?: string | null;
  country: string;
  statut: StatutRetraitTitreFraude;
  vicesDeProcedure: string[]; // vices de procédure relevés (défaut de contradictoire, etc.)
  motifsContestation: string[]; // moyens de contestation au fond
  delaiRecoursTA: string; // YYYY-MM-DD — date limite du recours devant le TA
  baseJuridique: string; // base juridique applicable (CESEDA)
}
