/**
 * SF-221-05 — modèles miroirs du contrat API (backend) pour l'outil décisionnel
 * « Recours CCE en suspension ordinaire (BE) » (F-IM-57-cce-suspension-be).
 *
 * BELGIQUE uniquement — évalue les conditions du recours CCE en SUSPENSION ORDINAIRE
 * (référé administratif : urgence non extrême + risque de préjudice grave difficilement
 * réparable, art. 39/82 Loi 15/12/1980 ; loi 15/09/2006). Greffé sur la requête en
 * annulation 30j.
 *
 * 3e recours CCE DISTINCT de l'annulation 30j (F-IM-31) et de l'extrême urgence
 * 5j ouvrables (F-IM-32).
 */

/**
 * Verdict de l'analyse :
 *  - CONDITIONS_REUNIES         : urgence + préjudice réunis, dans le délai 30j, pas d'éloignement imminent (vert)
 *  - URGENCE_NON_DEMONTREE      : urgence non invoquée / démontrée (orange)
 *  - PREJUDICE_NON_DEMONTRE     : préjudice grave difficilement réparable non démontré (orange)
 *  - HORS_DELAI_ANNULATION      : délai 30j dépassé, pas de recours en annulation introduit (orange)
 *  - REORIENTER_EXTREME_URGENCE : éloignement imminent → renvoi F-IM-32 (bleu info)
 */
export type CceSuspensionBeVerdict =
  | 'CONDITIONS_REUNIES'
  | 'URGENCE_NON_DEMONTREE'
  | 'PREJUDICE_NON_DEMONTRE'
  | 'HORS_DELAI_ANNULATION'
  | 'REORIENTER_EXTREME_URGENCE';

export interface CceSuspensionBeRequest {
  /** Date de notification de la décision attaquée (ISO yyyy-MM-dd, non future). */
  dateNotificationDecision: string;
  recoursAnnulationIntroduit: boolean;
  urgenceInvocable: boolean;
  risquePrejudiceGraveDifficilementReparable: boolean;
  mesureEloignementImminente: boolean;
}

export interface CceSuspensionBeResponse {
  caseFileId: string;
  dateNotificationDecision: string;
  recoursAnnulationIntroduit: boolean;
  urgenceInvocable: boolean;
  risquePrejudiceGraveDifficilementReparable: boolean;
  mesureEloignementImminente: boolean;
  verdict: CceSuspensionBeVerdict;
  /** Jours écoulés depuis la notification de la décision attaquée. */
  joursDepuisNotification: number;
  /** tool_id de renvoi cross-outil (F-IM-32 extrême urgence) si éloignement imminent, null sinon. */
  renvoiOutil: string | null;
  basesJuridiques: string[];
  messages: string[];
}

/** Délai calendaire de la requête en annulation, support de la suspension ordinaire. */
export const CCE_SUSPENSION_DELAI_ANNULATION_JOURS = 30;
