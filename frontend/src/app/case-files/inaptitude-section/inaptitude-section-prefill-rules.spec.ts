import {
  InaptitudeSectionPrefillRules,
  computePrefillCount,
  computeOrigineInaptitude,
  computeReclassementRespecte,
  computeAncienneteAnnees,
} from './inaptitude-section-prefill-rules';

describe('InaptitudeSectionPrefillRules', () => {
  const NOW = new Date('2025-01-01');

  it('cas 0 — input vide retourne 0', () => {
    expect(computePrefillCount({ now: NOW })).toBe(0);
  });

  it('cas M — 2 champs partiels', () => {
    expect(
      computePrefillCount({
        aiData: { salaireBrutMensuel: 2500, dateEntree: '2020-01-01' },
        now: NOW,
      }),
    ).toBe(2);
  });

  it('cas N — 5 champs nominal FR', () => {
    expect(
      computePrefillCount({
        aiData: {
          salaireBrutMensuel: 2500,
          dateEntree: '2020-01-01',
          origineInaptitudePressentie: 'ACCIDENT_TRAVAIL',
          avisMedecinTravailDate: '2024-06-01',
          reclassementRespecteDetected: { reponse: 'OUI' },
        },
        workspaceCountry: 'FRANCE',
        now: NOW,
      }),
    ).toBe(5);
  });

  it('origine skip en Belgique', () => {
    expect(
      computeOrigineInaptitude({
        aiData: { origineInaptitudePressentie: 'ACCIDENT_TRAVAIL' },
        workspaceCountry: 'BELGIQUE',
      }),
    ).toBeNull();
  });

  it('reclassement INCONNU retourne null', () => {
    expect(
      computeReclassementRespecte({
        aiData: { reclassementRespecteDetected: { reponse: 'INCONNU' } },
      }),
    ).toBeNull();
  });

  it('ancienneté future = null', () => {
    expect(
      computeAncienneteAnnees({
        aiData: { dateEntree: '2050-01-01' },
        now: NOW,
      }),
    ).toBeNull();
  });

  it('expose InaptitudeSectionPrefillRules barrel', () => {
    expect(InaptitudeSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
