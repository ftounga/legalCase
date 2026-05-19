import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { TypeCreance } from '../../core/models/prescription-be-litige-travail.model';

/**
 * SF-207-01b — Helper partagé pour le pré-remplissage IA de
 * `PrescriptionBeLitigeTravailSectionComponent` (F-207, BE uniquement).
 *
 * Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime `prefillFromAi()`
 * et static `getPrefillCount()` consomment le MÊME helper pur ; divergence
 * impossible par construction.
 *
 * 2 champs pré-remplis depuis `TravailExtractedData` (Travail BE) :
 *   1. `dateRupture` (string ISO YYYY-MM-DD) — depuis `aiData.dateRuptureContrat`.
 *      Validation `isoDateOrNull` : seules les chaînes ISO YYYY-MM-DD valides
 *      sont propagées.
 *   2. `typeCreance` (enum) — mapping `aiData.motifRupture` (texte libre côté
 *      backend) vers l'enum :
 *        - `LICENCIEMENT` / `DEMISSION` / `FAUTE_GRAVE` / `RCC` /
 *          `RUPTURE_AMIABLE` → `EX_CONTRAT_GENERAL`
 *        - autre / `null` / vide → `null` (pas de pré-fill : l'avocat tranche)
 *      La détection est tolérante à la casse, aux accents et à l'ordre des
 *      mots dans la chaîne libre extraite par le LLM (cf.
 *      `LegalDomainPromptBuilder.java` lignes 224-225).
 */

const MOTIF_TOKENS: ReadonlyArray<{ tokens: readonly string[]; mapsTo: TypeCreance }> = [
  // Ordre signifiant : `FAUTE_GRAVE` testé avant `LICENCIEMENT` car un
  // licenciement pour faute grave contient les 2 motifs (on retient la
  // qualification la plus spécifique côté UX, même si V1 = même TypeCreance).
  { tokens: ['FAUTE GRAVE', 'FAUTEGRAVE', 'MOTIF GRAVE'], mapsTo: 'EX_CONTRAT_GENERAL' },
  { tokens: ['LICENCIEMENT'], mapsTo: 'EX_CONTRAT_GENERAL' },
  { tokens: ['DEMISSION'], mapsTo: 'EX_CONTRAT_GENERAL' },
  { tokens: ['RCC'], mapsTo: 'EX_CONTRAT_GENERAL' },
  { tokens: ['RUPTURE AMIABLE', 'RUPTURE_AMIABLE'], mapsTo: 'EX_CONTRAT_GENERAL' },
];

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

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

export const PrescriptionBeLitigeTravailPrefillRules = {
  /** Date de rupture pré-remplie depuis `aiData.dateRuptureContrat`. */
  computeDateRupture(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull(ai.dateRuptureContrat);
  },

  /**
   * Mapping `aiData.motifRupture` (texte libre) → enum {@link TypeCreance}.
   * Retourne `null` si motif vide ou hors mapping (V1 — l'avocat tranche
   * manuellement les cas `PENDANT_CONTRAT` / `ARRIERES_SALAIRE` / `CCT_109`).
   */
  computeTypeCreance(input: PrefillCountInput): TypeCreance | null {
    const ai = input.aiData;
    if (!ai) return null;
    const motif = ai.motifRupture;
    if (typeof motif !== 'string' || motif.trim().length === 0) return null;
    const normalized = normalize(motif);
    for (const { tokens, mapsTo } of MOTIF_TOKENS) {
      if (tokens.some(t => normalized.includes(t))) return mapsTo;
    }
    return null;
  },

  /** Maître : compte les champs non-null pré-remplissables. */
  computePrefillCount(input: PrefillCountInput): number {
    let n = 0;
    if (this.computeDateRupture(input) !== null) n++;
    if (this.computeTypeCreance(input) !== null) n++;
    return n;
  },
} as const;
