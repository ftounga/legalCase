/**
 * SF-223-03 — Helper partagé pour l'outil "Recueil légal (kafala) — Belgique"
 * (`kafala-be-recueil-legal`).
 *
 * Pré-fill F-246 (BELGIQUE UNIQUEMENT). Deux champs sont factualisables depuis
 * le sous-objet IA `kafala_be_detection` (consolidé dans `Sf223Detail`,
 * @JsonUnwrapped → clés camelCase plates sur `familleExtractedData`) :
 *
 *  - `kafalaPaysOrigineDetecte` (ISO 3166-1 alpha-2) → champ `paysOrigineKafala` ;
 *  - `kafalaDateActeDetectee` (ISO YYYY-MM-DD) → champ `dateActeKafala`.
 *
 * `computePrefillCount` retourne le nombre de champs réellement pré-remplis (0
 * à 2). L'objectif envisagé, le caractère officiel de l'acte et les
 * consentements relèvent de l'appréciation de l'avocat et ne sont pas
 * factualisables de manière stable en V1 (laissés à 0). Le static
 * `getPrefillCount` du composant délègue à ce helper (parité runtime/static —
 * garde-fou `prefill-count-integrity`).
 */

interface KafalaBeAiShape {
  kafalaPaysOrigineDetecte?: string | null;
  kafalaDateActeDetectee?: string | null;
}

export interface KafalaBePrefillInput {
  aiData?: unknown;
  procedureChecks?: unknown[];
  aiQuestions?: unknown[];
  piecesManquantes?: unknown[];
  triggerEvents?: unknown[];
  workspaceCountry?: string;
}

const ISO2 = /^[A-Z]{2}$/;
const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Nombre de champs pré-remplis par l'IA (0 à 2) — BELGIQUE uniquement, sinon 0.
 */
export function computePrefillCount(input: KafalaBePrefillInput): number {
  if (input.workspaceCountry && input.workspaceCountry !== 'BELGIQUE') {
    return 0;
  }
  const ai = (input.aiData ?? null) as KafalaBeAiShape | null;
  if (!ai || typeof ai !== 'object') {
    return 0;
  }
  let count = 0;
  if (typeof ai.kafalaPaysOrigineDetecte === 'string'
      && ISO2.test(ai.kafalaPaysOrigineDetecte)) {
    count += 1;
  }
  if (typeof ai.kafalaDateActeDetectee === 'string'
      && ISO_DATE.test(ai.kafalaDateActeDetectee)) {
    count += 1;
  }
  return count;
}

export const KafalaBeRecueilLegalSectionPrefillRules = {
  computePrefillCount,
};
