/**
 * SF-223-07 — Tests Jest du helper `DipBeLoiApplicableFamilleSectionPrefillRules`.
 * Pré-fill F-246 : matière + résidence commune + nationalité commune + choix de
 * loi (0 à 4 champs), BE only.
 */

import { DipBeLoiApplicableFamilleSectionPrefillRules } from './dip-be-loi-applicable-famille-section-prefill-rules';

describe('DipBeLoiApplicableFamilleSectionPrefillRules (SF-223-07)', () => {
  it('computePrefillCount({}) = 0', () => {
    expect(DipBeLoiApplicableFamilleSectionPrefillRules.computePrefillCount({})).toBe(0);
  });

  it('aiData null = 0', () => {
    expect(DipBeLoiApplicableFamilleSectionPrefillRules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('matière seule détectée (BE) = 1', () => {
    expect(DipBeLoiApplicableFamilleSectionPrefillRules.computePrefillCount({
      aiData: { dipFamilleBeMatiereDetectee: 'DIVORCE' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
  });

  it('matière + résidence + nationalité + choix de loi (BE) = 4', () => {
    expect(DipBeLoiApplicableFamilleSectionPrefillRules.computePrefillCount({
      aiData: {
        dipFamilleBeMatiereDetectee: 'SUCCESSION',
        dipFamilleBeResidenceCommuneDetectee: 'FR',
        dipFamilleBeNationaliteCommuneDetectee: 'MA',
        dipFamilleBeChoixLoiDetecte: 'PT',
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(4);
  });

  it('matière hors whitelist ignorée = 0', () => {
    expect(DipBeLoiApplicableFamilleSectionPrefillRules.computePrefillCount({
      aiData: { dipFamilleBeMatiereDetectee: 'AUTRE' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('code pays non ISO-2 ignoré = 0', () => {
    expect(DipBeLoiApplicableFamilleSectionPrefillRules.computePrefillCount({
      aiData: { dipFamilleBeResidenceCommuneDetectee: 'FRA' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
    expect(DipBeLoiApplicableFamilleSectionPrefillRules.computePrefillCount({
      aiData: { dipFamilleBeNationaliteCommuneDetectee: 'fr' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('pays FRANCE → 0 même si aiData présent', () => {
    expect(DipBeLoiApplicableFamilleSectionPrefillRules.computePrefillCount({
      aiData: {
        dipFamilleBeMatiereDetectee: 'DIVORCE',
        dipFamilleBeResidenceCommuneDetectee: 'FR',
        dipFamilleBeNationaliteCommuneDetectee: 'MA',
        dipFamilleBeChoixLoiDetecte: 'PT',
      },
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });
});
