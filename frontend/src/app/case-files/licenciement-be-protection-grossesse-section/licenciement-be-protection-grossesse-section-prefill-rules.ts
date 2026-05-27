import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-213-05b — Helper partagé pour le pré-remplissage IA de
 * `LicenciementBeProtectionGrossesseSectionComponent` (F-213, BE uniquement).
 *
 * Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime `prefillFromAi()`
 * et static `getPrefillCount()` consomment le MÊME helper pur ; divergence
 * impossible par construction.
 *
 * <b>Champs pré-remplissables</b> depuis `TravailExtractedData` :
 *
 * <ol>
 *   <li>{@code dateLicenciement} ← `aiData.dateLicenciement` (champ existant
 *       Travail FR+BE alimenté par F-204 / SF-207-01). C'est la date de
 *       notification du licenciement.</li>
 *   <li>{@code remunerationMensuelleBrute} ← dérivé
 *       `salaireBrutMensuel` (priorité directe) ou `salaireBrutAnnuel / 12`
 *       (fallback). Arrondi 2 décimales.</li>
 * </ol>
 *
 * <p><b>Champs non pré-remplis</b> (volontairement) :
 * <ul>
 *   <li>{@code dateDebutGrossesse} — pas de champ IA BE-only dédié en DB
 *       actuellement (TravailExtractedData ne porte pas la grossesse —
 *       feedback F-213 backend pattern : pas d'enrichissement IA dans
 *       cette vague). L'avocat saisit la date depuis le dossier.</li>
 *   <li>{@code dateAccouchement}, {@code dateCongeMaterniteDebut},
 *       {@code dateCongeMaterniteFinale} — idem, saisie manuelle avocat.</li>
 *   <li>{@code grossesseNotifieeParEcrit} — booléen à arbitrer par l'avocat
 *       (lecture du courrier de notification annexé au dossier).</li>
 *   <li>{@code motifInvoqueParEmployeur} — texte libre saisie avocat.</li>
 * </ul>
 * Ces champs pourront être enrichis par une SF ultérieure F-213-XX une
 * fois que le pipeline IA aura été étendu pour extraire les indices de
 * grossesse / maternité (à arbitrer hors scope F-213).</p>
 *
 * <p>Gating : `workspaceCountry !== 'BELGIQUE'` → null (outil mono-pays BE,
 * pas d'équivalent FR direct — régime juridique distinct).</p>
 *
 * <p><b>Comptage parité F-236</b> :
 * <ul>
 *   <li>date licenciement = 1</li>
 *   <li>rémunération mensuelle brute (1 source IA = 1 champ) = 1</li>
 * </ul>
 * Max théorique = 2.</p>
 */

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

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

export const LicenciementBeProtectionGrossesseSectionPrefillRules = {

  /**
   * Date de licenciement = `aiData.dateLicenciement` (champ Travail standard
   * extrait par F-204 / SF-207-01). Validée ISO YYYY-MM-DD strict.
   */
  computeDateLicenciement(input: PrefillCountInput): string | null {
    if (input.workspaceCountry !== 'BELGIQUE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull((ai as { dateLicenciement?: unknown }).dateLicenciement);
  },

  /**
   * Rémunération mensuelle brute : priorité `salaireBrutMensuel` (direct),
   * fallback `salaireBrutAnnuel / 12`. Arrondi 2 décimales.
   */
  computeRemunerationMensuelleBrute(input: PrefillCountInput): number | null {
    if (input.workspaceCountry !== 'BELGIQUE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    const mensuel = positiveNumberOrNull((ai as { salaireBrutMensuel?: unknown }).salaireBrutMensuel);
    if (mensuel !== null) {
      return Math.round(mensuel * 100) / 100;
    }
    const annuel = positiveNumberOrNull((ai as { salaireBrutAnnuel?: unknown }).salaireBrutAnnuel);
    if (annuel !== null) {
      return Math.round((annuel / 12) * 100) / 100;
    }
    return null;
  },

  /**
   * Maître : compte les champs IA factuels pré-remplissables.
   *
   * <p>1 date licenciement + 1 rémunération mensuelle brute (1 source IA
   * comptée 1 fois quel que soit le canal). Max théorique = 2.</p>
   */
  computePrefillCount(input: PrefillCountInput): number {
    let n = 0;
    if (this.computeDateLicenciement(input) !== null) n++;
    if (this.computeRemunerationMensuelleBrute(input) !== null) n++;
    return n;
  },
} as const;
