/**
 * F-236 SF-236-02 — Helper partagé pour le comparateur d'indemnités (F-DT-09).
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir de `IndemniteComparatifSectionComponent.prefillFromAi()` :
 *   `salaireMensuel    ← aiData.salaireBrutMensuel`
 *   `ancienneteAnnees  ← synthesis.compensationEstimate.ancienneteAnnees`
 *   `ancienneteMois    ← synthesis.compensationEstimate.ancienneteMois`
 *   `typeRupture       ← synthesis.compensationEstimate.typeRupture` (si supporté
 *                        par le pays du dossier — FR / BE).
 *
 * F-236 SF-236-04 — couverture alerte F-IA-03 enrichie :
 * `computeAlertesValidite(input)` retourne les signaux issus de
 * `synthesis.ruptureConvValidityDetection` / `licenciementValidityDetection`
 * exposés en input mais pas encore consommés par le composant (audit-matrix
 * anomalie D). Le runtime peut s'en servir pour compléter `buildTypeRuptureAlert`
 * sans dupliquer la logique. Pas de pré-fill direct — c'est uniquement de la
 * couverture d'alerte (cf. directive SF-236-04 : "enrichit les alertes de
 * cohérence F-IA-03 — pas le pré-fill direct, juste la couverture alerte").
 */

const TYPES_FR_VALUES = new Set<string>(['LICENCIEMENT', 'LICENCIEMENT_ECONOMIQUE']);
const TYPES_BE_VALUES = new Set<string>(['LICENCIEMENT_ORDINAIRE']);

export interface IndemniteComparatifPrefillInput {
  aiData?: { salaireBrutMensuel?: number | null } | null;
  synthesis?: {
    compensationEstimate?: {
      ancienneteAnnees?: number | null;
      ancienneteMois?: number | null;
      typeRupture?: string | null;
    } | null;
    /**
     * F-236 SF-236-04 — détection validité rupture conventionnelle
     * (F-DT-10 / F-152) exposée en input via TOOL_REGISTRY mais
     * jusque-là non consommée. Signal indirect que le dossier est
     * orienté RUPTURE_CONVENTIONNELLE.
     */
    ruptureConvValidityDetection?: {
      detections?: { [code: string]: { reponse?: string } } | null;
    } | null;
    /**
     * F-236 SF-236-04 — idem F-DT-08 : signal indirect que le dossier
     * est orienté LICENCIEMENT.
     */
    licenciementValidityDetection?: {
      detections?: { [code: string]: { reponse?: string } } | null;
    } | null;
  } | null;
  workspaceCountry?: string;
}

export function computeSalaireMensuel(input: IndemniteComparatifPrefillInput): number | null {
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computeAncienneteAnnees(input: IndemniteComparatifPrefillInput): number | null {
  const v = input.synthesis?.compensationEstimate?.ancienneteAnnees;
  return typeof v === 'number' ? v : null;
}

export function computeAncienneteMois(input: IndemniteComparatifPrefillInput): number | null {
  const v = input.synthesis?.compensationEstimate?.ancienneteMois;
  return typeof v === 'number' ? v : null;
}

/** Retourne le typeRupture si supporté par le pays, null sinon. */
export function computeTypeRupture(input: IndemniteComparatifPrefillInput): string | null {
  const v = input.synthesis?.compensationEstimate?.typeRupture;
  if (typeof v !== 'string' || v.length === 0) return null;
  const allowed = input.workspaceCountry === 'BELGIQUE' ? TYPES_BE_VALUES : TYPES_FR_VALUES;
  return allowed.has(v) ? v : null;
}

export function computePrefillCount(input: IndemniteComparatifPrefillInput): number {
  let count = 0;
  if (computeSalaireMensuel(input) !== null) count++;
  if (computeAncienneteAnnees(input) !== null) count++;
  if (computeAncienneteMois(input) !== null) count++;
  if (computeTypeRupture(input) !== null) count++;
  return count;
}

/**
 * F-236 SF-236-04 — couverture alerte F-IA-03 enrichie.
 *
 * Inspecte `synthesis.ruptureConvValidityDetection` et
 * `synthesis.licenciementValidityDetection` (exposés en input via TOOL_REGISTRY
 * mais non consommés à ce jour par `buildTypeRuptureAlert`) et retourne, pour
 * chacun, un signal de présence de détections "OUI" (i.e. validité concrètement
 * affirmée par l'IA dans ce sens). Le runtime peut s'en servir pour enrichir
 * l'alerte F-IA-03 sur TYPE_RUPTURE quand la saisie avocat diverge du sens
 * affirmé par l'IA.
 *
 * Retourne `null` si aucune des deux détections n'est exploitable ; sinon un
 * objet avec un ou deux champs.
 */
export interface IndemniteValiditeAlertes {
  /**
   * Code suggéré par `synthesis.ruptureConvValidityDetection` quand au moins
   * un critère vaut OUI → on infère TYPE_RUPTURE = RUPTURE_CONVENTIONNELLE.
   */
  ruptureConvAlert?: 'RUPTURE_CONVENTIONNELLE';
  /**
   * Code suggéré par `synthesis.licenciementValidityDetection` quand au moins
   * un critère vaut OUI → on infère TYPE_RUPTURE = LICENCIEMENT.
   */
  licenciementAlert?: 'LICENCIEMENT';
}

function hasOuiDetection(
  detections: { [code: string]: { reponse?: string } } | null | undefined,
): boolean {
  if (!detections || typeof detections !== 'object') return false;
  for (const code of Object.keys(detections)) {
    if (detections[code]?.reponse === 'OUI') return true;
  }
  return false;
}

export function computeAlertesValidite(
  input: IndemniteComparatifPrefillInput,
): IndemniteValiditeAlertes | null {
  const synth = input.synthesis;
  if (!synth) return null;
  const out: IndemniteValiditeAlertes = {};
  if (hasOuiDetection(synth.ruptureConvValidityDetection?.detections)) {
    out.ruptureConvAlert = 'RUPTURE_CONVENTIONNELLE';
  }
  if (hasOuiDetection(synth.licenciementValidityDetection?.detections)) {
    out.licenciementAlert = 'LICENCIEMENT';
  }
  return out.ruptureConvAlert || out.licenciementAlert ? out : null;
}

export const IndemniteComparatifSectionPrefillRules = {
  computeSalaireMensuel,
  computeAncienneteAnnees,
  computeAncienneteMois,
  computeTypeRupture,
  computeAlertesValidite,
  computePrefillCount,
};
