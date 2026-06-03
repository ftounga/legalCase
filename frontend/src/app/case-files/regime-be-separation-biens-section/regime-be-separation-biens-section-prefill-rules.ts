/**
 * SF-223-06 — Helper partagé pour l'outil "Régime de séparation de biens
 * (Belgique)" (`regime-be-separation-biens`).
 *
 * Pré-fill F-246 (BELGIQUE UNIQUEMENT). Quatre champs sont factualisables depuis
 * le sous-objet IA `regime_separation_biens_be_detection` (consolidé dans
 * `Sf223Detail`, @JsonUnwrapped → clés camelCase plates sur
 * `familleExtractedData`) :
 *
 *  - `regimeSeparationBiensBeVarianteDetectee` (SEPARATION_PURE /
 *    SEPARATION_AVEC_SOCIETE_ACQUETS / SEPARATION_AVEC_PARTICIPATION_ACQUETS) →
 *    champ `varianteRegime` ;
 *  - `regimeSeparationBiensBeDateContratDetectee` (ISO YYYY-MM-DD) → champ
 *    `dateContrat` ;
 *  - `regimeSeparationBiensBePatrimoineEpoux1Detecte` (chaîne numérique) →
 *    champ `patrimoinePropreEpoux1Eur` ;
 *  - `regimeSeparationBiensBePatrimoineEpoux2Detecte` (chaîne numérique) →
 *    champ `patrimoinePropreEpoux2Eur`.
 *
 * `computePrefillCount` retourne le nombre de champs réellement pré-remplis (0
 * à 4). Le caractère notarié du contrat, la présence d'une clause de
 * participation et l'allégation d'une disproportion relèvent de l'appréciation
 * de l'avocat et ne sont pas factualisables de manière stable en V1 (laissés à
 * 0). Le static `getPrefillCount` du composant délègue à ce helper (parité
 * runtime/static — garde-fou `prefill-count-integrity`).
 */

interface RegimeBeSeparationBiensAiShape {
  regimeSeparationBiensBeVarianteDetectee?: string | null;
  regimeSeparationBiensBeDateContratDetectee?: string | null;
  regimeSeparationBiensBePatrimoineEpoux1Detecte?: string | null;
  regimeSeparationBiensBePatrimoineEpoux2Detecte?: string | null;
}

export interface RegimeBeSeparationBiensPrefillInput {
  aiData?: unknown;
  procedureChecks?: unknown[];
  aiQuestions?: unknown[];
  piecesManquantes?: unknown[];
  triggerEvents?: unknown[];
  workspaceCountry?: string;
}

const VARIANTE_VALUES = new Set([
  'SEPARATION_PURE',
  'SEPARATION_AVEC_SOCIETE_ACQUETS',
  'SEPARATION_AVEC_PARTICIPATION_ACQUETS',
]);
const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

function isPositiveNumericString(v: unknown): boolean {
  return typeof v === 'string'
    && v.trim() !== ''
    && !Number.isNaN(Number(v))
    && Number(v) >= 0;
}

/**
 * Nombre de champs pré-remplis par l'IA (0 à 4) — BELGIQUE uniquement, sinon 0.
 */
export function computePrefillCount(input: RegimeBeSeparationBiensPrefillInput): number {
  if (input.workspaceCountry && input.workspaceCountry !== 'BELGIQUE') {
    return 0;
  }
  const ai = (input.aiData ?? null) as RegimeBeSeparationBiensAiShape | null;
  if (!ai || typeof ai !== 'object') {
    return 0;
  }
  let count = 0;
  if (typeof ai.regimeSeparationBiensBeVarianteDetectee === 'string'
      && VARIANTE_VALUES.has(ai.regimeSeparationBiensBeVarianteDetectee)) {
    count += 1;
  }
  if (typeof ai.regimeSeparationBiensBeDateContratDetectee === 'string'
      && ISO_DATE.test(ai.regimeSeparationBiensBeDateContratDetectee)) {
    count += 1;
  }
  if (isPositiveNumericString(ai.regimeSeparationBiensBePatrimoineEpoux1Detecte)) {
    count += 1;
  }
  if (isPositiveNumericString(ai.regimeSeparationBiensBePatrimoineEpoux2Detecte)) {
    count += 1;
  }
  return count;
}

export const RegimeBeSeparationBiensSectionPrefillRules = {
  computePrefillCount,
};
