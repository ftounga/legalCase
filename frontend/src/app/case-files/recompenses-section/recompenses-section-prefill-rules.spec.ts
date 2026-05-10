/**
 * F-236 SF-236-02 — Tests du helper partagé `RecompensesPrefillRules`.
 *
 * Garantit la stricte parité runtime/static : ces tests valident la logique
 * commune. Cas obligatoires (3) :
 *   (a) 0 champs — input vide / aiData absent
 *   (b) M champs partiels — régime invalide (SEPARATION_BIENS, inconnu)
 *   (c) N champs cas nominal — régime valide détecté
 */
import {
  RecompensesPrefillRules,
  computePrefillCount,
  computeRegimeMatrimonial,
  VALID_REGIMES,
} from './recompenses-section-prefill-rules';

describe('RecompensesPrefillRules', () => {
  describe('computePrefillCount — 0 champs', () => {
    it('retourne 0 quand input vide', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('retourne 0 quand aiData est null', () => {
      expect(computePrefillCount({ aiData: null })).toBe(0);
    });

    it('retourne 0 quand regimeMatrimonialDetecte absent', () => {
      expect(computePrefillCount({ aiData: {} })).toBe(0);
    });

    it('retourne 0 quand regimeMatrimonialDetecte = null', () => {
      expect(computePrefillCount({ aiData: { regimeMatrimonialDetecte: null as unknown as string } })).toBe(0);
    });
  });

  describe('computePrefillCount — M champs partiels (régime invalide)', () => {
    it('retourne 0 quand IA détecte SEPARATION_BIENS (exclu)', () => {
      expect(computePrefillCount({ aiData: { regimeMatrimonialDetecte: 'SEPARATION_BIENS' } })).toBe(0);
    });

    it('retourne 0 quand IA détecte un régime inconnu', () => {
      expect(computePrefillCount({ aiData: { regimeMatrimonialDetecte: 'REGIME_INCONNU_X' } })).toBe(0);
    });

    it('retourne 0 quand regimeMatrimonialDetecte est un type non-string', () => {
      expect(
        computePrefillCount({
          aiData: { regimeMatrimonialDetecte: 42 as unknown as string },
        }),
      ).toBe(0);
    });
  });

  describe('computePrefillCount — N champs cas nominal', () => {
    it('retourne 1 pour COMMUNAUTE_LEGALE', () => {
      expect(computePrefillCount({ aiData: { regimeMatrimonialDetecte: 'COMMUNAUTE_LEGALE' } })).toBe(1);
    });

    it('retourne 1 pour PARTICIPATION_AUX_ACQUETS', () => {
      expect(
        computePrefillCount({ aiData: { regimeMatrimonialDetecte: 'PARTICIPATION_AUX_ACQUETS' } }),
      ).toBe(1);
    });

    it('retourne 1 pour COMMUNAUTE_UNIVERSELLE', () => {
      expect(
        computePrefillCount({ aiData: { regimeMatrimonialDetecte: 'COMMUNAUTE_UNIVERSELLE' } }),
      ).toBe(1);
    });

    it('normalise la casse (lowercase → uppercase)', () => {
      expect(computePrefillCount({ aiData: { regimeMatrimonialDetecte: 'communaute_legale' } })).toBe(1);
    });
  });

  describe('computeRegimeMatrimonial', () => {
    it('renvoie la valeur normalisée pour un régime valide', () => {
      expect(
        computeRegimeMatrimonial({ aiData: { regimeMatrimonialDetecte: 'communaute_legale' } }),
      ).toBe('COMMUNAUTE_LEGALE');
    });

    it('renvoie null pour SEPARATION_BIENS', () => {
      expect(computeRegimeMatrimonial({ aiData: { regimeMatrimonialDetecte: 'SEPARATION_BIENS' } })).toBeNull();
    });
  });

  describe('export de surface unifiée', () => {
    it('expose VALID_REGIMES, computeRegimeMatrimonial, computePrefillCount', () => {
      expect(RecompensesPrefillRules.VALID_REGIMES).toBe(VALID_REGIMES);
      expect(RecompensesPrefillRules.computeRegimeMatrimonial).toBe(computeRegimeMatrimonial);
      expect(RecompensesPrefillRules.computePrefillCount).toBe(computePrefillCount);
    });
  });
});
