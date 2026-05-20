import {
  LiquidationPartageBeSectionPrefillRules,
  computePrefillCount,
  computeDateDesignationNotaire,
  computeDateOuvertureOperations,
  computeDateNotificationProjet,
  computeDateHomologation,
} from './liquidation-partage-be-section-prefill-rules';

describe('LiquidationPartageBeSectionPrefillRules — SF-246-28', () => {

  // Test A : nominal → count 4
  it('Test A — aiData 4 dates ISO valides → count 4', () => {
    expect(
      computePrefillCount({
        aiData: {
          dateDesignationNotaireBeDetectee: '2025-03-10',
          dateOuvertureOperationsBeDetectee: '2025-04-15',
          dateNotificationProjetBeDetectee: '2025-09-01',
          dateHomologationBeDetectee: '2026-02-20',
        },
      }),
    ).toBe(4);
  });

  // Test B : aiData null → 0
  it('Test B — aiData null → count 0', () => {
    expect(computePrefillCount({ aiData: null })).toBe(0);
  });

  // Test C : sous-objet absent (tous null) → 0
  it('Test C — tous les champs de dates null → count 0', () => {
    expect(
      computePrefillCount({
        aiData: {
          dateDesignationNotaireBeDetectee: null,
          dateOuvertureOperationsBeDetectee: null,
          dateNotificationProjetBeDetectee: null,
          dateHomologationBeDetectee: null,
        },
      }),
    ).toBe(0);
  });

  // Test D : format non ISO → rejeté
  it('Test D — date format jj/mm/aaaa (non ISO) → rejetée, count réduit', () => {
    expect(
      computePrefillCount({
        aiData: {
          dateDesignationNotaireBeDetectee: '10/03/2025',  // non ISO
          dateOuvertureOperationsBeDetectee: '2025-04-15', // ISO valide
          dateNotificationProjetBeDetectee: null,
          dateHomologationBeDetectee: null,
        },
      }),
    ).toBe(1); // seule dateOuverture valide
  });

  describe('computeDateDesignationNotaire', () => {
    it('retourne la date ISO valide', () => {
      expect(
        computeDateDesignationNotaire({
          aiData: { dateDesignationNotaireBeDetectee: '2025-03-10' },
        }),
      ).toBe('2025-03-10');
    });
    it('retourne null si date non ISO', () => {
      expect(
        computeDateDesignationNotaire({
          aiData: { dateDesignationNotaireBeDetectee: '10-03-2025' },
        }),
      ).toBeNull();
    });
    it('retourne null si aiData absent', () => {
      expect(computeDateDesignationNotaire({})).toBeNull();
    });
  });

  describe('computeDateNotificationProjet', () => {
    it('retourne la date ISO valide', () => {
      expect(
        computeDateNotificationProjet({
          aiData: { dateNotificationProjetBeDetectee: '2025-09-01' },
        }),
      ).toBe('2025-09-01');
    });
    it('retourne null si date manquante', () => {
      expect(
        computeDateNotificationProjet({
          aiData: { dateNotificationProjetBeDetectee: null },
        }),
      ).toBeNull();
    });
  });

  describe('computeDateHomologation', () => {
    it('retourne la date ISO valide', () => {
      expect(
        computeDateHomologation({
          aiData: { dateHomologationBeDetectee: '2026-02-20' },
        }),
      ).toBe('2026-02-20');
    });
    it('retourne null si date vide string', () => {
      expect(
        computeDateHomologation({
          aiData: { dateHomologationBeDetectee: '' },
        }),
      ).toBeNull();
    });
  });

  it('expose le barrel LiquidationPartageBeSectionPrefillRules', () => {
    expect(LiquidationPartageBeSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
    expect(LiquidationPartageBeSectionPrefillRules.computeDateDesignationNotaire).toBe(computeDateDesignationNotaire);
    expect(LiquidationPartageBeSectionPrefillRules.computeDateOuvertureOperations).toBe(computeDateOuvertureOperations);
    expect(LiquidationPartageBeSectionPrefillRules.computeDateNotificationProjet).toBe(computeDateNotificationProjet);
    expect(LiquidationPartageBeSectionPrefillRules.computeDateHomologation).toBe(computeDateHomologation);
  });
});
