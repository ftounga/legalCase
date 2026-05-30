import { ApprentissageRupturePrefillRules as Rules } from './apprentissage-rupture-section-prefill-rules';

describe('ApprentissageRupturePrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { dateEntree: '2026-01-06', dateRuptureContrat: '2026-04-06', apprentissageMotifRupture: 'FAUTE_GRAVE' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateDebutContrat(input)).toBeNull();
    expect(Rules.computeDateRupture(input)).toBeNull();
    expect(Rules.computeMotifRupture(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 (partiel) when only dateEntree present', () => {
    const input = { aiData: { dateEntree: '2026-01-06' } };
    expect(Rules.computeDateDebutContrat(input)).toBe('2026-01-06');
    expect(Rules.computeDateRupture(input)).toBeNull();
    expect(Rules.computeMotifRupture(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 3 (nominal) when both dates + motif present', () => {
    const input = {
      aiData: { dateEntree: '2026-01-06', dateRuptureContrat: '2026-04-06', apprentissageMotifRupture: 'INAPTITUDE' },
    };
    expect(Rules.computeDateDebutContrat(input)).toBe('2026-01-06');
    expect(Rules.computeDateRupture(input)).toBe('2026-04-06');
    expect(Rules.computeMotifRupture(input)).toBe('INAPTITUDE');
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('falls back to dateLicenciement when dateRuptureContrat is absent', () => {
    const input = { aiData: { dateLicenciement: '2026-03-15' } };
    expect(Rules.computeDateRupture(input)).toBe('2026-03-15');
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('prefers dateRuptureContrat over dateLicenciement when both present', () => {
    const input = { aiData: { dateRuptureContrat: '2026-04-06', dateLicenciement: '2026-03-15' } };
    expect(Rules.computeDateRupture(input)).toBe('2026-04-06');
  });

  it('truncates an ISO datetime to YYYY-MM-DD', () => {
    const input = { aiData: { dateEntree: '2026-01-06T08:30:00Z' } };
    expect(Rules.computeDateDebutContrat(input)).toBe('2026-01-06');
  });

  it('rejects an unknown / non-string motif', () => {
    expect(Rules.computeMotifRupture({ aiData: { apprentissageMotifRupture: 'AUTRE' } })).toBeNull();
    expect(Rules.computeMotifRupture({ aiData: { apprentissageMotifRupture: 42 as unknown as string } })).toBeNull();
    expect(Rules.computeMotifRupture({ aiData: { apprentissageMotifRupture: null } })).toBeNull();
  });

  it('does NOT count apprentissageRuptureDetectee flag alone (visibility trigger, not a form field)', () => {
    const input = { aiData: { apprentissageRuptureDetectee: true } };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    const input = {
      aiData: { dateEntree: '2026-01-06', dateRuptureContrat: '2026-04-06', apprentissageMotifRupture: 'SANS_MOTIF' },
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });
});
