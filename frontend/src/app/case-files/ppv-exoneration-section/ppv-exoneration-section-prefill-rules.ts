import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-218-40 — Helper partagé pour {@link PpvExonerationSectionComponent}
 * (F-DT-52-ppv-exoneration) — Travail FR mono-pays.
 *
 * Champs pre-fill (depuis {@link TravailExtractedData}, sous-record consolidé
 * `Sf218dDetail` sérialisé `@JsonUnwrapped` en clés SNAKE_CASE) :
 *  - montantPrime : `aiData['montant_ppv']` (> 0) — montant de la PPV versée
 *    détecté par l'IA.
 *  - accordInteressementPresent : `aiData['accord_interessement_present']`
 *    (booléen) — présence d'un accord d'intéressement détectée par l'IA.
 *
 * Champs NON pré-remplis :
 *  - remunerationAnnuelleBrute : base du test « < 3 SMIC » — saisie avocat
 *    (donnée de paie agrégée non factualisable de façon fiable depuis les pièces).
 *  - effectifMoins50 : effectif de l'entreprise — saisie avocat (donnée
 *    structurelle non factualisable depuis un bulletin / un accord).
 *  - versementPlanEpargne : affectation à un plan d'épargne — saisie avocat.
 *  - `ppv_detectee` est un FLAG de visibilité (déclenche l'apparition de l'outil
 *    via DecisionToolVisibilityService) — ce n'est PAS un champ du formulaire,
 *    il ne compte donc pas dans le prefill count.
 *
 * Total : 2 champs pre-remplissables.
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

function positiveNumberOrNull(v: unknown): number | null {
  if (typeof v !== 'number' || !Number.isFinite(v) || v <= 0) return null;
  return v;
}

function booleanOrNull(v: unknown): boolean | null {
  if (typeof v !== 'boolean') return null;
  return v;
}

export const PpvExonerationPrefillRules = {

  computeMontantPrime(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData as { montant_ppv?: unknown } | null | undefined;
    if (!ai) return null;
    return positiveNumberOrNull(ai.montant_ppv);
  },

  computeAccordInteressementPresent(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData as { accord_interessement_present?: unknown } | null | undefined;
    if (!ai) return null;
    return booleanOrNull(ai.accord_interessement_present);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeMontantPrime(input) !== null) n++;
    if (this.computeAccordInteressementPresent(input) !== null) n++;
    return n;
  },
} as const;
