/**
 * SF-216-02 — Tests Jest pour `prestation-compensatoire-section-prefill-rules.ts`.
 *
 * Vérifie :
 *  - country gate FR-only (workspaceCountry !== 'FRANCE' → tous null) ;
 *  - mapping 1-pour-1 de chaque champ source IA vers la cible du formulaire ;
 *  - gestion des cas absents / négatifs / NaN / non-fini ;
 *  - parité computePrefillCount avec la somme des mappings.
 */

import {
  PrestationCompensatoireSectionPrefillRules,
  computeAgeEpoux1,
  computeAgeEpoux2,
  computeAvantageMatrimonial,
  computeDureeMariageAnnees,
  computePrefillCount,
  computeRevenusEpoux1,
  computeRevenusEpoux2,
} from './prestation-compensatoire-section-prefill-rules';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

const FULL_AI: FamilleExtractedData = {
  dureeMariageAnnees: 18,
  revenusAnnuelsEpoux1: 45000,
  revenusAnnuelsEpoux2: 22000,
  ageEpoux1Annees: 52,
  ageEpoux2Annees: 48,
  clauseAttributionIntegraleDetected: true,
};

describe('prestation-compensatoire-section-prefill-rules (SF-216-02)', () => {
  describe('country gate', () => {
    it('workspaceCountry undefined → tous null', () => {
      expect(computeDureeMariageAnnees({ aiData: FULL_AI })).toBeNull();
      expect(computeRevenusEpoux1({ aiData: FULL_AI })).toBeNull();
      expect(computeRevenusEpoux2({ aiData: FULL_AI })).toBeNull();
      expect(computeAgeEpoux1({ aiData: FULL_AI })).toBeNull();
      expect(computeAgeEpoux2({ aiData: FULL_AI })).toBeNull();
      expect(computeAvantageMatrimonial({ aiData: FULL_AI })).toBeNull();
    });

    it('workspaceCountry BELGIQUE → tous null', () => {
      const input = { aiData: FULL_AI, workspaceCountry: 'BELGIQUE' };
      expect(computeDureeMariageAnnees(input)).toBeNull();
      expect(computeRevenusEpoux1(input)).toBeNull();
      expect(computeRevenusEpoux2(input)).toBeNull();
      expect(computeAgeEpoux1(input)).toBeNull();
      expect(computeAgeEpoux2(input)).toBeNull();
      expect(computeAvantageMatrimonial(input)).toBeNull();
    });

    it('workspaceCountry FRANCE → mapping 1-pour-1', () => {
      const input = { aiData: FULL_AI, workspaceCountry: 'FRANCE' };
      expect(computeDureeMariageAnnees(input)).toBe(18);
      expect(computeRevenusEpoux1(input)).toBe(45000);
      expect(computeRevenusEpoux2(input)).toBe(22000);
      expect(computeAgeEpoux1(input)).toBe(52);
      expect(computeAgeEpoux2(input)).toBe(48);
      expect(computeAvantageMatrimonial(input)).toBe(true);
    });
  });

  describe('cas dégradés', () => {
    it('aiData absent → tous null', () => {
      expect(computeDureeMariageAnnees({ workspaceCountry: 'FRANCE' })).toBeNull();
      expect(computeRevenusEpoux1({ workspaceCountry: 'FRANCE' })).toBeNull();
      expect(computeAgeEpoux1({ workspaceCountry: 'FRANCE' })).toBeNull();
      expect(computeAvantageMatrimonial({ workspaceCountry: 'FRANCE' })).toBeNull();
    });

    it('aiData null → tous null', () => {
      const input = { aiData: null, workspaceCountry: 'FRANCE' };
      expect(computeDureeMariageAnnees(input)).toBeNull();
      expect(computeRevenusEpoux1(input)).toBeNull();
    });

    it('valeurs négatives rejetées', () => {
      const input = {
        aiData: { dureeMariageAnnees: -1, revenusAnnuelsEpoux1: -100, ageEpoux1Annees: -5 },
        workspaceCountry: 'FRANCE',
      };
      expect(computeDureeMariageAnnees(input)).toBeNull();
      expect(computeRevenusEpoux1(input)).toBeNull();
      expect(computeAgeEpoux1(input)).toBeNull();
    });

    it('NaN / Infinity rejetés', () => {
      const input = {
        aiData: { dureeMariageAnnees: NaN, revenusAnnuelsEpoux1: Infinity },
        workspaceCountry: 'FRANCE',
      };
      expect(computeDureeMariageAnnees(input)).toBeNull();
      expect(computeRevenusEpoux1(input)).toBeNull();
    });

    it('avantageMatrimonial : false reste false (distingue null/false)', () => {
      const input = {
        aiData: { clauseAttributionIntegraleDetected: false },
        workspaceCountry: 'FRANCE',
      };
      expect(computeAvantageMatrimonial(input)).toBe(false);
    });

    it('avantageMatrimonial : non-boolean → null', () => {
      const input = {
        aiData: { clauseAttributionIntegraleDetected: null },
        workspaceCountry: 'FRANCE',
      };
      expect(computeAvantageMatrimonial(input)).toBeNull();
    });
  });

  describe('computePrefillCount', () => {
    it('FR + FULL_AI → 6 champs', () => {
      expect(computePrefillCount({ aiData: FULL_AI, workspaceCountry: 'FRANCE' })).toBe(6);
    });

    it('BELGIQUE → 0', () => {
      expect(computePrefillCount({ aiData: FULL_AI, workspaceCountry: 'BELGIQUE' })).toBe(0);
    });

    it('FR + aiData partiel → comptage exact', () => {
      const input = {
        aiData: { dureeMariageAnnees: 10, revenusAnnuelsEpoux1: 30000 },
        workspaceCountry: 'FRANCE',
      };
      expect(computePrefillCount(input)).toBe(2);
    });

    it('FR + aiData null → 0', () => {
      expect(computePrefillCount({ aiData: null, workspaceCountry: 'FRANCE' })).toBe(0);
    });

    it('{} → 0 (gate par défaut)', () => {
      expect(computePrefillCount({})).toBe(0);
    });
  });

  describe('export en namespace', () => {
    it('PrestationCompensatoireSectionPrefillRules expose toutes les fonctions', () => {
      expect(PrestationCompensatoireSectionPrefillRules.computeDureeMariageAnnees).toBeDefined();
      expect(PrestationCompensatoireSectionPrefillRules.computeRevenusEpoux1).toBeDefined();
      expect(PrestationCompensatoireSectionPrefillRules.computeRevenusEpoux2).toBeDefined();
      expect(PrestationCompensatoireSectionPrefillRules.computeAgeEpoux1).toBeDefined();
      expect(PrestationCompensatoireSectionPrefillRules.computeAgeEpoux2).toBeDefined();
      expect(PrestationCompensatoireSectionPrefillRules.computeAvantageMatrimonial).toBeDefined();
      expect(PrestationCompensatoireSectionPrefillRules.computePrefillCount).toBeDefined();
    });
  });
});
