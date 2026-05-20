import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * F-236 SF-236-02 — Helper partagé pour `Belgian9terSectionComponent`
 * (F-IM-14-9ter-medical-be) — BE only.
 *
 * F-236 SF-236-04 : gating workspaceCountry === 'BELGIQUE' appliqué
 * dans `compute*` (early return null) + dans `computePrefillCount`
 * (early return 0). Pattern miroir de `ImmigrationWorkRightPrefillRules`.
 *
 * SF-246-20 : dateDebutSymptomes lit le champ typé be9terDateDebutSymptomes
 * (ISO strict, non-future). Suppression du cast `as any` sur l'ancien champ
 * aspirationnel dateDebutSymptomes.
 * Les champs boolean aspirationnels (maladieGraveCertifiee, soins*, menace*)
 * restent hors périmètre SF-246-20 — ils gardent les casts `as any`.
 */

/** ISO date strict YYYY-MM-DD. */
const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function strictBoolean(v: unknown): boolean | null {
  return typeof v === 'boolean' ? v : null;
}

/** F-236 SF-236-04 : gating BE-only. */
function isBelgium(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'BELGIQUE';
}

export const Belgian9terPrefillRules = {
  /**
   * SF-246-20 : lit le champ typé be9terDateDebutSymptomes.
   * Ancienne clé aspirationnelle dateDebutSymptomes n'est plus lue.
   */
  computeDateDebutSymptomes(input: PrefillCountInput): string | null {
    if (!isBelgium(input)) return null;
    const ai = input.aiData as ImmigrationExtractedData | null | undefined;
    if (!ai) return null;
    const v = ai.be9terDateDebutSymptomes;
    if (typeof v !== 'string' || !ISO_DATE_RE.test(v)) return null;
    return v;
  },

  /** Tolérance maladieGraveCertifiee (canonique) puis maladieGrave (alias). */
  computeMaladieGraveCertifiee(input: PrefillCountInput): boolean | null {
    if (!isBelgium(input)) return null;
    if (!input.aiData) return null;
    const a = input.aiData as any;
    const v = strictBoolean(a.maladieGraveCertifiee);
    if (v !== null) return v;
    return strictBoolean(a.maladieGrave);
  },

  computeSoinsNecessairesDisponiblesBe(input: PrefillCountInput): boolean | null {
    if (!isBelgium(input)) return null;
    if (!input.aiData) return null;
    return strictBoolean((input.aiData as any).soinsNecessairesDisponiblesBe);
  },

  computeSoinsInaccessiblesPaysOrigine(input: PrefillCountInput): boolean | null {
    if (!isBelgium(input)) return null;
    if (!input.aiData) return null;
    return strictBoolean((input.aiData as any).soinsInaccessiblesPaysOrigine);
  },

  computeMenaceOrdrePublic(input: PrefillCountInput): boolean | null {
    if (!isBelgium(input)) return null;
    if (!input.aiData) return null;
    return strictBoolean((input.aiData as any).menaceOrdrePublic);
  },

  computeDateDepotDemande(input: PrefillCountInput): string | null {
    if (!isBelgium(input)) return null;
    if (!input.aiData) return null;
    // Utilise dateDepotProcedure (champ typé). Alias aspirationnel dateDepotDemande supprimé.
    const ai = input.aiData as ImmigrationExtractedData | null | undefined;
    if (!ai) return null;
    const v = ai.dateDepotProcedure ?? (ai as any).dateDepotDemande;
    if (typeof v !== 'string' || v.length === 0) return null;
    return v;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isBelgium(input)) return 0;
    let n = 0;
    if (this.computeDateDebutSymptomes(input) !== null) n++;
    if (this.computeMaladieGraveCertifiee(input) !== null) n++;
    if (this.computeSoinsNecessairesDisponiblesBe(input) !== null) n++;
    if (this.computeSoinsInaccessiblesPaysOrigine(input) !== null) n++;
    if (this.computeMenaceOrdrePublic(input) !== null) n++;
    if (this.computeDateDepotDemande(input) !== null) n++;
    return n;
  },
} as const;
