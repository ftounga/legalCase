import {
  RegimeCommunauteLegaleBeSectionPrefillRules,
  computePrefillCount,
  computeDateMariage,
  computeContratMariage,
} from './regime-communaute-legale-be-section-prefill-rules';

describe('RegimeCommunauteLegaleBeSectionPrefillRules — SF-246-28', () => {

  // Test A : nominal → count 2
  it('Test A — date mariage ISO + contrat mariage true → count 2', () => {
    expect(
      computePrefillCount({
        aiData: {
          dateMariageBeDetectee: '2015-06-20',
          contratMariageSigneBeDetecte: true,
        },
      }),
    ).toBe(2);
  });

  // Test B : aiData null → 0
  it('Test B — aiData null → count 0', () => {
    expect(computePrefillCount({ aiData: null })).toBe(0);
  });

  // Test C : sous-objet absent (tous null) → 0
  it('Test C — champs BE null → count 0', () => {
    expect(
      computePrefillCount({
        aiData: {
          dateMariageBeDetectee: null,
          contratMariageSigneBeDetecte: null,
        },
      }),
    ).toBe(0);
  });

  // Test D : date non ISO → rejetée, seul contrat compté
  it('Test D — date non ISO → rejetée, contrat seul compté', () => {
    expect(
      computePrefillCount({
        aiData: {
          dateMariageBeDetectee: '20/06/2015',  // non ISO
          contratMariageSigneBeDetecte: false,   // false est une valeur valide
        },
      }),
    ).toBe(1); // seul contrat (false est non-null)
  });

  describe('computeDateMariage', () => {
    it('retourne la date ISO valide', () => {
      expect(
        computeDateMariage({
          aiData: { dateMariageBeDetectee: '2015-06-20' },
        }),
      ).toBe('2015-06-20');
    });
    it('retourne null si date format non ISO (slash)', () => {
      expect(
        computeDateMariage({
          aiData: { dateMariageBeDetectee: '20/06/2015' },
        }),
      ).toBeNull();
    });
    it('retourne null si date absente', () => {
      expect(computeDateMariage({ aiData: {} })).toBeNull();
    });
    it('retourne null si aiData absent', () => {
      expect(computeDateMariage({})).toBeNull();
    });
  });

  describe('computeContratMariage', () => {
    it('retourne true si contrat signé', () => {
      expect(
        computeContratMariage({
          aiData: { contratMariageSigneBeDetecte: true },
        }),
      ).toBe(true);
    });
    it('retourne false si contrat non signé (false est une valeur valide)', () => {
      expect(
        computeContratMariage({
          aiData: { contratMariageSigneBeDetecte: false },
        }),
      ).toBe(false);
    });
    it('retourne null si le champ est absent', () => {
      expect(computeContratMariage({ aiData: {} })).toBeNull();
    });
    it('retourne null si aiData null', () => {
      expect(computeContratMariage({ aiData: null })).toBeNull();
    });
  });

  it('expose le barrel RegimeCommunauteLegaleBeSectionPrefillRules', () => {
    expect(RegimeCommunauteLegaleBeSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
    expect(RegimeCommunauteLegaleBeSectionPrefillRules.computeDateMariage).toBe(computeDateMariage);
    expect(RegimeCommunauteLegaleBeSectionPrefillRules.computeContratMariage).toBe(computeContratMariage);
  });
});
