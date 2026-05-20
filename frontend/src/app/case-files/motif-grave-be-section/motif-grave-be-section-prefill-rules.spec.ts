import {
  MotifGraveBeSectionPrefillRules,
  computePrefillCount,
  computeDateNotificationRupture,
  computeSalaireMensuelReference,
  computeDateConnaissanceFait,
  computeDateNotificationMotifs,
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

    it('SF-246-23 — tous les 4 champs renseignés retourne 4', () => {
      expect(
        computePrefillCount({
          workspaceCountry: BE,
          aiData: {
            dateLicenciement: '2024-05-10',
            salaireBrutMensuel: 3000,
            dateConnaissanceFait: '2024-05-08',
            dateNotificationMotifs: '2024-05-13',
          },
        }),
      ).toBe(4);
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

  // SF-246-23 — computeDateConnaissanceFait

  describe('computeDateConnaissanceFait (SF-246-23)', () => {
    it('retourne la date si string non vide', () => {
      expect(
        computeDateConnaissanceFait({
          workspaceCountry: BE,
          aiData: { dateConnaissanceFait: '2024-05-08' },
        }),
      ).toBe('2024-05-08');
    });

    it('retourne null si string vide', () => {
      expect(
        computeDateConnaissanceFait({
          workspaceCountry: BE,
          aiData: { dateConnaissanceFait: '' },
        }),
      ).toBeNull();
    });

    it('retourne null si absente', () => {
      expect(
        computeDateConnaissanceFait({ workspaceCountry: BE, aiData: {} }),
      ).toBeNull();
    });

    it('retourne null si pays ≠ BELGIQUE', () => {
      expect(
        computeDateConnaissanceFait({
          workspaceCountry: 'FRANCE',
          aiData: { dateConnaissanceFait: '2024-05-08' },
        }),
      ).toBeNull();
    });
  });

  // SF-246-23 — computeDateNotificationMotifs

  describe('computeDateNotificationMotifs (SF-246-23)', () => {
    it('retourne la date si string non vide', () => {
      expect(
        computeDateNotificationMotifs({
          workspaceCountry: BE,
          aiData: { dateNotificationMotifs: '2024-05-13' },
        }),
      ).toBe('2024-05-13');
    });

    it('retourne null si absente', () => {
      expect(
        computeDateNotificationMotifs({ workspaceCountry: BE, aiData: {} }),
      ).toBeNull();
    });

    it('retourne null si pays ≠ BELGIQUE', () => {
      expect(
        computeDateNotificationMotifs({
          workspaceCountry: 'FRANCE',
          aiData: { dateNotificationMotifs: '2024-05-13' },
        }),
      ).toBeNull();
    });
  });
});
