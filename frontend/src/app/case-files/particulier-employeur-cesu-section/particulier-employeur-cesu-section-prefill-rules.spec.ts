import { ParticulierEmployeurCesuPrefillRules as Rules } from './particulier-employeur-cesu-section-prefill-rules';

describe('ParticulierEmployeurCesuPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: {
        dateEntree: '2020-01-01',
        dateLicenciement: '2023-06-15',
        cesuCategorieEmploye: 'ASSISTANT_MATERNEL',
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateEntree(input)).toBeNull();
    expect(Rules.computeDateRupture(input)).toBeNull();
    expect(Rules.computeCategorieEmploye(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 (partiel) when only dateEntree present', () => {
    const input = { aiData: { dateEntree: '2020-01-01' } };
    expect(Rules.computeDateEntree(input)).toBe('2020-01-01');
    expect(Rules.computeDateRupture(input)).toBeNull();
    expect(Rules.computeCategorieEmploye(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('maps dateRupture from aiData.dateLicenciement', () => {
    const input = { aiData: { dateLicenciement: '2023-06-15' } };
    expect(Rules.computeDateRupture(input)).toBe('2023-06-15');
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 3 (nominal) when all three fields present', () => {
    const input = {
      aiData: {
        dateEntree: '2020-01-01',
        dateLicenciement: '2023-06-15',
        cesuCategorieEmploye: 'SALARIE_PARTICULIER_EMPLOYEUR',
      },
    };
    expect(Rules.computeDateEntree(input)).toBe('2020-01-01');
    expect(Rules.computeDateRupture(input)).toBe('2023-06-15');
    expect(Rules.computeCategorieEmploye(input)).toBe('SALARIE_PARTICULIER_EMPLOYEUR');
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('accepts both valid categorieEmploye enum values, rejects unknown', () => {
    expect(Rules.computeCategorieEmploye({ aiData: { cesuCategorieEmploye: 'ASSISTANT_MATERNEL' } }))
      .toBe('ASSISTANT_MATERNEL');
    expect(Rules.computeCategorieEmploye({ aiData: { cesuCategorieEmploye: 'AUTRE' } })).toBeNull();
    expect(Rules.computeCategorieEmploye({ aiData: { cesuCategorieEmploye: 42 as unknown as string } })).toBeNull();
    expect(Rules.computeCategorieEmploye({ aiData: { cesuCategorieEmploye: null } })).toBeNull();
  });

  it('rejects malformed / non-string dates', () => {
    expect(Rules.computeDateEntree({ aiData: { dateEntree: '15/06/2023' } })).toBeNull();
    expect(Rules.computeDateEntree({ aiData: { dateEntree: '2023-6-1' } })).toBeNull();
    expect(Rules.computeDateEntree({ aiData: { dateEntree: 20230101 as unknown as string } })).toBeNull();
    expect(Rules.computeDateRupture({ aiData: { dateLicenciement: '' } })).toBeNull();
  });

  it('does NOT count particulierEmployeurDetecte flag alone (visibility trigger, not a form field)', () => {
    const input = { aiData: { particulierEmployeurDetecte: true } };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    const input = {
      aiData: { dateEntree: '2020-01-01', dateLicenciement: '2023-06-15' },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });
});
