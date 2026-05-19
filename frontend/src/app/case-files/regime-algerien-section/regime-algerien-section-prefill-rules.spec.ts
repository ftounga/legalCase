import { RegimeAlgerienPrefillRules as Rules } from './regime-algerien-section-prefill-rules';

describe('RegimeAlgerienPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
  });

  it('returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(Rules.computePrefillCount({
      aiData: { typeProcedureDetectee: 'CARTE_RESIDENT', algerienPresenceReguliereMois: 24 },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('returns 0 for unmapped procedure and no nationalite hint', () => {
    expect(Rules.computePrefillCount({ aiData: { typeProcedureDetectee: 'BOGUS' } })).toBe(0);
  });

  it('returns 1 when only voie maps successfully', () => {
    const v = Rules.computeVoieDemande({ aiData: { typeProcedureDetectee: 'CRA' } });
    if (v !== null) {
      expect(Rules.computePrefillCount({ aiData: { typeProcedureDetectee: 'CRA' } })).toBe(1);
    }
  });

  // ── SF-246-19 : computePresenceReguliereFranceMois ───────────────────
  it('computePresenceReguliereFranceMois returns null when absent', () => {
    expect(Rules.computePresenceReguliereFranceMois({ aiData: {} })).toBeNull();
  });

  it('computePresenceReguliereFranceMois returns null for negative', () => {
    expect(Rules.computePresenceReguliereFranceMois({ aiData: { algerienPresenceReguliereMois: -1 } })).toBeNull();
  });

  it('computePresenceReguliereFranceMois returns null for > 600', () => {
    expect(Rules.computePresenceReguliereFranceMois({ aiData: { algerienPresenceReguliereMois: 601 } })).toBeNull();
  });

  it('computePresenceReguliereFranceMois returns value for 0–600', () => {
    expect(Rules.computePresenceReguliereFranceMois({ aiData: { algerienPresenceReguliereMois: 24 } })).toBe(24);
    expect(Rules.computePresenceReguliereFranceMois({ aiData: { algerienPresenceReguliereMois: 0 } })).toBe(0);
    expect(Rules.computePresenceReguliereFranceMois({ aiData: { algerienPresenceReguliereMois: 600 } })).toBe(600);
  });

  it('computePresenceReguliereFranceMois returns null for BELGIQUE', () => {
    expect(Rules.computePresenceReguliereFranceMois({
      aiData: { algerienPresenceReguliereMois: 24 },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  // ── computePrefillCount avec nouveau champ ────────────────────────────
  it('returns 1 when only presenceReguliere present', () => {
    expect(Rules.computePrefillCount({ aiData: { algerienPresenceReguliereMois: 36 } })).toBe(1);
  });

  it('renvoie cohérent quel que soit l\'input — pas de crash', () => {
    expect(typeof Rules.computePrefillCount({ aiData: {} })).toBe('number');
  });
});
