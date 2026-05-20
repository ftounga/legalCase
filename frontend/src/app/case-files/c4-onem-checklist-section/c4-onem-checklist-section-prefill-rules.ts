import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-207-02b — Helper partagé pour le pré-remplissage IA de
 * `C4OnemChecklistSectionComponent` (F-207, BE uniquement).
 *
 * Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime `prefillFromAi()`
 * et static `getPrefillCount()` consomment le MÊME helper pur ; divergence
 * impossible par construction.
 *
 * 10 champs pré-remplissables depuis `TravailExtractedData` (Travail BE) :
 *   1. `raisonSocialeEmployeur` ← `aiData.raisonSocialeEmployeur`
 *   2. `numeroBce` ← `aiData.numeroBce` (10 chiffres si fourni)
 *   3. `nomSalarie` ← `aiData.nomSalarie` (+ prénom si présent)
 *   4. `numeroNationalRegistre` (pas exposé côté `TravailExtractedData` V1 — null)
 *   5. `dateEntreeService` ← `aiData.dateEntree` (champ FR/BE existant)
 *   6. `dateSortieService` ← `aiData.dateRuptureContrat` (livré SF-207-01)
 *   7. `categorieOnem` ← `aiData.categorieOnem`
 *   8. `motifExplicite` ← `aiData.motifExplicite` ou fallback `aiData.motifRupture`
 *   9. `fauteGraveMentionnee` ← dérivé : `aiData.motifRupture` contient
 *      « faute grave » (substring après normalisation Unicode NFD + accents)
 *   10. `preavisPresteJours` ← `aiData.preavisPresteJours`
 *   11. `dernierSalaireMensuelBrut` ← `aiData.dernierSalaireMensuelBrut`
 *
 * Le champ `numeroNationalRegistre` n'est volontairement pas instrumenté
 * (V1 — pas de source IA fiable BE, le NRN est souvent masqué côté
 * documents pour RGPD).
 */

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;
const BCE_REGEX = /^\d{10}$/;

function normalize(s: string): string {
  return s.toUpperCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
}

function isoDateOrNull(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  const trimmed = value.trim();
  if (!ISO_DATE.test(trimmed)) return null;
  const parsed = Date.parse(trimmed);
  if (Number.isNaN(parsed)) return null;
  return trimmed;
}

function trimmedStringOrNull(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}

function positiveNumberOrNull(value: unknown): number | null {
  if (typeof value !== 'number' || Number.isNaN(value)) return null;
  if (value < 0) return null;
  return value;
}

export const C4OnemChecklistPrefillRules = {

  computeRaisonSociale(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return trimmedStringOrNull(ai.raisonSocialeEmployeur);
  },

  /** BCE : 10 chiffres exacts. Toute autre valeur → null (le calculateur 400). */
  computeNumeroBce(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    const raw = trimmedStringOrNull(ai.numeroBce);
    if (!raw) return null;
    // tolère les espaces / points dans la valeur extraite par le LLM.
    const cleaned = raw.replace(/[\s.]/g, '');
    if (!BCE_REGEX.test(cleaned)) return null;
    return cleaned;
  },

  /**
   * Nom complet du salarié — concatène prénom + nom si les deux sont présents,
   * sinon retombe sur celui qui l'est. Trim ; null si vide.
   */
  computeNomSalarie(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    const nom = trimmedStringOrNull(ai.nomSalarie);
    const prenom = trimmedStringOrNull(ai.prenomSalarie);
    if (prenom && nom) return `${prenom} ${nom}`;
    return nom ?? prenom ?? null;
  },

  computeDateEntreeService(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull(ai.dateEntree);
  },

  computeDateSortieService(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull(ai.dateRuptureContrat);
  },

  computeCategorieOnem(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return trimmedStringOrNull(ai.categorieOnem);
  },

  /** Motif explicite : préfère `motifExplicite` puis fallback `motifRupture`. */
  computeMotifExplicite(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return trimmedStringOrNull(ai.motifExplicite) ?? trimmedStringOrNull(ai.motifRupture);
  },

  /**
   * `fauteGraveMentionnee` est dérivé de `motifRupture` :
   * - normalisation NFD + suppression accents + upper-case ;
   * - substring `FAUTE GRAVE` ou `MOTIF GRAVE` (synonymie BE) → true.
   *
   * Retourne :
   *   - `true` si le motif contient « faute grave » → pré-fill + provenance ;
   *   - `null` sinon (faute grave non détectée — l'avocat tranche, et la
   *     case reste à `false` côté form par défaut sans badge IA).
   */
  computeFauteGraveMentionnee(input: PrefillCountInput): true | null {
    const ai = input.aiData;
    if (!ai) return null;
    const motif = trimmedStringOrNull(ai.motifRupture);
    if (!motif) return null;
    const normalized = normalize(motif);
    if (normalized.includes('FAUTE GRAVE') || normalized.includes('MOTIF GRAVE')) {
      return true;
    }
    return null;
  },

  computePreavisPresteJours(input: PrefillCountInput): number | null {
    const ai = input.aiData;
    if (!ai) return null;
    return positiveNumberOrNull(ai.preavisPresteJours);
  },

  computeDernierSalaire(input: PrefillCountInput): number | null {
    const ai = input.aiData;
    if (!ai) return null;
    return positiveNumberOrNull(ai.dernierSalaireMensuelBrut);
  },

  /**
   * Maître : compte les champs non-null pré-remplissables. Parité stricte
   * avec `prefillFromAi()` runtime (test de parité côté spec).
   * Max théorique = 10 (NRN non instrumenté).
   */
  computePrefillCount(input: PrefillCountInput): number {
    let n = 0;
    if (this.computeRaisonSociale(input) !== null) n++;
    if (this.computeNumeroBce(input) !== null) n++;
    if (this.computeNomSalarie(input) !== null) n++;
    if (this.computeDateEntreeService(input) !== null) n++;
    if (this.computeDateSortieService(input) !== null) n++;
    if (this.computeCategorieOnem(input) !== null) n++;
    if (this.computeMotifExplicite(input) !== null) n++;
    if (this.computeFauteGraveMentionnee(input) !== null) n++;
    if (this.computePreavisPresteJours(input) !== null) n++;
    if (this.computeDernierSalaire(input) !== null) n++;
    return n;
  },
} as const;
