import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { MandatSyndicalType } from '../../core/models/delegation-syndicale.model';

/**
 * SF-218-34 — Helper partagé pour
 * {@link DelegationSyndicaleSectionComponent}
 * (F-DT-69-delegation-syndicale-protection) — Travail FR mono-pays.
 *
 * Champs pre-fill (depuis {@link TravailExtractedData}) :
 *  - effectif : `aiData.pseNombreSalaries` (entier > 0) — effectif de
 *    l'entreprise détecté par l'IA.
 *  - typeMandat : `aiData.mandatSyndicalType` (DELEGUE_SYNDICAL / RSS) — type de
 *    mandat syndical détecté par l'IA dans les pièces du dossier.
 *
 * Champs NON pré-remplis :
 *  - syndicatRepresentatif / pourcentageScorePersonnel / dateDesignation /
 *    licenciementEnvisage / autorisationInspecteurTravail : éléments de fait
 *    relevant de l'appréciation avocat (représentativité, score électoral,
 *    procédure de licenciement) non factualisables de façon fiable depuis les
 *    seules pièces du dossier.
 *  - `delegationSyndicaleDetectee` est un FLAG de visibilité (déclenche
 *    l'apparition de l'outil via DecisionToolVisibilityService) — ce n'est PAS
 *    un champ du formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 2 champs pre-remplissables (sur 7 saisissables).
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

function positiveIntOrNull(v: unknown): number | null {
  if (typeof v !== 'number' || !Number.isFinite(v) || v <= 0) return null;
  return Math.trunc(v);
}

function mandatTypeOrNull(v: unknown): MandatSyndicalType | null {
  return v === 'DELEGUE_SYNDICAL' || v === 'RSS' ? v : null;
}

export const DelegationSyndicalePrefillRules = {

  computeEffectif(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return positiveIntOrNull(ai.pseNombreSalaries);
  },

  computeTypeMandat(input: PrefillCountInput): MandatSyndicalType | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return mandatTypeOrNull(ai.mandatSyndicalType);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeEffectif(input) !== null) n++;
    if (this.computeTypeMandat(input) !== null) n++;
    return n;
  },
} as const;
