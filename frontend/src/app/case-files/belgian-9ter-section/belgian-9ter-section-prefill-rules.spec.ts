import { Belgian9terPrefillRules as Rules } from './belgian-9ter-section-prefill-rules';

/**
 * F-236 SF-236-04 — Tests étendus avec gating workspaceCountry === 'BELGIQUE'.
 */
const BE = { workspaceCountry: 'BELGIQUE' as const };

describe('Belgian9terPrefillRules', () => {
  it('returns 0 when no aiData (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE })).toBe(0);
    expect(Rules.computePrefillCount({ ...BE, aiData: {} })).toBe(0);
  });

  it('returns 0 for non-string dates and non-boolean toggles (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { dateDebutSymptomes: null } })).toBe(0);
    expect(Rules.computePrefillCount({ ...BE, aiData: { maladieGraveCertifiee: 'oui' } })).toBe(0);
  });

  it('returns 1 when only dateDebutSymptomes set (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { dateDebutSymptomes: '2025-01-01' } })).toBe(1);
  });

  it('returns 1 when only maladieGrave alias set (tolérance) (BE)', () => {
    expect(Rules.computeMaladieGraveCertifiee({ ...BE, aiData: { maladieGrave: true } })).toBe(true);
    expect(Rules.computePrefillCount({ ...BE, aiData: { maladieGrave: true } })).toBe(1);
  });

  it('returns 1 when only menaceOrdrePublic=false (BE)', () => {
    expect(Rules.computePrefillCount({ ...BE, aiData: { menaceOrdrePublic: false } })).toBe(1);
  });

  it('returns N=6 when all six sources alimente (BELGIQUE)', () => {
    const input = {
      ...BE,
      aiData: {
        dateDebutSymptomes: '2025-01-01',
        maladieGraveCertifiee: true,
        soinsNecessairesDisponiblesBe: false,
        soinsInaccessiblesPaysOrigine: true,
        menaceOrdrePublic: false,
        dateDepotDemande: '2026-04-01',
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(6);
  });

  it('canonical maladieGraveCertifiee wins over alias maladieGrave (BE)', () => {
    const input = {
      ...BE,
      aiData: { maladieGraveCertifiee: false, maladieGrave: true },
    };
    expect(Rules.computeMaladieGraveCertifiee(input)).toBe(false);
  });

  // F-236 SF-236-04 — gating BE-only.
  it('returns 0 when workspaceCountry === FRANCE (gating BE-only)', () => {
    const input = {
      aiData: {
        dateDebutSymptomes: '2025-01-01',
        maladieGraveCertifiee: true,
        soinsNecessairesDisponiblesBe: false,
        soinsInaccessiblesPaysOrigine: true,
        menaceOrdrePublic: false,
        dateDepotDemande: '2026-04-01',
      },
      workspaceCountry: 'FRANCE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeDateDebutSymptomes(input)).toBeNull();
    expect(Rules.computeMaladieGraveCertifiee(input)).toBeNull();
    expect(Rules.computeSoinsNecessairesDisponiblesBe(input)).toBeNull();
    expect(Rules.computeSoinsInaccessiblesPaysOrigine(input)).toBeNull();
    expect(Rules.computeMenaceOrdrePublic(input)).toBeNull();
    expect(Rules.computeDateDepotDemande(input)).toBeNull();
  });

  it('defaults to FRANCE gating (returns 0) when workspaceCountry omitted', () => {
    expect(Rules.computePrefillCount({ aiData: { dateDebutSymptomes: '2025-01-01' } })).toBe(0);
  });
});
