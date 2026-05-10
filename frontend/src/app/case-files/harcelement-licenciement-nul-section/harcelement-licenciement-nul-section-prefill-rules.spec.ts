import {
  HarcelementLicenciementNulSectionPrefillRules,
  computePrefillCount,
  computeMotifNullite,
} from './harcelement-licenciement-nul-section-prefill-rules';

describe('HarcelementLicenciementNulSectionPrefillRules', () => {
  describe('computePrefillCount', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('cas M — salaire seul retourne 1 (pas de gate FR)', () => {
      expect(
        computePrefillCount({
          aiData: { salaireBrutMensuel: 2500 },
        }),
      ).toBe(1);
    });

    it('cas N — salaire + motif en FRANCE retourne 2', () => {
      expect(
        computePrefillCount({
          aiData: {
            salaireBrutMensuel: 2500,
            motifNullitePressenti: 'HARCELEMENT_MORAL',
          },
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(2);
    });

    it('motif ignoré en Belgique (gate pays)', () => {
      expect(
        computeMotifNullite({
          aiData: { motifNullitePressenti: 'HARCELEMENT_MORAL' },
          workspaceCountry: 'BELGIQUE',
        }),
      ).toBeNull();
    });

    it('rejette salaire <= 0', () => {
      expect(computePrefillCount({ aiData: { salaireBrutMensuel: 0 } })).toBe(0);
    });

    it('mappe MATERNITE_PATERNITE → GROSSESSE', () => {
      expect(
        computeMotifNullite({
          aiData: { motifNullitePressenti: 'MATERNITE_PATERNITE' },
          workspaceCountry: 'FRANCE',
        }),
      ).toBe('GROSSESSE');
    });
  });

  it('expose HarcelementLicenciementNulSectionPrefillRules barrel', () => {
    expect(HarcelementLicenciementNulSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
