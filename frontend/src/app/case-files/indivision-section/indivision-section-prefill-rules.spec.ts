/**
 * F-236 SF-236-02 — Tests `IndivisionPrefillRules`.
 * SF-246-08 : ajout tests garde BE + `dateSeparation` champ réel.
 */
import {
  IndivisionPrefillRules,
  computePrefillCount,
} from './indivision-section-prefill-rules';

describe('IndivisionPrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('retourne 0 pour workspaceCountry BELGIQUE (garde BE)', () => {
    expect(
      computePrefillCount({
        aiData: { dateSeparation: '2024-03-01', logementCommunDetected: true } as any,
        workspaceCountry: 'BELGIQUE',
      }),
    ).toBe(0);
  });

  it('cas M — date seule', () => {
    expect(computePrefillCount({ aiData: { dateSeparation: '2024-03-01' } })).toBe(1);
  });

  it('logementCommun=false ne compte pas', () => {
    expect(
      computePrefillCount({ aiData: { logementCommunDetected: false } } as any),
    ).toBe(0);
  });

  it('cas N — 2/2', () => {
    expect(
      computePrefillCount({
        aiData: { dateSeparation: '2024-03-01', logementCommunDetected: true },
        workspaceCountry: 'FRANCE',
      } as any),
    ).toBe(2);
  });

  it('surface', () => {
    expect(IndivisionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
