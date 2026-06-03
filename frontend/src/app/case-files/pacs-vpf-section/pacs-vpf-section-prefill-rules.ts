import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { IntensiteCommunauteVie } from '../../core/models/pacs-vpf.model';

/**
 * SF-220-04 — Helper partagé pour {@link PacsVpfSectionComponent}
 * (F-IM-50-pacs-vpf-fr) — FR mono-pays.
 *
 * Champs pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - pacsConclu : `aiData.pacsConclu` (booléen).
 *  - datePacs : `aiData.pacsDate` (chaîne ISO yyyy-MM-dd).
 *  - dureeVieCommuneMois : `aiData.pacsDureeVieCommune` (entier ≥ 0).
 *  - intensiteCommunauteVie : `aiData.pacsIntensiteCommunauteVie` (whitelist 4 codes).
 *
 * Champs NON pré-remplis (non extraits par le pipeline IA — non factualisables
 * de façon fiable depuis les pièces, saisie avocat) : partenaireStatut,
 * autresLiensPrivesFamiliaux.
 *
 * Le flag `pacsDetecte` est le pivot de VISIBILITÉ (CONTEXTUAL), pas un champ
 * saisissable du formulaire — il ne compte pas dans le pré-fill.
 *
 * Total : 4 champs pre-remplissables.
 */

const INTENSITE_CODES: ReadonlySet<string> = new Set<string>([
  'FORTE',
  'MOYENNE',
  'FAIBLE',
  'NON_ETABLIE',
]);

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const PacsVpfPrefillRules = {

  computePacsConclu(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.pacsConclu;
    if (typeof v !== 'boolean') return null;
    return v;
  },

  computeDatePacs(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.pacsDate;
    if (typeof v !== 'string' || !ISO_DATE.test(v)) return null;
    return v;
  },

  computeDureeVieCommuneMois(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.pacsDureeVieCommune;
    if (typeof v !== 'number' || !Number.isFinite(v) || v < 0) return null;
    return v;
  },

  computeIntensiteCommunauteVie(input: PrefillCountInput): IntensiteCommunauteVie | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.pacsIntensiteCommunauteVie;
    if (typeof v !== 'string' || !INTENSITE_CODES.has(v)) return null;
    return v as IntensiteCommunauteVie;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computePacsConclu(input) !== null) n++;
    if (this.computeDatePacs(input) !== null) n++;
    if (this.computeDureeVieCommuneMois(input) !== null) n++;
    if (this.computeIntensiteCommunauteVie(input) !== null) n++;
    return n;
  },
} as const;
