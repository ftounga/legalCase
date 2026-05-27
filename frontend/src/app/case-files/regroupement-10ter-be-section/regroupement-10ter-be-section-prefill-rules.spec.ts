import { Regroupement10terBePrefillRules as Rules } from './regroupement-10ter-be-section-prefill-rules';

describe('Regroupement10terBePrefillRules', () => {
  it('returns 0 when no aiData (gate BE manquant)', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is FRANCE (mono-pays BE)', () => {
    const input = {
      aiData: {
        be10terLienFamilial: 'CONJOINT',
        be10terTypeCarte: 'CARTE_B',
        be10terRevenusMensuels: 1950,
        be10terDureeSejour: 24,
      },
      workspaceCountry: 'FRANCE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 2 when only 2 partial fields present (BELGIQUE)', () => {
    const input = {
      aiData: {
        be10terLienFamilial: 'CONJOINT',
        be10terRevenusMensuels: 1950,
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeLienFamilial(input)).toBe('CONJOINT');
    expect(Rules.computeRevenusMensuelsNetsRegroupant(input)).toBe(1950);
    expect(Rules.computeTypeCarteRegroupant(input)).toBeNull();
    expect(Rules.computeDureeSejour(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('returns 4 when all 4 prefill fields present (BELGIQUE)', () => {
    const input = {
      aiData: {
        be10terLienFamilial: 'PARTENAIRE_ENREGISTRE',
        be10terTypeCarte: 'CARTE_C',
        be10terRevenusMensuels: 2100,
        be10terDureeSejour: 60,
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
        aiData: { be10terLienFamilial: v },
        workspaceCountry: 'BELGIQUE',
      })).toBe(v);
    }
  });

  it('rejects lienFamilial outside whitelist', () => {
    expect(Rules.computeLienFamilial({
      aiData: { be10terLienFamilial: 'COLOCATAIRE' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeLienFamilial({
      aiData: { be10terLienFamilial: null },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('normalizes lienFamilial case (conjoint -> CONJOINT)', () => {
    expect(Rules.computeLienFamilial({
      aiData: { be10terLienFamilial: ' conjoint ' as 'CONJOINT' },
      workspaceCountry: 'BELGIQUE',
    })).toBe('CONJOINT');
  });

  it('accepts only CARTE_B / CARTE_C as typeCarte', () => {
    expect(Rules.computeTypeCarteRegroupant({
      aiData: { be10terTypeCarte: 'CARTE_B' },
      workspaceCountry: 'BELGIQUE',
    })).toBe('CARTE_B');
    expect(Rules.computeTypeCarteRegroupant({
      aiData: { be10terTypeCarte: 'CARTE_C' },
      workspaceCountry: 'BELGIQUE',
    })).toBe('CARTE_C');
    expect(Rules.computeTypeCarteRegroupant({
      aiData: { be10terTypeCarte: 'CARTE_A' as 'CARTE_B' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('rejects non-integer revenus and out-of-range', () => {
    expect(Rules.computeRevenusMensuelsNetsRegroupant({
      aiData: { be10terRevenusMensuels: 1500.5 as unknown as number },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeRevenusMensuelsNetsRegroupant({
      aiData: { be10terRevenusMensuels: -100 },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeRevenusMensuelsNetsRegroupant({
      aiData: { be10terRevenusMensuels: 100_001 },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    // 0 is allowed (no income, integer in range)
    expect(Rules.computeRevenusMensuelsNetsRegroupant({
      aiData: { be10terRevenusMensuels: 0 },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('rejects non-integer dureeSejour and out-of-range', () => {
    expect(Rules.computeDureeSejour({
      aiData: { be10terDureeSejour: 12.5 as unknown as number },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDureeSejour({
      aiData: { be10terDureeSejour: -1 },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDureeSejour({
      aiData: { be10terDureeSejour: 601 },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDureeSejour({
      aiData: { be10terDureeSejour: 0 },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
    expect(Rules.computeDureeSejour({
      aiData: { be10terDureeSejour: 600 },
      workspaceCountry: 'BELGIQUE',
    })).toBe(600);
  });

  it('does NOT count single-permit fields (other Immigration BE tool)', () => {
    const input = {
      aiData: {
        singlePermitDateDebut: '2026-01-01',
        singlePermitRegion: 'WALLONIE',
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });
});
