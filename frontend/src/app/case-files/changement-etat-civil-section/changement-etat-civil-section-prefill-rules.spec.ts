/** F-236 SF-236-02 — Tests `ChangementEtatCivilPrefillRules`. */
import {
  ChangementEtatCivilPrefillRules,
  computePrefillCount,
} from './changement-etat-civil-section-prefill-rules';

describe('ChangementEtatCivilPrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('cas M — type + motif', () => {
    expect(
      computePrefillCount({
        aiData: { typeChangementDetecte: 'NOM', motifChangementDetecte: 'INTERET_LEGITIME' },
      } as any),
    ).toBe(2);
  });

  it('rejette type inconnu', () => {
    expect(
      computePrefillCount({ aiData: { typeChangementDetecte: 'XYZ' } } as any),
    ).toBe(0);
  });

  it('cas N — 5/5', () => {
    expect(
      computePrefillCount({
        aiData: {
          typeChangementDetecte: 'PRENOM',
          motifChangementDetecte: 'MARIAGE',
          dateNaissanceDemandeurDetectee: '1990-04-05',
          majeurDemandeurDetected: true,
          consentementParentalDetected: false,
        },
      } as any),
    ).toBe(5);
  });

  it('surface', () => {
    expect(ChangementEtatCivilPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
