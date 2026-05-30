import { JournalisteStatutPrefillRules as Rules } from './journaliste-statut-section-prefill-rules';

describe('JournalisteStatutPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: {
        dateEntree: '2018-01-01',
        dateLicenciement: '2023-06-15',
        journalisteCartePresse: true,
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateEntree(input)).toBeNull();
    expect(Rules.computeDateRupture(input)).toBeNull();
    expect(Rules.computeCarteIdentiteProfessionnelle(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 (partiel) when only dateEntree present', () => {
    const input = { aiData: { dateEntree: '2018-01-01' } };
    expect(Rules.computeDateEntree(input)).toBe('2018-01-01');
    expect(Rules.computeDateRupture(input)).toBeNull();
    expect(Rules.computeCarteIdentiteProfessionnelle(input)).toBeNull();
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
        dateEntree: '2018-01-01',
        dateLicenciement: '2023-06-15',
        journalisteCartePresse: true,
      },
    };
    expect(Rules.computeDateEntree(input)).toBe('2018-01-01');
    expect(Rules.computeDateRupture(input)).toBe('2023-06-15');
    expect(Rules.computeCarteIdentiteProfessionnelle(input)).toBe(true);
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('counts journalisteCartePresse=false (booléen explicite) as pré-rempli', () => {
    const input = { aiData: { journalisteCartePresse: false } };
    expect(Rules.computeCarteIdentiteProfessionnelle(input)).toBe(false);
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('rejects non-boolean journalisteCartePresse', () => {
    expect(Rules.computeCarteIdentiteProfessionnelle({ aiData: { journalisteCartePresse: 'oui' } })).toBeNull();
    expect(Rules.computeCarteIdentiteProfessionnelle({ aiData: { journalisteCartePresse: 1 } })).toBeNull();
    expect(Rules.computeCarteIdentiteProfessionnelle({ aiData: { journalisteCartePresse: null } })).toBeNull();
  });

  it('rejects malformed / non-string dates', () => {
    expect(Rules.computeDateEntree({ aiData: { dateEntree: '15/06/2023' } })).toBeNull();
    expect(Rules.computeDateEntree({ aiData: { dateEntree: '2023-6-1' } })).toBeNull();
    expect(Rules.computeDateEntree({ aiData: { dateEntree: 20230101 as unknown as string } })).toBeNull();
    expect(Rules.computeDateRupture({ aiData: { dateLicenciement: '' } })).toBeNull();
  });

  it('does NOT count statutJournalisteDetecte flag alone (visibility trigger, not a form field)', () => {
    const input = { aiData: { statutJournalisteDetecte: true } };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    const input = {
      aiData: { dateEntree: '2018-01-01', dateLicenciement: '2023-06-15' },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });
});
