/**
 * SF-DT-16-02 : modèles TypeScript de l'outil "Licenciement nul — détection
 * multi-protections + indemnité plancher 6 mois" (F-DT-16, art. L.1235-3-1
 * al. 2 Code du travail). FRANCE uniquement (pendant BE backlog).
 *
 * Contrat API importé de SF-DT-16-01 (backend mergé PR #580).
 */

/** Codes de protection détectables (7 codes structurés — 8e cas LIBERTE_FONDAMENTALE non auto-détecté). */
export type LicenciementNulProtection =
  | 'MATERNITE'
  | 'ACCIDENT_TRAVAIL'
  | 'HARCELEMENT'
  | 'DISCRIMINATION'
  | 'LANCEUR_ALERTE'
  | 'MANDAT_SYNDICAL'
  | 'ACTION_JUSTICE';

/** Verdict de probabilité de nullité dérivé du score (0-100). */
export type LicenciementNulVerdict = 'FAIBLE' | 'MOYENNE' | 'ELEVEE';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/licenciement-nul-detection`.
 * Toutes les dates au format ISO `YYYY-MM-DD`.
 */
export interface LicenciementNulDetectionRequest {
  dateNotificationLicenciement: string;
  salarieEnceinte: boolean;
  dateAccouchement?: string | null;
  salarieAccidentTravail: boolean;
  dateConsolidationAT?: string | null;
  salarieHarceleAvere: boolean;
  salarieDiscriminationAlleguee: boolean;
  salarieMotifLanceurAlerte: boolean;
  salarieMandatRepresentant: boolean;
  salarieActionJustice: boolean;
  salaireMensuelBrutEur: number;
  ancienneteAnnees?: number | null;
}

/**
 * Réponse de l'endpoint — snapshot inputs (pré-remplissage UI) + outputs calculés.
 */
export interface LicenciementNulDetectionResponse {
  caseFileId: string;
  // --- Inputs (snapshot) ---
  dateNotificationLicenciement: string;
  salarieEnceinte: boolean;
  dateAccouchement: string | null;
  salarieAccidentTravail: boolean;
  dateConsolidationAT: string | null;
  salarieHarceleAvere: boolean;
  salarieDiscriminationAlleguee: boolean;
  salarieMotifLanceurAlerte: boolean;
  salarieMandatRepresentant: boolean;
  salarieActionJustice: boolean;
  salaireMensuelBrutEur: number;
  ancienneteAnnees: number | null;
  // --- Outputs calculés ---
  protectionsDetectees: LicenciementNulProtection[];
  nombreProtectionsActives: number;
  nulliteProbable: boolean;
  scoreNullite: number;
  verdictProbabiliteNullite: LicenciementNulVerdict;
  indemniteMinimumNuliteEur: number;
  indemniteMinimumMois: number;
  reintegrationOuverte: boolean;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}

/** Libellés UI des protections détectables. */
export interface ProtectionDescriptor {
  code: LicenciementNulProtection;
  label: string;
  article: string;
}

export const PROTECTION_LABELS: Readonly<Record<LicenciementNulProtection, ProtectionDescriptor>> = {
  MATERNITE: {
    code: 'MATERNITE',
    label: 'Maternité / grossesse',
    article: 'L.1225-4',
  },
  ACCIDENT_TRAVAIL: {
    code: 'ACCIDENT_TRAVAIL',
    label: 'Accident du travail / maladie pro.',
    article: 'L.1226-9',
  },
  HARCELEMENT: {
    code: 'HARCELEMENT',
    label: 'Harcèlement (moral ou sexuel)',
    article: 'L.1152-3',
  },
  DISCRIMINATION: {
    code: 'DISCRIMINATION',
    label: 'Discrimination',
    article: 'L.1132-4',
  },
  LANCEUR_ALERTE: {
    code: 'LANCEUR_ALERTE',
    label: "Lanceur d'alerte éthique",
    article: 'L.1132-3-3',
  },
  MANDAT_SYNDICAL: {
    code: 'MANDAT_SYNDICAL',
    label: 'Salarié protégé (mandat)',
    article: 'L.2411-1',
  },
  ACTION_JUSTICE: {
    code: 'ACTION_JUSTICE',
    label: 'Action en justice du salarié',
    article: 'L.1132-1',
  },
};
