/**
 * F-236 SF-236-02 — Tests `SeparationCorpsPrefillRules`.
 * SF-246-08 : ajout tests garde BE + `patrimoineCommunEur` (§5.2 > 0 ou null).
 */
import {
  SeparationCorpsPrefillRules,
  computePrefillCount,
  computePatrimoineCommunEur,
} from './separation-corps-section-prefill-rules';

describe('SeparationCorpsPrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('retourne 0 pour workspaceCountry BELGIQUE (garde BE)', () => {
    expect(
      computePrefillCount({
        aiData: { dateSeparation: '2024-06-15', patrimoineCommunEur: 200000 } as any,
        workspaceCountry: 'BELGIQUE',
      }),
    ).toBe(0);
  });

  it('cas M — date seule', () => {
    expect(
      computePrefillCount({ aiData: { dateSeparation: '2024-06-15' } }),
    ).toBe(1);
  });

  it('cas M — date + patrimoineCommun boolean', () => {
    expect(
      computePrefillCount({
        aiData: { dateSeparation: '2024-06-15', patrimoineCommun: true },
      } as any),
    ).toBe(2);
  });

  it('cas N — 3/3 (date + boolean + montant €)', () => {
    expect(
      computePrefillCount({
        aiData: {
          dateSeparation: '2024-06-15',
          patrimoineCommun: true,
          patrimoineCommunEur: 300000,
        } as any,
        workspaceCountry: 'FRANCE',
      }),
    ).toBe(3);
  });

  describe('computePatrimoineCommunEur — invariant §5.2', () => {
    it('retourne null pour 0', () => {
      expect(computePatrimoineCommunEur({ aiData: { patrimoineCommunEur: 0 } as any })).toBeNull();
    });

    it('retourne null pour valeur négative', () => {
      expect(computePatrimoineCommunEur({ aiData: { patrimoineCommunEur: -1 } as any })).toBeNull();
    });

    it('retourne la valeur pour un montant strictement positif', () => {
      expect(computePatrimoineCommunEur({ aiData: { patrimoineCommunEur: 150000 } as any })).toBe(150000);
    });
  });

  it('surface', () => {
    expect(SeparationCorpsPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
