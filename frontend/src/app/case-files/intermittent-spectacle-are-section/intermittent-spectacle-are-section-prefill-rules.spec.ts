import { IntermittentSpectacleArePrefillRules as Rules } from './intermittent-spectacle-are-section-prefill-rules';

describe('IntermittentSpectacleArePrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: {
        dateLicenciement: '2024-03-31',
        intermittentAnnexe: 'ANNEXE_8_TECHNICIENS' as const,
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateFinContrat(input)).toBeNull();
    expect(Rules.computeAnnexe(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 (partiel) when only dateFinContrat present', () => {
    const input = { aiData: { dateLicenciement: '2024-03-31' } };
    expect(Rules.computeDateFinContrat(input)).toBe('2024-03-31');
    expect(Rules.computeAnnexe(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('maps annexe from aiData.intermittentAnnexe', () => {
    const input = { aiData: { intermittentAnnexe: 'ANNEXE_10_ARTISTES' as const } };
    expect(Rules.computeAnnexe(input)).toBe('ANNEXE_10_ARTISTES');
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 2 (nominal) when both fields present', () => {
    const input = {
      aiData: {
        dateLicenciement: '2024-03-31',
        intermittentAnnexe: 'ANNEXE_8_TECHNICIENS' as const,
      },
    };
    expect(Rules.computeDateFinContrat(input)).toBe('2024-03-31');
    expect(Rules.computeAnnexe(input)).toBe('ANNEXE_8_TECHNICIENS');
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('rejects an unknown annexe value', () => {
    expect(Rules.computeAnnexe({ aiData: { intermittentAnnexe: 'ANNEXE_99' as unknown as 'ANNEXE_8_TECHNICIENS' } })).toBeNull();
    expect(Rules.computeAnnexe({ aiData: { intermittentAnnexe: 8 as unknown as 'ANNEXE_8_TECHNICIENS' } })).toBeNull();
    expect(Rules.computeAnnexe({ aiData: { intermittentAnnexe: null } })).toBeNull();
  });

  it('rejects malformed / non-string dateFinContrat', () => {
    expect(Rules.computeDateFinContrat({ aiData: { dateLicenciement: '31/03/2024' } })).toBeNull();
    expect(Rules.computeDateFinContrat({ aiData: { dateLicenciement: '2024-3-1' } })).toBeNull();
    expect(Rules.computeDateFinContrat({ aiData: { dateLicenciement: 20240331 as unknown as string } })).toBeNull();
    expect(Rules.computeDateFinContrat({ aiData: { dateLicenciement: '' } })).toBeNull();
  });

  it('does NOT count statutIntermittentDetecte flag alone (visibility trigger, not a form field)', () => {
    const input = { aiData: { statutIntermittentDetecte: true } };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    const input = {
      aiData: { dateLicenciement: '2024-03-31', intermittentAnnexe: 'ANNEXE_10_ARTISTES' as const },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });
});
