import { AesPresenceProuveePrefillRules as Rules } from './aes-presence-prouvee-section-prefill-rules';

describe('AesPresenceProuveePrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { aesDateEntreeFrance: '2019-01-01' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeInitialPeriode(input)).toBeNull();
  });

  it('returns 1 + initial periode when aesDateEntreeFrance is a valid past ISO date', () => {
    const input = { aiData: { aesDateEntreeFrance: '2019-01-01' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    const periode = Rules.computeInitialPeriode(input);
    expect(periode).not.toBeNull();
    expect(periode!.debut).toBe('2019-01-01');
    expect(periode!.fin).toBe(Rules.todayIso());
    expect(periode!.typePiece).toBe('AUTRE');
  });

  it('rejects malformed / non-string / future dates', () => {
    expect(Rules.computeInitialPeriode({ aiData: { aesDateEntreeFrance: '01/01/2019' } })).toBeNull();
    expect(Rules.computeInitialPeriode({ aiData: { aesDateEntreeFrance: 12345 as unknown as string } })).toBeNull();
    expect(Rules.computeInitialPeriode({ aiData: { aesDateEntreeFrance: null } })).toBeNull();
    expect(Rules.computeInitialPeriode({ aiData: { aesDateEntreeFrance: '2999-12-31' } })).toBeNull();
    expect(Rules.computePrefillCount({ aiData: { aesDateEntreeFrance: '2999-12-31' } })).toBe(0);
  });

  it('todayIso returns an ISO YYYY-MM-DD string', () => {
    expect(Rules.todayIso()).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });
});
