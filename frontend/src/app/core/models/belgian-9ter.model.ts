/**
 * SF-IM-14-06 : modèle TypeScript pour l'outil décisionnel
 * "Régularisation 9ter médical" (F-IM-14, BELGIQUE uniquement —
 * art. 9ter Loi 15/12/1980 + AR 17/05/2007 art. 7-8).
 *
 * Contrat figé par SF-IM-14-02 (backend, PR #509).
 */
export type Verdict9ter = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

export interface Belgian9terRequest {
  /** Date de début des symptômes (YYYY-MM-DD) — optionnel. */
  dateDebutSymptomes: string | null;
  /** Certificat médical type CM 1 et 2 produit. */
  maladieGraveCertifiee: boolean;
  /** Traitement nécessaire disponible en Belgique. */
  soinsNecessairesDisponiblesBe: boolean;
  /** Traitement inaccessible / impossible au pays d'origine. */
  soinsInaccessiblesPaysOrigine: boolean;
  /** Menace à l'ordre public présumée. */
  menaceOrdrePublic: boolean;
  /** Date de dépôt de la demande à l'OE (YYYY-MM-DD) — optionnel. */
  dateDepotDemande: string | null;
}

export interface Belgian9terResponse {
  caseFileId: string;
  dateDebutSymptomes: string | null;
  maladieGraveCertifiee: boolean;
  soinsNecessairesDisponiblesBe: boolean;
  soinsInaccessiblesPaysOrigine: boolean;
  menaceOrdrePublic: boolean;
  dateDepotDemande: string | null;
  country: 'BELGIQUE';
  certificatMedicalType1Ok: boolean;
  soinsRequisOk: boolean;
  inaccessibiliteOk: boolean;
  pasMenace: boolean;
  /** Score 0-100 (4 conditions × 25 pts). */
  scoreGlobal: number;
  /** Verdict de probabilité d'acceptation (ELEVEE ≥ 75, MOYENNE 50-74, FAIBLE < 50). */
  verdictProbabiliteAcceptation: Verdict9ter;
  criteresNonRemplis: string[];
  /** Date d'expiration prévisionnelle de l'instruction (dépôt + 12 mois). */
  dateExpirationInstruction: string | null;
  formule: string;
  baseJuridique: string;
  messages: string[];
}
