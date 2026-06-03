import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-218-42 — Helper partagé pour {@link EpargneSalarialeConformiteSectionComponent}
 * (F-DT-53-epargne-salariale-conformite) — Travail FR mono-pays.
 *
 * Champs pre-fill (depuis {@link TravailExtractedData}, sous-record consolidé
 * `Sf218dDetail` sérialisé `@JsonUnwrapped` en clés SNAKE_CASE) :
 *  - accordParticipationPresent : `aiData['accord_participation_present']`
 *    (booléen) — présence d'un accord/régime de participation détectée par l'IA.
 *  - accordInteressementPresent : `aiData['accord_interessement_present']`
 *    (booléen) — présence d'un accord d'intéressement détectée par l'IA (champ
 *    mutualisé avec F-DT-52).
 *
 * Champs NON pré-remplis :
 *  - effectif : effectif de l'entreprise — saisie avocat. Donnée structurelle
 *    non factualisable de façon fiable depuis les pièces (aucune clé `effectif`
 *    dans `Sf218dDetail`) ; conformément à F-246, l'absence d'info factualisable
 *    justifie l'absence de pré-fill.
 *  - beneficeNetFiscalPositif3Ans : rentabilité sur 3 exercices — saisie avocat
 *    (donnée comptable agrégée non factualisable depuis un bulletin / un accord).
 *  - entreprise11a49 : tranche d'effectif 11–49 — saisie avocat (corollaire de
 *    l'effectif, non factualisable).
 *  - `epargne_salariale_detectee` est un FLAG de visibilité (déclenche
 *    l'apparition de l'outil via DecisionToolVisibilityService) — ce n'est PAS un
 *    champ du formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 2 champs pre-remplissables.
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

function booleanOrNull(v: unknown): boolean | null {
  if (typeof v !== 'boolean') return null;
  return v;
}

export const EpargneSalarialeConformitePrefillRules = {

  computeAccordParticipationPresent(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData as { accord_participation_present?: unknown } | null | undefined;
    if (!ai) return null;
    return booleanOrNull(ai.accord_participation_present);
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
    if (this.computeAccordParticipationPresent(input) !== null) n++;
    if (this.computeAccordInteressementPresent(input) !== null) n++;
    return n;
  },
} as const;
