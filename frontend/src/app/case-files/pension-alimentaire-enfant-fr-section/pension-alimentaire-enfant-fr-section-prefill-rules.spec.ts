/**
 * SF-216-04 — Tests Jest pour `pension-alimentaire-enfant-fr-prefill-rules.ts`.
 *
 * Vérifie :
 *  - country gate FR-only (workspaceCountry !== 'FRANCE' → tous null) ;
 *  - mapping des 5 champs IA (revenus/12, nombre enfants, ages, mode résidence) ;
 *  - gestion des cas absents / négatifs / NaN ;
 *  - parité computePrefillCount avec la somme des mappings (5 max).
 */

import {
  PensionAlimentaireEnfantFrPrefillRules,
  computeAgesEnfants,
  computeModeResidence,
  computeNombreEnfants,
  computePrefillCount,
  computeRevenusNetsParent1,
  computeRevenusNetsParent2,
} from './pension-alimentaire-enfant-fr-section-prefill-rules';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

const FULL_AI: FamilleExtractedData = {
  // Revenus annuels (en € / an — convertis ÷ 12 par le helper).
  revenusAnnuelsEpoux1: 24_000, // → 2 000 €/mois
  revenusAnnuelsEpoux2: 36_000, // → 3 000 €/mois
  nbEnfantsACharge: 2,
  agesEnfantsDetectes: [8, 12],
  modeResidenceEnfantsDetecte: 'PRINCIPALE_PARENT1',
};

describe('pension-alimentaire-enfant-fr-prefill-rules (SF-216-04)', () => {
  describe('country gate', () => {
    it('workspaceCountry undefined → tous null', () => {
      expect(computeRevenusNetsParent1({ aiData: FULL_AI })).toBeNull();
      expect(computeRevenusNetsParent2({ aiData: FULL_AI })).toBeNull();
      expect(computeNombreEnfants({ aiData: FULL_AI })).toBeNull();
      expect(computeAgesEnfants({ aiData: FULL_AI })).toBeNull();
      expect(computeModeResidence({ aiData: FULL_AI })).toBeNull();
    });

    it('workspaceCountry BELGIQUE → tous null', () => {
      const input = { aiData: FULL_AI, workspaceCountry: 'BELGIQUE' };
      expect(computeRevenusNetsParent1(input)).toBeNull();
      expect(computeRevenusNetsParent2(input)).toBeNull();
      expect(computeNombreEnfants(input)).toBeNull();
      expect(computeAgesEnfants(input)).toBeNull();
      expect(computeModeResidence(input)).toBeNull();
    });

    it('workspaceCountry FRANCE → mapping 1-pour-1 avec conversion mensuelle', () => {
      const input = { aiData: FULL_AI, workspaceCountry: 'FRANCE' };
      expect(computeRevenusNetsParent1(input)).toBe(2_000);
      expect(computeRevenusNetsParent2(input)).toBe(3_000);
      expect(computeNombreEnfants(input)).toBe(2);
      expect(computeAgesEnfants(input)).toEqual([8, 12]);
      expect(computeModeResidence(input)).toBe('PRINCIPALE_PARENT1');
    });
  });

  describe('cas dégradés', () => {
    it('aiData absent → tous null', () => {
      expect(computeRevenusNetsParent1({ workspaceCountry: 'FRANCE' })).toBeNull();
      expect(computeNombreEnfants({ workspaceCountry: 'FRANCE' })).toBeNull();
      expect(computeAgesEnfants({ workspaceCountry: 'FRANCE' })).toBeNull();
      expect(computeModeResidence({ workspaceCountry: 'FRANCE' })).toBeNull();
    });

    it('aiData null → tous null', () => {
      const input = { aiData: null, workspaceCountry: 'FRANCE' };
      expect(computeRevenusNetsParent1(input)).toBeNull();
      expect(computeNombreEnfants(input)).toBeNull();
    });

    it('revenus négatifs rejetés', () => {
      const input = {
        aiData: { revenusAnnuelsEpoux1: -1000, revenusAnnuelsEpoux2: -2000 },
        workspaceCountry: 'FRANCE',
      };
      expect(computeRevenusNetsParent1(input)).toBeNull();
      expect(computeRevenusNetsParent2(input)).toBeNull();
    });

    it('NaN / Infinity rejetés', () => {
      const input = {
        aiData: { revenusAnnuelsEpoux1: NaN, revenusAnnuelsEpoux2: Infinity },
        workspaceCountry: 'FRANCE',
      };
      expect(computeRevenusNetsParent1(input)).toBeNull();
      expect(computeRevenusNetsParent2(input)).toBeNull();
    });

    it('nombreEnfants 0 → null', () => {
      const input = {
        aiData: { nbEnfantsACharge: 0 },
        workspaceCountry: 'FRANCE',
      };
      expect(computeNombreEnfants(input)).toBeNull();
    });

    it('agesEnfants vide → null', () => {
      const input = { aiData: { agesEnfantsDetectes: [] }, workspaceCountry: 'FRANCE' };
      expect(computeAgesEnfants(input)).toBeNull();
    });

    it('agesEnfants non-array → null', () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const input = { aiData: { agesEnfantsDetectes: 'broken' as any }, workspaceCountry: 'FRANCE' };
      expect(computeAgesEnfants(input)).toBeNull();
    });

    it('modeResidence hors whitelist → null', () => {
      const input = {
        aiData: { modeResidenceEnfantsDetecte: 'INVALIDE' },
        workspaceCountry: 'FRANCE',
      };
      expect(computeModeResidence(input)).toBeNull();
    });

    it('modeResidence chaîne vide → null', () => {
      const input = {
        aiData: { modeResidenceEnfantsDetecte: '' },
        workspaceCountry: 'FRANCE',
      };
      expect(computeModeResidence(input)).toBeNull();
    });
  });

  describe('computePrefillCount', () => {
    it('FR + FULL_AI → 5 champs (tous présents)', () => {
      expect(computePrefillCount({ aiData: FULL_AI, workspaceCountry: 'FRANCE' })).toBe(5);
    });

    it('BELGIQUE → 0', () => {
      expect(computePrefillCount({ aiData: FULL_AI, workspaceCountry: 'BELGIQUE' })).toBe(0);
    });

    it('FR + aiData partiel (revenus 1 seul) → 1', () => {
      const input = {
        aiData: { revenusAnnuelsEpoux1: 24_000 },
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

    it('FR + uniquement mode résidence → 1', () => {
      const input = {
        aiData: { modeResidenceEnfantsDetecte: 'ALTERNEE' as const },
        workspaceCountry: 'FRANCE',
      };
      expect(computePrefillCount(input)).toBe(1);
    });
  });

  describe('export en namespace', () => {
    it('PensionAlimentaireEnfantFrPrefillRules expose toutes les fonctions', () => {
      expect(PensionAlimentaireEnfantFrPrefillRules.computeRevenusNetsParent1).toBeDefined();
      expect(PensionAlimentaireEnfantFrPrefillRules.computeRevenusNetsParent2).toBeDefined();
      expect(PensionAlimentaireEnfantFrPrefillRules.computeNombreEnfants).toBeDefined();
      expect(PensionAlimentaireEnfantFrPrefillRules.computeAgesEnfants).toBeDefined();
      expect(PensionAlimentaireEnfantFrPrefillRules.computeModeResidence).toBeDefined();
      expect(PensionAlimentaireEnfantFrPrefillRules.computePrefillCount).toBeDefined();
    });
  });
});
