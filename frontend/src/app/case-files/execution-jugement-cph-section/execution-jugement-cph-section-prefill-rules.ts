import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { ExecutionJugementCphSituationEmployeur } from '../../core/models/execution-jugement-cph.model';

/**
 * SF-218-04 — Helper partagé pour {@link ExecutionJugementCphSectionComponent}
 * (F-DT-88-execution-jugement-cph) — Travail FR mono-pays.
 *
 * Champs pre-fill (depuis {@link TravailExtractedData}) :
 *  - montantCondamnation : `aiData.montantCondamnationCph` (nombre > 0). Montant total
 *    des condamnations prononcées par le jugement CPH.
 *  - situationEmployeur : `aiData.situationEmployeurDetectee`
 *    (IN_BONIS | REDRESSEMENT | LIQUIDATION). Proxy de l'état de l'employeur détecté
 *    par l'IA (redressement / liquidation).
 *
 * Champs NON pré-remplis :
 *  - dateJugement / executionProvisoireOrdonnee / dateOuvertureProcedureCollective /
 *    creancesSuperPrivilegiees : pas de champ IA dédié, saisis par l'avocat.
 *  - `executionJugementCphEnvisagee` est un FLAG de visibilité (déclenche
 *    l'apparition de l'outil via DecisionToolVisibilityService) — ce n'est PAS un
 *    champ du formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 2 champs pre-remplissables (sur 6 saisissables).
 */

const VALID_SITUATIONS: ReadonlyArray<ExecutionJugementCphSituationEmployeur> = [
  'IN_BONIS',
  'REDRESSEMENT',
  'LIQUIDATION',
];

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const ExecutionJugementCphPrefillRules = {

  computeMontantCondamnation(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.montantCondamnationCph;
    if (typeof v !== 'number' || !Number.isFinite(v) || v <= 0) return null;
    return v;
  },

  computeSituationEmployeur(input: PrefillCountInput): ExecutionJugementCphSituationEmployeur | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.situationEmployeurDetectee;
    if (typeof v !== 'string') return null;
    return VALID_SITUATIONS.includes(v as ExecutionJugementCphSituationEmployeur)
      ? (v as ExecutionJugementCphSituationEmployeur)
      : null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeMontantCondamnation(input) !== null) n++;
    if (this.computeSituationEmployeur(input) !== null) n++;
    return n;
  },
} as const;
