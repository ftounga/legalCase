import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { VictimeTraiteBePhase } from '../../core/models/victime-traite-be.model';

/**
 * SF-221-06 — Helper partagé pour {@link VictimeTraiteBeSectionComponent}
 * (F-IM-58-victime-traite-be) — BE mono-pays.
 *
 * Champs pré-fill RÉELS (depuis {@link ImmigrationExtractedData}, miroir backend) :
 *  - phaseProcedure                 : `aiData.victimeTraitePhase` (whitelist 4 valeurs)
 *  - ruptureAvecReseau              : `aiData.victimeTraiteRupture` (boolean)
 *  - accompagnementCentreSpecialise : `aiData.victimeTraiteAccompagnement` (boolean)
 *
 * Total : 3 champs pré-remplissables. Les champs `cooperationJudiciaire` et
 * `dateDebutAccompagnement` sont ASPIRATIONNELS — non factualisés de façon fiable par
 * l'IA (saisie / contrôle avocat F-246) — et ne comptent JAMAIS dans le prefill count.
 *
 * Gate BELGIQUE : `workspaceCountry === 'BELGIQUE'` (strict). Sur workspace FR tout
 * retourne null (le composant affiche une bannière info dans ce cas).
 */

const PHASES: ReadonlySet<VictimeTraiteBePhase> = new Set<VictimeTraiteBePhase>([
  'REFLEXION_45J',
  'DECLARATION_FAITE',
  'PROCEDURE_PENALE_EN_COURS',
  'AUCUNE',
]);

function isBelgique(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'BELGIQUE';
}

function normalizePhase(v: unknown): VictimeTraiteBePhase | null {
  return typeof v === 'string' && PHASES.has(v as VictimeTraiteBePhase)
    ? (v as VictimeTraiteBePhase)
    : null;
}

function normalizeBoolean(v: unknown): boolean | null {
  return typeof v === 'boolean' ? v : null;
}

export const VictimeTraiteBePrefillRules = {
  computePhase(input: PrefillCountInput): VictimeTraiteBePhase | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return normalizePhase(ai.victimeTraitePhase);
  },

  computeRupture(input: PrefillCountInput): boolean | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return normalizeBoolean(ai.victimeTraiteRupture);
  },

  computeAccompagnement(input: PrefillCountInput): boolean | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return normalizeBoolean(ai.victimeTraiteAccompagnement);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isBelgique(input)) return 0;
    let n = 0;
    if (this.computePhase(input) !== null) n++;
    if (this.computeRupture(input) !== null) n++;
    if (this.computeAccompagnement(input) !== null) n++;
    return n;
  },
} as const;
