import { PacsVpfPrefillRules as Rules } from './pacs-vpf-section-prefill-rules';

describe('PacsVpfPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: {
        pacsConclu: true,
        pacsDate: '2022-01-15',
        pacsDureeVieCommune: 24,
        pacsIntensiteCommunauteVie: 'FORTE' as const,
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns pacsConclu when boolean, rejects non-boolean', () => {
    expect(Rules.computePacsConclu({ aiData: { pacsConclu: true } })).toBe(true);
    expect(Rules.computePacsConclu({ aiData: { pacsConclu: false } })).toBe(false);
    expect(Rules.computePacsConclu({
      aiData: { pacsConclu: 'true' as unknown as boolean },
    })).toBeNull();
  });

  it('returns datePacs when ISO yyyy-MM-dd, rejects malformed', () => {
    expect(Rules.computeDatePacs({ aiData: { pacsDate: '2022-01-15' } })).toBe('2022-01-15');
    expect(Rules.computeDatePacs({ aiData: { pacsDate: '15/01/2022' } })).toBeNull();
    expect(Rules.computeDatePacs({ aiData: { pacsDate: null } })).toBeNull();
  });

  it('returns dureeVieCommuneMois when non-negative number, rejects invalid', () => {
    expect(Rules.computeDureeVieCommuneMois({ aiData: { pacsDureeVieCommune: 24 } })).toBe(24);
    expect(Rules.computeDureeVieCommuneMois({ aiData: { pacsDureeVieCommune: 0 } })).toBe(0);
    expect(Rules.computeDureeVieCommuneMois({ aiData: { pacsDureeVieCommune: -3 } })).toBeNull();
    expect(Rules.computeDureeVieCommuneMois({
      aiData: { pacsDureeVieCommune: '24' as unknown as number },
    })).toBeNull();
  });

  it('returns intensite when in whitelist, rejects others', () => {
    expect(Rules.computeIntensiteCommunauteVie({ aiData: { pacsIntensiteCommunauteVie: 'FORTE' } })).toBe('FORTE');
    expect(Rules.computeIntensiteCommunauteVie({ aiData: { pacsIntensiteCommunauteVie: 'NON_ETABLIE' } })).toBe('NON_ETABLIE');
    expect(Rules.computeIntensiteCommunauteVie({
      aiData: { pacsIntensiteCommunauteVie: 'ENORME' as unknown as 'FORTE' },
    })).toBeNull();
  });

  it('returns 1 when only pacsConclu is present (partiel)', () => {
    expect(Rules.computePrefillCount({ aiData: { pacsConclu: true } })).toBe(1);
  });

  it('returns 4 when all 4 prefill fields are present (complet)', () => {
    const input = {
      aiData: {
        pacsConclu: true,
        pacsDate: '2022-01-15',
        pacsDureeVieCommune: 24,
        pacsIntensiteCommunauteVie: 'FORTE' as const,
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(4);
  });
});
