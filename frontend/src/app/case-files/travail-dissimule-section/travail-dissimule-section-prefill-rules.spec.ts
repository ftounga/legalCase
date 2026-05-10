import {
  TravailDissimuleSectionPrefillRules,
  computePrefillCount,
} from './travail-dissimule-section-prefill-rules';

describe('TravailDissimuleSectionPrefillRules', () => {
  it('cas 0 — input vide retourne 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('cas M — salaire <= 0 retourne 0', () => {
    expect(computePrefillCount({ aiData: { salaireBrutMensuel: 0 } })).toBe(0);
  });

  it('cas N — salaire valide retourne 1', () => {
    expect(computePrefillCount({ aiData: { salaireBrutMensuel: 2500 } })).toBe(1);
  });

  it('expose TravailDissimuleSectionPrefillRules barrel', () => {
    expect(TravailDissimuleSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
