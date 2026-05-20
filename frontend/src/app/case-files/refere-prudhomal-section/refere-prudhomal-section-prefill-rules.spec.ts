import {
  ReferePrudhomalSectionPrefillRules,
  computeMontantProvision,
  computePrefillCount,
  monthsBetween,
  computeNatureCreance,
} from './refere-prudhomal-section-prefill-rules';

describe('ReferePrudhomalSectionPrefillRules', () => {
  const TODAY = '2025-01-15';
  const FR = 'FRANCE';

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
        workspaceCountry: FR,
        todayIso: TODAY,
      }),
    ).toBe(1);
  });

  it('cas N — 3 champs nominaux (date + anciennete + natureCreance)', () => {
    expect(
      computePrefillCount({
        aiData: {
          dateLicenciement: '2024-05-01',
          dateEntree: '2020-01-01',
          heuresSupMentionneesDansDossier: { totalDeclarees25pct: 10 },
        },
        workspaceCountry: FR,
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
        workspaceCountry: FR,
      }),
    ).toBeNull();
  });

  // SF-246-21 — computeMontantProvision
  describe('computeMontantProvision (SF-246-21)', () => {
    it('non-FRANCE → null', () => {
      expect(computeMontantProvision({ aiData: { refereMontantProvision: 8000 }, workspaceCountry: 'BELGIQUE' })).toBeNull();
    });

    it('montant absent → null', () => {
      expect(computeMontantProvision({ aiData: {}, workspaceCountry: FR })).toBeNull();
    });

    it('montant ≤ 0 → null', () => {
      expect(computeMontantProvision({ aiData: { refereMontantProvision: 0 }, workspaceCountry: FR })).toBeNull();
    });

    it('montant positif → retourne montant', () => {
      expect(computeMontantProvision({ aiData: { refereMontantProvision: 8000 }, workspaceCountry: FR })).toBe(8000);
    });
  });

  // count max = 4 quand tous les champs présents
  it('tous les champs remplis → count = 4', () => {
    expect(computePrefillCount({
      aiData: {
        dateLicenciement: '2024-05-01',
        dateEntree: '2020-01-01',
        heuresSupMentionneesDansDossier: { totalDeclarees25pct: 10 },
        refereMontantProvision: 5000,
      },
      workspaceCountry: FR,
      todayIso: TODAY,
    })).toBe(4);
  });

  it('expose ReferePrudhomalSectionPrefillRules barrel', () => {
    expect(ReferePrudhomalSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
