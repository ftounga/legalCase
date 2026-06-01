import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { AccordTypeOperation } from '../../core/models/accord-entreprise-validite.model';

/**
 * SF-218-32 — Helper partagé pour
 * {@link AccordEntrepriseValiditeSectionComponent}
 * (F-DT-67-accord-entreprise-validite) — Travail FR mono-pays.
 *
 * Champs pre-fill (depuis {@link TravailExtractedData}) :
 *  - pourcentageSuffragesSignataires : `aiData.accordPourcentageSignataires`
 *    (nombre ∈ [0 ; 100]) — % des suffrages exprimés au 1er tour recueilli par
 *    les syndicats signataires, détecté par l'IA.
 *  - typeOperation : `aiData.accordTypeOperation`
 *    (CONCLUSION / REVISION / DENONCIATION) — type d'opération détecté par l'IA.
 *
 * Champs NON pré-remplis :
 *  - referendumOrganise / referendumApprouve / signePartiesHabilitees /
 *    preavisDenonciationRespecte / dateDenonciation : éléments de procédure non
 *    factualisables de façon fiable depuis les pièces du dossier (saisie avocat).
 *  - `accordEntrepriseDetecte` est un FLAG de visibilité (déclenche l'apparition
 *    de l'outil via DecisionToolVisibilityService) — ce n'est PAS un champ du
 *    formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 2 champs pre-remplissables (sur 7 saisissables).
 */

const VALID_OPERATIONS: ReadonlySet<string> = new Set([
  'CONCLUSION', 'REVISION', 'DENONCIATION',
]);

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

function percentOrNull(v: unknown): number | null {
  if (typeof v !== 'number' || !Number.isFinite(v) || v < 0 || v > 100) return null;
  return v;
}

export const AccordEntrepriseValiditePrefillRules = {

  computePourcentageSignataires(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return percentOrNull(ai.accordPourcentageSignataires);
  },

  computeTypeOperation(input: PrefillCountInput): AccordTypeOperation | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.accordTypeOperation;
    return typeof v === 'string' && VALID_OPERATIONS.has(v)
      ? (v as AccordTypeOperation)
      : null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computePourcentageSignataires(input) !== null) n++;
    if (this.computeTypeOperation(input) !== null) n++;
    return n;
  },
} as const;
