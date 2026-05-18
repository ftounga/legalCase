import {
  ContributionConjointBeSectionPrefillRules,
  computePrefillCount,
  PREFILL_COUNT_ALWAYS_ZERO,
} from './contribution-conjoint-be-section-prefill-rules';

describe('ContributionConjointBeSectionPrefillRules', () => {
  describe('computePrefillCount — PREFILL_COUNT_ALWAYS_ZERO', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('aiData null retourne 0', () => {
      expect(computePrefillCount({ aiData: null })).toBe(0);
    });

    it('aiData renseigné retourne quand même 0 (aucun flag pension extrait en V1)', () => {
      expect(
        computePrefillCount({
          aiData: { typeDivorce: 'DDI', revenuCreancier: 900 },
        }),
      ).toBe(0);
    });
  });

  it('expose la constante PREFILL_COUNT_ALWAYS_ZERO = true', () => {
    expect(PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
  });

  it('expose le barrel ContributionConjointBeSectionPrefillRules', () => {
    expect(ContributionConjointBeSectionPrefillRules.computePrefillCount).toBe(
      computePrefillCount,
    );
    expect(ContributionConjointBeSectionPrefillRules.PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
  });
});
