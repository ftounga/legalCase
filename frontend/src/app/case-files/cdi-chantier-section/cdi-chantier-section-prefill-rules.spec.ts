import { CdiChantierPrefillRules as Rules } from './cdi-chantier-section-prefill-rules';

describe('CdiChantierPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { dateEntree: '2023-01-06', dateLicenciement: '2026-04-06', cdiChantierSecteur: 'BTP' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateEntree(input)).toBeNull();
    expect(Rules.computeDateRupture(input)).toBeNull();
    expect(Rules.computeSecteur(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 (partiel) when only dateEntree present', () => {
    const input = { aiData: { dateEntree: '2023-01-06' } };
    expect(Rules.computeDateEntree(input)).toBe('2023-01-06');
    expect(Rules.computeDateRupture(input)).toBeNull();
    expect(Rules.computeSecteur(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 3 (nominal) when both dates + secteur present', () => {
    const input = {
      aiData: { dateEntree: '2023-01-06', dateLicenciement: '2026-04-06', cdiChantierSecteur: 'INGENIERIE' },
    };
    expect(Rules.computeDateEntree(input)).toBe('2023-01-06');
    expect(Rules.computeDateRupture(input)).toBe('2026-04-06');
    expect(Rules.computeSecteur(input)).toBe('INGENIERIE');
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('falls back to dateRuptureContrat when dateLicenciement is absent', () => {
    const input = { aiData: { dateRuptureContrat: '2026-03-15' } };
    expect(Rules.computeDateRupture(input)).toBe('2026-03-15');
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('prefers dateLicenciement over dateRuptureContrat when both present', () => {
    const input = { aiData: { dateLicenciement: '2026-04-06', dateRuptureContrat: '2026-03-15' } };
    expect(Rules.computeDateRupture(input)).toBe('2026-04-06');
  });

  it('truncates an ISO datetime to YYYY-MM-DD', () => {
    const input = { aiData: { dateEntree: '2023-01-06T08:30:00Z' } };
    expect(Rules.computeDateEntree(input)).toBe('2023-01-06');
  });

  it('rejects an unknown / non-string secteur', () => {
    expect(Rules.computeSecteur({ aiData: { cdiChantierSecteur: 'CONSTRUCTION' as never } })).toBeNull();
    expect(Rules.computeSecteur({ aiData: { cdiChantierSecteur: 42 as unknown as never } })).toBeNull();
    expect(Rules.computeSecteur({ aiData: { cdiChantierSecteur: null } })).toBeNull();
  });

  it('does NOT count cdiChantierDetecte flag alone (visibility trigger, not a form field)', () => {
    const input = { aiData: { cdiChantierDetecte: true } };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    const input = {
      aiData: { dateEntree: '2023-01-06', dateLicenciement: '2026-04-06', cdiChantierSecteur: 'AUTRE' },
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });
});
