import {
  ProcedureNulliteLicenciementSectionPrefillRules,
  computePrefillCount,
  PREFILL_COUNT_ALWAYS_ZERO,
} from './procedure-nullite-licenciement-section-prefill-rules';

describe('ProcedureNulliteLicenciementSectionPrefillRules', () => {
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
          aiData: { salaireBrutMensuel: 2500, dateLicenciement: '2025-01-17' },
        }),
      ).toBe(0);
    });
  });

  it('expose la constante PREFILL_COUNT_ALWAYS_ZERO = true', () => {
    expect(PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
  });

  it('expose le barrel ProcedureNulliteLicenciementSectionPrefillRules', () => {
    expect(ProcedureNulliteLicenciementSectionPrefillRules.computePrefillCount).toBe(
      computePrefillCount,
    );
    expect(ProcedureNulliteLicenciementSectionPrefillRules.PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
  });
});
