import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-218-16 — Helper partagé pour {@link JournalisteStatutSectionComponent}
 * (F-DT-105-journaliste-statut) — Travail FR mono-pays.
 *
 * Champs pre-fill (depuis {@link TravailExtractedData}) :
 *  - dateEntree : `aiData.dateEntree` (ISO YYYY-MM-DD) — début du contrat.
 *  - dateRupture : `aiData.dateLicenciement` (ISO YYYY-MM-DD) — notification de
 *    la rupture.
 *  - carteIdentiteProfessionnelle : `aiData.journalisteCartePresse` (booléen) —
 *    détention de la carte de presse (CCIJP), présomption de qualité.
 *
 * Champs NON pré-remplis :
 *  - typeRupture : qualification juridique laissée à l'avocat.
 *  - salaireMensuelMoyen : assiette propre au calcul, non factualisable de
 *    façon fiable depuis les pièces.
 *  - cessionTitreConstatee / changementOrientationConstate : faits générateurs
 *    qualifiés par l'avocat.
 *  - `statutJournalisteDetecte` est un FLAG de visibilité (déclenche
 *    l'apparition de l'outil via DecisionToolVisibilityService) — ce n'est PAS
 *    un champ du formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 3 champs pre-remplissables (sur 7 saisissables).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

function isoDateOrNull(v: unknown): string | null {
  if (typeof v !== 'string') return null;
  const trimmed = v.trim();
  if (!ISO_DATE_RE.test(trimmed)) return null;
  return trimmed;
}

export const JournalisteStatutPrefillRules = {

  computeDateEntree(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull(ai.dateEntree);
  },

  computeDateRupture(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull(ai.dateLicenciement);
  },

  computeCarteIdentiteProfessionnelle(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.journalisteCartePresse;
    return typeof v === 'boolean' ? v : null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateEntree(input) !== null) n++;
    if (this.computeDateRupture(input) !== null) n++;
    if (this.computeCarteIdentiteProfessionnelle(input) !== null) n++;
    return n;
  },
} as const;
