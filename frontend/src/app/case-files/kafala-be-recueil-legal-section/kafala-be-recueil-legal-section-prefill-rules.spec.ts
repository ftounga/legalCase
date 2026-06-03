/**
 * SF-223-03 — Tests Jest du helper `KafalaBeRecueilLegalSectionPrefillRules`.
 * Pré-fill F-246 : pays d'origine (ISO-2) + date de l'acte (0 à 2 champs), BE only.
 */

import { KafalaBeRecueilLegalSectionPrefillRules } from './kafala-be-recueil-legal-section-prefill-rules';

describe('KafalaBeRecueilLegalSectionPrefillRules (SF-223-03)', () => {
  it('computePrefillCount({}) = 0', () => {
    expect(KafalaBeRecueilLegalSectionPrefillRules.computePrefillCount({})).toBe(0);
  });

  it('aiData null = 0', () => {
    expect(KafalaBeRecueilLegalSectionPrefillRules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('pays seul détecté (BE) = 1', () => {
    expect(KafalaBeRecueilLegalSectionPrefillRules.computePrefillCount({
      aiData: { kafalaPaysOrigineDetecte: 'MA' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
  });

  it('pays + date détectés (BE) = 2', () => {
    expect(KafalaBeRecueilLegalSectionPrefillRules.computePrefillCount({
      aiData: { kafalaPaysOrigineDetecte: 'DZ', kafalaDateActeDetectee: '2024-01-10' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(2);
  });

  it('pays non ISO-2 ignoré = 0', () => {
    expect(KafalaBeRecueilLegalSectionPrefillRules.computePrefillCount({
      aiData: { kafalaPaysOrigineDetecte: 'Maroc' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('date mal formée ignorée = 0', () => {
    expect(KafalaBeRecueilLegalSectionPrefillRules.computePrefillCount({
      aiData: { kafalaDateActeDetectee: '10/01/2024' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('pays FRANCE → 0 même si aiData présent', () => {
    expect(KafalaBeRecueilLegalSectionPrefillRules.computePrefillCount({
      aiData: { kafalaPaysOrigineDetecte: 'MA', kafalaDateActeDetectee: '2024-01-10' },
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });
});
