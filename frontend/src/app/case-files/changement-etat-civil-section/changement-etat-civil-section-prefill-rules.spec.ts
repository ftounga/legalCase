/**
 * SF-246-11 / F-236 SF-236-02 — Tests `ChangementEtatCivilPrefillRules`.
 *
 * SF-246-11 branche dateNaissanceDemandeur comme seul champ avec source backend réelle.
 * computePrefillCount() retourne 1 si FR + date valide, 0 sinon.
 */
import {
  ChangementEtatCivilPrefillRules,
  computeDateNaissance,
  computePrefillCount,
} from './changement-etat-civil-section-prefill-rules';

describe('ChangementEtatCivilPrefillRules — SF-246-11', () => {
  // -------------------------------------------------------------------------
  // computePrefillCount — cas 0
  // -------------------------------------------------------------------------

  it('cas 0 — aiData vide retourne 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('cas 0 — dateNaissanceDemandeurDetectee absente retourne 0', () => {
    expect(computePrefillCount({ aiData: {}, workspaceCountry: 'FRANCE' })).toBe(0);
  });

  it('cas 0 — date non ISO (format JJ/MM/AAAA) retourne 0', () => {
    expect(
      computePrefillCount({
        aiData: { dateNaissanceDemandeurDetectee: '22/03/1985' },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe(0);
  });

  it('cas 0 — workspaceCountry BELGIQUE retourne 0 même avec date valide', () => {
    expect(
      computePrefillCount({
        aiData: { dateNaissanceDemandeurDetectee: '1985-03-22' },
        workspaceCountry: 'BELGIQUE',
      }),
    ).toBe(0);
  });

  it('cas 0 — workspaceCountry absent retourne 0', () => {
    expect(
      computePrefillCount({
        aiData: { dateNaissanceDemandeurDetectee: '1985-03-22' },
      }),
    ).toBe(0);
  });

  it('cas 0 — aiData null retourne 0', () => {
    expect(computePrefillCount({ aiData: null, workspaceCountry: 'FRANCE' })).toBe(0);
  });

  // -------------------------------------------------------------------------
  // computePrefillCount — cas nominal (1 champ)
  // -------------------------------------------------------------------------

  it('cas nominal — date ISO valide + FRANCE retourne 1', () => {
    expect(
      computePrefillCount({
        aiData: { dateNaissanceDemandeurDetectee: '1985-03-22' },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe(1);
  });

  it('cas nominal — date YYYY-MM-DD stricte, pas YYYY-MM seul', () => {
    expect(
      computePrefillCount({
        aiData: { dateNaissanceDemandeurDetectee: '1985-03' },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe(0);
  });

  // -------------------------------------------------------------------------
  // computeDateNaissance — guard FR
  // -------------------------------------------------------------------------

  it('computeDateNaissance — retourne null pour BE même avec date valide', () => {
    expect(
      computeDateNaissance({
        aiData: { dateNaissanceDemandeurDetectee: '1990-07-15' },
        workspaceCountry: 'BELGIQUE',
      }),
    ).toBeNull();
  });

  it('computeDateNaissance — retourne la date pour FR', () => {
    expect(
      computeDateNaissance({
        aiData: { dateNaissanceDemandeurDetectee: '1990-07-15' },
        workspaceCountry: 'FRANCE',
      }),
    ).toBe('1990-07-15');
  });

  // -------------------------------------------------------------------------
  // Parité computePrefillCount ↔ computeDateNaissance (invariant mini-spec)
  // -------------------------------------------------------------------------

  it('parité stricte — computePrefillCount = 1 ssi computeDateNaissance != null', () => {
    const input = {
      aiData: { dateNaissanceDemandeurDetectee: '2000-01-01' },
      workspaceCountry: 'FRANCE',
    };
    const fromCount = computePrefillCount(input);
    const fromDate = computeDateNaissance(input) !== null ? 1 : 0;
    expect(fromCount).toBe(fromDate);
  });

  // -------------------------------------------------------------------------
  // Surface du module
  // -------------------------------------------------------------------------

  it('surface — computePrefillCount est exposé sur ChangementEtatCivilPrefillRules', () => {
    expect(ChangementEtatCivilPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });

  it('surface — computeDateNaissance est exposé (via import direct)', () => {
    expect(typeof computeDateNaissance).toBe('function');
  });
});
