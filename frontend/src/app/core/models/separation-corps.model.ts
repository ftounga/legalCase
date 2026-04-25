/**
 * SF-FA-21-02 : modèles TypeScript pour l'outil décisionnel
 * "Séparation de corps + conversion divorce art. 296+306 Cciv"
 * (F-FA-21, FRANCE uniquement).
 *
 * Contrat figé par SF-FA-21-01 (backend, parallèle).
 */

export type ModeProcedureSep =
  | 'CONSENTEMENT_MUTUEL'
  | 'ACCEPTATION_PRINCIPE'
  | 'FAUTE'
  | 'ALTERATION_DEFINITIVE';

export type VerdictConversion =
  | 'POSSIBLE'
  | 'PREMATUREE'
  | 'RECONCILIATION_BLOQUE';

export interface SeparationCorpsRequest {
  modeProcedure: ModeProcedureSep;
  /** ISO YYYY-MM-DD ou null (séparation pas encore prononcée). */
  dateJugementSeparationCorps: string | null;
  /** ISO YYYY-MM-DD ou null (requête de conversion pas encore déposée). */
  dateRequeteConversion: string | null;
  /** Durée de la séparation en années entières (depuis le jugement). */
  dureeSeparationAnnees: number;
  consentementMutuelConversion: boolean;
  patrimoineCommun: boolean;
  /** Nombre d'enfants mineurs encore concernés (≥ 0). */
  enfantsMineurs: number;
  demandeReconciliationFormulee: boolean;
}

export interface SeparationCorpsResponse {
  caseFileId: string;
  // input
  modeProcedure: ModeProcedureSep;
  dateJugementSeparationCorps: string | null;
  dateRequeteConversion: string | null;
  dureeSeparationAnnees: number;
  consentementMutuelConversion: boolean;
  patrimoineCommun: boolean;
  enfantsMineurs: number;
  demandeReconciliationFormulee: boolean;
  // output
  dureeSeparationOk: boolean;
  delaiConversion2AnsAtteint: boolean;
  conversionAutomatiquePossible: boolean;
  /** Score d'éligibilité conversion 0-100. */
  scoreEligibiliteConversion: number;
  verdictConversion: VerdictConversion;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}

// ---------------------------------------------------------------------------
// Options pour les mat-select.
// ---------------------------------------------------------------------------

export interface ModeProcedureSepOption {
  code: ModeProcedureSep;
  label: string;
}

export const MODES_PROCEDURE_SEP_FR: ModeProcedureSepOption[] = [
  { code: 'CONSENTEMENT_MUTUEL', label: 'Consentement mutuel (art. 296 al. 1)' },
  { code: 'ACCEPTATION_PRINCIPE', label: 'Acceptation du principe de la rupture' },
  { code: 'FAUTE', label: 'Faute (art. 296 + 242 Cciv)' },
  { code: 'ALTERATION_DEFINITIVE', label: 'Altération définitive du lien conjugal (art. 237)' },
];

// ---------------------------------------------------------------------------
// Helpers d'affichage.
// ---------------------------------------------------------------------------

export function modeProcedureSepLabel(code: ModeProcedureSep): string {
  return MODES_PROCEDURE_SEP_FR.find((o) => o.code === code)?.label ?? code;
}

export function verdictConversionLabel(v: VerdictConversion): string {
  switch (v) {
    case 'POSSIBLE': return 'Conversion possible';
    case 'PREMATUREE': return 'Conversion prématurée';
    case 'RECONCILIATION_BLOQUE': return 'Conversion bloquée (réconciliation)';
  }
}
