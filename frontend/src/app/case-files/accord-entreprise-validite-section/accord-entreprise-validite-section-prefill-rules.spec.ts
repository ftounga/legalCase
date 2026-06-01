import { AccordEntrepriseValiditePrefillRules as Rules } from './accord-entreprise-validite-section-prefill-rules';

describe('AccordEntrepriseValiditePrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { accordPourcentageSignataires: 55, accordTypeOperation: 'CONCLUSION' },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePourcentageSignataires(input)).toBeNull();
    expect(Rules.computeTypeOperation(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 (partiel) when only pourcentage present', () => {
    const input = { aiData: { accordPourcentageSignataires: 55 } };
    expect(Rules.computePourcentageSignataires(input)).toBe(55);
    expect(Rules.computeTypeOperation(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 1 (partiel) when only typeOperation present', () => {
    const input = { aiData: { accordTypeOperation: 'REVISION' } };
    expect(Rules.computeTypeOperation(input)).toBe('REVISION');
    expect(Rules.computePourcentageSignataires(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 2 (nominal) when both fields present', () => {
    const input = {
      aiData: { accordPourcentageSignataires: 35, accordTypeOperation: 'DENONCIATION' },
    };
    expect(Rules.computePourcentageSignataires(input)).toBe(35);
    expect(Rules.computeTypeOperation(input)).toBe('DENONCIATION');
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('accepts pourcentage 0 and 100, rejects out-of-range / non-numeric', () => {
    expect(Rules.computePourcentageSignataires({ aiData: { accordPourcentageSignataires: 0 } })).toBe(0);
    expect(Rules.computePourcentageSignataires({ aiData: { accordPourcentageSignataires: 100 } })).toBe(100);
    expect(Rules.computePourcentageSignataires({ aiData: { accordPourcentageSignataires: 120 } })).toBeNull();
    expect(Rules.computePourcentageSignataires({ aiData: { accordPourcentageSignataires: -5 } })).toBeNull();
    expect(Rules.computePourcentageSignataires({ aiData: { accordPourcentageSignataires: '55' as unknown as number } })).toBeNull();
    expect(Rules.computePourcentageSignataires({ aiData: { accordPourcentageSignataires: null } })).toBeNull();
  });

  it('rejects an unknown typeOperation value', () => {
    expect(Rules.computeTypeOperation({ aiData: { accordTypeOperation: 'AUTRE' } })).toBeNull();
    expect(Rules.computeTypeOperation({ aiData: { accordTypeOperation: '' } })).toBeNull();
    expect(Rules.computeTypeOperation({ aiData: { accordTypeOperation: null } })).toBeNull();
  });

  it('does NOT count accordEntrepriseDetecte flag alone (visibility trigger, not a form field)', () => {
    const input = { aiData: { accordEntrepriseDetecte: true } };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    const input = {
      aiData: { accordPourcentageSignataires: 55, accordTypeOperation: 'CONCLUSION' },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });
});
