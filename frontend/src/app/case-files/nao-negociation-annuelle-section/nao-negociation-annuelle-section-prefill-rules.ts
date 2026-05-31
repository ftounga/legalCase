import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-218-30 — Helper partagé pour
 * {@link NaoNegociationAnnuelleSectionComponent}
 * (F-DT-66-nao-negociation-annuelle) — Travail FR mono-pays.
 *
 * Champs pre-fill (depuis {@link TravailExtractedData}) :
 *  - effectif : `aiData.pseNombreSalaries` (entier > 0) — effectif de
 *    l'entreprise détecté par l'IA.
 *  - delegueSyndicalPresent : `aiData.delegueSyndicalPresent` (booléen) —
 *    présence d'au moins un délégué syndical détectée par l'IA (déclencheur de
 *    l'obligation de NAO).
 *
 * Champs NON pré-remplis :
 *  - blocRemunerationNegocie / blocEgaliteQvtNegocie / accordMethodePeriodicite /
 *    dateDerniereNegociation / periodiciteMois / pvDesaccordEtabli /
 *    negociationAboutie : éléments de déroulement de la négociation non
 *    factualisables de façon fiable depuis les pièces du dossier (saisie
 *    avocat / employeur).
 *  - `naoDetectee` est un FLAG de visibilité (déclenche l'apparition de l'outil
 *    via DecisionToolVisibilityService) — ce n'est PAS un champ du formulaire,
 *    il ne compte donc pas dans le prefill count.
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

export const NaoNegociationAnnuellePrefillRules = {

  computeEffectif(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return positiveIntOrNull(ai.pseNombreSalaries);
  },

  computeDelegueSyndicalPresent(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.delegueSyndicalPresent;
    return typeof v === 'boolean' ? v : null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeEffectif(input) !== null) n++;
    if (this.computeDelegueSyndicalPresent(input) !== null) n++;
    return n;
  },
} as const;
