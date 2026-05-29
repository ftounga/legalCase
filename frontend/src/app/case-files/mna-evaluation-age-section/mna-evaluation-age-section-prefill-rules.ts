import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-214-28 — Helper partagé pour {@link MnaEvaluationAgeSectionComponent}
 * (F-IM-38-mna-evaluation-age-fr) — FRANCE UNIQUEMENT.
 *
 * Champ pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - dateNaissanceDeclaree : `aiData.mineursDateNaissance` (ISO YYYY-MM-DD).
 *
 * Champs NON pré-remplis :
 *  - evaluationASERefusee / dateRefusASE / examenOsseuxOrdonne /
 *    resultatExamenOsseux : pas de champ IA factualisable dédié, saisis par
 *    l'avocat à partir de la situation administrative réelle.
 *
 * Total : 1 champ pre-remplissable (sur 5 saisissables).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const MnaEvaluationAgePrefillRules = {

  computeDateNaissanceDeclaree(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.mineursDateNaissance;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim();
    if (!ISO_DATE_RE.test(trimmed)) return null;
    return trimmed;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateNaissanceDeclaree(input) !== null) n++;
    return n;
  },
} as const;
