/**
 * SF-223-04 — Tests Jest du helper `GpaBeSituationContentieuseSectionPrefillRules`.
 * Pré-fill F-246 : lieu de la GPA + lien génétique (0 à 2 champs), BE only.
 */

import { GpaBeSituationContentieuseSectionPrefillRules } from './gpa-be-situation-contentieuse-section-prefill-rules';

describe('GpaBeSituationContentieuseSectionPrefillRules (SF-223-04)', () => {
  it('computePrefillCount({}) = 0', () => {
    expect(GpaBeSituationContentieuseSectionPrefillRules.computePrefillCount({})).toBe(0);
  });

  it('aiData null = 0', () => {
    expect(GpaBeSituationContentieuseSectionPrefillRules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('lieu seul détecté (BE) = 1', () => {
    expect(GpaBeSituationContentieuseSectionPrefillRules.computePrefillCount({
      aiData: { gpaBeLieuDetecte: 'BELGIQUE' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
  });

  it('lieu + lien détectés (BE) = 2', () => {
    expect(GpaBeSituationContentieuseSectionPrefillRules.computePrefillCount({
      aiData: { gpaBeLieuDetecte: 'ETRANGER', gpaBeLienGenetiqueDetecte: 'PERE_INTENTIONNEL' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(2);
  });

  it('lieu hors whitelist ignoré = 0', () => {
    expect(GpaBeSituationContentieuseSectionPrefillRules.computePrefillCount({
      aiData: { gpaBeLieuDetecte: 'FRANCE' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('lien hors whitelist ignoré = 0', () => {
    expect(GpaBeSituationContentieuseSectionPrefillRules.computePrefillCount({
      aiData: { gpaBeLienGenetiqueDetecte: 'PERE' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('pays FRANCE → 0 même si aiData présent', () => {
    expect(GpaBeSituationContentieuseSectionPrefillRules.computePrefillCount({
      aiData: { gpaBeLieuDetecte: 'BELGIQUE', gpaBeLienGenetiqueDetecte: 'LES_DEUX' },
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });
});
