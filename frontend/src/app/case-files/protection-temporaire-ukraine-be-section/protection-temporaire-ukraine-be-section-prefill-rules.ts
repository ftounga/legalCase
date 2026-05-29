import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-215-20 — Helper partagé pour {@link ProtectionTemporaireUkraineBeSectionComponent}
 * (F-IM-34-protection-temporaire-ukraine-be) — BE mono-pays.
 *
 * Régime de protection temporaire Ukraine (décision d'exécution (UE) 2022/382,
 * directive 2001/55/CE). BELGIQUE uniquement.
 *
 * Champs pré-fill RÉELS (depuis {@link ImmigrationExtractedData}, miroir backend) :
 *  - dateArrivee            : `aiData.ptUkraineDateArrivee` (ISO yyyy-MM-dd)
 *  - nationaliteUkrainienne : `aiData.ptUkraineNationalite` (booléen ; ne compte
 *                             QUE si === true — un false n'est pas un pré-remplissage)
 *
 * Total : 2 champs pré-remplissables. Les champs `residenceUkraineAvant24Fev2022`,
 * `apatridesUkraine`, `membreFamilleProtege` (checkboxes) et `titreSejourBE`
 * (dropdown) sont ASPIRATIONNELS — non extraits par l'IA, saisie avocat — et ne
 * comptent JAMAIS dans le prefill count.
 *
 * Gate BELGIQUE : `workspaceCountry === 'BELGIQUE'` (strict). Sur workspace FR
 * tout retourne null/0 (le composant affiche une bannière info dans ce cas).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function isBelgique(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'BELGIQUE';
}

function normalizeIsoDate(v: unknown): string | null {
  if (typeof v !== 'string') return null;
  const trimmed = v.trim();
  if (!ISO_DATE_RE.test(trimmed)) return null;
  // Validité calendaire stricte (ex. rejette 2026-02-30).
  const d = new Date(`${trimmed}T00:00:00Z`);
  if (isNaN(d.getTime())) return null;
  const iso = d.toISOString().slice(0, 10);
  return iso === trimmed ? trimmed : null;
}

export const ProtectionTemporaireUkraineBePrefillRules = {
  computeDateArrivee(input: PrefillCountInput): string | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return normalizeIsoDate(ai.ptUkraineDateArrivee);
  },

  /**
   * Renvoie `true` uniquement si l'IA a détecté la nationalité ukrainienne
   * (booléen strict === true). Un `false`/null/undefined renvoie null (pas de
   * pré-remplissage — l'avocat saisit lui-même).
   */
  computeNationaliteUkrainienne(input: PrefillCountInput): boolean | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return ai.ptUkraineNationalite === true ? true : null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isBelgique(input)) return 0;
    let n = 0;
    if (this.computeDateArrivee(input) !== null) n++;
    if (this.computeNationaliteUkrainienne(input) !== null) n++;
    return n;
  },
} as const;
