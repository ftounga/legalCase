import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { MotifUrgence } from '../../core/models/refere-tribunal-travail-be.model';

/**
 * SF-207-05b — Helper partagé pour le pré-remplissage IA de
 * `RefereTribunalTravailBeSectionComponent` (F-207, BE uniquement).
 *
 * Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime `prefillFromAi()`
 * et static `getPrefillCount()` consomment le MÊME helper pur ; divergence
 * impossible par construction.
 *
 * 3 champs pré-remplissables depuis `TravailExtractedData` (Travail BE) :
 *   1. `motifUrgence` ← `aiData.motifUrgenceDetecte` (whitelist 4 codes ;
 *      toute valeur hors whitelist → null pour éviter d'imposer un motif
 *      douteux à l'avocat).
 *   2. `dateFaitGenerateur` ← `aiData.dateFaitGenerateurUrgence` (ISO strict).
 *   3. `perilEnDemeure` ← `aiData.perilImmediatPresume` (true seulement —
 *      `false` ne déclenche pas de pré-fill : par défaut la case reste
 *      décochée et c'est l'avocat qui qualifie le péril).
 *
 * Les autres champs (`motifUrgenceDescription`, `dateDemarcheAmiable`,
 * `preuveUrgenceJointe`, `mesureProvisoireDemandee`,
 * `competenceTerritorialeIdentifiee`) ne sont volontairement pas
 * instrumentés : ils relèvent de la rédaction / qualification juridique
 * que l'avocat doit assumer (l'IA n'a pas l'autorité pour décider seule
 * que la compétence est identifiée ou que la mesure provisoire est précise).
 */

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

const WHITELIST_MOTIF: ReadonlyArray<MotifUrgence> = [
  'HARCELEMENT',
  'SALAIRE_IMPAYE',
  'MODIFICATION_UNILATERALE',
  'AUTRE',
];

function isoDateOrNull(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  const trimmed = value.trim();
  if (!ISO_DATE.test(trimmed)) return null;
  const parsed = Date.parse(trimmed);
  if (Number.isNaN(parsed)) return null;
  return trimmed;
}

export const RefereTribunalTravailBePrefillRules = {

  /**
   * Motif d'urgence : retourne le code IA si présent dans la whitelist
   * (case-sensitive après normalisation upper-case), sinon null.
   */
  computeMotifUrgence(input: PrefillCountInput): MotifUrgence | null {
    const ai = input.aiData;
    if (!ai) return null;
    const raw = (ai as { motifUrgenceDetecte?: unknown }).motifUrgenceDetecte;
    if (typeof raw !== 'string') return null;
    const trimmed = raw.trim().toUpperCase();
    if (trimmed.length === 0) return null;
    return (WHITELIST_MOTIF as ReadonlyArray<string>).includes(trimmed)
      ? (trimmed as MotifUrgence)
      : null;
  },

  /**
   * Date du fait générateur : ISO strict YYYY-MM-DD ; toute autre forme → null
   * (parité backend `LocalDate.parse`).
   */
  computeDateFaitGenerateur(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull((ai as { dateFaitGenerateurUrgence?: unknown }).dateFaitGenerateurUrgence);
  },

  /**
   * Péril en demeure : pré-fill UNIQUEMENT si l'IA a positivement détecté
   * un péril immédiat (true). `false` / `null` / `undefined` → null (la case
   * reste à `false` côté form par défaut, sans badge IA).
   */
  computePerilEnDemeure(input: PrefillCountInput): true | null {
    const ai = input.aiData;
    if (!ai) return null;
    const raw = (ai as { perilImmediatPresume?: unknown }).perilImmediatPresume;
    return raw === true ? true : null;
  },

  /**
   * Maître : compte les champs non-null pré-remplissables. Parité stricte
   * avec `prefillFromAi()` runtime (test de parité côté spec).
   * Max théorique = 3.
   */
  computePrefillCount(input: PrefillCountInput): number {
    let n = 0;
    if (this.computeMotifUrgence(input) !== null) n++;
    if (this.computeDateFaitGenerateur(input) !== null) n++;
    if (this.computePerilEnDemeure(input) !== null) n++;
    return n;
  },
} as const;
