/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Validité rupture conventionnelle".
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir : pré-remplit les réponses OUI/NON/INCONNU pour les codes
 * du référentiel RC_CODES. INCONNU est considéré comme "ne pré-remplit pas"
 * pour le décompte static (cohérent avec l'effet visuel : pas de badge).
 */

export const RC_CODES: ReadonlySet<string> = new Set<string>([
  'RC_CONSENTEMENT', 'RC_DELAI_RETRACTATION', 'RC_HOMOLOGATION',
  'RC_ASSISTANCE', 'RC_INDEMNITE', 'RC_ENTRETIENS',
]);

export type RuptureConvReponseIa = 'OUI' | 'NON' | 'INCONNU';

export interface RuptureConvPrefillInput {
  aiData?: {
    detections?: { [critereCode: string]: { reponse?: RuptureConvReponseIa } } | null;
  } | null;
}

export function computePrefilledCodes(input: RuptureConvPrefillInput): string[] {
  const det = input.aiData?.detections;
  if (!det || typeof det !== 'object') return [];
  const out: string[] = [];
  for (const code of Object.keys(det)) {
    if (!RC_CODES.has(code)) continue;
    const r = det[code]?.reponse;
    if (r === 'OUI' || r === 'NON') out.push(code);
  }
  return out;
}

export function computePrefillCount(input: RuptureConvPrefillInput): number {
  return computePrefilledCodes(input).length;
}

export const RuptureConvSectionPrefillRules = {
  computePrefilledCodes,
  computePrefillCount,
  RC_CODES,
};
