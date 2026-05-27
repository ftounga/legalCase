import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-213-04b — Helper partagé pour le pré-remplissage IA de
 * `LicenciementBeFormuleClaeysSectionComponent` (F-213, BE uniquement).
 *
 * Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime `prefillFromAi()`
 * et static `getPrefillCount()` consomment le MÊME helper pur ; divergence
 * impossible par construction.
 *
 * <b>Champs pré-remplissables</b> depuis `TravailExtractedData` branche BE :
 *
 * <ol>
 *   <li>{@code ancienneteAnneesPreStatutUnique} ← années écoulées entre
 *       {@code dateEntree} et la borne {@code 2014-01-01}. Couplé avec
 *       le reliquat de mois (= 1 source IA = 1 champ comptabilisé).</li>
 *   <li>{@code ancienneteMoisPreStatutUnique} ← reliquat 0-11 mois après
 *       les années complètes pré-2014 (même source). Couple intrinsèque
 *       avec {@code ancienneteAnneesPreStatutUnique}.</li>
 *   <li>{@code remunerationAnnuelleBruteEnMilliers} ← dérivé
 *       {@code salaireBrutAnnuel / 1000} (priorité) ou
 *       {@code salaireBrutMensuel × 12 / 1000} (fallback). NB : le backend
 *       attend la valeur en K€.</li>
 *   <li>{@code appliquerClauseSauvegarde} ← dérivé
 *       {@code dateEntree < 2014-01-01} (toujours true si contrat pré-2014
 *       pour bénéficier du préavis post-2014 cumulé via la loi 26/12/2013
 *       art. 67). NON compté dans le badge — flag UX dérivé.</li>
 *   <li>{@code ancienneteAnneesPostStatutUnique} ← années post-2014
 *       entre {@code 2014-01-01} et {@code dateLicenciement} (ou today
 *       en fallback). Compté quand pré-fillable.</li>
 *   <li>{@code salaireHebdomadaireBrut} ← dérivé
 *       {@code salaireBrutAnnuel / 52} (priorité) ou
 *       {@code salaireBrutMensuel × 12 / 52} (fallback). Compté quand
 *       pré-fillable.</li>
 * </ol>
 *
 * <p>Gating : `workspaceCountry !== 'BELGIQUE'` → null (outil mono-pays BE,
 * pas d'équivalent FR — régime juridiquement distinct).</p>
 *
 * <p><b>Comptage parité F-236</b> :
 * <ul>
 *   <li>ancienneté pré (années+mois) couplée = 1</li>
 *   <li>rémunération annuelle K€ = 1</li>
 *   <li>ancienneté post (si dateLicenciement après 2014) = 1</li>
 *   <li>salaire hebdo brut = 1</li>
 * </ul>
 * Max théorique = 4. Le flag {@code appliquerClauseSauvegarde} dérivé
 * n'est pas compté.</p>
 */

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

/** Borne légale d'entrée en vigueur du statut unique (Loi 26/12/2013). */
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
  if (parsed.toISOString().slice(0, 10) !== trimmed) return null;
  return trimmed;
}

