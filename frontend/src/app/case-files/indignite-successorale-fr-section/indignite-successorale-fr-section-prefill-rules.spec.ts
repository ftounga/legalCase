/**
 * SF-216-20 — tests Jest helper `indignite-successorale-fr-section-prefill-rules`.
 */
import {
  IndigniteSuccessoraleFrPrefillRules,
  computePrefillCount,
  prefillFromAi,
} from './indignite-successorale-fr-section-prefill-rules';

describe('IndigniteSuccessoraleFrPrefillRules', () => {
  describe('prefillFromAi', () => {
    it('retourne null partout en BELGIQUE', () => {
      const v = prefillFromAi({
        aiData: {
          condamnationPenaleSuccessionDetectee: true,
          pardonTestamentaireDetecte: false,
          dateOuvertureSuccessionDetectee: '2024-05-01',
        },
        workspaceCountry: 'BELGIQUE',
      });
      expect(v.condamnationPrononcee).toBeNull();
      expect(v.pardonTestamentaireDetecte).toBeNull();
      expect(v.dateOuvertureSuccession).toBeNull();
    });

    it('retourne null partout si aiData absent (FR)', () => {
      const v = prefillFromAi({ workspaceCountry: 'FRANCE' });
      expect(v.condamnationPrononcee).toBeNull();
      expect(v.pardonTestamentaireDetecte).toBeNull();
      expect(v.dateOuvertureSuccession).toBeNull();
    });

    it('retourne null partout si aiData null (FR)', () => {
      const v = prefillFromAi({ aiData: null, workspaceCountry: 'FRANCE' });
      expect(v.condamnationPrononcee).toBeNull();
      expect(v.pardonTestamentaireDetecte).toBeNull();
      expect(v.dateOuvertureSuccession).toBeNull();
    });

    it('mappe les 3 champs IA FR', () => {
      const v = prefillFromAi({
        aiData: {
          condamnationPenaleSuccessionDetectee: true,
          pardonTestamentaireDetecte: false,
          dateOuvertureSuccessionDetectee: '2025-02-14',
        },
        workspaceCountry: 'FRANCE',
      });
      expect(v.condamnationPrononcee).toBe(true);
      expect(v.pardonTestamentaireDetecte).toBe(false);
      expect(v.dateOuvertureSuccession).toBe('2025-02-14');
    });

    it('ignore les champs non-boolean / chaîne vide', () => {
      const v = prefillFromAi({
        aiData: {
          condamnationPenaleSuccessionDetectee: undefined,
          pardonTestamentaireDetecte: null,
          dateOuvertureSuccessionDetectee: '   ',
        } as any,
        workspaceCountry: 'FRANCE',
      });
      expect(v.condamnationPrononcee).toBeNull();
      expect(v.pardonTestamentaireDetecte).toBeNull();
      expect(v.dateOuvertureSuccession).toBeNull();
    });
  });

  describe('computePrefillCount', () => {
    it('retourne 0 hors France', () => {
      expect(
        computePrefillCount({
          aiData: {
            condamnationPenaleSuccessionDetectee: true,
            pardonTestamentaireDetecte: true,
            dateOuvertureSuccessionDetectee: '2024-01-01',
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
          aiData: {
            condamnationPenaleSuccessionDetectee: true,
          },
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(1);
      expect(
        computePrefillCount({
          aiData: {
            condamnationPenaleSuccessionDetectee: true,
            pardonTestamentaireDetecte: false,
          },
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(2);
      expect(
        computePrefillCount({
          aiData: {
            condamnationPenaleSuccessionDetectee: false,
            pardonTestamentaireDetecte: true,
            dateOuvertureSuccessionDetectee: '2025-01-01',
          },
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(3);
    });
  });

  it('expose un objet utilitaire stable', () => {
    expect(IndigniteSuccessoraleFrPrefillRules.computePrefillCount).toBe(
      computePrefillCount,
    );
    expect(IndigniteSuccessoraleFrPrefillRules.prefillFromAi).toBe(
      prefillFromAi,
    );
  });
});
