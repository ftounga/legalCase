import {
  RegimeCommunauteLegaleBeSectionPrefillRules,
  computePrefillCount,
  PREFILL_COUNT_ALWAYS_ZERO,
} from './regime-communaute-legale-be-section-prefill-rules';

describe('RegimeCommunauteLegaleBeSectionPrefillRules', () => {
  describe('computePrefillCount — PREFILL_COUNT_ALWAYS_ZERO', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('aiData null retourne 0', () => {
      expect(computePrefillCount({ aiData: null })).toBe(0);
    });

    it('aiData renseigné retourne quand même 0 (aucun flag patrimonial extrait en V1)', () => {
      expect(
        computePrefillCount({
          aiData: { dateMariage: '2015-06-20', patrimoineCommun: true },
        }),
      ).toBe(0);
    });
  });

  it('expose la constante PREFILL_COUNT_ALWAYS_ZERO = true', () => {
    expect(PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
  });

  it('expose le barrel RegimeCommunauteLegaleBeSectionPrefillRules', () => {
    expect(RegimeCommunauteLegaleBeSectionPrefillRules.computePrefillCount).toBe(
      computePrefillCount,
    );
    expect(RegimeCommunauteLegaleBeSectionPrefillRules.PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
  });
});
