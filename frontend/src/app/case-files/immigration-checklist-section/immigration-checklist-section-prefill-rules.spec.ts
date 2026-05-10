import { ImmigrationChecklistPrefillRules as Rules, KNOWN_TITRE_TYPES } from './immigration-checklist-section-prefill-rules';

describe('ImmigrationChecklistPrefillRules', () => {
  // ── Cas 0 ─────────────────────────────────────────────────────────────
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when aiData is empty object', () => {
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when inferredChecklistType is missing or non-string', () => {
    expect(Rules.computePrefillCount({ aiData: { inferredChecklistType: null } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { inferredChecklistType: 42 } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { inferredChecklistType: '' } })).toBe(0);
  });

  it('returns 0 when inferredChecklistType is not in KNOWN_TITRE_TYPES', () => {
    expect(Rules.computePrefillCount({ aiData: { inferredChecklistType: 'BOGUS_TYPE' } })).toBe(0);
    expect(Rules.computeInferredChecklistType({ aiData: { inferredChecklistType: 'BOGUS_TYPE' } })).toBeNull();
  });

  // ── Cas M (partiel) — N=1 mono-champ, donc M=N=1 ──────────────────────
  it('returns 1 when inferredChecklistType is a known régime (M=N=1)', () => {
    const input = { aiData: { inferredChecklistType: 'VISA_ETUDIANT' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeInferredChecklistType(input)).toBe('VISA_ETUDIANT');
  });

  // ── Cas N (nominal) ───────────────────────────────────────────────────
  it('returns 1 for each of the 13 known régimes', () => {
    for (const t of KNOWN_TITRE_TYPES) {
      expect(Rules.computePrefillCount({ aiData: { inferredChecklistType: t } })).toBe(1);
    }
  });

  it('KNOWN_TITRE_TYPES contains exactly 13 régimes', () => {
    expect(KNOWN_TITRE_TYPES.size).toBe(13);
  });
});
