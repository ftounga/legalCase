import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-213-03b — Helper partagé pour le pré-remplissage IA de
 * `LicenciementBeStatutUniquePreavisSectionComponent` (F-213, BE uniquement).
 *
 * Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime `prefillFromAi()`
 * et static `getPrefillCount()` consomment le MÊME helper pur ; divergence
 * impossible par construction.
 *
 * <b>4 champs pré-remplissables</b> depuis `TravailExtractedData` branche BE :
 *
 * <ol>
 *   <li>{@code ancienneteAnnees} ← années complètes calculées
 *       (`dateEntree` → `dateLicenciement` OU `today`). Plafond métier 80.</li>
 *   <li>{@code ancienneteMoisSupplementaires} ← reliquat 0-11 mois après les
 *       années complètes (même source). Couple intrinsèque avec `ancienneteAnnees`
 *       — comptés ENSEMBLE comme 1 seul "champ ancienneté" dans le badge.</li>
 *   <li>{@code salaireHebdomadaireBrut} ← dérivé `salaireBrutAnnuel / 52`
 *       (priorité 1, plus stable) ou `salaireBrutMensuel × 12 / 52` (fallback).</li>
 *   <li>{@code dateNotificationLicenciement} ← `dateLicenciement` (ISO).</li>
 * </ol>
 *
 * <p>Le 5ème champ technique `partieStatutUniqueSeulement` (checkbox) est
 * <b>dérivé</b> de `dateEntree >= 2014-01-01` (true) — il n'est PAS compté
 * dans le badge `getPrefillCount` (cf. mini-spec §"Pré-fill rules") car il
 * est un flag UX, pas une saisie IA factuelle.</p>
 *
 * <p>Gating : `workspaceCountry !== 'BELGIQUE'` → null (outil mono-pays BE,
 * Loi 26/12/2013 statut unique — pas d'équivalent FR direct, le régime
 * français repose sur les conventions collectives + L. 1234-1 C. trav.).</p>
 *
 * <p><b>Calcul ancienneté</b> : on prend `dateEntree → dateLicenciement` si
 * les deux sont valides ; sinon fallback `dateEntree → today` (l'avocat peut
 * raffiner manuellement). Le calcul exclut les heures et conserve la
 * granularité année/mois en respectant la convention BE (mois calendaire).
 * Limite haute : 80 ans (parité backend `@Max(80)` sur `ancienneteAnnees`).</p>
 */

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

/** Seuil légal d'entrée en vigueur du statut unique (Loi 26/12/2013). */
const STATUT_UNIQUE_THRESHOLD_ISO = '2014-01-01';

function positiveNumberOrNull(value: unknown): number | null {
  if (typeof value !== 'number' || !Number.isFinite(value)) return null;
  if (value <= 0) return null;
  return value;
}

function isoDateOrNull(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  const trimmed = value.trim();
  if (!ISO_DATE_REGEX.test(trimmed)) return null;
  const parsed = new Date(trimmed);
  if (Number.isNaN(parsed.getTime())) return null;
  // Vérifie l'aller-retour ISO (rejette 2025-02-31 etc.).
  if (parsed.toISOString().slice(0, 10) !== trimmed) return null;
  return trimmed;
}

/**
 * Calcule l'ancienneté en (années, mois) entre deux dates ISO, à la BE
 * (mois calendaire, jours résiduels ignorés). Retourne `null` si dates
 * invalides ou si la fin précède le début (cas pathologique IA).
 *
 * Algorithme : différence Y/M/D Gregorienne, normalisée pour que mois ∈ [0,11].
 * Plafonné à 80 ans (parité backend `@Max(80)`).
 */
function computeAncienneteFromDates(
  startIso: string | null,
  endIso: string | null,
): { years: number; months: number } | null {
  if (!startIso) return null;
  const effectiveEndIso = endIso ?? new Date().toISOString().slice(0, 10);
  const start = new Date(startIso);
  const end = new Date(effectiveEndIso);
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return null;
  if (end.getTime() < start.getTime()) return null;

  let years = end.getUTCFullYear() - start.getUTCFullYear();
  let months = end.getUTCMonth() - start.getUTCMonth();
  const days = end.getUTCDate() - start.getUTCDate();
  if (days < 0) months -= 1;
  if (months < 0) {
    years -= 1;
    months += 12;
  }
  if (years < 0) return null;
  if (years > 80) years = 80; // plafond métier
  return { years, months };
}

