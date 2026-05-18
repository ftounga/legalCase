import {
  ContributionAlimentaireEnfantsBeSectionPrefillRules,
  computePrefillCount,
  PREFILL_COUNT_ALWAYS_ZERO,
} from './contribution-alimentaire-enfants-be-section-prefill-rules';

describe('ContributionAlimentaireEnfantsBeSectionPrefillRules', () => {
  describe('computePrefillCount — PREFILL_COUNT_ALWAYS_ZERO', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('aiData null retourne 0', () => {
      expect(computePrefillCount({ aiData: null })).toBe(0);
    });

    it('aiData renseigné retourne quand même 0 (aucun flag contribution extrait en V1)', () => {
      expect(
        computePrefillCount({
          aiData: { nombreEnfants: 2, revenuParent1: 2800 },
        }),
      ).toBe(0);
    });
  });

  it('expose la constante PREFILL_COUNT_ALWAYS_ZERO = true', () => {
    expect(PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
  });

  it('expose le barrel ContributionAlimentaireEnfantsBeSectionPrefillRules', () => {
    expect(ContributionAlimentaireEnfantsBeSectionPrefillRules.computePrefillCount).toBe(
      computePrefillCount,
    );
    expect(ContributionAlimentaireEnfantsBeSectionPrefillRules.PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
  });
});
