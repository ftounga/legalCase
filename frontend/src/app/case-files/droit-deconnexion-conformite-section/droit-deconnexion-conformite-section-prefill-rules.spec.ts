import { DroitDeconnexionConformitePrefillRules } from './droit-deconnexion-conformite-section-prefill-rules';

describe('DroitDeconnexionConformitePrefillRules', () => {
  it('computeAccordOuChartePresent : booléen (FR) sinon null', () => {
    expect(DroitDeconnexionConformitePrefillRules.computeAccordOuChartePresent({
      aiData: { accord_deconnexion_present: true } as any, workspaceCountry: 'FRANCE',
    })).toBe(true);
    expect(DroitDeconnexionConformitePrefillRules.computeAccordOuChartePresent({
      aiData: { accord_deconnexion_present: false } as any, workspaceCountry: 'FRANCE',
    })).toBe(false);
    expect(DroitDeconnexionConformitePrefillRules.computeAccordOuChartePresent({
      aiData: { accord_deconnexion_present: 'oui' } as any, workspaceCountry: 'FRANCE',
    })).toBeNull();
    expect(DroitDeconnexionConformitePrefillRules.computeAccordOuChartePresent({})).toBeNull();
  });

  it('computePrefillCount : 0 (vide) / 1 (nominal)', () => {
    expect(DroitDeconnexionConformitePrefillRules.computePrefillCount({})).toBe(0);
    expect(DroitDeconnexionConformitePrefillRules.computePrefillCount({
      aiData: { accord_deconnexion_present: true } as any, workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('computePrefillCount compte le booléen false (présence d\'information)', () => {
    expect(DroitDeconnexionConformitePrefillRules.computePrefillCount({
      aiData: { accord_deconnexion_present: false } as any, workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('computePrefillCount = 0 hors FRANCE même avec données', () => {
    expect(DroitDeconnexionConformitePrefillRules.computePrefillCount({
      aiData: { accord_deconnexion_present: true } as any, workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });
});
