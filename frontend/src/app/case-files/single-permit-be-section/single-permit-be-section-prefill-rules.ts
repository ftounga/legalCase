import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  SinglePermitMotif,
  SinglePermitRegion,
  SinglePermitTypeActivite,
} from '../../core/models/single-permit-be.model';

/**
 * SF-215-02 — Helper partagé pour {@link SinglePermitBeSectionComponent}
 * (F-IM-25-single-permit-be) — BE mono-pays.
 *
 * Champs pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - dateDebutPermit    : `aiData.singlePermitDateDebut` (ISO YYYY-MM-DD)
 *  - dateFinPermit      : `aiData.singlePermitDateFin` (ISO YYYY-MM-DD)
 *  - regionInstruction  : `aiData.singlePermitRegion` (WALLONIE/FLANDRE/BRUXELLES)
 *  - typeActivite       : `aiData.singlePermitTypeActivite`
 *      (SALARIE/STAGIAIRE/DETACHE/CHERCHEUR/ETUDIANT)
 *  - motifDemande       : `aiData.singlePermitMotif` (NOUVEAU/RENOUVELLEMENT)
 *
 * Total : 5 champs pre-remplissables (sur 5 saisissables — tous les champs
 * formulaire sont susceptibles d'être pré-remplis par l'IA, conformément à
 * `feedback_decision_tools_all_fields_prefilled`).
 *
 * Gate BELGIQUE : `workspaceCountry === 'BELGIQUE'` (strict). Sur workspace FR
 * tout retourne null/0 (le composant affiche une bannière info dans ce cas).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export const SINGLE_PERMIT_REGION_SET: ReadonlySet<SinglePermitRegion> =
  new Set<SinglePermitRegion>(['WALLONIE', 'FLANDRE', 'BRUXELLES']);

export const SINGLE_PERMIT_TYPE_SET: ReadonlySet<SinglePermitTypeActivite> =
  new Set<SinglePermitTypeActivite>([
    'SALARIE',
    'STAGIAIRE',
    'DETACHE',
    'CHERCHEUR',
    'ETUDIANT',
  ]);

export const SINGLE_PERMIT_MOTIF_SET: ReadonlySet<SinglePermitMotif> =
  new Set<SinglePermitMotif>(['NOUVEAU', 'RENOUVELLEMENT']);

function isBelgique(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'BELGIQUE';
}

export const SinglePermitBePrefillRules = {
  ISO_DATE_RE,
  SINGLE_PERMIT_REGION_SET,
  SINGLE_PERMIT_TYPE_SET,
  SINGLE_PERMIT_MOTIF_SET,

  computeDateDebutPermit(input: PrefillCountInput): string | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.singlePermitDateDebut;
    if (typeof v !== 'string') return null;
    if (!ISO_DATE_RE.test(v)) return null;
    return v;
  },

  computeDateFinPermit(input: PrefillCountInput): string | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.singlePermitDateFin;
    if (typeof v !== 'string') return null;
    if (!ISO_DATE_RE.test(v)) return null;
    return v;
  },

  computeRegionInstruction(input: PrefillCountInput): SinglePermitRegion | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.singlePermitRegion;
    if (typeof v !== 'string') return null;
    const upper = v.trim().toUpperCase();
    if (!SINGLE_PERMIT_REGION_SET.has(upper as SinglePermitRegion)) return null;
    return upper as SinglePermitRegion;
  },

  computeTypeActivite(input: PrefillCountInput): SinglePermitTypeActivite | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.singlePermitTypeActivite;
    if (typeof v !== 'string') return null;
    const upper = v.trim().toUpperCase();
    if (!SINGLE_PERMIT_TYPE_SET.has(upper as SinglePermitTypeActivite)) return null;
    return upper as SinglePermitTypeActivite;
  },

  computeMotifDemande(input: PrefillCountInput): SinglePermitMotif | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.singlePermitMotif;
    if (typeof v !== 'string') return null;
    const upper = v.trim().toUpperCase();
    if (!SINGLE_PERMIT_MOTIF_SET.has(upper as SinglePermitMotif)) return null;
    return upper as SinglePermitMotif;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isBelgique(input)) return 0;
    let n = 0;
    if (this.computeDateDebutPermit(input) !== null) n++;
    if (this.computeDateFinPermit(input) !== null) n++;
    if (this.computeRegionInstruction(input) !== null) n++;
    if (this.computeTypeActivite(input) !== null) n++;
    if (this.computeMotifDemande(input) !== null) n++;
    return n;
  },
} as const;
