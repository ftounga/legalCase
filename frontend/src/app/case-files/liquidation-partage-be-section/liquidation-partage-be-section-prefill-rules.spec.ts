import {
  LiquidationPartageBeSectionPrefillRules,
  computePrefillCount,
  PREFILL_COUNT_ALWAYS_ZERO,
} from './liquidation-partage-be-section-prefill-rules';

describe('LiquidationPartageBeSectionPrefillRules', () => {
  describe('computePrefillCount — PREFILL_COUNT_ALWAYS_ZERO', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('aiData null retourne 0', () => {
      expect(computePrefillCount({ aiData: null })).toBe(0);
    });

    it('aiData renseigné retourne quand même 0 (aucun flag procédural extrait en V1)', () => {
      expect(
        computePrefillCount({
          aiData: { dateNotificationProjet: '2026-04-25', notaireDesigne: true },
        }),
      ).toBe(0);
    });
  });

  it('expose la constante PREFILL_COUNT_ALWAYS_ZERO = true', () => {
    expect(PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
  });

  it('expose le barrel LiquidationPartageBeSectionPrefillRules', () => {
    expect(LiquidationPartageBeSectionPrefillRules.computePrefillCount).toBe(
      computePrefillCount,
    );
    expect(LiquidationPartageBeSectionPrefillRules.PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
  });
});
