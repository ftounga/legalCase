/**
 * SF-IM-09-07 : modèle frontend AES voie humanitaire (L.435-2 CESEDA + L.432-14
 * commission du titre de séjour). Single-country FRANCE — pas d'équivalent
 * standard en droit belge.
 *
 * Contrat importé du backend SF-IM-09-03 (PR #507).
 */

/** Motifs humanitaires reconnus par l'art. L.435-2 CESEDA (faisceau d'indices). */
export type MotifHumanitaire =
  | 'RISQUES_AU_RETOUR'
  | 'ISOLEMENT_TOTAL'
  | 'VICTIME_VIOLENCES'
  | 'VICTIME_TRAITE'
  | 'SITUATION_MEDICALE_PRECAIRE_HORS_L425_9'
  | 'AUTRE_HUMANITAIRE';

export type AesHumanitaireVerdict = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

export interface AesHumanitaireRequest {
  dateEntreeFrance: string; // YYYY-MM-DD
  motifHumanitaireDominant: MotifHumanitaire;
  preuvesMedicales: boolean;
  preuvesViolencesOuTraite: boolean;
  demandeAsileDeposeeEtRejetee: boolean;
  commissionTitreSejourSaisie: boolean;
  menaceOrdrePublic: boolean;
  dateDepotDemande: string | null; // YYYY-MM-DD or null
}

export interface AesHumanitaireResponse {
  caseFileId: string;
  dateEntreeFrance: string;
  motifHumanitaireDominant: MotifHumanitaire;
  preuvesMedicales: boolean;
  preuvesViolencesOuTraite: boolean;
  demandeAsileDeposeeEtRejetee: boolean;
  commissionTitreSejourSaisie: boolean;
  menaceOrdrePublic: boolean;
  dateDepotDemande: string | null;
  country: 'FRANCE' | 'BELGIQUE';
  motifEligible: boolean;
  preuvesAdaptees: boolean;
  commissionRequise: boolean;
  pasMenace: boolean;
  scoreGlobal: number;
  verdictProbabiliteAcceptation: AesHumanitaireVerdict;
  criteresNonRemplis: string[];
  dateExpirationInstruction: string | null;
  formule: string;
  baseJuridique: string;
  messages: string[];
}
