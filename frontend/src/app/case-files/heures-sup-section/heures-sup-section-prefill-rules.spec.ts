import {
  HeuresSupSectionPrefillRules,
  computePrefillCount,
  computeTauxHoraireBrut,
} from './heures-sup-section-prefill-rules';

describe('HeuresSupSectionPrefillRules', () => {
  it('cas 0 — non-FRANCE retourne 0 même avec données', () => {
    expect(
      computePrefillCount({
        aiData: { salaireBrutMensuel: 2500 },
        workspaceCountry: 'BELGIQUE',
      }),
    ).toBe(0);
  });

  it('cas M — salaire seul retourne 1 (tauxHoraire dérivé)', () => {
    expect(
      computePrefillCount({
        aiData: { salaireBrutMensuel: 2500 },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe(1);
  });

  it('cas N — 4 champs nominal FR', () => {
    expect(
      computePrefillCount({
        aiData: {
          salaireBrutMensuel: 2500,
          heuresSupMentionneesDansDossier: {
            totalDeclarees25pct: 10,
            totalDeclarees50pct: 5,
            horsContingent: 2,
          },
        },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe(4);
  });

  it('tauxHoraire arrondi 2 décimales', () => {
    const t = computeTauxHoraireBrut({
      aiData: { salaireBrutMensuel: 2500 },
      workspaceCountry: 'FRANCE',
    });
    expect(t).not.toBeNull();
    // 2500 / 151.67 ≈ 16.4831...
    expect(t).toBeCloseTo(16.48, 2);
  });

  it('expose HeuresSupSectionPrefillRules barrel', () => {
    expect(HeuresSupSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
