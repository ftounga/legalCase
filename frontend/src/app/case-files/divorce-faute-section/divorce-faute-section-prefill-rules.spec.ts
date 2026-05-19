/**
 * F-237 SF-237-02 — Tests `DivorceFautePrefillRules`.
 */
import {
  DivorceFautePrefillRules,
  computePrefillCount,
  computeDureeMariage,
  computeRevenusDemandeur,
  computeRevenusDefendeur,
  computeDateDepotAssignation,
  computeFautesDetectees,
  VALID_FAUTE_CODES,
} from './divorce-faute-section-prefill-rules';

describe('DivorceFautePrefillRules', () => {
  it('cas 0 — retourne 0 quand vide', () => {
    expect(computePrefillCount({})).toBe(0);
    expect(computePrefillCount({ aiData: null })).toBe(0);
    expect(computePrefillCount({ aiData: undefined })).toBe(0);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    expect(computePrefillCount({ aiData: {} as any })).toBe(0);
  });

  it('cas M — partiel (durée + revenus demandeur = 2/5)', () => {
    expect(
      computePrefillCount({
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        aiData: { dureeMariageAnnees: 12, revenusAnnuelsDemandeurEur: 45000 } as any,
      }),
    ).toBe(2);
  });

  it('cas N — nominal 5/5', () => {
    expect(
      computePrefillCount({
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        aiData: {
          dureeMariageAnnees: 15,
          revenusAnnuelsDemandeurEur: 52000,
          revenusAnnuelsDefendeurEur: 38000,
          dateDepotAssignation: '2026-03-15',
          // SF-246-03 : `fautesDetectees` est désormais un champ réel du type.
          fautesDetectees: ['ADULTERE', 'VIOLENCES'],
        } as any,
      }),
    ).toBe(5);
  });

  // ── computeDureeMariage ────────────────────────────────────────────────
  it('computeDureeMariage — rejette 0 et négatifs', () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    expect(computeDureeMariage({ aiData: { dureeMariageAnnees: 0 } as any })).toBeNull();
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    expect(computeDureeMariage({ aiData: { dureeMariageAnnees: -3 } as any })).toBeNull();
  });

  // ── computeRevenus* ────────────────────────────────────────────────────
  it('computeRevenusDemandeur — rejette ≤ 0', () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    expect(computeRevenusDemandeur({ aiData: { revenusAnnuelsDemandeurEur: 0 } as any })).toBeNull();
  });

  it('computeRevenusDefendeur — accepte valeur positive', () => {
    expect(
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      computeRevenusDefendeur({ aiData: { revenusAnnuelsDefendeurEur: 41000 } as any }),
    ).toBe(41000);
  });

  // ── computeDateDepotAssignation ────────────────────────────────────────
  it('computeDateDepotAssignation — rejette chaîne vide', () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    expect(computeDateDepotAssignation({ aiData: { dateDepotAssignation: '' } as any })).toBeNull();
  });

  it('computeDateDepotAssignation — accepte ISO valide', () => {
    expect(
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      computeDateDepotAssignation({ aiData: { dateDepotAssignation: '2026-03-15' } as any }),
    ).toBe('2026-03-15');
  });

  // ── computeFautesDetectees ─────────────────────────────────────────────
  // SF-246-03 : `fautesDetectees` est désormais un champ réel de FamilleExtractedData.
  it('computeFautesDetectees — filtre codes invalides', () => {
    expect(
      computeFautesDetectees({
        aiData: { fautesDetectees: ['ADULTERE', 'PAS_VALIDE', 'AUTRE'] },
      }),
    ).toEqual(['ADULTERE', 'AUTRE']);
  });

  it('computeFautesDetectees — case-insensible (uppercase)', () => {
    expect(
      computeFautesDetectees({ aiData: { fautesDetectees: ['adultere'] } }),
    ).toEqual(['ADULTERE']);
  });

  it('computeFautesDetectees — tableau filtré vide → null', () => {
    expect(
      computeFautesDetectees({
        aiData: { fautesDetectees: ['BIDON', 'AUTRE_BIDON'] },
      }),
    ).toBeNull();
  });

  it('computeFautesDetectees — violences + abandon — cas nominal SF-246-03', () => {
    expect(
      computeFautesDetectees({
        aiData: { fautesDetectees: ['VIOLENCES', 'ABANDON'] },
      }),
    ).toEqual(['VIOLENCES', 'ABANDON']);
  });

  it('computeFautesDetectees — null quand aiData null', () => {
    expect(computeFautesDetectees({ aiData: null })).toBeNull();
    expect(computeFautesDetectees({ aiData: undefined })).toBeNull();
    expect(computeFautesDetectees({})).toBeNull();
  });

  it('computeFautesDetectees — null quand fautesDetectees null', () => {
    expect(
      computeFautesDetectees({ aiData: { fautesDetectees: null } }),
    ).toBeNull();
  });

  it('VALID_FAUTE_CODES contient les 8 codes attendus', () => {
    expect(VALID_FAUTE_CODES.size).toBe(8);
    expect(VALID_FAUTE_CODES.has('ADULTERE')).toBe(true);
    expect(VALID_FAUTE_CODES.has('AUTRE')).toBe(true);
  });

  it('expose surface complète', () => {
    expect(DivorceFautePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
