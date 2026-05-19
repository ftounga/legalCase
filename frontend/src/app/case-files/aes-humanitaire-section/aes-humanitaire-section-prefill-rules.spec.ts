import { AesHumanitairePrefillRules as Rules } from './aes-humanitaire-section-prefill-rules';

describe('AesHumanitairePrefillRules', () => {
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

  it('returns 0 when dépôt antérieur à entrée France (rejet guard)', () => {
    const input = {
      aiData: { aesDateEntreeFrance: '2024-01-01', dateDepotProcedure: '2023-01-01' },
    };
    expect(Rules.computePrefillCount(input)).toBe(1); // seulement entrée
    expect(Rules.computeDateDepotDemande(input)).toBeNull();
  });

  it('returns 1 when only dateDepotProcedure (sans entrée)', () => {
    expect(Rules.computePrefillCount({ aiData: { dateDepotProcedure: '2026-04-01' } })).toBe(1);
  });

  it('returns 2 when aesDateEntreeFrance + dateDepotProcedure cohérents', () => {
    const input = {
      aiData: { aesDateEntreeFrance: '2020-01-01', dateDepotProcedure: '2026-04-01' },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  // --- SF-246-18 : computeMotifHumanitaire ---
  it('computeMotifHumanitaire returns null when absent', () => {
    expect(Rules.computeMotifHumanitaire({ aiData: {} })).toBeNull();
  });

  it('computeMotifHumanitaire returns null for unknown code', () => {
    expect(Rules.computeMotifHumanitaire({ aiData: { aesMotifHumanitaire: 'INCONNU' } })).toBeNull();
  });

  it.each([
    ['RISQUES_AU_RETOUR'],
    ['ISOLEMENT_TOTAL'],
    ['VICTIME_VIOLENCES'],
    ['VICTIME_TRAITE'],
    ['SITUATION_MEDICALE_PRECAIRE_HORS_L425_9'],
    ['AUTRE_HUMANITAIRE'],
  ])('computeMotifHumanitaire returns %s for valid code', (code) => {
    expect(Rules.computeMotifHumanitaire({ aiData: { aesMotifHumanitaire: code } })).toBe(code);
  });

  it('computeMotifHumanitaire is case-insensitive', () => {
    expect(Rules.computeMotifHumanitaire({ aiData: { aesMotifHumanitaire: 'victime_violences' } })).toBe('VICTIME_VIOLENCES');
  });

  it('computeMotifHumanitaire returns null for BELGIQUE', () => {
    expect(Rules.computeMotifHumanitaire({
      aiData: { aesMotifHumanitaire: 'RISQUES_AU_RETOUR' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  // --- computePrefillCount avec motif ---
  it('returns 3 when dateEntreeFrance + dateDepot + motif all present', () => {
    const input = {
      aiData: {
        aesDateEntreeFrance: '2020-01-01',
        dateDepotProcedure: '2026-04-01',
        aesMotifHumanitaire: 'VICTIME_VIOLENCES',
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('returns 0 for BELGIQUE with all fields', () => {
    expect(Rules.computePrefillCount({
      aiData: { aesDateEntreeFrance: '2020-01-01', aesMotifHumanitaire: 'RISQUES_AU_RETOUR' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });
});
