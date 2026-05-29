import { UeEeeSuisseSejourPrefillRules as Rules } from './ue-eee-suisse-sejour-section-prefill-rules';

describe('UeEeeSuisseSejourPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { nationalite: 'Italienne', nationaliteUe: true, aesDureePresenceMois: 72 },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeNationalite(input)).toBeNull();
    expect(Rules.computeEstCitoyenUE(input)).toBeNull();
    expect(Rules.computeDureeSejourMois(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('pre-fills nationalite (trimmed, FRANCE)', () => {
    expect(Rules.computeNationalite({ aiData: { nationalite: '  Espagnole  ' } })).toBe('Espagnole');
    expect(Rules.computeNationalite({ aiData: { nationalite: '' } })).toBeNull();
    expect(Rules.computeNationalite({ aiData: { nationalite: '   ' } })).toBeNull();
    expect(Rules.computeNationalite({ aiData: { nationalite: 42 as unknown as string } })).toBeNull();
  });

  it('pre-fills estCitoyenUE from boolean nationaliteUe (both true and false)', () => {
    expect(Rules.computeEstCitoyenUE({ aiData: { nationaliteUe: true } })).toBe(true);
    expect(Rules.computeEstCitoyenUE({ aiData: { nationaliteUe: false } })).toBe(false);
    expect(Rules.computeEstCitoyenUE({ aiData: { nationaliteUe: null } })).toBeNull();
    expect(Rules.computeEstCitoyenUE({ aiData: { nationaliteUe: 'true' as unknown as boolean } })).toBeNull();
    expect(Rules.computeEstCitoyenUE({ aiData: {} })).toBeNull();
  });

  it('pre-fills dureeSejourMois (truncated, >= 0)', () => {
    expect(Rules.computeDureeSejourMois({ aiData: { aesDureePresenceMois: 60 } })).toBe(60);
    expect(Rules.computeDureeSejourMois({ aiData: { aesDureePresenceMois: 12.9 } })).toBe(12);
    expect(Rules.computeDureeSejourMois({ aiData: { aesDureePresenceMois: 0 } })).toBe(0);
    expect(Rules.computeDureeSejourMois({ aiData: { aesDureePresenceMois: -5 } })).toBeNull();
    expect(Rules.computeDureeSejourMois({ aiData: { aesDureePresenceMois: NaN } })).toBeNull();
    expect(Rules.computeDureeSejourMois({ aiData: { aesDureePresenceMois: '5' as unknown as number } })).toBeNull();
  });

  it('getPrefillCount returns 1 when only nationalite valid (FRANCE)', () => {
    expect(Rules.computePrefillCount({ aiData: { nationalite: 'Portugaise' } })).toBe(1);
  });

  it('getPrefillCount returns 2 when nationalite + citoyenUE valid (FRANCE)', () => {
    expect(Rules.computePrefillCount({ aiData: { nationalite: 'Allemande', nationaliteUe: true } })).toBe(2);
  });

  it('getPrefillCount returns 3 when all three valid (FRANCE)', () => {
    expect(Rules.computePrefillCount({
      aiData: { nationalite: 'Belge', nationaliteUe: true, aesDureePresenceMois: 80 },
    })).toBe(3);
  });

  it('counts estCitoyenUE=false as a pre-fill (boolean present)', () => {
    expect(Rules.computePrefillCount({ aiData: { nationaliteUe: false } })).toBe(1);
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    expect(Rules.computePrefillCount({
      aiData: { nationalite: 'Suisse', nationaliteUe: true, aesDureePresenceMois: 24 },
    })).toBe(3);
  });
});
