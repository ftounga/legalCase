import { PseSectionPrefillRules, computePrefillCount } from './pse-section-prefill-rules';

describe('PseSectionPrefillRules', () => {
  describe('computePrefillCount', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
      expect(computePrefillCount({ aiData: null })).toBe(0);
      expect(computePrefillCount({ aiData: {} })).toBe(0);
    });

    it('cas M — dateLicenciement invalide retourne 0', () => {
      expect(computePrefillCount({ aiData: { dateLicenciement: null } })).toBe(0);
      expect(computePrefillCount({ aiData: { dateLicenciement: '' } })).toBe(0);
    });

    it('cas N — dateLicenciement valide retourne 1', () => {
      expect(
        computePrefillCount({ aiData: { dateLicenciement: '2024-09-30' } }),
      ).toBe(1);
    });
  });

  it('expose PseSectionPrefillRules barrel', () => {
    expect(PseSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
