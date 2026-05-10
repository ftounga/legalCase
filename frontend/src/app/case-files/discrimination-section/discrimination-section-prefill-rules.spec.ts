import {
  DiscriminationSectionPrefillRules,
  computePrefillCount,
} from './discrimination-section-prefill-rules';

describe('DiscriminationSectionPrefillRules', () => {
  it('cas 0 — input vide retourne 0', () => {
    expect(computePrefillCount({})).toBe(0);
    expect(computePrefillCount({ aiData: null })).toBe(0);
  });

  it('cas M — salaire <= 0 retourne 0', () => {
    expect(computePrefillCount({ aiData: { salaireBrutMensuel: 0 } })).toBe(0);
  });

  it('cas N — salaire valide retourne 1', () => {
    expect(computePrefillCount({ aiData: { salaireBrutMensuel: 2500 } })).toBe(1);
  });

  it('expose DiscriminationSectionPrefillRules barrel', () => {
    expect(DiscriminationSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
