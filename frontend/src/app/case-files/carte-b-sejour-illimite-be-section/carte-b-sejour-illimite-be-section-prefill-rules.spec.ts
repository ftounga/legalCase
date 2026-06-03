import { CarteBSejourIllimiteBePrefillRules as Rules } from './carte-b-sejour-illimite-be-section-prefill-rules';

describe('CarteBSejourIllimiteBePrefillRules', () => {
  it('returns 0 when no aiData (gate BE manquant)', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is FRANCE (mono-pays BE)', () => {
    const input = {
      aiData: {
        carteBDateDebutSejour: '2020-01-01',
        carteBSejourIninterrompu: true,
        carteBMotifStable: true,
      },
      workspaceCountry: 'FRANCE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeDateDebut(input)).toBeNull();
    expect(Rules.computeSejourIninterrompu(input)).toBeNull();
    expect(Rules.computeMotifStable(input)).toBeNull();
  });

  it('returns 1 when only date present (BELGIQUE)', () => {
    const input = {
      aiData: { carteBDateDebutSejour: '2020-01-01' },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computeDateDebut(input)).toBe('2020-01-01');
    expect(Rules.computeSejourIninterrompu(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 3 (nominal) when all real fields present (BELGIQUE)', () => {
    const input = {
      aiData: {
        carteBDateDebutSejour: '2020-01-01',
        carteBSejourIninterrompu: true,
        carteBMotifStable: false,
      },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computeDateDebut(input)).toBe('2020-01-01');
    expect(Rules.computeSejourIninterrompu(input)).toBe(true);
    expect(Rules.computeMotifStable(input)).toBe(false);
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('counts boolean false as a present prefilled value', () => {
    const input = {
      aiData: {
        carteBSejourIninterrompu: false,
        carteBMotifStable: false,
      },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computeSejourIninterrompu(input)).toBe(false);
    expect(Rules.computeMotifStable(input)).toBe(false);
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('rejects malformed / invalid debut dates', () => {
    expect(Rules.computeDateDebut({
      aiData: { carteBDateDebutSejour: '01/01/2020' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDateDebut({
      aiData: { carteBDateDebutSejour: '2020-02-30' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDateDebut({
      aiData: { carteBDateDebutSejour: 12345 as unknown as string },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('rejects non-boolean values for booleans', () => {
    expect(Rules.computeSejourIninterrompu({
      aiData: { carteBSejourIninterrompu: 'true' as unknown as boolean },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('does NOT count aspirational fields absences / ordre public', () => {
    const input = {
      aiData: {
        carteBDateDebutSejour: '2020-01-01',
        carteBSejourIninterrompu: true,
        carteBMotifStable: true,
        // simulate AI accidentally returning appréciation fields — must be ignored:
        absencesSuperieuresLimites: true,
        ordrePublicRisque: true,
      } as unknown as Record<string, unknown>,
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('does NOT count other Immigration BE tool fields (carte A prorogation)', () => {
    const input = {
      aiData: {
        carteAProrogationDateExpiration: '2026-05-01',
        carteAProrogationMotifPersiste: true,
      } as unknown as Record<string, unknown>,
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });
});
