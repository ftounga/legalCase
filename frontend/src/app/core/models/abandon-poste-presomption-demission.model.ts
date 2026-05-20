/**
 * SF-206-02 : modèles TypeScript de l'outil "Abandon de poste /
 * présomption de démission" (F-DT-42 — FRANCE uniquement).
 *
 * Contrat API importé de SF-206-01 (backend, endpoints POST/GET figés).
 * Fondement : L. 1237-1-1 CT (loi 21/12/2022), D. 1237-2-1 s. (décret 17/04/2023).
 */

/** Solidité de la contestation salariale de la présomption de démission. */
export type AbandonPosteVerdict =
  | 'CONTESTATION_SOLIDE'
  | 'CONTESTATION_INCERTAINE'
  | 'CONTESTATION_DIFFICILE';

/** Mode de notification de la mise en demeure de reprendre le poste. */
export type AbandonPosteModeNotification = 'LRAR' | 'REMISE_MAIN_PROPRE' | 'AUTRE';

/** Motif d'absence éventuellement invoqué par le salarié. */
export type AbandonPosteMotifAbsence =
  | 'AUCUN'
  | 'MEDICAL'
  | 'DROIT_RETRAIT'
  | 'DROIT_GREVE'
  | 'MODIFICATION_CONTRAT_REFUSEE'
  | 'DEFAUT_PAIEMENT_SALAIRE'
  | 'AUTRE';

/** Codes structurés des motifs de contestation détectés (cf. SF-206-01). */
export type AbandonPosteCodeContestation =
  | 'DT42_DELAI_INSUFFISANT'
  | 'DT42_MED_MENTION_DELAI_MANQUANTE'
  | 'DT42_MED_MENTION_CONSEQUENCES_MANQUANTE'
  | 'DT42_MODE_NOTIFICATION_IRREGULIER'
  | 'DT42_MOTIF_LEGITIME'
  | 'DT42_REPRISE_DANS_DELAI';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/abandon-poste-presomption-demission`.
 * Dates au format ISO `YYYY-MM-DD` (nullable). Commentaires nullable (max 1000).
 */
export interface AbandonPostePresomptionDemissionRequest {
  dateMiseEnDemeurePresentee: string | null;
  modeNotificationMiseEnDemeure: AbandonPosteModeNotification;
  delaiAccordeJours: number;
  miseEnDemeureMentionneDelai: boolean;
  miseEnDemeureMentionneConsequences: boolean;
  motifAbsenceInvoque: AbandonPosteMotifAbsence;
  motifAbsenceCommentaire: string | null;
  repriseOuJustificationDansDelai: boolean;
  dateRepriseOuJustification: string | null;
}

/** Motif de contestation détecté (élément de `motifsContestation`). */
export interface AbandonPosteMotifContestation {
  code: AbandonPosteCodeContestation;
  libelle: string;
  fondement: string;
  poids: number;
  explication: string;
}

/**
 * Réponse de l'endpoint POST / GET.
 *
 * Ré-expose l'intégralité du snapshot d'inputs pour permettre la ré-édition du
 * formulaire après rechargement, **plus** les champs calculés (verdict, score,
 * motifs détectés, dateExpirationDelai, bases juridiques).
 */
export interface AbandonPostePresomptionDemissionResponse
  extends AbandonPostePresomptionDemissionRequest {
  caseFileId: string;
  verdict: AbandonPosteVerdict;
  scoreContestation: number;
  motifsContestation: AbandonPosteMotifContestation[];
  /** Date d'expiration du délai imparti par la MED (échéance suivie en onglet Suivi). */
  dateExpirationDelai: string | null;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
