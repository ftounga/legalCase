/**
 * SF-223-06 — Tests Jest du helper `RegimeBeSeparationBiensSectionPrefillRules`.
 * Pré-fill F-246 : variante + date du contrat + patrimoine époux 1 + patrimoine
 * époux 2 (0 à 4 champs), BE only.
 */

import { RegimeBeSeparationBiensSectionPrefillRules } from './regime-be-separation-biens-section-prefill-rules';

describe('RegimeBeSeparationBiensSectionPrefillRules (SF-223-06)', () => {
  it('computePrefillCount({}) = 0', () => {
    expect(RegimeBeSeparationBiensSectionPrefillRules.computePrefillCount({})).toBe(0);
  });

  it('aiData null = 0', () => {
    expect(RegimeBeSeparationBiensSectionPrefillRules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('variante seule détectée (BE) = 1', () => {
    expect(RegimeBeSeparationBiensSectionPrefillRules.computePrefillCount({
      aiData: { regimeSeparationBiensBeVarianteDetectee: 'SEPARATION_PURE' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
  });

  it('variante + date + 2 patrimoines détectés (BE) = 4', () => {
    expect(RegimeBeSeparationBiensSectionPrefillRules.computePrefillCount({
      aiData: {
        regimeSeparationBiensBeVarianteDetectee: 'SEPARATION_AVEC_PARTICIPATION_ACQUETS',
        regimeSeparationBiensBeDateContratDetectee: '2020-01-15',
        regimeSeparationBiensBePatrimoineEpoux1Detecte: '120000',
        regimeSeparationBiensBePatrimoineEpoux2Detecte: '80000',
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(4);
  });

  it('variante hors whitelist ignorée = 0', () => {
    expect(RegimeBeSeparationBiensSectionPrefillRules.computePrefillCount({
      aiData: { regimeSeparationBiensBeVarianteDetectee: 'COMMUNAUTE_UNIVERSELLE' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('date mal formée ignorée = 0', () => {
    expect(RegimeBeSeparationBiensSectionPrefillRules.computePrefillCount({
      aiData: { regimeSeparationBiensBeDateContratDetectee: '15/01/2020' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('patrimoine non numérique / négatif ignoré = 0', () => {
    expect(RegimeBeSeparationBiensSectionPrefillRules.computePrefillCount({
      aiData: { regimeSeparationBiensBePatrimoineEpoux1Detecte: 'beaucoup' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
    expect(RegimeBeSeparationBiensSectionPrefillRules.computePrefillCount({
      aiData: { regimeSeparationBiensBePatrimoineEpoux2Detecte: '-10' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('pays FRANCE → 0 même si aiData présent', () => {
    expect(RegimeBeSeparationBiensSectionPrefillRules.computePrefillCount({
      aiData: {
        regimeSeparationBiensBeVarianteDetectee: 'SEPARATION_PURE',
        regimeSeparationBiensBeDateContratDetectee: '2020-01-15',
        regimeSeparationBiensBePatrimoineEpoux1Detecte: '120000',
        regimeSeparationBiensBePatrimoineEpoux2Detecte: '80000',
      },
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });
});
