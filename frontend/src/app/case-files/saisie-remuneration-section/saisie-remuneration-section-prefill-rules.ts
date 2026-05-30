import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-218-08 — Helper partagé pour {@link SaisieRemunerationSectionComponent}
 * (F-DT-89-saisie-arret-remuneration) — Travail FR mono-pays.
 *
 * Champs pre-fill (depuis {@link TravailExtractedData}) :
 *  - remunerationNetteMensuelle : dérivé de `aiData.salaireBrutMensuel` (proxy —
 *    la conversion brut → net n'est pas garantie, une note de provenance le
 *    signale dans l'UI). Assiette de la quotité saisissable.
 *  - nombrePersonnesACharge : `aiData.nombrePersonnesACharge` (entier ≥ 0).
 *    Majore les seuils du barème (R. 3252-3).
 *
 * Champs NON pré-remplis :
 *  - creanceTotale : montant de la créance à recouvrer — propre au dossier de
 *    saisie, non factualisable depuis les pièces du contrat de travail.
 *  - creanceAlimentaire : qualification juridique laissée à l'avocat.
 *  - `saisieRemunerationDetectee` est un FLAG de visibilité (déclenche
 *    l'apparition de l'outil via DecisionToolVisibilityService) — ce n'est PAS
 *    un champ du formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 2 champs pre-remplissables (sur 4 saisissables).
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const SaisieRemunerationPrefillRules = {

  computeRemunerationNetteMensuelle(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.salaireBrutMensuel;
    if (typeof v !== 'number' || !Number.isFinite(v) || v <= 0) return null;
    return v;
  },

  computeNombrePersonnesACharge(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.nombrePersonnesACharge;
    if (typeof v !== 'number' || !Number.isFinite(v) || v < 0 || !Number.isInteger(v)) return null;
    return v;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeRemunerationNetteMensuelle(input) !== null) n++;
    if (this.computeNombrePersonnesACharge(input) !== null) n++;
    return n;
  },
} as const;
