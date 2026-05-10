import {
  MotifGraveBeSectionPrefillRules,
  computePrefillCount,
  computeDateNotificationRupture,
  computeSalaireMensuelReference,
} from './motif-grave-be-section-prefill-rules';

describe('MotifGraveBeSectionPrefillRules', () => {
  const BE = 'BELGIQUE';

  describe('computePrefillCount', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({ workspaceCountry: BE })).toBe(0);
      expect(computePrefillCount({ workspaceCountry: BE, aiData: null })).toBe(0);
      expect(computePrefillCount({ workspaceCountry: BE, aiData: {} })).toBe(0);
    });

    it('cas M — date seule retourne 1', () => {
      expect(
        computePrefillCount({
          workspaceCountry: BE,
          aiData: { dateLicenciement: '2024-05-01' },
        }),
      ).toBe(1);
    });

    it('cas N — 2 champs retourne 2', () => {
      expect(
        computePrefillCount({
          workspaceCountry: BE,
          aiData: { dateLicenciement: '2024-05-01', salaireBrutMensuel: 2500 },
        }),
      ).toBe(2);
    });
  });

  it('gating BE : workspaceCountry FRANCE retourne 0 même avec aiData complet', () => {
    expect(
      computePrefillCount({
        workspaceCountry: 'FRANCE',
        aiData: { dateLicenciement: '2024-05-01', salaireBrutMensuel: 2500 },
      }),
    ).toBe(0);
    expect(
      computeDateNotificationRupture({
        workspaceCountry: 'FRANCE',
        aiData: { dateLicenciement: '2024-05-01' },
      }),
    ).toBeNull();
    expect(
      computeSalaireMensuelReference({
        workspaceCountry: 'FRANCE',
        aiData: { salaireBrutMensuel: 2500 },
      }),
    ).toBeNull();
  });

  it('gating BE : workspaceCountry absent retourne 0', () => {
    expect(
      computePrefillCount({
        aiData: { dateLicenciement: '2024-05-01', salaireBrutMensuel: 2500 },
      }),
    ).toBe(0);
  });

  it('rejette salaire <= 0', () => {
    expect(
      computeSalaireMensuelReference({
        workspaceCountry: BE,
        aiData: { salaireBrutMensuel: 0 },
      }),
    ).toBeNull();
    expect(
      computeSalaireMensuelReference({
        workspaceCountry: BE,
        aiData: { salaireBrutMensuel: -100 },
      }),
    ).toBeNull();
  });

  it('expose barrel', () => {
    expect(MotifGraveBeSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });
});
