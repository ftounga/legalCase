/**
 * SF-216-06 — Tests Jest pour `liquidation-communaute-fr-prefill-rules.ts`.
 *
 * Vérifie :
 *  - country gate FR-only (workspaceCountry !== 'FRANCE' → tous null) ;
 *  - mapping 1-pour-1 des champs récompenses (les seuls qui comptent dans
 *    computePrefillCount) ;
 *  - lecture des signaux informatifs (régime, clause attribution intégrale) ;
 *  - gestion des cas absents / négatifs / NaN / non-fini ;
 *  - parité computePrefillCount avec la somme des mappings (2 champs).
 */

import {
  LiquidationCommunauteFrPrefillRules,
  computeClauseAttributionIntegrale,
  computePrefillCount,
  computeRecompensesEpoux1,
  computeRecompensesEpoux2,
  computeRegimeMatrimonialDetecte,
} from './liquidation-communaute-fr-section-prefill-rules';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

const FULL_AI: FamilleExtractedData = {
  regimeMatrimonialDetecte: 'COMMUNAUTE_LEGALE',
  valeurCommunauteEurDetectee: 320_000,
  clauseAttributionIntegraleDetected: false,
  recompensesEpoux1Eur: 5_000,
  recompensesEpoux2Eur: 12_000,
};

describe('liquidation-communaute-fr-prefill-rules (SF-216-06)', () => {
  describe('country gate', () => {
    it('workspaceCountry undefined → tous null', () => {
      expect(computeRegimeMatrimonialDetecte({ aiData: FULL_AI })).toBeNull();
      expect(computeClauseAttributionIntegrale({ aiData: FULL_AI })).toBeNull();
      expect(computeRecompensesEpoux1({ aiData: FULL_AI })).toBeNull();
      expect(computeRecompensesEpoux2({ aiData: FULL_AI })).toBeNull();
    });

    it('workspaceCountry BELGIQUE → tous null', () => {
      const input = { aiData: FULL_AI, workspaceCountry: 'BELGIQUE' };
      expect(computeRegimeMatrimonialDetecte(input)).toBeNull();
      expect(computeClauseAttributionIntegrale(input)).toBeNull();
      expect(computeRecompensesEpoux1(input)).toBeNull();
      expect(computeRecompensesEpoux2(input)).toBeNull();
    });

    it('workspaceCountry FRANCE → mapping 1-pour-1', () => {
      const input = { aiData: FULL_AI, workspaceCountry: 'FRANCE' };
      expect(computeRegimeMatrimonialDetecte(input)).toBe('COMMUNAUTE_LEGALE');
      expect(computeClauseAttributionIntegrale(input)).toBe(false);
      expect(computeRecompensesEpoux1(input)).toBe(5_000);
      expect(computeRecompensesEpoux2(input)).toBe(12_000);
    });
  });

  describe('cas dégradés', () => {
    it('aiData absent → tous null', () => {
      expect(computeRegimeMatrimonialDetecte({ workspaceCountry: 'FRANCE' })).toBeNull();
      expect(computeRecompensesEpoux1({ workspaceCountry: 'FRANCE' })).toBeNull();
      expect(computeClauseAttributionIntegrale({ workspaceCountry: 'FRANCE' })).toBeNull();
    });

    it('aiData null → tous null', () => {
      const input = { aiData: null, workspaceCountry: 'FRANCE' };
      expect(computeRecompensesEpoux1(input)).toBeNull();
      expect(computeRecompensesEpoux2(input)).toBeNull();
    });

    it('valeurs négatives rejetées', () => {
      const input = {
        aiData: { recompensesEpoux1Eur: -100, recompensesEpoux2Eur: -50 },
        workspaceCountry: 'FRANCE',
      };
      expect(computeRecompensesEpoux1(input)).toBeNull();
      expect(computeRecompensesEpoux2(input)).toBeNull();
    });

    it('NaN / Infinity rejetés', () => {
      const input = {
        aiData: { recompensesEpoux1Eur: NaN, recompensesEpoux2Eur: Infinity },
        workspaceCountry: 'FRANCE',
      };
      expect(computeRecompensesEpoux1(input)).toBeNull();
      expect(computeRecompensesEpoux2(input)).toBeNull();
    });

    it('clauseAttributionIntegrale : false reste false (distingue null/false)', () => {
      const input = {
        aiData: { clauseAttributionIntegraleDetected: false },
        workspaceCountry: 'FRANCE',
      };
      expect(computeClauseAttributionIntegrale(input)).toBe(false);
    });

    it('clauseAttributionIntegrale : non-boolean → null', () => {
      const input = {
        aiData: { clauseAttributionIntegraleDetected: null },
        workspaceCountry: 'FRANCE',
      };
      expect(computeClauseAttributionIntegrale(input)).toBeNull();
    });

    it('regimeMatrimonial chaîne vide → null', () => {
      const input = {
        aiData: { regimeMatrimonialDetecte: '' },
        workspaceCountry: 'FRANCE',
      };
      expect(computeRegimeMatrimonialDetecte(input)).toBeNull();
    });
  });

  describe('computePrefillCount', () => {
    it('FR + FULL_AI → 2 champs (récompenses 1/2 — régime et clause = signaux non comptés)', () => {
      expect(computePrefillCount({ aiData: FULL_AI, workspaceCountry: 'FRANCE' })).toBe(2);
    });

    it('BELGIQUE → 0', () => {
      expect(computePrefillCount({ aiData: FULL_AI, workspaceCountry: 'BELGIQUE' })).toBe(0);
    });

    it('FR + aiData partiel (recompenses 1 seul) → 1', () => {
      const input = {
        aiData: { recompensesEpoux1Eur: 5_000 },
        workspaceCountry: 'FRANCE',
      };
      expect(computePrefillCount(input)).toBe(1);
    });

    it('FR + aiData null → 0', () => {
      expect(computePrefillCount({ aiData: null, workspaceCountry: 'FRANCE' })).toBe(0);
    });

    it('{} → 0 (gate par défaut)', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('FR + uniquement régime + clause (pas de récompenses) → 0', () => {
      const input = {
        aiData: {
          regimeMatrimonialDetecte: 'COMMUNAUTE_LEGALE',
          clauseAttributionIntegraleDetected: true,
        },
        workspaceCountry: 'FRANCE',
      };
      expect(computePrefillCount(input)).toBe(0);
    });
  });

  describe('export en namespace', () => {
    it('LiquidationCommunauteFrPrefillRules expose toutes les fonctions', () => {
      expect(LiquidationCommunauteFrPrefillRules.computeRegimeMatrimonialDetecte).toBeDefined();
      expect(LiquidationCommunauteFrPrefillRules.computeClauseAttributionIntegrale).toBeDefined();
      expect(LiquidationCommunauteFrPrefillRules.computeRecompensesEpoux1).toBeDefined();
      expect(LiquidationCommunauteFrPrefillRules.computeRecompensesEpoux2).toBeDefined();
      expect(LiquidationCommunauteFrPrefillRules.computePrefillCount).toBeDefined();
    });
  });
});
