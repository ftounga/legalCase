import {
  AvantagesConventionnelsBeSectionPrefillRules,
  computePrefillCount,
  computeSalaireMensuelBrutEur,
  computeCommissionParitaire,
  computeJoursTravailles,
  computeJoursPrestes,
} from './avantages-conventionnels-be-section-prefill-rules';

describe('AvantagesConventionnelsBeSectionPrefillRules', () => {
  const BE = 'BELGIQUE';

  describe('computePrefillCount', () => {
    it('cas 0 — input vide retourne 0', () => {
      expect(computePrefillCount({ workspaceCountry: BE })).toBe(0);
      expect(computePrefillCount({ workspaceCountry: BE, aiData: null })).toBe(0);
      expect(computePrefillCount({ workspaceCountry: BE, aiData: {} })).toBe(0);
    });

    it('cas M — salaire <= 0 retourne 0', () => {
      expect(
        computePrefillCount({
          workspaceCountry: BE,
          aiData: { salaireBrutMensuel: 0 },
        }),
      ).toBe(0);
      expect(
        computePrefillCount({
          workspaceCountry: BE,
          aiData: { salaireBrutMensuel: -100 },
        }),
      ).toBe(0);
    });

    it('cas N — salaire valide retourne 1', () => {
      expect(
        computePrefillCount({
          workspaceCountry: BE,
          aiData: { salaireBrutMensuel: 2500 },
        }),
      ).toBe(1);
    });

    it('SF-246-23 — tous les 4 champs renseignés retourne 4', () => {
      expect(
        computePrefillCount({
          workspaceCountry: BE,
          aiData: {
            salaireBrutMensuel: 2500,
            commissionParitaireBe: 'CP 200',
            joursTravaillesAnneePrecedenteBe: 220,
            joursPrestesBe: 45,
          },
        }),
      ).toBe(4);
    });
  });

  it('gating BE : workspaceCountry FRANCE retourne 0 même avec aiData complet', () => {
    expect(
      computePrefillCount({
        workspaceCountry: 'FRANCE',
        aiData: { salaireBrutMensuel: 2500 },
      }),
    ).toBe(0);
    expect(
      computeSalaireMensuelBrutEur({
        workspaceCountry: 'FRANCE',
        aiData: { salaireBrutMensuel: 2500 },
      }),
    ).toBeNull();
  });

  it('gating BE : workspaceCountry absent retourne 0', () => {
    expect(
      computePrefillCount({
        aiData: { salaireBrutMensuel: 2500 },
      }),
    ).toBe(0);
  });

  it('expose barrel', () => {
    expect(AvantagesConventionnelsBeSectionPrefillRules.computePrefillCount).toBe(computePrefillCount);
  });

  // SF-246-23 — computeCommissionParitaire

  describe('computeCommissionParitaire (SF-246-23)', () => {
    it('normalise "CP 200" → CP_200', () => {
      expect(
        computeCommissionParitaire({ workspaceCountry: BE, aiData: { commissionParitaireBe: 'CP 200' } }),
      ).toBe('CP_200');
    });

    it('normalise "SCP 200.01" → CP_200', () => {
      expect(
        computeCommissionParitaire({ workspaceCountry: BE, aiData: { commissionParitaireBe: 'SCP 200.01' } }),
      ).toBe('CP_200');
    });

    it('normalise "CP 226" → CP_226', () => {
      expect(
        computeCommissionParitaire({ workspaceCountry: BE, aiData: { commissionParitaireBe: 'CP 226' } }),
      ).toBe('CP_226');
    });

    it('normalise "CP 124" → CP_124', () => {
      expect(
        computeCommissionParitaire({ workspaceCountry: BE, aiData: { commissionParitaireBe: 'CP 124' } }),
      ).toBe('CP_124');
    });

    it('normalise "CP 111" → CP_111', () => {
      expect(
        computeCommissionParitaire({ workspaceCountry: BE, aiData: { commissionParitaireBe: 'CP 111' } }),
      ).toBe('CP_111');
    });

    it('normalise "CP 337" → CP_337', () => {
      expect(
        computeCommissionParitaire({ workspaceCountry: BE, aiData: { commissionParitaireBe: 'CP 337' } }),
      ).toBe('CP_337');
    });

    it('retourne null si code non reconnu', () => {
      expect(
        computeCommissionParitaire({ workspaceCountry: BE, aiData: { commissionParitaireBe: 'CP 999' } }),
      ).toBeNull();
    });

    it('retourne null si string vide', () => {
      expect(
        computeCommissionParitaire({ workspaceCountry: BE, aiData: { commissionParitaireBe: '' } }),
      ).toBeNull();
    });

    it('retourne null si pays ≠ BELGIQUE', () => {
      expect(
        computeCommissionParitaire({ workspaceCountry: 'FRANCE', aiData: { commissionParitaireBe: 'CP 200' } }),
      ).toBeNull();
    });
  });

  // SF-246-23 — computeJoursTravailles

  describe('computeJoursTravailles (SF-246-23)', () => {
    it('retourne 220 pour 220 jours', () => {
      expect(
        computeJoursTravailles({ workspaceCountry: BE, aiData: { joursTravaillesAnneePrecedenteBe: 220 } }),
      ).toBe(220);
    });

    it('accepte 0 (borne inférieure)', () => {
      expect(
        computeJoursTravailles({ workspaceCountry: BE, aiData: { joursTravaillesAnneePrecedenteBe: 0 } }),
      ).toBe(0);
    });

    it('accepte 365 (borne supérieure)', () => {
      expect(
        computeJoursTravailles({ workspaceCountry: BE, aiData: { joursTravaillesAnneePrecedenteBe: 365 } }),
      ).toBe(365);
    });

    it('rejette 366 → null', () => {
      expect(
        computeJoursTravailles({ workspaceCountry: BE, aiData: { joursTravaillesAnneePrecedenteBe: 366 } }),
      ).toBeNull();
    });

    it('rejette -1 → null', () => {
      expect(
        computeJoursTravailles({ workspaceCountry: BE, aiData: { joursTravaillesAnneePrecedenteBe: -1 } }),
      ).toBeNull();
    });

    it('retourne null si pays ≠ BELGIQUE', () => {
      expect(
        computeJoursTravailles({ workspaceCountry: 'FRANCE', aiData: { joursTravaillesAnneePrecedenteBe: 220 } }),
      ).toBeNull();
    });
  });

  // SF-246-23 — computeJoursPrestes

  describe('computeJoursPrestes (SF-246-23)', () => {
    it('retourne 45 pour 45 jours', () => {
      expect(
        computeJoursPrestes({ workspaceCountry: BE, aiData: { joursPrestesBe: 45 } }),
      ).toBe(45);
    });

    it('rejette 400 → null', () => {
      expect(
        computeJoursPrestes({ workspaceCountry: BE, aiData: { joursPrestesBe: 400 } }),
      ).toBeNull();
    });

    it('retourne null si pays ≠ BELGIQUE', () => {
      expect(
        computeJoursPrestes({ workspaceCountry: 'FRANCE', aiData: { joursPrestesBe: 45 } }),
      ).toBeNull();
    });
  });
});
