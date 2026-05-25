/**
 * SF-212-18 / F-256 — Tests unitaires du helper partagé
 * `rupture-anticipee-cdd-section-prefill-rules` (3 champs IA réactivés).
 */

import { RuptureAnticipeeCddSectionPrefillRules } from './rupture-anticipee-cdd-section-prefill-rules';

describe('RuptureAnticipeeCddSectionPrefillRules', () => {
  describe('computePrefillCount', () => {
    it('retourne 0 sur fixture vide FR', () => {
      expect(
        RuptureAnticipeeCddSectionPrefillRules.computePrefillCount({
          aiData: {},
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(0);
    });

    it('retourne 0 hors France même si le sous-objet est rempli', () => {
      expect(
        RuptureAnticipeeCddSectionPrefillRules.computePrefillCount({
          aiData: {
            ruptureAnticipeeCddDetail: {
              ruptureAnticipeeCddAuteur: 'EMPLOYEUR',
              ruptureAnticipeeCddMotif: 'FAUTE_GRAVE',
              ruptureAnticipeeCddDateTerme: '2025-06-30',
            },
          },
          workspaceCountry: 'BELGIQUE',
        }),
      ).toBe(0);
    });

    it('retourne 0 sans aiData', () => {
      expect(
        RuptureAnticipeeCddSectionPrefillRules.computePrefillCount({
          aiData: null,
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(0);
    });

    it('retourne 3 quand les 3 champs sont remplis et valides', () => {
      expect(
        RuptureAnticipeeCddSectionPrefillRules.computePrefillCount({
          aiData: {
            ruptureAnticipeeCddDetail: {
              ruptureAnticipeeCddAuteur: 'EMPLOYEUR',
              ruptureAnticipeeCddMotif: 'FAUTE_GRAVE',
              ruptureAnticipeeCddDateTerme: '2025-06-30',
            },
          },
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(3);
    });

    it('retourne 1 quand seul l\'auteur est valide (autres invalides ou null)', () => {
      expect(
        RuptureAnticipeeCddSectionPrefillRules.computePrefillCount({
          aiData: {
            ruptureAnticipeeCddDetail: {
              ruptureAnticipeeCddAuteur: 'SALARIE',
              ruptureAnticipeeCddMotif: 'CODE_INCONNU',
              ruptureAnticipeeCddDateTerme: 'pas une date',
            },
          },
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(1);
    });
  });

  describe('computeAuteur', () => {
    it('retourne EMPLOYEUR si valide', () => {
      expect(
        RuptureAnticipeeCddSectionPrefillRules.computeAuteur({
          aiData: { ruptureAnticipeeCddDetail: { ruptureAnticipeeCddAuteur: 'EMPLOYEUR' } },
          workspaceCountry: 'FRANCE',
        }),
      ).toBe('EMPLOYEUR');
    });

    it('retourne null pour valeur hors whitelist', () => {
      expect(
        RuptureAnticipeeCddSectionPrefillRules.computeAuteur({
          aiData: { ruptureAnticipeeCddDetail: { ruptureAnticipeeCddAuteur: 'AUTRE_VALEUR' } },
          workspaceCountry: 'FRANCE',
        }),
      ).toBeNull();
    });
  });

  describe('computeMotif', () => {
    it('retourne FAUTE_GRAVE si valide', () => {
      expect(
        RuptureAnticipeeCddSectionPrefillRules.computeMotif({
          aiData: { ruptureAnticipeeCddDetail: { ruptureAnticipeeCddMotif: 'FAUTE_GRAVE' } },
          workspaceCountry: 'FRANCE',
        }),
      ).toBe('FAUTE_GRAVE');
    });
  });

  describe('computeDateTerme', () => {
    it('retourne la date ISO si valide', () => {
      expect(
        RuptureAnticipeeCddSectionPrefillRules.computeDateTerme({
          aiData: { ruptureAnticipeeCddDetail: { ruptureAnticipeeCddDateTerme: '2025-06-30' } },
          workspaceCountry: 'FRANCE',
        }),
      ).toBe('2025-06-30');
    });

    it('retourne null si format invalide', () => {
      expect(
        RuptureAnticipeeCddSectionPrefillRules.computeDateTerme({
          aiData: { ruptureAnticipeeCddDetail: { ruptureAnticipeeCddDateTerme: '30/06/2025' } },
          workspaceCountry: 'FRANCE',
        }),
      ).toBeNull();
    });
  });
});
