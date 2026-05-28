import { AesmMenaBePrefillRules as Rules } from './aesm-mena-be-section-prefill-rules';

describe('AesmMenaBePrefillRules', () => {
  it('returns 0 when no aiData (gate BE manquant)', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is FRANCE (mono-pays BE)', () => {
    const input = {
      aiData: {
        menaAge: 15,
        menaDateArrivee: '2024-01-15',
        menaDureeScolaire: 18,
      },
      workspaceCountry: 'FRANCE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 2 when only age + dateArrivee present (BELGIQUE) — partial 2/3', () => {
    const input = {
      aiData: { menaAge: 15, menaDateArrivee: '2024-01-15' },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computeAgeActuel(input)).toBe(15);
    expect(Rules.computeDateArriveeBelgique(input)).toBe('2024-01-15');
    expect(Rules.computeDureeScolaire(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('returns 3 when all 3 prefill fields present (BELGIQUE) — max possible', () => {
    const input = {
      aiData: {
        menaAge: 16,
        menaDateArrivee: '2023-09-01',
        menaDureeScolaire: 24,
      },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('rejects ageActuel out of bounds (>17, <0, non-integer)', () => {
    const baseAi = { menaAge: 18 };
    expect(Rules.computeAgeActuel({
      aiData: baseAi, workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
    expect(Rules.computeAgeActuel({
      aiData: { menaAge: -1 }, workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
    expect(Rules.computeAgeActuel({
      aiData: { menaAge: 15.5 as unknown as number }, workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
    expect(Rules.computeAgeActuel({
      aiData: { menaAge: 0 }, workspaceCountry: 'BELGIQUE' as const,
    })).toBe(0);
    expect(Rules.computeAgeActuel({
      aiData: { menaAge: 17 }, workspaceCountry: 'BELGIQUE' as const,
    })).toBe(17);
  });

  it('accepts ISO yyyy-MM-dd dateArrivee and rejects invalid formats', () => {
    expect(Rules.computeDateArriveeBelgique({
      aiData: { menaDateArrivee: '2024-01-15' },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBe('2024-01-15');
    expect(Rules.computeDateArriveeBelgique({
      aiData: { menaDateArrivee: '15/01/2024' as unknown as string },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
    expect(Rules.computeDateArriveeBelgique({
      aiData: { menaDateArrivee: '' },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
    expect(Rules.computeDateArriveeBelgique({
      aiData: { menaDateArrivee: null },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
  });

  it('rejects non-integer and out-of-range dureeScolaire', () => {
    expect(Rules.computeDureeScolaire({
      aiData: { menaDureeScolaire: 12.5 as unknown as number },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
    expect(Rules.computeDureeScolaire({
      aiData: { menaDureeScolaire: -1 },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
    expect(Rules.computeDureeScolaire({
      aiData: { menaDureeScolaire: 121 },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBeNull();
    expect(Rules.computeDureeScolaire({
      aiData: { menaDureeScolaire: 0 },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBe(0);
    expect(Rules.computeDureeScolaire({
      aiData: { menaDureeScolaire: 120 },
      workspaceCountry: 'BELGIQUE' as const,
    })).toBe(120);
  });

  it('does NOT count other Immigration BE fields (9bis / 12bis / art16 / 10ter)', () => {
    const input = {
      aiData: {
        be9bisDateEntreeBelgique: '2020-01-01',
        naturalisationBeDureeSejour: 60,
        naturalisationBeArt16DateMarriage: '2020-05-15',
        be10terLienFamilial: 'CONJOINT' as const,
      },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('PREFILL_COUNT_ALWAYS_ZERO for aspirational fields (5 checkboxes)', () => {
    // Even if the AI somehow surfaces them, they do NOT count towards the badge.
    // Only the 3 REAL signals (menaAge, menaDateArrivee, menaDureeScolaire) increment.
    const input = {
      aiData: {
        menaAge: 16,
        menaDateArrivee: '2023-09-01',
        menaDureeScolaire: 24,
        // Aspirational — must NOT count
        tuteurDesigne: true,
        integrationScolaire: true,
        projetVieElabore: true,
        perspectiveAutonomie: true,
        menaceOrdrePublic: true,
      },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBeLessThanOrEqual(3);
    expect(Rules.computePrefillCount(input)).toBe(3);
  });
});
