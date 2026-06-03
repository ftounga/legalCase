import { CceSuspensionBePrefillRules as Rules } from './cce-suspension-be-section-prefill-rules';

describe('CceSuspensionBePrefillRules', () => {
  it('returns 0 when no aiData (gate BE manquant)', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is FRANCE (mono-pays BE)', () => {
    const input = {
      aiData: {
        cceSuspensionDateNotification: '2026-05-30',
        cceSuspensionUrgence: true,
        cceSuspensionPrejudiceGrave: true,
      },
      workspaceCountry: 'FRANCE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
    expect(Rules.computeDateNotification(input)).toBeNull();
    expect(Rules.computeUrgence(input)).toBeNull();
    expect(Rules.computePrejudiceGrave(input)).toBeNull();
  });

  it('returns 1 when only date present (BELGIQUE)', () => {
    const input = {
      aiData: { cceSuspensionDateNotification: '2026-05-30' },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computeDateNotification(input)).toBe('2026-05-30');
    expect(Rules.computeUrgence(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 3 (nominal) when all real fields present (BELGIQUE)', () => {
    const input = {
      aiData: {
        cceSuspensionDateNotification: '2026-05-30',
        cceSuspensionUrgence: true,
        cceSuspensionPrejudiceGrave: false,
      },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computeDateNotification(input)).toBe('2026-05-30');
    expect(Rules.computeUrgence(input)).toBe(true);
    expect(Rules.computePrejudiceGrave(input)).toBe(false);
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('counts a false boolean as a real pré-fill (tri-state)', () => {
    const input = {
      aiData: { cceSuspensionUrgence: false, cceSuspensionPrejudiceGrave: false },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computeUrgence(input)).toBe(false);
    expect(Rules.computePrejudiceGrave(input)).toBe(false);
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('does NOT count a non-boolean value for urgence/prejudice', () => {
    const input = {
      aiData: {
        cceSuspensionUrgence: 'oui' as unknown as boolean,
        cceSuspensionPrejudiceGrave: 1 as unknown as boolean,
      },
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computeUrgence(input)).toBeNull();
    expect(Rules.computePrejudiceGrave(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('rejects malformed / invalid dates', () => {
    expect(Rules.computeDateNotification({
      aiData: { cceSuspensionDateNotification: '30/05/2026' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDateNotification({
      aiData: { cceSuspensionDateNotification: '2026-02-30' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('does NOT count aspirational procedural fields', () => {
    const input = {
      aiData: {
        cceSuspensionDateNotification: '2026-05-30',
        cceSuspensionUrgence: true,
        cceSuspensionPrejudiceGrave: true,
        // simulate AI accidentally returning aspirational fields — must be ignored:
        recoursAnnulationIntroduit: true,
        mesureEloignementImminente: true,
      } as unknown as Record<string, unknown>,
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(3);
  });

  it('does NOT count other Immigration BE tool fields (détention centre fermé)', () => {
    const input = {
      aiData: {
        detentionDateDebut: '2026-05-30',
        detentionBaseLegale: 'ART_7',
      } as unknown as Record<string, unknown>,
      workspaceCountry: 'BELGIQUE' as const,
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });
});
