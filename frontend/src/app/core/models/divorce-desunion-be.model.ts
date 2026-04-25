/**
 * SF-FA-11-02 : modèles TypeScript pour l'outil décisionnel "Divorce pour
 * désunion irrémédiable" en droit belge (Code civil belge, art. 229 — loi
 * du 27/04/2007). Le seul mode de divorce contentieux possible en BE.
 *
 * Contrat figé par SF-FA-11-01 (backend, en parallèle).
 *
 * BELGIQUE uniquement. Le pendant français se trouve dans F-FA-08
 * (altération définitive — art. 237 Cciv) et F-FA-10 (divorce accepté —
 * art. 233 Cciv).
 */

export type DivorceDesunionBeVerdict = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

export interface DivorceDesunionBeRequest {
  /** Date de séparation effective (ISO YYYY-MM-DD). */
  dateSeparation: string;
  /**
   * Séparation consentue par les 2 époux ?
   *  - true → seuil de 6 mois requis
   *  - false → seuil de 12 mois requis
   */
  separationConsentue: boolean;
  /** Preuves matérielles de séparation (déclaration, témoignages). */
  preuvesSeparation: boolean;
  /** Preuves documentaires (baux distincts, factures, attestations). */
  preuvesDocumentaires: boolean;
  /** Tentatives de réconciliation signalées (signal contre la séparation). */
  tentativesReconciliation: boolean;
  /** Date d'assignation devant le tribunal (ISO YYYY-MM-DD). Optionnelle. */
  dateAssignation?: string | null;
}

export interface DivorceDesunionBeResponse {
  caseFileId: string;
  dateSeparation: string;
  separationConsentue: boolean;
  preuvesSeparation: boolean;
  preuvesDocumentaires: boolean;
  tentativesReconciliation: boolean;
  dateAssignation: string | null;
  /** Durée écoulée entre `dateSeparation` et assignation (ou aujourd'hui), en mois. */
  dureeSeparationMois: number;
  /** Seuil légal applicable (6 si consentue, 12 sinon). */
  seuilSeparationMois: number;
  /** `dureeSeparationMois >= seuilSeparationMois`. */
  delaiObjectifOk: boolean;
  /** `delaiObjectifOk && (preuvesSeparation || preuvesDocumentaires) && !tentativesReconciliation`. */
  conditionsReunies: boolean;
  /** Score pondéré 0-100. */
  scoreGlobal: number;
  verdictProbabilite: DivorceDesunionBeVerdict;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'BELGIQUE';
}
