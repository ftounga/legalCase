import { DetentionCentreFermeBePrefillRules as Rules } from './detention-centre-ferme-be-section-prefill-rules';

describe('DetentionCentreFermeBePrefillRules', () => {
  it('returns 0 when no aiData (gate BE manquant)', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is FRANCE (mono-pays BE)', () => {
    const input = {
      aiData: {
        detentionDateDebut: '2026-05-30',
        detentionBaseLegale: 'ART_7',
        detentionDateNotification: '2026-06-01',
      },
      workspaceCountry: 'FRANCE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeDateDebut(input)).toBeNull();
    expect(Rules.computeBaseLegale(input)).toBeNull();
    expect(Rules.computeDateNotification(input)).toBeNull();
  });

  it('returns 1 when only date present (BELGIQUE)', () => {
    const input = {
      aiData: { detentionDateDebut: '2026-05-30' },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computeDateDebut(input)).toBe('2026-05-30');
    expect(Rules.computeBaseLegale(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 3 (nominal) when all real fields present (BELGIQUE)', () => {
    const input = {
      aiData: {
        detentionDateDebut: '2026-05-30',
        detentionBaseLegale: 'ART_74_5',
        detentionDateNotification: '2026-06-01',
      },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computeDateDebut(input)).toBe('2026-05-30');
    expect(Rules.computeBaseLegale(input)).toBe('ART_74_5');
    expect(Rules.computeDateNotification(input)).toBe('2026-06-01');
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('accepts each whitelisted base legale value', () => {
    for (const base of ['ART_7', 'ART_27', 'ART_29', 'ART_74_5', 'AUTRE']) {
      expect(Rules.computeBaseLegale({
        aiData: { detentionBaseLegale: base },
        workspaceCountry: 'BELGIQUE',
      })).toBe(base);
    }
  });

  it('rejects an out-of-whitelist base legale value', () => {
    expect(Rules.computeBaseLegale({
      aiData: { detentionBaseLegale: 'ART_999' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('rejects malformed / invalid dates', () => {
    expect(Rules.computeDateDebut({
      aiData: { detentionDateDebut: '30/05/2026' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDateNotification({
      aiData: { detentionDateNotification: '2026-02-30' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDateDebut({
      aiData: { detentionDateDebut: 12345 as unknown as string },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('does NOT count aspirational procedural fields', () => {
    const input = {
      aiData: {
        detentionDateDebut: '2026-05-30',
        detentionBaseLegale: 'ART_7',
        detentionDateNotification: '2026-06-01',
        // simulate AI accidentally returning procedural action fields — must be ignored:
        prolongationNotifiee: true,
        requeteMiseEnLiberteDeposee: true,
      } as unknown as Record<string, unknown>,
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('does NOT count other Immigration BE tool fields (résident longue durée UE)', () => {
    const input = {
      aiData: {
        rlueDateDebutSejour: '2020-01-01',
        rlueRessourcesSuffisantes: true,
      } as unknown as Record<string, unknown>,
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });
});
