import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { MotifRupture } from '../../core/models/apprentissage-rupture.model';

/**
 * SF-218-24 — Helper partagé pour {@link ApprentissageRuptureSectionComponent}
 * (F-DT-110-apprentissage-rupture) — Travail FR mono-pays.
 *
 * Champs pre-fill (depuis {@link TravailExtractedData}) :
 *  - dateDebutContrat : `aiData.dateEntree` (ISO YYYY-MM-DD) — date de début du
 *    contrat d'apprentissage détectée par l'IA.
 *  - dateRupture : `aiData.dateRuptureContrat` ou, à défaut, `aiData.dateLicenciement`
 *    (ISO YYYY-MM-DD) — date de rupture détectée par l'IA.
 *  - motifRupture : `aiData.apprentissageMotifRupture` (enum) — motif de rupture
 *    d'apprentissage détecté par l'IA.
 *
 * Champs NON pré-remplis :
 *  - auteurRupture / apprentiMajeur : appréciations / saisie avocat.
 *  - `apprentissageRuptureDetectee` est un FLAG de visibilité (déclenche
 *    l'apparition de l'outil via DecisionToolVisibilityService) — ce n'est PAS un
 *    champ du formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 3 champs pre-remplissables (sur 5 saisissables).
 */

const MOTIFS_VALIDES: ReadonlySet<MotifRupture> = new Set<MotifRupture>([
  'ACCORD_PARTIES',
  'FAUTE_GRAVE',
  'FORCE_MAJEURE',
  'INAPTITUDE',
  'EXCLUSION_DEFINITIVE_CFA',
  'SANS_MOTIF',
]);

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

function isoDateOrNull(v: unknown): string | null {
  if (typeof v !== 'string') return null;
  return /^\d{4}-\d{2}-\d{2}/.test(v) ? v.substring(0, 10) : null;
}

export const ApprentissageRupturePrefillRules = {

  computeDateDebutContrat(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull(ai.dateEntree);
  },

  computeDateRupture(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull(ai.dateRuptureContrat) ?? isoDateOrNull(ai.dateLicenciement);
  },

  computeMotifRupture(input: PrefillCountInput): MotifRupture | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const raw = ai.apprentissageMotifRupture;
    return typeof raw === 'string' && MOTIFS_VALIDES.has(raw as MotifRupture)
      ? (raw as MotifRupture)
      : null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateDebutContrat(input) !== null) n++;
    if (this.computeDateRupture(input) !== null) n++;
    if (this.computeMotifRupture(input) !== null) n++;
    return n;
  },
} as const;
