/**
 * SF-216-28 — tests Jest helper `partage-notarial-fr-section-prefill-rules`.
 */
import {
  PartageNotarialFrPrefillRules,
  computePrefillCount,
  prefillFromAi,
} from './partage-notarial-fr-section-prefill-rules';

describe('PartageNotarialFrPrefillRules', () => {
  describe('prefillFromAi', () => {
    it('retourne null partout en BELGIQUE', () => {
      const v = prefillFromAi({
        aiData: {
          dateOuvertureSuccessionDetectee: '2024-12-10',
          nombreCoheritiersDetecte: 3,
          montantSuccessionEurDetecte: 250000,
          presenceImmeubleSuccessionDetecte: true,
        },
        workspaceCountry: 'BELGIQUE',
      });
      expect(v.dateOuvertureSuccession).toBeNull();
      expect(v.nombreCoheritiers).toBeNull();
      expect(v.valeurMasseSuccessoraleEur).toBeNull();
      expect(v.presenceImmeuble).toBeNull();
    });

    it('retourne null partout si aiData absent (FR)', () => {
      const v = prefillFromAi({ workspaceCountry: 'FRANCE' });
      expect(v.dateOuvertureSuccession).toBeNull();
      expect(v.nombreCoheritiers).toBeNull();
      expect(v.valeurMasseSuccessoraleEur).toBeNull();
      expect(v.presenceImmeuble).toBeNull();
    });

    it('retourne null partout si aiData null (FR)', () => {
      const v = prefillFromAi({ aiData: null, workspaceCountry: 'FRANCE' });
      expect(v.dateOuvertureSuccession).toBeNull();
      expect(v.nombreCoheritiers).toBeNull();
    });

    it('mappe les 4 champs IA FR', () => {
      const v = prefillFromAi({
        aiData: {
          dateOuvertureSuccessionDetectee: '2024-12-10',
          nombreCoheritiersDetecte: 3,
          montantSuccessionEurDetecte: 250000,
          presenceImmeubleSuccessionDetecte: true,
        },
        workspaceCountry: 'FRANCE',
      });
      expect(v.dateOuvertureSuccession).toBe('2024-12-10');
      expect(v.nombreCoheritiers).toBe(3);
      expect(v.valeurMasseSuccessoraleEur).toBe(250000);
      expect(v.presenceImmeuble).toBe(true);
    });

    it('rejette les dates non-ISO', () => {
      const v = prefillFromAi({
        aiData: { dateOuvertureSuccessionDetectee: '10/12/2024' } as any,
        workspaceCountry: 'FRANCE',
      });
      expect(v.dateOuvertureSuccession).toBeNull();
    });

    it('rejette les nombres négatifs', () => {
      const v = prefillFromAi({
        aiData: {
          nombreCoheritiersDetecte: -2,
          montantSuccessionEurDetecte: -100,
        } as any,
        workspaceCountry: 'FRANCE',
      });
      expect(v.nombreCoheritiers).toBeNull();
      expect(v.valeurMasseSuccessoraleEur).toBeNull();
    });

    it('rejette les nombres non entiers pour cohéritiers', () => {
      const v = prefillFromAi({
        aiData: { nombreCoheritiersDetecte: 2.5 } as any,
        workspaceCountry: 'FRANCE',
      });
      expect(v.nombreCoheritiers).toBeNull();
    });

    it('arrondit la masse successorale (centimes ignorés)', () => {
      const v = prefillFromAi({
        aiData: { montantSuccessionEurDetecte: 250000.75 } as any,
        workspaceCountry: 'FRANCE',
      });
      expect(v.valeurMasseSuccessoraleEur).toBe(250001);
    });

    it('ignore presenceImmeuble non boolean', () => {
      const v = prefillFromAi({
        aiData: { presenceImmeubleSuccessionDetecte: 'oui' } as any,
        workspaceCountry: 'FRANCE',
      });
      expect(v.presenceImmeuble).toBeNull();
    });

    it('accepte presenceImmeuble=false explicite', () => {
      const v = prefillFromAi({
        aiData: { presenceImmeubleSuccessionDetecte: false },
        workspaceCountry: 'FRANCE',
      });
      expect(v.presenceImmeuble).toBe(false);
    });
  });

  describe('computePrefillCount', () => {
    it('retourne 0 hors France', () => {
      expect(
        computePrefillCount({
          aiData: {
            dateOuvertureSuccessionDetectee: '2024-12-10',
            nombreCoheritiersDetecte: 3,
          },
          workspaceCountry: 'BELGIQUE',
        }),
      ).toBe(0);
    });

    it('retourne 0 si rien détecté (FR)', () => {
      expect(computePrefillCount({ workspaceCountry: 'FRANCE' })).toBe(0);
      expect(
        computePrefillCount({ aiData: null, workspaceCountry: 'FRANCE' }),
      ).toBe(0);
    });

    it('compte chaque champ détecté (FR)', () => {
      expect(
        computePrefillCount({
          aiData: { dateOuvertureSuccessionDetectee: '2024-12-10' },
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(1);
      expect(
        computePrefillCount({
          aiData: {
            dateOuvertureSuccessionDetectee: '2024-12-10',
            nombreCoheritiersDetecte: 3,
            montantSuccessionEurDetecte: 250000,
            presenceImmeubleSuccessionDetecte: true,
          },
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(4);
    });
  });

  it('expose un objet utilitaire stable', () => {
    expect(PartageNotarialFrPrefillRules.computePrefillCount).toBe(
      computePrefillCount,
    );
    expect(PartageNotarialFrPrefillRules.prefillFromAi).toBe(prefillFromAi);
  });
});
