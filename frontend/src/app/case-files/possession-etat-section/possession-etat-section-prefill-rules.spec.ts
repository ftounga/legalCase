/**
 * F-236 SF-236-02 — Tests `PossessionEtatPrefillRules`.
 */
import {
  PossessionEtatPrefillRules,
  computePrefillCount,
} from './possession-etat-section-prefill-rules';

describe('PossessionEtatPrefillRules', () => {
  it('cas 0 — flag absent', () => {
    expect(computePrefillCount({})).toBe(0);
    expect(
      computePrefillCount({ aiData: { possessionEtatConforme5AnsDetected: false } }),
    ).toBe(0);
  });

  it('cas M — non applicable (binaire 0 ou 5)', () => {
    // ce flag est un faisceau agrégé : pas de cas intermédiaire
    expect(
      computePrefillCount({ aiData: { possessionEtatConforme5AnsDetected: null } as any }),
    ).toBe(0);
  });

  it('cas N — flag true → 5 champs', () => {
    expect(
      computePrefillCount({ aiData: { possessionEtatConforme5AnsDetected: true } }),
    ).toBe(5);
  });

  it('surface', () => {
    expect(PossessionEtatPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
