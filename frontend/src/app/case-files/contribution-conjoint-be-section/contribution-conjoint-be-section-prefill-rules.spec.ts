import {
  ContributionConjointBeSectionPrefillRules,
  computePrefillCount,
  computeDureeMariage,
  computeRevenuCreancier,
  computeRevenuDebiteur,
} from './contribution-conjoint-be-section-prefill-rules';

describe('ContributionConjointBeSectionPrefillRules — SF-246-28', () => {

  // Test A : nominal → count 3
  it('Test A — aiData complet 3 champs → count 3', () => {
    expect(
      computePrefillCount({
        aiData: {
          dureeMariageAnneesBeDetectee: 8,
          revenuMensuelCreancierBeDetecte: 1200,
          revenuMensuelDebiteurBeDetecte: 2800,
        },
      }),
    ).toBe(3);
  });

  // Test B : aiData null → 0
  it('Test B — aiData null → count 0', () => {
    expect(computePrefillCount({ aiData: null })).toBe(0);
  });

  // Test C : tous champs null → 0
  it('Test C — tous champs BE null → count 0', () => {
    expect(
      computePrefillCount({
        aiData: {
          dureeMariageAnneesBeDetectee: null,
          revenuMensuelCreancierBeDetecte: null,
          revenuMensuelDebiteurBeDetecte: null,
        },
      }),
    ).toBe(0);
  });

  // Test D : bornes invalides
  it('Test D — durée hors [0,80] → non comptée', () => {
    expect(
      computePrefillCount({
        aiData: {
          dureeMariageAnneesBeDetectee: 85,  // hors plage
          revenuMensuelCreancierBeDetecte: 900,
          revenuMensuelDebiteurBeDetecte: 2000,
        },
      }),
    ).toBe(2); // seulement creancier + debiteur
  });

  describe('computeDureeMariage', () => {
    it('retourne 0 si durée = 0 (mariage récent)', () => {
      expect(computeDureeMariage({ aiData: { dureeMariageAnneesBeDetectee: 0 } })).toBe(0);
    });
    it('retourne 80 si durée = 80 (borne max)', () => {
      expect(computeDureeMariage({ aiData: { dureeMariageAnneesBeDetectee: 80 } })).toBe(80);
    });
    it('retourne null si durée > 80', () => {
      expect(computeDureeMariage({ aiData: { dureeMariageAnneesBeDetectee: 81 } })).toBeNull();
    });
    it('retourne null si aiData absent', () => {
      expect(computeDureeMariage({})).toBeNull();
    });
  });

  describe('computeRevenuCreancier', () => {
    it('retourne 0 si revenu = 0 (créancier sans revenu — 0 est valide)', () => {
      expect(computeRevenuCreancier({ aiData: { revenuMensuelCreancierBeDetecte: 0 } })).toBe(0);
    });
    it('retourne null si revenu négatif', () => {
      expect(computeRevenuCreancier({ aiData: { revenuMensuelCreancierBeDetecte: -500 } })).toBeNull();
    });
  });

  describe('computeRevenuDebiteur', () => {
    it('retourne la valeur positive', () => {
      expect(computeRevenuDebiteur({ aiData: { revenuMensuelDebiteurBeDetecte: 3200 } })).toBe(3200);
    });
    it('retourne null si revenu négatif', () => {
      expect(computeRevenuDebiteur({ aiData: { revenuMensuelDebiteurBeDetecte: -1 } })).toBeNull();
    });
  });

  it('expose le barrel ContributionConjointBeSectionPrefillRules', () => {
    expect(ContributionConjointBeSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
    expect(ContributionConjointBeSectionPrefillRules.computeDureeMariage).toBe(computeDureeMariage);
    expect(ContributionConjointBeSectionPrefillRules.computeRevenuCreancier).toBe(computeRevenuCreancier);
    expect(ContributionConjointBeSectionPrefillRules.computeRevenuDebiteur).toBe(computeRevenuDebiteur);
  });
});
