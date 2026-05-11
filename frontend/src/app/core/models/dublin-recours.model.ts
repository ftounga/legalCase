/**
 * SF-208-06 : modeles miroirs du contrat API SF-208-02 (backend, PR #915)
 * pour l'outil decisionnel "Dublin recours 7 j suspensif" (F-IM-22).
 * FR uniquement (CESEDA L.572+ + Reglement UE 604/2013 art. 27 + 29).
 */

export type MotifTransfertDublin =
  | 'DEMANDE_ASILE_AUTRE_ETAT'
  | 'VISA_DELIVRE_AUTRE_ETAT'
  | 'ENTREE_IRREGULIERE_AUTRE_ETAT'
  | 'MEMBRE_FAMILLE_AUTRE_ETAT'
  | 'AUTRE';

export type StatutDublin =
  | 'DISPONIBLE'
  | 'URGENT'
  | 'EXPIRE'
  | 'RECOURS_FORME';

export interface DublinRecoursRequest {
  dateNotificationDecisionTransfert: string; // YYYY-MM-DD
  etatMembreResponsable: string | null;
  motifTransfert: MotifTransfertDublin;
  recoursForme: boolean;
  dateRecours?: string | null;
}

export interface DublinRecoursResponse {
  caseFileId: string;
  dateNotificationDecisionTransfert: string;
  etatMembreResponsable: string | null;
  motifTransfert: MotifTransfertDublin;
  recoursForme: boolean;
  dateRecours: string | null;
  country: 'FRANCE';
  dateExpirationRecours: string;
  dateLimiteTransfertEffectif: string;
  joursRestants: number;
  statut: StatutDublin;
  effetSuspensif: string;
  formule: string;
  baseJuridique: string;
  messages: string[];
}

export interface MotifTransfertDublinOption {
  code: MotifTransfertDublin;
  label: string;
}

export const MOTIFS_TRANSFERT_DUBLIN: MotifTransfertDublinOption[] = [
  { code: 'DEMANDE_ASILE_AUTRE_ETAT', label: "Demande d'asile dans un autre Etat membre" },
  { code: 'VISA_DELIVRE_AUTRE_ETAT', label: 'Visa delivre par un autre Etat membre' },
  { code: 'ENTREE_IRREGULIERE_AUTRE_ETAT', label: 'Entree irreguliere par un autre Etat membre' },
  { code: 'MEMBRE_FAMILLE_AUTRE_ETAT', label: 'Membre de famille dans un autre Etat membre' },
  { code: 'AUTRE', label: 'Autre motif' },
];
