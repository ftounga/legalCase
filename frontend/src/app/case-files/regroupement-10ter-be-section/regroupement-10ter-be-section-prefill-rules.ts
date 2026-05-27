import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  Regroupement10terLienFamilial,
  Regroupement10terTypeCarte,
} from '../../core/models/regroupement-10ter-be.model';

/**
 * SF-215-04 — Helper partagé pour {@link Regroupement10terBeSectionComponent}
 * (F-IM-26-regroupement-10ter-be) — BE mono-pays.
 *
 * Champs pré-fill (depuis {@link ImmigrationExtractedData}, miroir backend) :
 *  - lienFamilial                 : `aiData.be10terLienFamilial`
 *      (whitelist CONJOINT / PARTENAIRE_ENREGISTRE / ENFANT_MOINS_21 /
 *       ENFANT_21_PLUS_CHARGE / ASCENDANT_CHARGE)
 *  - typeCarteRegroupant          : `aiData.be10terTypeCarte`
 *      (whitelist CARTE_B / CARTE_C)
 *  - revenusMensuelsNetsRegroupant: `aiData.be10terRevenusMensuels`
 *      (entier borné 0–100 000)
 *  - dureeSejour                  : `aiData.be10terDureeSejour`
 *      (entier mois borné 0–600)
 *
 * Total : 4 champs pré-remplissables (les 3 checkboxes restantes
 * logementConforme/assuranceMaladie/menaceOrdrePublic ne sont pas
 * extraites par l'IA — saisie avocat).
 *
 * Gate BELGIQUE : `workspaceCountry === 'BELGIQUE'` (strict). Sur workspace FR
 * tout retourne null/0 (le composant affiche une bannière info dans ce cas).
 */

const MAX_REVENUS = 100_000;
const MAX_DUREE_SEJOUR_MOIS = 600;

export const REGROUPEMENT_10TER_LIEN_SET: ReadonlySet<Regroupement10terLienFamilial> =
  new Set<Regroupement10terLienFamilial>([
    'CONJOINT',
    'PARTENAIRE_ENREGISTRE',
    'ENFANT_MOINS_21',
    'ENFANT_21_PLUS_CHARGE',
    'ASCENDANT_CHARGE',
  ]);

export const REGROUPEMENT_10TER_TYPE_CARTE_SET: ReadonlySet<Regroupement10terTypeCarte> =
  new Set<Regroupement10terTypeCarte>(['CARTE_B', 'CARTE_C']);

function isBelgique(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'BELGIQUE';
}

function boundedInt(v: unknown, min: number, max: number): number | null {
  if (typeof v !== 'number' || isNaN(v) || !Number.isFinite(v)) return null;
  if (!Number.isInteger(v)) return null;
  if (v < min || v > max) return null;
  return v;
}

export const Regroupement10terBePrefillRules = {
  REGROUPEMENT_10TER_LIEN_SET,
  REGROUPEMENT_10TER_TYPE_CARTE_SET,

  computeLienFamilial(input: PrefillCountInput): Regroupement10terLienFamilial | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.be10terLienFamilial;
    if (typeof v !== 'string') return null;
    const upper = v.trim().toUpperCase();
    if (!REGROUPEMENT_10TER_LIEN_SET.has(upper as Regroupement10terLienFamilial)) return null;
    return upper as Regroupement10terLienFamilial;
  },

  computeTypeCarteRegroupant(input: PrefillCountInput): Regroupement10terTypeCarte | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.be10terTypeCarte;
    if (typeof v !== 'string') return null;
    const upper = v.trim().toUpperCase();
    if (!REGROUPEMENT_10TER_TYPE_CARTE_SET.has(upper as Regroupement10terTypeCarte)) return null;
    return upper as Regroupement10terTypeCarte;
  },

  computeRevenusMensuelsNetsRegroupant(input: PrefillCountInput): number | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return boundedInt(ai.be10terRevenusMensuels, 0, MAX_REVENUS);
  },

  computeDureeSejour(input: PrefillCountInput): number | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return boundedInt(ai.be10terDureeSejour, 0, MAX_DUREE_SEJOUR_MOIS);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isBelgique(input)) return 0;
    let n = 0;
    if (this.computeLienFamilial(input) !== null) n++;
    if (this.computeTypeCarteRegroupant(input) !== null) n++;
    if (this.computeRevenusMensuelsNetsRegroupant(input) !== null) n++;
    if (this.computeDureeSejour(input) !== null) n++;
    return n;
  },
} as const;