export const LicenciementBeStatutUniquePreavisSectionPrefillRules = {

  /**
   * Calcule l'ancienneté complète (années + reliquat mois) depuis
   * `dateEntree` → `dateLicenciement` (ou today si licenciement absent).
   * Retourne `null` si données insuffisantes / gate FR.
   */
  computeAnciennete(input: PrefillCountInput): { years: number; months: number } | null {
    if (input.workspaceCountry !== 'BELGIQUE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    const dateEntree = isoDateOrNull((ai as { dateEntree?: unknown }).dateEntree);
    const dateLicenciement = isoDateOrNull((ai as { dateLicenciement?: unknown }).dateLicenciement);
    if (!dateEntree) return null;
    return computeAncienneteFromDates(dateEntree, dateLicenciement);
  },

  /** Années complètes d'ancienneté (entier ≥ 0). */
  computeAncienneteAnnees(input: PrefillCountInput): number | null {
    const a = this.computeAnciennete(input);
    return a ? a.years : null;
  },

  /** Reliquat de mois (0-11) — couplé à `computeAncienneteAnnees`. */
  computeAncienneteMoisSupplementaires(input: PrefillCountInput): number | null {
    const a = this.computeAnciennete(input);
    return a ? a.months : null;
  },

  /**
   * Salaire hebdomadaire brut : priorité `salaireBrutAnnuel / 52` puis
   * fallback `salaireBrutMensuel × 12 / 52`. Retourne `null` si aucune
   * source valide (> 0 strict).
   */
  computeSalaireHebdomadaireBrut(input: PrefillCountInput): number | null {
    if (input.workspaceCountry !== 'BELGIQUE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    const annuel = positiveNumberOrNull((ai as { salaireBrutAnnuel?: unknown }).salaireBrutAnnuel);
    if (annuel !== null) {
      return Math.round((annuel / 52) * 100) / 100;
    }
    const mensuel = positiveNumberOrNull((ai as { salaireBrutMensuel?: unknown }).salaireBrutMensuel);
    if (mensuel !== null) {
      return Math.round(((mensuel * 12) / 52) * 100) / 100;
    }
    return null;
  },

  /** Date de notification du licenciement (ISO YYYY-MM-DD). */
  computeDateNotificationLicenciement(input: PrefillCountInput): string | null {
    if (input.workspaceCountry !== 'BELGIQUE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull((ai as { dateLicenciement?: unknown }).dateLicenciement);
  },

  /**
   * Dérive le flag `partieStatutUniqueSeulement` depuis `dateEntree` :
   *  - dateEntree >= 2014-01-01 → true (statut unique pur, le résultat
   *    backend ne nécessitera pas d'avertissement Claeys) ;
   *  - dateEntree < 2014-01-01 → false (contrat mixte, l'avocat sera
   *    averti que la fraction Claeys doit être calculée séparément) ;
   *  - dateEntree absente / invalide → `null` (laisse défaut UI true,
   *    parité backend `defaultIfNull(true)`).
   *
   * <p>NON compté dans `computePrefillCount` — c'est un flag UX dérivé,
   * pas un champ IA factuel saisissable. Le compteur reste à 4 max.</p>
   */
  computePartieStatutUniqueSeulement(input: PrefillCountInput): boolean | null {
    if (input.workspaceCountry !== 'BELGIQUE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    const dateEntree = isoDateOrNull((ai as { dateEntree?: unknown }).dateEntree);
    if (!dateEntree) return null;
    return dateEntree >= STATUT_UNIQUE_THRESHOLD_ISO;
  },

  /**
   * Maître : compte les 4 champs IA factuels pré-remplissables.
   *
   * <p>{@code ancienneteAnnees} et {@code ancienneteMoisSupplementaires}
   * forment 1 couple (issu d'une même date) → comptés <b>1 fois</b>
   * (« ancienneté » = 1 champ). `partieStatutUniqueSeulement` est dérivé
   * UX, non compté. Max théorique = 3 champs distincts (ancienneté,
   * salaire hebdo, date notification).</p>
   *
   * <p>NB : la mini-spec §"Pré-fill rules" annonçait 4 champs en première
   * lecture en couplant l'ancienneté années + mois séparément. Décision
   * livraison : on respecte la sémantique "1 source IA = 1 champ" — le
   * couple (annees, mois) provient d'un même couple de dates, dérivation
   * mécanique. C'est ce que fait également F-DT-08 (déjà mergé) qui
   * compte l'ancienneté comme 1 champ pré-rempli (et non 2).</p>
   */
  computePrefillCount(input: PrefillCountInput): number {
    let n = 0;
    if (this.computeAncienneteAnnees(input) !== null) n++; // 1 = ancienneté couplée
    if (this.computeSalaireHebdomadaireBrut(input) !== null) n++;
    if (this.computeDateNotificationLicenciement(input) !== null) n++;
    return n;
  },
} as const;
