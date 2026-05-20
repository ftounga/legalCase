import {
  ContributionAlimentaireEnfantsBeSectionPrefillRules,
  computePrefillCount,
  computeNombreEnfants,
  computeRevenuParent1,
  computeRevenuParent2,
  computeAllocationsFamiliales,
  computeNuitsParent1,
  computeNuitsParent2,
} from './contribution-alimentaire-enfants-be-section-prefill-rules';

const FULL_AI_DATA = {
  nombreEnfantsBeDetecte: 2,
  revenuMensuelParent1BeDetecte: 2800,
  revenuMensuelParent2BeDetecte: 1900,
  allocationsFamilialesMensuellesBeDetectees: 320,
  nuitsHebergementParent1BeDetectees: 15,
  nuitsHebergementParent2BeDetectees: 15,
};

describe('ContributionAlimentaireEnfantsBeSectionPrefillRules — SF-246-28', () => {

  // Test A : nominal → count 6
  it('Test A — aiData complet 6 champs → count 6', () => {
    expect(computePrefillCount({ aiData: FULL_AI_DATA })).toBe(6);
  });

  // Test B : aiData null → 0
  it('Test B — aiData null → count 0', () => {
    expect(computePrefillCount({ aiData: null })).toBe(0);
  });

  // Test C : sous-objet famille_be_detection_v2 absent (tous champs null) → 0
  it('Test C — tous les champs BE null → count 0', () => {
    expect(
      computePrefillCount({
        aiData: {
          nombreEnfantsBeDetecte: null,
          revenuMensuelParent1BeDetecte: null,
          revenuMensuelParent2BeDetecte: null,
          allocationsFamilialesMensuellesBeDetectees: null,
          nuitsHebergementParent1BeDetectees: null,
          nuitsHebergementParent2BeDetectees: null,
        },
      }),
    ).toBe(0);
  });

  // Test D : bornes invalides → count réduit
  it('Test D — nombreEnfants hors [1,12] et nuits hors [0,30] → non comptés', () => {
    expect(
      computePrefillCount({
        aiData: {
          nombreEnfantsBeDetecte: 13,          // hors plage max
          revenuMensuelParent1BeDetecte: 2000,
          revenuMensuelParent2BeDetecte: 1500,
          allocationsFamilialesMensuellesBeDetectees: 0, // 0 est valide (≥ 0)
          nuitsHebergementParent1BeDetectees: 31,        // hors plage
          nuitsHebergementParent2BeDetectees: 15,
        },
      }),
    ).toBe(4); // P1, P2, alloc, nuitsP2 (nombreEnfants et nuitsP1 rejetés)
  });

  describe('computeNombreEnfants', () => {
    it('retourne la valeur si dans [1, 12]', () => {
      expect(computeNombreEnfants({ aiData: { nombreEnfantsBeDetecte: 3 } })).toBe(3);
    });
    it('retourne null si 0 (hors plage min)', () => {
      expect(computeNombreEnfants({ aiData: { nombreEnfantsBeDetecte: 0 } })).toBeNull();
    });
    it('retourne null si null', () => {
      expect(computeNombreEnfants({ aiData: { nombreEnfantsBeDetecte: null } })).toBeNull();
    });
  });

  describe('computeRevenuParent1', () => {
    it('retourne 0 si revenu = 0 (0 est valide)', () => {
      expect(computeRevenuParent1({ aiData: { revenuMensuelParent1BeDetecte: 0 } })).toBe(0);
    });
    it('retourne null si revenu négatif', () => {
      expect(computeRevenuParent1({ aiData: { revenuMensuelParent1BeDetecte: -100 } })).toBeNull();
    });
    it('retourne null si aiData absent', () => {
      expect(computeRevenuParent1({})).toBeNull();
    });
  });

  describe('computeNuitsParent1', () => {
    it('retourne 0 si nuits = 0 (valide)', () => {
      expect(computeNuitsParent1({ aiData: { nuitsHebergementParent1BeDetectees: 0 } })).toBe(0);
    });
    it('retourne 30 si nuits = 30 (borne max)', () => {
      expect(computeNuitsParent1({ aiData: { nuitsHebergementParent1BeDetectees: 30 } })).toBe(30);
    });
    it('retourne null si nuits = 31 (hors plage)', () => {
      expect(computeNuitsParent1({ aiData: { nuitsHebergementParent1BeDetectees: 31 } })).toBeNull();
    });
  });

  it('expose le barrel ContributionAlimentaireEnfantsBeSectionPrefillRules', () => {
    expect(ContributionAlimentaireEnfantsBeSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
    expect(ContributionAlimentaireEnfantsBeSectionPrefillRules.computeNombreEnfants).toBe(computeNombreEnfants);
    expect(ContributionAlimentaireEnfantsBeSectionPrefillRules.computeRevenuParent1).toBe(computeRevenuParent1);
    expect(ContributionAlimentaireEnfantsBeSectionPrefillRules.computeRevenuParent2).toBe(computeRevenuParent2);
    expect(ContributionAlimentaireEnfantsBeSectionPrefillRules.computeAllocationsFamiliales).toBe(computeAllocationsFamiliales);
    expect(ContributionAlimentaireEnfantsBeSectionPrefillRules.computeNuitsParent1).toBe(computeNuitsParent1);
    expect(ContributionAlimentaireEnfantsBeSectionPrefillRules.computeNuitsParent2).toBe(computeNuitsParent2);
  });
});
