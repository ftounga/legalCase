/**
 * SF-FA-13-02 : modèles TypeScript pour l'outil décisionnel
 * "Révisions post-divorce" (F-FA-13). FRANCE uniquement.
 *
 * Contrat figé par SF-FA-13-01 (backend, parallèle). Tout champ inconnu de
 * `aiData` est ignoré silencieusement par le composant.
 *
 * Articles juridiques de référence :
 * - Pension alimentaire : art. 373-2-13 Cciv
 * - Prestation compensatoire : art. 276-3 Cciv (révision exceptionnelle)
 * - Résidence enfants / droit de visite : art. 373-2-9 Cciv
 * - Déménagement parent : art. 373-2 Cciv (information préalable obligatoire)
 */

/** Types de révision post-divorce supportés (enum métier). */
export type TypeRevisionPostDivorce =
  | 'PENSION_ALIMENTAIRE'
  | 'RESIDENCE'
  | 'DROIT_VISITE'
  | 'PRESTATION_COMPENSATOIRE'
  | 'DEMENAGEMENT_PARENT';

/** Mode de résidence des enfants (cas RESIDENCE / DROIT_VISITE). */
export type ModeResidence =
  | 'ALTERNEE'
  | 'EXCLUSIVE_MERE'
  | 'EXCLUSIVE_PERE'
  | 'LIBRE';

/** Verdict de probabilité de succès de la demande de révision. */
export type VerdictRevisionPossible = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

export interface RevisionsPostDivorceRequest {
  typeRevision: TypeRevisionPostDivorce;
  /** ISO YYYY-MM-DD — date de la décision dont on demande la révision. */
  dateDecisionInitiale: string;
  /** Description du changement de circonstance (≥ 20 caractères). */
  changementCirconstance: string;
  // Revenus (cas PENSION_ALIMENTAIRE / PRESTATION_COMPENSATOIRE)
  revenusInitialsDebiteurEur?: number | null;
  revenusActuelsDebiteurEur?: number | null;
  revenusInitialsCreancierEur?: number | null;
  revenusActuelsCreancierEur?: number | null;
  // Enfants / résidence (cas RESIDENCE / DROIT_VISITE)
  nbEnfantsACharge?: number | null;
  ageEnfants?: number[] | null;
  modeResidenceActuel?: ModeResidence | null;
  modeResidenceDemande?: ModeResidence | null;
}

export interface RevisionsPostDivorceResponse {
  caseFileId: string;
  // Echo des inputs
  typeRevision: TypeRevisionPostDivorce;
  dateDecisionInitiale: string;
  changementCirconstance: string;
  revenusInitialsDebiteurEur: number | null;
  revenusActuelsDebiteurEur: number | null;
  revenusInitialsCreancierEur: number | null;
  revenusActuelsCreancierEur: number | null;
  nbEnfantsACharge: number | null;
  ageEnfants: number[] | null;
  modeResidenceActuel: ModeResidence | null;
  modeResidenceDemande: ModeResidence | null;
  // Calculs
  /** Écart relatif des revenus du débiteur (en %, signé). null si non applicable. */
  ecartRevenusPct: number | null;
  /** True si la modification est jugée substantielle (jurisprudence). */
  modificationSubstantielle: boolean;
  /** True si la motivation textuelle est suffisante (longueur + précision). */
  motivationSuffisante: boolean;
  /** Score pondéré 0-100. */
  scoreGlobal: number;
  verdictRevisionPossible: VerdictRevisionPossible;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}

/** Options du select `typeRevision` (label avocat lisible). */
export interface TypeRevisionOption {
  code: TypeRevisionPostDivorce;
  label: string;
}

export const TYPES_REVISION_FR: ReadonlyArray<TypeRevisionOption> = [
  { code: 'PENSION_ALIMENTAIRE', label: 'Pension alimentaire (art. 373-2-13)' },
  { code: 'PRESTATION_COMPENSATOIRE', label: 'Prestation compensatoire (art. 276-3)' },
  { code: 'RESIDENCE', label: 'Résidence des enfants (art. 373-2-9)' },
  { code: 'DROIT_VISITE', label: 'Droit de visite et d\'hébergement (art. 373-2-9)' },
  { code: 'DEMENAGEMENT_PARENT', label: 'Déménagement d\'un parent (art. 373-2)' },
];

/** Options du select `modeResidence`. */
export interface ModeResidenceOption {
  code: ModeResidence;
  label: string;
}

export const MODES_RESIDENCE_FR: ReadonlyArray<ModeResidenceOption> = [
  { code: 'ALTERNEE', label: 'Alternée' },
  { code: 'EXCLUSIVE_MERE', label: 'Exclusive — mère' },
  { code: 'EXCLUSIVE_PERE', label: 'Exclusive — père' },
  { code: 'LIBRE', label: 'Libre / amiable' },
];

export function typeRevisionLabel(code: TypeRevisionPostDivorce): string {
  return TYPES_REVISION_FR.find((t) => t.code === code)?.label ?? code;
}

export function modeResidenceLabel(code: ModeResidence | null | undefined): string {
  if (!code) return '';
  return MODES_RESIDENCE_FR.find((m) => m.code === code)?.label ?? code;
}

export function verdictRevisionLabel(verdict: VerdictRevisionPossible): string {
  switch (verdict) {
    case 'ELEVEE': return 'Probabilité élevée';
    case 'MOYENNE': return 'Probabilité moyenne';
    case 'FAIBLE': return 'Probabilité faible';
  }
}
