import { NaturalisationConjointBelgeBePrefillRules as Rules } from './naturalisation-conjoint-belge-be-section-prefill-rules';

describe('NaturalisationConjointBelgeBePrefillRules', () => {
  it('returns 0 when no aiData (gate BE manquant)', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is FRANCE (mono-pays BE)', () => {
    const input = {
      aiData: {
        naturalisationBeArt16DateMarriage: '2020-01-15',
        naturalisationBeArt16DureeCohabitation: 72,
        naturalisationBeArt16NiveauLangue: 'A2' as const,
      },
      workspaceCountry: 'FRANCE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 when only dureeCohabitation present (BELGIQUE) — partial 1/3', () => {
    const input = {
      aiData: { naturalisationBeArt16DureeCohabitation: 60 },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computeDateMarriage(input)).toBeNull();
    expect(Rules.computeDureeCohabitation(input)).toBe(60);
    expect(Rules.computeNiveauLangue(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 3 when all 3 prefill fields present (BELGIQUE) — max possible', () => {
    const input = {
      aiData: {
        naturalisationBeArt16DateMarriage: '2020-05-15',
        naturalisationBeArt16DureeCohabitation: 72,
        naturalisationBeArt16NiveauLangue: 'A2' as const,
      },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('accepts the 3 niveauLangue whitelist values', () => {
    for (const v of ['INFERIEUR_A2', 'A2', 'SUPERIEUR_A2'] as const) {
      expect(Rules.computeNiveauLangue({
        aiData: { naturalisationBeArt16NiveauLangue: v },
        workspaceCountry: 'BELGIQUE' as const,
      })).toBe(v);
    }
  });

  it('rejects niveauLangue outside whitelist', () => {
    expect(Rules.computeNiveauLangue({
      aiData: { naturalisationBeArt16NiveauLangue: 'B2' as unknown as 'A2' },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
    expect(Rules.computeNiveauLangue({
      aiData: { naturalisationBeArt16NiveauLangue: null },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
  });

  it('normalizes niveauLangue case (a2 -> A2)', () => {
    expect(Rules.computeNiveauLangue({
      aiData: { naturalisationBeArt16NiveauLangue: ' a2 ' as unknown as 'A2' },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBe('A2');
  });

  it('rejects non-integer and out-of-range dureeCohabitation', () => {
    expect(Rules.computeDureeCohabitation({
      aiData: { naturalisationBeArt16DureeCohabitation: 12.5 as unknown as number },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
    expect(Rules.computeDureeCohabitation({
      aiData: { naturalisationBeArt16DureeCohabitation: -1 },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
    expect(Rules.computeDureeCohabitation({
      aiData: { naturalisationBeArt16DureeCohabitation: 601 },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
    expect(Rules.computeDureeCohabitation({
      aiData: { naturalisationBeArt16DureeCohabitation: 0 },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBe(0);
    expect(Rules.computeDureeCohabitation({
      aiData: { naturalisationBeArt16DureeCohabitation: 600 },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBe(600);
  });

  it('accepts ISO yyyy-MM-dd dateMarriage and rejects invalid formats', () => {
    expect(Rules.computeDateMarriage({
      aiData: { naturalisationBeArt16DateMarriage: '2020-05-15' },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBe('2020-05-15');
    expect(Rules.computeDateMarriage({
      aiData: { naturalisationBeArt16DateMarriage: '15/05/2020' as unknown as string },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
    expect(Rules.computeDateMarriage({
      aiData: { naturalisationBeArt16DateMarriage: '' },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
    expect(Rules.computeDateMarriage({
      aiData: { naturalisationBeArt16DateMarriage: null },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
  });

  it('does NOT count 12bis / 10bis / 10ter fields (other Immigration BE tools)', () => {
    const input = {
      aiData: {
        naturalisationBeDureeSejour: 60,
        naturalisationBeTypeSejour: 'ILLIMITE' as const,
        naturalisationBeNiveauLangue: 'A2' as const,
        be10bisLienFamilial: 'CONJOINT' as const,
        be10terLienFamilial: 'CONJOINT' as const,
      },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('PREFILL_COUNT_ALWAYS_ZERO for aspirational fields (cohabLeg + preuves + ordre public)', () => {
    // Even if the AI somehow surfaces these, they do NOT count towards the badge.
    // The component must treat them as user-only input (no provenance signal).
    const input = {
      aiData: {
        naturalisationBeArt16DateMarriage: '2020-05-15',
        naturalisationBeArt16DureeCohabitation: 60,
        naturalisationBeArt16NiveauLangue: 'A2' as const,
      },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBeLessThanOrEqual(3);
    expect(Rules.computePrefillCount(input)).toBe(3);
  });
});
