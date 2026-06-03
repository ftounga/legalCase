/**
 * SF-223-08 — Tests Jest du helper
 * `DipBeReconnaissanceDecisionEtrangereSectionPrefillRules`. Pré-fill F-246 :
 * nature + pays + date (0 à 3 champs), BE only.
 */

import { DipBeReconnaissanceDecisionEtrangereSectionPrefillRules } from './dip-be-reconnaissance-decision-etrangere-section-prefill-rules';

describe('DipBeReconnaissanceDecisionEtrangereSectionPrefillRules (SF-223-08)', () => {
  it('computePrefillCount({}) = 0', () => {
    expect(DipBeReconnaissanceDecisionEtrangereSectionPrefillRules.computePrefillCount({})).toBe(0);
  });

  it('aiData null = 0', () => {
    expect(DipBeReconnaissanceDecisionEtrangereSectionPrefillRules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('nature seule détectée (BE) = 1', () => {
    expect(DipBeReconnaissanceDecisionEtrangereSectionPrefillRules.computePrefillCount({
      aiData: { dipReconnaissanceNatureDetectee: 'JUGEMENT_ETRANGER_HORS_UE' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
  });

  it('nature + pays + date (BE) = 3', () => {
    expect(DipBeReconnaissanceDecisionEtrangereSectionPrefillRules.computePrefillCount({
      aiData: {
        dipReconnaissanceNatureDetectee: 'MARIAGE_RELIGIEUX_NON_CIVIL',
        dipReconnaissancePaysDetecte: 'DZ',
        dipReconnaissanceDateDetectee: '2022-05-01',
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(3);
  });

  it('nature hors whitelist ignorée = 0', () => {
    expect(DipBeReconnaissanceDecisionEtrangereSectionPrefillRules.computePrefillCount({
      aiData: { dipReconnaissanceNatureDetectee: 'AUTRE' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('code pays non ISO-2 et date mal formée ignorés = 0', () => {
    expect(DipBeReconnaissanceDecisionEtrangereSectionPrefillRules.computePrefillCount({
      aiData: { dipReconnaissancePaysDetecte: 'MAR' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
    expect(DipBeReconnaissanceDecisionEtrangereSectionPrefillRules.computePrefillCount({
      aiData: { dipReconnaissanceDateDetectee: '01/05/2022' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('pays FRANCE → 0 même si aiData présent', () => {
    expect(DipBeReconnaissanceDecisionEtrangereSectionPrefillRules.computePrefillCount({
      aiData: {
        dipReconnaissanceNatureDetectee: 'JUGEMENT_ETRANGER_HORS_UE',
        dipReconnaissancePaysDetecte: 'MA',
        dipReconnaissanceDateDetectee: '2022-05-01',
      },
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });
});
