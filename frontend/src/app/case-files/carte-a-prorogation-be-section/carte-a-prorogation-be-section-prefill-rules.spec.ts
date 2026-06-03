import { CarteAProrogationBePrefillRules as Rules } from './carte-a-prorogation-be-section-prefill-rules';

describe('CarteAProrogationBePrefillRules', () => {
  it('returns 0 when no aiData (gate BE manquant)', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is FRANCE (mono-pays BE)', () => {
    const input = {
      aiData: {
        carteAProrogationDateExpiration: '2026-09-01',
        carteAProrogationMotifPersiste: true,
        carteAProrogationConditionsReunies: true,
      },
      workspaceCountry: 'FRANCE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeDateExpiration(input)).toBeNull();
    expect(Rules.computeMotifPersiste(input)).toBeNull();
    expect(Rules.computeConditionsReunies(input)).toBeNull();
  });

  it('returns 1 when only date present (BELGIQUE)', () => {
    const input = {
      aiData: { carteAProrogationDateExpiration: '2026-09-01' },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computeDateExpiration(input)).toBe('2026-09-01');
    expect(Rules.computeMotifPersiste(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 3 (nominal) when all real fields present (BELGIQUE)', () => {
    const input = {
      aiData: {
        carteAProrogationDateExpiration: '2026-09-01',
        carteAProrogationMotifPersiste: true,
        carteAProrogationConditionsReunies: false,
      },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computeDateExpiration(input)).toBe('2026-09-01');
    expect(Rules.computeMotifPersiste(input)).toBe(true);
    expect(Rules.computeConditionsReunies(input)).toBe(false);
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('counts boolean false as a present prefilled value', () => {
    const input = {
      aiData: {
        carteAProrogationMotifPersiste: false,
        carteAProrogationConditionsReunies: false,
      },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computeMotifPersiste(input)).toBe(false);
    expect(Rules.computeConditionsReunies(input)).toBe(false);
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('rejects malformed / invalid expiration dates', () => {
    expect(Rules.computeDateExpiration({
      aiData: { carteAProrogationDateExpiration: '01/09/2026' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDateExpiration({
      aiData: { carteAProrogationDateExpiration: '2026-02-30' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDateExpiration({
      aiData: { carteAProrogationDateExpiration: 12345 as unknown as string },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('rejects non-boolean values for booleans', () => {
    expect(Rules.computeMotifPersiste({
      aiData: { carteAProrogationMotifPersiste: 'true' as unknown as boolean },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('does NOT count aspirational fields demandeDeposee / dateDemande', () => {
    const input = {
      aiData: {
        carteAProrogationDateExpiration: '2026-09-01',
        carteAProrogationMotifPersiste: true,
        carteAProrogationConditionsReunies: true,
        // simulate AI accidentally returning procedural fields — must be ignored:
        demandeDeposee: true,
        dateDemande: '2026-08-01',
      } as unknown as Record<string, unknown>,
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('does NOT count other Immigration BE tool fields', () => {
    const input = {
      aiData: {
        recoursCceDateNotification: '2026-05-01',
        protectionTemporaireUkraineDetectee: true,
      } as unknown as Record<string, unknown>,
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });
});
