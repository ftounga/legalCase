import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  Regroupement10bisLienFamilial,
} from '../../core/models/regroupement-10bis-be.model';

/**
 * SF-215-06 — Helper partagé pour {@link Regroupement10bisBeSectionComponent}
 * (F-IM-27-regroupement-10bis-be) — BE mono-pays.
 *
 * Champs pré-fill (depuis {@link ImmigrationExtractedData}, miroir backend) :
 *  - lienFamilial                 : `aiData.be10bisLienFamilial`
 *      (whitelist CONJOINT / PARTENAIRE_ENREGISTRE / ENFANT_MOINS_21 /
 *       ENFANT_21_PLUS_CHARGE / ASCENDANT_CHARGE)
 *  - revenusMensuelsNetsRegroupant: `aiData.be10bisRevenusMensuels`
 *      (entier borné 0–100 000)
 *  - dureeSejour                  : `aiData.be10bisDureeSejour`
 *      (entier mois borné 0–600)
 *  - dateFinCarteA                : `aiData.be10bisDateFinCarteA`
 *      (ISO date YYYY-MM-DD, validation lexicale stricte)
 *
 * Total : 4 champs pré-remplissables (les 3 checkboxes restantes
 * logementConforme/assuranceMaladie/menaceOrdrePublic ne sont pas
 * extraites par l'IA — saisie avocat ; typeCarteRegroupant non pré-rempli
 * car forcé à CARTE_A par construction de l'art. 10bis).
 *
 * Gate BELGIQUE : `workspaceCountry === 'BELGIQUE'` (strict). Sur workspace FR
 * tout retourne null/0 (le composant affiche une bannière info dans ce cas).
 */

const MAX_REVENUS = 100_000;
const MAX_DUREE_SEJOUR_MOIS = 600;

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export const REGROUPEMENT_10BIS_LIEN_SET: ReadonlySet<Regroupement10bisLienFamilial> =
  new Set<Regroupement10bisLienFamilial>([
    'CONJOINT',
    'PARTENAIRE_ENREGISTRE',
    'ENFANT_MOINS_21',
    'ENFANT_21_PLUS_CHARGE',
    'ASCENDANT_CHARGE',
  ]);

function isBelgique(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'BELGIQUE';
}

function boundedInt(v: unknown, min: number, max: number): number | null {
  if (typeof v !== 'number' || isNaN(v) || !Number.isFinite(v)) return null;
  if (!Number.isInteger(v)) return null;
  if (v < min || v > max) return null;
  return v;
}

function isoDate(v: unknown): string | null {
  if (typeof v !== 'string') return null;
  const s = v.trim();
  if (!ISO_DATE_RE.test(s)) return null;
  // Validate that it parses to a real calendar date.
  const ts = Date.parse(s);
  if (isNaN(ts)) return null;
  return s;
}

export const Regroupement10bisBePrefillRules = {
  REGROUPEMENT_10BIS_LIEN_SET,

  computeLienFamilial(input: PrefillCountInput): Regroupement10bisLienFamilial | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.be10bisLienFamilial;
    if (typeof v !== 'string') return null;
    const upper = v.trim().toUpperCase();
    if (!REGROUPEMENT_10BIS_LIEN_SET.has(upper as Regroupement10bisLienFamilial)) return null;
    return upper as Regroupement10bisLienFamilial;
  },

  computeRevenusMensuelsNetsRegroupant(input: PrefillCountInput): number | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return boundedInt(ai.be10bisRevenusMensuels, 0, MAX_REVENUS);
  },

  computeDureeSejour(input: PrefillCountInput): number | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return boundedInt(ai.be10bisDureeSejour, 0, MAX_DUREE_SEJOUR_MOIS);
  },

  computeDateFinCarteA(input: PrefillCountInput): string | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return isoDate(ai.be10bisDateFinCarteA);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isBelgique(input)) return 0;
    let n = 0;
    if (this.computeLienFamilial(input) !== null) n++;
    if (this.computeRevenusMensuelsNetsRegroupant(input) !== null) n++;
    if (this.computeDureeSejour(input) !== null) n++;
    if (this.computeDateFinCarteA(input) !== null) n++;
    return n;
  },
} as const;
