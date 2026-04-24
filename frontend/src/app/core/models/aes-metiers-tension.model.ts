/**
 * SF-IM-09-05 : modèle frontend AES Métiers en tension (FR uniquement).
 * Contrat figé par SF-IM-09-01 backend (PR #504).
 * Fondement juridique : loi n° 2024-42 du 26 janvier 2024 art. 26
 * créant l'art. L.435-4 CESEDA — voie expérimentale 1er janvier 2024
 * au 31 décembre 2026.
 */

export interface AesMetiersTensionRequest {
  dateEntreeFrance: string; // ISO YYYY-MM-DD (requis)
  moisActiviteSalarieeDernieres24Mois: number; // 0-24 (requis)
  metierEstEnTension: boolean;
  codeMetier?: string | null; // ROME, ex. "N1101"
  menaceOrdrePublic: boolean;
  contratOuPromesseValide: boolean;
  dateDepotDemande?: string | null; // ISO YYYY-MM-DD optionnel
}

export interface AesMetiersTensionResponse {
  caseFileId: string;
  dateEntreeFrance: string;
  moisActiviteSalarieeDernieres24Mois: number;
  metierEstEnTension: boolean;
  codeMetier: string | null;
  menaceOrdrePublic: boolean;
  contratOuPromesseValide: boolean;
  dateDepotDemande: string | null;
  country: 'FRANCE';
  presence3Ans: boolean;
  activite12MoisOk: boolean;
  conditionsReunies: boolean;
  criteresNonRemplis: string[];
  dateEligibiliteAtteinte: string | null;
  dateExpirationInstructionSiDemande: string | null;
  formule: string;
  baseJuridique: string;
  messages: string[];
}
