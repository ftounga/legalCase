/**
 * SF-212-16 — Tests unitaires du helper partagé
 * `teletravail-accord-section-prefill-rules`. Pattern aligné sur SF-212-14
 * (mutation-clause-mobilite-section-prefill-rules).
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { TeletravailAccordSectionPrefillRules } from './teletravail-accord-section-prefill-rules';

describe('TeletravailAccordSectionPrefillRules', () => {
  const FULL_AI_DATA: TravailExtractedData = {
    teletravailCadre: 'ACCORD_COLLECTIF',
    teletravailDoubleVolontariat: true,
    teletravailIndemniteVersee: true,
    teletravailMontantIndemniteJournalier: 2.60,
    teletravailAccidentDomicile: false,
    teletravailRetourBureauImpose: false,
    teletravailRefusCauseIncrimination: false,
  };

  describe('computePrefillCount (parité stricte avec prefillFromAi)', () => {
    it('retourne 7 sur fixture IA FR complète', () => {
      expect(TeletravailAccordSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'FRANCE',
      })).toBe(7);
    });

    it('retourne 0 hors France', () => {
      expect(TeletravailAccordSectionPrefillRules.computePrefillCount({
        aiData: FULL_AI_DATA,
        workspaceCountry: 'BELGIQUE',
      })).toBe(0);
    });

    it('retourne 0 sans aiData', () => {
      expect(TeletravailAccordSectionPrefillRules.computePrefillCount({
        aiData: null,
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });

    it('ignore les valeurs hors plage du montant journalier', () => {
      expect(TeletravailAccordSectionPrefillRules.computePrefillCount({
        aiData: { ...FULL_AI_DATA, teletravailMontantIndemniteJournalier: 999 },
        workspaceCountry: 'FRANCE',
      })).toBe(6);
    });

    it('ignore les booléens non boolean', () => {
      const broken = { ...FULL_AI_DATA, teletravailDoubleVolontariat: 'oui' as unknown as boolean };
      expect(TeletravailAccordSectionPrefillRules.computePrefillCount({
        aiData: broken,
        workspaceCountry: 'FRANCE',
      })).toBe(6);
    });

    it('ignore le montant négatif', () => {
      expect(TeletravailAccordSectionPrefillRules.computePrefillCount({
        aiData: { ...FULL_AI_DATA, teletravailMontantIndemniteJournalier: -1 },
        workspaceCountry: 'FRANCE',
      })).toBe(6);
    });

    it('ignore un cadre hors whitelist', () => {
      const broken = { ...FULL_AI_DATA, teletravailCadre: 'INVALID' };
      expect(TeletravailAccordSectionPrefillRules.computePrefillCount({
        aiData: broken,
        workspaceCountry: 'FRANCE',
      })).toBe(6);
    });
  });

  describe('computeCadre', () => {
    it('retourne ACCORD_COLLECTIF si présent et valide', () => {
      expect(TeletravailAccordSectionPrefillRules.computeCadre({
        aiData: { teletravailCadre: 'ACCORD_COLLECTIF' },
        workspaceCountry: 'FRANCE',
      })).toBe('ACCORD_COLLECTIF');
    });
    it('retourne CHARTE_UNILATERALE si présent et valide', () => {
      expect(TeletravailAccordSectionPrefillRules.computeCadre({
        aiData: { teletravailCadre: 'CHARTE_UNILATERALE' },
        workspaceCountry: 'FRANCE',
      })).toBe('CHARTE_UNILATERALE');
    });
    it('retourne AUCUN si présent et valide', () => {
      expect(TeletravailAccordSectionPrefillRules.computeCadre({
        aiData: { teletravailCadre: 'AUCUN' },
        workspaceCountry: 'FRANCE',
      })).toBe('AUCUN');
    });
    it('retourne null pour valeur hors whitelist', () => {
      expect(TeletravailAccordSectionPrefillRules.computeCadre({
        aiData: { teletravailCadre: 'AUTRE_CHOSE' },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null si absent', () => {
      expect(TeletravailAccordSectionPrefillRules.computeCadre({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null hors France', () => {
      expect(TeletravailAccordSectionPrefillRules.computeCadre({
        aiData: { teletravailCadre: 'ACCORD_COLLECTIF' },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  describe('computeDoubleVolontariat', () => {
    it('retourne true si présent et booléen', () => {
      expect(TeletravailAccordSectionPrefillRules.computeDoubleVolontariat({
        aiData: { teletravailDoubleVolontariat: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne false si présent et booléen false', () => {
      expect(TeletravailAccordSectionPrefillRules.computeDoubleVolontariat({
        aiData: { teletravailDoubleVolontariat: false },
        workspaceCountry: 'FRANCE',
      })).toBe(false);
    });
    it('retourne null si absent', () => {
      expect(TeletravailAccordSectionPrefillRules.computeDoubleVolontariat({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeIndemniteVersee', () => {
    it('retourne true si présent et booléen', () => {
      expect(TeletravailAccordSectionPrefillRules.computeIndemniteVersee({
        aiData: { teletravailIndemniteVersee: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne null hors France', () => {
      expect(TeletravailAccordSectionPrefillRules.computeIndemniteVersee({
        aiData: { teletravailIndemniteVersee: true },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  describe('computeMontantJournalier', () => {
    it('retourne la valeur décimale dans la plage [0, 50]', () => {
      expect(TeletravailAccordSectionPrefillRules.computeMontantJournalier({
        aiData: { teletravailMontantIndemniteJournalier: 2.60 },
        workspaceCountry: 'FRANCE',
      })).toBe(2.60);
    });
    it('retourne 0 (limite basse)', () => {
      expect(TeletravailAccordSectionPrefillRules.computeMontantJournalier({
        aiData: { teletravailMontantIndemniteJournalier: 0 },
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });
    it('retourne null pour valeur négative', () => {
      expect(TeletravailAccordSectionPrefillRules.computeMontantJournalier({
        aiData: { teletravailMontantIndemniteJournalier: -1 },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null pour valeur > 50', () => {
      expect(TeletravailAccordSectionPrefillRules.computeMontantJournalier({
        aiData: { teletravailMontantIndemniteJournalier: 100 },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null si absent', () => {
      expect(TeletravailAccordSectionPrefillRules.computeMontantJournalier({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
    it('retourne null pour non-number', () => {
      expect(TeletravailAccordSectionPrefillRules.computeMontantJournalier({
        aiData: { teletravailMontantIndemniteJournalier: '4' as unknown as number },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeAccidentDomicile', () => {
    it('retourne true si présent et booléen', () => {
      expect(TeletravailAccordSectionPrefillRules.computeAccidentDomicile({
        aiData: { teletravailAccidentDomicile: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne null si absent', () => {
      expect(TeletravailAccordSectionPrefillRules.computeAccidentDomicile({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computeRetourBureauImpose', () => {
    it('retourne true si présent et booléen', () => {
      expect(TeletravailAccordSectionPrefillRules.computeRetourBureauImpose({
        aiData: { teletravailRetourBureauImpose: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne null hors France', () => {
      expect(TeletravailAccordSectionPrefillRules.computeRetourBureauImpose({
        aiData: { teletravailRetourBureauImpose: true },
        workspaceCountry: 'BELGIQUE',
      })).toBeNull();
    });
  });

  describe('computeRefusIncrimine', () => {
    it('retourne true si présent et booléen', () => {
      expect(TeletravailAccordSectionPrefillRules.computeRefusIncrimine({
        aiData: { teletravailRefusCauseIncrimination: true },
        workspaceCountry: 'FRANCE',
      })).toBe(true);
    });
    it('retourne null si absent', () => {
      expect(TeletravailAccordSectionPrefillRules.computeRefusIncrimine({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });
});
