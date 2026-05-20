import {
  ContestationAreSectionPrefillRules,
  computeAreTypeDecision,
  computeAreMontantConteste,
  computePrefillCount,
} from './contestation-are-section-prefill-rules';

describe('ContestationAreSectionPrefillRules', () => {
  const TODAY = '2025-01-15';
  const FR = 'FRANCE';

  it('cas 0 — non-FRANCE retourne 0', () => {
    expect(
      computePrefillCount({
        aiData: { dateLicenciement: '2024-05-01' },
        workspaceCountry: 'BELGIQUE',
        todayIso: TODAY,
      }),
    ).toBe(0);
  });

  it('cas M — date hors format retourne 0', () => {
    expect(
      computePrefillCount({
        aiData: { dateLicenciement: '01/05/2024' },
        workspaceCountry: FR,
        todayIso: TODAY,
      }),
    ).toBe(0);
  });

  it('cas N — date passée valide retourne count ≥ 1', () => {
    expect(
      computePrefillCount({
        aiData: { dateLicenciement: '2024-05-01' },
        workspaceCountry: FR,
        todayIso: TODAY,
      }),
    ).toBeGreaterThanOrEqual(1);
  });

  it('rejette date future', () => {
    expect(
      computePrefillCount({
        aiData: { dateLicenciement: '2099-12-31' },
        workspaceCountry: FR,
        todayIso: TODAY,
      }),
    ).toBe(0);
  });

  // SF-246-21 — computeAreTypeDecision
  describe('computeAreTypeDecision (SF-246-21)', () => {
    it('non-FRANCE → null', () => {
      expect(computeAreTypeDecision({ aiData: { areTypeDecision: 'RADIATION' }, workspaceCountry: 'BELGIQUE' })).toBeNull();
    });

    it('code inconnu → null', () => {
      expect(computeAreTypeDecision({ aiData: { areTypeDecision: 'CODE_INCONNU' }, workspaceCountry: FR })).toBeNull();
    });

    it('RADIATION → RADIATION', () => {
      expect(computeAreTypeDecision({ aiData: { areTypeDecision: 'RADIATION' }, workspaceCountry: FR })).toBe('RADIATION');
    });

    it('REFUS_INSCRIPTION → REFUS_OUVERTURE_DROITS', () => {
      expect(computeAreTypeDecision({ aiData: { areTypeDecision: 'REFUS_INSCRIPTION' }, workspaceCountry: FR })).toBe('REFUS_OUVERTURE_DROITS');
    });

    it('REDUCTION_ARE → MONTANT_INDEMNITE', () => {
      expect(computeAreTypeDecision({ aiData: { areTypeDecision: 'REDUCTION_ARE' }, workspaceCountry: FR })).toBe('MONTANT_INDEMNITE');
    });
  });

  // SF-246-21 — computeAreMontantConteste
  describe('computeAreMontantConteste (SF-246-21)', () => {
    it('non-FRANCE → null', () => {
      expect(computeAreMontantConteste({ aiData: { areMontantConteste: 5000 }, workspaceCountry: 'BELGIQUE' })).toBeNull();
    });

    it('montant ≤ 0 → null', () => {
      expect(computeAreMontantConteste({ aiData: { areMontantConteste: 0 }, workspaceCountry: FR })).toBeNull();
    });

    it('montant positif → retourne montant', () => {
      expect(computeAreMontantConteste({ aiData: { areMontantConteste: 3500 }, workspaceCountry: FR })).toBe(3500);
    });
  });

  // SF-246-21 — count max = 3 (date + typeDecision + montant)
  it('tous les champs remplis → count = 3', () => {
    expect(computePrefillCount({
      aiData: {
        dateLicenciement: '2024-05-01',
        areTypeDecision: 'RADIATION',
        areMontantConteste: 2000,
      },
      workspaceCountry: FR,
      todayIso: TODAY,
    })).toBe(3);
  });

  it('expose ContestationAreSectionPrefillRules barrel', () => {
    expect(ContestationAreSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
