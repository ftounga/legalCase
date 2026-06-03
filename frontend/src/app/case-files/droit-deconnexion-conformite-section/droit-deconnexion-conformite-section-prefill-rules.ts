import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-218-54 — Helper partagé pour {@link DroitDeconnexionConformiteSectionComponent}
 * (F-DT-83-droit-deconnexion-conformite) — Travail FR mono-pays.
 *
 * Champs pre-fill (depuis {@link TravailExtractedData}, sous-record consolidé
 * `Sf218dDetail` sérialisé `@JsonUnwrapped` en clés SNAKE_CASE) :
 *  - accordOuChartePresent : `aiData['accord_deconnexion_present']` (booléen) —
 *    présence d'un accord ou d'une charte sur le droit à la déconnexion détectée
 *    par l'IA.
 *
 * Champs NON pré-remplis :
 *  - effectif : effectif de l'entreprise — saisie avocat. Donnée structurelle non
 *    factualisable de façon fiable depuis les pièces (aucune clé `effectif` dans
 *    `Sf218dDetail`) ; conformément à F-246, l'absence d'info factualisable
 *    justifie l'absence de pré-fill.
 *  - delegueSyndicalPresent : présence d'un délégué syndical — saisie avocat
 *    (donnée structurelle non factualisable de façon fiable).
 *  - plagesDeconnexionDefinies / actionsSensibilisation / avisCseRecueilliPourCharte :
 *    points de contrôle de conformité — saisie avocat (appréciation du contenu
 *    de l'accord ou de la charte, non factualisable de façon binaire fiable).
 *  - `droit_deconnexion_detecte` est un FLAG de visibilité (déclenche l'apparition
 *    de l'outil via DecisionToolVisibilityService) — ce n'est PAS un champ du
 *    formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 1 champ pre-remplissable.
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

function booleanOrNull(v: unknown): boolean | null {
  if (typeof v !== 'boolean') return null;
  return v;
}

export const DroitDeconnexionConformitePrefillRules = {

  computeAccordOuChartePresent(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData as { accord_deconnexion_present?: unknown } | null | undefined;
    if (!ai) return null;
    return booleanOrNull(ai.accord_deconnexion_present);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeAccordOuChartePresent(input) !== null) n++;
    return n;
  },
} as const;
