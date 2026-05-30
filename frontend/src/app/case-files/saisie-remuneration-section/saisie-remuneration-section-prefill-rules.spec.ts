import { SaisieRemunerationPrefillRules as Rules } from './saisie-remuneration-section-prefill-rules';

describe('SaisieRemunerationPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { salaireBrutMensuel: 2000, nombrePersonnesACharge: 2 },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeRemunerationNetteMensuelle(input)).toBeNull();
    expect(Rules.computeNombrePersonnesACharge(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 (partiel) when only salaireBrutMensuel present', () => {
    const input = { aiData: { salaireBrutMensuel: 1800 } };
    expect(Rules.computeRemunerationNetteMensuelle(input)).toBe(1800);
    expect(Rules.computeNombrePersonnesACharge(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 1 (partiel) when only nombrePersonnesACharge present', () => {
    const input = { aiData: { nombrePersonnesACharge: 3 } };
    expect(Rules.computeNombrePersonnesACharge(input)).toBe(3);
    expect(Rules.computeRemunerationNetteMensuelle(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 2 (nominal) when both fields present', () => {
    const input = { aiData: { salaireBrutMensuel: 2400, nombrePersonnesACharge: 1 } };
    expect(Rules.computeRemunerationNetteMensuelle(input)).toBe(2400);
    expect(Rules.computeNombrePersonnesACharge(input)).toBe(1);
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('counts nombrePersonnesACharge = 0 (valid zero is factualisable)', () => {
    const input = { aiData: { salaireBrutMensuel: 2000, nombrePersonnesACharge: 0 } };
    expect(Rules.computeNombrePersonnesACharge(input)).toBe(0);
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('rejects non-positive / non-finite / non-number salaire', () => {
    expect(Rules.computeRemunerationNetteMensuelle({ aiData: { salaireBrutMensuel: 0 } })).toBeNull();
    expect(Rules.computeRemunerationNetteMensuelle({ aiData: { salaireBrutMensuel: -100 } })).toBeNull();
    expect(Rules.computeRemunerationNetteMensuelle({ aiData: { salaireBrutMensuel: NaN } })).toBeNull();
    expect(Rules.computeRemunerationNetteMensuelle({ aiData: { salaireBrutMensuel: '2000' as unknown as number } })).toBeNull();
    expect(Rules.computeRemunerationNetteMensuelle({ aiData: { salaireBrutMensuel: null } })).toBeNull();
  });

  it('rejects negative / non-integer / non-number nombrePersonnesACharge', () => {
    expect(Rules.computeNombrePersonnesACharge({ aiData: { nombrePersonnesACharge: -1 } })).toBeNull();
    expect(Rules.computeNombrePersonnesACharge({ aiData: { nombrePersonnesACharge: 1.5 } })).toBeNull();
    expect(Rules.computeNombrePersonnesACharge({ aiData: { nombrePersonnesACharge: '2' as unknown as number } })).toBeNull();
    expect(Rules.computeNombrePersonnesACharge({ aiData: { nombrePersonnesACharge: null } })).toBeNull();
  });

  it('does NOT count saisieRemunerationDetectee flag alone (visibility trigger, not a form field)', () => {
    const input = { aiData: { saisieRemunerationDetectee: true } };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    const input = { aiData: { salaireBrutMensuel: 2100, nombrePersonnesACharge: 2 } };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });
});
