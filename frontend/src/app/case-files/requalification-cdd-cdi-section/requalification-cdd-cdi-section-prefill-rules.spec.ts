import {
  RequalificationCddCdiSectionPrefillRules,
  computePrefillCount,
} from './requalification-cdd-cdi-section-prefill-rules';

describe('RequalificationCddCdiSectionPrefillRules', () => {
  it('cas 0 — non-FRANCE retourne 0', () => {
    expect(computePrefillCount({ aiData: { salaireBrutMensuel: 2500 }, workspaceCountry: 'BELGIQUE' })).toBe(0);
  });
  it('cas M — input vide retourne 0', () => {
    expect(computePrefillCount({ workspaceCountry: 'FRANCE' })).toBe(0);
  });
  it('cas N — salaire valide retourne 1', () => {
    expect(computePrefillCount({ aiData: { salaireBrutMensuel: 2500 }, workspaceCountry: 'FRANCE' })).toBe(1);
  });
  it('expose barrel', () => {
    expect(RequalificationCddCdiSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
