import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { MotifDiscrimination } from '../../core/models/action-groupe-discrimination.model';

/**
 * SF-218-10 — Helper partagé pour {@link ActionGroupeDiscriminationSectionComponent}
 * (F-DT-90-action-groupe-discrimination) — Travail FR mono-pays.
 *
 * Champs pre-fill (depuis {@link TravailExtractedData}) :
 *  - motifDiscrimination : `aiData.motifDiscrimination` (best-effort parmi les
 *    critères L. 1132-1). Critère de discrimination prohibé invoqué.
 *  - dateMiseEnDemeure : `aiData.dateMiseEnDemeureDiscrimination` (ISO YYYY-MM-DD).
 *    Point de départ du délai de carence de 6 mois (L. 1134-9).
 *
 * Champs NON pré-remplis :
 *  - typeOrganisation : qualité à agir (syndicat / association agréée) laissée à
 *    l'avocat — saisie déclarative, non factualisable de façon fiable.
 *  - nombrePersonnesConcernees : appréciation du périmètre collectif laissée à
 *    l'avocat.
 *  - objetAction : choix procédural laissé à l'avocat.
 *  - `actionGroupeDiscriminationEnvisagee` est un FLAG de visibilité (déclenche
 *    l'apparition de l'outil via DecisionToolVisibilityService) — ce n'est PAS
 *    un champ du formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 2 champs pre-remplissables (sur 5 saisissables).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

const MOTIFS: readonly MotifDiscrimination[] = [
  'ORIGINE', 'SEXE', 'AGE', 'HANDICAP', 'ETAT_SANTE',
  'GROSSESSE', 'ACTIVITE_SYNDICALE', 'RELIGION', 'ORIENTATION_SEXUELLE', 'AUTRE',
];

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const ActionGroupeDiscriminationPrefillRules = {

  computeMotifDiscrimination(input: PrefillCountInput): MotifDiscrimination | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.motifDiscrimination;
    if (typeof v !== 'string') return null;
    return MOTIFS.includes(v as MotifDiscrimination) ? (v as MotifDiscrimination) : null;
  },

  computeDateMiseEnDemeure(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.dateMiseEnDemeureDiscrimination;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim();
    if (!ISO_DATE_RE.test(trimmed)) return null;
    return trimmed;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeMotifDiscrimination(input) !== null) n++;
    if (this.computeDateMiseEnDemeure(input) !== null) n++;
    return n;
  },
} as const;
