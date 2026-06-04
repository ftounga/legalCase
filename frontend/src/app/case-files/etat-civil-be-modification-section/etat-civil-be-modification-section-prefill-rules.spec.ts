/**
 * SF-223-09 — Tests Jest du helper
 * `EtatCivilBeModificationSectionPrefillRules`. Pré-fill F-246 : type +
 * majorité + nationalité/résidence (0 à 3 champs), BE only.
 */

import { EtatCivilBeModificationSectionPrefillRules } from './etat-civil-be-modification-section-prefill-rules';

describe('EtatCivilBeModificationSectionPrefillRules (SF-223-09)', () => {
  it('computePrefillCount({}) = 0', () => {
    expect(EtatCivilBeModificationSectionPrefillRules.computePrefillCount({})).toBe(0);
  });

  it('aiData null = 0', () => {
    expect(EtatCivilBeModificationSectionPrefillRules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('type seul détecté (BE) = 1', () => {
    expect(EtatCivilBeModificationSectionPrefillRules.computePrefillCount({
      aiData: { etatCivilModificationTypeDetecte: 'CHANGEMENT_SEXE' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
  });

  it('type + majorité + nationalité (BE) = 3', () => {
    expect(EtatCivilBeModificationSectionPrefillRules.computePrefillCount({
      aiData: {
        etatCivilModificationTypeDetecte: 'CHANGEMENT_PRENOM',
        etatCivilModificationMajeurDetecte: true,
        etatCivilModificationNationaliteResidentDetectee: false,
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(3);
  });

  it('type hors whitelist ignoré = 0', () => {
    expect(EtatCivilBeModificationSectionPrefillRules.computePrefillCount({
      aiData: { etatCivilModificationTypeDetecte: 'AUTRE' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('booleans non-boolean ignorés = 0', () => {
    expect(EtatCivilBeModificationSectionPrefillRules.computePrefillCount({
      aiData: {
        etatCivilModificationMajeurDetecte: 'oui' as unknown as boolean,
        etatCivilModificationNationaliteResidentDetectee: null,
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('pays FRANCE → 0 même si aiData présent', () => {
    expect(EtatCivilBeModificationSectionPrefillRules.computePrefillCount({
      aiData: {
        etatCivilModificationTypeDetecte: 'CHANGEMENT_NOM',
        etatCivilModificationMajeurDetecte: true,
        etatCivilModificationNationaliteResidentDetectee: true,
      },
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });
});
