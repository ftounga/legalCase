import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * F-236 SF-236-02 — Helper partagé pour `NaturalisationSectionComponent`
 * (F-IM-13-naturalisation) — FR mono-pays.
 *
 * Le runtime `prefillFromAi()` est aujourd'hui un no-op (aucun champ direct
 * mappable depuis `ImmigrationExtractedData`). Le compteur retourne donc 0
 * jusqu'à enrichissement du prompt IA.
 */

export const NaturalisationPrefillRules = {
  // Crochet pour évolution future (durée résidence, âge demandeur, durée mariage).
  computePrefillCount(_input: PrefillCountInput): number {
    return 0;
  },
} as const;
