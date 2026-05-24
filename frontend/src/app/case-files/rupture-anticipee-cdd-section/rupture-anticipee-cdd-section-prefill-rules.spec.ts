/**
 * SF-212-18 — Tests unitaires du helper partagé
 * `rupture-anticipee-cdd-section-prefill-rules`.
 *
 * Le helper retourne actuellement 0 sur toutes les fixtures (limite JVM
 * du backend, voir doc d'en-tête du module). Ces tests vérifient le contrat
 * d'interface (F-236) sans dépendre du sous-objet IA non encore projeté.
 */

import { RuptureAnticipeeCddSectionPrefillRules } from './rupture-anticipee-cdd-section-prefill-rules';

describe('RuptureAnticipeeCddSectionPrefillRules', () => {
  describe('computePrefillCount', () => {
    it('retourne 0 sur fixture FR (pas de pré-fill IA actuellement)', () => {
      expect(
        RuptureAnticipeeCddSectionPrefillRules.computePrefillCount({
          aiData: {},
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(0);
    });

    it('retourne 0 hors France', () => {
      expect(
        RuptureAnticipeeCddSectionPrefillRules.computePrefillCount({
          aiData: {},
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

    it('retourne 0 avec workspaceCountry absent', () => {
      expect(
        RuptureAnticipeeCddSectionPrefillRules.computePrefillCount({
          aiData: {},
        }),
      ).toBe(0);
    });
  });
});
