import {
  AutoriteParentaleBeSectionPrefillRules,
  computePrefillCount,
  computeModeHebergement,
} from './autorite-parentale-be-section-prefill-rules';

describe('AutoriteParentaleBeSectionPrefillRules — SF-246-28', () => {

  // Test A : nominal → computePrefillCount > 0
  it('Test A — aiData complet avec mode hébergement valide → count 1', () => {
    expect(
      computePrefillCount({
        aiData: { modeHebergementPrincipalBeDetecte: 'HEBERGEMENT_EGALITAIRE' },
      }),
    ).toBe(1);
  });

  // Test B : aiData null → 0
  it('Test B — aiData null → count 0', () => {
    expect(computePrefillCount({ aiData: null })).toBe(0);
  });

  // Test B2 : aiData undefined → 0
  it('aiData undefined → count 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  // Test C : code hors whitelist → 0
  it('Test C — code hors whitelist HEBERGEMENT_INCONNU → count 0', () => {
    expect(
      computePrefillCount({
        aiData: { modeHebergementPrincipalBeDetecte: 'HEBERGEMENT_INCONNU' },
      }),
    ).toBe(0);
  });

  // Test D : modeHebergementPrincipalBeDetecte null → 0
  it('Test D — modeHebergementPrincipalBeDetecte null → count 0', () => {
    expect(
      computePrefillCount({
        aiData: { modeHebergementPrincipalBeDetecte: null },
      }),
    ).toBe(0);
  });

  describe('computeModeHebergement', () => {
    it('retourne le code si dans la whitelist (HEBERGEMENT_PRINCIPAL_UN_PARENT)', () => {
      expect(
        computeModeHebergement({
          aiData: { modeHebergementPrincipalBeDetecte: 'HEBERGEMENT_PRINCIPAL_UN_PARENT' },
        }),
      ).toBe('HEBERGEMENT_PRINCIPAL_UN_PARENT');
    });

    it('retourne HEBERGEMENT_NON_FIXE si dans la whitelist', () => {
      expect(
        computeModeHebergement({
          aiData: { modeHebergementPrincipalBeDetecte: 'HEBERGEMENT_NON_FIXE' },
        }),
      ).toBe('HEBERGEMENT_NON_FIXE');
    });

    it('retourne null pour code hors whitelist', () => {
      expect(
        computeModeHebergement({
          aiData: { modeHebergementPrincipalBeDetecte: 'GARDE_EXCLUSIVE' },
        }),
      ).toBeNull();
    });

    it('retourne null si aiData est null', () => {
      expect(computeModeHebergement({ aiData: null })).toBeNull();
    });
  });

  it('expose le barrel AutoriteParentaleBeSectionPrefillRules', () => {
    expect(AutoriteParentaleBeSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
    expect(AutoriteParentaleBeSectionPrefillRules.computeModeHebergement).toBe(computeModeHebergement);
  });
});
