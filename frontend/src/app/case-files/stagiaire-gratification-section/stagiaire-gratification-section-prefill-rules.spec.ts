import { StagiaireGratificationPrefillRules as Rules } from './stagiaire-gratification-section-prefill-rules';

describe('StagiaireGratificationPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { dateDebutStage: '2026-01-06', dateFinStage: '2026-05-06' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateDebutStage(input)).toBeNull();
    expect(Rules.computeDateFinStage(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 (partiel) when only dateDebutStage present', () => {
    const input = { aiData: { dateDebutStage: '2026-01-06' } };
    expect(Rules.computeDateDebutStage(input)).toBe('2026-01-06');
    expect(Rules.computeDateFinStage(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 1 (partiel) when only dateFinStage present', () => {
    const input = { aiData: { dateFinStage: '2026-05-06' } };
    expect(Rules.computeDateFinStage(input)).toBe('2026-05-06');
    expect(Rules.computeDateDebutStage(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 2 (nominal) when both dates present', () => {
    const input = {
      aiData: { dateDebutStage: '2026-01-06', dateFinStage: '2026-05-06' },
    };
    expect(Rules.computeDateDebutStage(input)).toBe('2026-01-06');
    expect(Rules.computeDateFinStage(input)).toBe('2026-05-06');
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('truncates an ISO datetime to YYYY-MM-DD', () => {
    const input = { aiData: { dateDebutStage: '2026-01-06T08:30:00Z' } };
    expect(Rules.computeDateDebutStage(input)).toBe('2026-01-06');
  });

  it('rejects a non-string / non-ISO date', () => {
    expect(Rules.computeDateDebutStage({ aiData: { dateDebutStage: 20260106 as unknown as string } })).toBeNull();
    expect(Rules.computeDateDebutStage({ aiData: { dateDebutStage: 'janvier 2026' } })).toBeNull();
    expect(Rules.computeDateFinStage({ aiData: { dateFinStage: null } })).toBeNull();
  });

  it('does NOT count stageDetecte flag alone (visibility trigger, not a form field)', () => {
    const input = { aiData: { stageDetecte: true } };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    const input = {
      aiData: { dateDebutStage: '2026-01-06', dateFinStage: '2026-05-06' },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });
});
