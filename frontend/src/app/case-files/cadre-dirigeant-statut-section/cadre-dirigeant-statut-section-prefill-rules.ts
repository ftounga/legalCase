import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-218-20 — Helper partagé pour {@link CadreDirigeantStatutSectionComponent}
 * (F-DT-107-cadre-dirigeant-statut) — Travail FR mono-pays.
 *
 * Champs pre-fill (depuis {@link TravailExtractedData}) :
 *  - participationDirectionEntreprise : `aiData.cadreParticipationDirection`
 *    (booléen) — participation effective à la direction de l'entreprise détectée
 *    par l'IA (Cass. soc. post-2012).
 *  - niveauRemunerationConstate : `aiData.salaireBrutMensuel` (montant > 0) —
 *    rémunération mensuelle constatée, élément de preuve du critère 3.
 *
 * Champs NON pré-remplis :
 *  - independanceEmploiDuTemps / autonomieDecision / remunerationParmiPlusElevees :
 *    appréciations juridiques des 3 critères cumulatifs, non factualisables de
 *    façon fiable depuis les pièces du dossier (saisie avocat).
 *  - `statutCadreDirigeantDetecte` est un FLAG de visibilité (déclenche
 *    l'apparition de l'outil via DecisionToolVisibilityService) — ce n'est PAS
 *    un champ du formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 2 champs pre-remplissables (sur 5 saisissables).
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

function positiveNumberOrNull(v: unknown): number | null {
  if (typeof v !== 'number' || !Number.isFinite(v) || v <= 0) return null;
  return v;
}

export const CadreDirigeantStatutPrefillRules = {

  computeParticipationDirection(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.cadreParticipationDirection;
    return typeof v === 'boolean' ? v : null;
  },

  computeNiveauRemuneration(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return positiveNumberOrNull(ai.salaireBrutMensuel);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeParticipationDirection(input) !== null) n++;
    if (this.computeNiveauRemuneration(input) !== null) n++;
    return n;
  },
} as const;
