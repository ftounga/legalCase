import {
  ReferePrudhomalSectionPrefillRules,
  computePrefillCount,
  monthsBetween,
  computeNatureCreance,
} from './refere-prudhomal-section-prefill-rules';

describe('ReferePrudhomalSectionPrefillRules', () => {
  const TODAY = '2025-01-15';

  it('cas 0 — non-FRANCE retourne 0', () => {
    expect(
      computePrefillCount({
        aiData: { dateLicenciement: '2024-05-01' },
        workspaceCountry: 'BELGIQUE',
        todayIso: TODAY,
      }),
    ).toBe(0);
  });

  it('cas M — date seule retourne 1', () => {
    expect(
      computePrefillCount({
        aiData: { dateLicenciement: '2024-05-01' },
        workspaceCountry: 'FRANCE',
        todayIso: TODAY,
      }),
    ).toBe(1);
  });

  it('cas N — 3 champs nominal', () => {
    expect(
      computePrefillCount({
        aiData: {
          dateLicenciement: '2024-05-01',
          dateEntree: '2020-01-01',
          heuresSupMentionneesDansDossier: { totalDeclarees25pct: 10 },
        },
        workspaceCountry: 'FRANCE',
        todayIso: TODAY,
      }),
    ).toBe(3);
  });

  it('monthsBetween — calcul de base', () => {
    expect(monthsBetween('2024-01-01', '2024-07-01')).toBe(6);
    expect(monthsBetween('2024-07-01', '2024-01-01')).toBe(0); // end<start → 0
  });

  it('natureCreance — 0 si heuresSup tous à 0', () => {
    expect(
      computeNatureCreance({
        aiData: { heuresSupMentionneesDansDossier: { totalDeclarees25pct: 0 } },
        workspaceCountry: 'FRANCE',
      }),
    ).toBeNull();
  });

  it('expose ReferePrudhomalSectionPrefillRules barrel', () => {
    expect(ReferePrudhomalSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
