/**
 * F-237 SF-237-02 — Tests `TransactionPrefillRules`.
 *
 * 3 cas obligatoires (contrat F-236 SF-236-01 §4) + cas additionnels pour
 * couvrir les branches non triviales (heuristique keyword, ordre dates).
 */
import {
  TransactionPrefillRules,
  computePrefillCount,
  computeSalaire,
  computeAnciennete,
  computeRupture,
} from './transaction-section-prefill-rules';

describe('TransactionPrefillRules', () => {
  it('cas 0 — retourne 0 quand vide', () => {
    expect(computePrefillCount({})).toBe(0);
    expect(computePrefillCount({ aiData: null })).toBe(0);
    expect(computePrefillCount({ aiData: undefined })).toBe(0);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    expect(computePrefillCount({ aiData: {} as any })).toBe(0);
  });

  it('cas M — partiel (salaire seul = 1/3)', () => {
    expect(
      computePrefillCount({
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        aiData: { salaireBrutMensuel: 3000 } as any,
      }),
    ).toBe(1);
  });

  it('cas M — partiel (rupture mappée seule = 1/3)', () => {
    expect(
      computePrefillCount({
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        aiData: { motifLicenciement: 'LICENCIEMENT_ECONOMIQUE' } as any,
      }),
    ).toBe(1);
  });

  it('cas N — nominal 3/3 avec dateLicenciement explicite', () => {
    expect(
      computePrefillCount({
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        aiData: {
          salaireBrutMensuel: 3200,
          dateEntree: '2020-01-15',
          dateLicenciement: '2025-01-15',
          motifLicenciement: 'LICENCIEMENT_PERSONNEL',
        } as any,
      }),
    ).toBe(3);
  });

  // ── Branches `computeSalaire` ─────────────────────────────────────────
  it('computeSalaire — rejette ≤ 0', () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    expect(computeSalaire({ aiData: { salaireBrutMensuel: 0 } as any })).toBeNull();
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    expect(computeSalaire({ aiData: { salaireBrutMensuel: -100 } as any })).toBeNull();
  });

  // ── Branches `computeAnciennete` ──────────────────────────────────────
  it('computeAnciennete — rejette dateEntree non-ISO', () => {
    expect(
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      computeAnciennete({ aiData: { dateEntree: '15/03/2020' } as any }),
    ).toBeNull();
  });

  it('computeAnciennete — rejette ordre inversé (entree > ref)', () => {
    expect(
      computeAnciennete({
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        aiData: { dateEntree: '2030-01-01', dateLicenciement: '2025-01-01' } as any,
      }),
    ).toBeNull();
  });

  it('computeAnciennete — utilise aujourd\'hui comme fallback si dateLicenciement absente', () => {
    // 2020 = il y a quelques années → forcément > 0.
    const v = computeAnciennete({
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      aiData: { dateEntree: '2020-01-15' } as any,
    });
    expect(typeof v).toBe('number');
    expect(v).toBeGreaterThanOrEqual(0);
  });

  it('computeAnciennete — années entières (arrondi inférieur)', () => {
    expect(
      computeAnciennete({
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        aiData: { dateEntree: '2020-01-15', dateLicenciement: '2025-01-14' } as any,
      }),
    ).toBe(4);
  });

  // ── Branches `computeRupture` ─────────────────────────────────────────
  it('computeRupture — match strict enum', () => {
    expect(
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      computeRupture({ aiData: { motifLicenciement: 'DEMISSION' } as any }),
    ).toBe('DEMISSION');
  });

  it('computeRupture — keywords (économique avec accent)', () => {
    expect(
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      computeRupture({ aiData: { motifLicenciement: 'motif économique grave' } as any }),
    ).toBe('LICENCIEMENT_ECONOMIQUE');
  });

  it('computeRupture — keywords (faute)', () => {
    expect(
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      computeRupture({ aiData: { motifLicenciement: 'faute grave avérée' } as any }),
    ).toBe('LICENCIEMENT_PERSONNEL');
  });

  it('computeRupture — pas de mapping → null', () => {
    expect(
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      computeRupture({ aiData: { motifLicenciement: 'autre chose inconnu' } as any }),
    ).toBeNull();
  });

  it('expose surface complète', () => {
    expect(TransactionPrefillRules.computePrefillCount).toBe(computePrefillCount);
    expect(TransactionPrefillRules.computeSalaire).toBe(computeSalaire);
    expect(TransactionPrefillRules.computeAnciennete).toBe(computeAnciennete);
    expect(TransactionPrefillRules.computeRupture).toBe(computeRupture);
  });
});
