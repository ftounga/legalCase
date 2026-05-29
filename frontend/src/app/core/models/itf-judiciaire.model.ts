/**
 * SF-214-38 : modèles miroirs du contrat API (backend SF-214-37) pour l'outil
 * décisionnel "ITF judiciaire" (F-IM-43-itf-judiciaire-fr).
 * FRANCE uniquement — interdiction du territoire français (ITF) prononcée par le
 * juge pénal à titre de peine principale ou complémentaire (art. 131-30 et s. du
 * Code pénal). À distinguer de l'IRTF (interdiction de retour, mesure
 * administrative prononcée par le préfet, CESEDA).
 *
 * Analyseur de validité / délais : statut de la voie de recours pénale
 * (APPEL_POSSIBLE / RECOURS_PRESCRIT / RELEVE_POSSIBLE / EN_COURS_PURGE) +
 * date d'échéance du relevé + voies de recours ouvertes + conditions du relevé +
 * encadré explicatif de distinction ITF vs IRTF.
 */

export type StatutItfJudiciaire =
  | 'APPEL_POSSIBLE'
  | 'RECOURS_PRESCRIT'
  | 'RELEVE_POSSIBLE'
  | 'EN_COURS_PURGE';

export interface ItfJudiciaireRequest {
  dateCondamnation: string; // YYYY-MM-DD — requis
  dureeITFAnnees: number; // durée de l'ITF (années)
  infractionPrincipale: string; // ≤ 200 caractères
  condamnationDefinitive: boolean; // condamnation définitive ?
  dateEcheanceRecoursPenal?: string | null; // YYYY-MM-DD — optionnel
}

export interface ItfJudiciaireResponse {
  caseFileId: string;
  dateCondamnation: string;
  dureeITFAnnees: number;
  infractionPrincipale: string;
  condamnationDefinitive: boolean;
  dateEcheanceRecoursPenal?: string | null;
  country: string;
  statut: StatutItfJudiciaire;
  dateEcheanceReleve: string; // YYYY-MM-DD
  voiesRecours: string[];
  requisReleve: string[];
  distinctionItfVsIrtf: string;
}
