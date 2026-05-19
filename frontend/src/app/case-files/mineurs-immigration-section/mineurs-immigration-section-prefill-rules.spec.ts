import { MineursImmigrationPrefillRules as Rules } from './mineurs-immigration-section-prefill-rules';

describe('MineursImmigrationPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
  });

  it('returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(Rules.computePrefillCount({
      aiData: { mineursDateNaissance: '2010-01-01' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  // ── SF-246-19 : computeDateNaissance (typed) ──────────────────────────
  it('computeDateNaissance returns null when absent', () => {
    expect(Rules.computeDateNaissance({ aiData: {} })).toBeNull();
  });

  it('computeDateNaissance returns null when empty string', () => {
    expect(Rules.computeDateNaissance({ aiData: { mineursDateNaissance: '' } })).toBeNull();
  });

  it('computeDateNaissance returns value when present', () => {
    expect(Rules.computeDateNaissance({ aiData: { mineursDateNaissance: '2010-05-15' } })).toBe('2010-05-15');
  });

  it('computeDateNaissance returns null for BELGIQUE', () => {
    expect(Rules.computeDateNaissance({
      aiData: { mineursDateNaissance: '2010-05-15' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  // ── computeDateEntreeFrance via aesDateEntreeFrance (typed) ───────────
  it('computeDateEntreeFrance returns null when absent', () => {
    expect(Rules.computeDateEntreeFrance({ aiData: {} })).toBeNull();
  });

  it('computeDateEntreeFrance returns value from aesDateEntreeFrance', () => {
    expect(Rules.computeDateEntreeFrance({ aiData: { aesDateEntreeFrance: '2020-09-01' } })).toBe('2020-09-01');
  });

  // ── computeNationalite via nationalite (typed) ────────────────────────
  it('computeNationalite returns null when absent', () => {
    expect(Rules.computeNationalite({ aiData: {} })).toBeNull();
  });

  it('computeNationalite returns value from nationalite', () => {
    expect(Rules.computeNationalite({ aiData: { nationalite: 'Algérienne' } })).toBe('Algérienne');
  });

  it('computeNationalite returns null for empty string', () => {
    expect(Rules.computeNationalite({ aiData: { nationalite: '' } })).toBeNull();
  });

  // ── computePrefillCount ────────────────────────────────────────────────
  it('returns 1 for single field present', () => {
    expect(Rules.computePrefillCount({ aiData: { mineursDateNaissance: '2010-01-01' } })).toBe(1);
    expect(Rules.computePrefillCount({ aiData: { aesDateEntreeFrance: '2020-09-01' } })).toBe(1);
    expect(Rules.computePrefillCount({ aiData: { nationalite: 'Marocaine' } })).toBe(1);
  });

  it('returns 3 when all three fields present', () => {
    const input = {
      aiData: {
        mineursDateNaissance: '2010-01-01',
        aesDateEntreeFrance: '2020-09-01',
        nationalite: 'Algérienne',
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('returns 0 for BELGIQUE even with all fields', () => {
    expect(Rules.computePrefillCount({
      aiData: {
        mineursDateNaissance: '2010-01-01',
        aesDateEntreeFrance: '2020-09-01',
        nationalite: 'Algérienne',
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });
});
