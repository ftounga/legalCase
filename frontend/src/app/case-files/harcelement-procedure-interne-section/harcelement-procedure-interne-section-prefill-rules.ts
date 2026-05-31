import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-218-28 — Helper partagé pour
 * {@link HarcelementProcedureInterneSectionComponent}
 * (F-DT-59-harcelement-procedure-interne) — Travail FR mono-pays.
 *
 * Champs pre-fill (depuis {@link TravailExtractedData}) :
 *  - effectif : `aiData.pseNombreSalaries` (entier > 0) — effectif de
 *    l'entreprise détecté par l'IA.
 *  - signalementRecu : `aiData.harcelementSignalementInterne` (booléen) — un
 *    signalement interne de harcèlement a été reçu, détecté par l'IA.
 *
 * Champs NON pré-remplis :
 *  - referentCseDesigne / referentEmployeurDesigne / informationAffichageRealisee /
 *    enqueteContradictoire / mesuresConservatoiresPrises / dateSignalement /
 *    dateOuvertureEnquete : éléments de procédure interne non factualisables de
 *    façon fiable depuis les pièces du dossier (saisie avocat / employeur).
 *  - `harcelementProcedureInterneDetectee` est un FLAG de visibilité (déclenche
 *    l'apparition de l'outil via DecisionToolVisibilityService) — ce n'est PAS
 *    un champ du formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 2 champs pre-remplissables (sur 9 saisissables).
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

function positiveIntOrNull(v: unknown): number | null {
  if (typeof v !== 'number' || !Number.isFinite(v) || v <= 0) return null;
  return Math.trunc(v);
}

export const HarcelementProcedureInternePrefillRules = {

  computeEffectif(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return positiveIntOrNull(ai.pseNombreSalaries);
  },

  computeSignalementRecu(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.harcelementSignalementInterne;
    return typeof v === 'boolean' ? v : null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeEffectif(input) !== null) n++;
    if (this.computeSignalementRecu(input) !== null) n++;
    return n;
  },
} as const;
