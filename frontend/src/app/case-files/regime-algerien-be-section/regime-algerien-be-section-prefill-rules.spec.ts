/**
 * SF-223-05 — Tests Jest du helper `RegimeAlgerienBeSectionPrefillRules`.
 * Pré-fill F-246 : nature de l'acte + date + montant de la dot (0 à 3 champs),
 * BE only.
 */

import { RegimeAlgerienBeSectionPrefillRules } from './regime-algerien-be-section-prefill-rules';

describe('RegimeAlgerienBeSectionPrefillRules (SF-223-05)', () => {
  it('computePrefillCount({}) = 0', () => {
    expect(RegimeAlgerienBeSectionPrefillRules.computePrefillCount({})).toBe(0);
  });

  it('aiData null = 0', () => {
    expect(RegimeAlgerienBeSectionPrefillRules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('nature seule détectée (BE) = 1', () => {
    expect(RegimeAlgerienBeSectionPrefillRules.computePrefillCount({
      aiData: { regimeAlgerienBeNatureActeDetecte: 'MARIAGE_ALGERIEN' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
  });

  it('nature + date + montant détectés (BE) = 3', () => {
    expect(RegimeAlgerienBeSectionPrefillRules.computePrefillCount({
      aiData: {
        regimeAlgerienBeNatureActeDetecte: 'DOT_MAHR',
        regimeAlgerienBeDateActeDetectee: '2024-01-15',
        regimeAlgerienBeMontantDotDetecte: '5000',
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(3);
  });

  it('nature hors whitelist ignorée = 0', () => {
    expect(RegimeAlgerienBeSectionPrefillRules.computePrefillCount({
      aiData: { regimeAlgerienBeNatureActeDetecte: 'MARIAGE_TUNISIEN' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('date mal formée ignorée = 0', () => {
    expect(RegimeAlgerienBeSectionPrefillRules.computePrefillCount({
      aiData: { regimeAlgerienBeDateActeDetectee: '15/01/2024' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('montant non numérique / négatif ignoré = 0', () => {
    expect(RegimeAlgerienBeSectionPrefillRules.computePrefillCount({
      aiData: { regimeAlgerienBeMontantDotDetecte: 'beaucoup' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
    expect(RegimeAlgerienBeSectionPrefillRules.computePrefillCount({
      aiData: { regimeAlgerienBeMontantDotDetecte: '-10' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('pays FRANCE → 0 même si aiData présent', () => {
    expect(RegimeAlgerienBeSectionPrefillRules.computePrefillCount({
      aiData: {
        regimeAlgerienBeNatureActeDetecte: 'MARIAGE_ALGERIEN',
        regimeAlgerienBeDateActeDetectee: '2024-01-15',
        regimeAlgerienBeMontantDotDetecte: '5000',
      },
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });
});
