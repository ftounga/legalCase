/**
 * SF-IM-09-08 : modèle frontend AES voie étudiante (FR uniquement).
 * Contrat figé par SF-IM-09-04 backend (PR #505).
 * Fondement juridique : circulaire Valls 28/11/2012 (point III)
 * actualisée Darmanin — art. L.412-1 CESEDA. Outil single-country FR.
 */

export type AesEtudiantNiveauEtudes =
  | 'LYCEE' | 'BAC_PLUS_1_2' | 'BAC_PLUS_3_4' | 'BAC_PLUS_5_PLUS';

export type AesEtudiantResultats =
  | 'EXCELLENT' | 'MOYEN_PASSABLE' | 'DIFFICULTES_REPETEES';

export type AesEtudiantVerdict = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

export interface AesEtudiantRequest {
  dateEntreeFrance: string; // ISO YYYY-MM-DD (requis, non futur)
  dureePresenceMois: number; // 0-600 (requis)
  anneesScolariteEnFranceConsecutives: number; // 0-20 (requis)
  niveauEtudesActuel: AesEtudiantNiveauEtudes; // requis
  resultatsAcademiques: AesEtudiantResultats; // requis
  inscriptionEtablissementReconnu: boolean;
  moyensSubsistance: boolean;
  menaceOrdrePublic: boolean;
  parcoursCoherent: boolean;
  dateDepotDemande?: string | null; // ISO YYYY-MM-DD optionnel
}

export interface AesEtudiantResponse {
  caseFileId: string;
  dateEntreeFrance: string;
  dureePresenceMois: number;
  anneesScolariteEnFranceConsecutives: number;
  niveauEtudesActuel: AesEtudiantNiveauEtudes;
  resultatsAcademiques: AesEtudiantResultats;
  inscriptionEtablissementReconnu: boolean;
  moyensSubsistance: boolean;
  menaceOrdrePublic: boolean;
  parcoursCoherent: boolean;
  dateDepotDemande: string | null;
  country: 'FRANCE';
  presence3AnsOk: boolean;
  scolarite2AnsConsecutivesOk: boolean;
  resultatsAcceptables: boolean;
  inscriptionValide: boolean;
  moyensOk: boolean;
  pasMenace: boolean;
  parcoursCoherentOk: boolean;
  scoreGlobal: number;
  verdictProbabiliteAcceptation: AesEtudiantVerdict;
  criteresNonRemplis: string[];
  dateExpirationInstructionSiDemande: string | null;
  formule: string;
  baseJuridique: string;
  messages: string[];
}
