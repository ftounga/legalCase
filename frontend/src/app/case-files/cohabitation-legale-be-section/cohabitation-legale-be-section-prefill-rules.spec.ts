/**
 * SF-223-01 — Tests Jest du helper `CohabitationLegaleBeSectionPrefillRules`.
 * V1 : computePrefillCount retourne toujours 0 (PREFILL_COUNT_ALWAYS_ZERO).
 */

import { CohabitationLegaleBeSectionPrefillRules } from './cohabitation-legale-be-section-prefill-rules';

describe('CohabitationLegaleBeSectionPrefillRules (SF-223-01)', () => {
  it('computePrefillCount({}) = 0', () => {
    expect(CohabitationLegaleBeSectionPrefillRules.computePrefillCount({})).toBe(0);
  });

  it('computePrefillCount avec aiData / pays BE = 0 (PREFILL_COUNT_ALWAYS_ZERO)', () => {
    expect(CohabitationLegaleBeSectionPrefillRules.computePrefillCount({
      aiData: { cohabitationLegaleBeDetectee: true },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('computePrefillCount avec pays FR = 0', () => {
    expect(CohabitationLegaleBeSectionPrefillRules.computePrefillCount({
      aiData: {},
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });
});
