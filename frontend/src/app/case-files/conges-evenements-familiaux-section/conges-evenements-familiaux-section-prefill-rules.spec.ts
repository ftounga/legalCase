import { CongesEvenementsFamiliauxPrefillRules } from './conges-evenements-familiaux-section-prefill-rules';

describe('CongesEvenementsFamiliauxPrefillRules', () => {

  it('computeTypeEvenement normalise les valeurs directes de l\'enum', () => {
    expect(CongesEvenementsFamiliauxPrefillRules.computeTypeEvenement({
      aiData: { type_evenement_familial: 'MARIAGE_PACS' } as any,
      workspaceCountry: 'FRANCE',
    })).toBe('MARIAGE_PACS');
  });

  it('computeTypeEvenement mappe les libellés intermédiaires IA', () => {
    const cases: ReadonlyArray<[string, string]> = [
      ['MARIAGE', 'MARIAGE_PACS'],
      ['PACS', 'MARIAGE_PACS'],
      ['adoption', 'NAISSANCE'],
      ['DECES_CONJOINT', 'DECES_CONJOINT_PARTENAIRE'],
      ['deces partenaire', 'DECES_CONJOINT_PARTENAIRE'],
      ['DECES_PARENT', 'DECES_PERE_MERE'],
      ['deces-pere', 'DECES_PERE_MERE'],
      ['ANNONCE_HANDICAP', 'ANNONCE_HANDICAP_ENFANT'],
      ['demenagement', 'DEMENAGEMENT_NON_LEGAL'],
    ];
    for (const [raw, expected] of cases) {
      expect(CongesEvenementsFamiliauxPrefillRules.computeTypeEvenement({
        aiData: { type_evenement_familial: raw } as any,
        workspaceCountry: 'FRANCE',
      })).toBe(expected);
    }
  });

  it('computeTypeEvenement retourne null si non reconnu / absent', () => {
    expect(CongesEvenementsFamiliauxPrefillRules.computeTypeEvenement({
      aiData: { type_evenement_familial: 'XYZ_INCONNU' } as any,
      workspaceCountry: 'FRANCE',
    })).toBeNull();
    expect(CongesEvenementsFamiliauxPrefillRules.computeTypeEvenement({
      aiData: {} as any,
      workspaceCountry: 'FRANCE',
    })).toBeNull();
    expect(CongesEvenementsFamiliauxPrefillRules.computeTypeEvenement({})).toBeNull();
  });

  it('computeTypeEvenement retourne null hors France', () => {
    expect(CongesEvenementsFamiliauxPrefillRules.computeTypeEvenement({
      aiData: { type_evenement_familial: 'MARIAGE' } as any,
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('computePrefillCount = 1 si type reconnu, 0 sinon', () => {
    expect(CongesEvenementsFamiliauxPrefillRules.computePrefillCount({
      aiData: { type_evenement_familial: 'NAISSANCE' } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(1);
    expect(CongesEvenementsFamiliauxPrefillRules.computePrefillCount({})).toBe(0);
    expect(CongesEvenementsFamiliauxPrefillRules.computePrefillCount({
      aiData: { type_evenement_familial: 'MARIAGE' } as any,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });
});
