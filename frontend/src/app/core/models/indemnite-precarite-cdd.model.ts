/**
 * SF-DT-17-02 : modèles TypeScript pour l'outil décisionnel
 * "Indemnité de précarité CDD" (art. L.1243-8 Code du travail).
 *
 * Contrat API figé dans SF-DT-17-01 (backend).
 * Endpoint : POST/GET /api/v1/case-files/{caseFileId}/cdd-indemnite-precarite
 */

/**
 * Cas d'exclusion de l'art. L.1243-10 du Code du travail — situations dans
 * lesquelles l'indemnité de précarité n'est pas due.
 */
export type CasExclusionCdd =
  | 'CDD_ETUDIANT_VACANCES'
  | 'CDD_SAISONNIER'
  | 'CDD_USAGE'
  | 'CDI_REFUSE_PAR_SALARIE'
  | 'RUPTURE_ANTICIPEE_SALARIE'
  | 'RUPTURE_ANTICIPEE_FAUTE_GRAVE';

export interface CasExclusionOption {
  code: CasExclusionCdd;
  label: string;
  description: string;
}

/**
 * 6 cas d'exclusion L.1243-10 pour le <mat-select>.
 * Ordre : CDD structurellement exclus (1-3), puis comportements salarié (4-6).
 */
export const CAS_EXCLUSION_OPTIONS: CasExclusionOption[] = [
  {
    code: 'CDD_ETUDIANT_VACANCES',
    label: 'CDD étudiant vacances scolaires',
    description: 'CDD conclu avec un jeune pour la période des vacances (L.1243-10 1°)',
  },
  {
    code: 'CDD_SAISONNIER',
    label: 'CDD saisonnier',
    description: 'CDD saisonnier (L.1243-10 2° + L.1242-2 3°)',
  },
  {
    code: 'CDD_USAGE',
    label: 'CDD d\'usage',
    description: 'CDD d\'usage pour emploi par nature temporaire (L.1243-10 2°)',
  },
  {
    code: 'CDI_REFUSE_PAR_SALARIE',
    label: 'CDI proposé refusé par le salarié',
    description: 'CDI pour le même emploi ou un emploi similaire refusé (L.1243-10 3°)',
  },
  {
    code: 'RUPTURE_ANTICIPEE_SALARIE',
    label: 'Rupture anticipée à l\'initiative du salarié',
    description: 'Rupture à l\'initiative du salarié (L.1243-10 4°)',
  },
  {
    code: 'RUPTURE_ANTICIPEE_FAUTE_GRAVE',
    label: 'Rupture anticipée faute grave / force majeure',
    description: 'Faute grave du salarié ou force majeure (L.1243-10 4°)',
  },
];

export interface IndemnitePrecariteCddRequest {
  /** Total des rémunérations brutes perçues pendant le CDD (> 0). */
  totalSalairesBruts: number;
  /** 10 (défaut) ou 6 (convention collective étendue avec contrepartie L.1243-9). */
  tauxPrecarite?: number | null;
  /** Code d'exclusion L.1243-10 — nullable. */
  casExclusion?: CasExclusionCdd | null;
}

export interface IndemnitePrecariteCddResponse {
  caseFileId: string;
  totalSalairesBruts: number;
  tauxPrecarite: number;
  casExclusion: CasExclusionCdd | null;
  indemnitePrecarite: number;
  formule: string;
  baseJuridique: string;
  messages: string[];
}
