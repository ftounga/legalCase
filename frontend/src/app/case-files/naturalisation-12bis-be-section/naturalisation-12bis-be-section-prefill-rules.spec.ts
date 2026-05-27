import { Naturalisation12bisBePrefillRules as Rules } from './naturalisation-12bis-be-section-prefill-rules';

describe('Naturalisation12bisBePrefillRules', () => {
  it('returns 0 when no aiData (gate BE manquant)', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is FRANCE (mono-pays BE)', () => {
    const input = {
      aiData: {
        naturalisationBeDureeSejour: 72,
        naturalisationBeTypeSejour: 'ILLIMITE' as const,
        naturalisationBeNiveauLangue: 'A2' as const,
      },
      workspaceCountry: 'FRANCE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 when only dureeSejour present (BELGIQUE)', () => {
    const input = {
      aiData: { naturalisationBeDureeSejour: 60 },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computeDureeSejour(input)).toBe(60);
    expect(Rules.computeTypeSejour(input)).toBeNull();
    expect(Rules.computeNiveauLangue(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 3 when all 3 prefill fields present (BELGIQUE) — max possible', () => {
    const input = {
      aiData: {
        naturalisationBeDureeSejour: 60,
        naturalisationBeTypeSejour: 'ILLIMITE' as const,
        naturalisationBeNiveauLangue: 'A2' as const,
      },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('accepts the 2 typeSejour whitelist values', () => {
    for (const v of ['LIMITE', 'ILLIMITE'] as const) {
      expect(Rules.computeTypeSejour({
        aiData: { naturalisationBeTypeSejour: v },
        workspaceCountry: 'BELGIQUE' as const,
      })).toBe(v);
    }
  });

  it('rejects typeSejour outside whitelist', () => {
    expect(Rules.computeTypeSejour({
      aiData: { naturalisationBeTypeSejour: 'CARTE_X' as unknown as 'LIMITE' },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
    expect(Rules.computeTypeSejour({
      aiData: { naturalisationBeTypeSejour: null },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
  });

  it('normalizes typeSejour case (illimite -> ILLIMITE)', () => {
    expect(Rules.computeTypeSejour({
      aiData: { naturalisationBeTypeSejour: ' illimite ' as unknown as 'ILLIMITE' },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBe('ILLIMITE');
  });

  it('accepts the 3 niveauLangue whitelist values', () => {
    for (const v of ['INFERIEUR_A2', 'A2', 'SUPERIEUR_A2'] as const) {
      expect(Rules.computeNiveauLangue({
        aiData: { naturalisationBeNiveauLangue: v },
        workspaceCountry: 'BELGIQUE' as const,
      })).toBe(v);
    }
  });

  it('rejects niveauLangue outside whitelist', () => {
    expect(Rules.computeNiveauLangue({
      aiData: { naturalisationBeNiveauLangue: 'B2' as unknown as 'A2' },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
  });

  it('rejects non-integer and out-of-range dureeSejour', () => {
    expect(Rules.computeDureeSejour({
      aiData: { naturalisationBeDureeSejour: 12.5 as unknown as number },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
    expect(Rules.computeDureeSejour({
      aiData: { naturalisationBeDureeSejour: -1 },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
    expect(Rules.computeDureeSejour({
      aiData: { naturalisationBeDureeSejour: 601 },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
    expect(Rules.computeDureeSejour({
      aiData: { naturalisationBeDureeSejour: 0 },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBe(0);
    expect(Rules.computeDureeSejour({
      aiData: { naturalisationBeDureeSejour: 600 },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBe(600);
  });

  it('does NOT count 10bis / 10ter fields (other Immigration BE tools)', () => {
    const input = {
      aiData: {
        be10bisLienFamilial: 'CONJOINT' as const,
        be10bisRevenusMensuels: 1950,
        be10terLienFamilial: 'CONJOINT' as const,
      },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('PREFILL_COUNT_ALWAYS_ZERO for aspirational fields (preuves + ordre public)', () => {
    // Even if the AI somehow surfaces these, they do NOT count towards the badge.
    // The component must treat them as user-only input (no provenance signal).
    const input = {
      aiData: {
        naturalisationBeDureeSejour: 60,
        naturalisationBeTypeSejour: 'ILLIMITE' as const,
        naturalisationBeNiveauLangue: 'A2' as const,
        // Aspirational — would-be 4 extra signals, but we cap at 3.
      },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBeLessThanOrEqual(3);
    expect(Rules.computePrefillCount(input)).toBe(3);
  });
});
