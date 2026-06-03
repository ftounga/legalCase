import { PpvExonerationPrefillRules } from './ppv-exoneration-section-prefill-rules';

describe('PpvExonerationPrefillRules', () => {
  it('computeMontantPrime : nombre > 0 (FR) sinon null', () => {
    expect(PpvExonerationPrefillRules.computeMontantPrime({
      aiData: { montant_ppv: 2500 } as any, workspaceCountry: 'FRANCE',
    })).toBe(2500);
    expect(PpvExonerationPrefillRules.computeMontantPrime({
      aiData: { montant_ppv: 0 } as any, workspaceCountry: 'FRANCE',
    })).toBeNull();
    expect(PpvExonerationPrefillRules.computeMontantPrime({
      aiData: { montant_ppv: 2500 } as any, workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('computeAccordInteressementPresent : booléen (FR) sinon null', () => {
    expect(PpvExonerationPrefillRules.computeAccordInteressementPresent({
      aiData: { accord_interessement_present: true } as any, workspaceCountry: 'FRANCE',
    })).toBe(true);
    expect(PpvExonerationPrefillRules.computeAccordInteressementPresent({
      aiData: { accord_interessement_present: false } as any, workspaceCountry: 'FRANCE',
    })).toBe(false);
    expect(PpvExonerationPrefillRules.computeAccordInteressementPresent({
      aiData: { accord_interessement_present: 'oui' } as any, workspaceCountry: 'FRANCE',
    })).toBeNull();
    expect(PpvExonerationPrefillRules.computeAccordInteressementPresent({})).toBeNull();
  });

  it('computePrefillCount : 0 / partiel / nominal', () => {
    expect(PpvExonerationPrefillRules.computePrefillCount({})).toBe(0);
    expect(PpvExonerationPrefillRules.computePrefillCount({
      aiData: { montant_ppv: 2500 } as any, workspaceCountry: 'FRANCE',
    })).toBe(1);
    expect(PpvExonerationPrefillRules.computePrefillCount({
      aiData: { montant_ppv: 2500, accord_interessement_present: true } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(2);
  });

  it('computePrefillCount compte le booléen false (présence d\'information)', () => {
    expect(PpvExonerationPrefillRules.computePrefillCount({
      aiData: { accord_interessement_present: false } as any, workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('computePrefillCount = 0 hors FRANCE même avec données', () => {
    expect(PpvExonerationPrefillRules.computePrefillCount({
      aiData: { montant_ppv: 2500, accord_interessement_present: true } as any,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });
});
