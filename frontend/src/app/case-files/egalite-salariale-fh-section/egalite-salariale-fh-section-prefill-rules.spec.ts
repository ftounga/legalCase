/**
 * SF-212-24 — Tests unitaires du helper partagé
 * `egalite-salariale-fh-section-prefill-rules`. Pattern aligné sur
 * SF-212-20 (mise-a-pied-disciplinaire-section-prefill-rules).
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { EgaliteSalarialeFhSectionPrefillRules } from './egalite-salariale-fh-section-prefill-rules';

describe('EgaliteSalarialeFhSectionPrefillRules', () => {
  const FULL_AI_DATA: TravailExtractedData = {
    egaliteSalarialeDetail: {
      sexeSalarie: 'FEMME',
      salaireBrut: 2500,
      anciennete: 36,
      ecartPourcentage: 15.5,
    },
  };

  describe('computePrefillCount (parité stricte avec prefillFromAi)', () => {
    it('retourne 4 sur fixture IA FR complète', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'FRANCE',
      })).toBe(4);
    });

    it('retourne 0 hors France', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'BELGIQUE',
      })).toBe(0);
    });

    it('retourne 0 sans aiData', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computePrefillCount({
        aiData: null,
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });

    it('retourne 0 si egaliteSalarialeDetail null', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computePrefillCount({
        aiData: { egaliteSalarialeDetail: null },
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });

    it('ignore les valeurs hors plage du salaire', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computePrefillCount({
        aiData: { egaliteSalarialeDetail: { ...FULL_AI_DATA.egaliteSalarialeDetail!, salaireBrut: 99_999_999 } },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });

    it('ignore les valeurs négatives', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computePrefillCount({
        aiData: { egaliteSalarialeDetail: { ...FULL_AI_DATA.egaliteSalarialeDetail!, salaireBrut: -100 } },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });

    it('ignore l\'écart > 100 %', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computePrefillCount({
        aiData: { egaliteSalarialeDetail: { ...FULL_AI_DATA.egaliteSalarialeDetail!, ecartPourcentage: 150 } },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });

    it('ignore une ancienneté > 600 mois', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computePrefillCount({
        aiData: { egaliteSalarialeDetail: { ...FULL_AI_DATA.egaliteSalarialeDetail!, anciennete: 999 } },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });

    it('ignore un sexe hors enum', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computePrefillCount({
        aiData: { egaliteSalarialeDetail: { ...FULL_AI_DATA.egaliteSalarialeDetail!, sexeSalarie: 'AUTRE' } },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });
  });

  describe('computeSexeSalarie', () => {
    it('retourne FEMME si présent', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computeSexeSalarie({
        aiData: { egaliteSalarialeDetail: { sexeSalarie: 'FEMME' } },
        workspaceCountry: 'FRANCE',
      })).toBe('FEMME');
    });
    it('retourne HOMME si présent', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computeSexeSalarie({
        aiData: { egaliteSalarialeDetail: { sexeSalarie: 'HOMME' } },
        workspaceCountry: 'FRANCE',
      })).toBe('HOMME');
    });
    it('null hors enum', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computeSexeSalarie({
        aiData: { egaliteSalarialeDetail: { sexeSalarie: 'X' } },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('null hors France', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computeSexeSalarie({
        aiData: { egaliteSalarialeDetail: { sexeSalarie: 'FEMME' } },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
    it('null si type non string', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computeSexeSalarie({
        aiData: { egaliteSalarialeDetail: { sexeSalarie: 123 as unknown as string } },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeSalaireBrut', () => {
    it('retourne la valeur si valide', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computeSalaireBrut({
        aiData: { egaliteSalarialeDetail: { salaireBrut: 2500 } },
        workspaceCountry: 'FRANCE',
      })).toBe(2500);
    });
    it('null si Infinity', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computeSalaireBrut({
        aiData: { egaliteSalarialeDetail: { salaireBrut: Infinity } },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('null si valeur dépasse 1M', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computeSalaireBrut({
        aiData: { egaliteSalarialeDetail: { salaireBrut: 5_000_000 } },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('null hors France', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computeSalaireBrut({
        aiData: { egaliteSalarialeDetail: { salaireBrut: 2500 } },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  describe('computeAncienneteMois', () => {
    it('retourne l\'entier tronqué', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computeAncienneteMois({
        aiData: { egaliteSalarialeDetail: { anciennete: 36.7 } },
        workspaceCountry: 'FRANCE',
      })).toBe(36);
    });
    it('null si négatif', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computeAncienneteMois({
        aiData: { egaliteSalarialeDetail: { anciennete: -1 } },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('null si > 600 mois', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computeAncienneteMois({
        aiData: { egaliteSalarialeDetail: { anciennete: 1000 } },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeEcartPourcentage', () => {
    it('retourne la valeur si dans [0, 100]', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computeEcartPourcentage({
        aiData: { egaliteSalarialeDetail: { ecartPourcentage: 22.5 } },
        workspaceCountry: 'FRANCE',
      })).toBe(22.5);
    });
    it('null si > 100', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computeEcartPourcentage({
        aiData: { egaliteSalarialeDetail: { ecartPourcentage: 150 } },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('null si négatif', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computeEcartPourcentage({
        aiData: { egaliteSalarialeDetail: { ecartPourcentage: -5 } },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('null si NaN', () => {
      expect(EgaliteSalarialeFhSectionPrefillRules.computeEcartPourcentage({
        aiData: { egaliteSalarialeDetail: { ecartPourcentage: NaN } },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });
});
