import { VictimeTraiteBePrefillRules as Rules } from './victime-traite-be-section-prefill-rules';

describe('VictimeTraiteBePrefillRules', () => {
  it('returns 0 when no aiData (gate BE manquant)', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is FRANCE (mono-pays BE)', () => {
    const input = {
      aiData: {
        victimeTraitePhase: 'DECLARATION_FAITE' as const,
        victimeTraiteRupture: true,
        victimeTraiteAccompagnement: true,
      },
      workspaceCountry: 'FRANCE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computePhase(input)).toBeNull();
    expect(Rules.computeRupture(input)).toBeNull();
    expect(Rules.computeAccompagnement(input)).toBeNull();
  });

  it('returns 1 when only phase present (BELGIQUE)', () => {
    const input = {
      aiData: { victimeTraitePhase: 'REFLEXION_45J' as const },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePhase(input)).toBe('REFLEXION_45J');
    expect(Rules.computeRupture(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 3 (nominal) when all real fields present (BELGIQUE)', () => {
    const input = {
      aiData: {
        victimeTraitePhase: 'PROCEDURE_PENALE_EN_COURS' as const,
        victimeTraiteRupture: true,
        victimeTraiteAccompagnement: false,
      },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePhase(input)).toBe('PROCEDURE_PENALE_EN_COURS');
    expect(Rules.computeRupture(input)).toBe(true);
    expect(Rules.computeAccompagnement(input)).toBe(false);
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('counts a false boolean as a real pré-fill (tri-state)', () => {
    const input = {
      aiData: { victimeTraiteRupture: false, victimeTraiteAccompagnement: false },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computeRupture(input)).toBe(false);
    expect(Rules.computeAccompagnement(input)).toBe(false);
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('rejects a phase value outside the whitelist', () => {
    const input = {
      aiData: { victimeTraitePhase: 'PHASE_INEXISTANTE' as unknown as 'AUCUNE' },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePhase(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('does NOT count a non-boolean value for rupture/accompagnement', () => {
    const input = {
      aiData: {
        victimeTraiteRupture: 'oui' as unknown as boolean,
        victimeTraiteAccompagnement: 1 as unknown as boolean,
      },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computeRupture(input)).toBeNull();
    expect(Rules.computeAccompagnement(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('does NOT count aspirational fields (cooperation / dateDebut)', () => {
    const input = {
      aiData: {
        victimeTraitePhase: 'DECLARATION_FAITE' as const,
        victimeTraiteRupture: true,
        victimeTraiteAccompagnement: true,
        // simulate AI accidentally returning aspirational fields — must be ignored:
        cooperationJudiciaire: true,
        dateDebutAccompagnement: '2026-05-30',
      } as unknown as Record<string, unknown>,
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('does NOT count the FR victime de la traite field (F-IM-35)', () => {
    const input = {
      aiData: {
        victimeTraiteDetectee: true, // FR pivot (double e) — distinct du régime BE
      } as unknown as Record<string, unknown>,
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });
});
