import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-214-36 — Helper partagé pour {@link AssignationResidenceSectionComponent}
 * (F-IM-42-assignation-residence-fr) — FRANCE mono-pays.
 *
 * Champ pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - dateNotificationAssignation : `aiData.assignationDateNotification`
 *    (ISO YYYY-MM-DD) — date de notification de l'arrêté d'assignation à
 *    résidence, point de départ de la durée de l'assignation.
 *
 * Champs NON pré-remplis :
 *  - dureeAssignationJours : durée fixée par l'arrêté (chiffre) laissée à
 *    l'avocat — non factualisable de façon fiable depuis les pièces.
 *  - motifAssignation : qualification juridique (exécution OQTF / surveillance /
 *    autre) laissée à l'avocat.
 *  - obligationsPresentation : obligations de présentation (texte libre) laissées
 *    à l'avocat.
 *
 * Total : 1 champ pre-remplissable (sur 4 saisissables).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const AssignationResidencePrefillRules = {

  computeDateNotification(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.assignationDateNotification;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim();
    if (!ISO_DATE_RE.test(trimmed)) return null;
    return trimmed;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateNotification(input) !== null) n++;
    return n;
  },
} as const;
