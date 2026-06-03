import { RttAcquisitionPrefillRules } from './rtt-acquisition-section-prefill-rules';

describe('RttAcquisitionPrefillRules', () => {

  it('computeHoraireHebdomadaireCollectif lit un horaire cohérent depuis Sf218dDetail', () => {
    expect(RttAcquisitionPrefillRules.computeHoraireHebdomadaireCollectif({
      aiData: { horaire_hebdomadaire_collectif: 37 } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(37);
  });

  it('computeHoraireHebdomadaireCollectif accepte une chaîne numérique', () => {
    expect(RttAcquisitionPrefillRules.computeHoraireHebdomadaireCollectif({
      aiData: { horaire_hebdomadaire_collectif: '39' } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(39);
  });

  it('computeHoraireHebdomadaireCollectif ignore les valeurs hors borne (≤ 35 ou > 48)', () => {
    expect(RttAcquisitionPrefillRules.computeHoraireHebdomadaireCollectif({
      aiData: { horaire_hebdomadaire_collectif: 35 } as any,
      workspaceCountry: 'FRANCE',
    })).toBeNull();
    expect(RttAcquisitionPrefillRules.computeHoraireHebdomadaireCollectif({
      aiData: { horaire_hebdomadaire_collectif: 50 } as any,
      workspaceCountry: 'FRANCE',
    })).toBeNull();
  });

  it('computeHoraireHebdomadaireCollectif retourne null si absent / non numérique', () => {
    expect(RttAcquisitionPrefillRules.computeHoraireHebdomadaireCollectif({
      aiData: { horaire_hebdomadaire_collectif: 'abc' } as any,
      workspaceCountry: 'FRANCE',
    })).toBeNull();
    expect(RttAcquisitionPrefillRules.computeHoraireHebdomadaireCollectif({
      aiData: {} as any,
      workspaceCountry: 'FRANCE',
    })).toBeNull();
    expect(RttAcquisitionPrefillRules.computeHoraireHebdomadaireCollectif({})).toBeNull();
  });

  it('computeHoraireHebdomadaireCollectif retourne null hors France', () => {
    expect(RttAcquisitionPrefillRules.computeHoraireHebdomadaireCollectif({
      aiData: { horaire_hebdomadaire_collectif: 37 } as any,
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('computePrefillCount = 1 si horaire cohérent, 0 sinon', () => {
    expect(RttAcquisitionPrefillRules.computePrefillCount({
      aiData: { horaire_hebdomadaire_collectif: 38 } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(1);
    expect(RttAcquisitionPrefillRules.computePrefillCount({})).toBe(0);
    expect(RttAcquisitionPrefillRules.computePrefillCount({
      aiData: { horaire_hebdomadaire_collectif: 37 } as any,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });
});
