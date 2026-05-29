import { RetraitTitreFraudePrefillRules as Rules } from './retrait-titre-fraude-section-prefill-rules';

describe('RetraitTitreFraudePrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { retraitTitreDateRetrait: '2026-01-15', retraitTitreMotif: 'MARIAGE_GRIS' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateRetrait(input)).toBeNull();
    expect(Rules.computeMotifRetrait(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 when only the date is valid (FRANCE)', () => {
    const input = { aiData: { retraitTitreDateRetrait: '2026-01-15' } };
    expect(Rules.computeDateRetrait(input)).toBe('2026-01-15');
    expect(Rules.computeMotifRetrait(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 2 when both date and motif are valid (FRANCE)', () => {
    const input = {
      aiData: { retraitTitreDateRetrait: '2026-01-15', retraitTitreMotif: 'FRAUDE_DOCUMENTAIRE' },
    };
    expect(Rules.computeDateRetrait(input)).toBe('2026-01-15');
    expect(Rules.computeMotifRetrait(input)).toBe('FRAUDE_DOCUMENTAIRE');
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('trims surrounding whitespace on the ISO date', () => {
    expect(Rules.computeDateRetrait({ aiData: { retraitTitreDateRetrait: '  2026-02-01  ' } }))
      .toBe('2026-02-01');
    expect(Rules.computePrefillCount({ aiData: { retraitTitreDateRetrait: '  2026-02-01  ' } })).toBe(1);
  });

  it('rejects non-ISO / non-string / null dates', () => {
    expect(Rules.computeDateRetrait({ aiData: { retraitTitreDateRetrait: '15/01/2026' } })).toBeNull();
    expect(Rules.computeDateRetrait({ aiData: { retraitTitreDateRetrait: '2026-1-5' } })).toBeNull();
    expect(Rules.computeDateRetrait({
      aiData: { retraitTitreDateRetrait: 20260115 as unknown as string },
    })).toBeNull();
    expect(Rules.computeDateRetrait({ aiData: { retraitTitreDateRetrait: null } })).toBeNull();
  });

  it('rejects an out-of-whitelist motif', () => {
    expect(Rules.computeMotifRetrait({ aiData: { retraitTitreMotif: 'AUTRE' } })).toBeNull();
    expect(Rules.computeMotifRetrait({ aiData: { retraitTitreMotif: 123 as unknown as string } })).toBeNull();
    expect(Rules.computeMotifRetrait({ aiData: { retraitTitreMotif: null } })).toBeNull();
  });

  it('accepts every whitelisted motif', () => {
    for (const m of ['MARIAGE_GRIS', 'FAUSSES_DECLARATIONS', 'FRAUDE_DOCUMENTAIRE', 'PERTE_CONDITIONS']) {
      expect(Rules.computeMotifRetrait({ aiData: { retraitTitreMotif: m } })).toBe(m);
    }
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    expect(Rules.computePrefillCount({ aiData: { retraitTitreDateRetrait: '2026-03-10' } })).toBe(1);
  });
});
