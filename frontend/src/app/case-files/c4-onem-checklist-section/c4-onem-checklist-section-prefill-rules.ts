import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-207-04 (frontend) — Helper partagé pour le pré-remplissage IA de
 * `C4OnemChecklistSectionComponent` (F-207, BE uniquement).
 *
 * Pattern canonique F-IA-04 (cf. SF-207-01b prescription-be) — runtime
 * `prefillFromAi()` et static `getPrefillCount()` consomment le MÊME helper
 * pur ; divergence impossible par construction.
 *
 * 9 champs pré-remplissables depuis `TravailExtractedData` (Travail BE) :
 *  1. `raisonSocialeEmployeur` ← `aiData.raisonSocialeEmployeur`
 *     (fallback `nomEmployeur`)
 *  2. `numeroBce` ← `aiData.numeroBce` (fallback `bceEmployeur`)
 *  3. `nomSalarie` ← `aiData.nomSalarie`
 *  4. `dateEntreeService` ← `aiData.dateEntree`
 *  5. `dateSortieService` ← `aiData.dateRuptureContrat` (fallback `dateLicenciement`)
 *  6. `categorieOnem` ← `aiData.categorieOnem`
 *  7. `motifExplicite` ← `aiData.motifExplicite`
 *     (fallback `motifLicenciement` ou `motifRupture` ≥ 5 caractères)
 *  8. `fauteGraveMentionnee` ← détecté depuis `aiData.motifRupture` ou
 *     `aiData.motifExplicite` (mots-clés « faute grave »).
 *  9. `preavisPresteJours` ← `aiData.preavisPresteJours`
 * 10. `dernierSalaireMensuelBrut` ← `aiData.dernierSalaireMensuelBrut`
 *     (fallback `salaireBrutMensuel`)
 *
 * Tous fallbacks sont des « dérivations IA » (provenance `IA_DERIVE`) — l'IA
 * a extrait le champ générique mais pas le champ C4-spécifique. Sont comptés
 * pareillement dans `getPrefillCount` (l'avocat voit le pré-fill, peu importe
 * la source brute).
 */

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;
const BCE_DIGITS_ONLY = /^\d{10}$/;
const FAUTE_GRAVE_TOKENS = ['FAUTE GRAVE', 'FAUTEGRAVE', 'MOTIF GRAVE'];

function normalize(s: string): string {
  return s
    .toUpperCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '');
}

function isoDateOrNull(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  const trimmed = value.trim();
  if (!ISO_DATE.test(trimmed)) return null;
  const parsed = Date.parse(trimmed);
  if (Number.isNaN(parsed)) return null;
  return trimmed;
}

function nonEmptyStringOrNull(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function bceOrNull(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  // Garde uniquement les chiffres pour matcher la validation backend (10 chiffres).
  const digits = value.replace(/\D/g, '');
  return BCE_DIGITS_ONLY.test(digits) ? digits : null;
}

function positiveIntOrNull(value: unknown): number | null {
  if (typeof value !== 'number') return null;
  if (!Number.isFinite(value)) return null;
  if (value < 0) return null;
  return Math.floor(value);
}

function positiveNumberOrNull(value: unknown): number | null {
  if (typeof value !== 'number') return null;
  if (!Number.isFinite(value)) return null;
  if (value < 0) return null;
  return value;
}

function detectFauteGrave(text: unknown): boolean {
  if (typeof text !== 'string') return false;
  const norm = normalize(text);
  return FAUTE_GRAVE_TOKENS.some(t => norm.includes(t));
}

function motifAtLeast5Chars(value: unknown): string | null {
  const s = nonEmptyStringOrNull(value);
  return s !== null && s.length >= 5 ? s : null;
}

export const C4OnemChecklistPrefillRules = {

  computeRaisonSocialeEmployeur(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return nonEmptyStringOrNull(ai.raisonSocialeEmployeur)
      ?? nonEmptyStringOrNull(ai.nomEmployeur);
  },

  computeNumeroBce(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return bceOrNull(ai.numeroBce) ?? bceOrNull(ai.bceEmployeur);
  },

  computeNomSalarie(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return nonEmptyStringOrNull(ai.nomSalarie);
  },

  computeDateEntreeService(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull(ai.dateEntree);
  },

  computeDateSortieService(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull(ai.dateRuptureContrat)
      ?? isoDateOrNull(ai.dateLicenciement);
  },

  computeCategorieOnem(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return nonEmptyStringOrNull(ai.categorieOnem);
  },

  computeMotifExplicite(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return motifAtLeast5Chars(ai.motifExplicite)
      ?? motifAtLeast5Chars(ai.motifLicenciement)
      ?? motifAtLeast5Chars(ai.motifRupture);
  },

  /**
   * Pré-remplit le booléen `fauteGraveMentionnee` quand un signal IA suggère
   * la mention « faute grave » dans le motif. Retourne :
   *  - `true` si un mot-clé « faute grave » est trouvé.
   *  - `null` si aucun signal exploitable (l'avocat tranche, défaut UI = `false`).
   */
  computeFauteGraveMentionnee(input: PrefillCountInput): boolean | null {
    const ai = input.aiData;
    if (!ai) return null;
    const fromExplicit = detectFauteGrave(ai.motifExplicite);
    const fromMotifRupture = detectFauteGrave(ai.motifRupture);
    const fromMotifLicenciement = detectFauteGrave(ai.motifLicenciement);
    if (fromExplicit || fromMotifRupture || fromMotifLicenciement) return true;
    return null;
  },

  computePreavisPresteJours(input: PrefillCountInput): number | null {
    const ai = input.aiData;
    if (!ai) return null;
    return positiveIntOrNull(ai.preavisPresteJours);
  },

  computeDernierSalaireMensuelBrut(input: PrefillCountInput): number | null {
    const ai = input.aiData;
    if (!ai) return null;
    return positiveNumberOrNull(ai.dernierSalaireMensuelBrut)
      ?? positiveNumberOrNull(ai.salaireBrutMensuel);
  },

  /**
   * Maître : compte les champs non-null pré-remplissables.
   * Maximum théorique 10. Le booléen `fauteGraveMentionnee` ne compte que
   * lorsqu'il est explicitement détecté (true) — un défaut `false` non
   * détecté n'est pas un pré-fill IA.
   */
  computePrefillCount(input: PrefillCountInput): number {
    let n = 0;
    if (this.computeRaisonSocialeEmployeur(input) !== null) n++;
    if (this.computeNumeroBce(input) !== null) n++;
    if (this.computeNomSalarie(input) !== null) n++;
    if (this.computeDateEntreeService(input) !== null) n++;
    if (this.computeDateSortieService(input) !== null) n++;
    if (this.computeCategorieOnem(input) !== null) n++;
    if (this.computeMotifExplicite(input) !== null) n++;
    if (this.computeFauteGraveMentionnee(input) !== null) n++;
    if (this.computePreavisPresteJours(input) !== null) n++;
    if (this.computeDernierSalaireMensuelBrut(input) !== null) n++;
    return n;
  },
} as const;
