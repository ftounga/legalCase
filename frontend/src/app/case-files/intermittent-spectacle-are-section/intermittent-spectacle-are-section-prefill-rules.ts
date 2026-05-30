import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { AnnexeIntermittent } from '../../core/models/intermittent-spectacle-are.model';

/**
 * SF-218-18 — Helper partagé pour {@link IntermittentSpectacleAreSectionComponent}
 * (F-DT-106-intermittent-spectacle-are) — Travail FR mono-pays.
 *
 * Champs pre-fill (depuis {@link TravailExtractedData}) :
 *  - dateFinContrat : `aiData.dateLicenciement` (ISO YYYY-MM-DD) — fin du
 *    dernier contrat, point de départ de la recherche de droits ARE.
 *  - annexe : `aiData.intermittentAnnexe` (enum ANNEXE_8_TECHNICIENS /
 *    ANNEXE_10_ARTISTES) — annexe détectée par l'IA.
 *
 * Champs NON pré-remplis :
 *  - heuresTravaillees12Mois / nombreCachets / heuresFormationDispensees :
 *    volumes propres à l'attestation Unedic / France Travail, non factualisables
 *    de façon fiable depuis les pièces du dossier.
 *  - `statutIntermittentDetecte` est un FLAG de visibilité (déclenche
 *    l'apparition de l'outil via DecisionToolVisibilityService) — ce n'est PAS
 *    un champ du formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 2 champs pre-remplissables (sur 5 saisissables).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;
const ANNEXES: readonly AnnexeIntermittent[] = ['ANNEXE_8_TECHNICIENS', 'ANNEXE_10_ARTISTES'];

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

function isoDateOrNull(v: unknown): string | null {
  if (typeof v !== 'string') return null;
  const trimmed = v.trim();
  if (!ISO_DATE_RE.test(trimmed)) return null;
  return trimmed;
}

export const IntermittentSpectacleArePrefillRules = {

  computeDateFinContrat(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull(ai.dateLicenciement);
  },

  computeAnnexe(input: PrefillCountInput): AnnexeIntermittent | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.intermittentAnnexe;
    return typeof v === 'string' && ANNEXES.includes(v as AnnexeIntermittent)
      ? (v as AnnexeIntermittent)
      : null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateFinContrat(input) !== null) n++;
    if (this.computeAnnexe(input) !== null) n++;
    return n;
  },
} as const;
