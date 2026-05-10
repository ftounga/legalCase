import {
  CongesPayesSectionPrefillRules,
  computePrefillCount,
} from './conges-payes-section-prefill-rules';

describe('CongesPayesSectionPrefillRules', () => {
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

  it('cas N — 2 champs retourne 2', () => {
    expect(
      computePrefillCount({
        aiData: { salaireBrutMensuel: 2500, dateLicenciement: '2024-05-01' },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe(2);
  });

  it('expose CongesPayesSectionPrefillRules barrel', () => {
    expect(CongesPayesSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
