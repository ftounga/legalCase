import {
  IndemniteComparatifSectionPrefillRules,
  computePrefillCount,
  computeTypeRupture,
} from './indemnite-comparatif-section-prefill-rules';

describe('IndemniteComparatifSectionPrefillRules', () => {
  describe('computePrefillCount', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('cas M — 2 champs partiels retourne 2', () => {
      expect(
        computePrefillCount({
          aiData: { salaireBrutMensuel: 2500 },
          synthesis: { compensationEstimate: { ancienneteAnnees: 5 } },
        }),
      ).toBe(2);
    });

    it('cas N — 4 champs nominal FR', () => {
      expect(
        computePrefillCount({
          aiData: { salaireBrutMensuel: 2500 },
          synthesis: {
            compensationEstimate: {
              ancienneteAnnees: 5,
              ancienneteMois: 6,
              typeRupture: 'LICENCIEMENT',
            },
          },
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(4);
    });

    it('rejette typeRupture non supporté pour le pays', () => {
      expect(
        computeTypeRupture({
          synthesis: { compensationEstimate: { typeRupture: 'LICENCIEMENT' } },
          workspaceCountry: 'BELGIQUE',
        }),
      ).toBeNull();
      expect(
        computeTypeRupture({
          synthesis: { compensationEstimate: { typeRupture: 'LICENCIEMENT_ORDINAIRE' } },
          workspaceCountry: 'BELGIQUE',
        }),
      ).toBe('LICENCIEMENT_ORDINAIRE');
    });
  });

  it('expose IndemniteComparatifSectionPrefillRules barrel', () => {
    expect(IndemniteComparatifSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
