/**
 * SF-216-30 — tests Jest helper `donation-partage-fr-section-prefill-rules`.
 */
import {
  DonationPartageFrPrefillRules,
  computePrefillCount,
  prefillFromAi,
} from './donation-partage-fr-section-prefill-rules';

describe('DonationPartageFrPrefillRules', () => {
  describe('prefillFromAi', () => {
    it('retourne null partout en BELGIQUE', () => {
      const v = prefillFromAi({
        aiData: {
          nbDescendantsDetecte: 2,
          respectQuotiteDisponibleDetected: true,
          presencePetitsEnfantsSubstitutionDetectee: true,
          donationPartageConjonctiveDetectee: true,
        },
        workspaceCountry: 'BELGIQUE',
      });
      expect(v.nombreDescendants).toBeNull();
      expect(v.respectQuotiteDisponible).toBeNull();
      expect(v.presencePetitsEnfantsParSubstitution).toBeNull();
      expect(v.donationPartageConjonctive).toBeNull();
    });

    it('retourne null partout si aiData absent (FR)', () => {
      const v = prefillFromAi({ workspaceCountry: 'FRANCE' });
      expect(v.nombreDescendants).toBeNull();
      expect(v.respectQuotiteDisponible).toBeNull();
      expect(v.presencePetitsEnfantsParSubstitution).toBeNull();
      expect(v.donationPartageConjonctive).toBeNull();
    });

    it('retourne null partout si aiData null (FR)', () => {
      const v = prefillFromAi({ aiData: null, workspaceCountry: 'FRANCE' });
      expect(v.nombreDescendants).toBeNull();
      expect(v.respectQuotiteDisponible).toBeNull();
    });

    it('mappe les 4 champs IA FR', () => {
      const v = prefillFromAi({
        aiData: {
          nbDescendantsDetecte: 3,
          respectQuotiteDisponibleDetected: true,
          presencePetitsEnfantsSubstitutionDetectee: true,
          donationPartageConjonctiveDetectee: false,
        },
        workspaceCountry: 'FRANCE',
      });
      expect(v.nombreDescendants).toBe(3);
      expect(v.respectQuotiteDisponible).toBe(true);
      expect(v.presencePetitsEnfantsParSubstitution).toBe(true);
      expect(v.donationPartageConjonctive).toBe(false);
    });

    it('rejette les nombreDescendants <= 0', () => {
      const v = prefillFromAi({
        aiData: { nbDescendantsDetecte: 0 } as any,
        workspaceCountry: 'FRANCE',
      });
      expect(v.nombreDescendants).toBeNull();
    });

    it('rejette les nombreDescendants non numériques', () => {
      const v = prefillFromAi({
        aiData: { nbDescendantsDetecte: '3' as any } as any,
        workspaceCountry: 'FRANCE',
      });
      expect(v.nombreDescendants).toBeNull();
    });

    it('ignore les non-boolean', () => {
      const v = prefillFromAi({
        aiData: {
          respectQuotiteDisponibleDetected: undefined,
          presencePetitsEnfantsSubstitutionDetectee: null,
          donationPartageConjonctiveDetectee: 'non' as any,
        } as any,
        workspaceCountry: 'FRANCE',
      });
      expect(v.respectQuotiteDisponible).toBeNull();
      expect(v.presencePetitsEnfantsParSubstitution).toBeNull();
      expect(v.donationPartageConjonctive).toBeNull();
    });
  });

  describe('computePrefillCount', () => {
    it('retourne 0 hors France', () => {
      expect(
        computePrefillCount({
          aiData: {
            nbDescendantsDetecte: 2,
            respectQuotiteDisponibleDetected: true,
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
          aiData: { nbDescendantsDetecte: 2 },
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(1);
      expect(
        computePrefillCount({
          aiData: {
            nbDescendantsDetecte: 3,
            respectQuotiteDisponibleDetected: true,
            presencePetitsEnfantsSubstitutionDetectee: false,
            donationPartageConjonctiveDetectee: true,
          },
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(4);
    });
  });

  it('expose un objet utilitaire stable', () => {
    expect(DonationPartageFrPrefillRules.computePrefillCount).toBe(
      computePrefillCount,
    );
    expect(DonationPartageFrPrefillRules.prefillFromAi).toBe(prefillFromAi);
  });
});
