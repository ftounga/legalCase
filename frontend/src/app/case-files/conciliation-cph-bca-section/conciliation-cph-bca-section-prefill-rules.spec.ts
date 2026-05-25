/**
 * SF-212-38 — Tests unitaires du helper partagé
 * `conciliation-cph-bca-section-prefill-rules`. Pattern aligné sur
 * SF-212-32 (elections-cse-conformite-section-prefill-rules).
 *
 * <p>F-212 19/19 — dernier outil livré du bundle F-212.</p>
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { ConciliationCphBcaSectionPrefillRules } from './conciliation-cph-bca-section-prefill-rules';

describe('ConciliationCphBcaSectionPrefillRules', () => {
  const FULL_AI_DATA: TravailExtractedData = {
    conciliationCphAncienneteMois: 144, // 12 ans → palier 8 à 15 ans
    conciliationCphSalaire: 3000,
    conciliationCphMontantDemandes: 50000,
  };

  describe('computePrefillCount (parité stricte avec prefillFromAi)', () => {
    it('retourne 3 sur fixture IA FR complète', () => {
      expect(ConciliationCphBcaSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });

    it('retourne 0 hors France', () => {
      expect(ConciliationCphBcaSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'BELGIQUE',
      })).toBe(0);
    });

    it('retourne 0 sans aiData', () => {
      expect(ConciliationCphBcaSectionPrefillRules.computePrefillCount({
        aiData: null,
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });

    it('ignore les nombres non finis', () => {
      const broken = { ...FULL_AI_DATA, conciliationCphSalaire: NaN };
      expect(ConciliationCphBcaSectionPrefillRules.computePrefillCount({
        aiData: broken,
        workspaceCountry: 'FRANCE',
      })).toBe(2);
    });

    it('ignore ancienneté non entière', () => {
      expect(ConciliationCphBcaSectionPrefillRules.computePrefillCount({
        aiData: { ...FULL_AI_DATA, conciliationCphAncienneteMois: 12.5 },
        workspaceCountry: 'FRANCE',
      })).toBe(2);
    });
  });

  describe('computeAncienneteMois', () => {
    it('retourne la valeur entière dans la plage [1, 600]', () => {
      expect(ConciliationCphBcaSectionPrefillRules.computeAncienneteMois({
        aiData: { conciliationCphAncienneteMois: 36 },
        workspaceCountry: 'FRANCE',
      })).toBe(36);
    });

    it('retourne null si valeur < 1', () => {
      expect(ConciliationCphBcaSectionPrefillRules.computeAncienneteMois({
        aiData: { conciliationCphAncienneteMois: 0 },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });

    it('retourne null si valeur > 600', () => {
      expect(ConciliationCphBcaSectionPrefillRules.computeAncienneteMois({
        aiData: { conciliationCphAncienneteMois: 700 },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });

    it('retourne null hors France', () => {
      expect(ConciliationCphBcaSectionPrefillRules.computeAncienneteMois({
        aiData: { conciliationCphAncienneteMois: 36 },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });

    it('retourne null si absent', () => {
      expect(ConciliationCphBcaSectionPrefillRules.computeAncienneteMois({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeSalaire', () => {
    it('retourne le décimal dans la plage [1200, 50000]', () => {
      expect(ConciliationCphBcaSectionPrefillRules.computeSalaire({
        aiData: { conciliationCphSalaire: 2500 },
        workspaceCountry: 'FRANCE',
      })).toBe(2500);
    });

    it('retourne null si valeur < 1200', () => {
      expect(ConciliationCphBcaSectionPrefillRules.computeSalaire({
        aiData: { conciliationCphSalaire: 500 },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });

    it('retourne null si valeur > 50 000', () => {
      expect(ConciliationCphBcaSectionPrefillRules.computeSalaire({
        aiData: { conciliationCphSalaire: 100000 },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });

    it('retourne null hors France', () => {
      expect(ConciliationCphBcaSectionPrefillRules.computeSalaire({
        aiData: { conciliationCphSalaire: 2500 },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  describe('computeMontantDemandes', () => {
    it('retourne le décimal dans la plage [500, 1 000 000]', () => {
      expect(ConciliationCphBcaSectionPrefillRules.computeMontantDemandes({
        aiData: { conciliationCphMontantDemandes: 30000 },
        workspaceCountry: 'FRANCE',
      })).toBe(30000);
    });

    it('retourne null si valeur < 500', () => {
      expect(ConciliationCphBcaSectionPrefillRules.computeMontantDemandes({
        aiData: { conciliationCphMontantDemandes: 100 },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });

    it('retourne null si valeur > 1 000 000', () => {
      expect(ConciliationCphBcaSectionPrefillRules.computeMontantDemandes({
        aiData: { conciliationCphMontantDemandes: 2000000 },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });

    it('retourne null hors France', () => {
      expect(ConciliationCphBcaSectionPrefillRules.computeMontantDemandes({
        aiData: { conciliationCphMontantDemandes: 30000 },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });
});
