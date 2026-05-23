import { ForfaitJoursFrSectionPrefillRules } from './forfait-jours-fr-section-prefill-rules';

describe('ForfaitJoursFrSectionPrefillRules', () => {
  const aiFull = {
    forfaitJoursAccordCollectifExiste: true,
    forfaitJoursEntretienAnnuelRealise: false,
    forfaitJoursDocumentControle: true,
    forfaitJoursCategorieAutonome: false,
    forfaitJoursNbJours: 218,
  };

  describe('gate FR', () => {
    it('retourne null hors France', () => {
      const input = { aiData: aiFull, workspaceCountry: 'BELGIQUE' };
      expect(ForfaitJoursFrSectionPrefillRules.computeAccordCollectifExiste(input)).toBeNull();
      expect(ForfaitJoursFrSectionPrefillRules.computeNbJoursForfait(input)).toBeNull();
    });

    it('retourne null si aiData absent', () => {
      const input = { aiData: null, workspaceCountry: 'FRANCE' };
      expect(ForfaitJoursFrSectionPrefillRules.computeEntretienAnnuelRealise(input)).toBeNull();
    });
  });

  describe('computeAccordCollectifExiste', () => {
    it('retourne true depuis IA', () => {
      const input = { aiData: aiFull, workspaceCountry: 'FRANCE' };
      expect(ForfaitJoursFrSectionPrefillRules.computeAccordCollectifExiste(input)).toBe(true);
    });

    it('retourne null si non booléen', () => {
      const input = {
        aiData: { ...aiFull, forfaitJoursAccordCollectifExiste: undefined } as any,
        workspaceCountry: 'FRANCE',
      };
      expect(ForfaitJoursFrSectionPrefillRules.computeAccordCollectifExiste(input)).toBeNull();
    });
  });

  describe('computeNbJoursForfait', () => {
    it('retourne la valeur entière dans la plage [0, 235]', () => {
      expect(ForfaitJoursFrSectionPrefillRules.computeNbJoursForfait({
        aiData: { ...aiFull, forfaitJoursNbJours: 218 },
        workspaceCountry: 'FRANCE',
      })).toBe(218);
    });

    it('tronque les décimales', () => {
      expect(ForfaitJoursFrSectionPrefillRules.computeNbJoursForfait({
        aiData: { ...aiFull, forfaitJoursNbJours: 218.7 },
        workspaceCountry: 'FRANCE',
      })).toBe(218);
    });

    it('retourne null si > 235', () => {
      expect(ForfaitJoursFrSectionPrefillRules.computeNbJoursForfait({
        aiData: { ...aiFull, forfaitJoursNbJours: 400 },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });

    it('retourne null si négatif', () => {
      expect(ForfaitJoursFrSectionPrefillRules.computeNbJoursForfait({
        aiData: { ...aiFull, forfaitJoursNbJours: -1 },
        workspaceCountry: 'FRANCE',
      })).toBeNull();
    });
  });

  describe('computePrefillCount', () => {
    it('= 5 sur fixture IA complète FR', () => {
      expect(ForfaitJoursFrSectionPrefillRules.computePrefillCount({
        aiData: aiFull,
        workspaceCountry: 'FRANCE',
      })).toBe(5);
    });

    it('= 0 hors France', () => {
      expect(ForfaitJoursFrSectionPrefillRules.computePrefillCount({
        aiData: aiFull,
        workspaceCountry: 'BELGIQUE',
      })).toBe(0);
    });

    it('= 2 si seulement 2 champs présents', () => {
      expect(ForfaitJoursFrSectionPrefillRules.computePrefillCount({
        aiData: {
          forfaitJoursAccordCollectifExiste: true,
          forfaitJoursNbJours: 218,
        },
        workspaceCountry: 'FRANCE',
      })).toBe(2);
    });

    it('= 0 si tous undefined', () => {
      expect(ForfaitJoursFrSectionPrefillRules.computePrefillCount({
        aiData: {},
        workspaceCountry: 'FRANCE',
      })).toBe(0);
    });
  });
});
