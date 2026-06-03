import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-221-01 — Helper partagé pour {@link CarteAProrogationBeSectionComponent}
 * (F-IM-53-carte-a-prorogation-be) — BE mono-pays.
 *
 * Champs pré-fill RÉELS (depuis {@link ImmigrationExtractedData}, miroir backend) :
 *  - dateExpirationCarteA               : `aiData.carteAProrogationDateExpiration` (ISO yyyy-MM-dd)
 *  - motifSejourPersiste                : `aiData.carteAProrogationMotifPersiste` (booléen)
 *  - conditionsInitialesToujoursReunies : `aiData.carteAProrogationConditionsReunies` (booléen)
 *
 * Total : 3 champs pré-remplissables. Les 2 champs `demandeDeposee` (checkbox) et
 * `dateDemande` (date conditionnelle) sont ASPIRATIONNELS — actions procédurales
 * non extraites par l'IA, saisie avocat — et ne comptent JAMAIS dans le prefill count.
 *
 * Gate BELGIQUE : `workspaceCountry === 'BELGIQUE'` (strict). Sur workspace FR
 * tout retourne null (le composant affiche une bannière info dans ce cas).
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

function normalizeBoolean(v: unknown): boolean | null {
  if (typeof v === 'boolean') return v;
  return null;
}

export const CarteAProrogationBePrefillRules = {
  computeDateExpiration(input: PrefillCountInput): string | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return normalizeIsoDate(ai.carteAProrogationDateExpiration);
  },

  computeMotifPersiste(input: PrefillCountInput): boolean | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return normalizeBoolean(ai.carteAProrogationMotifPersiste);
  },

  computeConditionsReunies(input: PrefillCountInput): boolean | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return normalizeBoolean(ai.carteAProrogationConditionsReunies);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isBelgique(input)) return 0;
    let n = 0;
    if (this.computeDateExpiration(input) !== null) n++;
    if (this.computeMotifPersiste(input) !== null) n++;
    if (this.computeConditionsReunies(input) !== null) n++;
    return n;
  },
} as const;
