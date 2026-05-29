/**
 * SF-214-08 : modèles miroirs du contrat API (backend SF-214-07) pour l'outil
 * décisionnel "Validation VLS-TS OFII" (F-IM-28-vls-ts-validation-ofii-fr).
 * FR uniquement (CESEDA R.431-16+ — validation OFII dans les 3 mois de l'entrée).
 *
 * Calculateur de délai : statut de validation + date d'échéance + jours restants
 * + procédure de recours si le délai est expiré.
 */

export type TypeVlsTs =
  | 'ETUDIANT'
  | 'SALARIE'
  | 'VISITEUR'
  | 'CONJOINT_FRANCAIS'
  | 'AUTRE';

export type StatutVlsTsValidation =
  | 'A_VALIDER'
  | 'URGENT'
  | 'EXPIRE'
  | 'VALIDE';

export interface VlsTsValidationRequest {
  dateEntreeFrance: string; // YYYY-MM-DD
  typeVlsTs: TypeVlsTs;
  validationOFIIEffectuee: boolean;
  dateValidationOFII?: string | null; // YYYY-MM-DD, optionnel
}

export interface VlsTsValidationResponse {
  caseFileId: string;
  dateEntreeFrance: string;
  typeVlsTs: TypeVlsTs;
  validationOFIIEffectuee: boolean;
  dateValidationOFII?: string | null;
  country: string;
  statut: StatutVlsTsValidation;
  dateEcheanceValidation: string; // YYYY-MM-DD
  joursRestantsValidation: number | null;
  risqueIrregularite: boolean;
  procedureRecours: string | null;
}
