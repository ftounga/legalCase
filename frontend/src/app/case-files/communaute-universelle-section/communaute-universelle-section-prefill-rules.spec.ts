/**
 * F-236 SF-236-02 — Tests `CommunauteUniversellePrefillRules`.
 * SF-246-07 : mise à jour suite réalignement sur le record réel `FamilleExtractedData`.
 */
import {
  CommunauteUniversellePrefillRules,
  computePrefillCount,
  computeValeurCommunaute,
} from './communaute-universelle-section-prefill-rules';

describe('CommunauteUniversellePrefillRules', () => {
  describe('computePrefillCount — cas 0', () => {
    it('retourne 0 pour un input vide', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('retourne 0 pour aiData null', () => {
      expect(computePrefillCount({ aiData: null })).toBe(0);
    });

    it('retourne 0 pour workspaceCountry BELGIQUE (garde BE)', () => {
      expect(
        computePrefillCount({
          aiData: {
            contratNotarieDetected: true,
            valeurCommunauteEurDetectee: 200000,
          } as any,
          workspaceCountry: 'BELGIQUE',
        }),
      ).toBe(0);
    });
  });

  describe('computePrefillCount — cas M', () => {
    it('2/4 (booléens seulement)', () => {
      expect(
        computePrefillCount({
          aiData: { contratNotarieDetected: true, enfantsNonCommunsDetected: false } as any,
        }),
      ).toBe(2);
    });

    it('valeur nulle rejetée', () => {
      expect(
        computePrefillCount({ aiData: { valeurCommunauteEurDetectee: null } as any }),
      ).toBe(0);
    });
  });

  describe('computePrefillCount — cas N', () => {
    it('4/4 — tous les champs présents dont valeurCommunauteEurDetectee (SF-246-07)', () => {
      expect(
        computePrefillCount({
          aiData: {
            contratNotarieDetected: true,
            enfantsNonCommunsDetected: false,
            clauseAttributionIntegraleDetected: true,
            valeurCommunauteEurDetectee: 200000,
          } as any,
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(4);
    });
  });

  describe('computeValeurCommunaute — invariant §5.2 (jamais 0)', () => {
    it('retourne null pour 0', () => {
      expect(computeValeurCommunaute({ aiData: { valeurCommunauteEurDetectee: 0 } as any })).toBeNull();
    });

    it('retourne null pour valeur négative', () => {
      expect(computeValeurCommunaute({ aiData: { valeurCommunauteEurDetectee: -1 } as any })).toBeNull();
    });

    it('retourne la valeur pour un montant strictement positif', () => {
      expect(computeValeurCommunaute({ aiData: { valeurCommunauteEurDetectee: 350000 } as any })).toBe(350000);
    });
  });

  it('surface', () => {
    expect(CommunauteUniversellePrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
