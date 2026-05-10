import {
  IndemnitePrecariteCddSectionPrefillRules,
  computePrefillCount,
} from './indemnite-precarite-cdd-section-prefill-rules';

describe('IndemnitePrecariteCddSectionPrefillRules', () => {
  it('cas 0 — non-FRANCE retourne 0', () => {
    expect(
      computePrefillCount({
        aiData: { salaireBrutMensuel: 2500 },
        workspaceCountry: 'BELGIQUE',
      }),
    ).toBe(0);
  });

  it('cas M — input vide retourne 0', () => {
    expect(computePrefillCount({ workspaceCountry: 'FRANCE' })).toBe(0);
  });

  it('cas N — salaire valide en FR retourne 1', () => {
    expect(
      computePrefillCount({
        aiData: { salaireBrutMensuel: 2500 },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe(1);
  });

  it('expose IndemnitePrecariteCddSectionPrefillRules barrel', () => {
    expect(IndemnitePrecariteCddSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
