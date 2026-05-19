/** F-236 SF-236-02 — Tests `AutoriteParentalePrefillRules`. */
import {
  AutoriteParentalePrefillRules,
  computePrefillCount,
  computeRegimeActuel,
} from './autorite-parentale-section-prefill-rules';

describe('AutoriteParentalePrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('cas M — régime + 1 bool', () => {
    expect(
      computePrefillCount({
        aiData: { regimeExerciceActuel: 'conjoint', dangerCaracterise: false },
      } as any),
    ).toBe(2);
  });

  it('rejette régime inconnu', () => {
    expect(
      computeRegimeActuel({ aiData: { regimeExerciceActuel: 'BIZARRE' } } as any),
    ).toBeNull();
  });

  it('âges filtrés (négatifs / > 25 / décimaux exclus) — SF-246-10 plage [0, 25]', () => {
    expect(
      AutoriteParentalePrefillRules.computeAgesEnfants({
        aiData: { agesEnfantsDetectes: [5, -1, 26, 7.5, 12] },
      } as any),
    ).toEqual([5, 12]);
  });

  it('cas N — 5/5', () => {
    expect(
      computePrefillCount({
        aiData: {
          regimeExerciceActuel: 'EXCLUSIF_MERE',
          dangerCaracterise: true,
          consentementAutreParent: false,
          interferenceVieEnfant: true,
          agesEnfantsDetectes: [5, 12],
        },
      } as any),
    ).toBe(5);
  });

  it('surface', () => {
    expect(AutoriteParentalePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
