/**
 * SF-216-26 — tests Jest helper `presomption-paternite-fr-section-prefill-rules`.
 */
import {
  PresomptionPaterniteFrPrefillRules,
  computePrefillCount,
  prefillFromAi,
} from './presomption-paternite-fr-section-prefill-rules';

describe('PresomptionPaterniteFrPrefillRules', () => {
  describe('prefillFromAi', () => {
    it('retourne null partout en BELGIQUE', () => {
      const v = prefillFromAi({
        aiData: {
          dateNaissanceEnfantDetectee: '2025-09-01',
          possessionEtatConforme5AnsDetected: true,
          dateConclusionMariageDetectee: '2020-06-01',
          dateDissolutionMariageDetectee: null,
          desaveuEnvisage: true,
        },
        workspaceCountry: 'BELGIQUE',
      });
      expect(v.dateNaissanceEnfant).toBeNull();
      expect(v.possessionEtatConformeDetecte).toBeNull();
      expect(v.dateConclusionMariage).toBeNull();
      expect(v.dateDissolutionMariage).toBeNull();
      expect(v.desaveuEnvisage).toBeNull();
    });

    it('retourne null partout si aiData absent (FR)', () => {
      const v = prefillFromAi({ workspaceCountry: 'FRANCE' });
      expect(v.dateNaissanceEnfant).toBeNull();
      expect(v.possessionEtatConformeDetecte).toBeNull();
      expect(v.dateConclusionMariage).toBeNull();
      expect(v.dateDissolutionMariage).toBeNull();
      expect(v.desaveuEnvisage).toBeNull();
    });

    it('retourne null partout si aiData null (FR)', () => {
      const v = prefillFromAi({ aiData: null, workspaceCountry: 'FRANCE' });
      expect(v.dateNaissanceEnfant).toBeNull();
      expect(v.possessionEtatConformeDetecte).toBeNull();
      expect(v.dateConclusionMariage).toBeNull();
      expect(v.dateDissolutionMariage).toBeNull();
      expect(v.desaveuEnvisage).toBeNull();
    });

    it('mappe les 5 champs IA FR', () => {
      const v = prefillFromAi({
        aiData: {
          dateNaissanceEnfantDetectee: '2025-09-01',
          possessionEtatConforme5AnsDetected: true,
          dateConclusionMariageDetectee: '2020-06-01',
          dateDissolutionMariageDetectee: '2024-12-01',
          desaveuEnvisage: true,
        },
        workspaceCountry: 'FRANCE',
      });
      expect(v.dateNaissanceEnfant).toBe('2025-09-01');
      expect(v.possessionEtatConformeDetecte).toBe(true);
      expect(v.dateConclusionMariage).toBe('2020-06-01');
      expect(v.dateDissolutionMariage).toBe('2024-12-01');
      expect(v.desaveuEnvisage).toBe(true);
    });

    it('ignore les dates au mauvais format', () => {
      const v = prefillFromAi({
        aiData: {
          dateNaissanceEnfantDetectee: '01/09/2025',
          dateConclusionMariageDetectee: 'bidon',
          dateDissolutionMariageDetectee: '   ',
        } as any,
        workspaceCountry: 'FRANCE',
      });
      expect(v.dateNaissanceEnfant).toBeNull();
      expect(v.dateConclusionMariage).toBeNull();
      expect(v.dateDissolutionMariage).toBeNull();
    });

    it('ignore les champs non-boolean / nullish', () => {
      const v = prefillFromAi({
        aiData: {
          possessionEtatConforme5AnsDetected: undefined,
          desaveuEnvisage: null,
        } as any,
        workspaceCountry: 'FRANCE',
      });
      expect(v.possessionEtatConformeDetecte).toBeNull();
      expect(v.desaveuEnvisage).toBeNull();
    });

    it('possession false admis (boolean explicite)', () => {
      const v = prefillFromAi({
        aiData: {
          possessionEtatConforme5AnsDetected: false,
          desaveuEnvisage: false,
        } as any,
        workspaceCountry: 'FRANCE',
      });
      expect(v.possessionEtatConformeDetecte).toBe(false);
      expect(v.desaveuEnvisage).toBe(false);
    });
  });

  describe('computePrefillCount', () => {
    it('retourne 0 hors France', () => {
      expect(
        computePrefillCount({
          aiData: {
            dateNaissanceEnfantDetectee: '2025-09-01',
            possessionEtatConforme5AnsDetected: true,
            desaveuEnvisage: true,
          } as any,
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
          aiData: { dateNaissanceEnfantDetectee: '2025-09-01' } as any,
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(1);
      expect(
        computePrefillCount({
          aiData: {
            dateNaissanceEnfantDetectee: '2025-09-01',
            possessionEtatConforme5AnsDetected: true,
          } as any,
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(2);
      expect(
        computePrefillCount({
          aiData: {
            dateNaissanceEnfantDetectee: '2025-09-01',
            possessionEtatConforme5AnsDetected: true,
            dateConclusionMariageDetectee: '2020-06-01',
            dateDissolutionMariageDetectee: '2024-12-01',
            desaveuEnvisage: true,
          } as any,
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(5);
    });
  });

  it('expose un objet utilitaire stable', () => {
    expect(PresomptionPaterniteFrPrefillRules.computePrefillCount).toBe(
      computePrefillCount,
    );
    expect(PresomptionPaterniteFrPrefillRules.prefillFromAi).toBe(prefillFromAi);
  });
});
