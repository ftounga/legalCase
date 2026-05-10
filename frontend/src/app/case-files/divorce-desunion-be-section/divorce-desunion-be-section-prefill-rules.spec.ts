/**
 * F-236 SF-236-02 — Tests `DivorceDesunionBePrefillRules`.
 */
import {
  DivorceDesunionBePrefillRules,
  computePrefillCount,
} from './divorce-desunion-be-section-prefill-rules';

describe('DivorceDesunionBePrefillRules', () => {
  it('cas 0 — vide retourne 0', () => {
    expect(computePrefillCount({})).toBe(0);
    expect(computePrefillCount({ aiData: null })).toBe(0);
  });

  it('cas M — date seule', () => {
    expect(computePrefillCount({ aiData: { dateSeparation: '2024-08-01' } })).toBe(1);
  });

  it('cas M — chaîne vide rejetée', () => {
    expect(computePrefillCount({ aiData: { dateSeparation: '' } })).toBe(0);
  });

  it('cas N — date + consentement', () => {
    expect(
      computePrefillCount({
        aiData: { dateSeparation: '2024-08-01', separationConsentue: true },
      }),
    ).toBe(2);
  });

  it('separationConsentue=false compte', () => {
    expect(
      computePrefillCount({ aiData: { separationConsentue: false } }),
    ).toBe(1);
  });

  it('surface', () => {
    expect(DivorceDesunionBePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
