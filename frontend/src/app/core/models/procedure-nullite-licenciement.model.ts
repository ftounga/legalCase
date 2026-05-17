/**
 * SF-DT-36-02 : modèles TypeScript de l'outil "Nullité de procédure de
 * licenciement" (F-DT-36 — vices de forme côté employeur). FRANCE uniquement.
 *
 * Contrat API importé de SF-DT-36-01 (backend, endpoints POST/GET figés).
 */

/** Verdict de nullité dérivé du score (0-100). */
export type ProcedureNulliteVerdict =
  | 'NULLITE_AVEREE'
  | 'NULLITE_PROBABLE'
  | 'PROCEDURE_REGULIERE';

/** Gravité d'un vice détecté. */
export type ProcedureNulliteGravite = 'AVERE' | 'PROBABLE';

/** Codes des 10 vices de procédure détectables (cf. SF-DT-36-01). */
export type ProcedureNulliteViceCode =
  | 'ABSENCE_CONVOCATION'
  | 'DELAI_CONVOCATION_INSUFFISANT'
  | 'ABSENCE_ENTRETIEN'
  | 'NOTIFICATION_PREMATUREE'
  | 'ABSENCE_LETTRE'
  | 'LETTRE_NON_MOTIVEE'
  | 'MOTIVATION_INSUFFISANTE'
  | 'ABSENCE_CONVOCATION_MOTIF_GRAVE'
  | 'PROCEDURE_CSE_NON_RESPECTEE'
  | 'CONVENTION_COLLECTIVE_NON_RESPECTEE';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/procedure-nullite-licenciement`.
 * Dates au format ISO `YYYY-MM-DD` (nullable). Commentaires nullable (max 1000).
 */
export interface ProcedureNulliteLicenciementRequest {
  convocationEnvoyee: boolean;
  dateConvocationPresentee: string | null;
  dateEntretienPrealable: string | null;
  entretienTenu: boolean;
  dateNotificationLicenciement: string | null;
  lettreLicenciementEcrite: boolean;
  lettreMotivee: boolean;
  motivationSuffisante: boolean;
  motivationCommentaire: string | null;
  licenciementPourMotifGrave: boolean;
  licenciementCollectif: boolean;
  procedureCseRespectee: boolean | null;
  conventionCollectiveApplicable: boolean;
  conventionCollectiveRespectee: boolean;
  conventionCollectiveCommentaire: string | null;
}

/** Vice de procédure détecté (élément de `vicesDetectes`). */
export interface ProcedureNulliteVice {
  code: ProcedureNulliteViceCode;
  libelle: string;
  fondement: string;
  gravite: ProcedureNulliteGravite;
  explication: string;
}

/**
 * Réponse de l'endpoint POST / GET.
 *
 * Ré-expose l'intégralité du snapshot d'inputs (hérité de
 * `ProcedureNulliteLicenciementRequest`) pour permettre la ré-édition du
 * formulaire après rechargement, **plus** les champs calculés (verdict, score,
 * vices, bases juridiques).
 */
export interface ProcedureNulliteLicenciementResponse
  extends ProcedureNulliteLicenciementRequest {
  caseFileId: string;
  verdict: ProcedureNulliteVerdict;
  scoreNullite: number;
  vicesDetectes: ProcedureNulliteVice[];
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
