import { Belgian9terPrefillRules as Rules } from './belgian-9ter-section-prefill-rules';

describe('Belgian9terPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 for non-string dates and non-boolean toggles', () => {
    expect(Rules.computePrefillCount({ aiData: { dateDebutSymptomes: null } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { maladieGraveCertifiee: 'oui' } })).toBe(0);
  });

  it('returns 1 when only dateDebutSymptomes set', () => {
    expect(Rules.computePrefillCount({ aiData: { dateDebutSymptomes: '2025-01-01' } })).toBe(1);
  });

  it('returns 1 when only maladieGrave alias set (tolérance)', () => {
    expect(Rules.computeMaladieGraveCertifiee({ aiData: { maladieGrave: true } })).toBe(true);
    expect(Rules.computePrefillCount({ aiData: { maladieGrave: true } })).toBe(1);
  });

  it('returns 1 when only menaceOrdrePublic=false', () => {
    expect(Rules.computePrefillCount({ aiData: { menaceOrdrePublic: false } })).toBe(1);
  });

  it('returns N=6 when all six sources alimente', () => {
    const input = {
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

  it('canonical maladieGraveCertifiee wins over alias maladieGrave', () => {
    const input = {
      aiData: { maladieGraveCertifiee: false, maladieGrave: true },
    };
    expect(Rules.computeMaladieGraveCertifiee(input)).toBe(false);
  });
});
