/**
 * SF-223-07 — Helper partagé pour l'outil "Loi applicable en droit de la famille
 * (Belgique — DIP)" (`dip-be-loi-applicable-famille`).
 *
 * Pré-fill F-246 (BELGIQUE UNIQUEMENT). Quatre champs sont factualisables depuis
 * le sous-objet IA `dip_famille_be_detection` (consolidé dans `Sf223Detail`,
 * @JsonUnwrapped → clés camelCase plates sur `familleExtractedData`) :
 *
 *  - `dipFamilleBeMatiereDetectee` (DIVORCE / REGIME_MATRIMONIAL / SUCCESSION) →
 *    champ `matiere` ;
 *  - `dipFamilleBeResidenceCommuneDetectee` (ISO 3166-1 alpha-2) → champ
 *    `residenceHabituelleCommune` ;
 *  - `dipFamilleBeNationaliteCommuneDetectee` (ISO 3166-1 alpha-2) → champ
 *    `nationaliteCommune` ;
 *  - `dipFamilleBeChoixLoiDetecte` (ISO 3166-1 alpha-2) → champ
 *    `choixLoiParLesParties`.
 *
 * `computePrefillCount` retourne le nombre de champs réellement pré-remplis (0 à
 * 4). La date (mariage / décès) et le lieu de situation des biens ne sont pas
 * pré-remplis ici (faiblement factualisables de manière stable en V1, laissés à
 * 0). Le static `getPrefillCount` du composant délègue à ce helper (parité
 * runtime/static — garde-fou `prefill-count-integrity`).
 */

interface DipBeLoiApplicableFamilleAiShape {
  dipFamilleBeMatiereDetectee?: string | null;
  dipFamilleBeResidenceCommuneDetectee?: string | null;
  dipFamilleBeNationaliteCommuneDetectee?: string | null;
  dipFamilleBeChoixLoiDetecte?: string | null;
}

export interface DipBeLoiApplicableFamillePrefillInput {
  aiData?: unknown;
  procedureChecks?: unknown[];
  aiQuestions?: unknown[];
  piecesManquantes?: unknown[];
  triggerEvents?: unknown[];
  workspaceCountry?: string;
}

const MATIERE_VALUES = new Set(['DIVORCE', 'REGIME_MATRIMONIAL', 'SUCCESSION']);
const ISO2 = /^[A-Z]{2}$/;

function isIso2(v: unknown): boolean {
  return typeof v === 'string' && ISO2.test(v);
}

/**
 * Nombre de champs pré-remplis par l'IA (0 à 4) — BELGIQUE uniquement, sinon 0.
 */
export function computePrefillCount(input: DipBeLoiApplicableFamillePrefillInput): number {
  if (input.workspaceCountry && input.workspaceCountry !== 'BELGIQUE') {
    return 0;
  }
  const ai = (input.aiData ?? null) as DipBeLoiApplicableFamilleAiShape | null;
  if (!ai || typeof ai !== 'object') {
    return 0;
  }
  let count = 0;
  if (typeof ai.dipFamilleBeMatiereDetectee === 'string'
      && MATIERE_VALUES.has(ai.dipFamilleBeMatiereDetectee)) {
    count += 1;
  }
  if (isIso2(ai.dipFamilleBeResidenceCommuneDetectee)) {
    count += 1;
  }
  if (isIso2(ai.dipFamilleBeNationaliteCommuneDetectee)) {
    count += 1;
  }
  if (isIso2(ai.dipFamilleBeChoixLoiDetecte)) {
    count += 1;
  }
  return count;
}

export const DipBeLoiApplicableFamilleSectionPrefillRules = {
  computePrefillCount,
};
