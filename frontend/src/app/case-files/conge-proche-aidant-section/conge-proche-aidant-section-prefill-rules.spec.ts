import { CongeProcheAidantPrefillRules } from './conge-proche-aidant-section-prefill-rules';

describe('CongeProcheAidantPrefillRules', () => {

  it('computeLienPersonneAidee lit un lien connu depuis Sf218dDetail', () => {
    expect(CongeProcheAidantPrefillRules.computeLienPersonneAidee({
      aiData: { lien_personne_aidee: 'ASCENDANT' } as any,
      workspaceCountry: 'FRANCE',
    })).toBe('ASCENDANT');
  });

  it('computeLienPersonneAidee normalise la casse', () => {
    expect(CongeProcheAidantPrefillRules.computeLienPersonneAidee({
      aiData: { lien_personne_aidee: 'conjoint' } as any,
      workspaceCountry: 'FRANCE',
    })).toBe('CONJOINT');
  });

  it('computeLienPersonneAidee retourne null si valeur inconnue / absente', () => {
    expect(CongeProcheAidantPrefillRules.computeLienPersonneAidee({
      aiData: { lien_personne_aidee: 'PAS_UN_LIEN' } as any,
      workspaceCountry: 'FRANCE',
    })).toBeNull();
    expect(CongeProcheAidantPrefillRules.computeLienPersonneAidee({
      aiData: {} as any,
      workspaceCountry: 'FRANCE',
    })).toBeNull();
    expect(CongeProcheAidantPrefillRules.computeLienPersonneAidee({})).toBeNull();
  });

  it('computeLienPersonneAidee retourne null hors France', () => {
    expect(CongeProcheAidantPrefillRules.computeLienPersonneAidee({
      aiData: { lien_personne_aidee: 'ASCENDANT' } as any,
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('computePrefillCount = 1 si lien reconnu, 0 sinon', () => {
    expect(CongeProcheAidantPrefillRules.computePrefillCount({
      aiData: { lien_personne_aidee: 'DESCENDANT' } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(1);
    expect(CongeProcheAidantPrefillRules.computePrefillCount({})).toBe(0);
    expect(CongeProcheAidantPrefillRules.computePrefillCount({
      aiData: { lien_personne_aidee: 'ASCENDANT' } as any,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });
});
