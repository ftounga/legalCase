import {
  AutoriteParentaleBeSectionPrefillRules,
  computePrefillCount,
  PREFILL_COUNT_ALWAYS_ZERO,
} from './autorite-parentale-be-section-prefill-rules';

describe('AutoriteParentaleBeSectionPrefillRules', () => {
  describe('computePrefillCount — PREFILL_COUNT_ALWAYS_ZERO', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('aiData null retourne 0', () => {
      expect(computePrefillCount({ aiData: null })).toBe(0);
    });

    it('aiData renseigné retourne quand même 0 (aucun flag AP extrait en V1)', () => {
      expect(
        computePrefillCount({
          aiData: { enfantMineurDetecte: true, autoriteExclusive: true },
        }),
      ).toBe(0);
    });
  });

  it('expose la constante PREFILL_COUNT_ALWAYS_ZERO = true', () => {
    expect(PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
  });

  it('expose le barrel AutoriteParentaleBeSectionPrefillRules', () => {
    expect(AutoriteParentaleBeSectionPrefillRules.computePrefillCount).toBe(
      computePrefillCount,
    );
    expect(AutoriteParentaleBeSectionPrefillRules.PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
  });
});
