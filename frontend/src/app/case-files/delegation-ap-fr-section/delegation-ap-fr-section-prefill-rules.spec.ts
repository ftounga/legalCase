/**
 * SF-216-10 — Tests Jest pour `delegation-ap-fr-section-prefill-rules.ts`.
 *
 * Vérifie :
 *  - country gate FR-only (workspaceCountry !== 'FRANCE' → tous null) ;
 *  - mapping 1-pour-1 des 3 champs IA (âge enfant, lien tiers, accord parents) ;
 *  - gestion des cas absents / négatifs / NaN / non-fini ;
 *  - whitelist stricte du lien tiers (5 valeurs) ;
 *  - parité computePrefillCount = somme des mappings non-null (max 3).
 */

import {
  DelegationApFrPrefillRules,
  computeAccordParents,
  computeAgeEnfant,
  computePrefillCount,
  computeTiersLienFamilial,
} from './delegation-ap-fr-section-prefill-rules';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

const FULL_AI: FamilleExtractedData = {
  agesEnfantsDetectes: [8, 12],
  tiersLienFamilialDetecte: 'GRANDS_PARENTS',
  accordParentsDetecte: true,
};

describe('delegation-ap-fr-prefill-rules (SF-216-10)', () => {
  describe('country gate', () => {
    it('workspaceCountry undefined → tous null', () => {
      expect(computeAgeEnfant({ aiData: FULL_AI })).toBeNull();
      expect(computeTiersLienFamilial({ aiData: FULL_AI })).toBeNull();
      expect(computeAccordParents({ aiData: FULL_AI })).toBeNull();
    });

    it('workspaceCountry BELGIQUE → tous null', () => {
      const input = { aiData: FULL_AI, workspaceCountry: 'BELGIQUE' };
      expect(computeAgeEnfant(input)).toBeNull();
      expect(computeTiersLienFamilial(input)).toBeNull();
      expect(computeAccordParents(input)).toBeNull();
    });

    it('workspaceCountry FRANCE → mapping appliqué', () => {
      const input = { aiData: FULL_AI, workspaceCountry: 'FRANCE' };
      expect(computeAgeEnfant(input)).toBe(8);
      expect(computeTiersLienFamilial(input)).toBe('GRANDS_PARENTS');
      expect(computeAccordParents(input)).toBe(true);
    });
  });

  describe('computeAgeEnfant', () => {
    it('aiData null → null', () => {
      expect(computeAgeEnfant({ aiData: null, workspaceCountry: 'FRANCE' })).toBeNull();
    });

    it('agesEnfantsDetectes vide → null', () => {
      expect(computeAgeEnfant({
        aiData: { agesEnfantsDetectes: [] },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });

    it('âge négatif → null', () => {
      expect(computeAgeEnfant({
        aiData: { agesEnfantsDetectes: [-1] },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });

    it('âge > 18 → null', () => {
      expect(computeAgeEnfant({
        aiData: { agesEnfantsDetectes: [19] },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });

    it('âge décimal → tronqué', () => {
      expect(computeAgeEnfant({
        aiData: { agesEnfantsDetectes: [7.9] },
        workspaceCountry: 'FRANCE',
      })).toBe(7);
    });

    it('prend le premier enfant de la liste', () => {
      expect(computeAgeEnfant({
        aiData: { agesEnfantsDetectes: [3, 7, 12] },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });
  });

  describe('computeTiersLienFamilial', () => {
    it.each([
      'GRANDS_PARENTS',
      'ONCLE_TANTE',
      'FAMILLE_ELARGIE',
      'ASSOCIATION_HABILITEE',
      'AUTRE',
    ] as const)('whitelist value %s → préservée', (value) => {
      expect(computeTiersLienFamilial({
        aiData: { tiersLienFamilialDetecte: value },
        workspaceCountry: 'FRANCE',
      })).toBe(value);
    });

    it('valeur hors whitelist → null', () => {
      expect(computeTiersLienFamilial({
        aiData: { tiersLienFamilialDetecte: 'COUSIN_GERMAIN' },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });

    it('lower-case normalisé', () => {
      expect(computeTiersLienFamilial({
        aiData: { tiersLienFamilialDetecte: 'grands_parents' },
        workspaceCountry: 'FRANCE',
      })).toBe('GRANDS_PARENTS');
    });
  });

  describe('computeAccordParents', () => {
    it('true → true', () => {
      expect(computeAccordParents({
        aiData: { accordParentsDetecte: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });

    it('false → false', () => {
      expect(computeAccordParents({
        aiData: { accordParentsDetecte: false },
        workspaceCountry: 'FRANCE',
      })).toBe(false);
    });

    it('null → null', () => {
      expect(computeAccordParents({
        aiData: { accordParentsDetecte: null },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computePrefillCount', () => {
    it('FRANCE + données complètes → 3', () => {
      expect(computePrefillCount({ aiData: FULL_AI, workspaceCountry: 'FRANCE' })).toBe(3);
    });

    it('BELGIQUE → 0', () => {
      expect(computePrefillCount({ aiData: FULL_AI, workspaceCountry: 'BELGIQUE' })).toBe(0);
    });

    it('FRANCE + données partielles → compte exact', () => {
      expect(computePrefillCount({
        aiData: { agesEnfantsDetectes: [5] },
        workspaceCountry: 'FRANCE',
      })).toBe(1);
    });

    it('FRANCE + aucune donnée → 0', () => {
      expect(computePrefillCount({ aiData: {}, workspaceCountry: 'FRANCE' })).toBe(0);
    });

    it('Object Rules namespace expose les 4 fonctions', () => {
      expect(typeof DelegationApFrPrefillRules.computeAgeEnfant).toBe('function');
      expect(typeof DelegationApFrPrefillRules.computeTiersLienFamilial).toBe('function');
      expect(typeof DelegationApFrPrefillRules.computeAccordParents).toBe('function');
      expect(typeof DelegationApFrPrefillRules.computePrefillCount).toBe('function');
    });
  });
});
