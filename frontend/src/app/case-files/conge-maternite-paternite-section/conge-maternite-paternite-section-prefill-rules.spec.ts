/**
 * SF-212-30 — Tests unitaires du helper partagé
 * `conge-maternite-paternite-section-prefill-rules`. Pattern aligné sur
 * SF-212-36 (pdv-rcc-conformite-section-prefill-rules).
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CongeMaternitePaterniteSectionPrefillRules } from './conge-maternite-paternite-section-prefill-rules';

describe('CongeMaternitePaterniteSectionPrefillRules', () => {
  const FULL_AI_DATA: TravailExtractedData = {
    congeMaternitePaterniteDetail: {
      congeMaternitePaterniteType: 'MATERNITE',
      congeMaterniteRangEnfant: 1,
      congeMaterniteNaissanceMultiple: false,
      congeMaterniteDateDebut: '2026-06-01',
      congeMaterniteSalaireMensuelBrut: 3000,
    },
  };

  describe('computePrefillCount (parité stricte avec prefillFromAi)', () => {
    it('retourne 5 sur fixture IA FR complète', () => {
      expect(CongeMaternitePaterniteSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'FRANCE',
      })).toBe(5);
    });

    it('retourne 0 hors France', () => {
      expect(CongeMaternitePaterniteSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'BELGIQUE',
      })).toBe(0);
    });

    it('retourne 0 sans aiData', () => {
      expect(CongeMaternitePaterniteSectionPrefillRules.computePrefillCount({
        aiData: null,
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });

    it('retourne 0 si sous-objet null', () => {
      expect(CongeMaternitePaterniteSectionPrefillRules.computePrefillCount({
        aiData: { congeMaternitePaterniteDetail: null },
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });

    it('ignore un type hors enum', () => {
      expect(CongeMaternitePaterniteSectionPrefillRules.computePrefillCount({
        aiData: {
          congeMaternitePaterniteDetail: {
            ...FULL_AI_DATA.congeMaternitePaterniteDetail!,
            congeMaternitePaterniteType: 'INCONNU',
          },
        },
        workspaceCountry: 'FRANCE',
      })).toBe(4);
    });

    it('ignore une date au format invalide', () => {
      expect(CongeMaternitePaterniteSectionPrefillRules.computePrefillCount({
        aiData: {
          congeMaternitePaterniteDetail: {
            ...FULL_AI_DATA.congeMaternitePaterniteDetail!,
            congeMaterniteDateDebut: 'pas-une-date',
          },
        },
        workspaceCountry: 'FRANCE',
      })).toBe(4);
    });

    it('ignore un rang hors plage [1, 20]', () => {
      expect(CongeMaternitePaterniteSectionPrefillRules.computePrefillCount({
        aiData: {
          congeMaternitePaterniteDetail: {
            ...FULL_AI_DATA.congeMaternitePaterniteDetail!,
            congeMaterniteRangEnfant: 0,
          },
        },
        workspaceCountry: 'FRANCE',
      })).toBe(4);
    });

    it('ignore un salaire négatif ou nul', () => {
      expect(CongeMaternitePaterniteSectionPrefillRules.computePrefillCount({
        aiData: {
          congeMaternitePaterniteDetail: {
            ...FULL_AI_DATA.congeMaternitePaterniteDetail!,
            congeMaterniteSalaireMensuelBrut: -100,
          },
        },
        workspaceCountry: 'FRANCE',
      })).toBe(4);
    });
  });

  describe('computeTypeConge', () => {
    it('retourne MATERNITE si présent', () => {
      expect(CongeMaternitePaterniteSectionPrefillRules.computeTypeConge({
        aiData: { congeMaternitePaterniteDetail: { congeMaternitePaterniteType: 'MATERNITE' } },
        workspaceCountry: 'FRANCE',
      })).toBe('MATERNITE');
    });
    it('retourne PATERNITE si présent', () => {
      expect(CongeMaternitePaterniteSectionPrefillRules.computeTypeConge({
        aiData: { congeMaternitePaterniteDetail: { congeMaternitePaterniteType: 'PATERNITE' } },
        workspaceCountry: 'FRANCE',
      })).toBe('PATERNITE');
    });
    it('null hors enum', () => {
      expect(CongeMaternitePaterniteSectionPrefillRules.computeTypeConge({
        aiData: { congeMaternitePaterniteDetail: { congeMaternitePaterniteType: 'X' } },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('null hors France', () => {
      expect(CongeMaternitePaterniteSectionPrefillRules.computeTypeConge({
        aiData: { congeMaternitePaterniteDetail: { congeMaternitePaterniteType: 'MATERNITE' } },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  describe('computeRangEnfant', () => {
    it('retourne le rang si entier ∈ [1, 20]', () => {
      expect(CongeMaternitePaterniteSectionPrefillRules.computeRangEnfant({
        aiData: { congeMaternitePaterniteDetail: { congeMaterniteRangEnfant: 3 } },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });
    it('null si non entier', () => {
      expect(CongeMaternitePaterniteSectionPrefillRules.computeRangEnfant({
        aiData: { congeMaternitePaterniteDetail: { congeMaterniteRangEnfant: 1.5 } },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeNaissanceMultiple', () => {
    it('retourne true si présent', () => {
      expect(CongeMaternitePaterniteSectionPrefillRules.computeNaissanceMultiple({
        aiData: { congeMaternitePaterniteDetail: { congeMaterniteNaissanceMultiple: true } },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('null si type non boolean', () => {
      expect(CongeMaternitePaterniteSectionPrefillRules.computeNaissanceMultiple({
        aiData: { congeMaternitePaterniteDetail: { congeMaterniteNaissanceMultiple: 'true' as unknown as boolean } },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeDateDebutConge', () => {
    it('retourne date ISO valide', () => {
      expect(CongeMaternitePaterniteSectionPrefillRules.computeDateDebutConge({
        aiData: { congeMaternitePaterniteDetail: { congeMaterniteDateDebut: '2026-06-01' } },
        workspaceCountry: 'FRANCE',
      })).toBe('2026-06-01');
    });
    it('null si format invalide', () => {
      expect(CongeMaternitePaterniteSectionPrefillRules.computeDateDebutConge({
        aiData: { congeMaternitePaterniteDetail: { congeMaterniteDateDebut: '01/06/2026' } },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeSalaireMensuelBrut', () => {
    it('retourne le montant si strictement positif', () => {
      expect(CongeMaternitePaterniteSectionPrefillRules.computeSalaireMensuelBrut({
        aiData: { congeMaternitePaterniteDetail: { congeMaterniteSalaireMensuelBrut: 3000 } },
        workspaceCountry: 'FRANCE',
      })).toBe(3000);
    });
    it('null si zéro', () => {
      expect(CongeMaternitePaterniteSectionPrefillRules.computeSalaireMensuelBrut({
        aiData: { congeMaternitePaterniteDetail: { congeMaterniteSalaireMensuelBrut: 0 } },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });
});
