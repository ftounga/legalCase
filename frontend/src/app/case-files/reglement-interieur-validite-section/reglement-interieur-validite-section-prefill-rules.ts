import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-218-36 — Helper partagé pour
 * {@link ReglementInterieurValiditeSectionComponent}
 * (F-DT-100-reglement-interieur-validite) — Travail FR mono-pays.
 *
 * Champs pre-fill (depuis {@link TravailExtractedData}) :
 *  - effectif : `aiData.pseNombreSalaries` (entier > 0) — effectif de
 *    l'entreprise détecté par l'IA (le RI est obligatoire dès 50 salariés,
 *    art. L.1311-2 CT).
 *  - reglementExiste : `aiData.reglementInterieurPresent` (booléen) — présence
 *    effective d'un règlement intérieur détectée par l'IA.
 *
 * Champs NON pré-remplis :
 *  - contenuHygieneSecurite / contenuDiscipline / contenuDroitsDefense /
 *    contenuHarcelementAgissements / clauseAtteinteLibertesNonJustifiee /
 *    clauseSanctionPecuniaire / consultationCseRealisee /
 *    transmissionInspectionTravail / depotGreffeCph : le contenu détaillé du RI
 *    et les formalités de mise en place ne sont pas factualisables de façon
 *    fiable depuis les pièces du dossier (lecture clause par clause / preuve des
 *    dépôts — saisie avocat).
 *  - `reglementInterieurDetecte` est un FLAG de visibilité (déclenche
 *    l'apparition de l'outil via DecisionToolVisibilityService) — ce n'est PAS
 *    un champ du formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 2 champs pre-remplissables (sur 11 saisissables).
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

function positiveIntOrNull(v: unknown): number | null {
  if (typeof v !== 'number' || !Number.isFinite(v) || v <= 0) return null;
  return Math.trunc(v);
}

export const ReglementInterieurValiditePrefillRules = {

  computeEffectif(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return positiveIntOrNull(ai.pseNombreSalaries);
  },

  computeReglementExiste(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.reglementInterieurPresent;
    return typeof v === 'boolean' ? v : null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeEffectif(input) !== null) n++;
    if (this.computeReglementExiste(input) !== null) n++;
    return n;
  },
} as const;
