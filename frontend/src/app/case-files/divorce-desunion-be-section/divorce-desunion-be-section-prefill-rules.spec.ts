/**
 * F-236 SF-236-02 — Tests `DivorceDesunionBePrefillRules`.
 * F-236 SF-236-04 — Étendus avec gating workspaceCountry === 'BELGIQUE'.
 */
import {
  DivorceDesunionBePrefillRules,
  computePrefillCount,
  computeDateSeparation,
  computeSeparationConsentue,
} from './divorce-desunion-be-section-prefill-rules';

const BE = { workspaceCountry: 'BELGIQUE' as const };

describe('DivorceDesunionBePrefillRules', () => {
  it('cas 0 — vide retourne 0 (BE)', () => {
    expect(computePrefillCount({ ...BE })).toBe(0);
    expect(computePrefillCount({ ...BE, aiData: null })).toBe(0);
  });

  it('cas M — date seule (BE)', () => {
    expect(computePrefillCount({ ...BE, aiData: { dateSeparation: '2024-08-01' } })).toBe(1);
  });

  it('cas M — chaîne vide rejetée (BE)', () => {
    expect(computePrefillCount({ ...BE, aiData: { dateSeparation: '' } })).toBe(0);
  });

  it('cas N — date + consentement (BELGIQUE)', () => {
    expect(
      computePrefillCount({
        ...BE,
        aiData: { dateSeparation: '2024-08-01', separationConsentue: true },
      }),
    ).toBe(2);
  });

  it('separationConsentue=false compte (BE)', () => {
    expect(
      computePrefillCount({ ...BE, aiData: { separationConsentue: false } }),
    ).toBe(1);
  });

  it('surface', () => {
    expect(DivorceDesunionBePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });

  // F-236 SF-236-04 — gating BE-only.
  it('returns 0 when workspaceCountry === FRANCE (gating BE-only)', () => {
    const input = {
      aiData: { dateSeparation: '2024-08-01', separationConsentue: true },
      workspaceCountry: 'FRANCE',
    };
    expect(computePrefillCount(input)).toBe(0);
    expect(computeDateSeparation(input)).toBeNull();
    expect(computeSeparationConsentue(input)).toBeNull();
  });

  it('defaults to FRANCE gating (returns 0) when workspaceCountry omitted', () => {
    expect(computePrefillCount({ aiData: { dateSeparation: '2024-08-01' } })).toBe(0);
  });
});
