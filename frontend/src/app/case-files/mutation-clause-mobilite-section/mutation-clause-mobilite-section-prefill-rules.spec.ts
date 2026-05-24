/**
 * SF-212-14 — Tests unitaires du helper partagé
 * `mutation-clause-mobilite-section-prefill-rules`. Pattern aligné sur
 * SF-212-12 (modification-contrat-refus-section-prefill-rules).
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { MutationClauseMobiliteSectionPrefillRules } from './mutation-clause-mobilite-section-prefill-rules';

describe('MutationClauseMobiliteSectionPrefillRules', () => {
  const FULL_AI_DATA: TravailExtractedData = {
    mutationClausePresente: true,
    mutationZoneGeographiquePrecise: true,
    mutationInteretLegitimeEmployeur: true,
    mutationDelaiPrevenanceSemaines: 4,
    mutationSituationFamilialeContraingnante: false,
    mutationMotifProfessionnel: true,
  };

  describe('computePrefillCount (parité stricte avec prefillFromAi)', () => {
    it('retourne 6 sur fixture IA FR complète', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'FRANCE',
      })).toBe(6);
    });

    it('retourne 0 hors France', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'BELGIQUE',
      })).toBe(0);
    });

    it('retourne 0 sans aiData', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computePrefillCount({
        aiData: null,
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });

    it('ignore les valeurs hors plage du délai', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computePrefillCount({
        aiData: { ...FULL_AI_DATA, mutationDelaiPrevenanceSemaines: 999 },
        workspaceCountry: 'FRANCE',
      })).toBe(5);
    });

    it('ignore les booléens non boolean', () => {
      const broken = { ...FULL_AI_DATA, mutationClausePresente: 'oui' as unknown as boolean };
      expect(MutationClauseMobiliteSectionPrefillRules.computePrefillCount({
        aiData: broken,
        workspaceCountry: 'FRANCE',
      })).toBe(5);
    });

    it('ignore le délai négatif', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computePrefillCount({
        aiData: { ...FULL_AI_DATA, mutationDelaiPrevenanceSemaines: -1 },
        workspaceCountry: 'FRANCE',
      })).toBe(5);
    });
  });

  describe('computeClausePresente', () => {
    it('retourne true si présent et booléen', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computeClausePresente({
        aiData: { mutationClausePresente: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne false si présent et booléen false', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computeClausePresente({
        aiData: { mutationClausePresente: false },
        workspaceCountry: 'FRANCE',
      })).toBe(false);
    });
    it('retourne null si absent', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computeClausePresente({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null hors France', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computeClausePresente({
        aiData: { mutationClausePresente: true },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  describe('computeZonePrecise', () => {
    it('retourne true si présent et booléen', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computeZonePrecise({
        aiData: { mutationZoneGeographiquePrecise: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne null si absent', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computeZonePrecise({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeInteretLegitime', () => {
    it('retourne false si présent et booléen false', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computeInteretLegitime({
        aiData: { mutationInteretLegitimeEmployeur: false },
        workspaceCountry: 'FRANCE',
      })).toBe(false);
    });
    it('retourne null hors France', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computeInteretLegitime({
        aiData: { mutationInteretLegitimeEmployeur: true },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  describe('computeDelaiPrevenance', () => {
    it('retourne la valeur entière dans la plage [0, 52]', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computeDelaiPrevenance({
        aiData: { mutationDelaiPrevenanceSemaines: 4 },
        workspaceCountry: 'FRANCE',
      })).toBe(4);
    });
    it('tronque les décimaux', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computeDelaiPrevenance({
        aiData: { mutationDelaiPrevenanceSemaines: 4.7 },
        workspaceCountry: 'FRANCE',
      })).toBe(4);
    });
    it('retourne null pour valeur négative', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computeDelaiPrevenance({
        aiData: { mutationDelaiPrevenanceSemaines: -1 },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null pour valeur > 52', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computeDelaiPrevenance({
        aiData: { mutationDelaiPrevenanceSemaines: 100 },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null si absent', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computeDelaiPrevenance({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null pour non-number', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computeDelaiPrevenance({
        aiData: { mutationDelaiPrevenanceSemaines: '4' as unknown as number },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeSituationFamiliale', () => {
    it('retourne true si présent et booléen', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computeSituationFamiliale({
        aiData: { mutationSituationFamilialeContraingnante: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne null si absent', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computeSituationFamiliale({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeMotifProfessionnel', () => {
    it('retourne true si présent et booléen', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computeMotifProfessionnel({
        aiData: { mutationMotifProfessionnel: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne null hors France', () => {
      expect(MutationClauseMobiliteSectionPrefillRules.computeMotifProfessionnel({
        aiData: { mutationMotifProfessionnel: true },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });
});