/**
 * Calcule l'ancienneté en (années, mois) entre deux dates ISO, à la BE
 * (mois calendaire, jours résiduels ignorés). Retourne null si dates
 * invalides ou si la fin précède le début. Plafonné à 80 ans.
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
  if (years > 80) years = 80;
  return { years, months };
}

export const LicenciementBeFormuleClaeysSectionPrefillRules = {

  /**
   * Calcule l'ancienneté pré-statut unique = de `dateEntree` au pivot
   * 2014-01-01. Retourne null si dateEntree absente / postérieure au pivot
   * (contrat post-2014 → Claeys non applicable).
   */
  computeAnciennetePreStatutUnique(input: PrefillCountInput): { years: number; months: number } | null {
    if (input.workspaceCountry !== 'BELGIQUE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    const dateEntree = isoDateOrNull((ai as { dateEntree?: unknown }).dateEntree);
    if (!dateEntree) return null;
    // Claeys non applicable si contrat post-2014 (le statut unique pur
    // couvre tout, pas de fraction Claeys à calculer).
    if (dateEntree >= STATUT_UNIQUE_THRESHOLD_ISO) return null;
    return computeAncienneteFromDates(dateEntree, STATUT_UNIQUE_THRESHOLD_ISO);
  },

  /** Années complètes pré-2014 (entier ≥ 0). */
  computeAncienneteAnneesPreStatutUnique(input: PrefillCountInput): number | null {
    const a = this.computeAnciennetePreStatutUnique(input);
    return a ? a.years : null;
  },

  /** Reliquat de mois pré-2014 (0-11) — couplé à `computeAncienneteAnneesPreStatutUnique`. */
  computeAncienneteMoisPreStatutUnique(input: PrefillCountInput): number | null {
    const a = this.computeAnciennetePreStatutUnique(input);
    return a ? a.months : null;
  },

  /**
   * Calcule l'ancienneté post-statut unique = de 2014-01-01 à
   * `dateLicenciement` (ou today en fallback). Retourne null si pas de
   * dateEntree pré-2014 (cas où Claeys ne s'applique pas).
   */
  computeAnciennetePostStatutUnique(input: PrefillCountInput): { years: number; months: number } | null {
    if (input.workspaceCountry !== 'BELGIQUE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    const dateEntree = isoDateOrNull((ai as { dateEntree?: unknown }).dateEntree);
    // Pas de pré-2014 → pas d'application Claeys → pas de fraction post non plus.
    if (!dateEntree || dateEntree >= STATUT_UNIQUE_THRESHOLD_ISO) return null;
    const dateLicenciement = isoDateOrNull((ai as { dateLicenciement?: unknown }).dateLicenciement);
    return computeAncienneteFromDates(STATUT_UNIQUE_THRESHOLD_ISO, dateLicenciement);
  },

  /** Années post-2014 (entier ≥ 0). */
  computeAncienneteAnneesPostStatutUnique(input: PrefillCountInput): number | null {
    const a = this.computeAnciennetePostStatutUnique(input);
    return a ? a.years : null;
  },

  /**
   * Rémunération annuelle brute en K€. Priorité `salaireBrutAnnuel / 1000`,
   * fallback `salaireBrutMensuel × 12 / 1000`. Arrondi 2 décimales.
   */
  computeRemunerationAnnuelleBruteEnMilliers(input: PrefillCountInput): number | null {
    if (input.workspaceCountry !== 'BELGIQUE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    const annuel = positiveNumberOrNull((ai as { salaireBrutAnnuel?: unknown }).salaireBrutAnnuel);
    if (annuel !== null) {
      return Math.round((annuel / 1000) * 100) / 100;
    }
    const mensuel = positiveNumberOrNull((ai as { salaireBrutMensuel?: unknown }).salaireBrutMensuel);
    if (mensuel !== null) {
      return Math.round(((mensuel * 12) / 1000) * 100) / 100;
    }
    return null;
  },

  /**
   * Salaire hebdomadaire brut. Priorité `salaireBrutAnnuel / 52`, fallback
   * `salaireBrutMensuel × 12 / 52`. Arrondi 2 décimales.
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

  /**
   * Dérive le flag `appliquerClauseSauvegarde` depuis `dateEntree` :
   *  - dateEntree < 2014-01-01 → true (clause activable pour cumuler
   *    Claeys + statut unique post-2014, parité backend defaultIfNull) ;
   *  - dateEntree >= 2014-01-01 → null (Claeys non applicable, l'outil
   *    n'aurait pas dû être proposé — laisse défaut UI true sans IA) ;
   *  - dateEntree absente / invalide → null.
   *
   * <p>NON compté dans `computePrefillCount` — flag UX dérivé.</p>
   */
  computeAppliquerClauseSauvegarde(input: PrefillCountInput): boolean | null {
    if (input.workspaceCountry !== 'BELGIQUE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    const dateEntree = isoDateOrNull((ai as { dateEntree?: unknown }).dateEntree);
    if (!dateEntree) return null;
    return dateEntree < STATUT_UNIQUE_THRESHOLD_ISO ? true : null;
  },

  /**
   * Maître : compte les champs IA factuels pré-remplissables.
   *
   * <p>1 ancienneté pré (couplée années+mois) + 1 rémunération K€ +
   * 1 ancienneté post (si dateLicenciement post-2014) + 1 salaire hebdo.
   * Le flag `appliquerClauseSauvegarde` dérivé n'est PAS compté.</p>
   *
   * <p>Max théorique = 4.</p>
   */
  computePrefillCount(input: PrefillCountInput): number {
    let n = 0;
    if (this.computeAncienneteAnneesPreStatutUnique(input) !== null) n++; // 1 = ancienneté pré couplée
    if (this.computeRemunerationAnnuelleBruteEnMilliers(input) !== null) n++;
    if (this.computeAncienneteAnneesPostStatutUnique(input) !== null) n++;
    if (this.computeSalaireHebdomadaireBrut(input) !== null) n++;
    return n;
  },
} as const;
