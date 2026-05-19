import { AesFamillePrefillRules as Rules } from './aes-famille-section-prefill-rules';

describe('AesFamillePrefillRules', () => {
  // --- helpers existants ---
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE', () => {
    const input = {
      aiData: { aesDateEntreeFrance: '2020-01-01' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 0 when aesDateEntreeFrance is too short or non-string', () => {
    expect(Rules.computePrefillCount({ aiData: { aesDateEntreeFrance: '2020' } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { aesDateEntreeFrance: 42 } })).toBe(0);
  });

  it('returns 2 when only aesDateEntreeFrance provided (date + dureePresence)', () => {
    const input = { aiData: { aesDateEntreeFrance: '2020-01-15' } };
    expect(Rules.computePrefillCount(input)).toBe(2);
    expect(Rules.computeDateEntreeFrance(input)).toBe('2020-01-15');
    expect(Rules.computeDureePresenceMois(input)).toBeGreaterThan(0);
  });

  // --- SF-246-18 : computeDureeScolaritePlusAncienEnfant ---
  it('computeDureeScolaritePlusAncienEnfant returns null when absent', () => {
    expect(Rules.computeDureeScolaritePlusAncienEnfant({ aiData: {} })).toBeNull();
  });

  it('computeDureeScolaritePlusAncienEnfant returns value for valid integer', () => {
    expect(Rules.computeDureeScolaritePlusAncienEnfant({ aiData: { aesDureeScolaritePlusAncienEnfantAnnees: 4 } })).toBe(4);
  });

  it('computeDureeScolaritePlusAncienEnfant returns null for > 30 (hors plage)', () => {
    expect(Rules.computeDureeScolaritePlusAncienEnfant({ aiData: { aesDureeScolaritePlusAncienEnfantAnnees: 31 } })).toBeNull();
  });

  it('computeDureeScolaritePlusAncienEnfant returns 0 for zero years', () => {
    expect(Rules.computeDureeScolaritePlusAncienEnfant({ aiData: { aesDureeScolaritePlusAncienEnfantAnnees: 0 } })).toBe(0);
  });

  // --- SF-246-18 : computeDateDepotDemande ---
  it('computeDateDepotDemande returns null when absent', () => {
    expect(Rules.computeDateDepotDemande({ aiData: {} })).toBeNull();
  });

  it('computeDateDepotDemande returns null for future date', () => {
    expect(Rules.computeDateDepotDemande({ aiData: { dateDepotProcedure: '2099-01-01' } })).toBeNull();
  });

  it('computeDateDepotDemande returns ISO date for valid past date', () => {
    expect(Rules.computeDateDepotDemande({ aiData: { dateDepotProcedure: '2026-01-15' } })).toBe('2026-01-15');
  });

  // --- computePrefillCount ---
  it('returns 4 when all 4 fields present', () => {
    const input = {
      aiData: {
        aesDateEntreeFrance: '2018-09-01',
        aesDureeScolaritePlusAncienEnfantAnnees: 5,
        dateDepotProcedure: '2026-01-15',
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(4);
  });

  it('returns 2 when only aesDateEntreeFrance present', () => {
    expect(Rules.computePrefillCount({ aiData: { aesDateEntreeFrance: '2018-09-01' } })).toBe(2);
  });

  it('returns 0 for BE even with all fields', () => {
    expect(Rules.computePrefillCount({
      aiData: { aesDateEntreeFrance: '2018-09-01', aesDureeScolaritePlusAncienEnfantAnnees: 5 },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });
});
