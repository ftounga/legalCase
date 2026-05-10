import { AtMpSectionPrefillRules, computePrefillCount } from './at-mp-section-prefill-rules';

describe('AtMpSectionPrefillRules', () => {
  describe('computePrefillCount', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
      expect(computePrefillCount({ aiData: null })).toBe(0);
      expect(computePrefillCount({ aiData: {} })).toBe(0);
    });

    it('cas M — dateLicenciement absent / vide / non-string retourne 0', () => {
      expect(computePrefillCount({ aiData: { dateLicenciement: null } })).toBe(0);
      expect(computePrefillCount({ aiData: { dateLicenciement: '' } })).toBe(0);
      expect(computePrefillCount({ aiData: { dateLicenciement: undefined } })).toBe(0);
    });

    it('cas N — dateLicenciement valide retourne 1', () => {
      expect(
        computePrefillCount({ aiData: { dateLicenciement: '2024-05-12' } }),
      ).toBe(1);
    });
  });

  it('expose AtMpSectionPrefillRules barrel', () => {
    expect(AtMpSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
