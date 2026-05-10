import {
  FinMissionInterimSectionPrefillRules,
  computePrefillCount,
} from './fin-mission-interim-section-prefill-rules';

describe('FinMissionInterimSectionPrefillRules', () => {
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

  it('cas N — salaire valide retourne 1', () => {
    expect(
      computePrefillCount({
        aiData: { salaireBrutMensuel: 2500 },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe(1);
  });

  it('expose FinMissionInterimSectionPrefillRules barrel', () => {
    expect(FinMissionInterimSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
