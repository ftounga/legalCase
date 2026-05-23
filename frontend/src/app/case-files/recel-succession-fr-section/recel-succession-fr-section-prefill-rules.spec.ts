/**
 * SF-216-22 — tests Jest helper `recel-succession-fr-section-prefill-rules`.
 */
import {
  RecelSuccessionFrPrefillRules,
  computePrefillCount,
  prefillFromAi,
} from './recel-succession-fr-section-prefill-rules';

describe('RecelSuccessionFrPrefillRules', () => {
  describe('prefillFromAi', () => {
    it('retourne null partout en BELGIQUE', () => {
      const v = prefillFromAi({
        aiData: {
          typeRecelDetecte: 'DISSIMULATION_BIEN',
          preuveRecelDetectee: 'DOCUMENT',
          dateOuvertureSuccessionDetectee: '2024-05-01',
        },
        workspaceCountry: 'BELGIQUE',
      });
      expect(v.typeRecel).toBeNull();
      expect(v.preuveRecel).toBeNull();
      expect(v.dateOuvertureSuccession).toBeNull();
    });

    it('retourne null partout si aiData absent (FR)', () => {
      const v = prefillFromAi({ workspaceCountry: 'FRANCE' });
      expect(v.typeRecel).toBeNull();
      expect(v.preuveRecel).toBeNull();
      expect(v.dateOuvertureSuccession).toBeNull();
    });

    it('retourne null partout si aiData null (FR)', () => {
      const v = prefillFromAi({ aiData: null, workspaceCountry: 'FRANCE' });
      expect(v.typeRecel).toBeNull();
      expect(v.preuveRecel).toBeNull();
      expect(v.dateOuvertureSuccession).toBeNull();
    });

    it('mappe les 3 champs IA FR', () => {
      const v = prefillFromAi({
        aiData: {
          typeRecelDetecte: 'DESTRUCTION_TESTAMENT',
          preuveRecelDetectee: 'FAISCEAU_INDICES',
          dateOuvertureSuccessionDetectee: '2025-02-14',
        },
        workspaceCountry: 'FRANCE',
      });
      expect(v.typeRecel).toBe('DESTRUCTION_TESTAMENT');
      expect(v.preuveRecel).toBe('FAISCEAU_INDICES');
      expect(v.dateOuvertureSuccession).toBe('2025-02-14');
    });

    it('ignore les valeurs enum non whitelistées', () => {
      const v = prefillFromAi({
        aiData: {
          typeRecelDetecte: 'INCONNU_NON_WHITELISTE',
          preuveRecelDetectee: 'PAS_UN_ENUM',
          dateOuvertureSuccessionDetectee: '   ',
        } as any,
        workspaceCountry: 'FRANCE',
      });
      expect(v.typeRecel).toBeNull();
      expect(v.preuveRecel).toBeNull();
      expect(v.dateOuvertureSuccession).toBeNull();
    });

    it('ignore les champs non-string / nullish', () => {
      const v = prefillFromAi({
        aiData: {
          typeRecelDetecte: undefined,
          preuveRecelDetectee: null,
          dateOuvertureSuccessionDetectee: null,
        } as any,
        workspaceCountry: 'FRANCE',
      });
      expect(v.typeRecel).toBeNull();
      expect(v.preuveRecel).toBeNull();
      expect(v.dateOuvertureSuccession).toBeNull();
    });
  });

  describe('computePrefillCount', () => {
    it('retourne 0 hors France', () => {
      expect(
        computePrefillCount({
          aiData: {
            typeRecelDetecte: 'DISSIMULATION_BIEN',
            preuveRecelDetectee: 'AVEUX',
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
            typeRecelDetecte: 'DISSIMULATION_BIEN',
          },
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(1);
      expect(
        computePrefillCount({
          aiData: {
            typeRecelDetecte: 'DISSIMULATION_BIEN',
            preuveRecelDetectee: 'DOCUMENT',
          },
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(2);
      expect(
        computePrefillCount({
          aiData: {
            typeRecelDetecte: 'DESTRUCTION_TESTAMENT',
            preuveRecelDetectee: 'TEMOIGNAGE',
            dateOuvertureSuccessionDetectee: '2025-01-01',
          },
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(3);
    });
  });

  it('expose un objet utilitaire stable', () => {
    expect(RecelSuccessionFrPrefillRules.computePrefillCount).toBe(
      computePrefillCount,
    );
    expect(RecelSuccessionFrPrefillRules.prefillFromAi).toBe(prefillFromAi);
  });
});
