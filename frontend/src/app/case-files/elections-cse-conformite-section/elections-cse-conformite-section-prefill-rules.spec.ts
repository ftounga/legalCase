/**
 * SF-212-32 — Tests unitaires du helper partagé
 * `elections-cse-conformite-section-prefill-rules`. Pattern aligné sur
 * SF-212-28 (burnout-reconnaissance-mp-section-prefill-rules).
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { ElectionsCseConformiteSectionPrefillRules } from './elections-cse-conformite-section-prefill-rules';

describe('ElectionsCseConformiteSectionPrefillRules', () => {
  const FULL_AI_DATA: TravailExtractedData = {
    electionCseDateElection: '2026-05-15',
    electionCsePapNegocie: true,
    electionCseCollegesConformes: true,
    electionCseResultatsContestes: false,
  };

  describe('computePrefillCount (parité stricte avec prefillFromAi)', () => {
    it('retourne 4 sur fixture IA FR complète', () => {
      expect(ElectionsCseConformiteSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'FRANCE',
      })).toBe(4);
    });

    it('retourne 0 hors France', () => {
      expect(ElectionsCseConformiteSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'BELGIQUE',
      })).toBe(0);
    });

    it('retourne 0 sans aiData', () => {
      expect(ElectionsCseConformiteSectionPrefillRules.computePrefillCount({
        aiData: null,
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });

    it('ignore les booléens non boolean', () => {
      const broken = { ...FULL_AI_DATA, electionCsePapNegocie: 'oui' as unknown as boolean };
      expect(ElectionsCseConformiteSectionPrefillRules.computePrefillCount({
        aiData: broken,
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });

    it('ignore date non ISO', () => {
      expect(ElectionsCseConformiteSectionPrefillRules.computePrefillCount({
        aiData: { ...FULL_AI_DATA, electionCseDateElection: '15/05/2026' },
        workspaceCountry: 'FRANCE',
      })).toBe(3);
    });
  });

  describe('computeDateElection', () => {
    it('retourne la date ISO si valide', () => {
      expect(ElectionsCseConformiteSectionPrefillRules.computeDateElection({
        aiData: { electionCseDateElection: '2026-05-15' },
        workspaceCountry: 'FRANCE',
      })).toBe('2026-05-15');
    });
    it('retourne null si format invalide', () => {
      expect(ElectionsCseConformiteSectionPrefillRules.computeDateElection({
        aiData: { electionCseDateElection: '15-05-2026' },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null hors France', () => {
      expect(ElectionsCseConformiteSectionPrefillRules.computeDateElection({
        aiData: { electionCseDateElection: '2026-05-15' },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
    it('retourne null si absent', () => {
      expect(ElectionsCseConformiteSectionPrefillRules.computeDateElection({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computePapNegocie', () => {
    it('retourne true si présent et boolean true', () => {
      expect(ElectionsCseConformiteSectionPrefillRules.computePapNegocie({
        aiData: { electionCsePapNegocie: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne false si présent et boolean false', () => {
      expect(ElectionsCseConformiteSectionPrefillRules.computePapNegocie({
        aiData: { electionCsePapNegocie: false },
        workspaceCountry: 'FRANCE',
      })).toBe(false);
    });
    it('retourne null hors France', () => {
      expect(ElectionsCseConformiteSectionPrefillRules.computePapNegocie({
        aiData: { electionCsePapNegocie: true },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  describe('computeCollegesConformes', () => {
    it('retourne le boolean tel quel', () => {
      expect(ElectionsCseConformiteSectionPrefillRules.computeCollegesConformes({
        aiData: { electionCseCollegesConformes: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
      expect(ElectionsCseConformiteSectionPrefillRules.computeCollegesConformes({
        aiData: { electionCseCollegesConformes: false },
        workspaceCountry: 'FRANCE',
      })).toBe(false);
    });
  });

  describe('computeResultatsContestes', () => {
    it('retourne le boolean tel quel', () => {
      expect(ElectionsCseConformiteSectionPrefillRules.computeResultatsContestes({
        aiData: { electionCseResultatsContestes: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne null hors France', () => {
      expect(ElectionsCseConformiteSectionPrefillRules.computeResultatsContestes({
        aiData: { electionCseResultatsContestes: true },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });
});
