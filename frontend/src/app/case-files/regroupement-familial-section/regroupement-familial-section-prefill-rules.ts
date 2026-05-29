import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { TypeRegroupement } from '../../core/models/regroupement-familial.model';

/**
 * SF-214-04 — Helper partagé pour {@link RegroupementFamilialSectionComponent}
 * (F-IM-26-regroupement-familial-fr) — FR mono-pays.
 *
 * Champs pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - dureeSejourRegulierMois : `aiData.aesDureePresenceMois` (entier ≥ 0).
 *  - ressourcesMensuellesNettes : `aiData.regroupementRessourcesMensuelles` (≥ 0 fini).
 *  - typeRegroupement : `aiData.regroupementType` (whitelist CONJOINT/ENFANT_MINEUR/AUTRE).
 *
 * Champs NON pré-remplis :
 *  - tailleLogementM2 / nombrePersonnesFoyer / membresFamilleARegrouper :
 *    pas de champ IA dédié, saisis par l'avocat.
 *
 * Total : 3 champs pre-remplissables (sur 6 saisissables).
 */

export const TYPE_REGROUPEMENT_SET: ReadonlySet<TypeRegroupement> =
  new Set<TypeRegroupement>(['CONJOINT', 'ENFANT_MINEUR', 'AUTRE']);

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const RegroupementFamilialPrefillRules = {
  TYPE_REGROUPEMENT_SET,

  computeDureeSejourRegulierMois(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.aesDureePresenceMois;
    if (typeof v !== 'number' || !Number.isFinite(v) || v < 0) return null;
    return Math.trunc(v);
  },

  computeRessourcesMensuellesNettes(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.regroupementRessourcesMensuelles;
    if (typeof v !== 'number' || !Number.isFinite(v) || v < 0) return null;
    return v;
  },

  computeTypeRegroupement(input: PrefillCountInput): TypeRegroupement | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.regroupementType;
    if (typeof v !== 'string') return null;
    const upper = v.trim().toUpperCase();
    if (!TYPE_REGROUPEMENT_SET.has(upper as TypeRegroupement)) return null;
    return upper as TypeRegroupement;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDureeSejourRegulierMois(input) !== null) n++;
    if (this.computeRessourcesMensuellesNettes(input) !== null) n++;
    if (this.computeTypeRegroupement(input) !== null) n++;
    return n;
  },
} as const;
