/**
 * SF-216-08 — Tests Jest pour `aripa-recouvrement-fr-section-prefill-rules.ts`.
 *
 * Vérifie :
 *  - country gate FR-only (workspaceCountry !== 'FRANCE' → tous null) ;
 *  - mapping 1-pour-1 des 3 champs IA (montant pension, titre, nb enfants) ;
 *  - gestion des cas absents / négatifs / NaN / non-fini ;
 *  - parité computePrefillCount = somme des mappings non-null (max 3).
 */

import {
  AripaRecouvrementFrPrefillRules,
  computeMontantPensionMensuelle,
  computeNombreEnfantsACharge,
  computePrefillCount,
  computeTitreExecutoire,
} from './aripa-recouvrement-fr-section-prefill-rules';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

const FULL_AI: FamilleExtractedData = {
  montantPensionMensuelleDueEur: 300,
  titreExecutoireDetecte: true,
  nombreEnfantsDetecte: 2,
};

describe('aripa-recouvrement-fr-prefill-rules (SF-216-08)', () => {
  describe('country gate', () => {
    it('workspaceCountry undefined → tous null', () => {
      expect(computeMontantPensionMensuelle({ aiData: FULL_AI })).toBeNull();
      expect(computeTitreExecutoire({ aiData: FULL_AI })).toBeNull();
      expect(computeNombreEnfantsACharge({ aiData: FULL_AI })).toBeNull();
    });

    it('workspaceCountry BELGIQUE → tous null', () => {
      const input = { aiData: FULL_AI, workspaceCountry: 'BELGIQUE' };
      expect(computeMontantPensionMensuelle(input)).toBeNull();
      expect(computeTitreExecutoire(input)).toBeNull();
      expect(computeNombreEnfantsACharge(input)).toBeNull();
    });

    it('workspaceCountry FRANCE → mapping 1-pour-1', () => {
      const input = { aiData: FULL_AI, workspaceCountry: 'FRANCE' };
      expect(computeMontantPensionMensuelle(input)).toBe(300);
      expect(computeTitreExecutoire(input)).toBe(true);
      expect(computeNombreEnfantsACharge(input)).toBe(2);
    });
  });

  describe('null / absent / invalides', () => {
    it('aiData null → tous null', () => {
      const input = { aiData: null, workspaceCountry: 'FRANCE' };
      expect(computeMontantPensionMensuelle(input)).toBeNull();
      expect(computeTitreExecutoire(input)).toBeNull();
      expect(computeNombreEnfantsACharge(input)).toBeNull();
    });

    it('aiData {} → tous null', () => {
      const input = { aiData: {}, workspaceCountry: 'FRANCE' };
      expect(computeMontantPensionMensuelle(input)).toBeNull();
      expect(computeTitreExecutoire(input)).toBeNull();
      expect(computeNombreEnfantsACharge(input)).toBeNull();
    });

    it('montant négatif → null', () => {
      const input = {
        aiData: { montantPensionMensuelleDueEur: -10 } as FamilleExtractedData,
        workspaceCountry: 'FRANCE',
      };
      expect(computeMontantPensionMensuelle(input)).toBeNull();
    });

    it('montant NaN → null', () => {
      const input = {
        aiData: { montantPensionMensuelleDueEur: Number.NaN } as FamilleExtractedData,
        workspaceCountry: 'FRANCE',
      };
      expect(computeMontantPensionMensuelle(input)).toBeNull();
    });

    it('titreExecutoireDetecte non boolean → null', () => {
      const input = {
        aiData: { titreExecutoireDetecte: 'yes' as unknown as boolean } as FamilleExtractedData,
        workspaceCountry: 'FRANCE',
      };
      expect(computeTitreExecutoire(input)).toBeNull();
    });

    it('titreExecutoireDetecte = false → false (valide)', () => {
      const input = {
        aiData: { titreExecutoireDetecte: false } as FamilleExtractedData,
        workspaceCountry: 'FRANCE',
      };
      expect(computeTitreExecutoire(input)).toBe(false);
    });
  });

  describe('computePrefillCount', () => {
    it('FULL_AI + FRANCE → 3', () => {
      expect(computePrefillCount({ aiData: FULL_AI, workspaceCountry: 'FRANCE' })).toBe(3);
    });

    it('FULL_AI + BELGIQUE → 0', () => {
      expect(computePrefillCount({ aiData: FULL_AI, workspaceCountry: 'BELGIQUE' })).toBe(0);
    });

    it('empty {} → 0', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('aiData null → 0', () => {
      expect(computePrefillCount({ aiData: null, workspaceCountry: 'FRANCE' })).toBe(0);
    });

    it('partial AI (montant seul) → 1', () => {
      const partial = { montantPensionMensuelleDueEur: 250 } as FamilleExtractedData;
      expect(computePrefillCount({ aiData: partial, workspaceCountry: 'FRANCE' })).toBe(1);
    });

    it('partial AI (titre + enfants) → 2', () => {
      const partial = {
        titreExecutoireDetecte: true,
        nombreEnfantsDetecte: 1,
      } as FamilleExtractedData;
      expect(computePrefillCount({ aiData: partial, workspaceCountry: 'FRANCE' })).toBe(2);
    });
  });

  describe('export AripaRecouvrementFrPrefillRules object', () => {
    it('expose computePrefillCount + computeXxx', () => {
      expect(typeof AripaRecouvrementFrPrefillRules.computePrefillCount).toBe('function');
      expect(typeof AripaRecouvrementFrPrefillRules.computeMontantPensionMensuelle).toBe('function');
      expect(typeof AripaRecouvrementFrPrefillRules.computeTitreExecutoire).toBe('function');
      expect(typeof AripaRecouvrementFrPrefillRules.computeNombreEnfantsACharge).toBe('function');
    });
  });
});
