import {
  ContestationAreSectionPrefillRules,
  computePrefillCount,
} from './contestation-are-section-prefill-rules';

describe('ContestationAreSectionPrefillRules', () => {
  const TODAY = '2025-01-15';

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
        workspaceCountry: 'FRANCE',
        todayIso: TODAY,
      }),
    ).toBe(0);
  });

  it('cas N — date passée valide retourne 1', () => {
    expect(
      computePrefillCount({
        aiData: { dateLicenciement: '2024-05-01' },
        workspaceCountry: 'FRANCE',
        todayIso: TODAY,
      }),
    ).toBe(1);
  });

  it('rejette date future', () => {
    expect(
      computePrefillCount({
        aiData: { dateLicenciement: '2099-12-31' },
        workspaceCountry: 'FRANCE',
        todayIso: TODAY,
      }),
    ).toBe(0);
  });

  it('expose ContestationAreSectionPrefillRules barrel', () => {
    expect(ContestationAreSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
