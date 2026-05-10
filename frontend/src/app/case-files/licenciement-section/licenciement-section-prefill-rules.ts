/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Validité du licenciement".
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir de `LicenciementSectionComponent.prefillFromAi()` :
 *   Pour chaque code de `aiData.detections`, applique la réponse IA
 *   (OUI/NON) si elle est valide. INCONNU est ignoré côté pré-fill.
 *
 * Côté static (avant instanciation), on compte le nombre maximal de
 * critères que l'IA pourrait pré-remplir — soit `OUI` ou `NON` dans
 * `detections`. Le runtime applique en plus une gate sur `criteresForm`
 * et l'état provenance UI qui ne sont pas observables côté static.
 */

export type LicenciementReponseIa = 'OUI' | 'NON' | 'INCONNU';

export interface LicenciementPrefillInput {
  aiData?: {
    detections?: { [critereCode: string]: { reponse?: LicenciementReponseIa } } | null;
  } | null;
}

/** Retourne la liste des codes critères que l'IA pré-remplirait (OUI/NON). */
export function computePrefilledCodes(input: LicenciementPrefillInput): string[] {
  const det = input.aiData?.detections;
  if (!det || typeof det !== 'object') return [];
  const out: string[] = [];
  for (const code of Object.keys(det)) {
    const r = det[code]?.reponse;
    if (r === 'OUI' || r === 'NON') out.push(code);
  }
  return out;
}

export function computePrefillCount(input: LicenciementPrefillInput): number {
  return computePrefilledCodes(input).length;
}

export const LicenciementSectionPrefillRules = {
  computePrefilledCodes,
  computePrefillCount,
};
