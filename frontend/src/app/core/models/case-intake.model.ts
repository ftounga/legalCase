// F-285 / SF-285-01 — Modèles de la qualification d'entrée du dossier (intake guidé).
// Alignés sur le contrat API figé de SF-285-01 (GET/PUT /api/v1/case-files/{id}/intake).

/** Verdict de recevabilité / compétence. */
export type AdmissibilityStatus = 'A_VERIFIER' | 'RECEVABLE' | 'IRRECEVABLE';

/** Statut de prescription / forclusion. */
export type PrescriptionStatus =
  | 'A_VERIFIER'
  | 'DANS_LES_DELAIS'
  | 'RISQUE_FORCLUSION'
  | 'PRESCRIT';

/**
 * Qualification d'entrée d'un dossier.
 * `qualified=false` (+ `qualifiedAt=null`) ⇒ dossier jamais qualifié (état vide UI).
 */
export interface CaseIntake {
  disputeType: string | null;
  admissibility: AdmissibilityStatus | null;
  prescriptionStatus: PrescriptionStatus | null;
  /** ISO date (yyyy-MM-dd) ou null. */
  prescriptionDeadline: string | null;
  jurisdiction: string | null;
  /** Valeur estimée du litige en centimes d'EUR, ou null. */
  estimatedValueCents: number | null;
  notes: string | null;
  qualifiedAt: string | null;
  qualified: boolean;
}

/** Corps de la requête PUT — tous les champs sont optionnels. */
export interface CaseIntakeUpdate {
  disputeType: string | null;
  admissibility: AdmissibilityStatus | null;
  prescriptionStatus: PrescriptionStatus | null;
  prescriptionDeadline: string | null;
  jurisdiction: string | null;
  estimatedValueCents: number | null;
  notes: string | null;
}
