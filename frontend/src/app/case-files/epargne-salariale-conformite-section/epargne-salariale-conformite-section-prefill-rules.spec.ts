import { EpargneSalarialeConformitePrefillRules } from './epargne-salariale-conformite-section-prefill-rules';

describe('EpargneSalarialeConformitePrefillRules', () => {
  it('computeAccordParticipationPresent : booléen (FR) sinon null', () => {
    expect(EpargneSalarialeConformitePrefillRules.computeAccordParticipationPresent({
      aiData: { accord_participation_present: true } as any, workspaceCountry: 'FRANCE',
    })).toBe(true);
    expect(EpargneSalarialeConformitePrefillRules.computeAccordParticipationPresent({
      aiData: { accord_participation_present: false } as any, workspaceCountry: 'FRANCE',
    })).toBe(false);
    expect(EpargneSalarialeConformitePrefillRules.computeAccordParticipationPresent({
      aiData: { accord_participation_present: 'oui' } as any, workspaceCountry: 'FRANCE',
    })).toBeNull();
    expect(EpargneSalarialeConformitePrefillRules.computeAccordParticipationPresent({})).toBeNull();
  });

  it('computeAccordInteressementPresent : booléen (FR) sinon null', () => {
    expect(EpargneSalarialeConformitePrefillRules.computeAccordInteressementPresent({
      aiData: { accord_interessement_present: true } as any, workspaceCountry: 'FRANCE',
    })).toBe(true);
    expect(EpargneSalarialeConformitePrefillRules.computeAccordInteressementPresent({
      aiData: { accord_interessement_present: false } as any, workspaceCountry: 'FRANCE',
    })).toBe(false);
    expect(EpargneSalarialeConformitePrefillRules.computeAccordInteressementPresent({})).toBeNull();
  });

  it('computePrefillCount : 0 / partiel / nominal', () => {
    expect(EpargneSalarialeConformitePrefillRules.computePrefillCount({})).toBe(0);
    expect(EpargneSalarialeConformitePrefillRules.computePrefillCount({
      aiData: { accord_participation_present: true } as any, workspaceCountry: 'FRANCE',
    })).toBe(1);
    expect(EpargneSalarialeConformitePrefillRules.computePrefillCount({
      aiData: { accord_participation_present: true, accord_interessement_present: false } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(2);
  });

  it('computePrefillCount compte le booléen false (présence d\'information)', () => {
    expect(EpargneSalarialeConformitePrefillRules.computePrefillCount({
      aiData: { accord_participation_present: false } as any, workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('computePrefillCount = 0 hors FRANCE même avec données', () => {
    expect(EpargneSalarialeConformitePrefillRules.computePrefillCount({
      aiData: { accord_participation_present: true, accord_interessement_present: true } as any,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });
});
