/** F-236 SF-236-02 — Tests `ChangementResidencePrefillRules`. */
import {
  ChangementResidencePrefillRules,
  computePrefillCount,
} from './changement-residence-section-prefill-rules';

describe('ChangementResidencePrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('cas M — 2/5', () => {
    expect(
      computePrefillCount({
        aiData: { raisonChangementDetectee: 'travail', consentementAutreParent: true },
      } as any),
    ).toBe(2);
  });

  it('rejette raison + mode inconnus', () => {
    expect(
      computePrefillCount({
        aiData: { raisonChangementDetectee: 'BIZARRE', modeResidenceActuel: 'XYZ' },
      } as any),
    ).toBe(0);
  });

  it('cas N — 5/5', () => {
    expect(
      computePrefillCount({
        aiData: {
          raisonChangementDetectee: 'TRAVAIL',
          consentementAutreParent: true,
          informePrealablement: false,
          modeResidenceActuel: 'ALTERNEE',
          ageEnfants: [3, 7],
        },
      } as any),
    ).toBe(5);
  });

  it('surface', () => {
    expect(ChangementResidencePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
