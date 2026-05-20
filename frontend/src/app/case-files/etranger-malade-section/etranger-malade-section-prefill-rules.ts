import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { AvisOFII } from '../../core/models/etranger-malade.model';

/**
 * SF-214-02 — Helper partagé pour {@link EtrangerMaladeSectionComponent}
 * (F-IM-25-etranger-malade-l4259-fr) — FR mono-pays.
 *
 * Champs pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - pathologiePrincipale : `aiData.etrangerMaladePathologie` (texte ≤ 500 car., non vide).
 *  - traitementDisponiblePaysOrigine : `aiData.etrangerMaladeTraitementDisponible` (boolean strict).
 *  - avisOFII : `aiData.etrangerMaladeAvisOFII` (whitelist FAVORABLE/DEFAVORABLE/EN_ATTENTE).
 *  - dateAvisOFII : `aiData.etrangerMalaDateAvisOFII` (ISO YYYY-MM-DD, non future).
 *
 * Champs NON pré-remplis :
 *  - paysOrigine : pas de champ IA dédié, saisi par l'avocat.
 *  - dateDepotDossierOFII : non extraite par la pipeline IA.
 *
 * Total : 4 champs pre-remplissables (sur 6 saisissables).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export const AVIS_OFII_SET: ReadonlySet<AvisOFII> =
  new Set<AvisOFII>(['FAVORABLE', 'DEFAVORABLE', 'EN_ATTENTE']);

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const EtrangerMaladePrefillRules = {
  ISO_DATE_RE,
  AVIS_OFII_SET,

  computePathologiePrincipale(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.etrangerMaladePathologie;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim();
    if (!trimmed) return null;
    if (trimmed.length > 500) return null;
    return trimmed;
  },

  computeTraitementDisponiblePaysOrigine(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.etrangerMaladeTraitementDisponible;
    if (typeof v !== 'boolean') return null;
    return v;
  },

  computeAvisOFII(input: PrefillCountInput): AvisOFII | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.etrangerMaladeAvisOFII;
    if (typeof v !== 'string') return null;
    const upper = v.trim().toUpperCase();
    if (!AVIS_OFII_SET.has(upper as AvisOFII)) return null;
    return upper as AvisOFII;
  },

  computeDateAvisOFII(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.etrangerMalaDateAvisOFII;
    if (typeof v !== 'string') return null;
    if (!ISO_DATE_RE.test(v)) return null;
    if (v > todayIso()) return null;
    return v;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computePathologiePrincipale(input) !== null) n++;
    if (this.computeTraitementDisponiblePaysOrigine(input) !== null) n++;
    if (this.computeAvisOFII(input) !== null) n++;
    if (this.computeDateAvisOFII(input) !== null) n++;
    return n;
  },
} as const;
