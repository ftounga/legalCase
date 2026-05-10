/**
 * F-236 SF-236-02 — Tests `PartageImmobilierPrefillRules`.
 * Cas obligatoires : (a) 0 champs, (b) M partiels, (c) N nominal.
 */
import {
  PartageImmobilierPrefillRules,
  computeCapitalRestantDu,
  computePrefillCount,
  computeValeurImmeuble,
} from './partage-immobilier-section-prefill-rules';
import { LiquidationCommunaute } from '../../core/models/case-analysis.model';

describe('PartageImmobilierPrefillRules', () => {
  describe('cas 0 — input vide', () => {
    it('retourne 0 quand input vide', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('retourne 0 quand aiData null + liquidation null', () => {
      expect(computePrefillCount({ aiData: null, liquidationCommunaute: null })).toBe(0);
    });
  });

  describe('cas M — partiels', () => {
    it('retourne 1 quand seulement valeurImmeuble présent', () => {
      expect(
        computePrefillCount({ aiData: { valeurImmeuble: 250000 } }),
      ).toBe(1);
    });

    it('retourne 1 quand seulement capitalRestantDu présent', () => {
      expect(computePrefillCount({ aiData: { capitalRestantDu: 0 } })).toBe(1);
    });

    it('retourne 0 quand valeurImmeuble <= 0 (sentinelle "non saisi")', () => {
      expect(computePrefillCount({ aiData: { valeurImmeuble: 0 } })).toBe(0);
    });

    it('retourne 0 quand fallback liquidation ambigu (2 biens immo)', () => {
      const liq: Partial<LiquidationCommunaute> = {
        actifCommun: [
          { libelle: 'Maison Paris', valeur: 300000 } as unknown as never,
          { libelle: 'Appartement Lyon', valeur: 200000 } as unknown as never,
        ],
      };
      expect(
        computePrefillCount({ liquidationCommunaute: liq as LiquidationCommunaute }),
      ).toBe(0);
    });
  });

  describe('cas N — nominal', () => {
    it('retourne 2 quand aiData fournit valeur + capital', () => {
      expect(
        computePrefillCount({
          aiData: { valeurImmeuble: 250000, capitalRestantDu: 80000 },
        }),
      ).toBe(2);
    });

    it('retourne 2 via fallback mono-bien + mono-pret', () => {
      const liq: Partial<LiquidationCommunaute> = {
        actifCommun: [
          { libelle: 'Appartement Lyon', valeur: 250000 } as unknown as never,
        ],
        passifCommun: [
          { libelle: 'Prêt immobilier BNP', valeur: 80000 } as unknown as never,
        ],
      };
      expect(
        computePrefillCount({ liquidationCommunaute: liq as LiquidationCommunaute }),
      ).toBe(2);
    });

    it('priorise aiData sur le fallback liquidation', () => {
      const liq: Partial<LiquidationCommunaute> = {
        actifCommun: [
          { libelle: 'Maison', valeur: 999999 } as unknown as never,
        ],
      };
      expect(
        computeValeurImmeuble({
          aiData: { valeurImmeuble: 250000 },
          liquidationCommunaute: liq as LiquidationCommunaute,
        }),
      ).toBe(250000);
    });
  });

  describe('computeCapitalRestantDu — sentinelles', () => {
    it('accepte capitalRestantDu = 0 (prêt soldé)', () => {
      expect(computeCapitalRestantDu({ aiData: { capitalRestantDu: 0 } })).toBe(0);
    });

    it('rejette capitalRestantDu négatif (incohérent)', () => {
      expect(computeCapitalRestantDu({ aiData: { capitalRestantDu: -10 } })).toBeNull();
    });
  });

  describe('surface PartageImmobilierPrefillRules', () => {
    it('expose les fonctions et constantes', () => {
      expect(PartageImmobilierPrefillRules.computePrefillCount).toBe(computePrefillCount);
      expect(PartageImmobilierPrefillRules.computeValeurImmeuble).toBe(computeValeurImmeuble);
      expect(PartageImmobilierPrefillRules.computeCapitalRestantDu).toBe(computeCapitalRestantDu);
      expect(Array.isArray(PartageImmobilierPrefillRules.IMMO_KEYWORDS)).toBe(true);
    });
  });
});
