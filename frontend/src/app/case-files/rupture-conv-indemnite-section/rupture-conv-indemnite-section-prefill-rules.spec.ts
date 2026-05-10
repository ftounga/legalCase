import {
  RuptureConvIndemniteSectionPrefillRules,
  computePrefillCount,
  computeSalaireMensuel,
} from './rupture-conv-indemnite-section-prefill-rules';

describe('RuptureConvIndemniteSectionPrefillRules', () => {
  describe('computePrefillCount', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('cas M — fallback synthesis sans aiData', () => {
      expect(
        computePrefillCount({
          synthesis: {
            compensationEstimate: { salaireReference: 3000, ancienneteAnnees: 7 },
          },
        }),
      ).toBe(2);
    });

    it('cas N — aiData salaire prioritaire sur synthesis', () => {
      expect(
        computeSalaireMensuel({
          aiData: { salaireBrutMensuel: 2500 },
          synthesis: { compensationEstimate: { salaireReference: 9999 } },
        }),
      ).toBe(2500);
    });

    it('rejette salaire <= 0', () => {
      expect(computePrefillCount({ aiData: { salaireBrutMensuel: 0 } })).toBe(0);
    });
  });

  it('expose RuptureConvIndemniteSectionPrefillRules barrel', () => {
    expect(RuptureConvIndemniteSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
