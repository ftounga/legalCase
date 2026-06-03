/**
 * SF-223-02 — Tests Jest du helper `AdoptionBeSectionPrefillRules`.
 * Pré-fill F-246 : type d'adoption + agrément mentionné (0 à 2 champs), BE only.
 */

import { AdoptionBeSectionPrefillRules } from './adoption-be-section-prefill-rules';

describe('AdoptionBeSectionPrefillRules (SF-223-02)', () => {
  it('computePrefillCount({}) = 0', () => {
    expect(AdoptionBeSectionPrefillRules.computePrefillCount({})).toBe(0);
  });

  it('aiData null = 0', () => {
    expect(AdoptionBeSectionPrefillRules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('type seul détecté (BE) = 1', () => {
    expect(AdoptionBeSectionPrefillRules.computePrefillCount({
      aiData: { adoptionBeTypeDetecte: 'PLENIERE' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
  });

  it('type + agrément détectés (BE) = 2', () => {
    expect(AdoptionBeSectionPrefillRules.computePrefillCount({
      aiData: { adoptionBeTypeDetecte: 'CO_PARENTALE', adoptionBeAgrementMentionneBeDetecte: true },
      workspaceCountry: 'BELGIQUE',
    })).toBe(2);
  });

  it('agrément false compte aussi (booléen présent) = 1', () => {
    expect(AdoptionBeSectionPrefillRules.computePrefillCount({
      aiData: { adoptionBeAgrementMentionneBeDetecte: false },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
  });

  it('type invalide ignoré = 0', () => {
    expect(AdoptionBeSectionPrefillRules.computePrefillCount({
      aiData: { adoptionBeTypeDetecte: 'INTERNATIONALE' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('pays FRANCE → 0 même si aiData présent', () => {
    expect(AdoptionBeSectionPrefillRules.computePrefillCount({
      aiData: { adoptionBeTypeDetecte: 'PLENIERE', adoptionBeAgrementMentionneBeDetecte: true },
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });
});
