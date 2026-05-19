import { AesEtudiantPrefillRules as Rules } from './aes-etudiant-section-prefill-rules';

describe('AesEtudiantPrefillRules', () => {
  // --- helpers existants ---
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
  });

  it('returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(Rules.computePrefillCount({
      aiData: { dateDepotProcedure: '2026-04-01' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('returns 0 for future dateDepotProcedure or malformed', () => {
    expect(Rules.computePrefillCount({ aiData: { dateDepotProcedure: '2099-12-31' } })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: { dateDepotProcedure: 'bad' } })).toBe(0);
  });

  it('returns 1 when only dateDepotProcedure is set', () => {
    expect(Rules.computePrefillCount({ aiData: { dateDepotProcedure: '2026-01-15' } })).toBe(1);
  });

  // --- SF-246-18 : computeDateEntreeFrance ---
  it('computeDateEntreeFrance returns null when aesDateEntreeFrance absent', () => {
    expect(Rules.computeDateEntreeFrance({ aiData: {} })).toBeNull();
  });

  it('computeDateEntreeFrance returns null for future date', () => {
    expect(Rules.computeDateEntreeFrance({ aiData: { aesDateEntreeFrance: '2099-01-01' } })).toBeNull();
  });

  it('computeDateEntreeFrance returns date when valid past ISO', () => {
    const result = Rules.computeDateEntreeFrance({ aiData: { aesDateEntreeFrance: '2021-03-15' } });
    expect(result).toBe('2021-03-15');
  });

  // --- SF-246-18 : computeDureePresenceMois ---
  it('computeDureePresenceMois returns null when aesDureePresenceMois absent', () => {
    expect(Rules.computeDureePresenceMois({ aiData: {} })).toBeNull();
  });

  it('computeDureePresenceMois returns null for negative value', () => {
    expect(Rules.computeDureePresenceMois({ aiData: { aesDureePresenceMois: -1 } })).toBeNull();
  });

  it('computeDureePresenceMois returns value when valid integer >= 0', () => {
    expect(Rules.computeDureePresenceMois({ aiData: { aesDureePresenceMois: 38 } })).toBe(38);
  });

  it('computeDureePresenceMois returns 0 when zero months', () => {
    expect(Rules.computeDureePresenceMois({ aiData: { aesDureePresenceMois: 0 } })).toBe(0);
  });

  // --- SF-246-18 : computeAnneesScolariteConsecutives ---
  it('computeAnneesScolariteConsecutives returns null when absent', () => {
    expect(Rules.computeAnneesScolariteConsecutives({ aiData: {} })).toBeNull();
  });

  it('computeAnneesScolariteConsecutives returns value for valid integer', () => {
    expect(Rules.computeAnneesScolariteConsecutives({ aiData: { aesAnneesScolariteConsecutives: 3 } })).toBe(3);
  });

  it('computeAnneesScolariteConsecutives returns null for > 20', () => {
    expect(Rules.computeAnneesScolariteConsecutives({ aiData: { aesAnneesScolariteConsecutives: 21 } })).toBeNull();
  });

  // --- SF-246-18 : computeNiveauEtudes ---
  it('computeNiveauEtudes returns null when absent', () => {
    expect(Rules.computeNiveauEtudes({ aiData: {} })).toBeNull();
  });

  it('computeNiveauEtudes returns null for unknown code', () => {
    expect(Rules.computeNiveauEtudes({ aiData: { aesNiveauEtudes: 'MASTER_2' } })).toBeNull();
  });

  it.each([
    ['LYCEE'],
    ['BAC_PLUS_1_2'],
    ['BAC_PLUS_3_4'],
    ['BAC_PLUS_5_PLUS'],
  ])('computeNiveauEtudes returns %s when valid', (code) => {
    expect(Rules.computeNiveauEtudes({ aiData: { aesNiveauEtudes: code } })).toBe(code);
  });

  it('computeNiveauEtudes is case-insensitive', () => {
    expect(Rules.computeNiveauEtudes({ aiData: { aesNiveauEtudes: 'lycee' } })).toBe('LYCEE');
  });

  // --- computePrefillCount ---
  it('computePrefillCount returns 5 when all 5 fields present', () => {
    const input = {
      aiData: {
        aesDateEntreeFrance: '2021-03-15',
        aesDureePresenceMois: 38,
        aesAnneesScolariteConsecutives: 3,
        aesNiveauEtudes: 'BAC_PLUS_3_4',
        dateDepotProcedure: '2026-01-15',
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(5);
  });

  it('computePrefillCount returns 1 when only dateDepotProcedure present', () => {
    expect(Rules.computePrefillCount({ aiData: { dateDepotProcedure: '2026-01-15' } })).toBe(1);
  });

  it('computePrefillCount returns 0 for BE', () => {
    expect(Rules.computePrefillCount({
      aiData: { aesDateEntreeFrance: '2021-03-15', aesDureePresenceMois: 38 },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });
});
