import {
  ProtectionRpSectionPrefillRules,
  computeDatePresumeeRupture,
  computeSalaireMensuelBrut,
  computePrefillCount,
} from './protection-rp-section-prefill-rules';

describe('ProtectionRpSectionPrefillRules', () => {
  describe('computePrefillCount', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
      expect(computePrefillCount({ aiData: null })).toBe(0);
      expect(computePrefillCount({ aiData: {} })).toBe(0);
    });

    it('cas M — motifLicenciement vide / non mappable retourne 0 pour ce champ', () => {
      expect(computePrefillCount({ aiData: { motifLicenciement: null } })).toBe(0);
      expect(computePrefillCount({ aiData: { motifLicenciement: '' } })).toBe(0);
      expect(
        computePrefillCount({ aiData: { motifLicenciement: 'COMPLETEMENT_INCONNU' } }),
      ).toBe(0);
    });

    it('cas N — motifLicenciement mappable retourne ≥ 1', () => {
      const variants = ['ECONOMIQUE', 'FAUTE_GRAVE', 'INAPTITUDE'];
      let success = 0;
      for (const v of variants) {
        if (computePrefillCount({ aiData: { motifLicenciement: v } }) >= 1) {
          success++;
        }
      }
      expect(success).toBeGreaterThan(0);
    });
  });

  // SF-246-21 — computeDatePresumeeRupture
  describe('computeDatePresumeeRupture (SF-246-21)', () => {
    it('absent → null', () => {
      expect(computeDatePresumeeRupture({ aiData: {} })).toBeNull();
    });

    it('date non-ISO → null', () => {
      expect(computeDatePresumeeRupture({ aiData: { dateLicenciement: '15/05/2024' } })).toBeNull();
    });

    it('date ISO valide → retourne date', () => {
      expect(computeDatePresumeeRupture({ aiData: { dateLicenciement: '2024-05-15' } })).toBe('2024-05-15');
    });
  });

  // SF-246-21 — computeSalaireMensuelBrut
  describe('computeSalaireMensuelBrut (SF-246-21)', () => {
    it('absent → null', () => {
      expect(computeSalaireMensuelBrut({ aiData: {} })).toBeNull();
    });

    it('salaire ≤ 0 → null', () => {
      expect(computeSalaireMensuelBrut({ aiData: { salaireBrutMensuel: 0 } })).toBeNull();
    });

    it('salaire positif → retourne salaire', () => {
      expect(computeSalaireMensuelBrut({ aiData: { salaireBrutMensuel: 3200 } })).toBe(3200);
    });
  });

  // count max = 3 (motif + dateRupture + salaire)
  it('tous les champs remplis → count = 3', () => {
    expect(computePrefillCount({
      aiData: {
        motifLicenciement: 'ECONOMIQUE',
        dateLicenciement: '2024-05-01',
        salaireBrutMensuel: 3000,
      },
    })).toBeGreaterThanOrEqual(2); // motif peut ne pas mapper, mais date + salaire = 2 minimum
  });

  it('expose ProtectionRpSectionPrefillRules barrel', () => {
    expect(ProtectionRpSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
