/**
 * F-236 SF-236-02 — Tests `AdoptionPrefillRules`.
 */
import {
  AdoptionPrefillRules,
  computePrefillCount,
} from './adoption-section-prefill-rules';

describe('AdoptionPrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('cas M — formeAdoption SIMPLE seule', () => {
    expect(
      computePrefillCount({ aiData: { formeAdoptionDemandeeDetected: 'SIMPLE' } } as any),
    ).toBe(1);
  });

  it('rejette forme inconnue', () => {
    expect(
      computePrefillCount({ aiData: { formeAdoptionDemandeeDetected: 'AUTRE' } } as any),
    ).toBe(0);
  });

  it('pupilleEtat=false ne compte pas', () => {
    expect(
      computePrefillCount({ aiData: { pupilleEtatDetected: false } } as any),
    ).toBe(0);
  });

  it('cas N — 5/5 champs', () => {
    expect(
      computePrefillCount({
        aiData: {
          formeAdoptionDemandeeDetected: 'PLENIERE',
          pupilleEtatDetected: true,
          adoptantMarieDetected: true,
          ageAdoptantDetecte: 35,
          ageAdopteDetecte: 8,
        },
      } as any),
    ).toBe(5);
  });

  it('surface', () => {
    expect(AdoptionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
