import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * F-236 SF-236-02 — Helper partagé pour `Belgian9terSectionComponent`
 * (F-IM-14-9ter-medical-be) — BE only.
 *
 * NOTE F-236 SF-236-04 : gating workspaceCountry === 'BELGIQUE' non
 * appliqué ici (anomalie (E) — rattrapage SF-236-04).
 *
 * 7 champs : dateDebutSymptomes, maladieGraveCertifiee (boolean),
 * soinsNecessairesDisponiblesBe, soinsInaccessiblesPaysOrigine,
 * menaceOrdrePublic, dateDepotDemande, plus alias maladieGrave.
 */

function nonEmptyString(v: unknown): string | null {
  return typeof v === 'string' && v.length > 0 ? v : null;
}
function strictBoolean(v: unknown): boolean | null {
  return typeof v === 'boolean' ? v : null;
}

export const Belgian9terPrefillRules = {
  computeDateDebutSymptomes(input: PrefillCountInput): string | null {
    if (!input.aiData) return null;
    return nonEmptyString((input.aiData as any).dateDebutSymptomes);
  },

  /** Tolérance maladieGraveCertifiee (canonique) puis maladieGrave (alias). */
  computeMaladieGraveCertifiee(input: PrefillCountInput): boolean | null {
    if (!input.aiData) return null;
    const a = input.aiData as any;
    const v = strictBoolean(a.maladieGraveCertifiee);
    if (v !== null) return v;
    return strictBoolean(a.maladieGrave);
  },

  computeSoinsNecessairesDisponiblesBe(input: PrefillCountInput): boolean | null {
    if (!input.aiData) return null;
    return strictBoolean((input.aiData as any).soinsNecessairesDisponiblesBe);
  },

  computeSoinsInaccessiblesPaysOrigine(input: PrefillCountInput): boolean | null {
    if (!input.aiData) return null;
    return strictBoolean((input.aiData as any).soinsInaccessiblesPaysOrigine);
  },

  computeMenaceOrdrePublic(input: PrefillCountInput): boolean | null {
    if (!input.aiData) return null;
    return strictBoolean((input.aiData as any).menaceOrdrePublic);
  },

  computeDateDepotDemande(input: PrefillCountInput): string | null {
    if (!input.aiData) return null;
    return nonEmptyString((input.aiData as any).dateDepotDemande);
  },

  computePrefillCount(input: PrefillCountInput): number {
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
