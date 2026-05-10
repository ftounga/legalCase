import {
  AvantagesConventionnelsBeSectionPrefillRules,
  computePrefillCount,
  computeSalaireMensuelBrutEur,
} from './avantages-conventionnels-be-section-prefill-rules';

describe('AvantagesConventionnelsBeSectionPrefillRules', () => {
  const BE = 'BELGIQUE';

  describe('computePrefillCount', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({ workspaceCountry: BE })).toBe(0);
      expect(computePrefillCount({ workspaceCountry: BE, aiData: null })).toBe(0);
      expect(computePrefillCount({ workspaceCountry: BE, aiData: {} })).toBe(0);
    });

    it('cas M — salaire <= 0 retourne 0', () => {
      expect(
        computePrefillCount({
          workspaceCountry: BE,
          aiData: { salaireBrutMensuel: 0 },
        }),
      ).toBe(0);
      expect(
        computePrefillCount({
          workspaceCountry: BE,
          aiData: { salaireBrutMensuel: -100 },
        }),
      ).toBe(0);
    });

    it('cas N — salaire valide retourne 1', () => {
      expect(
        computePrefillCount({
          workspaceCountry: BE,
          aiData: { salaireBrutMensuel: 2500 },
        }),
      ).toBe(1);
    });
  });

  it('gating BE : workspaceCountry FRANCE retourne 0 même avec aiData complet', () => {
    expect(
      computePrefillCount({
        workspaceCountry: 'FRANCE',
        aiData: { salaireBrutMensuel: 2500 },
      }),
    ).toBe(0);
    expect(
      computeSalaireMensuelBrutEur({
        workspaceCountry: 'FRANCE',
        aiData: { salaireBrutMensuel: 2500 },
      }),
    ).toBeNull();
  });

  it('gating BE : workspaceCountry absent retourne 0', () => {
    expect(
      computePrefillCount({
        aiData: { salaireBrutMensuel: 2500 },
      }),
    ).toBe(0);
  });

  it('expose barrel', () => {
    expect(AvantagesConventionnelsBeSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
