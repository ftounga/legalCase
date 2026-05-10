import { DivorceChecklistPrefillRules as Rules } from './divorce-checklist-section-prefill-rules';

describe('DivorceChecklistPrefillRules', () => {
  // ── Cas 0 ─────────────────────────────────────────────────────────────
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: undefined })).toBe(0);
  });

  it('returns 0 when aiData is empty object', () => {
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when dateAcceptationPV is not a string', () => {
    expect(Rules.computePrefillCount({ aiData: { dateAcceptationPV: 123 } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { dateAcceptationPV: null } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { dateAcceptationPV: undefined } })).toBe(0);
  });

  it('returns 0 when dateAcceptationPV is malformed (not ISO YYYY-MM-DD)', () => {
    expect(Rules.computePrefillCount({ aiData: { dateAcceptationPV: '2026' } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { dateAcceptationPV: '12/04/2026' } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { dateAcceptationPV: '' } })).toBe(0);
    // Note : la regex ne valide que le format syntaxique (YYYY-MM-DD), pas la validité calendaire.
    // `2026-13-01` est syntaxiquement valide pour le helper — c'est cohérent avec
    // le format strict de l'API backend (qui valide la cohérence calendaire en amont).
  });

  it('computeDateAcceptationPV returns null when not pré-remplissable', () => {
    expect(Rules.computeDateAcceptationPV({})).toBeNull();
    expect(Rules.computeDateAcceptationPV({ aiData: {} })).toBeNull();
    expect(Rules.computeDateAcceptationPV({ aiData: { dateAcceptationPV: 'invalid' } })).toBeNull();
  });

  // ── Cas N (nominal) ───────────────────────────────────────────────────
  it('returns N=2 when dateAcceptationPV is a valid ISO date (FR + BE signature steps pré-cochées)', () => {
    const input = { aiData: { dateAcceptationPV: '2026-04-15' } };
    expect(Rules.computePrefillCount(input)).toBe(2);
    expect(Rules.computeDateAcceptationPV(input)).toBe('2026-04-15');
  });

  it('still returns N=2 with surrounding fields (count = signature steps × 1)', () => {
    const input = {
      aiData: {
        dateAcceptationPV: '2026-01-01',
        regimeMatrimonialDetecte: 'COMMUNAUTE_REDUITE_AUX_ACQUETS',
        anotherField: 'ignored',
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('exposes SIGNATURE_STEP_CODES as readonly tuple with 2 entries', () => {
    expect(Rules.SIGNATURE_STEP_CODES.length).toBe(2);
    expect(Rules.SIGNATURE_STEP_CODES).toContain('FR_SIGNATURE_CONVENTION');
    expect(Rules.SIGNATURE_STEP_CODES).toContain('BE_REDACTION_CONVENTION');
  });
});
