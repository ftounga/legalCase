import { Regroupement10bisBePrefillRules as Rules } from './regroupement-10bis-be-section-prefill-rules';

describe('Regroupement10bisBePrefillRules', () => {
  it('returns 0 when no aiData (gate BE manquant)', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is FRANCE (mono-pays BE)', () => {
    const input = {
      aiData: {
        be10bisLienFamilial: 'CONJOINT',
        be10bisRevenusMensuels: 1950,
        be10bisDureeSejour: 24,
        be10bisDateFinCarteA: '2027-12-31',
      },
      workspaceCountry: 'FRANCE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 2 when only 2 partial fields present (BELGIQUE)', () => {
    const input = {
      aiData: {
        be10bisLienFamilial: 'CONJOINT',
        be10bisRevenusMensuels: 1950,
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeLienFamilial(input)).toBe('CONJOINT');
    expect(Rules.computeRevenusMensuelsNetsRegroupant(input)).toBe(1950);
    expect(Rules.computeDureeSejour(input)).toBeNull();
    expect(Rules.computeDateFinCarteA(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('returns 4 when all 4 prefill fields present (BELGIQUE)', () => {
    const input = {
      aiData: {
        be10bisLienFamilial: 'PARTENAIRE_ENREGISTRE',
        be10bisRevenusMensuels: 2100,
        be10bisDureeSejour: 60,
        be10bisDateFinCarteA: '2027-06-30',
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(4);
  });

  it('accepts the 5 lienFamilial whitelist values', () => {
    for (const v of [
      'CONJOINT',
      'PARTENAIRE_ENREGISTRE',
      'ENFANT_MOINS_21',
      'ENFANT_21_PLUS_CHARGE',
      'ASCENDANT_CHARGE',
    ]) {
      expect(Rules.computeLienFamilial({
        aiData: { be10bisLienFamilial: v },
        workspaceCountry: 'BELGIQUE',
      })).toBe(v);
    }
  });

  it('rejects lienFamilial outside whitelist', () => {
    expect(Rules.computeLienFamilial({
      aiData: { be10bisLienFamilial: 'COLOCATAIRE' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeLienFamilial({
      aiData: { be10bisLienFamilial: null },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('normalizes lienFamilial case (conjoint -> CONJOINT)', () => {
    expect(Rules.computeLienFamilial({
      aiData: { be10bisLienFamilial: ' conjoint ' as 'CONJOINT' },
      workspaceCountry: 'BELGIQUE',
    })).toBe('CONJOINT');
  });

  it('rejects non-integer revenus and out-of-range', () => {
    expect(Rules.computeRevenusMensuelsNetsRegroupant({
      aiData: { be10bisRevenusMensuels: 1500.5 as unknown as number },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeRevenusMensuelsNetsRegroupant({
      aiData: { be10bisRevenusMensuels: -100 },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeRevenusMensuelsNetsRegroupant({
      aiData: { be10bisRevenusMensuels: 100_001 },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeRevenusMensuelsNetsRegroupant({
      aiData: { be10bisRevenusMensuels: 0 },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('rejects non-integer dureeSejour and out-of-range', () => {
    expect(Rules.computeDureeSejour({
      aiData: { be10bisDureeSejour: 12.5 as unknown as number },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDureeSejour({
      aiData: { be10bisDureeSejour: -1 },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDureeSejour({
      aiData: { be10bisDureeSejour: 601 },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDureeSejour({
      aiData: { be10bisDureeSejour: 0 },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
    expect(Rules.computeDureeSejour({
      aiData: { be10bisDureeSejour: 600 },
      workspaceCountry: 'BELGIQUE',
    })).toBe(600);
  });

  it('accepts ISO date strings for dateFinCarteA', () => {
    expect(Rules.computeDateFinCarteA({
      aiData: { be10bisDateFinCarteA: '2027-12-31' },
      workspaceCountry: 'BELGIQUE',
    })).toBe('2027-12-31');
  });

  it('rejects non-ISO date and gibberish for dateFinCarteA', () => {
    expect(Rules.computeDateFinCarteA({
      aiData: { be10bisDateFinCarteA: '31/12/2027' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDateFinCarteA({
      aiData: { be10bisDateFinCarteA: 'pas-une-date' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDateFinCarteA({
      aiData: { be10bisDateFinCarteA: 12345 as unknown as string },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDateFinCarteA({
      aiData: { be10bisDateFinCarteA: '2027-13-45' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('does NOT count 10ter fields (other Immigration BE tool)', () => {
    const input = {
      aiData: {
        be10terLienFamilial: 'CONJOINT',
        be10terTypeCarte: 'CARTE_B',
        be10terRevenusMensuels: 1950,
        be10terDureeSejour: 24,
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });
});
