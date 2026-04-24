/**
 * SF-IM-09-06 : modèle frontend AES voie familiale (L.435-1 CESEDA + circulaire
 * Valls 28/11/2012). Single-country FRANCE — pas d'équivalent BE standard.
 * Contrat importé de SF-IM-09-02 (PR #506).
 */

export type AesFamilleVerdict = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

export interface AesFamilleRequest {
  dateEntreeFrance: string; // YYYY-MM-DD
  dureePresenceMois: number;
  conjointFrancaisOuRegulier: boolean;
  enfantsScolarisesFrance: number;
  dureeScolaritePlusAncienEnfantAnnees: number;
  preuvesInsertion: boolean;
  menaceOrdrePublic: boolean;
  dateDepotDemande: string | null; // YYYY-MM-DD or null
}

export interface AesFamilleResponse {
  caseFileId: string;
  dateEntreeFrance: string;
  dureePresenceMois: number;
  conjointFrancaisOuRegulier: boolean;
  enfantsScolarisesFrance: number;
  dureeScolaritePlusAncienEnfantAnnees: number;
  preuvesInsertion: boolean;
  menaceOrdrePublic: boolean;
  dateDepotDemande: string | null;
  country: 'FRANCE' | 'BELGIQUE';
  presence5AnsOk: boolean;
  presence10AnsOk: boolean;
  liensFamiliauxOk: boolean;
  insertionOk: boolean;
  pasMenace: boolean;
  scoreGlobal: number;
  verdictProbabiliteAcceptation: AesFamilleVerdict;
  criteresNonRemplis: string[];
  dateExpirationInstructionSiDemande: string | null;
  formule: string;
  baseJuridique: string;
  messages: string[];
}
