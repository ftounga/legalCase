/**
 * SF-212-34 — Tests unitaires du helper partagé
 * `temps-partiel-requalification-section-prefill-rules`. Pattern aligné sur
 * SF-212-28 (burnout-reconnaissance-mp-section-prefill-rules).
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { TempsPartielRequalificationSectionPrefillRules } from './temps-partiel-requalification-section-prefill-rules';

describe('TempsPartielRequalificationSectionPrefillRules', () => {
  const FULL_AI_DATA: TravailExtractedData = {
    tempsPartielDureeContractuelle: 24,
    tempsPartielMentionsDuree: false,
    tempsPartielMentionsRepartition: true,
    tempsPartielHCMoyenne: 6,
  };

  describe('computePrefillCount (parité stricte avec prefillFromAi)', () => {
    it('retourne 4 sur fixture IA FR complète', () => {
      expect(TempsPartielRequalificationSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'FRANCE',
      })).toBe(4);
    });

    it('retourne 0 hors France', () => {
      expect(TempsPartielRequalificationSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'BELGIQUE',
      })).toBe(0);
    });

    it('retourne 0 sans aiData', () => {
      expect(TempsPartielRequalificationSectionPrefillRules.computePrefillCount({
        aiData: null,
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });

    it('ignore les booléens non boolean', () => {
      const broken = { ...FULL_AI_DATA, tempsPartielMentionsDuree: 'non' as unknown as boolean };
      expect(TempsPartielRequalificationSectionPrefillRules.computePrefillCount({
        aiData: broken,
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });

    it('ignore durée contractuelle >= 35h (incohérent avec temps partiel)', () => {
      expect(TempsPartielRequalificationSectionPrefillRules.computePrefillCount({
        aiData: { ...FULL_AI_DATA, tempsPartielDureeContractuelle: 35 },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
      expect(TempsPartielRequalificationSectionPrefillRules.computePrefillCount({
        aiData: { ...FULL_AI_DATA, tempsPartielDureeContractuelle: 0 },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });

    it('ignore HC moyenne hors plage [0, 20]', () => {
      expect(TempsPartielRequalificationSectionPrefillRules.computePrefillCount({
        aiData: { ...FULL_AI_DATA, tempsPartielHCMoyenne: 25 },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
      expect(TempsPartielRequalificationSectionPrefillRules.computePrefillCount({
        aiData: { ...FULL_AI_DATA, tempsPartielHCMoyenne: -1 },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });
  });

  describe('computeDureeContractuelle', () => {
    it('accepte une durée 24h', () => {
      expect(TempsPartielRequalificationSectionPrefillRules.computeDureeContractuelle({
        aiData: { tempsPartielDureeContractuelle: 24 },
        workspaceCountry: 'FRANCE',
      })).toBe(24);
    });

    it('rejette durée >= 35h', () => {
      expect(TempsPartielRequalificationSectionPrefillRules.computeDureeContractuelle({
        aiData: { tempsPartielDureeContractuelle: 35 },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });

    it('retourne null hors France', () => {
      expect(TempsPartielRequalificationSectionPrefillRules.computeDureeContractuelle({
        aiData: { tempsPartielDureeContractuelle: 24 },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  describe('computeMentionsDuree', () => {
    it('retourne true si présent et boolean true', () => {
      expect(TempsPartielRequalificationSectionPrefillRules.computeMentionsDuree({
        aiData: { tempsPartielMentionsDuree: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne false si présent et boolean false', () => {
      expect(TempsPartielRequalificationSectionPrefillRules.computeMentionsDuree({
        aiData: { tempsPartielMentionsDuree: false },
        workspaceCountry: 'FRANCE',
      })).toBe(false);
    });
    it('retourne null hors France', () => {
      expect(TempsPartielRequalificationSectionPrefillRules.computeMentionsDuree({
        aiData: { tempsPartielMentionsDuree: false },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
    it('retourne null si absent', () => {
      expect(TempsPartielRequalificationSectionPrefillRules.computeMentionsDuree({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeMentionsRepartition', () => {
    it('retourne le boolean tel quel', () => {
      expect(TempsPartielRequalificationSectionPrefillRules.computeMentionsRepartition({
        aiData: { tempsPartielMentionsRepartition: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
      expect(TempsPartielRequalificationSectionPrefillRules.computeMentionsRepartition({
        aiData: { tempsPartielMentionsRepartition: false },
        workspaceCountry: 'FRANCE',
      })).toBe(false);
    });
  });

  describe('computeHCMoyenne', () => {
    it('accepte 0 et 20', () => {
      expect(TempsPartielRequalificationSectionPrefillRules.computeHCMoyenne({
        aiData: { tempsPartielHCMoyenne: 0 },
        workspaceCountry: 'FRANCE',
      })).toBe(0);
      expect(TempsPartielRequalificationSectionPrefillRules.computeHCMoyenne({
        aiData: { tempsPartielHCMoyenne: 20 },
        workspaceCountry: 'FRANCE',
      })).toBe(20);
    });
    it('rejette HC > 20', () => {
      expect(TempsPartielRequalificationSectionPrefillRules.computeHCMoyenne({
        aiData: { tempsPartielHCMoyenne: 25 },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('rejette HC négative', () => {
      expect(TempsPartielRequalificationSectionPrefillRules.computeHCMoyenne({
        aiData: { tempsPartielHCMoyenne: -1 },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null hors France', () => {
      expect(TempsPartielRequalificationSectionPrefillRules.computeHCMoyenne({
        aiData: { tempsPartielHCMoyenne: 5 },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });
});
