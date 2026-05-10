import {
  IndemnitePreavisSectionPrefillRules,
  computePrefillCount,
} from './indemnite-preavis-section-prefill-rules';

describe('IndemnitePreavisSectionPrefillRules', () => {
  it('cas 0 — non-FRANCE retourne 0', () => {
    expect(
      computePrefillCount({
        aiData: { salaireBrutMensuel: 2500, dateLicenciement: '2024-05-01' },
        workspaceCountry: 'BELGIQUE',
      }),
    ).toBe(0);
  });

  it('cas M — salaire seul retourne 1', () => {
    expect(
      computePrefillCount({
        aiData: { salaireBrutMensuel: 2500 },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe(1);
  });

  it('cas N — 2 champs (convention non comptée)', () => {
    expect(
      computePrefillCount({
        aiData: {
          salaireBrutMensuel: 2500,
          dateLicenciement: '2024-05-01',
          conventionCollective: '3043',
        },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe(2);
  });

  it('expose IndemnitePreavisSectionPrefillRules barrel', () => {
    expect(IndemnitePreavisSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
