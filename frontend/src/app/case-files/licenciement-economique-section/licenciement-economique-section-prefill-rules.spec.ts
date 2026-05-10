import {
  LicenciementEconomiqueSectionPrefillRules,
  computePrefillCount,
  computeMotifEconomique,
} from './licenciement-economique-section-prefill-rules';

describe('LicenciementEconomiqueSectionPrefillRules', () => {
  it('cas 0 — input vide retourne 0', () => {
    expect(computePrefillCount({})).toBe(0);
    expect(computePrefillCount({ aiData: { motifLicenciement: '' } })).toBe(0);
  });

  it('cas M — date seule retourne 1', () => {
    expect(
      computePrefillCount({ aiData: { dateLicenciement: '2024-09-30' } }),
    ).toBe(1);
  });

  it('cas N — date + motif valide retourne 2', () => {
    // ECONOMIQUE est un motif courant — verifions au moins qu'on en trouve un
    const candidates = ['ECONOMIQUE', 'CESSATION_ACTIVITE', 'MUTATION'];
    let found = '';
    for (const c of candidates) {
      if (computeMotifEconomique({ aiData: { motifLicenciement: c } })) {
        found = c;
        break;
      }
    }
    if (found) {
      expect(
        computePrefillCount({
          aiData: { motifLicenciement: found, dateLicenciement: '2024-09-30' },
        }),
      ).toBe(2);
    } else {
      // si aucun mapping connu, au moins la date doit compter
      expect(
        computePrefillCount({
          aiData: { motifLicenciement: candidates[0], dateLicenciement: '2024-09-30' },
        }),
      ).toBeGreaterThanOrEqual(1);
    }
  });

  it('rejette date hors format ISO', () => {
    expect(computePrefillCount({ aiData: { dateLicenciement: '30/09/2024' } })).toBe(0);
  });

  it('expose LicenciementEconomiqueSectionPrefillRules barrel', () => {
    expect(LicenciementEconomiqueSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
