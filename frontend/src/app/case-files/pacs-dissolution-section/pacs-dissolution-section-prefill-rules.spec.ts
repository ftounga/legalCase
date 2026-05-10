/** F-236 SF-236-02 — Tests `PacsDissolutionPrefillRules`. */
import {
  PacsDissolutionPrefillRules,
  computePrefillCount,
} from './pacs-dissolution-section-prefill-rules';

describe('PacsDissolutionPrefillRules', () => {
  it('cas 0', () => {
    expect(computePrefillCount({})).toBe(0);
  });

  it('cas M — 3/5', () => {
    expect(
      computePrefillCount({
        aiData: {
          dateConclusionPacs: '2018-05-12',
          modeDissolutionPacsDetecte: 'DECLARATION_UNILATERALE',
          patrimoineCommunSignificatifDetecte: false,
        },
      } as any),
    ).toBe(3);
  });

  it('rejette date non-ISO', () => {
    expect(
      computePrefillCount({ aiData: { dateConclusionPacs: '12/05/2018' } } as any),
    ).toBe(0);
  });

  it('cas N — 5/5', () => {
    expect(
      computePrefillCount({
        aiData: {
          dateConclusionPacs: '2018-05-12',
          modeDissolutionPacsDetecte: 'DECLARATION_CONJOINTE',
          regimeBiensPacsDetecte: 'SEPARATION_BIENS',
          creancesAllegueesDetectees: ['AUCUNE'],
          patrimoineCommunSignificatifDetecte: true,
        },
      } as any),
    ).toBe(5);
  });

  it('surface', () => {
    expect(PacsDissolutionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
